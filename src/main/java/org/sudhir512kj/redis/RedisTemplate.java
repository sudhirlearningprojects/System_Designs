package org.sudhir512kj.redis;

import lombok.RequiredArgsConstructor;
import org.sudhir512kj.redis.service.RedisService;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisTemplate {
    private final RedisService redisService;
    
    // String operations
    public void set(String key, String value) {
        redisService.set(key, value, null);
    }
    
    public void set(String key, String value, Duration ttl) {
        redisService.set(key, value, ttl);
    }
    
    public String get(String key) {
        return redisService.get(key);
    }
    
    // List operations
    public int leftPush(String key, String... values) {
        return redisService.lpush(key, values);
    }
    
    public String leftPop(String key) {
        return redisService.lpop(key);
    }
    
    // Set operations
    public int addToSet(String key, String... members) {
        return redisService.sadd(key, members);
    }
    
    public boolean isMember(String key, String member) {
        return redisService.sismember(key, member);
    }
    
    // Hash operations
    public void hashSet(String key, String field, String value) {
        redisService.hset(key, field, value);
    }
    
    public String hashGet(String key, String field) {
        return redisService.hget(key, field);
    }
    
    // Generic operations
    public boolean delete(String key) {
        return redisService.del(key);
    }
    
    public boolean exists(String key) {
        return redisService.exists(key);
    }
    
    public boolean expire(String key, Duration ttl) {
        return redisService.expire(key, ttl);
    }
}