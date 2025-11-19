package org.sudhir512kj.ticketbooking.dto;

import java.time.LocalDateTime;

public class UpdateShowRequest {
    private LocalDateTime showDate;
    
    public UpdateShowRequest() {}
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
}