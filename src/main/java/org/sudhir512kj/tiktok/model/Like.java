package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "likes", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "videoId"}),
    indexes = {
        @Index(name = "idx_user_video", columnList = "userId,videoId"),
        @Index(name = "idx_video_created", columnList = "videoId,createdAt")
    }
)
@Data
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long videoId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
