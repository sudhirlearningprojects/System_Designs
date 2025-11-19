package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;

public class ValidateOfferRequest {
    private String offerCode;
    private BigDecimal bookingAmount;
    private Long userId;
    private Long eventId;
    
    public ValidateOfferRequest() {}
    
    public String getOfferCode() { return offerCode; }
    public void setOfferCode(String offerCode) { this.offerCode = offerCode; }
    
    public BigDecimal getBookingAmount() { return bookingAmount; }
    public void setBookingAmount(BigDecimal bookingAmount) { this.bookingAmount = bookingAmount; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}