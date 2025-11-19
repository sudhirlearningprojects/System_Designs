package org.sudhir512kj.ticketbooking.dto;

import org.sudhir512kj.ticketbooking.model.EventCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EventSearchRequest {
    private String city;
    private String genre;
    private String name;
    private EventCategory category;
    private String language;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    
    public EventSearchRequest() {}
    
    public EventSearchRequest(String city, String genre, String name, EventCategory category, 
                            String language, LocalDate fromDate, LocalDate toDate, 
                            BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        this.city = city;
        this.genre = genre;
        this.name = name;
        this.category = category;
        this.language = language;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.sortBy = sortBy;
    }
    
    // Getters and Setters
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}