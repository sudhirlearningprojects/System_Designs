package org.sudhir512kj.tiktok.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sudhir512kj.tiktok.model.LiveStream;
import org.sudhir512kj.tiktok.repository.LiveStreamRepository;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final LiveStreamRepository liveStreamRepository;
    
    public void startStreamMetrics(Long streamId) {
        String key = "analytics:stream:" + streamId;
        redisTemplate.opsForHash().put(key, "startTime", System.currentTimeMillis());
        redisTemplate.opsForHash().put(key, "totalViewers", 0L);
        redisTemplate.opsForHash().put(key, "peakViewers", 0L);
        redisTemplate.opsForHash().put(key, "chatMessages", 0L);
        redisTemplate.opsForHash().put(key, "likes", 0L);
        log.info("Started metrics collection for stream: {}", streamId);
    }
    
    @Scheduled(fixedRate = 5000)
    public void collectMetrics() {
        Set<Object> activeStreams = redisTemplate.opsForSet().members("live:active");
        if (activeStreams == null) return;
        
        activeStreams.forEach(streamIdObj -> {
            Long streamId = (Long) streamIdObj;
            
            Long viewerCount = getViewerCount(streamId);
            Long chatRate = getChatMessageRate(streamId);
            Long likeRate = getLikeRate(streamId);
            
            updatePeakViewerCount(streamId, viewerCount);
            
            log.debug("Stream metrics: streamId={}, viewers={}, chatRate={}, likeRate={}", 
                streamId, viewerCount, chatRate, likeRate);
        });
    }
    
    public StreamAnalytics getStreamAnalytics(Long streamId) {
        String key = "analytics:stream:" + streamId;
        
        Long totalViewers = (Long) redisTemplate.opsForHash().get(key, "totalViewers");
        Long peakViewers = (Long) redisTemplate.opsForHash().get(key, "peakViewers");
        Long chatMessages = (Long) redisTemplate.opsForHash().get(key, "chatMessages");
        Long likes = (Long) redisTemplate.opsForHash().get(key, "likes");
        
        return StreamAnalytics.builder()
            .streamId(streamId)
            .totalViewers(totalViewers != null ? totalViewers : 0L)
            .peakViewers(peakViewers != null ? peakViewers : 0L)
            .totalChatMessages(chatMessages != null ? chatMessages : 0L)
            .totalLikes(likes != null ? likes : 0L)
            .build();
    }
    
    private Long getViewerCount(Long streamId) {
        return redisTemplate.opsForSet().size("live:viewers:" + streamId);
    }
    
    private Long getChatMessageRate(Long streamId) {
        return (Long) redisTemplate.opsForValue()
            .get("analytics:chat:rate:" + streamId);
    }
    
    private Long getLikeRate(Long streamId) {
        return (Long) redisTemplate.opsForValue()
            .get("analytics:like:rate:" + streamId);
    }
    
    private void updatePeakViewerCount(Long streamId, Long currentCount) {
        LiveStream stream = liveStreamRepository.findById(streamId).orElse(null);
        if (stream != null && currentCount > stream.getPeakViewerCount()) {
            stream.setPeakViewerCount(currentCount);
            liveStreamRepository.save(stream);
        }
    }
    
    public void incrementChatCount(Long streamId) {
        String key = "analytics:stream:" + streamId;
        redisTemplate.opsForHash().increment(key, "chatMessages", 1);
    }
    
    @Data
    @Builder
    public static class StreamAnalytics {
        private Long streamId;
        private Long totalViewers;
        private Long peakViewers;
        private Long totalChatMessages;
        private Long totalLikes;
    }
}
