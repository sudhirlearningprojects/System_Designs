package org.sudhir512kj.cronjob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.sudhir512kj.cronjob.model.CronJob;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CronJobRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String namespace;

    @NotBlank
    private String cronExpression;

    private String timezone = "UTC";

    @NotNull
    private CronJob.TaskType taskType;

    private Map<String, Object> taskConfig; // url, method, headers, body, command, topic

    private UUID dagId;
    private List<UUID> dependsOn;

    private Integer timeoutSeconds = 300;
    private Integer maxRetries = 3;
    private Integer retryDelaySeconds = 60;
    private CronJob.ConcurrencyPolicy concurrencyPolicy = CronJob.ConcurrencyPolicy.FORBID;

    private String description;
    private Map<String, String> labels;
}
