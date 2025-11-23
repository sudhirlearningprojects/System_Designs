package org.sudhir512kj.tiktok.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.tiktok.dto.LiveStreamRequest;
import org.sudhir512kj.tiktok.model.LiveStream;
import org.sudhir512kj.tiktok.repository.LiveStreamRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LiveStreamService {
    private final LiveStreamRepository liveStreamRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Transactional
    public LiveStream createLiveStream(Long userId, LiveStreamRequest request) {
        // Check if user already has active stream
        Optional<LiveStream> existing = liveStreamRepository
            .findByUserIdAndStatus(userId, LiveStream.StreamStatus.LIVE);
        if (existing.isPresent()) {
            throw new RuntimeException("User already has an active stream");
        }
        
        LiveStream stream = new LiveStream();
        stream.setUserId(userId);
        stream.setStreamKey(UUID.randomUUID().toString());
        stream.setTitle(request.getTitle());
        stream.setDescription(request.getDescription());
        stream.setStatus(LiveStream.StreamStatus.SCHEDULED);
        
        // Generate RTMP and HLS URLs
        String streamKey = stream.getStreamKey();
        stream.setRtmpUrl("rtmp://live.tiktok.com/live/" + streamKey);
        stream.setHlsUrl("https://cdn.tiktok.com/live/" + streamKey + "/index.m3u8");
        
        return liveStreamRepository.save(stream);
    }
    
    @Transactional
    public void startStream(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new RuntimeException("Stream not found"));
        
        stream.setStatus(LiveStream.StreamStatus.LIVE);
        stream.setStartedAt(LocalDateTime.now());
        liveStreamRepository.save(stream);
        
        // Add to active streams in Redis
        redisTemplate.opsForSet().add("live:active", stream.getStreamId());
    }
    
    @Transactional
    public void endStream(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new RuntimeException("Stream not found"));
        
        stream.setStatus(LiveStream.StreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        liveStreamRepository.save(stream);
        
        // Remove from active streams
        redisTemplate.opsForSet().remove("live:active", stream.getStreamId());
        redisTemplate.delete("live:viewers:" + stream.getStreamId());
    }
    
    public List<LiveStream> getActiveLiveStreams() {
        String cacheKey = "live:list";
        List<LiveStream> cached = (List<LiveStream>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;
        
        List<LiveStream> streams = liveStreamRepository.findByStatus(LiveStream.StreamStatus.LIVE);
        redisTemplate.opsForValue().set(cacheKey, streams, 10, TimeUnit.SECONDS);
        
        return streams;
    }
    
    @Transactional
    public void joinStream(Long streamId, Long userId) {
        LiveStream stream = liveStreamRepository.findById(streamId)
            .orElseThrow(() -> new RuntimeException("Stream not found"));
        
        // Add viewer to Redis set
        String viewerKey = "live:viewers:" + streamId;
        redisTemplate.opsForSet().add(viewerKey, userId);
        
        // Update viewer count
        Long viewerCount = redisTemplate.opsForSet().size(viewerKey);
        stream.setViewerCount(viewerCount);
        
        if (viewerCount > stream.getPeakViewerCount()) {
            stream.setPeakViewerCount(viewerCount);
        }
        
        liveStreamRepository.save(stream);
    }
    
    @Transactional
    public void leaveStream(Long streamId, Long userId) {
        String viewerKey = "live:viewers:" + streamId;
        redisTemplate.opsForSet().remove(viewerKey, userId);
        
        LiveStream stream = liveStreamRepository.findById(streamId)
            .orElseThrow(() -> new RuntimeException("Stream not found"));
        
        Long viewerCount = redisTemplate.opsForSet().size(viewerKey);
        stream.setViewerCount(viewerCount);
        liveStreamRepository.save(stream);
    }
}
