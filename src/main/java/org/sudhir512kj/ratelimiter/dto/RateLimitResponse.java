package org.sudhir512kj.ratelimiter.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitResponse {
    private boolean allowed;
    private int remainingRequests;
    private long resetTimeEpoch;
    private long retryAfterSeconds;
    private String ruleKey;
    private String algorithm;
    
    public static RateLimitResponse allowed(int remaining, long resetTime, String ruleKey, String algorithm) {
        return new RateLimitResponse(true, remaining, resetTime, 0, ruleKey, algorithm);
    }
    
    public static RateLimitResponse denied(long retryAfter, String ruleKey, String algorithm) {
        return new RateLimitResponse(false, 0, 0, retryAfter, ruleKey, algorithm);
    }
}