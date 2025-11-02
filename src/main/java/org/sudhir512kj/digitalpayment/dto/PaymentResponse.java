package org.sudhir512kj.digitalpayment.dto;

public class PaymentResponse {
    private String pspTransactionId;
    private String status;
    private String message;
    
    public PaymentResponse() {}
    
    public PaymentResponse(String pspTransactionId, String status, String message) {
        this.pspTransactionId = pspTransactionId;
        this.status = status;
        this.message = message;
    }
    
    public String getPspTransactionId() { return pspTransactionId; }
    public void setPspTransactionId(String pspTransactionId) { this.pspTransactionId = pspTransactionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}