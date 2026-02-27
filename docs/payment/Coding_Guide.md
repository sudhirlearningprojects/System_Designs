# Payment Service - Complete Coding Guide

## System Design Overview

**Problem**: Process payments with exactly-once guarantee

**Core Features**:
1. Idempotent payment processing
2. Retry with circuit breaker
3. Transaction management
4. Refunds

## SOLID Principles

- **SRP**: Payment, Transaction, Gateway separate
- **OCP**: Add new payment gateways without modifying
- **DIP**: Depend on PaymentGateway interface

## Design Patterns

1. **Strategy Pattern**: Different payment gateways
2. **Circuit Breaker**: Fault tolerance
3. **Saga Pattern**: Distributed transactions

## Complete Implementation

```java
import java.util.*;
import java.time.LocalDateTime;

enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
enum GatewayType { STRIPE, PAYPAL, SQUARE }

class Payment {
    String id, userId, orderId;
    double amount;
    PaymentStatus status;
    GatewayType gateway;
    LocalDateTime createdAt;
    int retryCount = 0;
    
    Payment(String userId, String orderId, double amount, GatewayType gateway) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.gateway = gateway;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}

interface PaymentGateway {
    boolean processPayment(Payment payment) throws Exception;
    boolean refund(Payment payment) throws Exception;
    GatewayType getType();
}

class StripeGateway implements PaymentGateway {
    public boolean processPayment(Payment payment) throws Exception {
        System.out.println("  [STRIPE] Processing $" + payment.amount);
        if (Math.random() > 0.8) throw new Exception("Network timeout");
        return true;
    }
    
    public boolean refund(Payment payment) throws Exception {
        System.out.println("  [STRIPE] Refunding $" + payment.amount);
        return true;
    }
    
    public GatewayType getType() { return GatewayType.STRIPE; }
}

class PayPalGateway implements PaymentGateway {
    public boolean processPayment(Payment payment) throws Exception {
        System.out.println("  [PAYPAL] Processing $" + payment.amount);
        if (Math.random() > 0.85) throw new Exception("PayPal error");
        return true;
    }
    
    public boolean refund(Payment payment) throws Exception {
        System.out.println("  [PAYPAL] Refunding $" + payment.amount);
        return true;
    }
    
    public GatewayType getType() { return GatewayType.PAYPAL; }
}

class CircuitBreaker {
    private int failureCount = 0;
    private int threshold = 3;
    private boolean open = false;
    private LocalDateTime lastFailure;
    
    public boolean isOpen() {
        if (open && lastFailure != null) {
            if (LocalDateTime.now().minusSeconds(30).isAfter(lastFailure)) {
                reset();
            }
        }
        return open;
    }
    
    public void recordSuccess() {
        failureCount = 0;
        open = false;
    }
    
    public void recordFailure() {
        failureCount++;
        lastFailure = LocalDateTime.now();
        if (failureCount >= threshold) {
            open = true;
            System.out.println("  ⚠ Circuit breaker OPEN");
        }
    }
    
    private void reset() {
        failureCount = 0;
        open = false;
        System.out.println("  ✓ Circuit breaker CLOSED");
    }
}

class PaymentService {
    private Map<GatewayType, PaymentGateway> gateways = new HashMap<>();
    private Map<String, Payment> payments = new HashMap<>();
    private Map<String, String> idempotencyKeys = new HashMap<>();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private static final int MAX_RETRIES = 3;
    
    public PaymentService() {
        registerGateway(new StripeGateway());
        registerGateway(new PayPalGateway());
    }
    
    private void registerGateway(PaymentGateway gateway) {
        gateways.put(gateway.getType(), gateway);
    }
    
    public Payment processPayment(String userId, String orderId, double amount, 
                                   GatewayType gatewayType, String idempotencyKey) {
        
        // Check idempotency
        if (idempotencyKeys.containsKey(idempotencyKey)) {
            String paymentId = idempotencyKeys.get(idempotencyKey);
            System.out.println("Duplicate request detected, returning existing payment: " + paymentId);
            return payments.get(paymentId);
        }
        
        System.out.println("\n=== Processing Payment ===");
        System.out.println("Order: " + orderId + " | Amount: $" + amount);
        
        Payment payment = new Payment(userId, orderId, amount, gatewayType);
        payments.put(payment.id, payment);
        idempotencyKeys.put(idempotencyKey, payment.id);
        
        // Process with retry
        PaymentGateway gateway = gateways.get(gatewayType);
        
        while (payment.retryCount < MAX_RETRIES) {
            if (circuitBreaker.isOpen()) {
                System.out.println("  ✗ Circuit breaker open, payment failed");
                payment.status = PaymentStatus.FAILED;
                return payment;
            }
            
            try {
                if (gateway.processPayment(payment)) {
                    payment.status = PaymentStatus.SUCCESS;
                    circuitBreaker.recordSuccess();
                    System.out.println("  ✓ Payment successful: " + payment.id);
                    return payment;
                }
            } catch (Exception e) {
                payment.retryCount++;
                circuitBreaker.recordFailure();
                System.out.println("  ✗ Attempt " + payment.retryCount + " failed: " + e.getMessage());
                
                if (payment.retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000 * payment.retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        payment.status = PaymentStatus.FAILED;
        System.out.println("  ✗ Payment failed after " + MAX_RETRIES + " attempts");
        return payment;
    }
    
    public boolean refund(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null || payment.status != PaymentStatus.SUCCESS) {
            return false;
        }
        
        System.out.println("\n=== Processing Refund ===");
        PaymentGateway gateway = gateways.get(payment.gateway);
        
        try {
            if (gateway.refund(payment)) {
                payment.status = PaymentStatus.REFUNDED;
                System.out.println("  ✓ Refund successful");
                return true;
            }
        } catch (Exception e) {
            System.out.println("  ✗ Refund failed: " + e.getMessage());
        }
        
        return false;
    }
}

public class PaymentDemo {
    public static void main(String[] args) {
        System.out.println("=== Payment Service ===");
        
        PaymentService service = new PaymentService();
        
        // Process payments
        Payment p1 = service.processPayment("user1", "order1", 99.99, 
            GatewayType.STRIPE, "idempotency-key-1");
        
        // Duplicate request (idempotency)
        Payment p2 = service.processPayment("user1", "order1", 99.99, 
            GatewayType.STRIPE, "idempotency-key-1");
        
        // Another payment
        Payment p3 = service.processPayment("user2", "order2", 149.99, 
            GatewayType.PAYPAL, "idempotency-key-2");
        
        // Refund
        if (p1.status == PaymentStatus.SUCCESS) {
            service.refund(p1.id);
        }
    }
}
```

## Key Concepts

**Idempotency**:
- Store idempotency key
- Return same result for duplicate requests
- Prevent double charging

**Circuit Breaker**:
- Open after N failures
- Half-open after timeout
- Prevent cascading failures

**Retry Strategy**:
- Exponential backoff
- Max retries: 3
- Idempotent operations only

## Interview Questions

**Q: Exactly-once processing?**
A: Idempotency keys + database transactions

**Q: Handle gateway failures?**
A: Circuit breaker, retry with backoff, fallback gateway

**Q: Distributed transactions?**
A: Saga pattern with compensating transactions

**Q: Scale to 100K TPS?**
A: Async processing, message queue, database sharding

Run: https://www.jdoodle.com/online-java-compiler
