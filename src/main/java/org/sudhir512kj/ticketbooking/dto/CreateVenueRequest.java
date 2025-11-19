package org.sudhir512kj.ticketbooking.dto;

import org.sudhir512kj.ticketbooking.model.VenueType;

public class CreateVenueRequest {
    private String name;
    private String city;
    private VenueType type;
    
    public CreateVenueRequest() {}
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public VenueType getType() { return type; }
    public void setType(VenueType type) { this.type = type; }
}