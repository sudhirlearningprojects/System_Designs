package org.sudhir512kj.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.payment.dto.PaymentRequest;
import org.sudhir512kj.payment.dto.PaymentResponse;
import org.sudhir512kj.payment.service.PaymentService;
import org.sudhir512kj.payment.service.IdempotencyService;
import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        // Validate idempotency key
        if (!idempotencyService.isValidIdempotencyKey(idempotencyKey)) {
            return ResponseEntity.badRequest().build();
        }
        
        PaymentResponse response = paymentService.processPayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransactionStatus(
            @PathVariable UUID transactionId) {
        
        PaymentResponse response = paymentService.getTransactionStatus(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> processRefund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        if (!idempotencyService.isValidIdempotencyKey(idempotencyKey)) {
            return ResponseEntity.badRequest().build();
        }
        
        // Refund processing logic would go here
        return ResponseEntity.ok().build();
    }
    
    public static class RefundRequest {
        public UUID originalTransactionId;
        public java.math.BigDecimal amount;
        public String reason;
    }
}