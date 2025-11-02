package org.sudhir512kj.jobscheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_executions", indexes = {
    @Index(name = "idx_job_id_started_at", columnList = "job_id, started_at"),
    @Index(name = "idx_status_completed_at", columnList = "status, completed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecution {
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    
    @Column(name = "execution_id", nullable = false)
    private String executionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(columnDefinition = "JSONB")
    private String result;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "executor_node")
    private String executorNode;
    
    public enum ExecutionStatus {
        RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
    }
}