package org.sudhir512kj.urlshortener.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.urlshortener.dto.RedirectResponse;
import org.sudhir512kj.urlshortener.dto.ShortenUrlRequest;
import org.sudhir512kj.urlshortener.dto.ShortenUrlResponse;
import org.sudhir512kj.urlshortener.service.RedirectService;
import org.sudhir512kj.urlshortener.service.UrlShortenerService;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
@RequiredArgsConstructor
public class UrlController {
    
    private final UrlShortenerService urlShortenerService;
    
    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request) {
        
        ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
        return ResponseEntity.ok(response);
    }
}

@RestController
@RequiredArgsConstructor
class RedirectController {
    
    private final RedirectService redirectService;
    
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortUrl,
            HttpServletRequest request) {
        
        RedirectResponse response = redirectService.redirect(shortUrl, request);
        
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", response.getLongUrl())
                .build();
    }
}