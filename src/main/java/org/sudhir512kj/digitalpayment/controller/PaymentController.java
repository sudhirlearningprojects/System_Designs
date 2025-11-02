package org.sudhir512kj.digitalpayment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.digitalpayment.dto.*;
import org.sudhir512kj.digitalpayment.model.Transaction;
import org.sudhir512kj.digitalpayment.repository.TransactionRepository;
import org.sudhir512kj.digitalpayment.service.LedgerService;
import org.sudhir512kj.digitalpayment.service.PaymentService;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private LedgerService ledgerService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(@RequestBody PaymentInitiationRequest request) {
        try {
            PaymentInitiationResponse response = paymentService.initiatePayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new PaymentInitiationResponse(null, "FAILED", "Payment initiation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<TransactionStatusResponse> getTransactionStatus(@PathVariable String transactionId) {
        TransactionStatusResponse response = paymentService.getTransactionStatus(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody PaymentCallbackRequest callback) {
        try {
            paymentService.processCallback(callback);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Callback processing failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/balance/{userId}")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable String userId) {
        BigDecimal balance = ledgerService.getWalletBalance(userId);
        return ResponseEntity.ok(balance);
    }
    
    @GetMapping("/history/{userId}")
    public ResponseEntity<Page<Transaction>> getTransactionHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Transaction> transactions = transactionRepository
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(transactions);
    }
}