package org.sudhir512kj.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.urlshortener.model.URL;
import org.sudhir512kj.urlshortener.repository.UrlRepository;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UrlRepository urlRepository;
    
    @Cacheable(value = "urls", key = "#shortUrl")
    public String getLongUrl(String shortUrl) {
        log.debug("Getting long URL for: {}", shortUrl);
        
        // L2: Check Redis
        String longUrl = redisTemplate.opsForValue().get("url:" + shortUrl);
        if (longUrl != null) {
            log.debug("Cache hit in Redis for: {}", shortUrl);
            return longUrl;
        }
        
        // L3: Check database
        URL url = urlRepository.findByShortUrl(shortUrl);
        if (url != null && url.getIsActive() && !url.isExpired()) {
            // Cache in Redis for 1 hour
            redisTemplate.opsForValue().set("url:" + shortUrl, 
                                           url.getLongUrl(), 
                                           Duration.ofHours(1));
            log.debug("Cached URL in Redis: {}", shortUrl);
            return url.getLongUrl();
        }
        
        log.debug("URL not found: {}", shortUrl);
        return null;
    }
    
    public void cacheUrl(String shortUrl, String longUrl) {
        redisTemplate.opsForValue().set("url:" + shortUrl, longUrl, Duration.ofHours(1));
        log.debug("Cached new URL: {} -> {}", shortUrl, longUrl);
    }
    
    public void evictUrl(String shortUrl) {
        redisTemplate.delete("url:" + shortUrl);
        log.debug("Evicted URL from cache: {}", shortUrl);
    }
}