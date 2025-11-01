package org.sudhir512kj.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sudhir512kj.payment.model.PaymentTransaction;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessorService {
    private final RestTemplate restTemplate;
    private final CircuitBreakerService circuitBreakerService;
    
    @CircuitBreaker(name = "payment-processor", fallbackMethod = "fallbackProcessor")
    @TimeLimiter(name = "payment-processor")
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, random = true)
    )
    public String processPayment(PaymentTransaction transaction) {
        log.info("Processing payment with processor: {} for transaction: {}", 
                transaction.getProcessor(), transaction.getId());
        
        try {
            return switch (transaction.getProcessor()) {
                case "STRIPE" -> processWithStripe(transaction);
                case "PAYPAL" -> processWithPayPal(transaction);
                case "SQUARE" -> processWithSquare(transaction);
                default -> throw new IllegalArgumentException("Unsupported processor: " + transaction.getProcessor());
            };
        } catch (Exception e) {
            log.error("Payment processing failed for transaction: {}", transaction.getId(), e);
            throw new RuntimeException("External processor error: " + e.getMessage(), e);
        }
    }
    
    private String processWithStripe(PaymentTransaction transaction) {
        // Simulate Stripe API call
        log.info("Processing with Stripe for amount: {} {}", 
                transaction.getAmount(), transaction.getCurrency());
        
        // In real implementation, this would call Stripe API
        if (Math.random() > 0.1) { // 90% success rate simulation
            return "pi_" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("Stripe processing failed");
        }
    }
    
    private String processWithPayPal(PaymentTransaction transaction) {
        // Simulate PayPal API call
        log.info("Processing with PayPal for amount: {} {}", 
                transaction.getAmount(), transaction.getCurrency());
        
        if (Math.random() > 0.15) { // 85% success rate simulation
            return "PAYID-" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("PayPal processing failed");
        }
    }
    
    private String processWithSquare(PaymentTransaction transaction) {
        // Simulate Square API call
        log.info("Processing with Square for amount: {} {}", 
                transaction.getAmount(), transaction.getCurrency());
        
        if (Math.random() > 0.2) { // 80% success rate simulation
            return "sq_" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("Square processing failed");
        }
    }
    
    // Fallback method when circuit breaker is open
    public String fallbackProcessor(PaymentTransaction transaction, Exception ex) {
        log.warn("Using fallback processor for transaction: {} due to: {}", 
                transaction.getId(), ex.getMessage());
        
        // Try alternative processor
        String originalProcessor = transaction.getProcessor();
        String fallbackProcessor = getFallbackProcessor(originalProcessor);
        
        if (fallbackProcessor != null) {
            transaction.setProcessor(fallbackProcessor);
            return processPayment(transaction);
        }
        
        throw new RuntimeException("All payment processors are unavailable");
    }
    
    private String getFallbackProcessor(String primaryProcessor) {
        return switch (primaryProcessor) {
            case "STRIPE" -> "PAYPAL";
            case "PAYPAL" -> "SQUARE";
            case "SQUARE" -> "STRIPE";
            default -> null;
        };
    }
}