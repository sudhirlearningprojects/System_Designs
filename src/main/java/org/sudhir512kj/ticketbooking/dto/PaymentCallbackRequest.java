package org.sudhir512kj.ticketbooking.dto;

public class PaymentCallbackRequest {
    private String paymentId;
    private String status;
    private String transactionId;
    
    public PaymentCallbackRequest() {}
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}