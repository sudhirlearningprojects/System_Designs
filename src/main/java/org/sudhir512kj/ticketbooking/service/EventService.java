package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.model.Event;
import org.sudhir512kj.ticketbooking.model.TicketType;
import org.sudhir512kj.ticketbooking.repository.EventRepository;
import org.sudhir512kj.ticketbooking.repository.TicketTypeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private TicketTypeRepository ticketTypeRepository;
    
    @Cacheable(value = "events", key = "#city + '_' + #genre + '_' + #name + '_' + #pageable.pageNumber")
    public Page<Event> searchEvents(String city, String genre, String name, Pageable pageable) {
        return eventRepository.searchEvents(city, genre, name, LocalDateTime.now(), pageable);
    }
    
    @Cacheable(value = "event", key = "#id")
    public Event getEventById(Long id) {
        return eventRepository.findById(id).orElse(null);
    }
    
    @Cacheable(value = "ticketTypes", key = "#eventId")
    public List<TicketType> getTicketTypesByEvent(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId);
    }
    
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }
}