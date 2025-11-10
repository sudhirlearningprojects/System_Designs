package org.sudhir512kj.ratelimiter.dto;

public class RateLimitResponse {
    private boolean allowed;
    private int remainingRequests;
    private long resetTimeEpoch;
    private long retryAfterSeconds;
    private String ruleKey;
    private String algorithm;
    
    public RateLimitResponse() {}
    
    public RateLimitResponse(boolean allowed, int remainingRequests, long resetTimeEpoch, 
                            long retryAfterSeconds, String ruleKey, String algorithm) {
        this.allowed = allowed;
        this.remainingRequests = remainingRequests;
        this.resetTimeEpoch = resetTimeEpoch;
        this.retryAfterSeconds = retryAfterSeconds;
        this.ruleKey = ruleKey;
        this.algorithm = algorithm;
    }
    
    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }
    
    public int getRemainingRequests() { return remainingRequests; }
    public void setRemainingRequests(int remainingRequests) { this.remainingRequests = remainingRequests; }
    
    public long getResetTimeEpoch() { return resetTimeEpoch; }
    public void setResetTimeEpoch(long resetTimeEpoch) { this.resetTimeEpoch = resetTimeEpoch; }
    
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
    
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    
    public static RateLimitResponse allowed(int remaining, long resetTime, String ruleKey, String algorithm) {
        return new RateLimitResponse(true, remaining, resetTime, 0, ruleKey, algorithm);
    }
    
    public static RateLimitResponse denied(long retryAfter, String ruleKey, String algorithm) {
        return new RateLimitResponse(false, 0, 0, retryAfter, ruleKey, algorithm);
    }
}