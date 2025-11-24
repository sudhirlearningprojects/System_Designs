package org.sudhir512kj.tiktok.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.tiktok.model.LiveStream;
import org.sudhir512kj.tiktok.model.User;
import org.sudhir512kj.tiktok.repository.LiveStreamRepository;
import org.sudhir512kj.tiktok.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaIngestionService {
    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    
    public boolean authenticateStream(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new RuntimeException("Invalid stream key"));
        
        if (stream.getStatus() != LiveStream.StreamStatus.SCHEDULED) {
            throw new IllegalStateException("Stream not in SCHEDULED state");
        }
        
        User user = userRepository.findById(stream.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!canStartLiveStream(user)) {
            throw new RuntimeException("User not authorized for live streaming");
        }
        
        return true;
    }
    
    @Transactional
    public void onStreamStart(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new RuntimeException("Stream not found"));
        
        stream.setStatus(LiveStream.StreamStatus.LIVE);
        stream.setStartedAt(java.time.LocalDateTime.now());
        liveStreamRepository.save(stream);
        
        notificationService.notifyFollowers(stream.getUserId(), 
            "started a live stream: " + stream.getTitle());
        
        analyticsService.startStreamMetrics(stream.getStreamId());
        
        log.info("Stream started: streamId={}, userId={}", 
            stream.getStreamId(), stream.getUserId());
    }
    
    private boolean canStartLiveStream(User user) {
        // Check if user has minimum followers or is verified
        return user.getFollowerCount() >= 1000 || user.getIsVerified();
    }
}
