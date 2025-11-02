package org.sudhir512kj.digitalpayment.dto;

public class PaymentInitiationResponse {
    private String transactionId;
    private String status;
    private String message;
    
    public PaymentInitiationResponse() {}
    
    public PaymentInitiationResponse(String transactionId, String status, String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
    }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}