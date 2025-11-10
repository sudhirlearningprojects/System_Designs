package org.sudhir512kj.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.notification.model.*;
import org.sudhir512kj.notification.repository.DLQRepository;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private final DLQRepository dlqRepository;
    private final MetricsService metricsService;
    
    public void scheduleRetry(Notification notification, int attemptNumber, String errorMessage) {
        if (attemptNumber >= MAX_RETRIES) {
            moveToDLQ(notification, DLQReason.MAX_RETRIES_EXCEEDED, errorMessage);
            return;
        }
        
        long delayMs = calculateDelay(attemptNumber);
        notification.setRetryCount(attemptNumber);
        
        String topic = getRetryTopic(notification.getPriority());
        kafkaTemplate.send(topic, notification.getId(), notification);
        
        log.info("Scheduled retry {} for notification {} with delay {}ms", 
            attemptNumber, notification.getId(), delayMs);
    }
    
    private long calculateDelay(int attemptNumber) {
        long delayMs = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber));
        long jitter = ThreadLocalRandom.current().nextLong(0, delayMs / 10);
        return delayMs + jitter;
    }
    
    public void moveToDLQ(Notification notification, DLQReason reason, String errorMessage) {
        DLQEntry entry = new DLQEntry();
        entry.setNotificationId(notification.getId());
        entry.setPayload(notification.toString());
        entry.setReason(reason);
        entry.setErrorMessage(errorMessage);
        entry.setTotalAttempts(notification.getRetryCount());
        entry.setFirstAttemptAt(notification.getCreatedAt());
        entry.setLastAttemptAt(Instant.now());
        
        dlqRepository.save(entry);
        metricsService.recordDLQEntry(reason.name());
        
        log.error("Moved notification {} to DLQ. Reason: {}", notification.getId(), reason);
    }
    
    private String getRetryTopic(NotificationPriority priority) {
        return "notifications." + priority.name().toLowerCase() + ".retry";
    }
}
