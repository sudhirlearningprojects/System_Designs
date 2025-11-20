package org.sudhir512kj.cloudflare.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CDNCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final OriginFetchService originFetchService;
    
    public ResponseEntity<byte[]> serveContent(String domain, String path, 
                                             HttpServletRequest request) {
        String cacheKey = generateCacheKey(domain, path, request);
        
        // Check cache
        CachedContent cached = getCachedContent(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache HIT for key: {}", cacheKey);
            return buildResponse(cached, "HIT");
        }
        
        log.debug("Cache MISS for key: {}", cacheKey);
        
        // Fetch from origin
        OriginFetchService.OriginResponse origin = originFetchService.fetch(domain, path, request);
        
        // Cache if cacheable
        if (origin.isCacheable()) {
            cacheContent(cacheKey, origin, origin.getTtl());
        }
        
        return buildResponse(origin, "MISS");
    }
    
    private String generateCacheKey(String domain, String path, 
                                  HttpServletRequest request) {
        StringBuilder key = new StringBuilder()
            .append(domain)
            .append(":")
            .append(path);
            
        String queryString = request.getQueryString();
        if (queryString != null) {
            key.append("?").append(queryString);
        }
        
        return md5Hex(key.toString());
    }
    
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
    
    private CachedContent getCachedContent(String key) {
        try {
            return (CachedContent) redisTemplate.opsForValue().get("cache:" + key);
        } catch (Exception e) {
            log.warn("Cache retrieval failed for key: {}", key, e);
            return null;
        }
    }
    
    private void cacheContent(String key, OriginFetchService.OriginResponse origin, Duration ttl) {
        try {
            CachedContent cached = CachedContent.builder()
                .content(origin.getContent())
                .headers(origin.getHeaders())
                .statusCode(origin.getStatusCode())
                .cachedAt(Instant.now())
                .ttl(ttl)
                .build();
                
            redisTemplate.opsForValue().set("cache:" + key, cached, ttl);
        } catch (Exception e) {
            log.warn("Cache storage failed for key: {}", key, e);
        }
    }
    
    private ResponseEntity<byte[]> buildResponse(CachedContent cached, String cacheStatus) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(cached.getHeaders());
        headers.set("CF-Cache-Status", cacheStatus);
        headers.set("CF-Ray", generateRayId());
        
        return ResponseEntity.status(cached.getStatusCode())
            .headers(headers)
            .body(cached.getContent());
    }
    
    private ResponseEntity<byte[]> buildResponse(OriginFetchService.OriginResponse origin, String cacheStatus) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(origin.getHeaders());
        headers.set("CF-Cache-Status", cacheStatus);
        headers.set("CF-Ray", generateRayId());
        
        return ResponseEntity.status(origin.getStatusCode())
            .headers(headers)
            .body(origin.getContent());
    }
    
    private String generateRayId() {
        return Long.toHexString(System.currentTimeMillis()) + 
               Integer.toHexString((int)(Math.random() * 1000));
    }
    
    public static class CachedContent {
        private byte[] content;
        private HttpHeaders headers;
        private int statusCode;
        private Instant cachedAt;
        private Duration ttl;
        
        public boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plus(ttl));
        }
        
        // Builder pattern implementation
        public static CachedContentBuilder builder() {
            return new CachedContentBuilder();
        }
        
        public static class CachedContentBuilder {
            private byte[] content;
            private HttpHeaders headers;
            private int statusCode;
            private Instant cachedAt;
            private Duration ttl;
            
            public CachedContentBuilder content(byte[] content) {
                this.content = content;
                return this;
            }
            
            public CachedContentBuilder headers(HttpHeaders headers) {
                this.headers = headers;
                return this;
            }
            
            public CachedContentBuilder statusCode(int statusCode) {
                this.statusCode = statusCode;
                return this;
            }
            
            public CachedContentBuilder cachedAt(Instant cachedAt) {
                this.cachedAt = cachedAt;
                return this;
            }
            
            public CachedContentBuilder ttl(Duration ttl) {
                this.ttl = ttl;
                return this;
            }
            
            public CachedContent build() {
                CachedContent cached = new CachedContent();
                cached.content = this.content;
                cached.headers = this.headers;
                cached.statusCode = this.statusCode;
                cached.cachedAt = this.cachedAt;
                cached.ttl = this.ttl;
                return cached;
            }
        }
        
        // Getters
        public byte[] getContent() { return content; }
        public HttpHeaders getHeaders() { return headers; }
        public int getStatusCode() { return statusCode; }
        public Instant getCachedAt() { return cachedAt; }
        public Duration getTtl() { return ttl; }
    }
    

}