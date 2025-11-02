package org.sudhir512kj.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.payment.dto.PaymentRequest;
import org.sudhir512kj.payment.dto.PaymentResponse;
import org.sudhir512kj.payment.model.PaymentTransaction;
import org.sudhir512kj.payment.model.PaymentTransaction.PaymentStatus;
import org.sudhir512kj.payment.repository.PaymentTransactionRepository;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    
    @Autowired
    private PaymentTransactionRepository transactionRepository;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    @Autowired
    private PaymentProcessorService processorService;
    
    @Autowired
    private TransactionManagerService transactionManager;
    
    @Autowired
    private AuditService auditService;
    
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        System.out.println("Processing payment with idempotency key: " + idempotencyKey);
        
        // Check for duplicate request
        PaymentResponse cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);
        if (cachedResponse != null) {
            System.out.println("Returning cached response for idempotency key: " + idempotencyKey);
            return cachedResponse;
        }
        
        // Create transaction record
        PaymentTransaction transaction = createTransaction(request, idempotencyKey);
        transaction = transactionRepository.save(transaction);
        
        try {
            // Process with external processor
            transaction.setStatus(PaymentStatus.PROCESSING);
            transactionRepository.save(transaction);
            
            String processorTxnId = processorService.processPayment(transaction);
            
            // Update transaction on success
            transaction.setProcessorTransactionId(processorTxnId);
            transaction.setStatus(PaymentStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            
            PaymentResponse response = mapToResponse(transaction);
            
            // Cache successful response
            idempotencyService.cacheResponse(idempotencyKey, response, transaction.getId());
            
            // Audit log
            auditService.logPaymentSuccess(transaction);
            
            System.out.println("Payment completed successfully: " + transaction.getId());
            return response;
            
        } catch (Exception e) {
            System.err.println("Payment processing failed for transaction: " + transaction.getId() + ", error: " + e.getMessage());
            
            // Update transaction on failure
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            
            // Schedule retry if applicable
            transactionManager.scheduleRetry(transaction, e);
            
            // Audit log
            auditService.logPaymentFailure(transaction, e);
            
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }
    
    public PaymentResponse getTransactionStatus(UUID transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        return mapToResponse(transaction);
    }
    
    private PaymentTransaction createTransaction(PaymentRequest request, String idempotencyKey) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setMerchantId(request.getMerchantId());
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentMethod(request.getPaymentMethod().getType());
        transaction.setProcessor(determineProcessor(request));
        transaction.setStatus(PaymentStatus.PENDING);
        return transaction;
    }
    
    private String determineProcessor(PaymentRequest request) {
        // Logic to select appropriate payment processor
        return "STRIPE"; // Simplified
    }
    
    private PaymentResponse mapToResponse(PaymentTransaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transaction.getId());
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setProcessorTransactionId(transaction.getProcessorTransactionId());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());
        return response;
    }
}