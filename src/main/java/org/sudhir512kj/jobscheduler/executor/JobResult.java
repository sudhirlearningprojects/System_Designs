package org.sudhir512kj.jobscheduler.executor;

import java.util.Map;

public class JobResult {
    private boolean success;
    private String message;
    private Map<String, Object> data;
    private Exception exception;
    
    public JobResult() {}
    
    public JobResult(boolean success, String message, Map<String, Object> data, Exception exception) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.exception = exception;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    
    public Exception getException() { return exception; }
    public void setException(Exception exception) { this.exception = exception; }
    
    public static JobResult success(String message, Map<String, Object> data) {
        return new JobResult(true, message, data, null);
    }
    
    public static JobResult failure(String message, Exception exception) {
        return new JobResult(false, message, null, exception);
    }
}