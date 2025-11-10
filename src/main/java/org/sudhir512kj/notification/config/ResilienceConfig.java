package org.sudhir512kj.notification.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker emailCircuitBreaker() {
        return CircuitBreaker.of("email-provider", CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .permittedNumberOfCallsInHalfOpenState(5)
            .build());
    }
    
    @Bean
    public CircuitBreaker smsCircuitBreaker() {
        return CircuitBreaker.of("sms-provider", CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .build());
    }
    
    @Bean
    public RateLimiter emailRateLimiter() {
        return RateLimiter.of("email-rate-limiter", RateLimiterConfig.custom()
            .limitForPeriod(1000)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(100))
            .build());
    }
    
    @Bean
    public RateLimiter smsRateLimiter() {
        return RateLimiter.of("sms-rate-limiter", RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(100))
            .build());
    }
}
