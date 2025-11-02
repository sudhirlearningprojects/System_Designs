package org.sudhir512kj.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.payment.dto.PaymentRequest;
import org.sudhir512kj.payment.dto.PaymentResponse;
import org.sudhir512kj.payment.service.PaymentService;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        try {
            PaymentResponse response = paymentService.processPayment(request, idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setTransactionId(null);
            errorResponse.setFailureReason(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransactionStatus(@PathVariable UUID transactionId) {
        try {
            PaymentResponse response = paymentService.getTransactionStatus(transactionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment service is healthy");
    }
}