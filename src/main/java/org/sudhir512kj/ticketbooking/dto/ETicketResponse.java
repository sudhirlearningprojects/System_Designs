package org.sudhir512kj.ticketbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ETicketResponse {
    private String bookingReference;
    private String eventName;
    private LocalDateTime showDate;
    private String venueName;
    private String venueAddress;
    private List<String> seatNumbers;
    private BigDecimal totalAmount;
    private String qrCode;
    private String userName;
    
    public ETicketResponse() {}
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
    
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    
    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
    
    public List<String> getSeatNumbers() { return seatNumbers; }
    public void setSeatNumbers(List<String> seatNumbers) { this.seatNumbers = seatNumbers; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}