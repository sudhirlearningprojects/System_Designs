package org.sudhir512kj.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.payment.model.PaymentTransaction;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditService {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void logPaymentSuccess(PaymentTransaction transaction) {
        Map<String, Object> auditEvent = createBaseAuditEvent(transaction);
        auditEvent.put("eventType", "PAYMENT_SUCCESS");
        auditEvent.put("processorTransactionId", transaction.getProcessorTransactionId());
        auditEvent.put("completedAt", transaction.getCompletedAt());
        
        kafkaTemplate.send("payment-audit-events", transaction.getId().toString(), auditEvent);
        
        System.out.println("Audit log created for successful payment: " + transaction.getId());
    }
    
    public void logPaymentFailure(PaymentTransaction transaction, Exception error) {
        Map<String, Object> auditEvent = createBaseAuditEvent(transaction);
        auditEvent.put("eventType", "PAYMENT_FAILURE");
        auditEvent.put("errorMessage", error.getMessage());
        auditEvent.put("errorClass", error.getClass().getSimpleName());
        
        kafkaTemplate.send("payment-audit-events", transaction.getId().toString(), auditEvent);
        
        System.out.println("Audit log created for failed payment: " + transaction.getId());
    }
    
    public void logRefund(PaymentTransaction originalTransaction, PaymentTransaction refundTransaction) {
        Map<String, Object> auditEvent = createBaseAuditEvent(refundTransaction);
        auditEvent.put("eventType", "REFUND_PROCESSED");
        auditEvent.put("originalTransactionId", originalTransaction.getId());
        auditEvent.put("refundAmount", refundTransaction.getAmount());
        
        kafkaTemplate.send("payment-audit-events", refundTransaction.getId().toString(), auditEvent);
        
        System.out.println("Audit log created for refund: " + refundTransaction.getId());
    }
    
    public void logSecurityEvent(String eventType, String details, String userId) {
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", eventType);
        securityEvent.put("details", details);
        securityEvent.put("userId", userId);
        securityEvent.put("timestamp", LocalDateTime.now());
        securityEvent.put("severity", "HIGH");
        
        kafkaTemplate.send("security-audit-events", userId, securityEvent);
        
        System.out.println("Security audit event logged: " + eventType + " for user: " + userId);
    }
    
    private Map<String, Object> createBaseAuditEvent(PaymentTransaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("transactionId", transaction.getId());
        event.put("merchantId", transaction.getMerchantId());
        event.put("userId", transaction.getUserId());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("paymentMethod", transaction.getPaymentMethod());
        event.put("processor", transaction.getProcessor());
        event.put("status", transaction.getStatus());
        event.put("timestamp", LocalDateTime.now());
        return event;
    }
}