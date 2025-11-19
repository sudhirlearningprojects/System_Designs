package org.sudhir512kj.ticketbooking.dto;

import java.util.List;

public class SeatHoldRequest {
    private Long userId;
    private List<Long> seatIds;
    
    public SeatHoldRequest() {}
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }
}