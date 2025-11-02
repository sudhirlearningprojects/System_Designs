package org.sudhir512kj.jobscheduler.executor;

import org.sudhir512kj.jobscheduler.model.Job;

public interface JobHandler {
    JobResult execute(Job job) throws Exception;
    String getJobType();
    boolean canHandle(String jobType);
}