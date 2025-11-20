package org.sudhir512kj.urlshortener.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CounterService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String COUNTER_KEY = "url_counter";
    
    public long getNextCounter() {
        return redisTemplate.opsForValue().increment(COUNTER_KEY);
    }
}