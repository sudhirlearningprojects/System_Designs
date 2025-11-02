package org.sudhir512kj.payment.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class CircuitBreakerService {
    private final Map<String, Boolean> circuitStates = new ConcurrentHashMap<>();
    
    public boolean isCircuitOpen(String name) {
        return circuitStates.getOrDefault(name, false);
    }
    
    public void openCircuit(String name) {
        circuitStates.put(name, true);
        System.out.println("Circuit breaker " + name + " opened");
    }
    
    public void closeCircuit(String name) {
        circuitStates.put(name, false);
        System.out.println("Circuit breaker " + name + " closed");
    }
}