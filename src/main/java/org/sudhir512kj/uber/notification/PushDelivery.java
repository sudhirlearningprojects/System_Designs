package org.sudhir512kj.uber.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushDelivery {
    private static final Logger log = LoggerFactory.getLogger(PushDelivery.class);
    
    public void deliver(PushMessage message) {
        try {
            switch (message.getPriority()) {
                case HIGH -> deliverToFCM(message);
                case MEDIUM -> deliverToFCM(message);
                case LOW -> deliverToFCM(message);
            }
            message.setStatus(PushMessage.MessageStatus.SENT);
            log.info("Delivered message {} to user {}", message.getMessageId(), message.getUserId());
        } catch (Exception e) {
            message.setStatus(PushMessage.MessageStatus.FAILED);
            log.error("Failed to deliver message {}", message.getMessageId(), e);
        }
    }
    
    private void deliverToFCM(PushMessage message) {
        log.info("FCM: Sending {} priority notification to user {}: {}", 
            message.getPriority(), message.getUserId(), message.getTitle());
    }
}
