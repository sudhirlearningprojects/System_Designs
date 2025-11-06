package org.sudhir512kj.ticketbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    private String venue;
    private String city;
    private String genre;
    
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketType> ticketTypes;
    
    // Constructors
    public Event() {}
    
    public Event(String name, String venue, String city, LocalDateTime eventDate) {
        this.name = name;
        this.venue = venue;
        this.city = city;
        this.eventDate = eventDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<TicketType> getTicketTypes() { return ticketTypes; }
    public void setTicketTypes(List<TicketType> ticketTypes) { this.ticketTypes = ticketTypes; }
}