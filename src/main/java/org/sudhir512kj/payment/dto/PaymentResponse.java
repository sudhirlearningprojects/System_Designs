package org.sudhir512kj.payment.dto;

import lombok.Data;
import org.sudhir512kj.payment.model.PaymentTransaction.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PaymentResponse {
    private UUID transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String processorTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}