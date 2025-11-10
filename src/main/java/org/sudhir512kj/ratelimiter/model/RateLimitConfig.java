package org.sudhir512kj.ratelimiter.model;

public class RateLimitConfig {
    private String ruleKey;
    private Integer requestsPerWindow;
    private Integer windowSizeSeconds;
    private Integer burstCapacity;
    private Double refillRate;
    private Boolean enabled = true;
    
    public RateLimitConfig() {}
    
    public RateLimitConfig(String ruleKey, Integer requestsPerWindow, Integer windowSizeSeconds,
                          Integer burstCapacity, Double refillRate, Boolean enabled) {
        this.ruleKey = ruleKey;
        this.requestsPerWindow = requestsPerWindow;
        this.windowSizeSeconds = windowSizeSeconds;
        this.burstCapacity = burstCapacity;
        this.refillRate = refillRate;
        this.enabled = enabled;
    }
    
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    
    public Integer getRequestsPerWindow() { return requestsPerWindow; }
    public void setRequestsPerWindow(Integer requestsPerWindow) { this.requestsPerWindow = requestsPerWindow; }
    
    public Integer getWindowSizeSeconds() { return windowSizeSeconds; }
    public void setWindowSizeSeconds(Integer windowSizeSeconds) { this.windowSizeSeconds = windowSizeSeconds; }
    
    public Integer getBurstCapacity() { return burstCapacity; }
    public void setBurstCapacity(Integer burstCapacity) { this.burstCapacity = burstCapacity; }
    
    public Double getRefillRate() { return refillRate; }
    public void setRefillRate(Double refillRate) { this.refillRate = refillRate; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}