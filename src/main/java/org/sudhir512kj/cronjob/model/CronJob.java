package org.sudhir512kj.cronjob.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cron_jobs", indexes = {
    @Index(name = "idx_cron_namespace_name", columnList = "namespace, name", unique = true),
    @Index(name = "idx_cron_status", columnList = "status"),
    @Index(name = "idx_cron_next_run", columnList = "next_run_at"),
    @Index(name = "idx_cron_dag_id", columnList = "dag_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronJob {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    private String timezone; // e.g., "America/New_York"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "task_config", columnDefinition = "TEXT")
    private String taskConfig; // JSON: URL, headers, body for HTTP; script for SHELL

    @Column(name = "dag_id")
    private UUID dagId;

    @Column(name = "depends_on", columnDefinition = "TEXT")
    private String dependsOn; // JSON array of job IDs this depends on

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 300;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "retry_delay_seconds")
    private Integer retryDelaySeconds = 60;

    @Column(name = "concurrency_policy")
    @Enumerated(EnumType.STRING)
    private ConcurrencyPolicy concurrencyPolicy = ConcurrencyPolicy.FORBID;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_run_status")
    @Enumerated(EnumType.STRING)
    private RunStatus lastRunStatus;

    @Column(name = "success_count")
    private Long successCount = 0L;

    @Column(name = "failure_count")
    private Long failureCount = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String labels; // JSON key-value pairs for filtering

    public enum JobStatus {
        ACTIVE, PAUSED, DISABLED
    }

    public enum TaskType {
        HTTP_WEBHOOK, SHELL_COMMAND, KAFKA_PUBLISH, GRPC_CALL
    }

    public enum ConcurrencyPolicy {
        ALLOW,   // Allow concurrent runs
        FORBID,  // Skip if previous still running
        REPLACE  // Kill previous and start new
    }

    public enum RunStatus {
        SUCCESS, FAILED, TIMEOUT, SKIPPED, RUNNING
    }
}
