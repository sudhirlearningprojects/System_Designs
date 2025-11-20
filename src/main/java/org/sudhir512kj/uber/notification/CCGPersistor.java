package org.sudhir512kj.uber.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CCGPersistor {
    private static final Logger log = LoggerFactory.getLogger(CCGPersistor.class);
    private final RedisTemplate<String, Object> redisTemplate;
    
    public CCGPersistor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void persist(PushMessage message) {
        message.setMessageId(UUID.randomUUID());
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus(PushMessage.MessageStatus.PENDING);
        
        String key = "push:inbox:" + message.getUserId() + ":" + message.getMessageId();
        redisTemplate.opsForValue().set(key, message, 24, TimeUnit.HOURS);
        
        String queueKey = "push:queue:" + message.getPriority().name().toLowerCase();
        redisTemplate.opsForList().rightPush(queueKey, message.getMessageId().toString());
        
        log.info("Persisted message {} for user {} with priority {}", 
            message.getMessageId(), message.getUserId(), message.getPriority());
    }
}
