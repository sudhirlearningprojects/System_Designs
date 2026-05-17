package org.sudhir512kj.cronjob.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cron_dags")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronDAG {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DAGStatus status = DAGStatus.ACTIVE;

    @Column(name = "max_parallel_tasks")
    private Integer maxParallelTasks = 5;

    @Column(name = "dag_timeout_seconds")
    private Integer dagTimeoutSeconds = 3600;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String description;

    public enum DAGStatus {
        ACTIVE, PAUSED, DISABLED
    }
}
