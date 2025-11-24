package org.sudhir512kj.tiktok.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessingWorker {
    
    @KafkaListener(topics = "video-processing", groupId = "tiktok-video-processor")
    public void processVideo(Map<String, Object> event) {
        String videoUrl = (String) event.get("videoUrl");
        String fileName = (String) event.get("fileName");
        
        log.info("Processing video: url={}, file={}", videoUrl, fileName);
        
        // Transcode video to multiple resolutions
        // Generate thumbnail
        // Update video metadata in database
    }
}
