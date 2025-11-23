package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_streams", indexes = {
    @Index(name = "idx_user_status", columnList = "userId,status"),
    @Index(name = "idx_status_started", columnList = "status,startedAt")
})
@Data
public class LiveStream {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long streamId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, unique = true)
    private String streamKey;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamStatus status = StreamStatus.SCHEDULED;
    
    @Column(nullable = false)
    private String rtmpUrl;
    
    @Column(nullable = false)
    private String hlsUrl;
    
    @Column(nullable = false)
    private Long viewerCount = 0L;
    
    @Column(nullable = false)
    private Long peakViewerCount = 0L;
    
    @Column(nullable = false)
    private Long likeCount = 0L;
    
    @Column(nullable = false)
    private Long giftCount = 0L;
    
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum StreamStatus {
        SCHEDULED, LIVE, ENDED, CANCELLED
    }
}
