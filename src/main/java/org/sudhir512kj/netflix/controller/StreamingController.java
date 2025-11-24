package org.sudhir512kj.netflix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.sudhir512kj.netflix.model.ContentMetadata;
import org.sudhir512kj.netflix.service.StreamingService;
import org.sudhir512kj.netflix.service.RecommendationService;

@RestController
@RequestMapping("/api/v1/streaming")
public class StreamingController {
    
    @Autowired
    private StreamingService streamingService;
    
    @Autowired
    private RecommendationService recommendationService;
    
    @PostMapping("/start")
    public Mono<String> startStream(
            @RequestParam Long userId,
            @RequestParam String contentId,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "1080p") String quality) {
        return streamingService.startStream(userId, contentId, deviceId, quality);
    }
    
    @PutMapping("/progress/{sessionId}")
    public Mono<Void> updateProgress(
            @PathVariable String sessionId,
            @RequestParam Integer watchedSeconds) {
        return streamingService.updateProgress(sessionId, watchedSeconds);
    }
    
    @GetMapping("/recommendations/{userId}")
    public Flux<ContentMetadata> getRecommendations(@PathVariable Long userId) {
        return recommendationService.getPersonalizedRecommendations(userId);
    }
}