package org.sudhir512kj.jobscheduler.executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.sudhir512kj.jobscheduler.model.Job;
import org.sudhir512kj.jobscheduler.model.JobExecution;
import org.sudhir512kj.jobscheduler.repository.JobRepository;
import org.sudhir512kj.jobscheduler.scheduler.ScheduledJob;
import org.sudhir512kj.jobscheduler.service.JobExecutionService;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobExecutor {
    private final JobRepository jobRepository;
    private final JobExecutionService executionService;
    private final ThreadPoolExecutor executorPool;
    private final Map<String, JobHandler> jobHandlers = new ConcurrentHashMap<>();
    
    @KafkaListener(topics = "job-ready-queue", groupId = "job-executor-group")
    public void executeJob(ScheduledJob scheduledJob) {
        log.info("Received job for execution: {}", scheduledJob.getJobId());
        
        try {
            // Get full job details from database
            Job job = jobRepository.findById(scheduledJob.getJobUuid())
                .orElseThrow(() -> new RuntimeException("Job not found: " + scheduledJob.getJobId()));
            
            // Check if job is still in valid state for execution
            if (!isExecutable(job)) {
                log.warn("Job {} is not in executable state: {}", job.getId(), job.getStatus());
                return;
            }
            
            // Execute job asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    executionService.executeJob(job);
                } catch (Exception e) {
                    log.error("Job execution failed: {}", job.getId(), e);
                }
            }, executorPool);
            
        } catch (Exception e) {
            log.error("Failed to process job execution request: {}", scheduledJob.getJobId(), e);
        }
    }
    
    public void registerJobHandler(String jobType, JobHandler handler) {
        jobHandlers.put(jobType, handler);
        log.info("Registered job handler for type: {}", jobType);
    }
    
    public JobHandler getJobHandler(String jobType) {
        JobHandler handler = jobHandlers.get(jobType);
        if (handler == null) {
            handler = jobHandlers.get("default");
        }
        return handler;
    }
    
    private boolean isExecutable(Job job) {
        return job.getStatus() == Job.JobStatus.SCHEDULED || 
               job.getStatus() == Job.JobStatus.RETRYING;
    }
    
    public int getActiveJobCount() {
        return executorPool.getActiveCount();
    }
    
    public int getQueuedJobCount() {
        return executorPool.getQueue().size();
    }
}