package org.sudhir512kj.digitalpayment.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    
    public PaymentRequest() {}
    
    public PaymentRequest(BigDecimal amount, String paymentMethod, String transactionId) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
    }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}