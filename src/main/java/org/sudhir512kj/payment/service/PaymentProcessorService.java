package org.sudhir512kj.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sudhir512kj.payment.model.PaymentTransaction;

@Service
public class PaymentProcessorService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public String processPayment(PaymentTransaction transaction) {
        System.out.println("Processing payment with processor: " + transaction.getProcessor() + " for transaction: " + transaction.getId());
        
        try {
            switch (transaction.getProcessor()) {
                case "STRIPE": return processWithStripe(transaction);
                case "PAYPAL": return processWithPayPal(transaction);
                case "SQUARE": return processWithSquare(transaction);
                default: throw new IllegalArgumentException("Unsupported processor: " + transaction.getProcessor());
            }
        } catch (Exception e) {
            System.err.println("Payment processing failed for transaction: " + transaction.getId() + ", error: " + e.getMessage());
            throw new RuntimeException("External processor error: " + e.getMessage(), e);
        }
    }
    
    private String processWithStripe(PaymentTransaction transaction) {
        // Simulate Stripe API call
        System.out.println("Processing with Stripe for amount: " + transaction.getAmount() + " " + transaction.getCurrency());
        
        // In real implementation, this would call Stripe API
        if (Math.random() > 0.1) { // 90% success rate simulation
            return "pi_" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("Stripe processing failed");
        }
    }
    
    private String processWithPayPal(PaymentTransaction transaction) {
        // Simulate PayPal API call
        System.out.println("Processing with PayPal for amount: " + transaction.getAmount() + " " + transaction.getCurrency());
        
        if (Math.random() > 0.15) { // 85% success rate simulation
            return "PAYID-" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("PayPal processing failed");
        }
    }
    
    private String processWithSquare(PaymentTransaction transaction) {
        // Simulate Square API call
        System.out.println("Processing with Square for amount: " + transaction.getAmount() + " " + transaction.getCurrency());
        
        if (Math.random() > 0.2) { // 80% success rate simulation
            return "sq_" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("Square processing failed");
        }
    }
    
    private String getFallbackProcessor(String primaryProcessor) {
        switch (primaryProcessor) {
            case "STRIPE": return "PAYPAL";
            case "PAYPAL": return "SQUARE";
            case "SQUARE": return "STRIPE";
            default: return null;
        }
    }
}