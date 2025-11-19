package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
public class ShowController {
    
    @Autowired
    private ShowService showService;
    
    @Autowired
    private SeatService seatService;
    
    // Show Details
    @GetMapping("/{id}")
    public ResponseEntity<ShowDetailResponse> getShow(@PathVariable Long id) {
        ShowDetailResponse show = showService.getShowDetails(id);
        if (show == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(show);
    }
    
    // Seat Selection
    @GetMapping("/{id}/seats")
    public ResponseEntity<SeatLayoutResponse> getShowSeats(@PathVariable Long id) {
        SeatLayoutResponse seatLayout = seatService.getShowSeatLayout(id);
        return ResponseEntity.ok(seatLayout);
    }
    
    @PostMapping("/{id}/seats/hold")
    public ResponseEntity<SeatHoldResponse> holdSeats(
            @PathVariable Long id,
            @RequestBody SeatHoldRequest request) {
        SeatHoldResponse response = seatService.holdSeats(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/seats/release")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable Long id,
            @RequestBody SeatReleaseRequest request) {
        seatService.releaseSeats(id, request);
        return ResponseEntity.ok().build();
    }
    
    // Admin Operations
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@RequestBody CreateShowRequest request) {
        ShowResponse show = showService.createShow(request);
        return ResponseEntity.ok(show);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(@PathVariable Long id, @RequestBody UpdateShowRequest request) {
        ShowResponse show = showService.updateShow(id, request);
        return ResponseEntity.ok(show);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.ok().build();
    }
}