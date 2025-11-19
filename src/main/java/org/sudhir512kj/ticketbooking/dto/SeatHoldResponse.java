package org.sudhir512kj.ticketbooking.dto;

import java.time.LocalDateTime;

public class SeatHoldResponse {
    private String holdId;
    private LocalDateTime expiresAt;
    
    public SeatHoldResponse() {}
    
    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}