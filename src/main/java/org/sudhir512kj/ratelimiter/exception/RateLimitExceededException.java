package org.sudhir512kj.ratelimiter.exception;

public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfter;
    private final String ruleKey;
    
    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfter = 60;
        this.ruleKey = "unknown";
    }
    
    public RateLimitExceededException(String message, long retryAfter, String ruleKey) {
        super(message);
        this.retryAfter = retryAfter;
        this.ruleKey = ruleKey;
    }
    
    public long getRetryAfter() {
        return retryAfter;
    }
    
    public String getRuleKey() {
        return ruleKey;
    }
}