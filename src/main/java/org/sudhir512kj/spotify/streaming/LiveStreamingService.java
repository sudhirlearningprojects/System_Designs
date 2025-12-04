package org.sudhir512kj.spotify.streaming;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LiveStreamingService {
    
    private final ConcurrentHashMap<String, LiveStream> activeStreams = new ConcurrentHashMap<>();
    
    public LiveStream createLiveStream(String streamId, String title) {
        LiveStream stream = new LiveStream();
        stream.setStreamId(streamId);
        stream.setTitle(title);
        stream.setStartTime(Instant.now());
        stream.setStatus("LIVE");
        stream.setViewerCount(0);
        
        activeStreams.put(streamId, stream);
        log.info("Created live stream: {}", streamId);
        
        return stream;
    }
    
    public void ingestChunk(String streamId, byte[] chunkData, int chunkNumber) {
        LiveStream stream = activeStreams.get(streamId);
        if (stream == null) {
            throw new RuntimeException("Stream not found: " + streamId);
        }
        
        // In production: 
        // 1. Transcode chunk to multiple bitrates (96/160/320 kbps)
        // 2. Upload to CDN
        // 3. Update HLS manifest (.m3u8)
        
        log.info("Ingested chunk {} for stream {}", chunkNumber, streamId);
        stream.getChunks().add(chunkNumber);
        stream.setLatestChunk(chunkNumber);
    }
    
    public String getHLSManifest(String streamId) {
        LiveStream stream = activeStreams.get(streamId);
        if (stream == null) {
            throw new RuntimeException("Stream not found: " + streamId);
        }
        
        // Generate HLS manifest
        StringBuilder manifest = new StringBuilder();
        manifest.append("#EXTM3U\n");
        manifest.append("#EXT-X-VERSION:3\n");
        manifest.append("#EXT-X-TARGETDURATION:6\n");
        manifest.append("#EXT-X-MEDIA-SEQUENCE:").append(stream.getLatestChunk() - 5).append("\n");
        
        // Last 5 chunks (30 seconds buffer)
        int startChunk = Math.max(0, stream.getLatestChunk() - 5);
        for (int i = startChunk; i <= stream.getLatestChunk(); i++) {
            manifest.append("#EXTINF:6.0,\n");
            manifest.append(String.format("chunk_%d.aac\n", i));
        }
        
        return manifest.toString();
    }
    
    public void incrementViewerCount(String streamId) {
        LiveStream stream = activeStreams.get(streamId);
        if (stream != null) {
            stream.setViewerCount(stream.getViewerCount() + 1);
        }
    }
    
    public void endLiveStream(String streamId) {
        LiveStream stream = activeStreams.get(streamId);
        if (stream != null) {
            stream.setStatus("ENDED");
            stream.setEndTime(Instant.now());
            log.info("Ended live stream: {} with {} viewers", streamId, stream.getViewerCount());
        }
    }
    
    @Data
    public static class LiveStream {
        private String streamId;
        private String title;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private int viewerCount;
        private int latestChunk;
        private List<Integer> chunks = new ArrayList<>();
    }
}
