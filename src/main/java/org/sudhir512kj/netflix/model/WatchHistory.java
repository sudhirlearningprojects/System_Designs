package org.sudhir512kj.netflix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watch_history")
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "content_id", nullable = false)
    private String contentId;
    
    @Column(name = "watch_duration_seconds")
    private Integer watchDurationSeconds;
    
    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;
    
    @Column(name = "completion_percentage")
    private Double completionPercentage;
    
    @Column(name = "last_watched_position")
    private Integer lastWatchedPosition; // Resume position in seconds
    
    @Column(name = "watched_at")
    private LocalDateTime watchedAt;
    
    @Column(name = "device_type")
    private String deviceType; // TV, Mobile, Web, Tablet
    
    @Column(name = "quality_watched")
    private String qualityWatched; // 360p, 720p, 1080p, 4K
    
    @Column(name = "is_completed")
    private Boolean isCompleted = false;
    
    // Constructors
    public WatchHistory() {}
    
    public WatchHistory(String userId, String contentId, Integer totalDurationSeconds) {
        this.userId = userId;
        this.contentId = contentId;
        this.totalDurationSeconds = totalDurationSeconds;
        this.watchedAt = LocalDateTime.now();
        this.watchDurationSeconds = 0;
        this.lastWatchedPosition = 0;
        this.completionPercentage = 0.0;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    
    public Integer getWatchDurationSeconds() { return watchDurationSeconds; }
    public void setWatchDurationSeconds(Integer watchDurationSeconds) { 
        this.watchDurationSeconds = watchDurationSeconds;
        updateCompletionPercentage();
    }
    
    public Integer getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(Integer totalDurationSeconds) { 
        this.totalDurationSeconds = totalDurationSeconds;
        updateCompletionPercentage();
    }
    
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public Integer getLastWatchedPosition() { return lastWatchedPosition; }
    public void setLastWatchedPosition(Integer lastWatchedPosition) { this.lastWatchedPosition = lastWatchedPosition; }
    
    public LocalDateTime getWatchedAt() { return watchedAt; }
    public void setWatchedAt(LocalDateTime watchedAt) { this.watchedAt = watchedAt; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public String getQualityWatched() { return qualityWatched; }
    public void setQualityWatched(String qualityWatched) { this.qualityWatched = qualityWatched; }
    
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    
    private void updateCompletionPercentage() {
        if (totalDurationSeconds != null && totalDurationSeconds > 0 && watchDurationSeconds != null) {
            this.completionPercentage = (double) watchDurationSeconds / totalDurationSeconds * 100;
            this.isCompleted = completionPercentage >= 90.0; // Consider 90% as completed
        }
    }
}