package org.sudhir512kj.ticketbooking.dto;

public class CreateOfferRequest {
    private String title;
    private String offerCode;
    
    public CreateOfferRequest() {}
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getOfferCode() { return offerCode; }
    public void setOfferCode(String offerCode) { this.offerCode = offerCode; }
}