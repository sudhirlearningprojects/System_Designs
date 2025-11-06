package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stories", indexes = {
    @Index(name = "idx_user_stories", columnList = "userId, createdAt"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String storyId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String mediaUrl;
    
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;
    
    private Integer duration = 15; // seconds
    
    private String backgroundColor;
    
    @Column(nullable = false)
    private Integer viewCount = 0;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    public enum MediaType {
        IMAGE, VIDEO
    }
}