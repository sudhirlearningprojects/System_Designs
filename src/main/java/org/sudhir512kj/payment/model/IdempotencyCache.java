package org.sudhir512kj.payment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_idempotency_cache", indexes = {
    @Index(name = "idx_idempotency_expires", columnList = "expires_at")
})
public class IdempotencyCache {
    @Id
    @Column(name = "idempotency_key")
    private String key;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    // Constructors
    public IdempotencyCache() {}
    
    public IdempotencyCache(String key, UUID transactionId, String responseData, LocalDateTime expiresAt) {
        this.key = key;
        this.transactionId = transactionId;
        this.responseData = responseData;
        this.expiresAt = expiresAt;
    }
    
    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    
    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}