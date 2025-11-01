package org.sudhir512kj.payment.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
public class CircuitBreakerService {
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(name, this::createCircuitBreaker);
    }
    
    private CircuitBreaker createCircuitBreaker(String name) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate threshold
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s in open state
            .slidingWindowSize(10) // Consider last 10 calls
            .minimumNumberOfCalls(5) // Minimum 5 calls before calculating failure rate
            .build();
        
        CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Circuit breaker {} transitioned from {} to {}", 
                    name, event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()));
        
        return circuitBreaker;
    }
}