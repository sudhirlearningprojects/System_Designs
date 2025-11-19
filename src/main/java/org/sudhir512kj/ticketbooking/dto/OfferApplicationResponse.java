package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;

public class OfferApplicationResponse {
    private Boolean success;
    private BigDecimal discountAmount;
    private String message;
    
    public OfferApplicationResponse() {}
    
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}