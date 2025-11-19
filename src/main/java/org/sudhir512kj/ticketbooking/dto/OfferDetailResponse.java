package org.sudhir512kj.ticketbooking.dto;

public class OfferDetailResponse {
    private Long id;
    private String title;
    private String description;
    
    public OfferDetailResponse() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}