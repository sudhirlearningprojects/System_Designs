package org.sudhir512kj.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_logs", indexes = {
    @Index(name = "idx_notification_id", columnList = "notificationId"),
    @Index(name = "idx_user_channel", columnList = "userId,channel")
})
@Data
public class DeliveryLog {
    @Id
    private String id = UUID.randomUUID().toString();
    
    @Column(nullable = false)
    private String notificationId;
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private Channel channel;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private String providerId;
    private String errorMessage;
    private int attemptNumber;
    private Instant timestamp = Instant.now();
}
