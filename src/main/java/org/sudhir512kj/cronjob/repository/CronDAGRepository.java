package org.sudhir512kj.cronjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cronjob.model.CronDAG;
import java.util.UUID;

public interface CronDAGRepository extends JpaRepository<CronDAG, UUID> {
}
