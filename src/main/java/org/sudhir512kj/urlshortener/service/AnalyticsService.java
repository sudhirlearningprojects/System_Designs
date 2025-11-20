package org.sudhir512kj.urlshortener.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Async
    public void trackClick(String shortUrl, HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String referrer = request.getHeader("Referer");
            
            // Update click count in cache
            redisTemplate.opsForValue().increment("clicks:" + shortUrl);
            
            log.info("Tracked click for {}: IP={}, UserAgent={}, Referrer={}", 
                    shortUrl, ipAddress, userAgent, referrer);
                    
        } catch (Exception e) {
            log.error("Error tracking analytics for {}: {}", shortUrl, e.getMessage());
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}