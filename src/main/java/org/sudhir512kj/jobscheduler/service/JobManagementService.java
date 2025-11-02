package org.sudhir512kj.jobscheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.jobscheduler.dto.JobRequest;
import org.sudhir512kj.jobscheduler.dto.JobResponse;
import org.sudhir512kj.jobscheduler.model.Job;
import org.sudhir512kj.jobscheduler.model.Job.JobStatus;
import org.sudhir512kj.jobscheduler.model.JobExecution;
import org.sudhir512kj.jobscheduler.repository.JobRepository;
import org.sudhir512kj.jobscheduler.scheduler.JobScheduler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobManagementService {
    private final JobRepository jobRepository;
    private final JobScheduler jobScheduler;
    private final CronService cronService;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public JobResponse submitJob(JobRequest request) {
        log.info("Submitting job: {}", request.getName());
        
        // Validate schedule
        if (!isValidSchedule(request)) {
            throw new IllegalArgumentException("Invalid schedule configuration");
        }
        
        // Create job entity
        Job job = createJobFromRequest(request);
        job = jobRepository.save(job);
        
        // Schedule the job
        jobScheduler.scheduleJob(job);
        
        log.info("Job submitted successfully: {}", job.getId());
        return mapToResponse(job);
    }
    
    @Transactional
    public void cancelJob(UUID jobId) {
        log.info("Cancelling job: {}", jobId);
        
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel job in status: " + job.getStatus());
        }
        
        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);
        
        // Remove from scheduler
        jobScheduler.cancelScheduledJob(jobId);
        
        log.info("Job cancelled: {}", jobId);
    }
    
    @Transactional
    public void pauseJob(UUID jobId) {
        log.info("Pausing job: {}", jobId);
        
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        if (job.getStatus() != JobStatus.SCHEDULED) {
            throw new IllegalStateException("Can only pause scheduled jobs");
        }
        
        job.setStatus(JobStatus.PAUSED);
        jobRepository.save(job);
        
        // Remove from active scheduling
        jobScheduler.cancelScheduledJob(jobId);
        
        log.info("Job paused: {}", jobId);
    }
    
    @Transactional
    public void resumeJob(UUID jobId) {
        log.info("Resuming job: {}", jobId);
        
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        if (job.getStatus() != JobStatus.PAUSED) {
            throw new IllegalStateException("Can only resume paused jobs");
        }
        
        job.setStatus(JobStatus.SCHEDULED);
        
        // Calculate next execution time
        if (job.getScheduleType() == Job.ScheduleType.CRON) {
            LocalDateTime nextExecution = cronService.getNextExecution(
                job.getScheduleValue(), LocalDateTime.now());
            job.setNextExecutionAt(nextExecution);
        }
        
        jobRepository.save(job);
        
        // Reschedule the job
        jobScheduler.scheduleJob(job);
        
        log.info("Job resumed: {}", jobId);
    }
    
    public JobResponse getJobStatus(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        return mapToResponse(job);
    }
    
    private boolean isValidSchedule(JobRequest request) {
        switch (request.getScheduleType()) {
            case ONCE:
                return request.getScheduledAt() != null;
            case CRON:
                return cronService.isValidCronExpression(request.getScheduleValue());
            case INTERVAL:
                try {
                    long interval = Long.parseLong(request.getScheduleValue());
                    return interval > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return false;
        }
    }
    
    private Job createJobFromRequest(JobRequest request) {
        Job job = new Job();
        job.setName(request.getName());
        job.setType(request.getType());
        job.setScheduleType(request.getScheduleType());
        job.setScheduleValue(request.getScheduleValue());
        job.setPriority(request.getPriority());
        job.setMaxRetries(request.getMaxRetries());
        job.setTimeoutSeconds(request.getTimeoutSeconds());
        job.setStatus(JobStatus.SCHEDULED);
        
        // Set payload
        try {
            job.setPayload(objectMapper.writeValueAsString(request.getPayload()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
        
        // Set tags
        try {
            job.setTags(objectMapper.writeValueAsString(request.getTags()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tags", e);
        }
        
        // Set scheduled time
        if (request.getScheduleType() == Job.ScheduleType.ONCE) {
            job.setScheduledAt(request.getScheduledAt());
        } else if (request.getScheduleType() == Job.ScheduleType.CRON) {
            LocalDateTime nextExecution = cronService.getNextExecution(
                request.getScheduleValue(), LocalDateTime.now());
            job.setScheduledAt(nextExecution);
            job.setNextExecutionAt(nextExecution);
        } else if (request.getScheduleType() == Job.ScheduleType.INTERVAL) {
            long intervalMs = Long.parseLong(request.getScheduleValue());
            LocalDateTime nextExecution = LocalDateTime.now().plusNanos(intervalMs * 1_000_000);
            job.setScheduledAt(nextExecution);
            job.setNextExecutionAt(nextExecution);
        }
        
        return job;
    }
    
    private JobResponse mapToResponse(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setName(job.getName());
        response.setType(job.getType());
        response.setScheduleType(job.getScheduleType());
        response.setScheduleValue(job.getScheduleValue());
        response.setStatus(job.getStatus());
        response.setPriority(job.getPriority());
        response.setMaxRetries(job.getMaxRetries());
        response.setCurrentRetries(job.getCurrentRetries());
        response.setCreatedAt(job.getCreatedAt());
        response.setScheduledAt(job.getScheduledAt());
        response.setNextExecutionAt(job.getNextExecutionAt());
        response.setLastExecutionAt(job.getLastExecutionAt());
        response.setCreatedBy(job.getCreatedBy());
        return response;
    }
}