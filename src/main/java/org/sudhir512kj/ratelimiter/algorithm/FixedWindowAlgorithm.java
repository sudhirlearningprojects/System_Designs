package org.sudhir512kj.ratelimiter.algorithm;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.sudhir512kj.ratelimiter.dto.RateLimitResponse;
import org.sudhir512kj.ratelimiter.model.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FixedWindowAlgorithm implements RateLimitAlgorithm {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public RateLimitResponse checkRateLimit(String key, RateLimitConfig config) {
        long now = Instant.now().getEpochSecond();
        long windowStart = (now / config.getWindowSizeSeconds()) * config.getWindowSizeSeconds();
        String redisKey = "rate_limit:fixed:" + key + ":" + windowStart;
        
        String countStr = redisTemplate.opsForValue().get(redisKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (currentCount >= config.getRequestsPerWindow()) {
            long retryAfter = windowStart + config.getWindowSizeSeconds() - now;
            return RateLimitResponse.denied(retryAfter, config.getRuleKey(), getAlgorithmName());
        }
        
        redisTemplate.opsForValue().increment(redisKey);
        redisTemplate.expire(redisKey, config.getWindowSizeSeconds(), TimeUnit.SECONDS);
        
        int remaining = config.getRequestsPerWindow() - currentCount - 1;
        long resetTime = windowStart + config.getWindowSizeSeconds();
        
        return RateLimitResponse.allowed(remaining, resetTime, config.getRuleKey(), getAlgorithmName());
    }
    
    @Override
    public void resetRateLimit(String key) {
        String pattern = "rate_limit:fixed:" + key + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
    
    @Override
    public String getAlgorithmName() {
        return "REDIS_FIXED_WINDOW";
    }
}