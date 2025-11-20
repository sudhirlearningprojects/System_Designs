package org.sudhir512kj.urlshortener.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sudhir512kj.urlshortener.dto.RedirectResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {
    
    private final CacheService cacheService;
    private final AnalyticsService analyticsService;
    
    public RedirectResponse redirect(String shortUrl, HttpServletRequest request) {
        log.info("Redirecting short URL: {}", shortUrl);
        
        // Get long URL from cache/database
        String longUrl = cacheService.getLongUrl(shortUrl);
        
        if (longUrl == null) {
            throw new IllegalArgumentException("Short URL not found");
        }
        
        // Async analytics tracking
        analyticsService.trackClick(shortUrl, request);
        
        log.info("Redirect successful: {} -> {}", shortUrl, longUrl);
        
        return RedirectResponse.builder()
                .longUrl(longUrl)
                .statusCode(HttpStatus.MOVED_PERMANENTLY)
                .build();
    }
}