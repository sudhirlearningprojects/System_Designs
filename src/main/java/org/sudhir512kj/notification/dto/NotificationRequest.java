package org.sudhir512kj.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.sudhir512kj.notification.model.*;
import java.time.Instant;
import java.util.*;

@Data
public class NotificationRequest {
    @NotBlank
    private String userId;
    
    @NotNull
    private NotificationType type;
    
    @NotNull
    private NotificationPriority priority = NotificationPriority.MEDIUM;
    
    @NotEmpty
    private List<Channel> channels;
    
    @NotBlank
    private String templateId;
    
    private Map<String, Object> templateData = new HashMap<>();
    
    private Instant scheduledAt;
    
    private String idempotencyKey;
}
