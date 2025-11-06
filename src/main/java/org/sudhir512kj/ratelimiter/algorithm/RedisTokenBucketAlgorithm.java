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
public class RedisTokenBucketAlgorithm implements RateLimitAlgorithm {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public RateLimitResponse checkRateLimit(String key, RateLimitConfig config) {
        String redisKey = "rate_limit:token:" + key;
        long now = Instant.now().getEpochSecond();
        
        // Get current bucket state
        String bucketData = redisTemplate.opsForValue().get(redisKey);
        TokenBucket bucket;
        
        if (bucketData == null) {
            bucket = new TokenBucket(config.getBurstCapacity(), now);
        } else {
            bucket = TokenBucket.fromString(bucketData);
            bucket.refill(now, config.getRefillRate());
        }
        
        if (bucket.tokens >= 1) {
            bucket.tokens--;
            bucket.lastRefill = now;
            
            // Save updated bucket
            redisTemplate.opsForValue().set(redisKey, bucket.toString(), 
                config.getWindowSizeSeconds(), TimeUnit.SECONDS);
            
            long resetTime = now + (long)((config.getBurstCapacity() - bucket.tokens) / config.getRefillRate());
            return RateLimitResponse.allowed((int)bucket.tokens, resetTime, config.getRuleKey(), getAlgorithmName());
        }
        
        long retryAfter = (long)(1.0 / config.getRefillRate());
        return RateLimitResponse.denied(retryAfter, config.getRuleKey(), getAlgorithmName());
    }
    
    @Override
    public void resetRateLimit(String key) {
        redisTemplate.delete("rate_limit:token:" + key);
    }
    
    @Override
    public String getAlgorithmName() {
        return "REDIS_TOKEN_BUCKET";
    }
    
    private static class TokenBucket {
        double tokens;
        long lastRefill;
        
        TokenBucket(double capacity, long timestamp) {
            this.tokens = capacity;
            this.lastRefill = timestamp;
        }
        
        void refill(long now, double refillRate) {
            double tokensToAdd = (now - lastRefill) * refillRate;
            tokens = Math.min(tokens + tokensToAdd, tokens + tokensToAdd);
            lastRefill = now;
        }
        
        @Override
        public String toString() {
            return tokens + ":" + lastRefill;
        }
        
        static TokenBucket fromString(String data) {
            String[] parts = data.split(":");
            return new TokenBucket(Double.parseDouble(parts[0]), Long.parseLong(parts[1]));
        }
    }
}