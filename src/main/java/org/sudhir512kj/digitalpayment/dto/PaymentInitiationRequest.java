package org.sudhir512kj.digitalpayment.dto;

import java.math.BigDecimal;

public class PaymentInitiationRequest {
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String type; // P2P, P2M, etc.
    private String paymentMethod; // UPI, WALLET, etc.
    private String description;
    private String idempotencyKey;
    
    // Constructors
    public PaymentInitiationRequest() {}
    
    public PaymentInitiationRequest(String senderId, String receiverId, BigDecimal amount, 
                                  String type, String paymentMethod, String idempotencyKey) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
    }
    
    // Getters and setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}