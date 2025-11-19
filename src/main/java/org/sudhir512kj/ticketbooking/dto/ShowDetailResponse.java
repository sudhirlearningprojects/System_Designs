package org.sudhir512kj.ticketbooking.dto;

import java.time.LocalDateTime;

public class ShowDetailResponse {
    private Long id;
    private LocalDateTime showDate;
    private String eventName;
    private String venueName;
    private String venueAddress;
    
    public ShowDetailResponse() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    
    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
}