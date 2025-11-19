package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private ShowService showService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private RecommendationService recommendationService;
    
    // Event Discovery and Search
    @GetMapping("/search")
    public ResponseEntity<Page<Event>> searchEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            Pageable pageable) {
        
        EventSearchRequest searchRequest = new EventSearchRequest(
            city, genre, name, category, language, fromDate, toDate, minPrice, maxPrice, sortBy
        );
        
        Page<Event> events = eventService.searchEvents(searchRequest, pageable);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/featured")
    public ResponseEntity<List<Event>> getFeaturedEvents() {
        List<Event> events = eventService.getFeaturedEvents();
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/trending")
    public ResponseEntity<List<Event>> getTrendingEvents(@RequestParam(required = false) String city) {
        List<Event> events = eventService.getTrendingEvents(city);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/recommendations")
    public ResponseEntity<List<Event>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false) String city) {
        List<Event> events = recommendationService.getPersonalizedRecommendations(userId, city);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getEventsByCategory(
            @RequestParam(required = false) String city) {
        List<CategoryResponse> categories = eventService.getEventsByCategory(city);
        return ResponseEntity.ok(categories);
    }
    
    // Event Details
    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponse> getEvent(@PathVariable Long id) {
        EventDetailResponse event = eventService.getEventDetails(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }
    
    @GetMapping("/{id}/shows")
    public ResponseEntity<List<ShowResponse>> getEventShows(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String city) {
        List<ShowResponse> shows = showService.getEventShows(id, date, city);
        return ResponseEntity.ok(shows);
    }
    
    @GetMapping("/{id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getEventReviews(
            @PathVariable Long id,
            Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getEventReviews(id, pageable);
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/{id}/gallery")
    public ResponseEntity<List<String>> getEventGallery(@PathVariable Long id) {
        List<String> gallery = eventService.getEventGallery(id);
        return ResponseEntity.ok(gallery);
    }
    
    // Admin Operations
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody CreateEventRequest request) {
        Event createdEvent = eventService.createEvent(request);
        return ResponseEntity.ok(createdEvent);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody UpdateEventRequest request) {
        Event updatedEvent = eventService.updateEvent(id, request);
        return ResponseEntity.ok(updatedEvent);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }
}