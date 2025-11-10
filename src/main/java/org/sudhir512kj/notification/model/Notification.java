package org.sudhir512kj.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_scheduled_at", columnList = "scheduledAt")
})
@Data
public class Notification {
    @Id
    private String id = UUID.randomUUID().toString();
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;
    
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<Channel> channels = new ArrayList<>();
    
    private String templateId;
    
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> templateData = new HashMap<>();
    
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;
    
    private Instant scheduledAt;
    private Instant sentAt;
    private Instant deliveredAt;
    
    private int retryCount = 0;
    
    @Column(unique = true)
    private String idempotencyKey;
    
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
