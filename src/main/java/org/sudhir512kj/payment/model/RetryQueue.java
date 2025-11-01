package org.sudhir512kj.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_retry_queue", indexes = {
    @Index(name = "idx_retry_next_retry", columnList = "next_retry_at"),
    @Index(name = "idx_retry_transaction", columnList = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryQueue {
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 5;
    
    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}