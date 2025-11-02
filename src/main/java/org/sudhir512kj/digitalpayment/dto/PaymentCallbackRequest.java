package org.sudhir512kj.digitalpayment.dto;

public class PaymentCallbackRequest {
    private String transactionId;
    private String pspTransactionId;
    private String status;
    private String message;
    
    public PaymentCallbackRequest() {}
    
    public PaymentCallbackRequest(String transactionId, String pspTransactionId, String status, String message) {
        this.transactionId = transactionId;
        this.pspTransactionId = pspTransactionId;
        this.status = status;
        this.message = message;
    }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getPspTransactionId() { return pspTransactionId; }
    public void setPspTransactionId(String pspTransactionId) { this.pspTransactionId = pspTransactionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}