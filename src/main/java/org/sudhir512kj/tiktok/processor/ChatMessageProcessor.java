package org.sudhir512kj.tiktok.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.sudhir512kj.tiktok.model.ChatMessage;
import org.sudhir512kj.tiktok.repository.ChatMessageRepository;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProcessor {
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @KafkaListener(topics = "live-chat-messages", groupId = "tiktok-chat-processor", concurrency = "3")
    public void processMessage(ChatMessage message) {
        chatMessageRepository.save(message);
        
        String cacheKey = "chat:recent:" + message.getStreamId();
        redisTemplate.opsForList().leftPush(cacheKey, message);
        redisTemplate.opsForList().trim(cacheKey, 0, 99);
        redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
        
        log.debug("Processed chat message: streamId={}, userId={}", 
            message.getStreamId(), message.getUserId());
    }
}
