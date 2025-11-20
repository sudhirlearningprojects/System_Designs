package org.sudhir512kj.redis.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sudhir512kj.redis.RedisTemplate;
import org.sudhir512kj.redis.annotation.CacheEvict;
import org.sudhir512kj.redis.annotation.Cacheable;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {
    private final RedisTemplate redisTemplate;
    
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String key = cacheable.key();
        
        // Try to get from cache
        String cachedValue = redisTemplate.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        // Execute method and cache result
        Object result = joinPoint.proceed();
        if (result != null) {
            redisTemplate.set(key, result.toString(), Duration.ofSeconds(cacheable.ttl()));
        }
        
        return result;
    }
    
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        Object result = joinPoint.proceed();
        
        // Evict from cache
        redisTemplate.delete(cacheEvict.key());
        
        return result;
    }
}