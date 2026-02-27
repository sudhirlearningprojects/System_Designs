# Job Scheduler - Complete Coding Guide

## System Design Overview

**Problem**: Schedule and execute jobs at specific times

**Core Features**:
1. Schedule one-time and recurring jobs
2. Execute jobs at scheduled time
3. Retry failed jobs
4. Distributed coordination

## SOLID Principles

- **SRP**: Job, Scheduler, Executor separate
- **OCP**: Add new job types without modifying
- **Strategy**: Different scheduling strategies (cron, interval)

## Design Patterns

1. **Strategy Pattern**: Scheduling strategies
2. **Observer Pattern**: Job status notifications
3. **Command Pattern**: Job execution

## Complete Implementation

```java
import java.util.*;
import java.time.*;
import java.util.concurrent.*;

enum JobStatus { SCHEDULED, RUNNING, COMPLETED, FAILED }
enum ScheduleType { ONE_TIME, RECURRING }

interface Job {
    void execute();
    String getId();
}

class PrintJob implements Job {
    private String id, message;
    
    PrintJob(String id, String message) {
        this.id = id;
        this.message = message;
    }
    
    public void execute() {
        System.out.println("  [JOB " + id + "] Executing: " + message);
    }
    
    public String getId() { return id; }
}

class ScheduledJob {
    String id;
    Job job;
    LocalDateTime scheduledTime;
    ScheduleType type;
    Duration interval;
    JobStatus status;
    int retryCount = 0;
    
    ScheduledJob(Job job, LocalDateTime scheduledTime, ScheduleType type, Duration interval) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.job = job;
        this.scheduledTime = scheduledTime;
        this.type = type;
        this.interval = interval;
        this.status = JobStatus.SCHEDULED;
    }
}

class JobScheduler {
    private PriorityQueue<ScheduledJob> jobQueue;
    private Map<String, ScheduledJob> jobs = new HashMap<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    private volatile boolean running = true;
    
    public JobScheduler() {
        jobQueue = new PriorityQueue<>(
            Comparator.comparing(j -> j.scheduledTime)
        );
    }
    
    public String scheduleOneTime(Job job, LocalDateTime scheduledTime) {
        ScheduledJob sJob = new ScheduledJob(job, scheduledTime, ScheduleType.ONE_TIME, null);
        jobQueue.offer(sJob);
        jobs.put(sJob.id, sJob);
        
        System.out.println("Scheduled one-time job " + sJob.id + " at " + scheduledTime);
        return sJob.id;
    }
    
    public String scheduleRecurring(Job job, LocalDateTime startTime, Duration interval) {
        ScheduledJob sJob = new ScheduledJob(job, startTime, ScheduleType.RECURRING, interval);
        jobQueue.offer(sJob);
        jobs.put(sJob.id, sJob);
        
        System.out.println("Scheduled recurring job " + sJob.id + " every " + interval.toMinutes() + " minutes");
        return sJob.id;
    }
    
    public void start() {
        executor.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            
            while (!jobQueue.isEmpty() && 
                   jobQueue.peek().scheduledTime.isBefore(now.plusSeconds(1))) {
                
                ScheduledJob sJob = jobQueue.poll();
                executeJob(sJob);
                
                // Reschedule if recurring
                if (sJob.type == ScheduleType.RECURRING && sJob.status == JobStatus.COMPLETED) {
                    sJob.scheduledTime = sJob.scheduledTime.plus(sJob.interval);
                    sJob.status = JobStatus.SCHEDULED;
                    jobQueue.offer(sJob);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        System.out.println("Job scheduler started\n");
    }
    
    private void executeJob(ScheduledJob sJob) {
        System.out.println("\n=== Executing Job ===");
        System.out.println("Job ID: " + sJob.id);
        System.out.println("Scheduled: " + sJob.scheduledTime);
        
        sJob.status = JobStatus.RUNNING;
        
        try {
            sJob.job.execute();
            sJob.status = JobStatus.COMPLETED;
            System.out.println("  ✓ Job completed");
        } catch (Exception e) {
            sJob.retryCount++;
            System.out.println("  ✗ Job failed (attempt " + sJob.retryCount + ")");
            
            if (sJob.retryCount < 3) {
                sJob.scheduledTime = LocalDateTime.now().plusMinutes(1);
                sJob.status = JobStatus.SCHEDULED;
                jobQueue.offer(sJob);
                System.out.println("  ⏳ Rescheduled for retry");
            } else {
                sJob.status = JobStatus.FAILED;
                System.out.println("  ✗ Job failed permanently");
            }
        }
    }
    
    public void cancelJob(String jobId) {
        ScheduledJob sJob = jobs.get(jobId);
        if (sJob != null) {
            jobQueue.remove(sJob);
            jobs.remove(jobId);
            System.out.println("Cancelled job: " + jobId);
        }
    }
    
    public void shutdown() {
        running = false;
        executor.shutdown();
    }
}

public class JobSchedulerDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Job Scheduler System ===\n");
        
        JobScheduler scheduler = new JobScheduler();
        
        // Schedule one-time jobs
        LocalDateTime now = LocalDateTime.now();
        scheduler.scheduleOneTime(
            new PrintJob("J1", "Send welcome email"),
            now.plusSeconds(2)
        );
        
        scheduler.scheduleOneTime(
            new PrintJob("J2", "Generate report"),
            now.plusSeconds(5)
        );
        
        // Schedule recurring job
        scheduler.scheduleRecurring(
            new PrintJob("J3", "Cleanup temp files"),
            now.plusSeconds(3),
            Duration.ofSeconds(4)
        );
        
        // Start scheduler
        scheduler.start();
        
        // Run for 15 seconds
        Thread.sleep(15000);
        
        scheduler.shutdown();
        System.out.println("\n=== Scheduler stopped ===");
    }
}
```

## Key Concepts

**Timing Wheel**:
- Efficient for large number of timers
- O(1) insert/delete
- Hierarchical wheels for different granularities

**Distributed Coordination**:
- Use ZooKeeper or etcd for leader election
- Lease-based job ownership
- Prevent duplicate execution

**Fault Tolerance**:
- Retry with exponential backoff
- Dead letter queue for failed jobs
- Checkpoint for long-running jobs

## Interview Questions

**Q: Handle millions of jobs?**
A: Timing wheel, partitioning by time bucket, distributed workers

**Q: Exactly-once execution?**
A: Distributed locks, idempotency, lease-based coordination

**Q: Cron expression parsing?**
A: Parse to next execution time, use Quartz library

**Q: Job dependencies?**
A: DAG execution, topological sort, wait for dependencies

Run: https://www.jdoodle.com/online-java-compiler
