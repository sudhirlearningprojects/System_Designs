package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.User;
import org.sudhir512kj.ticketbooking.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookingService bookingService;
    
    @GetMapping("/{id}/bookings")
    public ResponseEntity<Page<BookingHistoryResponse>> getBookingHistory(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<BookingHistoryResponse> bookings = bookingService.getUserBookingHistory(id, status, pageable);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/{id}/bookings/upcoming")
    public ResponseEntity<List<BookingResponse>> getUpcomingBookings(@PathVariable Long id) {
        List<BookingResponse> bookings = bookingService.getUpcomingBookings(id);
        return ResponseEntity.ok(bookings);
    }
}