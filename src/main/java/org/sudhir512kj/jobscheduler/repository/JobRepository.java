package org.sudhir512kj.jobscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.jobscheduler.model.Job;
import org.sudhir512kj.jobscheduler.model.Job.JobStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    
    List<Job> findByStatusAndScheduledAtBefore(JobStatus status, LocalDateTime before);
    
    List<Job> findByStatusAndNextExecutionAtBefore(JobStatus status, LocalDateTime before);
    
    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.startedAt < :cutoff")
    List<Job> findOrphanedJobs(JobStatus status, LocalDateTime cutoff);
    
    @Modifying
    @Query("UPDATE Job j SET j.status = :newStatus WHERE j.id = :jobId AND j.status = :currentStatus")
    int updateJobStatus(UUID jobId, JobStatus currentStatus, JobStatus newStatus);
    
    @Query("SELECT j FROM Job j WHERE j.type = :jobType AND j.status IN :statuses")
    List<Job> findByTypeAndStatusIn(String jobType, List<JobStatus> statuses);
    
    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = :status")
    long countByStatus(JobStatus status);
}