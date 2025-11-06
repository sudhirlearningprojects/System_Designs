package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.BookingRequest;
import org.sudhir512kj.ticketbooking.dto.BookingResponse;
import org.sudhir512kj.ticketbooking.model.Booking;
import org.sudhir512kj.ticketbooking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    
    @Autowired
    private BookingService bookingService;
    
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
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }
}