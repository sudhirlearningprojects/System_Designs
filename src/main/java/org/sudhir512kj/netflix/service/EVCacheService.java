package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Service
public class EVCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void put(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
    }
    
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public Object getOrCompute(String key, int ttlSeconds, Callable<Object> supplier) {
        Object cached = get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Object value = supplier.call();
            if (value != null) {
                put(key, value, Duration.ofSeconds(ttlSeconds));
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }
}