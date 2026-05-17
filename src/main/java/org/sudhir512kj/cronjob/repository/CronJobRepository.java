package org.sudhir512kj.cronjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.sudhir512kj.cronjob.model.CronJob;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CronJobRepository extends JpaRepository<CronJob, UUID> {

    Optional<CronJob> findByNamespaceAndName(String namespace, String name);

    List<CronJob> findByNamespace(String namespace);

    List<CronJob> findByDagId(UUID dagId);

    @Query("SELECT j FROM CronJob j WHERE j.status = 'ACTIVE' AND j.nextRunAt <= :now")
    List<CronJob> findJobsDueForExecution(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE CronJob j SET j.status = :status WHERE j.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") CronJob.JobStatus status);
}
