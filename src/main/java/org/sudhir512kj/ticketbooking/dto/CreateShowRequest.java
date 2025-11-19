package org.sudhir512kj.ticketbooking.dto;

import java.time.LocalDateTime;

public class CreateShowRequest {
    private Long eventId;
    private Long venueId;
    private LocalDateTime showDate;
    
    public CreateShowRequest() {}
    
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    
    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
}