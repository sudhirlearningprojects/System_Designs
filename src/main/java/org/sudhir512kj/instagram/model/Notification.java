package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_notifications", columnList = "userId, createdAt"),
    @Index(name = "idx_user_read", columnList = "userId, isRead")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String notificationId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long actorId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    private String entityId;
    
    @Column(nullable = false)
    private Boolean isRead = false;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum NotificationType {
        LIKE, COMMENT, FOLLOW, MENTION, STORY_VIEW
    }
}
