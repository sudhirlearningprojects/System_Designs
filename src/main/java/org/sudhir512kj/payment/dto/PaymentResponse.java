package org.sudhir512kj.payment.dto;

import org.sudhir512kj.payment.model.PaymentTransaction.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponse {
    private UUID transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String processorTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    // Getters and setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getProcessorTransactionId() { return processorTransactionId; }
    public void setProcessorTransactionId(String processorTransactionId) { this.processorTransactionId = processorTransactionId; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}