package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingHistoryResponse {
    private Long bookingId;
    private String bookingReference;
    private String eventName;
    private LocalDateTime showDate;
    private String venueName;
    private Integer seatCount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime bookedAt;
    
    public BookingHistoryResponse() {}
    
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
    
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    
    public Integer getSeatCount() { return seatCount; }
    public void setSeatCount(Integer seatCount) { this.seatCount = seatCount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
}