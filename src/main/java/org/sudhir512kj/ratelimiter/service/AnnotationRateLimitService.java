package org.sudhir512kj.ratelimiter.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ratelimiter.algorithm.RateLimitAlgorithm;
import org.sudhir512kj.ratelimiter.annotation.RateLimit;
import org.sudhir512kj.ratelimiter.dto.RateLimitResponse;
import org.sudhir512kj.ratelimiter.model.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationRateLimitService {
    
    private final Map<String, RateLimitAlgorithm> algorithms;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    public boolean checkRateLimit(HttpServletRequest request, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String clientKey = buildClientKey(request, joinPoint, rateLimit);
        RateLimitConfig config = convertToConfig(rateLimit, joinPoint);
        
        String algorithmName = "REDIS_" + rateLimit.algorithm().name();
        RateLimitAlgorithm algorithm = algorithms.get(algorithmName);
        
        if (algorithm == null) {
            log.warn("Algorithm not found: {}", algorithmName);
            return true;
        }
        
        RateLimitResponse response = algorithm.checkRateLimit(clientKey, config);
        
        if (!response.isAllowed()) {
            log.info("Rate limit exceeded for key: {}, rule: {}", clientKey, config.getRuleKey());
        }
        
        return response.isAllowed();
    }
    
    private String buildClientKey(HttpServletRequest request, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String methodKey = joinPoint.getSignature().toShortString();
        
        if (!rateLimit.key().isEmpty()) {
            return evaluateCustomKey(request, joinPoint, rateLimit.key()) + ":" + methodKey;
        }
        
        return switch (rateLimit.scope()) {
            case USER -> "user:" + extractUserId(request) + ":" + methodKey;
            case IP -> "ip:" + getClientIpAddress(request) + ":" + methodKey;
            case API_KEY -> "key:" + extractApiKey(request) + ":" + methodKey;
            case TENANT -> "tenant:" + extractTenantId(request) + ":" + methodKey;
            case GLOBAL -> "global:" + methodKey;
            case CUSTOM -> evaluateCustomKey(request, joinPoint, rateLimit.key()) + ":" + methodKey;
        };
    }
    
    private String evaluateCustomKey(HttpServletRequest request, ProceedingJoinPoint joinPoint, String keyExpression) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("request", request);
            context.setVariable("method", joinPoint.getSignature().getName());
            context.setVariable("args", joinPoint.getArgs());
            context.setVariable("userId", extractUserId(request));
            context.setVariable("apiKey", extractApiKey(request));
            context.setVariable("ip", getClientIpAddress(request));
            
            Object result = parser.parseExpression(keyExpression).getValue(context);
            return result != null ? result.toString() : "unknown";
        } catch (Exception e) {
            log.error("Error evaluating custom key expression: {}", keyExpression, e);
            return "error";
        }
    }
    
    private RateLimitConfig convertToConfig(RateLimit rateLimit, ProceedingJoinPoint joinPoint) {
        RateLimitConfig config = new RateLimitConfig();
        config.setRuleKey(joinPoint.getSignature().toShortString());
        config.setRequestsPerWindow(rateLimit.requests());
        config.setWindowSizeSeconds(rateLimit.window());
        config.setBurstCapacity(rateLimit.burstCapacity() > 0 ? rateLimit.burstCapacity() : rateLimit.requests());
        config.setRefillRate(rateLimit.refillRate());
        config.setEnabled(rateLimit.enabled());
        return config;
    }
    
    private String extractUserId(HttpServletRequest request) {
        return request.getHeader("X-User-ID");
    }
    
    private String extractApiKey(HttpServletRequest request) {
        return request.getHeader("X-API-Key");
    }
    
    private String extractTenantId(HttpServletRequest request) {
        return request.getHeader("X-Tenant-ID");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}