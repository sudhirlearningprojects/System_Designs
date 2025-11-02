package org.sudhir512kj.jobscheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.jobscheduler.model.Job;
import org.sudhir512kj.jobscheduler.repository.JobRepository;
import org.sudhir512kj.jobscheduler.scheduler.JobScheduler;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {
    private final JobRepository jobRepository;
    private final JobScheduler jobScheduler;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String DEAD_LETTER_TOPIC = "job-dead-letter-queue";
    
    public void scheduleRetry(Job job, Exception error) {
        log.info("Scheduling retry for job: {} (attempt {}/{})", 
                job.getId(), job.getCurrentRetries() + 1, job.getMaxRetries());
        
        job.setCurrentRetries(job.getCurrentRetries() + 1);
        job.setStatus(Job.JobStatus.RETRYING);
        
        // Calculate retry delay using exponential backoff
        long delaySeconds = calculateRetryDelay(job.getCurrentRetries());
        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(delaySeconds);
        
        job.setScheduledAt(nextRetry);
        job.setNextExecutionAt(nextRetry);
        
        jobRepository.save(job);
        
        // Reschedule the job
        jobScheduler.scheduleJob(job);
        
        log.info("Job {} scheduled for retry at {} (delay: {} seconds)", 
                job.getId(), nextRetry, delaySeconds);
    }
    
    public void moveToDeadLetterQueue(Job job, String reason) {
        log.warn("Moving job {} to dead letter queue: {}", job.getId(), reason);
        
        job.setStatus(Job.JobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        
        // Send to dead letter queue for manual intervention
        try {
            kafkaTemplate.send(DEAD_LETTER_TOPIC, job.getId().toString(), job);
            log.info("Job {} sent to dead letter queue", job.getId());
        } catch (Exception e) {
            log.error("Failed to send job {} to dead letter queue", job.getId(), e);
        }
    }
    
    private long calculateRetryDelay(int retryCount) {
        // Exponential backoff: base_delay * (2^retry_count) with max delay
        long baseDelaySeconds = 30; // 30 seconds base delay
        long maxDelaySeconds = 300; // 5 minutes max delay
        
        long delay = baseDelaySeconds * (1L << (retryCount - 1));
        return Math.min(delay, maxDelaySeconds);
    }
}