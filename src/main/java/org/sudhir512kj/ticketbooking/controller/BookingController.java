package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NotificationService notificationService;
    
    // Booking Workflow
    @PostMapping("/initiate")
    public ResponseEntity<BookingInitiationResponse> initiateBooking(@RequestBody BookingInitiationRequest request) {
        BookingInitiationResponse response = bookingService.initiateBooking(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/add-food")
    public ResponseEntity<BookingResponse> addFoodItems(
            @PathVariable Long id, 
            @RequestBody AddFoodItemsRequest request) {
        BookingResponse response = bookingService.addFoodItems(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/apply-offer")
    public ResponseEntity<BookingResponse> applyOffer(
            @PathVariable Long id, 
            @RequestBody ApplyOfferRequest request) {
        BookingResponse response = bookingService.applyOffer(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/payment")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @PathVariable Long id, 
            @RequestBody PaymentRequest request) {
        PaymentInitiationResponse response = paymentService.initiatePayment(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/payment/callback")
    public ResponseEntity<BookingResponse> handlePaymentCallback(
            @PathVariable Long id, 
            @RequestBody PaymentCallbackRequest request) {
        BookingResponse response = paymentService.handlePaymentCallback(id, request);
        return ResponseEntity.ok(response);
    }
    
    // Booking Management
    @GetMapping("/{id}")
    public ResponseEntity<BookingDetailResponse> getBooking(@PathVariable Long id) {
        BookingDetailResponse booking = bookingService.getBookingDetails(id);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(booking);
    }
    
    @GetMapping("/reference/{reference}")
    public ResponseEntity<BookingDetailResponse> getBookingByReference(@PathVariable String reference) {
        BookingDetailResponse booking = bookingService.getBookingByReference(reference);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(booking);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancellationResponse> cancelBooking(
            @PathVariable Long id, 
            @RequestBody CancellationRequest request) {
        CancellationResponse response = bookingService.cancelBooking(id, request);
        return ResponseEntity.ok(response);
    }
    
    // E-Tickets and QR Codes
    @GetMapping("/{id}/ticket")
    public ResponseEntity<ETicketResponse> getETicket(@PathVariable Long id) {
        ETicketResponse ticket = bookingService.generateETicket(id);
        return ResponseEntity.ok(ticket);
    }
    
    @GetMapping("/{id}/qr-code")
    public ResponseEntity<byte[]> getQRCode(@PathVariable Long id) {
        byte[] qrCode = bookingService.generateQRCode(id);
        return ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .body(qrCode);
    }
    
    @PostMapping("/verify-ticket")
    public ResponseEntity<TicketVerificationResponse> verifyTicket(@RequestBody VerifyTicketRequest request) {
        TicketVerificationResponse response = bookingService.verifyTicket(request);
        return ResponseEntity.ok(response);
    }
    
    // User Bookings
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<BookingHistoryResponse>> getUserBookings(
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<BookingHistoryResponse> bookings = bookingService.getUserBookingHistory(userId, status, pageable);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<List<BookingResponse>> getUpcomingBookings(@PathVariable Long userId) {
        List<BookingResponse> bookings = bookingService.getUpcomingBookings(userId);
        return ResponseEntity.ok(bookings);
    }
    
    // Legacy endpoints for backward compatibility
    @PostMapping("/hold")
    public ResponseEntity<BookingResponse> holdTickets(@RequestBody BookingRequest request) {
        try {
            BookingResponse response = bookingService.holdTickets(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long bookingId, @RequestParam String paymentId) {
        try {
            BookingResponse response = bookingService.confirmBooking(bookingId, paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}