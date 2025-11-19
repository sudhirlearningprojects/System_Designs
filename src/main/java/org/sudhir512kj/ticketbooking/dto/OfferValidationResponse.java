package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;

public class OfferValidationResponse {
    private Boolean isValid;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;
    
    public OfferValidationResponse() {}
    
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}