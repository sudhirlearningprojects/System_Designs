package org.sudhir512kj.ticketbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_reference", unique = true, nullable = false)
    private String bookingReference;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;
    
    @Column(name = "ticket_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketAmount;
    
    @Column(name = "food_amount", precision = 10, scale = 2)
    private BigDecimal foodAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "convenience_fee", precision = 10, scale = 2)
    private BigDecimal convenienceFee = BigDecimal.ZERO;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;
    
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;
    
    @Column(name = "qr_code")
    private String qrCode;
    
    @Column(name = "offer_code")
    private String offerCode;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookingSeat> bookingSeats;
    
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookingFoodItem> bookingFoodItems;
    
    // Constructors
    public Booking() {}
    
    public Booking(String bookingReference, User user, Show show, BigDecimal totalAmount) {
        this.bookingReference = bookingReference;
        this.user = user;
        this.show = show;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.HELD;
        this.holdExpiresAt = LocalDateTime.now().plusMinutes(10);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }
    
    public BigDecimal getTicketAmount() { return ticketAmount; }
    public void setTicketAmount(BigDecimal ticketAmount) { this.ticketAmount = ticketAmount; }
    
    public BigDecimal getFoodAmount() { return foodAmount; }
    public void setFoodAmount(BigDecimal foodAmount) { this.foodAmount = foodAmount; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getConvenienceFee() { return convenienceFee; }
    public void setConvenienceFee(BigDecimal convenienceFee) { this.convenienceFee = convenienceFee; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public String getOfferCode() { return offerCode; }
    public void setOfferCode(String offerCode) { this.offerCode = offerCode; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public List<BookingSeat> getBookingSeats() { return bookingSeats; }
    public void setBookingSeats(List<BookingSeat> bookingSeats) { this.bookingSeats = bookingSeats; }
    
    public List<BookingFoodItem> getBookingFoodItems() { return bookingFoodItems; }
    public void setBookingFoodItems(List<BookingFoodItem> bookingFoodItems) { this.bookingFoodItems = bookingFoodItems; }
}