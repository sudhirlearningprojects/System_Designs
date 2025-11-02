package org.sudhir512kj.jobscheduler.dto;

import lombok.Data;
import org.sudhir512kj.jobscheduler.model.Job.JobStatus;
import org.sudhir512kj.jobscheduler.model.Job.ScheduleType;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class JobResponse {
    private UUID id;
    private String name;
    private String type;
    private ScheduleType scheduleType;
    private String scheduleValue;
    private JobStatus status;
    private Integer priority;
    private Integer maxRetries;
    private Integer currentRetries;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime nextExecutionAt;
    private LocalDateTime lastExecutionAt;
    private String createdBy;
}