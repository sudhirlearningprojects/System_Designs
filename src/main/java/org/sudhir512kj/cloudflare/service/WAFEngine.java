package org.sudhir512kj.cloudflare.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.cloudflare.model.SecurityRule;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WAFEngine {
    
    private final SecurityRuleService ruleService;
    
    public WAFResult evaluateRequest(HttpServletRequest request, String domain) {
        List<SecurityRule> rules = ruleService.getRulesForDomain(domain);
        
        // Sort by priority (higher priority first)
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        for (SecurityRule rule : rules) {
            if (rule.getEnabled() && matchesRule(rule, request)) {
                log.info("WAF rule matched - Rule: {}, Action: {}, Domain: {}", 
                        rule.getId(), rule.getAction(), domain);
                        
                return WAFResult.builder()
                    .action(rule.getAction())
                    .ruleId(rule.getId())
                    .reason(rule.getRuleType())
                    .matched(true)
                    .build();
            }
        }
        
        return WAFResult.allow();
    }
    
    private boolean matchesRule(SecurityRule rule, HttpServletRequest request) {
        String pattern = rule.getPattern();
        String ruleType = rule.getRuleType();
        
        try {
            Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            
            switch (ruleType.toLowerCase()) {
                case "waf":
                    return matchesWAFRule(compiledPattern, request);
                case "rate_limit":
                    return matchesRateLimitRule(compiledPattern, request);
                case "firewall":
                    return matchesFirewallRule(compiledPattern, request);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("Error evaluating rule pattern: {}", pattern, e);
            return false;
        }
    }
    
    private boolean matchesWAFRule(Pattern pattern, HttpServletRequest request) {
        // Check URL path
        if (pattern.matcher(request.getRequestURI()).find()) {
            return true;
        }
        
        // Check query string
        String queryString = request.getQueryString();
        if (queryString != null && pattern.matcher(queryString).find()) {
            return true;
        }
        
        // Check headers
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && pattern.matcher(userAgent).find()) {
            return true;
        }
        
        String referer = request.getHeader("Referer");
        if (referer != null && pattern.matcher(referer).find()) {
            return true;
        }
        
        return false;
    }
    
    private boolean matchesRateLimitRule(Pattern pattern, HttpServletRequest request) {
        // Rate limit rules are handled by DDoSProtectionService
        // This is for custom rate limiting patterns
        String clientIp = getClientIp(request);
        return pattern.matcher(clientIp).matches();
    }
    
    private boolean matchesFirewallRule(Pattern pattern, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String path = request.getRequestURI();
        
        // Check IP pattern
        if (pattern.matcher(clientIp).matches()) {
            return true;
        }
        
        // Check user agent pattern
        if (userAgent != null && pattern.matcher(userAgent).find()) {
            return true;
        }
        
        // Check path pattern
        return pattern.matcher(path).find();
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    public static class WAFResult {
        private String action;
        private UUID ruleId;
        private String reason;
        private boolean matched;
        
        public static WAFResult allow() {
            return WAFResult.builder()
                .action("ALLOW")
                .matched(false)
                .build();
        }
        
        public static WAFResult block(UUID ruleId, String reason) {
            return WAFResult.builder()
                .action("BLOCK")
                .ruleId(ruleId)
                .reason(reason)
                .matched(true)
                .build();
        }
        
        public static WAFResult challenge(UUID ruleId, String reason) {
            return WAFResult.builder()
                .action("CHALLENGE")
                .ruleId(ruleId)
                .reason(reason)
                .matched(true)
                .build();
        }
    }
}