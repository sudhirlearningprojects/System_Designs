package org.sudhir512kj.netflix.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Table("viewing_sessions")
public class ViewingSession {
    @PrimaryKey
    private UUID sessionId;
    private Long userId;
    private String contentId;
    private String deviceId;
    private Instant startTime;
    private Instant endTime;
    private Integer watchedSeconds;
    private String quality;
    
    public ViewingSession() {}
    
    public ViewingSession(Long userId, String contentId, String deviceId) {
        this.sessionId = UUID.randomUUID();
        this.userId = userId;
        this.contentId = contentId;
        this.deviceId = deviceId;
        this.startTime = Instant.now();
    }
    
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    
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
}