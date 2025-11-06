# Job Scheduler - Architecture Diagrams

## Understanding Job Scheduler Architecture

### What Makes Job Scheduling Complex?
Job scheduling systems face unique challenges that require specialized architectural patterns:

1. **Timing Precision**: Execute jobs at exact scheduled times
2. **Distributed Coordination**: Multiple scheduler nodes without conflicts
3. **Fault Tolerance**: Handle node failures without losing jobs
4. **Scalability**: Handle millions of scheduled jobs efficiently
5. **Exactly-Once Execution**: Prevent duplicate job executions

### Key Architectural Decisions

#### Centralized vs Distributed Scheduling

**Centralized Approach (Simple but Limited)**
```
Single Scheduler Node
│
├── Pros: Simple, no coordination needed
└── Cons: Single point of failure, limited scale
```

**Distributed Approach (Complex but Scalable)**
```
Multiple Scheduler Nodes + Coordination
│
├── Pros: High availability, horizontal scaling
└── Cons: Complex coordination, potential conflicts
```

#### Timing Wheel vs Priority Queue

**Priority Queue (Traditional)**
```java
// O(log n) insertion and removal
PriorityQueue<ScheduledJob> queue = new PriorityQueue<>(
    Comparator.comparing(ScheduledJob::getExecutionTime)
);

// Add job: O(log n)
queue.offer(new ScheduledJob(jobId, executionTime));

// Get next job: O(log n)
ScheduledJob nextJob = queue.poll();
```
**Problems**: 
- O(log n) operations become expensive with millions of jobs
- Memory overhead for large heaps
- Not suitable for high-frequency scheduling

**Timing Wheel (Optimized)**
```java
public class TimingWheel {
    private final int wheelSize = 3600; // 1 hour with 1-second ticks
    private final List<ScheduledJob>[] buckets = new List[wheelSize];
    private int currentTick = 0;
    
    // O(1) insertion
    public void schedule(ScheduledJob job, long delaySeconds) {
        int targetBucket = (currentTick + (int)delaySeconds) % wheelSize;
        buckets[targetBucket].add(job);
    }
    
    // O(1) tick processing
    public List<ScheduledJob> tick() {
        List<ScheduledJob> readyJobs = buckets[currentTick];
        buckets[currentTick] = new ArrayList<>(); // Clear bucket
        currentTick = (currentTick + 1) % wheelSize;
        return readyJobs;
    }
}
```
**Benefits**:
- O(1) insertion and tick processing
- Memory efficient for sparse schedules
- Handles high-frequency scheduling

#### Lease-Based Coordination

**The Split-Brain Problem**
```
Scenario: Network partition splits scheduler cluster
Node A: "I'm the leader" → Schedules Job X
Node B: "I'm the leader" → Schedules Job X
Result: Job X executed twice!
```

**Solution: Lease-Based Leadership**
```java
public class LeaseManager {
    private static final long LEASE_DURATION_MS = 30_000; // 30 seconds
    
    public boolean acquireLease(String partitionKey, String nodeId) {
        // Atomic compare-and-swap operation
        return jdbcTemplate.update(
            "INSERT INTO scheduler_leases (partition_key, node_id, expires_at) " +
            "VALUES (?, ?, ?) ON CONFLICT (partition_key) DO UPDATE SET " +
            "node_id = EXCLUDED.node_id, expires_at = EXCLUDED.expires_at " +
            "WHERE scheduler_leases.expires_at < NOW()",
            partitionKey, nodeId, Instant.now().plusMillis(LEASE_DURATION_MS)
        ) > 0;
    }
    
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void renewLease() {
        for (String partition : ownedPartitions) {
            boolean renewed = jdbcTemplate.update(
                "UPDATE scheduler_leases SET expires_at = ?, heartbeat_at = NOW() " +
                "WHERE partition_key = ? AND node_id = ?",
                Instant.now().plusMillis(LEASE_DURATION_MS),
                partition, nodeId
            ) > 0;
            
            if (!renewed) {
                log.warn("Lost lease for partition: {}", partition);
                ownedPartitions.remove(partition);
            }
        }
    }
}
```

### Message Queue Architecture

#### Why Kafka for Job Scheduling?
1. **Durability**: Messages persisted to disk
2. **Ordering**: Partition-based message ordering
3. **Scalability**: Horizontal scaling with partitions
4. **Reliability**: Replication and fault tolerance

#### Queue Design Patterns
```java
// Producer: Scheduler publishes ready jobs
public class JobScheduler {
    public void publishReadyJob(ScheduledJob job) {
        JobMessage message = new JobMessage(
            job.getId(),
            job.getType(),
            job.getPayload(),
            job.getPriority()
        );
        
        // Use job type as partition key for load balancing
        kafkaTemplate.send("ready-jobs", job.getType(), message);
    }
}

// Consumer: Executor processes jobs
@KafkaListener(topics = "ready-jobs")
public class JobExecutor {
    public void processJob(JobMessage message) {
        try {
            // Execute job logic
            JobResult result = executeJob(message);
            
            // Update job status
            jobService.markCompleted(message.getJobId(), result);
            
        } catch (Exception e) {
            // Handle failure and retry logic
            handleJobFailure(message, e);
        }
    }
    
    private void handleJobFailure(JobMessage message, Exception error) {
        Job job = jobService.getJob(message.getJobId());
        
        if (job.getCurrentRetries() < job.getMaxRetries()) {
            // Schedule retry with exponential backoff
            long delayMs = calculateRetryDelay(job.getCurrentRetries());
            
            RetryMessage retryMessage = new RetryMessage(
                message,
                job.getCurrentRetries() + 1,
                System.currentTimeMillis() + delayMs
            );
            
            kafkaTemplate.send("retry-jobs", retryMessage);
        } else {
            // Move to dead letter queue
            kafkaTemplate.send("dead-letter-jobs", message);
        }
    }
}
```

## 1. High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Dashboard]
        API[API Clients]
        CLI[CLI Tools]
    end
    
    subgraph "API Gateway"
        GATEWAY[API Gateway]
        AUTH[Authentication]
        RATE_LIMIT[Rate Limiting]
    end
    
    subgraph "Job Management Layer"
        JMS[Job Management Service]
        JLS[Job Lifecycle Service]
        JST[Job Status Tracker]
    end
    
    subgraph "Scheduler Cluster"
        JS1[Job Scheduler 1]
        JS2[Job Scheduler 2]
        JS3[Job Scheduler N]
        TW[Timing Wheel]
        CRON[Cron Engine]
        LM[Lease Manager]
    end
    
    subgraph "Message Queue"
        KAFKA[Apache Kafka]
        READY_Q[Ready Queue]
        RETRY_Q[Retry Queue]
        DLQ[Dead Letter Queue]
    end
    
    subgraph "Execution Layer"
        JE1[Job Executor 1]
        JE2[Job Executor 2]
        JE3[Job Executor N]
        JH[Job Handlers]
    end
    
    subgraph "Data Layer"
        POSTGRES[PostgreSQL Cluster]
        REDIS[Redis Cache]
        MONITORING[Monitoring & Metrics]
    end
    
    WEB --> GATEWAY
    API --> GATEWAY
    CLI --> GATEWAY
    
    GATEWAY --> AUTH
    AUTH --> RATE_LIMIT
    RATE_LIMIT --> JMS
    
    JMS --> JLS
    JMS --> JST
    JLS --> JS1
    JLS --> JS2
    JLS --> JS3
    
    JS1 --> TW
    JS1 --> CRON
    JS1 --> LM
    
    TW --> KAFKA
    CRON --> KAFKA
    
    KAFKA --> READY_Q
    KAFKA --> RETRY_Q
    KAFKA --> DLQ
    
    READY_Q --> JE1
    READY_Q --> JE2
    READY_Q --> JE3
    
    JE1 --> JH
    JE2 --> JH
    JE3 --> JH
    
    JMS --> POSTGRES
    JLS --> POSTGRES
    JST --> REDIS
    
    JE1 --> MONITORING
    JS1 --> MONITORING
```

## 2. Job Scheduling Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as API Gateway
    participant JMS as Job Management Service
    participant DB as Database
    participant Scheduler as Job Scheduler
    participant TW as Timing Wheel
    participant Kafka
    participant Executor as Job Executor
    
    Client->>API: Submit Job Request
    API->>JMS: Validate & Process
    JMS->>DB: Store Job Metadata
    DB-->>JMS: Job Saved
    
    JMS->>Scheduler: Schedule Job
    Scheduler->>TW: Add to Timing Wheel
    TW-->>Scheduler: Job Scheduled
    
    Note over TW: Time passes...
    
    TW->>Scheduler: Job Ready (Tick)
    Scheduler->>Kafka: Publish to Ready Queue
    Kafka->>Executor: Consume Job Message
    
    Executor->>DB: Update Status (RUNNING)
    Executor->>Executor: Execute Job Logic
    
    alt Success
        Executor->>DB: Update Status (COMPLETED)
        Executor->>Kafka: Publish Success Event
    else Failure
        Executor->>DB: Update Status (FAILED)
        Executor->>Kafka: Publish to Retry Queue
    end
    
    JMS-->>Client: Job Status Response
```

### Cron Expression Processing

#### Understanding Cron Complexity
Cron expressions seem simple but have many edge cases:

```java
public class CronProcessor {
    // Handle timezone conversions
    public LocalDateTime getNextExecution(String cronExpression, ZoneId timezone) {
        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime now = ZonedDateTime.now(timezone);
        
        // Handle Daylight Saving Time transitions
        ZonedDateTime next = cron.next(now);
        
        // DST "spring forward" - 2 AM becomes 3 AM
        if (next.getHour() == 2 && isDSTTransition(next)) {
            next = next.plusHours(1); // Skip to 3 AM
        }
        
        return next.toLocalDateTime();
    }
    
    // Handle complex expressions like "last Friday of month"
    public boolean isValidCronExpression(String expression) {
        try {
            CronExpression cron = CronExpression.parse(expression);
            
            // Test with multiple dates to catch edge cases
            LocalDateTime testDate = LocalDateTime.now();
            for (int i = 0; i < 100; i++) {
                LocalDateTime next = cron.next(testDate);
                if (next == null) return false;
                testDate = next.plusMinutes(1);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### Cron Expression Examples
```
"0 0 9 * * MON"     - Every Monday at 9 AM
"0 */15 * * * *"    - Every 15 minutes
"0 0 0 1 */3 *"     - First day of every quarter
"0 0 2 * * SUN#2"   - Second Sunday of month at 2 AM
"0 0 0 L * *"       - Last day of every month
```

## 3. Timing Wheel Architecture

### Understanding Timing Wheel Efficiency

The timing wheel is a crucial optimization for handling millions of scheduled jobs:

#### Hierarchical Design Benefits
1. **Memory Efficiency**: Only allocate buckets for time ranges with jobs
2. **O(1) Operations**: Constant time insertion, deletion, and tick processing
3. **Scalability**: Handle millions of jobs without performance degradation

#### Implementation Details
```java
public class HierarchicalTimingWheel {
    private final TimingWheelLevel[] levels;
    
    public HierarchicalTimingWheel() {
        levels = new TimingWheelLevel[] {
            new TimingWheelLevel(3600, 1000),      // Level 1: 1-second ticks, 1 hour
            new TimingWheelLevel(1440, 60000),     // Level 2: 1-minute ticks, 1 day
            new TimingWheelLevel(720, 3600000),    // Level 3: 1-hour ticks, 30 days
            new TimingWheelLevel(365, 86400000)    // Level 4: 1-day ticks, 1 year
        };
    }
    
    public void addJob(ScheduledJob job, long delayMs) {
        // Find appropriate level for the delay
        for (int i = 0; i < levels.length; i++) {
            if (delayMs <= levels[i].getMaxDelay()) {
                levels[i].addJob(job, delayMs);
                return;
            }
        }
        
        // For very long delays, use the highest level
        levels[levels.length - 1].addJob(job, delayMs);
    }
    
    public Set<ScheduledJob> tick() {
        Set<ScheduledJob> readyJobs = new HashSet<>();
        
        // Process each level
        for (TimingWheelLevel level : levels) {
            Set<ScheduledJob> levelJobs = level.tick();
            
            // Jobs from higher levels need to be re-inserted into lower levels
            for (ScheduledJob job : levelJobs) {
                long remainingDelay = job.getExecutionTime() - System.currentTimeMillis();
                
                if (remainingDelay <= 0) {
                    readyJobs.add(job); // Job is ready for execution
                } else {
                    addJob(job, remainingDelay); // Re-insert into appropriate level
                }
            }
        }
        
        return readyJobs;
    }
}
```

```mermaid
graph TD
    subgraph "Hierarchical Timing Wheel"
        L1[Level 1: Seconds<br/>3600 buckets × 1s]
        L2[Level 2: Minutes<br/>1440 buckets × 1m]
        L3[Level 3: Hours<br/>720 buckets × 1h]
        L4[Level 4: Days<br/>365 buckets × 1d]
    end
    
    subgraph "Bucket Structure"
        B1[Bucket 0<br/>Jobs: J1, J5]
        B2[Bucket 1<br/>Jobs: J2, J7]
        B3[Bucket 2<br/>Jobs: J3]
        B4[Bucket N<br/>Jobs: J4, J6]
    end
    
    subgraph "Operations"
        ADD[Add Job<br/>O(1)]
        TICK[Tick Process<br/>O(1)]
        CANCEL[Cancel Job<br/>O(1)]
    end
    
    L1 --> L2
    L2 --> L3
    L3 --> L4
    
    L1 --> B1
    L1 --> B2
    L1 --> B3
    L1 --> B4
    
    ADD --> L1
    TICK --> L1
    CANCEL --> L1
    
    style L1 fill:#e1f5fe
    style ADD fill:#c8e6c9
    style TICK fill:#c8e6c9
    style CANCEL fill:#ffcdd2
```

## 4. Distributed Coordination with Leases

```mermaid
stateDiagram-v2
    [*] --> Acquiring
    Acquiring --> LeaseHeld: Acquire Success
    Acquiring --> Waiting: Acquire Failed
    
    LeaseHeld --> Renewing: Heartbeat Timer
    Renewing --> LeaseHeld: Renew Success
    Renewing --> Lost: Renew Failed
    
    Waiting --> Acquiring: Retry Timer
    Lost --> Acquiring: Retry
    
    LeaseHeld --> Released: Graceful Shutdown
    Released --> [*]
    
    note right of LeaseHeld
        Node processes jobs
        for assigned partitions
    end note
    
    note right of Lost
        Another node takes over
        the partition
    end note
```

## 5. Job Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED
    SCHEDULED --> RUNNING: Execution Started
    SCHEDULED --> CANCELLED: User Cancellation
    SCHEDULED --> PAUSED: User Pause
    
    RUNNING --> COMPLETED: Success
    RUNNING --> FAILED: Error (Max Retries)
    RUNNING --> RETRYING: Error (Retry Available)
    
    RETRYING --> RUNNING: Retry Execution
    RETRYING --> FAILED: Max Retries Reached
    
    PAUSED --> SCHEDULED: User Resume
    PAUSED --> CANCELLED: User Cancellation
    
    COMPLETED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
    
    note right of SCHEDULED
        Job is waiting for
        scheduled execution time
    end note
    
    note right of RETRYING
        Job failed but has
        retries remaining
    end note
```

## 6. Retry Mechanism with Exponential Backoff

```mermaid
graph TD
    A[Job Execution Failed] --> B{Retries < Max?}
    B -->|Yes| C[Calculate Backoff Delay]
    B -->|No| D[Move to Dead Letter Queue]
    
    C --> E[Exponential Backoff<br/>base_delay × 2^retry_count]
    E --> F[Add Jitter<br/>±10% randomization]
    F --> G[Schedule Retry]
    G --> H[Update Job Status<br/>RETRYING]
    H --> I[Wait for Delay]
    I --> J[Execute Job Again]
    
    J --> K{Success?}
    K -->|Yes| L[Mark COMPLETED]
    K -->|No| B
    
    D --> M[Manual Intervention<br/>Required]
    L --> N[End]
    M --> N
    
    style D fill:#ffcdd2
    style L fill:#c8e6c9
    style M fill:#fff3e0
```

## 7. Cron Expression Processing

```mermaid
flowchart TD
    A[Cron Expression<br/>'0 0 9 * * MON'] --> B[Parse Components]
    B --> C[Minute: 0]
    B --> D[Hour: 0]
    B --> E[Day: 9]
    B --> F[Month: *]
    B --> G[Year: *]
    B --> H[DayOfWeek: MON]
    
    C --> I[Validation Engine]
    D --> I
    E --> I
    F --> I
    G --> I
    H --> I
    
    I --> J{Valid?}
    J -->|Yes| K[Calculate Next Execution]
    J -->|No| L[Return Error]
    
    K --> M[Consider Timezone]
    M --> N[Handle DST]
    N --> O[Return Next DateTime]
    
    style J fill:#e3f2fd
    style K fill:#c8e6c9
    style L fill:#ffcdd2
```

## 8. Database Schema Relationships

```mermaid
erDiagram
    JOBS ||--o{ JOB_EXECUTIONS : has
    JOBS ||--o{ SCHEDULER_LEASES : managed_by
    
    JOBS {
        uuid id PK
        string name
        string type
        enum schedule_type
        string schedule_value
        jsonb payload
        enum status
        integer priority
        integer max_retries
        integer current_retries
        integer timeout_seconds
        timestamp created_at
        timestamp scheduled_at
        timestamp next_execution_at
        string created_by
        jsonb tags
    }
    
    JOB_EXECUTIONS {
        uuid id PK
        uuid job_id FK
        string execution_id UK
        enum status
        timestamp started_at
        timestamp completed_at
        bigint duration_ms
        jsonb result
        text error_message
        integer retry_count
        string executor_node
    }
    
    SCHEDULER_LEASES {
        string partition_key PK
        string node_id
        timestamp lease_expires_at
        timestamp heartbeat_at
    }
```

## 9. Message Queue Architecture

```mermaid
graph TB
    subgraph "Kafka Cluster"
        subgraph "Ready Queue Topic"
            RQ_P1[Partition 0]
            RQ_P2[Partition 1]
            RQ_P3[Partition N]
        end
        
        subgraph "Retry Queue Topic"
            RT_P1[Partition 0]
            RT_P2[Partition 1]
            RT_P3[Partition N]
        end
        
        subgraph "Dead Letter Queue Topic"
            DLQ_P1[Partition 0]
            DLQ_P2[Partition 1]
            DLQ_P3[Partition N]
        end
    end
    
    subgraph "Producers"
        SCHEDULER[Job Scheduler]
        RETRY_SVC[Retry Service]
    end
    
    subgraph "Consumers"
        EXECUTOR1[Job Executor 1]
        EXECUTOR2[Job Executor 2]
        EXECUTOR3[Job Executor N]
        DLQ_HANDLER[DLQ Handler]
    end
    
    SCHEDULER --> RQ_P1
    SCHEDULER --> RQ_P2
    SCHEDULER --> RQ_P3
    
    RETRY_SVC --> RT_P1
    RETRY_SVC --> RT_P2
    RETRY_SVC --> RT_P3
    
    RQ_P1 --> EXECUTOR1
    RQ_P2 --> EXECUTOR2
    RQ_P3 --> EXECUTOR3
    
    RT_P1 --> EXECUTOR1
    RT_P2 --> EXECUTOR2
    RT_P3 --> EXECUTOR3
    
    DLQ_P1 --> DLQ_HANDLER
    DLQ_P2 --> DLQ_HANDLER
    DLQ_P3 --> DLQ_HANDLER
```

## 10. Monitoring and Alerting Architecture

```mermaid
graph TB
    subgraph "Metrics Collection"
        APP_METRICS[Application Metrics]
        JOB_METRICS[Job Execution Metrics]
        SYS_METRICS[System Metrics]
        KAFKA_METRICS[Kafka Metrics]
    end
    
    subgraph "Metrics Storage"
        PROMETHEUS[Prometheus]
        INFLUXDB[InfluxDB]
    end
    
    subgraph "Visualization"
        GRAFANA[Grafana Dashboards]
        KIBANA[Kibana Logs]
    end
    
    subgraph "Alerting"
        ALERT_MGR[Alert Manager]
        PAGERDUTY[PagerDuty]
        SLACK[Slack]
        EMAIL[Email]
    end
    
    subgraph "Key Metrics"
        JOBS_SCHEDULED[Jobs Scheduled/sec]
        JOBS_EXECUTED[Jobs Executed/sec]
        EXECUTION_LATENCY[Execution Latency]
        FAILURE_RATE[Failure Rate %]
        QUEUE_DEPTH[Queue Depth]
        LEASE_STATUS[Lease Status]
    end
    
    APP_METRICS --> PROMETHEUS
    JOB_METRICS --> PROMETHEUS
    SYS_METRICS --> PROMETHEUS
    KAFKA_METRICS --> INFLUXDB
    
    PROMETHEUS --> GRAFANA
    INFLUXDB --> GRAFANA
    PROMETHEUS --> KIBANA
    
    PROMETHEUS --> ALERT_MGR
    ALERT_MGR --> PAGERDUTY
    ALERT_MGR --> SLACK
    ALERT_MGR --> EMAIL
    
    JOBS_SCHEDULED --> APP_METRICS
    JOBS_EXECUTED --> JOB_METRICS
    EXECUTION_LATENCY --> JOB_METRICS
    FAILURE_RATE --> JOB_METRICS
    QUEUE_DEPTH --> KAFKA_METRICS
    LEASE_STATUS --> SYS_METRICS
```

## 11. Auto-Scaling Architecture

```mermaid
graph TB
    subgraph "Metrics Sources"
        CPU[CPU Usage]
        MEMORY[Memory Usage]
        QUEUE[Queue Depth]
        LATENCY[Execution Latency]
    end
    
    subgraph "Auto Scaler"
        COLLECTOR[Metrics Collector]
        ANALYZER[Scale Analyzer]
        DECISION[Scale Decision Engine]
    end
    
    subgraph "Orchestration"
        K8S[Kubernetes HPA]
        DOCKER[Docker Swarm]
        AWS_ASG[AWS Auto Scaling]
    end
    
    subgraph "Scale Actions"
        SCALE_OUT[Scale Out<br/>Add Instances]
        SCALE_IN[Scale In<br/>Remove Instances]
        REBALANCE[Rebalance<br/>Redistribute Load]
    end
    
    CPU --> COLLECTOR
    MEMORY --> COLLECTOR
    QUEUE --> COLLECTOR
    LATENCY --> COLLECTOR
    
    COLLECTOR --> ANALYZER
    ANALYZER --> DECISION
    
    DECISION --> K8S
    DECISION --> DOCKER
    DECISION --> AWS_ASG
    
    K8S --> SCALE_OUT
    K8S --> SCALE_IN
    DOCKER --> SCALE_OUT
    DOCKER --> SCALE_IN
    AWS_ASG --> REBALANCE
    
    style SCALE_OUT fill:#c8e6c9
    style SCALE_IN fill:#ffcdd2
    style REBALANCE fill:#fff3e0
```

## 12. Disaster Recovery Architecture

```mermaid
graph TB
    subgraph "Primary Region (us-east-1)"
        PRIMARY_SCHEDULER[Scheduler Cluster]
        PRIMARY_EXECUTOR[Executor Cluster]
        PRIMARY_DB[PostgreSQL Primary]
        PRIMARY_KAFKA[Kafka Cluster]
    end
    
    subgraph "Secondary Region (us-west-2)"
        SECONDARY_SCHEDULER[Scheduler Cluster]
        SECONDARY_EXECUTOR[Executor Cluster]
        SECONDARY_DB[PostgreSQL Replica]
        SECONDARY_KAFKA[Kafka Mirror]
    end
    
    subgraph "DR Region (eu-west-1)"
        DR_SCHEDULER[Scheduler Cluster]
        DR_EXECUTOR[Executor Cluster]
        DR_DB[PostgreSQL Standby]
        DR_KAFKA[Kafka Standby]
    end
    
    subgraph "Monitoring & Failover"
        HEALTH_CHECK[Health Monitoring]
        FAILOVER_MGR[Failover Manager]
        DNS_FAILOVER[DNS Failover]
    end
    
    PRIMARY_DB -.->|Streaming Replication| SECONDARY_DB
    PRIMARY_DB -.->|Backup & Restore| DR_DB
    PRIMARY_KAFKA -.->|Mirror Maker| SECONDARY_KAFKA
    PRIMARY_KAFKA -.->|Backup| DR_KAFKA
    
    HEALTH_CHECK --> PRIMARY_SCHEDULER
    HEALTH_CHECK --> SECONDARY_SCHEDULER
    HEALTH_CHECK --> DR_SCHEDULER
    
    HEALTH_CHECK --> FAILOVER_MGR
    FAILOVER_MGR --> DNS_FAILOVER
    
    style PRIMARY_SCHEDULER fill:#c8e6c9
    style SECONDARY_SCHEDULER fill:#fff3e0
    style DR_SCHEDULER fill:#ffcdd2
```