package org.sudhir512kj.ticketbooking.dto;

import java.time.LocalDateTime;

public class TicketVerificationResponse {
    private Boolean isValid;
    private String eventName;
    private LocalDateTime showDate;
    private String venueName;
    private String userName;
    private String message;
    
    public TicketVerificationResponse() {}
    
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
    
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}