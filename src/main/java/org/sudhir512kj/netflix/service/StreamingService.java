package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.Content;
import org.sudhir512kj.netflix.model.WatchHistory;
import org.sudhir512kj.netflix.repository.ContentRepository;
import org.sudhir512kj.netflix.repository.WatchHistoryRepository;

import java.util.Map;
import java.util.Optional;

@Service
public class StreamingService {
    
    @Autowired
    private CDNService cdnService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    
    public Map<String, String> getStreamingUrls(String contentId, String userId, String region, String deviceType) {
        // Validate content exists
        Optional<Content> content = contentRepository.findById(contentId);
        if (content.isEmpty()) {
            throw new RuntimeException("Content not found");
        }
        
        // Get CDN URLs for different qualities
        Map<String, String> streamingUrls = cdnService.getVideoUrls(contentId, region);
        
        // Create or update watch history
        createOrUpdateWatchHistory(userId, contentId, deviceType, content.get());
        
        // Increment view count
        Content contentEntity = content.get();
        contentEntity.setViewCount(contentEntity.getViewCount() + 1);
        contentRepository.save(contentEntity);
        
        return streamingUrls;
    }
    
    public String getAdaptiveStreamingUrl(String contentId, String region, String bandwidth) {
        String quality = selectQualityBasedOnBandwidth(bandwidth);
        Map<String, String> urls = cdnService.getVideoUrls(contentId, region);
        return urls.get(quality);
    }
    
    private String selectQualityBasedOnBandwidth(String bandwidth) {
        if (bandwidth == null) return "720p";
        
        int bw = Integer.parseInt(bandwidth.replaceAll("[^0-9]", ""));
        
        if (bw >= 25000) return "4K";      // 25+ Mbps
        if (bw >= 5000) return "1080p";    // 5+ Mbps
        if (bw >= 3000) return "720p";     // 3+ Mbps
        return "360p";                     // < 3 Mbps
    }
    
    public void updateWatchProgress(String userId, String contentId, int currentPosition, String quality) {
        Optional<WatchHistory> historyOpt = watchHistoryRepository.findByUserIdAndContentId(userId, contentId);
        
        if (historyOpt.isPresent()) {
            WatchHistory history = historyOpt.get();
            history.setLastWatchedPosition(currentPosition);
            history.setWatchDurationSeconds(Math.max(history.getWatchDurationSeconds(), currentPosition));
            history.setQualityWatched(quality);
            watchHistoryRepository.save(history);
        }
    }
    
    public Integer getResumePosition(String userId, String contentId) {
        Optional<WatchHistory> history = watchHistoryRepository.findByUserIdAndContentId(userId, contentId);
        return history.map(WatchHistory::getLastWatchedPosition).orElse(0);
    }
    
    private void createOrUpdateWatchHistory(String userId, String contentId, String deviceType, Content content) {
        Optional<WatchHistory> existingHistory = watchHistoryRepository.findByUserIdAndContentId(userId, contentId);
        
        if (existingHistory.isEmpty()) {
            WatchHistory history = new WatchHistory(userId, contentId, content.getDurationMinutes() * 60);
            history.setDeviceType(deviceType);
            watchHistoryRepository.save(history);
        }
    }
    
    public Map<String, Object> getPlaybackInfo(String contentId, String userId, String region) {
        Optional<Content> content = contentRepository.findById(contentId);
        if (content.isEmpty()) {
            throw new RuntimeException("Content not found");
        }
        
        Content contentEntity = content.get();
        Integer resumePosition = getResumePosition(userId, contentId);
        Map<String, String> streamingUrls = cdnService.getVideoUrls(contentId, region);
        
        return Map.of(
            "contentId", contentId,
            "title", contentEntity.getTitle(),
            "duration", contentEntity.getDurationMinutes() * 60,
            "resumePosition", resumePosition,
            "streamingUrls", streamingUrls,
            "thumbnailUrl", contentEntity.getThumbnailUrl() != null ? contentEntity.getThumbnailUrl() : "",
            "subtitles", Map.of(
                "en", cdnService.selectOptimalCDN(region, contentId) + "/subtitles/en.vtt",
                "es", cdnService.selectOptimalCDN(region, contentId) + "/subtitles/es.vtt"
            )
        );
    }
}