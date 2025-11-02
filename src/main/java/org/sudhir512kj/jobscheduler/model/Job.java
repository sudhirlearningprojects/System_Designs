package org.sudhir512kj.jobscheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_status_scheduled_at", columnList = "status, scheduled_at"),
    @Index(name = "idx_next_execution", columnList = "next_execution_at"),
    @Index(name = "idx_type_status", columnList = "type, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;
    
    @Column(name = "schedule_value", nullable = false, columnDefinition = "TEXT")
    private String scheduleValue;
    
    @Column(columnDefinition = "JSONB")
    private String payload;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;
    
    @Column(nullable = false)
    private Integer priority = 5;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "current_retries")
    private Integer currentRetries = 0;
    
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 300;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;
    
    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(columnDefinition = "JSONB")
    private String tags;
    
    @PreUpdate
    public void preUpdate() {
        // Auto-update logic if needed
    }
    
    public enum ScheduleType {
        ONCE, CRON, INTERVAL
    }
    
    public enum JobStatus {
        SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED, PAUSED, RETRYING
    }
}