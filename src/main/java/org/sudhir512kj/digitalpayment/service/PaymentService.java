package org.sudhir512kj.digitalpayment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.digitalpayment.dto.*;
import org.sudhir512kj.digitalpayment.model.*;
import org.sudhir512kj.digitalpayment.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private PaymentGatewayFactory gatewayFactory;
    
    @Autowired
    private LedgerService ledgerService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private FraudDetectionService fraudDetectionService;
    
    @Transactional
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request) {
        // Check idempotency
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null) {
            Object cachedResult = redisTemplate.opsForValue().get("idempotency:" + idempotencyKey);
            if (cachedResult != null) {
                return (PaymentInitiationResponse) cachedResult;
            }
        }
        
        // Fraud detection
        if (fraudDetectionService.isSuspicious(request)) {
            return new PaymentInitiationResponse(null, "FAILED", "Transaction blocked by fraud detection");
        }
        
        // Create transaction
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(
            transactionId,
            request.getSenderId(),
            request.getReceiverId(),
            request.getAmount(),
            TransactionType.valueOf(request.getType()),
            PaymentMethod.valueOf(request.getPaymentMethod()),
            idempotencyKey
        );
        
        transaction.setDescription(request.getDescription());
        transactionRepository.save(transaction);
        
        // Process payment based on method
        PaymentResponse pspResponse;
        if ("WALLET".equals(request.getPaymentMethod())) {
            pspResponse = processWalletPayment(request);
        } else {
            PaymentGatewayStrategy gateway = gatewayFactory.getPaymentGateway(request.getPaymentMethod());
            PaymentRequest pspRequest = new PaymentRequest(request.getAmount(), request.getPaymentMethod(), transactionId);
            pspResponse = gateway.processPayment(pspRequest);
        }
        
        // Update transaction status
        transaction.setPspTransactionId(pspResponse.getPspTransactionId());
        transaction.setStatus(TransactionStatus.valueOf(pspResponse.getStatus()));
        transactionRepository.save(transaction);
        
        PaymentInitiationResponse response = new PaymentInitiationResponse(
            transactionId, pspResponse.getStatus(), pspResponse.getMessage());
        
        // Cache result for idempotency
        if (idempotencyKey != null) {
            redisTemplate.opsForValue().set("idempotency:" + idempotencyKey, response, 24, TimeUnit.HOURS);
        }
        
        return response;
    }
    
    private PaymentResponse processWalletPayment(PaymentInitiationRequest request) {
        try {
            boolean success = ledgerService.transferFunds(
                request.getSenderId(), 
                request.getReceiverId(), 
                request.getAmount()
            );
            
            if (success) {
                return new PaymentResponse("WALLET_" + System.currentTimeMillis(), "SUCCESS", "Wallet transfer completed");
            } else {
                return new PaymentResponse(null, "FAILED", "Insufficient wallet balance");
            }
        } catch (Exception e) {
            return new PaymentResponse(null, "FAILED", "Wallet transfer failed: " + e.getMessage());
        }
    }
    
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            return new TransactionStatusResponse(transactionId, "NOT_FOUND", "Transaction not found");
        }
        
        return new TransactionStatusResponse(
            transactionId, 
            transaction.getStatus().toString(), 
            "Transaction " + transaction.getStatus().toString().toLowerCase()
        );
    }
    
    @Transactional
    public void processCallback(PaymentCallbackRequest callback) {
        Transaction transaction = transactionRepository.findById(callback.getTransactionId()).orElse(null);
        if (transaction != null) {
            transaction.setStatus(TransactionStatus.valueOf(callback.getStatus()));
            transaction.setPspTransactionId(callback.getPspTransactionId());
            transactionRepository.save(transaction);
        }
    }
}