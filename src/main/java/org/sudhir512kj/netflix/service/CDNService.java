package org.sudhir512kj.netflix.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class CDNService {
    
    private final Map<String, List<String>> regionServers = Map.of(
        "US-EAST", Arrays.asList("cdn-us-east-1.netflix.com", "cdn-us-east-2.netflix.com"),
        "US-WEST", Arrays.asList("cdn-us-west-1.netflix.com", "cdn-us-west-2.netflix.com"),
        "EU", Arrays.asList("cdn-eu-west-1.netflix.com", "cdn-eu-central-1.netflix.com"),
        "ASIA", Arrays.asList("cdn-ap-south-1.netflix.com", "cdn-ap-southeast-1.netflix.com")
    );
    
    private final Map<String, Integer> serverLoad = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastHealthCheck = new ConcurrentHashMap<>();
    
    @Cacheable(value = "cdnUrls", key = "#contentId + '_' + #region")
    public String selectOptimalCDN(String userRegion, String contentId) {
        List<String> servers = regionServers.getOrDefault(userRegion, regionServers.get("US-EAST"));
        
        // Select server with lowest load that's healthy
        String optimalServer = servers.stream()
            .filter(this::isServerHealthy)
            .min(Comparator.comparing(server -> serverLoad.getOrDefault(server, 0)))
            .orElse(servers.get(0)); // Fallback to first server
            
        // Increment load counter
        serverLoad.merge(optimalServer, 1, Integer::sum);
        
        return "https://" + optimalServer + "/content/" + contentId;
    }
    
    @Cacheable(value = "videoUrls", key = "#contentId + '_' + #region")
    public Map<String, String> getVideoUrls(String contentId, String region) {
        String baseUrl = selectOptimalCDN(region, contentId);
        return Map.of(
            "360p", baseUrl + "/360p/playlist.m3u8",
            "720p", baseUrl + "/720p/playlist.m3u8", 
            "1080p", baseUrl + "/1080p/playlist.m3u8",
            "4K", baseUrl + "/4k/playlist.m3u8"
        );
    }
    
    public boolean isServerHealthy(String serverUrl) {
        LocalDateTime lastCheck = lastHealthCheck.get(serverUrl);
        if (lastCheck == null || lastCheck.isBefore(LocalDateTime.now().minusMinutes(5))) {
            boolean healthy = new Random().nextDouble() > 0.05; // 95% uptime
            lastHealthCheck.put(serverUrl, LocalDateTime.now());
            return healthy;
        }
        return true; // Assume healthy if recently checked
    }
    
    public void decrementServerLoad(String serverUrl) {
        serverLoad.computeIfPresent(serverUrl, (k, v) -> Math.max(0, v - 1));
    }
    
    public Map<String, Integer> getServerLoadStats() {
        return new HashMap<>(serverLoad);
    }
}