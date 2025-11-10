package org.sudhir512kj.ratelimiter.algorithm;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.sudhir512kj.ratelimiter.dto.RateLimitResponse;
import org.sudhir512kj.ratelimiter.model.RateLimitConfig;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class LeakyBucketAlgorithm implements RateLimitAlgorithm {
    private final RedisTemplate<String, String> redisTemplate;
    
    public LeakyBucketAlgorithm(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public RateLimitResponse checkRateLimit(String key, RateLimitConfig config) {
        String redisKey = "rate_limit:leaky:" + key;
        long now = Instant.now().getEpochSecond();
        
        String bucketData = redisTemplate.opsForValue().get(redisKey);
        LeakyBucket bucket;
        
        if (bucketData == null) {
            bucket = new LeakyBucket(0, now, config.getRefillRate());
        } else {
            bucket = LeakyBucket.fromString(bucketData);
            bucket.leak(now);
        }
        
        if (bucket.volume < config.getRequestsPerWindow()) {
            bucket.volume++;
            bucket.lastLeak = now;
            
            redisTemplate.opsForValue().set(redisKey, bucket.toString(), 
                config.getWindowSizeSeconds(), TimeUnit.SECONDS);
            
            int remaining = config.getRequestsPerWindow() - (int)bucket.volume;
            long resetTime = now + (long)(bucket.volume / config.getRefillRate());
            
            return RateLimitResponse.allowed(remaining, resetTime, config.getRuleKey(), getAlgorithmName());
        }
        
        long retryAfter = (long)(1.0 / config.getRefillRate());
        return RateLimitResponse.denied(retryAfter, config.getRuleKey(), getAlgorithmName());
    }
    
    @Override
    public void resetRateLimit(String key) {
        redisTemplate.delete("rate_limit:leaky:" + key);
    }
    
    @Override
    public String getAlgorithmName() {
        return "REDIS_LEAKY_BUCKET";
    }
    
    private static class LeakyBucket {
        double volume;
        long lastLeak;
        double leakRate;
        
        LeakyBucket(double volume, long timestamp, double leakRate) {
            this.volume = volume;
            this.lastLeak = timestamp;
            this.leakRate = leakRate;
        }
        
        void leak(long now) {
            double leaked = (now - lastLeak) * leakRate;
            volume = Math.max(0, volume - leaked);
            lastLeak = now;
        }
        
        @Override
        public String toString() {
            return volume + ":" + lastLeak + ":" + leakRate;
        }
        
        static LeakyBucket fromString(String data) {
            String[] parts = data.split(":");
            return new LeakyBucket(
                Double.parseDouble(parts[0]), 
                Long.parseLong(parts[1]),
                Double.parseDouble(parts[2])
            );
        }
    }
}