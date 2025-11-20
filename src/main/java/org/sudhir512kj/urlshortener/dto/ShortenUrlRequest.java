package org.sudhir512kj.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortenUrlRequest {
    
    @NotBlank(message = "Long URL is required")
    private String longUrl;
    
    @Size(max = 20, message = "Custom alias must be less than 20 characters")
    private String customAlias;
    
    private Long userId;
    
    private LocalDateTime expiresAt;
}