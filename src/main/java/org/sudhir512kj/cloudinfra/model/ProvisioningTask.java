package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "provisioning_tasks")
public class ProvisioningTask {
    @Id
    private String taskId;
    private String resourceId;
    
    @Enumerated(EnumType.STRING)
    private TaskType taskType;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    public enum TaskType {
        PROVISION, DEPROVISION, SCALE, UPDATE
    }
    
    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, DEAD_LETTER
    }
}
