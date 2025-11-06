package org.sudhir512kj.ratelimiter.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitConfig {
    private String ruleKey;
    private Integer requestsPerWindow;
    private Integer windowSizeSeconds;
    private Integer burstCapacity;
    private Double refillRate;
    private Boolean enabled = true;
}