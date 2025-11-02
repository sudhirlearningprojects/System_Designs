package org.sudhir512kj.jobscheduler.executor;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobResult {
    private boolean success;
    private String message;
    private Map<String, Object> data;
    private Exception exception;
    
    public static JobResult success(String message, Map<String, Object> data) {
        return new JobResult(true, message, data, null);
    }
    
    public static JobResult failure(String message, Exception exception) {
        return new JobResult(false, message, null, exception);
    }
}