package org.sudhir512kj.redis.storage;

import org.sudhir512kj.redis.model.RedisValue;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class InMemoryStorage {
    private final ConcurrentHashMap<String, RedisValue> data = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public RedisValue get(String key) {
        lock.readLock().lock();
        try {
            RedisValue value = data.get(key);
            if (value != null && value.isExpired()) {
                data.remove(key);
                return null;
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void set(String key, RedisValue value) {
        lock.writeLock().lock();
        try {
            data.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean delete(String key) {
        lock.writeLock().lock();
        try {
            return data.remove(key) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean exists(String key) {
        return get(key) != null;
    }
    
    public int size() {
        return data.size();
    }
}