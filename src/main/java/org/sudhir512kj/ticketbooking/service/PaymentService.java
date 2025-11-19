package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.*;
// Removed external payment dependencies - using internal implementation

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    
    // Removed external payment processor dependency
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Transactional
    public PaymentInitiationResponse initiatePayment(Long bookingId, PaymentRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.HELD) {
            throw new RuntimeException("Invalid booking status for payment");
        }
        
        if (booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Booking hold expired");
        }
        
        // Internal payment processing
        String paymentId = "pay_" + System.currentTimeMillis();
        String gatewayUrl = "https://payment.gateway.com/pay/" + paymentId;
        
        PaymentInitiationResponse response = new PaymentInitiationResponse();
        response.setPaymentId(paymentId);
        response.setStatus("INITIATED");
        response.setGatewayUrl(gatewayUrl);
        response.setAmount(booking.getTotalAmount());
        
        return response;
    }
    
    @Transactional
    public BookingResponse handlePaymentCallback(Long bookingId, PaymentCallbackRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if ("SUCCESS".equals(request.getStatus())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentId(request.getTransactionId());
            booking.setQrCode(generateQRCode(booking));
            
            // Confirm seat bookings
            for (BookingSeat bookingSeat : booking.getBookingSeats()) {
                ShowSeat showSeat = bookingSeat.getShowSeat();
                showSeat.setStatus(SeatStatus.BOOKED);
                showSeat.setBookingId(booking.getId());
            }
            
            bookingRepository.save(booking);
            
            // Send confirmation notification
            notificationService.sendBookingConfirmation(booking);
            
        } else {
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            bookingRepository.save(booking);
            
            // Release held seats
            releaseHeldSeats(booking);
        }
        
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setStatus(booking.getStatus());
        response.setTotalAmount(booking.getTotalAmount());
        
        return response;
    }
    
    public boolean processPayment(String paymentId, BigDecimal amount) {
        // Internal payment processing - always return success for demo
        return true;
    }
    
    private String generateQRCode(Booking booking) {
        return "QR_" + booking.getBookingReference() + "_" + booking.getId();
    }
    
    private void releaseHeldSeats(Booking booking) {
        for (BookingSeat bookingSeat : booking.getBookingSeats()) {
            ShowSeat showSeat = bookingSeat.getShowSeat();
            showSeat.setStatus(SeatStatus.AVAILABLE);
            showSeat.setBookingId(null);
            showSeat.setBookedByUserId(null);
            showSeat.setHoldExpiresAt(null);
        }
    }
}