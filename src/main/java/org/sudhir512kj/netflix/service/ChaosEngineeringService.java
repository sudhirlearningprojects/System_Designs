package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChaosEngineeringService {
    private final Random random = new Random();
    private final AtomicBoolean chaosEnabled = new AtomicBoolean(false);
    
    public void enableChaos() {
        chaosEnabled.set(true);
    }
    
    public void disableChaos() {
        chaosEnabled.set(false);
    }
    
    public void injectLatency(String service, int maxDelayMs) {
        if (!chaosEnabled.get() || random.nextDouble() >= 0.1) return;
        
        // Non-blocking delay simulation
        long delay = random.nextInt(Math.min(maxDelayMs, 100)); // Cap at 100ms
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void injectFailure(String service, double failureRate) {
        if (chaosEnabled.get() && random.nextDouble() < failureRate) {
            throw new RuntimeException("Chaos: Simulated failure in " + service);
        }
    }
    
    public boolean shouldCircuitBreak(String service) {
        return chaosEnabled.get() && random.nextDouble() < 0.05;
    }
}