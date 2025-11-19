package org.sudhir512kj.ticketbooking.dto;

public class SeatReleaseRequest {
    private String holdId;
    
    public SeatReleaseRequest() {}
    
    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }
}