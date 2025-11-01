package org.sudhir512kj.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.payment.model.IdempotencyCache;
import java.time.LocalDateTime;

@Repository
public interface IdempotencyCacheRepository extends JpaRepository<IdempotencyCache, String> {
    
    @Modifying
    @Query("DELETE FROM IdempotencyCache i WHERE i.expiresAt < :now")
    void deleteExpiredEntries(LocalDateTime now);
}