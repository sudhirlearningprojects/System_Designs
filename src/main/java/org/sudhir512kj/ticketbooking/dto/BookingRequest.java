package org.sudhir512kj.ticketbooking.dto;

public class BookingRequest {
    private Long userId;
    private Long eventId;
    private Long ticketTypeId;
    private Integer quantity;
    
    // Constructors
    public BookingRequest() {}
    
    public BookingRequest(Long userId, Long eventId, Long ticketTypeId, Integer quantity) {
        this.userId = userId;
        this.eventId = eventId;
        this.ticketTypeId = ticketTypeId;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    
    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}