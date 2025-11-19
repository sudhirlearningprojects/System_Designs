package org.sudhir512kj.ticketbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "show_seats")
public class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_seat_id", nullable = false)
    private VenueSeat venueSeat;
    
    @Column(name = "final_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal finalPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;
    
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;
    
    @Column(name = "booked_by_user_id")
    private Long bookedByUserId;
    
    @Column(name = "booking_id")
    private Long bookingId;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public ShowSeat() {}
    
    public ShowSeat(Show show, VenueSeat venueSeat, BigDecimal finalPrice) {
        this.show = show;
        this.venueSeat = venueSeat;
        this.finalPrice = finalPrice;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }
    
    public VenueSeat getVenueSeat() { return venueSeat; }
    public void setVenueSeat(VenueSeat venueSeat) { this.venueSeat = venueSeat; }
    
    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
    
    public Long getBookedByUserId() { return bookedByUserId; }
    public void setBookedByUserId(Long bookedByUserId) { this.bookedByUserId = bookedByUserId; }
    
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}