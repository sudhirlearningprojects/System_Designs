package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.Venue;
import org.sudhir512kj.ticketbooking.service.VenueService;
import org.sudhir512kj.ticketbooking.service.FoodBeverageService;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {
    
    @Autowired
    private VenueService venueService;
    
    @Autowired
    private FoodBeverageService foodBeverageService;
    
    // Venue Discovery
    @GetMapping("/search")
    public ResponseEntity<Page<Venue>> searchVenues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        Page<Venue> venues = venueService.searchVenues(city, name, type, pageable);
        return ResponseEntity.ok(venues);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<VenueDetailResponse> getVenue(@PathVariable Long id) {
        VenueDetailResponse venue = venueService.getVenueDetails(id);
        if (venue == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(venue);
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<List<Venue>> getNearbyVenues(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10") Double radiusKm) {
        List<Venue> venues = venueService.getNearbyVenues(latitude, longitude, radiusKm);
        return ResponseEntity.ok(venues);
    }
    
    // F&B Menu
    @GetMapping("/{id}/menu")
    public ResponseEntity<List<FoodBeverageResponse>> getVenueMenu(@PathVariable Long id) {
        List<FoodBeverageResponse> menu = foodBeverageService.getVenueMenu(id);
        return ResponseEntity.ok(menu);
    }
    
    // Admin Operations
    @PostMapping
    public ResponseEntity<Venue> createVenue(@RequestBody CreateVenueRequest request) {
        Venue venue = venueService.createVenue(request);
        return ResponseEntity.ok(venue);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Venue> updateVenue(@PathVariable Long id, @RequestBody UpdateVenueRequest request) {
        Venue venue = venueService.updateVenue(id, request);
        return ResponseEntity.ok(venue);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok().build();
    }
}