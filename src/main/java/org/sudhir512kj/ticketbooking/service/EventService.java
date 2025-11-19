package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.dto.*;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public Page<Event> searchEvents(EventSearchRequest searchRequest, Pageable pageable) {
        String cacheKey = "event_search:" + searchRequest.hashCode();
        
        // Try cache first
        List<Event> cachedResults = (List<Event>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResults != null) {
            return new PageImpl<>(cachedResults, pageable, cachedResults.size());
        }
        
        // Build dynamic query
        Page<Event> events = eventRepository.searchEventsWithFilters(
            searchRequest.getCity(),
            searchRequest.getCategory(),
            searchRequest.getGenre(),
            searchRequest.getLanguage(),
            searchRequest.getName(),
            searchRequest.getFromDate(),
            searchRequest.getToDate(),
            searchRequest.getMinPrice(),
            searchRequest.getMaxPrice(),
            pageable
        );
        
        // Cache results for 5 minutes
        redisTemplate.opsForValue().set(cacheKey, events.getContent(), 5, TimeUnit.MINUTES);
        
        return events;
    }
    
    public List<Event> getFeaturedEvents() {
        String cacheKey = "featured_events";
        List<Event> cached = (List<Event>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        List<Event> featured = eventRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();
        redisTemplate.opsForValue().set(cacheKey, featured, 10, TimeUnit.MINUTES);
        
        return featured;
    }
    
    public List<Event> getTrendingEvents(String city) {
        String cacheKey = "trending_events:" + (city != null ? city : "all");
        List<Event> cached = (List<Event>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        // Get trending based on booking count in last 7 days
        List<Event> trending = eventRepository.findTrendingEvents(city, LocalDateTime.now().minusDays(7));
        redisTemplate.opsForValue().set(cacheKey, trending, 15, TimeUnit.MINUTES);
        
        return trending;
    }
    
    public EventDetailResponse getEventDetails(Long eventId) {
        String cacheKey = "event_details:" + eventId;
        EventDetailResponse cached = (EventDetailResponse) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return null;
        }
        
        EventDetailResponse response = convertToEventDetailResponse(event);
        redisTemplate.opsForValue().set(cacheKey, response, 30, TimeUnit.MINUTES);
        
        return response;
    }
    
    public List<CategoryResponse> getEventsByCategory(String city) {
        String cacheKey = "events_by_category:" + (city != null ? city : "all");
        List<CategoryResponse> cached = (List<CategoryResponse>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        List<CategoryResponse> categories = eventRepository.findEventCountByCategory(city)
            .stream()
            .map(result -> {
                CategoryResponse category = new CategoryResponse();
                category.setCategory((EventCategory) result[0]);
                category.setEventCount((Long) result[1]);
                return category;
            })
            .collect(Collectors.toList());
        
        redisTemplate.opsForValue().set(cacheKey, categories, 20, TimeUnit.MINUTES);
        return categories;
    }
    
    public List<String> getEventGallery(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || event.getGalleryUrls() == null) {
            return List.of();
        }
        
        // Parse JSON array of gallery URLs
        return parseGalleryUrls(event.getGalleryUrls());
    }
    
    public Event createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setGenre(request.getGenre());
        event.setLanguage(request.getLanguage());
        event.setDuration(request.getDuration());
        event.setEventDate(request.getEventDate());
        event.setPosterUrl(request.getPosterUrl());
        event.setTrailerUrl(request.getTrailerUrl());
        
        return eventRepository.save(event);
    }
    
    public Event updateEvent(Long id, UpdateEventRequest request) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        
        if (request.getName() != null) event.setName(request.getName());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getPosterUrl() != null) event.setPosterUrl(request.getPosterUrl());
        
        // Clear cache
        redisTemplate.delete("event_details:" + id);
        
        return eventRepository.save(event);
    }
    
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
        redisTemplate.delete("event_details:" + id);
    }
    
    private EventDetailResponse convertToEventDetailResponse(Event event) {
        EventDetailResponse response = new EventDetailResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setCategory(event.getCategory());
        response.setGenre(event.getGenre());
        response.setLanguage(event.getLanguage());
        response.setDuration(event.getDuration());
        response.setRating(event.getRating());
        response.setReviewCount(event.getReviewCount());
        response.setPosterUrl(event.getPosterUrl());
        response.setTrailerUrl(event.getTrailerUrl());
        response.setCast(parseCastCrew(event.getCast()));
        response.setCrew(parseCastCrew(event.getCrew()));
        
        return response;
    }
    
    private List<String> parseGalleryUrls(String galleryUrls) {
        // Simple JSON parsing - in production use Jackson
        return List.of(galleryUrls.split(","));
    }
    
    private List<Object> parseCastCrew(String castCrewJson) {
        // Simple parsing - in production use Jackson
        return List.of();
    }
}