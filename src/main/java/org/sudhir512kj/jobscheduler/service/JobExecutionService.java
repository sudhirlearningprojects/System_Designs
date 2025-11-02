package org.sudhir512kj.jobscheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.jobscheduler.executor.JobExecutor;
import org.sudhir512kj.jobscheduler.executor.JobHandler;
import org.sudhir512kj.jobscheduler.executor.JobResult;
import org.sudhir512kj.jobscheduler.model.Job;
import org.sudhir512kj.jobscheduler.model.JobExecution;
import org.sudhir512kj.jobscheduler.repository.JobExecutionRepository;
import org.sudhir512kj.jobscheduler.repository.JobRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {
    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobExecutor jobExecutor;
    private final RetryService retryService;
    
    @Transactional
    public void executeJob(Job job) {
        String executionId = generateExecutionId(job);
        
        log.info("Starting execution of job: {} with execution ID: {}", job.getId(), executionId);
        
        // Check if already executed (idempotency)
        if (executionRepository.existsByExecutionId(executionId)) {
            log.info("Job {} already executed with execution ID {}", job.getId(), executionId);
            return;
        }
        
        // Create execution log entry
        JobExecution execution = new JobExecution();
        execution.setJobId(job.getId());
        execution.setExecutionId(executionId);
        execution.setStatus(JobExecution.ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution.setRetryCount(job.getCurrentRetries());
        execution.setExecutorNode(getExecutorNodeId());
        execution = executionRepository.save(execution);
        
        // Update job status
        job.setStatus(Job.JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);
        
        try {
            // Get appropriate job handler
            JobHandler handler = jobExecutor.getJobHandler(job.getType());
            if (handler == null) {
                throw new RuntimeException("No handler found for job type: " + job.getType());
            }
            
            // Execute the job
            JobResult result = handler.execute(job);
            
            // Handle execution result
            if (result.isSuccess()) {
                handleSuccessfulExecution(job, execution, result);
            } else {
                handleFailedExecution(job, execution, result);
            }
            
        } catch (Exception e) {
            log.error("Job execution failed: {}", job.getId(), e);
            handleFailedExecution(job, execution, JobResult.failure(e.getMessage(), e));
        }
    }
    
    private void handleSuccessfulExecution(Job job, JobExecution execution, JobResult result) {
        LocalDateTime completedAt = LocalDateTime.now();
        long durationMs = ChronoUnit.MILLIS.between(execution.getStartedAt(), completedAt);
        
        // Update execution log
        execution.setStatus(JobExecution.ExecutionStatus.COMPLETED);
        execution.setCompletedAt(completedAt);
        execution.setDurationMs(durationMs);
        execution.setResult(serializeResult(result.getData()));
        executionRepository.save(execution);
        
        // Update job status
        job.setStatus(Job.JobStatus.COMPLETED);
        job.setCompletedAt(completedAt);
        job.setLastExecutionAt(completedAt);
        
        // Handle recurring jobs
        if (job.getScheduleType() != Job.ScheduleType.ONCE) {
            scheduleNextExecution(job);
        }
        
        jobRepository.save(job);
        
        log.info("Job {} completed successfully in {} ms", job.getId(), durationMs);
    }
    
    private void handleFailedExecution(Job job, JobExecution execution, JobResult result) {
        LocalDateTime completedAt = LocalDateTime.now();
        long durationMs = ChronoUnit.MILLIS.between(execution.getStartedAt(), completedAt);
        
        // Update execution log
        execution.setStatus(JobExecution.ExecutionStatus.FAILED);
        execution.setCompletedAt(completedAt);
        execution.setDurationMs(durationMs);
        execution.setErrorMessage(result.getMessage());
        executionRepository.save(execution);
        
        // Handle retry logic
        if (job.getCurrentRetries() < job.getMaxRetries()) {
            retryService.scheduleRetry(job, result.getException());
        } else {
            // Max retries reached
            job.setStatus(Job.JobStatus.FAILED);
            job.setCompletedAt(completedAt);
            jobRepository.save(job);
            
            log.error("Job {} failed permanently after {} retries", job.getId(), job.getMaxRetries());
        }
    }
    
    private void scheduleNextExecution(Job job) {
        try {
            LocalDateTime nextExecution = calculateNextExecution(job);
            if (nextExecution != null) {
                job.setNextExecutionAt(nextExecution);
                job.setScheduledAt(nextExecution);
                job.setStatus(Job.JobStatus.SCHEDULED);
                job.setCurrentRetries(0); // Reset retry count for next execution
                
                log.info("Scheduled next execution for job {} at {}", job.getId(), nextExecution);
            }
        } catch (Exception e) {
            log.error("Failed to schedule next execution for job: {}", job.getId(), e);
        }
    }
    
    private LocalDateTime calculateNextExecution(Job job) {
        // This would use CronService for CRON jobs or calculate interval for INTERVAL jobs
        // Simplified implementation
        return LocalDateTime.now().plusHours(1);
    }
    
    private String generateExecutionId(Job job) {
        return job.getId() + "-" + System.currentTimeMillis() + "-" + job.getCurrentRetries();
    }
    
    private String getExecutorNodeId() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-node";
        }
    }
    
    private String serializeResult(Object data) {
        try {
            if (data == null) return null;
            // In real implementation, use ObjectMapper
            return data.toString();
        } catch (Exception e) {
            log.warn("Failed to serialize job result", e);
            return null;
        }
    }
}