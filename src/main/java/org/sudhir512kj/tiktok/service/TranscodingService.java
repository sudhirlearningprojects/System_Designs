package org.sudhir512kj.tiktok.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingService {
    private final ExecutorService transcodingPool = 
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    public void transcodeStream(String streamKey, String inputRtmpUrl) {
        List<TranscodingTask> tasks = Arrays.asList(
            new TranscodingTask("360p", 640, 360, 500),
            new TranscodingTask("480p", 854, 480, 1000),
            new TranscodingTask("720p", 1280, 720, 2000),
            new TranscodingTask("1080p", 1920, 1080, 4000)
        );
        
        tasks.forEach(task -> transcodingPool.submit(() -> {
            String command = String.format(
                "ffmpeg -i %s -vf scale=%d:%d -b:v %dk -c:v libx264 " +
                "-preset veryfast -g 60 -sc_threshold 0 " +
                "-c:a aac -b:a 128k -f flv rtmp://localhost/hls/%s_%s",
                inputRtmpUrl, task.width, task.height, task.bitrate,
                streamKey, task.quality
            );
            
            executeFFmpeg(command, streamKey, task.quality);
        }));
    }
    
    private void executeFFmpeg(String command, String streamKey, String quality) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("Transcoding completed: streamKey={}, quality={}", 
                    streamKey, quality);
            } else {
                log.error("Transcoding failed: streamKey={}, quality={}, exitCode={}", 
                    streamKey, quality, exitCode);
            }
        } catch (Exception e) {
            log.error("Transcoding error: streamKey={}, quality={}", 
                streamKey, quality, e);
        }
    }
    
    @Data
    @AllArgsConstructor
    static class TranscodingTask {
        String quality;
        int width;
        int height;
        int bitrate;
    }
}
