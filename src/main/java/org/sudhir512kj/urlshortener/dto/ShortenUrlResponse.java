package org.sudhir512kj.urlshortener.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShortenUrlResponse {
    
    private String shortUrl;
    private String longUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}