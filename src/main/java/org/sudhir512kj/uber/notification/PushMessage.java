package org.sudhir512kj.uber.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {
    private UUID messageId;
    private UUID userId;
    private String title;
    private String body;
    private Map<String, String> data;
    private Priority priority;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private MessageStatus status;
    
    public enum Priority {
        HIGH, MEDIUM, LOW
    }
    
    public enum MessageStatus {
        PENDING, SCHEDULED, SENT, DELIVERED, FAILED
    }
}
