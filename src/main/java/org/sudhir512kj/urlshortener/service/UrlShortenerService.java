package org.sudhir512kj.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.urlshortener.dto.ShortenUrlRequest;
import org.sudhir512kj.urlshortener.dto.ShortenUrlResponse;
import org.sudhir512kj.urlshortener.model.URL;
import org.sudhir512kj.urlshortener.repository.UrlRepository;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {
    
    private final UrlRepository urlRepository;
    private final CacheService cacheService;
    private final CounterService counterService;
    private final UrlValidator urlValidator;
    
    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;
    
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        log.info("Shortening URL: {}", request.getLongUrl());
        
        // Validate URL
        if (!urlValidator.isValidUrl(request.getLongUrl())) {
            throw new IllegalArgumentException("Invalid URL format");
        }
        
        // Check if custom alias is provided
        String shortUrl;
        if (request.getCustomAlias() != null) {
            if (urlRepository.existsByShortUrl(request.getCustomAlias())) {
                throw new IllegalArgumentException("Custom alias already exists");
            }
            shortUrl = request.getCustomAlias();
        } else {
            // Generate unique short URL
            shortUrl = generateUniqueShortUrl();
        }
        
        // Create URL entity
        URL url = URL.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .userId(request.getUserId())
                .expiresAt(request.getExpiresAt())
                .build();
        
        // Save to database
        urlRepository.save(url);
        
        // Cache the mapping
        cacheService.cacheUrl(shortUrl, request.getLongUrl());
        
        log.info("URL shortened successfully: {} -> {}", request.getLongUrl(), shortUrl);
        
        return ShortenUrlResponse.builder()
                .shortUrl(baseUrl + shortUrl)
                .longUrl(request.getLongUrl())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .build();
    }
    
    private String generateUniqueShortUrl() {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            long counter = counterService.getNextCounter();
            String shortUrl = Base62Encoder.encode(counter);
            
            if (!urlRepository.existsByShortUrl(shortUrl)) {
                return shortUrl;
            }
        }
        throw new RuntimeException("Unable to generate unique short URL");
    }
}