package org.sudhir512kj.tiktok.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.sudhir512kj.tiktok.dto.NotificationEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProcessor {
    
    @KafkaListener(topics = "tiktok-notifications", groupId = "tiktok-notification-processor")
    public void processNotification(NotificationEvent event) {
        log.info("Processing notification: type={}, userId={}, targetUserId={}", 
            event.getType(), event.getUserId(), event.getTargetUserId());
        
        // Send push notification, email, or in-app notification
        // Implementation depends on notification service (FCM, APNS, etc.)
    }
}
