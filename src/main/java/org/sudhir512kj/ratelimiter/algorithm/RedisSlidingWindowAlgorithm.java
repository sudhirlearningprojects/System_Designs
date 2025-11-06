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
public class RedisSlidingWindowAlgorithm implements RateLimitAlgorithm {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public RateLimitResponse checkRateLimit(String key, RateLimitConfig config) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - config.getWindowSizeSeconds();
        String redisKey = "rate_limit:sliding:" + key;
        
        // Remove expired entries
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        
        // Count current requests in window
        Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, now);
        
        if (currentCount >= config.getRequestsPerWindow()) {
            // Get the oldest request time to calculate retry after
            Double oldestScore = redisTemplate.opsForZSet().range(redisKey, 0, 0)
                .stream().findFirst()
                .map(member -> redisTemplate.opsForZSet().score(redisKey, member))
                .orElse((double) now);
            
            long retryAfter = Math.max(0, oldestScore.longValue() + config.getWindowSizeSeconds() - now);
            return RateLimitResponse.denied(retryAfter, config.getRuleKey(), getAlgorithmName());
        }
        
        // Add current request
        redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
        redisTemplate.expire(redisKey, config.getWindowSizeSeconds(), TimeUnit.SECONDS);
        
        int remaining = config.getRequestsPerWindow() - currentCount.intValue() - 1;
        long resetTime = now + config.getWindowSizeSeconds();
        
        return RateLimitResponse.allowed(remaining, resetTime, config.getRuleKey(), getAlgorithmName());
    }
    
    @Override
    public void resetRateLimit(String key) {
        redisTemplate.delete("rate_limit:sliding:" + key);
    }
    
    @Override
    public String getAlgorithmName() {
        return "REDIS_SLIDING_WINDOW";
    }
}