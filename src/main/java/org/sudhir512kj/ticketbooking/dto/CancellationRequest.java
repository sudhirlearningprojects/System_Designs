package org.sudhir512kj.ticketbooking.dto;

public class CancellationRequest {
    private String reason;
    
    public CancellationRequest() {}
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}