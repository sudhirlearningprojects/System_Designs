package org.sudhir512kj.redis.model;

import lombok.Data;
import java.time.Instant;

@Data
public class RedisValue {
    private Object value;
    private ValueType type;
    private Instant expiresAt;
    
    public enum ValueType {
        STRING, LIST, SET, HASH, ZSET
    }
    
    public RedisValue(Object value, ValueType type) {
        this.value = value;
        this.type = type;
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}