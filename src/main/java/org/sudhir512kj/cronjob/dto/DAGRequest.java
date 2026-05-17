package org.sudhir512kj.cronjob.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DAGRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String namespace;

    @NotBlank
    private String cronExpression;

    private String timezone = "UTC";
    private Integer maxParallelTasks = 5;
    private Integer dagTimeoutSeconds = 3600;
    private String description;
}
