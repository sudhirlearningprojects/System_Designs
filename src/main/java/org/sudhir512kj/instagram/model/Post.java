package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_user_created", columnList = "userId, createdAt"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String postId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ElementCollection
    @CollectionTable(name = "post_media_urls")
    private List<String> mediaUrls;
    
    @ElementCollection
    @CollectionTable(name = "post_hashtags")
    private Set<String> hashtags;
    
    private String location;
    
    @Column(nullable = false)
    private Integer likeCount = 0;
    
    @Column(nullable = false)
    private Integer commentCount = 0;
    
    @Column(nullable = false)
    private Integer shareCount = 0;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}