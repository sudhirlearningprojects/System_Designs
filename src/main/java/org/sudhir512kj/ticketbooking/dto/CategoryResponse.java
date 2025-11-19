package org.sudhir512kj.ticketbooking.dto;

import org.sudhir512kj.ticketbooking.model.EventCategory;

public class CategoryResponse {
    private EventCategory category;
    private Long eventCount;
    
    public CategoryResponse() {}
    
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    
    public Long getEventCount() { return eventCount; }
    public void setEventCount(Long eventCount) { this.eventCount = eventCount; }
}