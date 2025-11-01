package org.sudhir512kj.payment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID merchantId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethodDto paymentMethod;
    private Map<String, String> metadata;
    
    @Data
    public static class PaymentMethodDto {
        private String type; // CARD, BANK_TRANSFER, WALLET
        private String cardToken;
        private String bankAccount;
        private String walletId;
    }
}