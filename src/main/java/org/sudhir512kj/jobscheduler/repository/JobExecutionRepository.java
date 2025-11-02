package org.sudhir512kj.jobscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.jobscheduler.model.JobExecution;
import org.sudhir512kj.jobscheduler.model.JobExecution.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
    
    boolean existsByExecutionId(String executionId);
    
    List<JobExecution> findByJobIdOrderByStartedAtDesc(UUID jobId);
    
    List<JobExecution> findByStatusAndStartedAtBefore(ExecutionStatus status, LocalDateTime before);
    
    long countByJobIdAndStatus(UUID jobId, ExecutionStatus status);
}