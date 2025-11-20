package org.sudhir512kj.cloudflare.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DDoSProtectionService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityRuleService ruleService;
    
    private static final Set<String> BLACKLISTED_IPS = Set.of(
        "192.168.1.100", "10.0.0.50" // Example blacklisted IPs
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|script).*"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script|javascript:|onload=|onerror=|onclick=).*"
    );
    
    public boolean isRequestAllowed(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        
        // Check IP reputation
        if (isBlacklistedIp(clientIp)) {
            log.warn("Blocked blacklisted IP: {}", clientIp);
            return false;
        }
        
        // Rate limiting check
        if (!checkRateLimit(clientIp, request)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return false;
        }
        
        // Behavioral analysis
        if (detectSuspiciousPattern(clientIp, request)) {
            log.warn("Suspicious pattern detected for IP: {}", clientIp);
            return false;
        }
        
        return true;
    }
    
    private boolean isBlacklistedIp(String clientIp) {
        return BLACKLISTED_IPS.contains(clientIp) || 
               Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + clientIp));
    }
    
    private boolean checkRateLimit(String clientIp, HttpServletRequest request) {
        String key = "rate_limit:" + clientIp;
        String countStr = redisTemplate.opsForValue().get(key);
        
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        int limit = getRateLimitForIp(clientIp);
        
        if (currentCount >= limit) {
            // Add to temporary blacklist for 5 minutes
            redisTemplate.opsForValue().set("blacklist:" + clientIp, "true", Duration.ofMinutes(5));
            return false;
        }
        
        // Increment counter
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(1));
        
        return true;
    }
    
    private int getRateLimitForIp(String clientIp) {
        // Check if IP is in whitelist (higher limits)
        if (Boolean.TRUE.equals(redisTemplate.hasKey("whitelist:" + clientIp))) {
            return 1000; // Higher limit for whitelisted IPs
        }
        return 100; // Default limit
    }
    
    private boolean detectSuspiciousPattern(String clientIp, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // Check for empty or suspicious user agents
        if (userAgent == null || userAgent.trim().isEmpty() || 
            userAgent.toLowerCase().contains("bot") && !isLegitimateBot(userAgent)) {
            return true;
        }
        
        // SQL injection patterns
        if (containsSqlInjection(path) || containsSqlInjection(userAgent) || 
            (queryString != null && containsSqlInjection(queryString))) {
            return true;
        }
        
        // XSS patterns
        if (containsXssPattern(path) || containsXssPattern(userAgent) ||
            (queryString != null && containsXssPattern(queryString))) {
            return true;
        }
        
        // Check request frequency pattern
        return isRequestPatternSuspicious(clientIp);
    }
    
    private boolean containsSqlInjection(String input) {
        return input != null && SQL_INJECTION_PATTERN.matcher(input).matches();
    }
    
    private boolean containsXssPattern(String input) {
        return input != null && XSS_PATTERN.matcher(input).matches();
    }
    
    private boolean isLegitimateBot(String userAgent) {
        return userAgent.toLowerCase().contains("googlebot") ||
               userAgent.toLowerCase().contains("bingbot") ||
               userAgent.toLowerCase().contains("slurp");
    }
    
    private boolean isRequestPatternSuspicious(String clientIp) {
        String patternKey = "pattern:" + clientIp;
        String requestCount = redisTemplate.opsForValue().get(patternKey);
        
        int count = requestCount != null ? Integer.parseInt(requestCount) : 0;
        
        // If more than 50 requests in 10 seconds, it's suspicious
        if (count > 50) {
            return true;
        }
        
        redisTemplate.opsForValue().increment(patternKey);
        redisTemplate.expire(patternKey, Duration.ofSeconds(10));
        
        return false;
    }
    
    private String getClientIp(HttpServletRequest request) {
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
    
    public void addToBlacklist(String ip, Duration duration) {
        redisTemplate.opsForValue().set("blacklist:" + ip, "true", duration);
        log.info("Added IP {} to blacklist for {}", ip, duration);
    }
    
    public void addToWhitelist(String ip) {
        redisTemplate.opsForValue().set("whitelist:" + ip, "true");
        log.info("Added IP {} to whitelist", ip);
    }
}