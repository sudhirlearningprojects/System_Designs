package org.sudhir512kj.tiktok.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.tiktok.dto.ChatMessageDTO;
import org.sudhir512kj.tiktok.model.ChatMessage;
import org.sudhir512kj.tiktok.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Transactional
    public ChatMessage saveMessage(Long streamId, Long userId, ChatMessageDTO dto) {
        ChatMessage message = new ChatMessage();
        message.setStreamId(streamId);
        message.setUserId(userId);
        message.setContent(dto.getContent());
        message.setType(ChatMessage.MessageType.TEXT);
        
        ChatMessage saved = chatMessageRepository.save(message);
        
        // Cache recent messages
        String cacheKey = "chat:recent:" + streamId;
        redisTemplate.opsForList().leftPush(cacheKey, saved);
        redisTemplate.opsForList().trim(cacheKey, 0, 99);
        redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
        
        log.debug("Saved chat message: streamId={}, userId={}", streamId, userId);
        
        return saved;
    }
    
    public List<ChatMessage> getRecentMessages(Long streamId, int limit) {
        String cacheKey = "chat:recent:" + streamId;
        List<Object> cached = redisTemplate.opsForList().range(cacheKey, 0, limit - 1);
        
        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                .map(obj -> (ChatMessage) obj)
                .toList();
        }
        
        return chatMessageRepository.findTop50ByStreamIdOrderByTimestampDesc(streamId);
    }
}
