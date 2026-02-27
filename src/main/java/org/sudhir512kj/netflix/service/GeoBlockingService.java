package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.UUID;

@Service
public class GeoBlockingService {
    
    public Mono<Boolean> isContentAvailableInRegion(UUID contentId, String region) {
        // In production, this would check content licensing by region
        Set<String> restrictedRegions = Set.of("CN", "KP", "IR"); // Example restrictions
        return Mono.just(!restrictedRegions.contains(region));
    }
    
    public Mono<String> getUserRegion(UUID userId) {
        // In production, this would determine user's region from IP/account
        return Mono.just("US"); // Simplified for demo
    }
    
    public Mono<Boolean> canUserAccessContent(UUID userId, UUID contentId) {
        return getUserRegion(userId)
            .flatMap(region -> isContentAvailableInRegion(contentId, region));
    }
}