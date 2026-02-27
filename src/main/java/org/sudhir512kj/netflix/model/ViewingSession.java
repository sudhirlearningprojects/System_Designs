package org.sudhir512kj.netflix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Table("viewing_sessions")
public class ViewingSession {
    @Id
    private UUID sessionId;
    private UUID userId;
    private UUID contentId;
    private String deviceId;
    private Instant startTime;
    private Instant endTime;
    private Integer watchedSeconds;
    private String quality;
    private Integer bandwidth;
    
    public ViewingSession() {
        this.sessionId = UUID.randomUUID();
        this.startTime = Instant.now();
    }
    
    public ViewingSession(UUID userId, UUID contentId, String deviceId) {
        this();
        this.userId = userId;
        this.contentId = contentId;
        this.deviceId = deviceId;
    }
    
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getContentId() { return contentId; }
    public void setContentId(UUID contentId) { this.contentId = contentId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public Integer getWatchedSeconds() { return watchedSeconds; }
    public void setWatchedSeconds(Integer watchedSeconds) { this.watchedSeconds = watchedSeconds; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public Integer getBandwidth() { return bandwidth; }
    public void setBandwidth(Integer bandwidth) { this.bandwidth = bandwidth; }
}