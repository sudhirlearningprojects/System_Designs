package org.sudhir512kj.ticketbooking.dto;

import java.util.List;

public class BookingInitiationRequest {
    private Long userId;
    private Long showId;
    private List<Long> seatIds;
    
    public BookingInitiationRequest() {}
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }
    
    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }
}