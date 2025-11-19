package org.sudhir512kj.ticketbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_seats")
public class BookingSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;
    
    @Column(name = "seat_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal seatPrice;
    
    // Constructors
    public BookingSeat() {}
    
    public BookingSeat(Booking booking, ShowSeat showSeat, BigDecimal seatPrice) {
        this.booking = booking;
        this.showSeat = showSeat;
        this.seatPrice = seatPrice;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    
    public ShowSeat getShowSeat() { return showSeat; }
    public void setShowSeat(ShowSeat showSeat) { this.showSeat = showSeat; }
    
    public BigDecimal getSeatPrice() { return seatPrice; }
    public void setSeatPrice(BigDecimal seatPrice) { this.seatPrice = seatPrice; }
}