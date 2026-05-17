# Distributed Cron Job System

A fault-tolerant, multi-node distributed cron system with DAG-based dependencies, timezone-aware scheduling, and exactly-once execution guarantees.

## Architecture Highlights

```
┌─────────────────────────────────────────────┐
│  Scheduler Node 1  │  Scheduler Node 2  │ ...│  (Active-Active)
├─────────────────────────────────────────────┤
│  Distributed Lock Layer (Fencing Tokens)    │  (Exactly-Once)
├─────────────────────────────────────────────┤
│  Task Executors: HTTP │ Shell │ Kafka │ gRPC│  (Pluggable)
├─────────────────────────────────────────────┤
│  PostgreSQL (jobs, locks, history)          │  (Durable)
└─────────────────────────────────────────────┘
```

## Key Differentiators from General Job Schedulers

| Feature | This System |
|---------|-------------|
| DAG Dependencies | Job B runs only after Job A succeeds |
| Timezone-Aware | Handles DST transitions correctly |
| Concurrency Policies | ALLOW / FORBID / REPLACE |
| Fencing Tokens | Prevents stale execution after GC pauses |
| Namespace Isolation | Multi-tenant with per-namespace job lists |
| Multiple Task Types | HTTP, Shell, Kafka, gRPC |

## Quick Start

```bash
# Start the system
./run-systems.sh cronjob  # Port 8102

# Create a cron job
curl -X POST http://localhost:8102/api/v1/cron/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "health-check",
    "namespace": "monitoring",
    "cronExpression": "0 */5 * * * *",
    "timezone": "UTC",
    "taskType": "HTTP_WEBHOOK",
    "taskConfig": {"url": "https://api.example.com/health", "method": "GET"}
  }'

# Trigger manually
curl -X POST http://localhost:8102/api/v1/cron/jobs/{id}/trigger

# View execution history
curl http://localhost:8102/api/v1/cron/jobs/{id}/executions
```

## Scale

| Metric | Target |
|--------|--------|
| Total cron jobs | 10M+ |
| Executions/sec | 100K |
| Scheduling precision | ±1 second |
| Availability | 99.99% |
| Lock acquisition | <5ms |
