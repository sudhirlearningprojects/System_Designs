package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.whatsapp.dto.MessageDTO;
import org.sudhir512kj.whatsapp.model.Message;
import org.sudhir512kj.whatsapp.model.MessageDelivery;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.repository.MessageDeliveryRepository;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;
import org.sudhir512kj.whatsapp.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDeliveryService {
    
    private final MessageRepository messageRepository;
    private final MessageDeliveryRepository messageDeliveryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConnectionManagerService connectionManager;
    
    public void deliverMessage(MessageDTO messageDTO, Set<User> recipients) {
        Message message = messageRepository.findById(messageDTO.getId()).orElse(null);
        if (message == null) {
            log.error("Message not found: {}", messageDTO.getId());
            return;
        }
        
        recipients.forEach(recipient -> {
            if (!recipient.getId().equals(messageDTO.getSenderId())) {
                deliverToUser(messageDTO, message, recipient);
            }
        });
    }
    
    private void deliverToUser(MessageDTO messageDTO, Message message, User recipient) {
        String serverId = connectionManager.getUserServer(recipient.getId());
        
        if (serverId != null) {
            // User is online - deliver immediately
            deliverOnline(messageDTO, recipient);
            updateDeliveryStatus(message, recipient, MessageDelivery.DeliveryStatus.DELIVERED);
        } else {
            // User is offline - store for later delivery
            storeOfflineMessage(messageDTO, recipient);
            log.info("Stored offline message for user: {}", recipient.getId());
        }
    }
    
    private void deliverOnline(MessageDTO messageDTO, User recipient) {
        try {
            // Send to user's personal queue
            messagingTemplate.convertAndSendToUser(
                recipient.getId(), 
                WhatsAppConstants.USER_QUEUE, 
                messageDTO
            );
            
            log.debug("Message delivered online to user: {}", recipient.getId());
        } catch (Exception e) {
            log.error("Failed to deliver message online to user: {}", recipient.getId(), e);
        }
    }
    
    private void storeOfflineMessage(MessageDTO messageDTO, User recipient) {
        String key = WhatsAppConstants.OFFLINE_MESSAGES_KEY + recipient.getId();
        redisTemplate.opsForList().rightPush(key, messageDTO);
        
        // Set TTL for offline messages
        redisTemplate.expire(key, java.time.Duration.ofDays(WhatsAppConstants.OFFLINE_MESSAGES_TTL_DAYS));
    }
    
    public void deliverOfflineMessages(String userId) {
        String key = WhatsAppConstants.OFFLINE_MESSAGES_KEY + userId;
        List<Object> offlineMessages = redisTemplate.opsForList().range(key, 0, -1);
        
        if (offlineMessages != null && !offlineMessages.isEmpty()) {
            offlineMessages.forEach(messageObj -> {
                if (messageObj instanceof MessageDTO) {
                    MessageDTO messageDTO = (MessageDTO) messageObj;
                    messagingTemplate.convertAndSendToUser(
                        userId, 
                        WhatsAppConstants.USER_QUEUE, 
                        messageDTO
                    );
                }
            });
            
            // Clear offline messages after delivery
            redisTemplate.delete(key);
            log.info("Delivered {} offline messages to user: {}", offlineMessages.size(), userId);
        }
    }
    
    public void updateDeliveryStatus(Message message, User user, MessageDelivery.DeliveryStatus status) {
        MessageDelivery delivery = messageDeliveryRepository.findByMessageAndUser(message, user)
                .stream().findFirst().orElse(null);
        
        if (delivery != null) {
            delivery.setStatus(status);
            if (status == MessageDelivery.DeliveryStatus.READ) {
                delivery.setReadAt(LocalDateTime.now());
            }
            messageDeliveryRepository.save(delivery);
            
            // Notify sender about delivery status
            notifyDeliveryStatus(message, user, status);
        }
    }
    
    private void notifyDeliveryStatus(Message message, User user, MessageDelivery.DeliveryStatus status) {
        String topic = WhatsAppConstants.CHAT_TOPIC + message.getChat().getId() + "/delivery";
        DeliveryStatusUpdate update = new DeliveryStatusUpdate(
            message.getId(), 
            user.getId(), 
            status.name()
        );
        
        messagingTemplate.convertAndSend(topic, update);
    }
    
    public static class DeliveryStatusUpdate {
        private String messageId;
        private String userId;
        private String status;
        
        public DeliveryStatusUpdate(String messageId, String userId, String status) {
            this.messageId = messageId;
            this.userId = userId;
            this.status = status;
        }
        
        // Getters
        public String getMessageId() { return messageId; }
        public String getUserId() { return userId; }
        public String getStatus() { return status; }
    }
}