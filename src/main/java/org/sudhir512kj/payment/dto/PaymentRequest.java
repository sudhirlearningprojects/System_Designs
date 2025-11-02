package org.sudhir512kj.payment.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class PaymentRequest {
    private UUID merchantId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethodDto paymentMethod;
    private Map<String, String> metadata;
    
    // Getters and setters
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public PaymentMethodDto getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethodDto paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public static class PaymentMethodDto {
        private String type; // CARD, BANK_TRANSFER, WALLET
        private String cardToken;
        private String bankAccount;
        private String walletId;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getCardToken() { return cardToken; }
        public void setCardToken(String cardToken) { this.cardToken = cardToken; }
        
        public String getBankAccount() { return bankAccount; }
        public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
        
        public String getWalletId() { return walletId; }
        public void setWalletId(String walletId) { this.walletId = walletId; }
    }
}