package org.sudhir512kj.netflix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.netflix.dto.UserRegistrationRequest;
import org.sudhir512kj.netflix.dto.LoginRequest;
import org.sudhir512kj.netflix.model.Content;
import org.sudhir512kj.netflix.model.User;
import org.sudhir512kj.netflix.service.RecommendationService;
import org.sudhir512kj.netflix.service.StreamingService;
import org.sudhir512kj.netflix.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/netflix")
@CrossOrigin(origins = "*")
public class NetflixController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StreamingService streamingService;
    
    @Autowired
    private RecommendationService recommendationService;
    
    // User Management APIs
    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword(), 
                request.getName(), request.getRegion());
            
            if (request.getPreferredGenres() != null) {
                userService.updateUserPreferences(user.getId(), request.getPreferredGenres());
            }
            
            return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "plan", user.getPlan(),
                "region", user.getRegion()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/auth/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        
        Optional<User> userOpt = userService.authenticateUser(email, password);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "plan", user.getPlan(),
                "region", user.getRegion(),
                "token", "jwt_token_" + user.getId() // Simplified JWT
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
    
    @PutMapping("/users/{userId}/subscription")
    public ResponseEntity<?> updateSubscription(@PathVariable String userId, 
                                              @RequestBody Map<String, String> request) {
        try {
            User.SubscriptionPlan newPlan = User.SubscriptionPlan.valueOf(request.get("plan"));
            User user = userService.updateSubscriptionPlan(userId, newPlan);
            return ResponseEntity.ok(Map.of("plan", user.getPlan()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Content Discovery APIs
    @GetMapping("/content/recommendations/{userId}")
    public ResponseEntity<List<Content>> getRecommendations(@PathVariable String userId) {
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Content> recommendations = recommendationService.getPersonalizedRecommendations(userId, userOpt.get());
        return ResponseEntity.ok(recommendations);
    }
    
    @GetMapping("/content/search")
    public ResponseEntity<List<Content>> searchContent(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year) {
        
        List<Content> results = recommendationService.searchContent(query, genre, year);
        return ResponseEntity.ok(results);
    }
    
    // Streaming APIs
    @PostMapping("/stream/start")
    public ResponseEntity<?> startStreaming(@RequestParam String userId,
                                          @RequestParam String contentId,
                                          @RequestParam(defaultValue = "Web") String deviceType,
                                          @RequestParam(required = false) String bandwidth) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            String region = userOpt.get().getRegion();
            Map<String, Object> playbackInfo = streamingService.getPlaybackInfo(contentId, userId, region);
            
            return ResponseEntity.ok(playbackInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/stream/progress")
    public ResponseEntity<?> updateWatchProgress(@RequestParam String userId,
                                               @RequestParam String contentId,
                                               @RequestParam int currentPosition,
                                               @RequestParam(defaultValue = "720p") String quality) {
        try {
            streamingService.updateWatchProgress(userId, contentId, currentPosition, quality);
            return ResponseEntity.ok(Map.of("status", "progress updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/stream/resume/{userId}/{contentId}")
    public ResponseEntity<?> getResumePosition(@PathVariable String userId, @PathVariable String contentId) {
        Integer resumePosition = streamingService.getResumePosition(userId, contentId);
        return ResponseEntity.ok(Map.of("resumePosition", resumePosition));
    }
    
    // Adaptive Streaming
    @GetMapping("/stream/adaptive")
    public ResponseEntity<?> getAdaptiveStream(@RequestParam String contentId,
                                             @RequestParam String userId,
                                             @RequestParam String bandwidth) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            String region = userOpt.get().getRegion();
            String streamUrl = streamingService.getAdaptiveStreamingUrl(contentId, region, bandwidth);
            
            return ResponseEntity.ok(Map.of("streamUrl", streamUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Health Check
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Netflix Clone",
            "timestamp", System.currentTimeMillis()
        ));
    }
}