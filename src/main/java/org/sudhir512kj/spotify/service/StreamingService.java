package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.spotify.dto.StreamRequest;
import org.sudhir512kj.spotify.model.ListeningHistory;
import org.sudhir512kj.spotify.repository.ListeningHistoryRepository;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {
    private final StorageService storageService;
    private final TrackService trackService;
    private final ListeningHistoryRepository listeningHistoryRepository;
    
    public byte[] streamTrack(StreamRequest request) {
        log.info("Streaming track {} for user {}", request.getTrackId(), request.getUserId());
        
        // Get audio file based on quality
        String quality = mapQuality(request.getAudioQuality());
        byte[] audioData = storageService.getAudioFile(request.getTrackId(), quality);
        
        // Record listening history asynchronously
        recordListeningHistory(request).subscribe();
        
        // Increment play count
        trackService.incrementPlayCount(request.getTrackId());
        
        return audioData;
    }
    
    private Mono<ListeningHistory> recordListeningHistory(StreamRequest request) {
        ListeningHistory history = ListeningHistory.builder()
            .id(UUID.randomUUID())
            .userId(request.getUserId())
            .trackId(request.getTrackId())
            .playedAt(Instant.now())
            .deviceType(request.getDeviceId())
            .isOffline(request.getIsOffline())
            .audioQuality(request.getAudioQuality())
            .build();
        
        return listeningHistoryRepository.save(history);
    }
    
    private String mapQuality(String quality) {
        return switch (quality) {
            case "LOW" -> "96kbps";
            case "MEDIUM" -> "160kbps";
            case "HIGH" -> "320kbps";
            case "LOSSLESS" -> "lossless";
            default -> "160kbps";
        };
    }
}
