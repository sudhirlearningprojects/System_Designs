package org.sudhir512kj.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.notification.dto.NotificationRequest;
import org.sudhir512kj.notification.dto.NotificationResponse;
import org.sudhir512kj.notification.model.*;
import org.sudhir512kj.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final PreferenceService preferenceService;
    private final IdempotencyService idempotencyService;
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private final MetricsService metricsService;
    
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        if (idempotencyService.isDuplicate(request.getIdempotencyKey())) {
            log.warn("Duplicate notification request: {}", request.getIdempotencyKey());
            return NotificationResponse.builder()
                .success(false)
                .message("Duplicate request")
                .build();
        }
        
        Notification notification = createNotification(request);
        
        List<Channel> allowedChannels = request.getChannels().stream()
            .filter(channel -> preferenceService.shouldSendNotification(
                request.getUserId(), request.getType(), channel))
            .toList();
        
        if (allowedChannels.isEmpty()) {
            log.info("No allowed channels for user {} and type {}", 
                request.getUserId(), request.getType());
            return NotificationResponse.builder()
                .success(false)
                .message("No allowed channels based on user preferences")
                .build();
        }
        
        notification.setChannels(allowedChannels);
        notificationRepository.save(notification);
        
        if (notification.getScheduledAt() != null && 
            notification.getScheduledAt().isAfter(Instant.now())) {
            notification.setStatus(NotificationStatus.SCHEDULED);
            notificationRepository.save(notification);
        } else {
            publishToKafka(notification);
        }
        
        return NotificationResponse.builder()
            .success(true)
            .notificationId(notification.getId())
            .message("Notification queued successfully")
            .build();
    }
    
    private Notification createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setPriority(request.getPriority());
        notification.setChannels(request.getChannels());
        notification.setTemplateId(request.getTemplateId());
        notification.setTemplateData(request.getTemplateData());
        notification.setScheduledAt(request.getScheduledAt());
        notification.setIdempotencyKey(request.getIdempotencyKey());
        return notification;
    }
    
    private void publishToKafka(Notification notification) {
        String topic = getTopicByPriority(notification.getPriority());
        kafkaTemplate.send(topic, notification.getId(), notification);
        
        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);
        
        log.info("Published notification {} to topic {}", notification.getId(), topic);
    }
    
    private String getTopicByPriority(NotificationPriority priority) {
        return "notifications." + priority.name().toLowerCase();
    }
    
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
