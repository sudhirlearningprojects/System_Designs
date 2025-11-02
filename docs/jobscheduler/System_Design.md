# Distributed Job Scheduler System Design

## Table of Contents
1. [System Overview](#system-overview)
2. [High-Level Design (HLD)](#high-level-design-hld)
3. [Low-Level Design (LLD)](#low-level-design-lld)
4. [Functional Requirements](#functional-requirements)
5. [Non-Functional Requirements](#non-functional-requirements)
6. [Data Model](#data-model)
7. [System Components](#system-components)
8. [Critical Design Decisions](#critical-design-decisions)
9. [Fault Tolerance & Reliability](#fault-tolerance--reliability)
10. [Scalability Strategies](#scalability-strategies)

## System Overview

A distributed job scheduling system that allows users to schedule one-off or recurring tasks with high reliability, scalability, and fault tolerance. The system handles millions of jobs with precise timing and ensures no job is missed or executed multiple times.

### Key Features
- One-off and recurring job scheduling
- Cron-based and interval-based scheduling
- Job pause/resume/cancel functionality
- Fault-tolerant execution with retries
- Horizontal scalability
- Real-time job status tracking
- Dead letter queue for failed jobs

## High-Level Design (HLD)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                 API Gateway                                     │
│            (Authentication, Rate Limiting)                      │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Job Management Service                              │
├─────────────────────┼───────────────────────────────────────────┤
│ Job Submitter │ Job Metadata │ Job Status │ Job Lifecycle Mgmt │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                Job Scheduler Cluster                            │
├─────────────────────┼───────────────────────────────────────────┤
│ Timing Wheel │ Cron Engine │ Priority Queue │ Lease Manager    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Message Queue (Kafka)                              │
├─────────────────────┼───────────────────────────────────────────┤
│ Ready Queue │ Retry Queue │ Dead Letter │ Status Updates       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│Job Executor  │ │Job Store│ │ Monitoring  │
│   Cluster    │ │(Database)│ │ & Logging   │
│              │ │         │ │             │
└──────────────┘ └─────────┘ └─────────────┘
```

## Low-Level Design (LLD)

### Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Job Scheduler Core                           │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│ Timing Wheel    │ Cron Scheduler  │ Priority Queue  │ Lease Mgr │
├─────────────────┼─────────────────┼─────────────────┼───────────┤
│• O(1) Insert    │• Cron Parsing   │• Min Heap       │• ZooKeeper│
│• O(1) Tick      │• Next Execution │• Priority Based │• Heartbeat│
│• Hierarchical   │• DST Handling   │• Time Ordered   │• Failover │
└─────────────────┴─────────────────┴─────────────────┴───────────┘
                              │
┌─────────────────────────────▼─────────────────────────────────────┐
│                   Job Execution Engine                           │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│ Job Dispatcher  │ Executor Pool   │ Retry Manager   │ DLQ Handler│
├─────────────────┼─────────────────┼─────────────────┼───────────┤
│• Load Balancing │• Thread Pool    │• Exp Backoff    │• Manual   │
│• Job Routing    │• Async Exec     │• Max Retries    │• Recovery │
│• Health Check   │• Resource Mgmt  │• Failure Types  │• Analysis │
└─────────────────┴─────────────────┴─────────────────┴───────────┘
```

## Functional Requirements

### 1. Job Submission
Users can submit jobs with:
- **Job ID**: Unique identifier (UUID)
- **Job Type**: email, data_processing, cleanup, custom
- **Schedule**: One-time, cron expression, or interval
- **Payload**: JSON data for job execution
- **Metadata**: Priority, max retries, timeout, tags

### 2. Job Execution
- Execute jobs at precise scheduled times
- Handle different job types with pluggable executors
- Support for long-running and short-lived jobs
- Resource allocation and management

### 3. Job Management
- **Cancel**: Stop scheduled job before execution
- **Pause**: Temporarily disable recurring jobs
- **Resume**: Re-enable paused jobs
- **Update**: Modify job parameters (limited)

### 4. Job Status Tracking
- **SCHEDULED**: Job is scheduled for future execution
- **RUNNING**: Job is currently executing
- **COMPLETED**: Job finished successfully
- **FAILED**: Job execution failed
- **CANCELLED**: Job was cancelled by user
- **PAUSED**: Recurring job is temporarily disabled
- **RETRYING**: Job is being retried after failure

## Non-Functional Requirements

### 1. Scalability
- Handle millions of scheduled jobs
- Support 100K+ job executions per second
- Horizontal scaling of all components
- Auto-scaling based on load

### 2. Reliability & Fault Tolerance
- 99.99% availability (52.6 minutes downtime/year)
- No job loss during system failures
- Exactly-once execution guarantee
- Automatic failover and recovery

### 3. Low Latency
- Job execution within 1 second of scheduled time
- Sub-second job submission response
- Real-time status updates

### 4. Durability
- Persistent job storage with replication
- Transaction log for job state changes
- Backup and recovery mechanisms

## Data Model

### Job Entity
```sql
CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    schedule_type VARCHAR(20) NOT NULL, -- ONCE, CRON, INTERVAL
    schedule_value TEXT NOT NULL, -- cron expression or interval
    payload JSONB,
    status VARCHAR(20) NOT NULL,
    priority INTEGER DEFAULT 5,
    max_retries INTEGER DEFAULT 3,
    current_retries INTEGER DEFAULT 0,
    timeout_seconds INTEGER DEFAULT 300,
    created_at TIMESTAMP DEFAULT NOW(),
    scheduled_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    next_execution_at TIMESTAMP,
    last_execution_at TIMESTAMP,
    created_by VARCHAR(255),
    tags JSONB,
    
    INDEX idx_status_scheduled_at (status, scheduled_at),
    INDEX idx_next_execution (next_execution_at),
    INDEX idx_type_status (type, status)
);
```

### Job Execution Log
```sql
CREATE TABLE job_executions (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs(id),
    execution_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    result JSONB,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    executor_node VARCHAR(255),
    
    INDEX idx_job_id_started_at (job_id, started_at),
    INDEX idx_status_completed_at (status, completed_at)
);
```

### Scheduler Lease
```sql
CREATE TABLE scheduler_leases (
    partition_key VARCHAR(255) PRIMARY KEY,
    node_id VARCHAR(255) NOT NULL,
    lease_expires_at TIMESTAMP NOT NULL,
    heartbeat_at TIMESTAMP NOT NULL,
    
    INDEX idx_lease_expires (lease_expires_at)
);
```

## System Components

### 1. Job Management Service
```java
@Service
public class JobManagementService {
    public JobResponse submitJob(JobRequest request);
    public void cancelJob(UUID jobId);
    public void pauseJob(UUID jobId);
    public void resumeJob(UUID jobId);
    public JobStatus getJobStatus(UUID jobId);
    public List<JobExecution> getJobHistory(UUID jobId);
}
```

### 2. Job Scheduler
```java
@Component
public class JobScheduler {
    private final TimingWheel timingWheel;
    private final CronScheduler cronScheduler;
    private final PriorityQueue<ScheduledJob> priorityQueue;
    
    public void scheduleJob(Job job);
    public void rescheduleJob(Job job);
    public void cancelScheduledJob(UUID jobId);
}
```

### 3. Job Executor
```java
@Component
public class JobExecutor {
    private final Map<String, JobHandler> jobHandlers;
    private final ThreadPoolExecutor executorPool;
    
    public CompletableFuture<JobResult> executeJob(Job job);
    public void registerJobHandler(String jobType, JobHandler handler);
}
```

### 4. Timing Wheel Implementation
```java
public class TimingWheel {
    private final int wheelSize;
    private final long tickDuration;
    private final TimingWheelBucket[] buckets;
    private volatile long currentTick;
    
    public void addJob(ScheduledJob job, long delayMs);
    public Set<ScheduledJob> tick();
    public boolean cancelJob(UUID jobId);
}
```

## Critical Design Decisions

### 1. Scheduling Mechanisms

#### Timing Wheel for Short-term Scheduling
```
Hierarchical Timing Wheel:
- Level 1: 1-second ticks, 3600 buckets (1 hour)
- Level 2: 1-minute ticks, 1440 buckets (1 day)  
- Level 3: 1-hour ticks, 720 buckets (30 days)
- Level 4: 1-day ticks, 365 buckets (1 year)

Benefits:
- O(1) insertion and deletion
- O(1) tick processing
- Memory efficient for sparse schedules
```

#### Cron Scheduler for Recurring Jobs
```java
public class CronScheduler {
    public LocalDateTime getNextExecution(String cronExpression, LocalDateTime from) {
        CronExpression cron = CronExpression.parse(cronExpression);
        return cron.next(from);
    }
    
    public boolean isValidCronExpression(String expression) {
        try {
            CronExpression.parse(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 2. Distributed Coordination

#### Lease-based Partitioning
```java
@Component
public class LeaseManager {
    private static final long LEASE_DURATION_MS = 30_000; // 30 seconds
    
    public boolean acquireLease(String partitionKey, String nodeId) {
        return jdbcTemplate.update(
            "INSERT INTO scheduler_leases (partition_key, node_id, lease_expires_at, heartbeat_at) " +
            "VALUES (?, ?, ?, ?) ON CONFLICT (partition_key) DO UPDATE SET " +
            "node_id = EXCLUDED.node_id, lease_expires_at = EXCLUDED.lease_expires_at " +
            "WHERE scheduler_leases.lease_expires_at < NOW()",
            partitionKey, nodeId, 
            Timestamp.from(Instant.now().plusMillis(LEASE_DURATION_MS)),
            Timestamp.from(Instant.now())
        ) > 0;
    }
}
```

### 3. Job Pause/Resume Implementation

#### Status-based Control
```java
public class JobLifecycleManager {
    public void pauseJob(UUID jobId) {
        // Update job status to PAUSED
        jobRepository.updateStatus(jobId, JobStatus.PAUSED);
        
        // Remove from active scheduling
        scheduler.cancelScheduledJob(jobId);
        
        // Cancel any queued executions
        messageQueue.cancelPendingMessages(jobId);
    }
    
    public void resumeJob(UUID jobId) {
        Job job = jobRepository.findById(jobId);
        if (job.getStatus() == JobStatus.PAUSED) {
            job.setStatus(JobStatus.SCHEDULED);
            jobRepository.save(job);
            
            // Reschedule based on job type
            if (job.getScheduleType() == ScheduleType.CRON) {
                LocalDateTime nextExecution = cronScheduler.getNextExecution(
                    job.getScheduleValue(), LocalDateTime.now());
                job.setNextExecutionAt(nextExecution);
                scheduler.scheduleJob(job);
            }
        }
    }
}
```

### 4. Thundering Herd Prevention

#### Time Jittering and Load Spreading
```java
public class LoadBalancer {
    private static final long MAX_JITTER_MS = 5000; // 5 seconds
    
    public long addJitter(long scheduledTime, int priority) {
        if (priority <= 3) { // High priority jobs get less jitter
            return scheduledTime + ThreadLocalRandom.current().nextLong(0, 1000);
        } else {
            return scheduledTime + ThreadLocalRandom.current().nextLong(0, MAX_JITTER_MS);
        }
    }
    
    public void distributeLoad(List<Job> jobs) {
        // Spread jobs across multiple time slots
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            long offset = (i * 100) % 60000; // Spread over 1 minute
            job.setScheduledAt(new Timestamp(baseTime + offset));
        }
    }
}
```

## Fault Tolerance & Reliability

### 1. Exactly-Once Execution
```java
@Transactional
public class JobExecutionService {
    public void executeJob(Job job) {
        String executionId = generateExecutionId(job);
        
        // Check if already executed (idempotency)
        if (executionLogRepository.existsByExecutionId(executionId)) {
            log.info("Job {} already executed with execution ID {}", job.getId(), executionId);
            return;
        }
        
        // Create execution log entry
        JobExecution execution = new JobExecution();
        execution.setJobId(job.getId());
        execution.setExecutionId(executionId);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        executionLogRepository.save(execution);
        
        try {
            JobResult result = jobExecutor.executeJob(job);
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setResult(result.getData());
        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            scheduleRetry(job, execution);
        } finally {
            execution.setCompletedAt(LocalDateTime.now());
            executionLogRepository.save(execution);
        }
    }
}
```

### 2. Failure Recovery
```java
public class FailureRecoveryService {
    @Scheduled(fixedDelay = 60000) // Every minute
    public void recoverOrphanedJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        
        // Find jobs stuck in RUNNING state
        List<Job> orphanedJobs = jobRepository.findByStatusAndStartedAtBefore(
            JobStatus.RUNNING, cutoff);
        
        for (Job job : orphanedJobs) {
            log.warn("Recovering orphaned job: {}", job.getId());
            job.setStatus(JobStatus.FAILED);
            scheduleRetry(job);
        }
    }
    
    private void scheduleRetry(Job job) {
        if (job.getCurrentRetries() < job.getMaxRetries()) {
            job.setCurrentRetries(job.getCurrentRetries() + 1);
            job.setStatus(JobStatus.SCHEDULED);
            
            // Exponential backoff
            long delayMs = calculateRetryDelay(job.getCurrentRetries());
            job.setScheduledAt(Timestamp.from(Instant.now().plusMillis(delayMs)));
            
            jobRepository.save(job);
            scheduler.scheduleJob(job);
        } else {
            // Move to dead letter queue
            job.setStatus(JobStatus.FAILED);
            deadLetterQueueService.addJob(job);
        }
    }
}
```

### 3. Database Consistency
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public class JobStateManager {
    public boolean transitionJobState(UUID jobId, JobStatus from, JobStatus to) {
        int updated = jdbcTemplate.update(
            "UPDATE jobs SET status = ?, updated_at = NOW() " +
            "WHERE id = ? AND status = ?",
            to.name(), jobId, from.name()
        );
        
        if (updated == 1) {
            publishStatusChangeEvent(jobId, from, to);
            return true;
        }
        return false;
    }
}
```

## Scalability Strategies

### 1. Horizontal Partitioning
```java
public class JobPartitioner {
    private static final int PARTITION_COUNT = 1024;
    
    public String getPartition(UUID jobId) {
        int hash = jobId.hashCode();
        int partition = Math.abs(hash) % PARTITION_COUNT;
        return String.format("partition_%04d", partition);
    }
    
    public List<String> getPartitionsForNode(String nodeId, int totalNodes) {
        List<String> partitions = new ArrayList<>();
        int partitionsPerNode = PARTITION_COUNT / totalNodes;
        int nodeIndex = getNodeIndex(nodeId);
        
        for (int i = 0; i < partitionsPerNode; i++) {
            int partitionIndex = (nodeIndex * partitionsPerNode + i) % PARTITION_COUNT;
            partitions.add(String.format("partition_%04d", partitionIndex));
        }
        return partitions;
    }
}
```

### 2. Auto-scaling Configuration
```java
@Component
public class AutoScaler {
    private final CloudWatchMetrics metrics;
    private final KubernetesClient k8sClient;
    
    @Scheduled(fixedDelay = 30000)
    public void checkScalingNeeds() {
        double avgCpuUsage = metrics.getAverageCpuUsage();
        int queueDepth = metrics.getJobQueueDepth();
        int currentReplicas = k8sClient.getCurrentReplicas("job-scheduler");
        
        if (avgCpuUsage > 80 || queueDepth > 10000) {
            int targetReplicas = Math.min(currentReplicas * 2, 50);
            k8sClient.scaleDeployment("job-scheduler", targetReplicas);
        } else if (avgCpuUsage < 30 && queueDepth < 1000) {
            int targetReplicas = Math.max(currentReplicas / 2, 3);
            k8sClient.scaleDeployment("job-scheduler", targetReplicas);
        }
    }
}
```

### 3. Message Queue Optimization
```java
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, JobMessage> jobProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Performance settings
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

## Interview Questions & Solutions

### 1. How do you ensure jobs are not missed if the Scheduler node crashes?

**Solution**: 
- Use lease-based partitioning with ZooKeeper/etcd
- Each scheduler node holds leases for specific partitions
- Lease expires in 30 seconds with heartbeat every 10 seconds
- Other nodes can take over expired leases
- Jobs are persisted in database, not in-memory

### 2. How do you handle pause/resume for jobs already in the execution queue?

**Solution**:
- Implement job status checks at execution time
- Executor checks job status before execution
- If job is PAUSED, skip execution and don't reschedule
- For recurring jobs, calculate next execution time when resumed
- Use message queue features to cancel/delay messages

### 3. How do you manage the thundering herd problem?

**Solution**:
- Add random jitter (0-5 seconds) to scheduled times
- Implement priority-based jitter (high priority gets less jitter)
- Use multiple execution queues with load balancing
- Spread identical scheduled times across a time window
- Implement rate limiting at the executor level

### 4. Message Broker Choice: Kafka vs RabbitMQ vs SQS?

**Choice: Apache Kafka**

**Reasons**:
- **High Throughput**: Handles millions of messages/second
- **Durability**: Messages persisted to disk with replication
- **Ordering**: Partition-based ordering guarantees
- **Scalability**: Horizontal scaling with partitions
- **Retention**: Configurable message retention for replay
- **Exactly-once**: Idempotent producers and transactional consumers

**Trade-offs**:
- More complex setup than SQS
- Higher resource requirements than RabbitMQ
- Learning curve for operations team

This design provides a robust, scalable, and fault-tolerant distributed job scheduling system that can handle millions of jobs with precise timing and reliability guarantees.