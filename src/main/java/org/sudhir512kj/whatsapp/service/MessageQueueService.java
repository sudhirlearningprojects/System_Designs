package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.whatsapp.dto.MessageDTO;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;
import org.sudhir512kj.whatsapp.model.Chat;
import org.sudhir512kj.whatsapp.model.Message;
import org.sudhir512kj.whatsapp.repository.ChatRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageQueueService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    private final PresenceService presenceService;
    
    public void publishMessage(MessageDTO message) {
        // Publish to Kafka for reliable delivery
        kafkaTemplate.send(WhatsAppConstants.MESSAGES_TOPIC, message.getChatId(), message);
        log.info("Message published to Kafka: {}", message.getId());
    }
    
    @KafkaListener(topics = WhatsAppConstants.MESSAGES_TOPIC, groupId = "message-delivery-group")
    public void handleMessageDelivery(MessageDTO message) {
        try {
            // Get chat participants
            Chat chat = chatRepository.findById(message.getChatId()).orElse(null);
            if (chat == null) {
                log.error("Chat not found: {}", message.getChatId());
                return;
            }
            
            // Deliver to online users via WebSocket
            deliverToOnlineUsers(message, chat);
            
            // Store for offline users (handled by MessageService)
            log.info("Message delivered: {}", message.getId());
            
        } catch (Exception e) {
            log.error("Error delivering message: {}", message.getId(), e);
            // Could implement retry logic or DLQ here
        }
    }
    
    private void deliverToOnlineUsers(MessageDTO message, Chat chat) {
        chat.getParticipants().forEach(participant -> {
            if (!participant.getId().equals(message.getSenderId())) {
                // Check if user is online
                if (presenceService.getUserPresence(participant.getId()) != 
                    org.sudhir512kj.whatsapp.model.User.UserStatus.OFFLINE) {
                    
                    // Send via WebSocket
                    messagingTemplate.convertAndSendToUser(
                        participant.getId(), 
                        WhatsAppConstants.USER_QUEUE, 
                        message
                    );
                }
            }
        });
        
        // Also broadcast to chat topic
        messagingTemplate.convertAndSend(WhatsAppConstants.CHAT_TOPIC + message.getChatId(), message);
    }
    
    public void publishTypingIndicator(String chatId, String userId, String userName, boolean isTyping) {
        TypingEvent event = new TypingEvent(userId, userName, isTyping);
        kafkaTemplate.send(WhatsAppConstants.TYPING_TOPIC, chatId, event);
    }
    
    @KafkaListener(topics = WhatsAppConstants.TYPING_TOPIC, groupId = "typing-group")
    public void handleTypingIndicator(TypingEvent event) {
        String topic = event.isTyping ? WhatsAppConstants.CHAT_TOPIC + event.getChatId() + "/typing" 
                                      : WhatsAppConstants.CHAT_TOPIC + event.getChatId() + "/stop-typing";
        messagingTemplate.convertAndSend(topic, event);
    }
    
    public static class TypingEvent {
        private String userId;
        private String userName;
        private String chatId;
        private boolean isTyping;
        
        public TypingEvent() {}
        
        public TypingEvent(String userId, String userName, boolean isTyping) {
            this.userId = userId;
            this.userName = userName;
            this.isTyping = isTyping;
        }
        
        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public boolean isTyping() { return isTyping; }
        public void setTyping(boolean typing) { isTyping = typing; }
    }
}