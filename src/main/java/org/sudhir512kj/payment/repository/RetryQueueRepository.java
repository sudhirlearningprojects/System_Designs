package org.sudhir512kj.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.payment.model.RetryQueue;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RetryQueueRepository extends JpaRepository<RetryQueue, UUID> {
    
    @Query("SELECT r FROM RetryQueue r WHERE r.nextRetryAt <= ?1 AND r.retryCount < r.maxRetries")
    List<RetryQueue> findByNextRetryAtBeforeAndRetryCountLessThanMaxRetries(LocalDateTime now);
    
    List<RetryQueue> findByTransactionId(UUID transactionId);
}