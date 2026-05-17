package org.sudhir512kj.cronjob.dto;

import lombok.Data;
import org.sudhir512kj.cronjob.model.CronJob;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CronJobResponse {
    private UUID id;
    private String name;
    private String namespace;
    private String cronExpression;
    private String timezone;
    private CronJob.JobStatus status;
    private CronJob.TaskType taskType;
    private CronJob.ConcurrencyPolicy concurrencyPolicy;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private CronJob.RunStatus lastRunStatus;
    private Long successCount;
    private Long failureCount;
    private LocalDateTime createdAt;
    private String description;

    public static CronJobResponse from(CronJob job) {
        CronJobResponse r = new CronJobResponse();
        r.setId(job.getId());
        r.setName(job.getName());
        r.setNamespace(job.getNamespace());
        r.setCronExpression(job.getCronExpression());
        r.setTimezone(job.getTimezone());
        r.setStatus(job.getStatus());
        r.setTaskType(job.getTaskType());
        r.setConcurrencyPolicy(job.getConcurrencyPolicy());
        r.setNextRunAt(job.getNextRunAt());
        r.setLastRunAt(job.getLastRunAt());
        r.setLastRunStatus(job.getLastRunStatus());
        r.setSuccessCount(job.getSuccessCount());
        r.setFailureCount(job.getFailureCount());
        r.setCreatedAt(job.getCreatedAt());
        r.setDescription(job.getDescription());
        return r;
    }
}
