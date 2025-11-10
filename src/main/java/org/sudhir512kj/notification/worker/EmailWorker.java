package org.sudhir512kj.notification.worker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.sudhir512kj.notification.model.*;
import org.sudhir512kj.notification.repository.DeliveryLogRepository;
import org.sudhir512kj.notification.repository.NotificationRepository;
import org.sudhir512kj.notification.service.MetricsService;
import org.sudhir512kj.notification.service.RetryService;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailWorker {
    private final CircuitBreaker emailCircuitBreaker;
    private final RateLimiter emailRateLimiter;
    private final RetryService retryService;
    private final DeliveryLogRepository deliveryLogRepository;
    private final NotificationRepository notificationRepository;
    private final MetricsService metricsService;
    
    @KafkaListener(topics = {"notifications.critical", "notifications.high", 
                             "notifications.medium", "notifications.low"}, 
                   concurrency = "10",
                   groupId = "email-worker-group")
    public void processEmailNotification(Notification notification) {
        if (!notification.getChannels().contains(Channel.EMAIL)) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (!emailRateLimiter.acquirePermission()) {
                log.warn("Rate limit exceeded for notification {}", notification.getId());
                retryService.scheduleRetry(notification, notification.getRetryCount() + 1, 
                    "Rate limit exceeded");
                return;
            }
            
            emailCircuitBreaker.executeRunnable(() -> {
                sendEmail(notification);
                logDelivery(notification, DeliveryStatus.SENT, null);
                
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notificationRepository.save(notification);
                
                long latency = System.currentTimeMillis() - startTime;
                metricsService.recordDeliveryLatency(Channel.EMAIL, latency);
                metricsService.recordNotificationSent(Channel.EMAIL, notification.getPriority());
            });
            
        } catch (Exception e) {
            handleFailure(notification, e);
        }
    }
    
    private void sendEmail(Notification notification) {
        log.info("Sending email for notification {} to user {}", 
            notification.getId(), notification.getUserId());
    }
    
    private void handleFailure(Notification notification, Exception error) {
        log.error("Failed to send email for notification {}", notification.getId(), error);
        
        logDelivery(notification, DeliveryStatus.FAILED, error.getMessage());
        metricsService.recordFailure(Channel.EMAIL, error.getClass().getSimpleName());
        
        if (isRetryable(error)) {
            retryService.scheduleRetry(notification, notification.getRetryCount() + 1, 
                error.getMessage());
        } else {
            retryService.moveToDLQ(notification, DLQReason.UNRETRYABLE_ERROR, 
                error.getMessage());
        }
    }
    
    private boolean isRetryable(Exception error) {
        return !(error instanceof IllegalArgumentException);
    }
    
    private void logDelivery(Notification notification, DeliveryStatus status, String errorMessage) {
        DeliveryLog log = new DeliveryLog();
        log.setNotificationId(notification.getId());
        log.setUserId(notification.getUserId());
        log.setChannel(Channel.EMAIL);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setAttemptNumber(notification.getRetryCount());
        deliveryLogRepository.save(log);
    }
}
