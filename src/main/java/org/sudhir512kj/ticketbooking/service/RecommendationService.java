package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.ticketbooking.model.*;
import org.sudhir512kj.ticketbooking.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserPreferenceRepository userPreferenceRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public List<Event> getPersonalizedRecommendations(Long userId, String city) {
        String cacheKey = "recommendations:" + userId + ":" + (city != null ? city : "all");
        List<Event> cached = (List<Event>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return getPopularEvents(city);
        }
        
        // Get user preferences
        UserPreference preferences = userPreferenceRepository.findByUserId(userId).orElse(null);
        
        // Get user's booking history
        List<Booking> userBookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Calculate recommendations using collaborative filtering + content-based filtering
        List<Event> recommendations = calculateRecommendations(user, preferences, userBookings, city);
        
        // Cache for 1 hour
        redisTemplate.opsForValue().set(cacheKey, recommendations, 1, TimeUnit.HOURS);
        
        return recommendations;
    }
    
    private List<Event> calculateRecommendations(User user, UserPreference preferences, 
                                               List<Booking> userBookings, String city) {
        
        Map<Event, Double> eventScores = new HashMap<>();
        
        // Content-based filtering
        if (preferences != null) {
            addContentBasedScores(eventScores, preferences, city);
        }
        
        // Collaborative filtering
        addCollaborativeFilteringScores(eventScores, userBookings, city);
        
        // Popularity boost
        addPopularityScores(eventScores, city);
        
        // Sort by score and return top 20
        return eventScores.entrySet().stream()
                .sorted(Map.Entry.<Event, Double>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private void addContentBasedScores(Map<Event, Double> eventScores, 
                                     UserPreference preferences, String city) {
        
        List<String> preferredGenres = parseJsonArray(preferences.getPreferredGenres());
        List<String> preferredLanguages = parseJsonArray(preferences.getPreferredLanguages());
        List<String> preferredCities = parseJsonArray(preferences.getPreferredCities());
        
        List<Event> events = eventRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();
        
        for (Event event : events) {
            double score = 0.0;
            
            // Genre matching
            if (preferredGenres.contains(event.getGenre())) {
                score += 3.0;
            }
            
            // Language matching
            if (preferredLanguages.contains(event.getLanguage())) {
                score += 2.0;
            }
            
            // City matching
            if (city != null && city.equalsIgnoreCase(event.getCity())) {
                score += 1.5;
            } else if (preferredCities.contains(event.getCity())) {
                score += 1.0;
            }
            
            // Rating boost
            if (event.getRating() != null) {
                score += event.getRating().doubleValue() * 0.5;
            }
            
            if (score > 0) {
                eventScores.put(event, score);
            }
        }
    }
    
    private void addCollaborativeFilteringScores(Map<Event, Double> eventScores, 
                                               List<Booking> userBookings, String city) {
        
        if (userBookings.isEmpty()) {
            return;
        }
        
        // Find similar users based on booking history
        Set<Long> userEventIds = userBookings.stream()
                .map(b -> b.getShow().getEvent().getId())
                .collect(Collectors.toSet());
        
        // Find users who booked similar events
        List<User> similarUsers = findSimilarUsers(userEventIds);
        
        // Get events booked by similar users
        for (User similarUser : similarUsers) {
            List<Booking> similarUserBookings = bookingRepository
                    .findByUserIdOrderByCreatedAtDesc(similarUser.getId());
            
            for (Booking booking : similarUserBookings) {
                Event event = booking.getShow().getEvent();
                
                // Skip if user already booked this event
                if (userEventIds.contains(event.getId())) {
                    continue;
                }
                
                // Filter by city if specified
                if (city != null && !city.equalsIgnoreCase(event.getCity())) {
                    continue;
                }
                
                double score = eventScores.getOrDefault(event, 0.0);
                score += 1.0; // Collaborative filtering score
                eventScores.put(event, score);
            }
        }
    }
    
    private void addPopularityScores(Map<Event, Double> eventScores, String city) {
        // Get trending events from last 7 days
        List<Event> trendingEvents = eventRepository.findTrendingEvents(city, 
                LocalDateTime.now().minusDays(7));
        
        for (int i = 0; i < trendingEvents.size(); i++) {
            Event event = trendingEvents.get(i);
            double popularityScore = (trendingEvents.size() - i) * 0.1; // Decreasing score
            
            double currentScore = eventScores.getOrDefault(event, 0.0);
            eventScores.put(event, currentScore + popularityScore);
        }
    }
    
    private List<User> findSimilarUsers(Set<Long> userEventIds) {
        // Simplified similarity calculation
        // In production, use more sophisticated algorithms like cosine similarity
        
        List<User> similarUsers = new ArrayList<>();
        List<Booking> allBookings = bookingRepository.findAll();
        
        Map<Long, Set<Long>> userEventMap = new HashMap<>();
        for (Booking booking : allBookings) {
            Long userId = booking.getUser().getId();
            Long eventId = booking.getShow().getEvent().getId();
            
            userEventMap.computeIfAbsent(userId, k -> new HashSet<>()).add(eventId);
        }
        
        // Find users with at least 2 common events
        for (Map.Entry<Long, Set<Long>> entry : userEventMap.entrySet()) {
            Set<Long> otherUserEvents = entry.getValue();
            Set<Long> intersection = new HashSet<>(userEventIds);
            intersection.retainAll(otherUserEvents);
            
            if (intersection.size() >= 2) {
                userRepository.findById(entry.getKey()).ifPresent(similarUsers::add);
            }
        }
        
        return similarUsers.stream().limit(10).collect(Collectors.toList());
    }
    
    private List<Event> getPopularEvents(String city) {
        return eventRepository.findTrendingEvents(city, LocalDateTime.now().minusDays(30));
    }
    
    private List<String> parseJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Simple JSON parsing - in production use Jackson
        return Arrays.asList(jsonArray.replace("[", "").replace("]", "")
                .replace("\"", "").split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}