package org.sudhir512kj.cloudflare.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.cloudflare.service.CDNCacheService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CDNController {
    
    private final CDNCacheService cdnCacheService;
    
    @GetMapping("/**")
    public ResponseEntity<byte[]> handleRequest(HttpServletRequest request) {
        String domain = request.getServerName();
        String path = request.getRequestURI();
        
        log.info("CDN request - Domain: {}, Path: {}, IP: {}", 
                domain, path, getClientIp(request));
        
        return cdnCacheService.serveContent(domain, path, request);
    }
    
    @PostMapping("/api/v1/purge")
    public ResponseEntity<String> purgeCache(@RequestParam String domain,
                                           @RequestParam(required = false) String path) {
        // TODO: Implement cache purging
        log.info("Cache purge requested - Domain: {}, Path: {}", domain, path);
        return ResponseEntity.ok("Cache purged successfully");
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}