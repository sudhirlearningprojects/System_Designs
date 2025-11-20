package org.sudhir512kj.redis.service;

import lombok.RequiredArgsConstructor;
import org.sudhir512kj.redis.model.RedisValue;
import org.sudhir512kj.redis.storage.InMemoryStorage;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final InMemoryStorage storage;
    
    // String operations
    public String set(String key, String value, Duration ttl) {
        RedisValue redisValue = new RedisValue(value, RedisValue.ValueType.STRING);
        if (ttl != null) {
            redisValue.setExpiresAt(Instant.now().plus(ttl));
        }
        storage.set(key, redisValue);
        return "OK";
    }
    
    public String get(String key) {
        RedisValue value = storage.get(key);
        return value != null && value.getType() == RedisValue.ValueType.STRING ? 
               (String) value.getValue() : null;
    }
    
    // List operations
    public int lpush(String key, String... values) {
        RedisValue redisValue = storage.get(key);
        List<String> list = redisValue != null ? (List<String>) redisValue.getValue() : new ArrayList<>();
        
        for (String value : values) {
            list.add(0, value);
        }
        
        storage.set(key, new RedisValue(list, RedisValue.ValueType.LIST));
        return list.size();
    }
    
    public String lpop(String key) {
        RedisValue redisValue = storage.get(key);
        if (redisValue == null || redisValue.getType() != RedisValue.ValueType.LIST) return null;
        
        List<String> list = (List<String>) redisValue.getValue();
        return list.isEmpty() ? null : list.remove(0);
    }
    
    // Set operations
    public int sadd(String key, String... members) {
        RedisValue redisValue = storage.get(key);
        Set<String> set = redisValue != null ? (Set<String>) redisValue.getValue() : new HashSet<>();
        
        int added = 0;
        for (String member : members) {
            if (set.add(member)) added++;
        }
        
        storage.set(key, new RedisValue(set, RedisValue.ValueType.SET));
        return added;
    }
    
    public boolean sismember(String key, String member) {
        RedisValue redisValue = storage.get(key);
        if (redisValue == null || redisValue.getType() != RedisValue.ValueType.SET) return false;
        
        Set<String> set = (Set<String>) redisValue.getValue();
        return set.contains(member);
    }
    
    // Hash operations
    public String hset(String key, String field, String value) {
        RedisValue redisValue = storage.get(key);
        Map<String, String> hash = redisValue != null ? (Map<String, String>) redisValue.getValue() : new HashMap<>();
        
        hash.put(field, value);
        storage.set(key, new RedisValue(hash, RedisValue.ValueType.HASH));
        return "OK";
    }
    
    public String hget(String key, String field) {
        RedisValue redisValue = storage.get(key);
        if (redisValue == null || redisValue.getType() != RedisValue.ValueType.HASH) return null;
        
        Map<String, String> hash = (Map<String, String>) redisValue.getValue();
        return hash.get(field);
    }
    
    // Generic operations
    public boolean del(String key) {
        return storage.delete(key);
    }
    
    public boolean exists(String key) {
        return storage.exists(key);
    }
    
    public boolean expire(String key, Duration ttl) {
        RedisValue value = storage.get(key);
        if (value == null) return false;
        
        value.setExpiresAt(Instant.now().plus(ttl));
        storage.set(key, value);
        return true;
    }
}