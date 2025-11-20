package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;
import org.sudhir512kj.whatsapp.model.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    public void updateUserPresence(String userId, User.UserStatus status) {
        String key = WhatsAppConstants.PRESENCE_KEY + userId;
        
        // Store presence in Redis with TTL
        redisTemplate.opsForValue().set(key, status.name(), WhatsAppConstants.PRESENCE_TTL_MINUTES, TimeUnit.MINUTES);
        
        // Store heartbeat timestamp
        redisTemplate.opsForValue().set(WhatsAppConstants.HEARTBEAT_KEY + userId, 
            LocalDateTime.now().toString(), WhatsAppConstants.PRESENCE_TTL_MINUTES, TimeUnit.MINUTES);
        
        // Broadcast presence update to contacts
        broadcastPresenceUpdate(userId, status);
        
        log.debug("Updated presence for user {}: {}", userId, status);
    }
    
    public User.UserStatus getUserPresence(String userId) {
        String key = WhatsAppConstants.PRESENCE_KEY + userId;
        String status = (String) redisTemplate.opsForValue().get(key);
        
        if (status == null) {
            return User.UserStatus.OFFLINE;
        }
        
        return User.UserStatus.valueOf(status);
    }
    
    public void addUserToChat(String userId, String chatId) {
        String key = "chat_users:" + chatId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, Duration.ofHours(24));
    }
    
    public void removeUserFromChat(String userId, String chatId) {
        String key = "chat_users:" + chatId;
        redisTemplate.opsForSet().remove(key, userId);
    }
    
    public Set<Object> getChatUsers(String chatId) {
        String key = "chat_users:" + chatId;
        return redisTemplate.opsForSet().members(key);
    }
    
    private void broadcastPresenceUpdate(String userId, User.UserStatus status) {
        // Get user's active chats and broadcast to participants
        messagingTemplate.convertAndSend(WhatsAppConstants.PRESENCE_TOPIC + userId, status.name());
    }
    
    public void heartbeat(String userId) {
        updateUserPresence(userId, User.UserStatus.ONLINE);
    }
}