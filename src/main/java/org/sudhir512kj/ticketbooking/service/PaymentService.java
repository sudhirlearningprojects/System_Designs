package org.sudhir512kj.ticketbooking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {
    
    public boolean processPayment(String paymentId, BigDecimal amount) {
        // Simulate payment processing
        // In real implementation, integrate with payment gateway
        try {
            Thread.sleep(100); // Simulate network call
            return true; // Assume payment succeeds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}