package org.sudhir512kj.spotify.streaming;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.spotify.model.Track;
import org.sudhir512kj.spotify.service.TrackService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrefetchService {
    
    private final TrackService trackService;
    private final CDNService cdnService;
    
    public void prefetchNextTrack(String currentTrackId, String nextTrackId, String userLocation) {
        if (nextTrackId == null) {
            return;
        }
        
        try {
            Track nextTrack = trackService.getTrack(nextTrackId);
            
            // Prefetch first 30 seconds (3 chunks of 10 seconds each)
            String cdnEndpoint = cdnService.getOptimalCDNEndpoint(userLocation, nextTrackId);
            
            for (int chunk = 0; chunk < 3; chunk++) {
                String chunkPath = String.format("/tracks/%s/chunk_%03d_320kbps.aac", nextTrackId, chunk);
                String signedUrl = cdnService.generateSignedUrl(cdnEndpoint, chunkPath, 300);
                
                log.debug("Prefetching chunk {} for track {}: {}", chunk, nextTrackId, signedUrl);
                // In production: Trigger background download
            }
            
            log.info("Prefetched first 30 seconds of track: {}", nextTrackId);
        } catch (Exception e) {
            log.error("Failed to prefetch track: {}", nextTrackId, e);
        }
    }
    
    public void prefetchPlaylist(String playlistId, String userLocation, int startIndex, int count) {
        log.info("Prefetching {} tracks from playlist {} starting at index {}", 
            count, playlistId, startIndex);
        
        // In production: Fetch playlist tracks and prefetch in background
        // Priority: Next 3 tracks, then next 10 tracks
    }
    
    public boolean shouldPrefetch(String networkType, int batteryLevel) {
        // Prefetch only on WiFi by default
        if (!"WIFI".equals(networkType)) {
            return false;
        }
        
        // Don't prefetch if battery is low
        if (batteryLevel < 20) {
            return false;
        }
        
        return true;
    }
}
