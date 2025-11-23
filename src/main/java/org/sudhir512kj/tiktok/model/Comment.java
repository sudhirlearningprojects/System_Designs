package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_video_created", columnList = "videoId,createdAt"),
    @Index(name = "idx_parent", columnList = "parentCommentId")
})
@Data
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;
    
    @Column(nullable = false)
    private Long videoId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 500)
    private String content;
    
    private Long parentCommentId;
    
    @Column(nullable = false)
    private Long likeCount = 0L;
    
    @Column(nullable = false)
    private Long replyCount = 0L;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
