package org.sudhir512kj.uber.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.uber.dto.PaymentRequest;
import org.sudhir512kj.uber.model.Payment;
import org.sudhir512kj.uber.service.PaymentProcessingService;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    private final PaymentProcessingService paymentService;
    
    public PaymentController(PaymentProcessingService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processRidePayment(request.getRideId(), request.getPaymentMethod());
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<Payment> getPaymentByRide(@PathVariable UUID rideId) {
        return paymentService.getPaymentByRideId(rideId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{rideId}")
    public ResponseEntity<Payment.PaymentStatus> getPaymentStatus(@PathVariable UUID rideId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(rideId));
    }
}
