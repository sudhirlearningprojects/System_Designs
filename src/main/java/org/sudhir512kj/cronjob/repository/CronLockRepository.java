package org.sudhir512kj.cronjob.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.sudhir512kj.cronjob.model.CronLock;
import java.time.LocalDateTime;

public interface CronLockRepository extends JpaRepository<CronLock, String> {

    @Modifying
    @Query("DELETE FROM CronLock l WHERE l.expiresAt < :now")
    void deleteExpiredLocks(@Param("now") LocalDateTime now);
}
