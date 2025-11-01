package org.sudhir512kj.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.payment.model.PaymentTransaction;
import org.sudhir512kj.payment.model.PaymentTransaction.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    
    List<PaymentTransaction> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status);
    
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = :status AND p.createdAt < :before")
    List<PaymentTransaction> findStaleTransactions(PaymentStatus status, LocalDateTime before);
    
    @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.merchantId = :merchantId AND p.createdAt >= :since")
    Long countTransactionsSince(UUID merchantId, LocalDateTime since);
}