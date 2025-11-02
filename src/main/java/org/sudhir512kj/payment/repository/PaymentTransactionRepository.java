package org.sudhir512kj.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.payment.model.PaymentTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    
    List<PaymentTransaction> findByMerchantIdAndCreatedAtBetween(UUID merchantId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = 'PENDING' AND p.createdAt < ?1")
    List<PaymentTransaction> findExpiredPendingTransactions(LocalDateTime cutoffTime);
}