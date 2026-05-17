package org.sudhir512kj.cronjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.sudhir512kj.cronjob.model.CronJob;
import org.sudhir512kj.cronjob.model.CronJobExecution;
import java.util.List;
import java.util.UUID;

public interface CronJobExecutionRepository extends JpaRepository<CronJobExecution, UUID> {

    List<CronJobExecution> findByJobIdOrderByStartedAtDesc(UUID jobId);

    @Query("SELECT e FROM CronJobExecution e WHERE e.jobId = :jobId AND e.status = 'RUNNING'")
    List<CronJobExecution> findRunningExecutions(@Param("jobId") UUID jobId);

    List<CronJobExecution> findByDagRunIdOrderByStartedAtAsc(UUID dagRunId);
}
