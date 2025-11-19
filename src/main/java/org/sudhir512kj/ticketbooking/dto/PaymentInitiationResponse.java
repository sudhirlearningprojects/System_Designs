package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;

public class PaymentInitiationResponse {
    private String paymentId;
    private String status;
    private String gatewayUrl;
    private BigDecimal amount;
    
    public PaymentInitiationResponse() {}
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}