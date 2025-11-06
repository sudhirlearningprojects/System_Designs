package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.ticketbooking.dto.BookingRequest;
import org.sudhir512kj.ticketbooking.dto.BookingResponse;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.BookingRepository;
import org.sudhir512kj.ticketbooking.repository.EventRepository;
import org.sudhir512kj.ticketbooking.repository.TicketTypeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private TicketTypeRepository ticketTypeRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Transactional
    public BookingResponse holdTickets(BookingRequest request) {
        Event event = eventRepository.findById(request.getEventId()).orElse(null);
        if (event == null) {
            throw new RuntimeException("Event not found");
        }
        
        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId()).orElse(null);
        if (ticketType == null) {
            throw new RuntimeException("Ticket type not found");
        }
        
        String holdId = UUID.randomUUID().toString();
        boolean holdSuccess = inventoryService.holdTickets(request.getTicketTypeId(), request.getQuantity(), holdId);
        
        if (!holdSuccess) {
            throw new RuntimeException("Insufficient tickets available");
        }
        
        BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        
        Booking booking = new Booking(request.getUserId(), event, ticketType, request.getQuantity(), totalAmount);
        booking = bookingRepository.save(booking);
        
        return new BookingResponse(booking.getId(), holdId, booking.getStatus(), booking.getHoldExpiresAt(), totalAmount);
    }
    
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String paymentId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.HELD) {
            throw new RuntimeException("Invalid booking or booking expired");
        }
        
        if (booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new RuntimeException("Booking hold expired");
        }
        
        // Process payment
        boolean paymentSuccess = paymentService.processPayment(paymentId, booking.getTotalAmount());
        if (!paymentSuccess) {
            throw new RuntimeException("Payment failed");
        }
        
        // Confirm inventory hold
        String holdId = paymentId; // Using paymentId as holdId for simplicity
        inventoryService.confirmHold(holdId, booking.getTicketType().getId());
        
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking = bookingRepository.save(booking);
        
        return new BookingResponse(booking.getId(), null, booking.getStatus(), null, booking.getTotalAmount());
    }
    
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupExpiredHolds() {
        List<Booking> expiredBookings = bookingRepository.findExpiredHolds(BookingStatus.HELD, LocalDateTime.now());
        
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            
            // Release inventory hold
            inventoryService.releaseHold(booking.getId().toString(), booking.getTicketType().getId());
        }
    }
}