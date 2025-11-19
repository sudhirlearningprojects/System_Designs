package org.sudhir512kj.ticketbooking.dto;

public class OfferAnalyticsResponse {
    private Long totalUsage;
    private String status;
    
    public OfferAnalyticsResponse() {}
    
    public Long getTotalUsage() { return totalUsage; }
    public void setTotalUsage(Long totalUsage) { this.totalUsage = totalUsage; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}