package org.sudhir512kj.instagram.dto;

import lombok.Data;
import lombok.Builder;
import org.sudhir512kj.instagram.model.Notification;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String notificationId;
    private Long actorId;
    private String actorUsername;
    private String actorProfilePictureUrl;
    private Notification.NotificationType type;
    private String entityId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
