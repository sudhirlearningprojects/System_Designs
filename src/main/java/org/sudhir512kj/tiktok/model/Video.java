package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos", indexes = {
    @Index(name = "idx_user_created", columnList = "userId,createdAt"),
    @Index(name = "idx_created", columnList = "createdAt")
})
@Data
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String videoUrl;
    
    @Column(nullable = false)
    private String thumbnailUrl;
    
    @Column(length = 2200)
    private String caption;
    
    @Column(nullable = false)
    private Integer durationSeconds;
    
    @Column(nullable = false)
    private Long viewCount = 0L;
    
    @Column(nullable = false)
    private Long likeCount = 0L;
    
    @Column(nullable = false)
    private Long commentCount = 0L;
    
    @Column(nullable = false)
    private Long shareCount = 0L;
    
    @Column(nullable = false)
    private String videoQuality = "720p";
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private Boolean isPublic = true;
    
    @Column(nullable = false)
    private Boolean allowComments = true;
    
    @Column(nullable = false)
    private Boolean allowDuet = true;
    
    @Column(nullable = false)
    private Boolean allowStitch = true;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
