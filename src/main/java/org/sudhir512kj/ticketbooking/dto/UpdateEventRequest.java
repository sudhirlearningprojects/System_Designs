package org.sudhir512kj.ticketbooking.dto;

public class UpdateEventRequest {
    private String name;
    private String description;
    private String posterUrl;
    
    public UpdateEventRequest() {}
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
}