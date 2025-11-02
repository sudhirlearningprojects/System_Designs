package org.sudhir512kj.jobscheduler.scheduler;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJob {
    private String jobId;
    private UUID jobUuid;
    private String jobType;
    private String payload;
    private int priority;
    private LocalDateTime scheduledTime;
    private long targetTick;
    private boolean readyForExecution;
    private int retryCount;
    private String executorNode;
    
    public ScheduledJob(UUID jobUuid, String jobType, String payload, int priority, LocalDateTime scheduledTime) {
        this.jobId = jobUuid.toString();
        this.jobUuid = jobUuid;
        this.jobType = jobType;
        this.payload = payload;
        this.priority = priority;
        this.scheduledTime = scheduledTime;
        this.readyForExecution = false;
        this.retryCount = 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledJob that = (ScheduledJob) o;
        return jobId.equals(that.jobId);
    }
    
    @Override
    public int hashCode() {
        return jobId.hashCode();
    }
}