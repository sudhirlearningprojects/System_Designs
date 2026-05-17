package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.EncodedVideo;
import org.sudhir512kj.netflix.model.EncodingStatus;
import org.sudhir512kj.netflix.model.VideoQuality;
import org.sudhir512kj.netflix.repository.EncodedVideoRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EncodingService {
    
    @Autowired
    private EncodedVideoRepository encodedVideoRepository;
    
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void initiateEncoding(UUID contentId, String sourceUrl) {
        startEncoding(sourceUrl, contentId);
    }
    
    public UUID startEncoding(String sourceUrl, UUID contentId) {
        EncodedVideo encodedVideo = new EncodedVideo();
        encodedVideo.setEncodingId(UUID.randomUUID());
        encodedVideo.setContentId(contentId);
        encodedVideo.setSourceUrl(sourceUrl);
        encodedVideo.setStatus(EncodingStatus.IN_PROGRESS);
        encodedVideo.setCreatedAt(Instant.now());
        
        EncodedVideo saved = encodedVideoRepository.save(encodedVideo);
        processEncoding(saved.getEncodingId(), contentId);
        return saved.getEncodingId();
    }
    
    private void processEncoding(UUID encodingId, UUID contentId) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                
                EncodedVideo video = encodedVideoRepository.findById(encodingId).orElse(null);
                if (video != null) {
                    video.setStatus(EncodingStatus.COMPLETED);
                    video.setCompletedAt(Instant.now());
                    
                    Map<VideoQuality, String> urls = Map.of(
                        VideoQuality.SD_360P, "https://cdn.netflix.com/" + contentId + "/360p/playlist.m3u8",
                        VideoQuality.HD_720P, "https://cdn.netflix.com/" + contentId + "/720p/playlist.m3u8",
                        VideoQuality.FHD_1080P, "https://cdn.netflix.com/" + contentId + "/1080p/playlist.m3u8",
                        VideoQuality.UHD_4K, "https://cdn.netflix.com/" + contentId + "/4k/playlist.m3u8"
                    );
                    video.setEncodedUrls(urls);
                    encodedVideoRepository.save(video);
                    
                    if (kafkaTemplate != null) {
                        Map<String, Object> event = Map.of(
                            "eventType", "ENCODING_COMPLETED",
                            "encodingId", encodingId.toString(),
                            "contentId", contentId.toString(),
                            "timestamp", Instant.now().toString()
                        );
                        kafkaTemplate.send("encoding-events", event);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public Optional<EncodedVideo> getEncodingStatus(UUID encodingId) {
        return encodedVideoRepository.findById(encodingId);
    }
}
