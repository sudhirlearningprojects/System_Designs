package org.sudhir512kj.spotify.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdaptiveBitrateService {
    
    private static final int CHUNK_DURATION_SECONDS = 10;
    private static final int BANDWIDTH_SAMPLE_SIZE = 5;
    
    public String selectOptimalQuality(long bandwidthBps, String subscriptionTier) {
        // Convert to kbps
        long bandwidthKbps = bandwidthBps / 1000;
        
        // Apply subscription tier limits
        String maxQuality = getMaxQualityForTier(subscriptionTier);
        
        // Select quality based on available bandwidth (with 30% buffer)
        long effectiveBandwidth = (long) (bandwidthKbps * 0.7);
        
        if (effectiveBandwidth >= 1411 && "LOSSLESS".equals(maxQuality)) {
            return "LOSSLESS"; // FLAC
        } else if (effectiveBandwidth >= 320 && isHighQualityAllowed(maxQuality)) {
            return "HIGH"; // 320 kbps
        } else if (effectiveBandwidth >= 160 && isMediumQualityAllowed(maxQuality)) {
            return "MEDIUM"; // 160 kbps
        } else {
            return "LOW"; // 96 kbps
        }
    }
    
    private String getMaxQualityForTier(String tier) {
        return switch (tier) {
            case "FREE" -> "LOW";
            case "INDIVIDUAL", "DUO", "FAMILY", "STUDENT" -> "HIGH";
            case "HIFI" -> "LOSSLESS";
            default -> "LOW";
        };
    }
    
    private boolean isHighQualityAllowed(String maxQuality) {
        return "HIGH".equals(maxQuality) || "LOSSLESS".equals(maxQuality);
    }
    
    private boolean isMediumQualityAllowed(String maxQuality) {
        return !"LOW".equals(maxQuality);
    }
    
    public int getChunkNumber(int currentPositionMs, int trackDurationMs) {
        return currentPositionMs / (CHUNK_DURATION_SECONDS * 1000);
    }
    
    public String getChunkPath(String trackId, int chunkNumber, String quality) {
        return String.format("/tracks/%s/chunk_%03d_%s.aac", trackId, chunkNumber, quality);
    }
}
