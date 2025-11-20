package org.sudhir512kj.uber.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CCGScheduler {
    private static final Logger log = LoggerFactory.getLogger(CCGScheduler.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final PushDelivery pushDelivery;
    
    public CCGScheduler(RedisTemplate<String, Object> redisTemplate, PushDelivery pushDelivery) {
        this.redisTemplate = redisTemplate;
        this.pushDelivery = pushDelivery;
    }
    
    @Scheduled(fixedRate = 1000)
    public void processHighPriority() {
        processQueue("push:queue:high");
    }
    
    @Scheduled(fixedRate = 5000)
    public void processMediumPriority() {
        processQueue("push:queue:medium");
    }
    
    @Scheduled(fixedRate = 10000)
    public void processLowPriority() {
        processQueue("push:queue:low");
    }
    
    private void processQueue(String queueKey) {
        String messageId = (String) redisTemplate.opsForList().leftPop(queueKey);
        if (messageId == null) return;
        
        PushMessage message = (PushMessage) redisTemplate.opsForValue()
            .get("push:inbox:*:" + messageId);
        
        if (message != null && shouldSendNow(message)) {
            pushDelivery.deliver(message);
        }
    }
    
    private boolean shouldSendNow(PushMessage message) {
        return message.getScheduledAt() == null || 
               message.getScheduledAt().isBefore(LocalDateTime.now());
    }
}
