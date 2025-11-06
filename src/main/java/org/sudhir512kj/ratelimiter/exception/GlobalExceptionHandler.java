package org.sudhir512kj.ratelimiter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletResponse response) {
        
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfter()));
        response.setHeader("X-RateLimit-Rule", ex.getRuleKey());
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Rate limit exceeded",
            "message", ex.getMessage(),
            "retryAfter", ex.getRetryAfter(),
            "ruleKey", ex.getRuleKey(),
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
}