package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CircuitBreakerService {
    
    private enum State { CLOSED, OPEN, HALF_OPEN }
    
    private static class CircuitBreaker {
        private State state = State.CLOSED;
        private AtomicInteger failureCount = new AtomicInteger(0);
        private Instant lastFailureTime = Instant.now();
        private final int failureThreshold = 5;
        private final long timeoutMs = 60000; // 1 minute
    }
    
    private final Map<String, CircuitBreaker> circuits = new ConcurrentHashMap<>();
    
    public boolean isCallAllowed(String serviceName) {
        CircuitBreaker circuit = circuits.computeIfAbsent(serviceName, k -> new CircuitBreaker());
        
        switch (circuit.state) {
            case CLOSED:
                return true;
            case OPEN:
                if (Instant.now().toEpochMilli() - circuit.lastFailureTime.toEpochMilli() > circuit.timeoutMs) {
                    circuit.state = State.HALF_OPEN;
                    return true;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return false;
        }
    }
    
    public void recordSuccess(String serviceName) {
        CircuitBreaker circuit = circuits.get(serviceName);
        if (circuit != null) {
            circuit.failureCount.set(0);
            circuit.state = State.CLOSED;
        }
    }
    
    public void recordFailure(String serviceName) {
        CircuitBreaker circuit = circuits.computeIfAbsent(serviceName, k -> new CircuitBreaker());
        circuit.lastFailureTime = Instant.now();
        
        if (circuit.failureCount.incrementAndGet() >= circuit.failureThreshold) {
            circuit.state = State.OPEN;
        }
    }
}