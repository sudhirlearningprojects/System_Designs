package org.sudhir512kj.ticketbooking.dto;

import org.sudhir512kj.ticketbooking.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingResponse {
    private Long bookingId;
    private String holdId;
    private BookingStatus status;
    private LocalDateTime holdExpiresAt;
    private BigDecimal totalAmount;
    
    // Constructors
    public BookingResponse() {}
    
    public BookingResponse(Long bookingId, String holdId, BookingStatus status, LocalDateTime holdExpiresAt, BigDecimal totalAmount) {
        this.bookingId = bookingId;
        this.holdId = holdId;
        this.status = status;
        this.holdExpiresAt = holdExpiresAt;
        this.totalAmount = totalAmount;
    }
    
    // Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    
    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }
    
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}