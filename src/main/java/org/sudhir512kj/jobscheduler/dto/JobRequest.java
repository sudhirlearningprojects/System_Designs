package org.sudhir512kj.jobscheduler.dto;

import lombok.Data;
import org.sudhir512kj.jobscheduler.model.Job.ScheduleType;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class JobRequest {
    private String name;
    private String type;
    private ScheduleType scheduleType;
    private String scheduleValue; // cron expression or interval
    private Map<String, Object> payload;
    private Integer priority = 5;
    private Integer maxRetries = 3;
    private Integer timeoutSeconds = 300;
    private LocalDateTime scheduledAt;
    private Map<String, String> tags;
}