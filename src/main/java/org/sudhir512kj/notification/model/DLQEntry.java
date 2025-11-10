package org.sudhir512kj.notification.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlq_entries")
@Data
public class DLQEntry {
    @Id
    private String id = UUID.randomUUID().toString();
    
    private String notificationId;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(columnDefinition = "TEXT")
    private String stackTrace;
    
    private int totalAttempts;
    
    @Enumerated(EnumType.STRING)
    private DLQReason reason;
    
    private Instant firstAttemptAt;
    private Instant lastAttemptAt;
    private Instant createdAt = Instant.now();
    
    private boolean reprocessed = false;
}
