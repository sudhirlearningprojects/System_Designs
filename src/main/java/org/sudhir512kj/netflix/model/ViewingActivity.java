package org.sudhir512kj.netflix.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.cql.Ordering;
import java.time.LocalDateTime;
import java.util.UUID;

@PrimaryKeyClass
class ViewingActivityKey {
    @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.PARTITIONED)
    private UUID userId;
    
    @PrimaryKeyColumn(name = "watched_at", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private LocalDateTime watchedAt;
    
    @PrimaryKeyColumn(name = "activity_id", type = PrimaryKeyType.CLUSTERED)
    private UUID activityId;
    
    public ViewingActivityKey() {}
    
    public ViewingActivityKey(UUID userId, LocalDateTime watchedAt, UUID activityId) {
        this.userId = userId;
        this.watchedAt = watchedAt;
        this.activityId = activityId;
    }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public LocalDateTime getWatchedAt() { return watchedAt; }
    public void setWatchedAt(LocalDateTime watchedAt) { this.watchedAt = watchedAt; }
    
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
}

@Table("viewing_activity")
public class ViewingActivity {
    @PrimaryKey
    private ViewingActivityKey key;
    private UUID contentId;
    private Integer watchDuration;
    private VideoQuality quality;
    
    public ViewingActivity() {}
    
    public ViewingActivity(UUID userId, UUID contentId) {
        this.key = new ViewingActivityKey(userId, LocalDateTime.now(), UUID.randomUUID());
        this.contentId = contentId;
    }
    
    public ViewingActivityKey getKey() { return key; }
    public void setKey(ViewingActivityKey key) { this.key = key; }
    
    public UUID getContentId() { return contentId; }
    public void setContentId(UUID contentId) { this.contentId = contentId; }
    
    public Integer getWatchDuration() { return watchDuration; }
    public void setWatchDuration(Integer watchDuration) { this.watchDuration = watchDuration; }
    
    public VideoQuality getQuality() { return quality; }
    public void setQuality(VideoQuality quality) { this.quality = quality; }
}