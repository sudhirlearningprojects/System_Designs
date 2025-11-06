package org.sudhir512kj.ratelimiter.algorithm;

import org.sudhir512kj.ratelimiter.dto.RateLimitResponse;
import org.sudhir512kj.ratelimiter.model.RateLimitConfig;

public interface RateLimitAlgorithm {
    RateLimitResponse checkRateLimit(String key, RateLimitConfig config);
    void resetRateLimit(String key);
    String getAlgorithmName();
}