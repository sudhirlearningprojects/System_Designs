package org.sudhir512kj.ticketbooking.dto;

public class VerifyTicketRequest {
    private String qrCode;
    private String bookingReference;
    
    public VerifyTicketRequest() {}
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
}