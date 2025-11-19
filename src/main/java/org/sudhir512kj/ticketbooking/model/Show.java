package org.sudhir512kj.ticketbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shows")
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
    
    @Column(name = "show_date", nullable = false)
    private LocalDateTime showDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "booking_start_date")
    private LocalDateTime bookingStartDate;
    
    @Column(name = "booking_end_date")
    private LocalDateTime bookingEndDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShowSeat> showSeats;
    
    // Constructors
    public Show() {}
    
    public Show(Event event, Venue venue, LocalDateTime showDate) {
        this.event = event;
        this.venue = venue;
        this.showDate = showDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }
    
    public LocalDateTime getShowDate() { return showDate; }
    public void setShowDate(LocalDateTime showDate) { this.showDate = showDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public LocalDateTime getBookingStartDate() { return bookingStartDate; }
    public void setBookingStartDate(LocalDateTime bookingStartDate) { this.bookingStartDate = bookingStartDate; }
    
    public LocalDateTime getBookingEndDate() { return bookingEndDate; }
    public void setBookingEndDate(LocalDateTime bookingEndDate) { this.bookingEndDate = bookingEndDate; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<ShowSeat> getShowSeats() { return showSeats; }
    public void setShowSeats(List<ShowSeat> showSeats) { this.showSeats = showSeats; }
}