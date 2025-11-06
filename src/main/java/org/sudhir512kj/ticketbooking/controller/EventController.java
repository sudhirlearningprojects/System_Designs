package org.sudhir512kj.ticketbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.ticketbooking.model.Event;
import org.sudhir512kj.ticketbooking.model.TicketType;
import org.sudhir512kj.ticketbooking.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @GetMapping("/search")
    public ResponseEntity<Page<Event>> searchEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String name,
            Pageable pageable) {
        Page<Event> events = eventService.searchEvents(city, genre, name, pageable);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }
    
    @GetMapping("/{id}/ticket-types")
    public ResponseEntity<List<TicketType>> getTicketTypes(@PathVariable Long id) {
        List<TicketType> ticketTypes = eventService.getTicketTypesByEvent(id);
        return ResponseEntity.ok(ticketTypes);
    }
    
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event createdEvent = eventService.createEvent(event);
        return ResponseEntity.ok(createdEvent);
    }
}