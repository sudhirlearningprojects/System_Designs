package org.sudhir512kj.parkinglot.service;

import org.springframework.stereotype.Service;
import org.sudhir512kj.parkinglot.model.Ticket;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PaymentService {
    
    private static final double HOURLY_RATE = 5.0;
    
    public double calculateFee(Ticket ticket) {
        LocalDateTime exitTime = LocalDateTime.now();
        Duration duration = Duration.between(ticket.getEntryTime(), exitTime);
        long hours = duration.toHours();
        if (duration.toMinutes() % 60 > 0) hours++; // Round up
        return Math.max(hours * HOURLY_RATE, HOURLY_RATE);
    }
    
    public boolean processPayment(String ticketId, double amount, String paymentType) {
        // Circuit breaker pattern would be implemented here
        try {
            PaymentMethod method = createPaymentMethod(paymentType);
            return method.process(amount);
        } catch (Exception e) {
            // Fallback to manual processing
            return false;
        }
    }
    
    private PaymentMethod createPaymentMethod(String type) {
        switch (type) {
            case "CREDIT_CARD": return new CreditCardPayment();
            case "CASH": return new CashPayment();
            default: return new CreditCardPayment();
        }
    }
}

interface PaymentMethod {
    boolean process(double amount);
}

class CreditCardPayment implements PaymentMethod {
    public boolean process(double amount) {
        // Integration with payment processor
        return true;
    }
}

class CashPayment implements PaymentMethod {
    public boolean process(double amount) {
        return true;
    }
}