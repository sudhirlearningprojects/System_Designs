package org.sudhir512kj.spotify.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class CDNService {
    
    private static final List<String> CDN_POPS = List.of(
        "us-east-1.cdn.spotify.com",
        "us-west-1.cdn.spotify.com",
        "eu-west-1.cdn.spotify.com",
        "ap-southeast-1.cdn.spotify.com"
    );
    
    public String getOptimalCDNEndpoint(String userLocation, String trackId) {
        // In production: Use GeoDNS or latency-based routing
        // For now: Simple region-based routing
        
        String region = extractRegion(userLocation);
        String cdnPop = selectCDNPop(region);
        
        log.info("Routing user from {} to CDN PoP: {}", userLocation, cdnPop);
        return cdnPop;
    }
    
    private String extractRegion(String location) {
        if (location == null) return "us-east-1";
        
        if (location.startsWith("US") || location.startsWith("CA")) {
            return "us-east-1";
        } else if (location.startsWith("EU") || location.startsWith("GB")) {
            return "eu-west-1";
        } else if (location.startsWith("AS") || location.startsWith("IN")) {
            return "ap-southeast-1";
        } else {
            return "us-west-1";
        }
    }
    
    private String selectCDNPop(String region) {
        // In production: Health check and load-based selection
        return CDN_POPS.stream()
            .filter(pop -> pop.startsWith(region))
            .findFirst()
            .orElse(CDN_POPS.get(0));
    }
    
    public String generateSignedUrl(String cdnEndpoint, String trackPath, int expirySeconds) {
        // In production: Generate signed URL with expiry and signature
        // For now: Simple URL construction
        long expiryTime = System.currentTimeMillis() / 1000 + expirySeconds;
        String signature = generateSignature(trackPath, expiryTime);
        
        return String.format("https://%s%s?expires=%d&signature=%s", 
            cdnEndpoint, trackPath, expiryTime, signature);
    }
    
    private String generateSignature(String path, long expiry) {
        // In production: HMAC-SHA256 signature
        return Integer.toHexString((path + expiry).hashCode());
    }
    
    public void invalidateCache(String trackId) {
        log.info("Invalidating CDN cache for track: {}", trackId);
        // In production: Send purge request to CDN
        // POST https://api.cdn.com/purge with track URLs
    }
    
    public void warmCache(String trackId, List<String> regions) {
        log.info("Warming CDN cache for track {} in regions: {}", trackId, regions);
        // In production: Pre-fetch content to edge PoPs
        // POST https://api.cdn.com/prefetch with track URLs
    }
}
