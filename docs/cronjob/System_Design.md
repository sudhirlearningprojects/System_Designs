# Distributed Cron Job System Design

## Understanding Distributed Cron Systems

### What is a Distributed Cron System?
A distributed cron system is a fault-tolerant, multi-node replacement for Unix crontab that guarantees exactly-once execution of recurring tasks across a cluster. Unlike single-machine cron, it handles node failures, timezone-aware scheduling, job dependencies (DAGs), and multi-tenant isolation.

### How It Differs from a General Job Scheduler
| Feature | General Job Scheduler | Distributed Cron System |
|---------|----------------------|------------------------|
| Focus | One-off + recurring tasks | Recurring cron-based tasks |
| Dependencies | Simple priority | DAG-based dependencies |
| Scheduling | Time-based + event-based | Cron expressions + timezone |
| Concurrency | Queue-based | Policy-based (Allow/Forbid/Replace) |
| Multi-tenancy | Single tenant | Namespace isolation |
| Execution | Internal handlers | HTTP/Shell/Kafka/gRPC tasks |

---

## Table of Contents
1. [System Overview](#system-overview)
2. [High-Level Design](#high-level-design)
3. [Low-Level Design](#low-level-design)
4. [Data Model](#data-model)
5. [Core Algorithms](#core-algorithms)
6. [Fault Tolerance](#fault-tolerance)
7. [Scalability](#scalability)
8. [API Reference](#api-reference)

---

## System Overview

### Functional Requirements
- Create/update/delete cron jobs with standard cron expressions
- Timezone-aware scheduling (DST handling)
- DAG-based job dependencies (run B after A completes)
- Concurrency policies: Allow, Forbid (skip), Replace (kill previous)
- Multi-tenant namespace isolation
- HTTP webhook, shell command, Kafka, gRPC task types
- Manual trigger and execution history
- Pause/resume individual jobs

### Non-Functional Requirements
- **Exactly-once execution** via distributed locks with fencing tokens
- **99.99% availability** with multi-node active-active
- **Sub-second scheduling precision** (±1s of target time)
- **10M+ cron jobs** across all namespaces
- **100K executions/sec** peak throughput

---

## High-Level Design

```
┌──────────────────────────────────────────────────────────────┐
│                      API Layer (REST)                         │
│  Create Job │ Pause/Resume │ Trigger │ History │ DAG Mgmt    │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                  Cron Scheduler Engine                        │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │ Tick Loop   │  │ Timezone     │  │ DAG Orchestrator   │  │
│  │ (1s poll)   │  │ Calculator   │  │ (dependency check) │  │
│  └─────────────┘  └──────────────┘  └────────────────────┘  │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│              Distributed Lock Layer                           │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  │
│  │ Fencing Tokens  │  │ Lock TTL + Auto-expiry           │  │
│  └─────────────────┘  └──────────────────────────────────┘  │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                  Task Executor                                │
│  ┌──────┐  ┌───────┐  ┌───────┐  ┌──────┐                  │
│  │ HTTP │  │ Shell │  │ Kafka │  │ gRPC │                  │
│  └──────┘  └───────┘  └───────┘  └──────┘                  │
└──────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                  Storage Layer                                │
│  PostgreSQL (jobs, executions, locks) + Redis (hot state)    │
└──────────────────────────────────────────────────────────────┘
```

---

## Low-Level Design

### Tick Loop (Scheduler Engine)

```
Every 1 second:
  1. Query: SELECT * FROM cron_jobs WHERE status='ACTIVE' AND next_run_at <= NOW()
  2. For each due job:
     a. Try acquire distributed lock (cron:{jobId})
     b. If lock acquired:
        - Check concurrency policy
        - Execute task (HTTP/Shell/Kafka/gRPC)
        - Record execution result
        - Calculate and set next_run_at
        - Release lock
     c. If lock not acquired: skip (another node handles it)
```

### Fencing Token Mechanism

```
Problem: Node A acquires lock, gets paused by GC, lock expires,
         Node B acquires lock and starts executing,
         Node A resumes and also executes → DUPLICATE

Solution: Monotonically increasing fencing tokens
  - Each lock acquisition gets token T
  - Task execution includes token in request
  - Downstream systems reject requests with token < last seen token
```

### DAG Execution Flow

```
DAG: A → B → C
         ↘ D

1. DAG trigger fires at cron time
2. Execute root nodes (A) - no dependencies
3. On A completion:
   - Check dependents: B depends on [A] ✓ → execute B
   - Check dependents: D depends on [A] ✓ → execute D
4. On B completion:
   - Check dependents: C depends on [B] ✓ → execute C
5. DAG complete when all leaf nodes finish
```

### Timezone-Aware Scheduling

```java
// Problem: "0 0 2 * * *" in America/New_York
// During DST spring-forward: 2:00 AM doesn't exist
// During DST fall-back: 2:00 AM happens twice

// Solution: Calculate next run in target timezone, convert to server time
ZonedDateTime nowInZone = ZonedDateTime.now(ZoneId.of("America/New_York"));
LocalDateTime nextInZone = cronExpression.next(nowInZone.toLocalDateTime());
ZonedDateTime nextZoned = nextInZone.atZone(ZoneId.of("America/New_York"));
LocalDateTime serverTime = nextZoned.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
```

---

## Data Model

### cron_jobs
```sql
CREATE TABLE cron_jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    namespace VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    task_type VARCHAR(20) NOT NULL,
    task_config TEXT,
    dag_id UUID REFERENCES cron_dags(id),
    depends_on TEXT,  -- JSON array of job UUIDs
    timeout_seconds INT DEFAULT 300,
    max_retries INT DEFAULT 3,
    retry_delay_seconds INT DEFAULT 60,
    concurrency_policy VARCHAR(20) DEFAULT 'FORBID',
    next_run_at TIMESTAMP,
    last_run_at TIMESTAMP,
    last_run_status VARCHAR(20),
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(namespace, name)
);
```

### cron_locks (Distributed Locking)
```sql
CREATE TABLE cron_locks (
    lock_key VARCHAR(255) PRIMARY KEY,
    owner_node VARCHAR(255) NOT NULL,
    fencing_token BIGINT NOT NULL,
    acquired_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
```

### cron_job_executions
```sql
CREATE TABLE cron_job_executions (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES cron_jobs(id),
    dag_run_id UUID,
    status VARCHAR(20) NOT NULL,
    attempt_number INT DEFAULT 1,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    executor_node VARCHAR(255),
    fencing_token BIGINT,
    http_status_code INT,
    response_body TEXT,
    error_message TEXT,
    triggered_by VARCHAR(20)  -- SCHEDULER, MANUAL, DAG_DEPENDENCY
);
```

---

## Core Algorithms

### 1. Exactly-Once Execution with Distributed Locks

```
acquire_lock(job_id, node_id):
    DELETE FROM cron_locks WHERE expires_at < NOW()
    
    existing = SELECT * FROM cron_locks WHERE lock_key = 'cron:{job_id}'
    IF existing AND existing.expires_at > NOW():
        RETURN None  // Lock held
    
    token = atomic_increment(global_counter)
    UPSERT INTO cron_locks (lock_key, owner_node, fencing_token, expires_at)
        VALUES ('cron:{job_id}', node_id, token, NOW() + 60s)
    RETURN token
```

### 2. Concurrency Policy Enforcement

```
FORBID:  If running execution exists → skip, record SKIPPED
ALLOW:   Always execute regardless of running instances
REPLACE: Kill running execution → start new one
```

### 3. Retry with Exponential Backoff

```
retry_delay = retry_delay_seconds * 2^(attempt - 1)
max_delay = min(retry_delay, 3600)  // Cap at 1 hour

Attempt 1: immediate
Attempt 2: 60s delay
Attempt 3: 120s delay
```

---

## Fault Tolerance

### Node Failure Recovery
- Lock TTL = 60 seconds
- If a node dies mid-execution, lock expires automatically
- Next tick cycle, another node picks up the job
- Execution marked as TIMEOUT after job.timeoutSeconds

### Split-Brain Prevention
- Fencing tokens ensure stale executions are rejected
- Database-level locks prevent concurrent lock acquisition
- Serializable isolation on lock table

### Missed Schedule Detection
```
On node startup:
  SELECT * FROM cron_jobs 
  WHERE status = 'ACTIVE' AND next_run_at < NOW() - INTERVAL '5 minutes'
  
  For each missed job:
    - Execute immediately (catch-up)
    - Advance next_run_at to future
```

---

## Scalability

### Horizontal Scaling Strategy
- **Multiple scheduler nodes** poll independently
- **Distributed locks** prevent duplicate execution
- **Namespace-based sharding**: assign namespace ranges to nodes
- **Database partitioning**: partition cron_job_executions by month

### Performance Optimizations
- **Index on next_run_at**: O(log n) query for due jobs
- **Batch processing**: fetch 100 due jobs per tick
- **Connection pooling**: HikariCP with 20 connections per node
- **Redis caching**: cache job configs, reduce DB reads

### Scale Numbers
| Metric | Value |
|--------|-------|
| Total cron jobs | 10M+ |
| Executions/sec | 100K |
| Scheduler nodes | 10-50 |
| Scheduling precision | ±1 second |
| Lock acquisition | <5ms |
| Execution history retention | 90 days |

---

## API Reference

### Create Cron Job
```bash
POST /api/v1/cron/jobs
{
  "name": "daily-report",
  "namespace": "analytics",
  "cronExpression": "0 0 9 * * MON-FRI",
  "timezone": "America/New_York",
  "taskType": "HTTP_WEBHOOK",
  "taskConfig": {
    "url": "https://api.example.com/reports/generate",
    "method": "POST",
    "headers": {"Authorization": "Bearer <token>"},
    "body": {"type": "daily"}
  },
  "concurrencyPolicy": "FORBID",
  "timeoutSeconds": 120,
  "maxRetries": 3
}
```

### Create DAG
```bash
POST /api/v1/cron/dags
{
  "name": "etl-pipeline",
  "namespace": "data-eng",
  "cronExpression": "0 0 2 * * *",
  "timezone": "UTC",
  "maxParallelTasks": 3
}
```

### Create DAG Task with Dependency
```bash
POST /api/v1/cron/jobs
{
  "name": "transform-data",
  "namespace": "data-eng",
  "cronExpression": "0 0 2 * * *",
  "dagId": "<dag-uuid>",
  "dependsOn": ["<extract-job-uuid>"],
  "taskType": "SHELL_COMMAND",
  "taskConfig": {"command": "python /opt/etl/transform.py"}
}
```

### Trigger Manual Execution
```bash
POST /api/v1/cron/jobs/{id}/trigger
```

### Get Execution History
```bash
GET /api/v1/cron/jobs/{id}/executions
```

---

## Interview Discussion Points

### Q: How do you prevent duplicate execution across nodes?
Distributed locks with fencing tokens. Each lock acquisition generates a monotonically increasing token. If a node's GC pause causes its lock to expire and another node takes over, the stale node's token will be lower than the current token, allowing downstream systems to reject stale requests.

### Q: How do you handle timezone and DST changes?
All scheduling calculations happen in the job's configured timezone. We use ZonedDateTime to handle DST transitions correctly — if 2:00 AM doesn't exist (spring forward), the next valid time is used. If 2:00 AM occurs twice (fall back), the first occurrence is used.

### Q: Why database locks instead of Redis for distributed locking?
For a cron system, correctness > performance. Database locks with serializable isolation provide stronger guarantees than Redis (which can lose data on failover). The lock acquisition rate is low (once per job per schedule), so the extra latency is acceptable.

### Q: How do you scale to 10M cron jobs?
- Partition jobs by namespace across scheduler nodes
- Each node only polls its assigned partitions
- Index on (status, next_run_at) makes the poll query O(log n)
- Batch fetch 100 due jobs per tick to reduce round trips
