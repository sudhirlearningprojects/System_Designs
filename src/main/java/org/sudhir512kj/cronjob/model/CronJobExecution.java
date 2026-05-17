package org.sudhir512kj.cronjob.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cron_job_executions", indexes = {
    @Index(name = "idx_exec_job_id", columnList = "job_id, started_at DESC"),
    @Index(name = "idx_exec_status", columnList = "status"),
    @Index(name = "idx_exec_dag_run", columnList = "dag_run_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronJobExecution {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "dag_run_id")
    private UUID dagRunId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CronJob.RunStatus status;

    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "executor_node")
    private String executorNode;

    @Column(name = "fencing_token")
    private Long fencingToken;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "log_output", columnDefinition = "TEXT")
    private String logOutput;

    @Column(name = "triggered_by")
    private String triggeredBy; // SCHEDULER, MANUAL, DAG_DEPENDENCY
}
