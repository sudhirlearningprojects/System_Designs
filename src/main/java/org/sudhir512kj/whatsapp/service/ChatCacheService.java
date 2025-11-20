package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.whatsapp.dto.ChatDTO;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;
import org.sudhir512kj.whatsapp.dto.MessageDTO;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void cacheChat(ChatDTO chat) {
        String key = WhatsAppConstants.CHAT_METADATA_KEY + chat.getId();
        redisTemplate.opsForValue().set(key, chat, Duration.ofHours(6));
        
        // Cache participants for quick lookup
        String participantsKey = WhatsAppConstants.CHAT_PARTICIPANTS_KEY + chat.getId();
        chat.getParticipants().forEach(participant -> 
            redisTemplate.opsForSet().add(participantsKey, participant.getId())
        );
        redisTemplate.expire(participantsKey, Duration.ofHours(6));
        
        log.debug("Cached chat metadata: {}", chat.getId());
    }
    
    public ChatDTO getCachedChat(String chatId) {
        String key = WhatsAppConstants.CHAT_METADATA_KEY + chatId;
        return (ChatDTO) redisTemplate.opsForValue().get(key);
    }
    
    public Set<Object> getChatParticipants(String chatId) {
        String key = WhatsAppConstants.CHAT_PARTICIPANTS_KEY + chatId;
        return redisTemplate.opsForSet().members(key);
    }
    
    public void cacheUserChats(String userId, List<ChatDTO> chats) {
        String key = WhatsAppConstants.USER_CHATS_KEY + userId;
        redisTemplate.delete(key);
        chats.forEach(chat -> redisTemplate.opsForList().rightPush(key, chat));
        redisTemplate.expire(key, Duration.ofMinutes(30));
        
        log.debug("Cached {} chats for user: {}", chats.size(), userId);
    }
    
    public List<Object> getCachedUserChats(String userId) {
        String key = WhatsAppConstants.USER_CHATS_KEY + userId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }
    
    public void cacheRecentMessages(String chatId, List<MessageDTO> messages) {
        String key = WhatsAppConstants.RECENT_MESSAGES_KEY + chatId;
        redisTemplate.delete(key);
        messages.forEach(msg -> redisTemplate.opsForList().rightPush(key, msg));
        redisTemplate.expire(key, Duration.ofMinutes(15));
        
        log.debug("Cached {} recent messages for chat: {}", messages.size(), chatId);
    }
    
    public List<Object> getCachedRecentMessages(String chatId) {
        String key = WhatsAppConstants.RECENT_MESSAGES_KEY + chatId;
        return redisTemplate.opsForList().range(key, 0, WhatsAppConstants.RECENT_MESSAGES_LIMIT - 1);
    }
    
    public void invalidateChatCache(String chatId) {
        redisTemplate.delete(WhatsAppConstants.CHAT_METADATA_KEY + chatId);
        redisTemplate.delete(WhatsAppConstants.CHAT_PARTICIPANTS_KEY + chatId);
        redisTemplate.delete(WhatsAppConstants.RECENT_MESSAGES_KEY + chatId);
        
        log.debug("Invalidated cache for chat: {}", chatId);
    }
    
    public void invalidateUserChatsCache(String userId) {
        redisTemplate.delete(WhatsAppConstants.USER_CHATS_KEY + userId);
        log.debug("Invalidated user chats cache: {}", userId);
    }
}