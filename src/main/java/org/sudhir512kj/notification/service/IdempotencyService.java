package org.sudhir512kj.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    
    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null) return false;
        
        String key = "idempotency:" + idempotencyKey;
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        return result == null || !result;
    }
    
    public void markAsProcessed(String notificationId) {
        String key = "processed:" + notificationId;
        redisTemplate.opsForValue()
            .set(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
    }
}
