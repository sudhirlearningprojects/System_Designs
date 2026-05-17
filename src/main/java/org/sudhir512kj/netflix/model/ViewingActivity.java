package org.sudhir512kj.netflix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "viewing_activity")
public class ViewingActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID userId;
    private UUID contentId;
    private LocalDateTime watchedAt;
    private Integer watchDuration;
    
    @Enumerated(EnumType.STRING)
    private VideoQuality quality;
    
    public ViewingActivity() {}
    
    public ViewingActivity(UUID userId, UUID contentId) {
        this.userId = userId;
        this.contentId = contentId;
        this.watchedAt = LocalDateTime.now();
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public UUID getContentId() { return contentId; }
    public void setContentId(UUID contentId) { this.contentId = contentId; }
    
    public LocalDateTime getWatchedAt() { return watchedAt; }
    public void setWatchedAt(LocalDateTime watchedAt) { this.watchedAt = watchedAt; }
    
    public Integer getWatchDuration() { return watchDuration; }
    public void setWatchDuration(Integer watchDuration) { this.watchDuration = watchDuration; }
    
    public VideoQuality getQuality() { return quality; }
    public void setQuality(VideoQuality quality) { this.quality = quality; }
}
