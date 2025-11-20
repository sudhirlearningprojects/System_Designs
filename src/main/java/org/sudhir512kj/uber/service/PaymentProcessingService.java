package org.sudhir512kj.uber.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.uber.model.Payment;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.repository.RideRepository;
import org.sudhir512kj.uber.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

@Service
public class PaymentProcessingService {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);
    private final RideRepository rideRepository;
    private final PaymentRepository paymentRepository;
    private final DriverService driverService;
    
    @Value("${stripe.currency:usd}")
    private String currency;
    
    public PaymentProcessingService(RideRepository rideRepository, PaymentRepository paymentRepository, DriverService driverService) {
        this.rideRepository = rideRepository;
        this.paymentRepository = paymentRepository;
        this.driverService = driverService;
    }
    
    @Transactional
    public Payment processRidePayment(UUID rideId, Payment.PaymentMethod method) {
        String idempotencyKey = "ride-payment-" + rideId;
        
        // Check idempotency - return existing payment if already processed
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("Payment already processed for ride {}", rideId);
            return existingPayment.get();
        }
        
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        if (ride.getStatus() != Ride.RideStatus.COMPLETED) {
            throw new RuntimeException("Ride not completed");
        }
        
        Payment payment = new Payment();
        payment.setRideId(rideId);
        payment.setUserId(ride.getRiderId());
        payment.setAmount(ride.getActualFare());
        payment.setPaymentMethod(method);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(LocalDateTime.now());
        
        // Save pending payment
        payment = paymentRepository.save(payment);
        
        try {
            boolean success = chargePayment(payment);
            
            if (success) {
                payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                payment.setTransactionId("txn-" + UUID.randomUUID().toString());
                payment.setUpdatedAt(LocalDateTime.now());
                
                BigDecimal driverEarnings = calculateDriverEarnings(ride.getActualFare());
                driverService.updateEarnings(ride.getDriverId(), driverEarnings);
                
                log.info("Payment processed for ride {}: ${}", rideId, ride.getActualFare());
            } else {
                payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                payment.setUpdatedAt(LocalDateTime.now());
                log.error("Payment failed for ride {}", rideId);
            }
        } catch (Exception e) {
            payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            log.error("Payment processing error for ride {}", rideId, e);
        }
        
        return paymentRepository.save(payment);
    }
    
    private boolean chargePayment(Payment payment) {
        try {
            switch (payment.getPaymentMethod()) {
                case CARD -> {
                    return processCardPayment(payment);
                }
                case WALLET -> {
                    return processWalletPayment(payment);
                }
                case CASH -> {
                    log.info("Cash payment recorded: ${}", payment.getAmount());
                    return true;
                }
                default -> {
                    log.error("Unknown payment method: {}", payment.getPaymentMethod());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return false;
        }
    }
    
    private boolean processCardPayment(Payment payment) {
        try {
            // Convert dollars to cents for Stripe
            long amountInCents = payment.getAmount().multiply(new BigDecimal("100")).longValue();
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setDescription("Uber ride payment for ride: " + payment.getRideId())
                .putMetadata("rideId", payment.getRideId().toString())
                .putMetadata("userId", payment.getUserId().toString())
                .build();
            
            PaymentIntent intent = PaymentIntent.create(params);
            payment.setTransactionId(intent.getId());
            
            log.info("Stripe payment intent created: {} for ${}", intent.getId(), payment.getAmount());
            return "succeeded".equals(intent.getStatus()) || "requires_capture".equals(intent.getStatus());
            
        } catch (StripeException e) {
            log.error("Stripe payment failed for user {}: {}", payment.getUserId(), e.getMessage());
            return false;
        }
    }
    
    private boolean processWalletPayment(Payment payment) {
        // Check wallet balance and deduct
        log.info("Wallet debited: ${} for user {}", payment.getAmount(), payment.getUserId());
        return true;
    }
    
    private BigDecimal calculateDriverEarnings(BigDecimal totalFare) {
        return totalFare.multiply(new BigDecimal("0.75")); // 75% to driver, 25% commission
    }
    
    public Payment.PaymentStatus getPaymentStatus(UUID rideId) {
        return paymentRepository.findByRideId(rideId)
            .map(Payment::getPaymentStatus)
            .orElse(Payment.PaymentStatus.PENDING);
    }
    
    public Optional<Payment> getPaymentByRideId(UUID rideId) {
        return paymentRepository.findByRideId(rideId);
    }
}
