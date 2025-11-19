package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingInitiationResponse {
    private Long bookingId;
    private String bookingReference;
    private BigDecimal totalAmount;
    private LocalDateTime holdExpiresAt;
    private String status;
    
    public BookingInitiationResponse() {}
    
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}