package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.UUID;

@Service
public class GeoBlockingService {
    
    public boolean isContentAvailableInRegion(UUID contentId, String region) {
        Set<String> restrictedRegions = Set.of("CN", "KP", "IR");
        return !restrictedRegions.contains(region);
    }
    
    public String getUserRegion(UUID userId) {
        return "US";
    }
    
    public boolean canUserAccessContent(UUID userId, UUID contentId) {
        String region = getUserRegion(userId);
        return isContentAvailableInRegion(contentId, region);
    }
}
