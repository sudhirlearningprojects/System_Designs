package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.sudhir512kj.netflix.model.ViewingSession;
import org.sudhir512kj.netflix.model.ContentMetadata;
import org.sudhir512kj.netflix.repository.ViewingSessionRepository;
import org.sudhir512kj.netflix.repository.ContentRepository;
import java.time.Instant;
import java.util.Map;

@Service
public class StreamingService {
    
    @Autowired
    private ViewingSessionRepository sessionRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public Mono<String> startStream(Long userId, String contentId, String deviceId, String quality) {
        return contentRepository.findById(contentId)
            .switchIfEmpty(Mono.error(new RuntimeException("Content not found")))
            .flatMap(content -> {
                ViewingSession session = new ViewingSession(userId, contentId, deviceId);
                session.setQuality(quality);
                
                return sessionRepository.save(session)
                    .doOnSuccess(savedSession -> {
                        Map<String, Object> event = Map.of(
                            "eventType", "STREAM_START",
                            "userId", userId,
                            "contentId", contentId,
                            "sessionId", savedSession.getSessionId().toString(),
                            "timestamp", Instant.now()
                        );
                        kafkaTemplate.send("streaming-events", event);
                    })
                    .map(savedSession -> content.getVideoUrls().get(quality));
            });
    }
    
    public Mono<Void> updateProgress(String sessionId, Integer watchedSeconds) {
        return sessionRepository.findById(java.util.UUID.fromString(sessionId))
            .flatMap(session -> {
                session.setWatchedSeconds(watchedSeconds);
                return sessionRepository.save(session);
            })
            .doOnSuccess(session -> {
                Map<String, Object> event = Map.of(
                    "eventType", "PROGRESS_UPDATE",
                    "sessionId", sessionId,
                    "watchedSeconds", watchedSeconds,
                    "timestamp", Instant.now()
                );
                kafkaTemplate.send("streaming-events", event);
            })
            .then();
    }
}