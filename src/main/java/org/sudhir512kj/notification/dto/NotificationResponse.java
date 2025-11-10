package org.sudhir512kj.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private boolean success;
    private String notificationId;
    private String message;
}
