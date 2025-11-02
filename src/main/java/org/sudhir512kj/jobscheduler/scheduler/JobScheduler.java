package org.sudhir512kj.jobscheduler.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sudhir512kj.jobscheduler.model.Job;
import org.sudhir512kj.jobscheduler.service.CronService;
import org.sudhir512kj.jobscheduler.service.LeaseManagerService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {
    private final TimingWheel timingWheel;
    private final CronService cronService;
    private final LeaseManagerService leaseManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String READY_JOBS_TOPIC = "job-ready-queue";
    private static final long MAX_JITTER_MS = 5000; // 5 seconds
    
    public void scheduleJob(Job job) {
        log.info("Scheduling job: {} of type: {}", job.getId(), job.getType());
        
        try {
            ScheduledJob scheduledJob = createScheduledJob(job);
            long delayMs = calculateDelay(job);
            
            // Add jitter to prevent thundering herd
            delayMs = addJitter(delayMs, job.getPriority());
            
            timingWheel.addJob(scheduledJob, delayMs);
            
            log.debug("Job {} scheduled with delay: {} ms", job.getId(), delayMs);
        } catch (Exception e) {
            log.error("Failed to schedule job: {}", job.getId(), e);
            throw new RuntimeException("Job scheduling failed", e);
        }
    }
    
    public void rescheduleJob(Job job) {
        log.info("Rescheduling job: {}", job.getId());
        
        // Cancel existing schedule
        cancelScheduledJob(job.getId());
        
        // Schedule again
        scheduleJob(job);
    }
    
    public void cancelScheduledJob(UUID jobId) {
        log.info("Cancelling scheduled job: {}", jobId);
        
        boolean cancelled = timingWheel.cancelJob(jobId.toString());
        if (cancelled) {
            log.debug("Job {} cancelled from timing wheel", jobId);
        } else {
            log.warn("Job {} not found in timing wheel", jobId);
        }
    }
    
    @Scheduled(fixedDelay = 1000) // Tick every second
    public void processTick() {
        try {
            // Check if this node has the lease to process jobs
            if (!leaseManager.hasLease("scheduler-main")) {
                return;
            }
            
            Set<ScheduledJob> readyJobs = timingWheel.tick();
            
            for (ScheduledJob job : readyJobs) {
                try {
                    publishJobToQueue(job);
                } catch (Exception e) {
                    log.error("Failed to publish job to queue: {}", job.getJobId(), e);
                    // Could implement retry logic here
                }
            }
            
            if (!readyJobs.isEmpty()) {
                log.info("Processed {} ready jobs in this tick", readyJobs.size());
            }
            
        } catch (Exception e) {
            log.error("Error during scheduler tick processing", e);
        }
    }
    
    private ScheduledJob createScheduledJob(Job job) {
        return new ScheduledJob(
            job.getId(),
            job.getType(),
            job.getPayload(),
            job.getPriority(),
            job.getScheduledAt()
        );
    }
    
    private long calculateDelay(Job job) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = job.getScheduledAt();
        
        if (scheduledTime.isBefore(now)) {
            return 0; // Execute immediately
        }
        
        return scheduledTime.toEpochSecond(ZoneOffset.UTC) - now.toEpochSecond(ZoneOffset.UTC);
    }
    
    private long addJitter(long delayMs, int priority) {
        if (priority <= 3) { // High priority jobs get less jitter
            return delayMs + ThreadLocalRandom.current().nextLong(0, 1000);
        } else {
            return delayMs + ThreadLocalRandom.current().nextLong(0, MAX_JITTER_MS);
        }
    }
    
    private void publishJobToQueue(ScheduledJob job) {
        try {
            kafkaTemplate.send(READY_JOBS_TOPIC, job.getJobId(), job);
            log.debug("Published job {} to ready queue", job.getJobId());
        } catch (Exception e) {
            log.error("Failed to publish job {} to Kafka", job.getJobId(), e);
            throw e;
        }
    }
    
    public int getScheduledJobCount() {
        return timingWheel.getJobCount();
    }
}