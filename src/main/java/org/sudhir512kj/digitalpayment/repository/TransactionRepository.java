package org.sudhir512kj.digitalpayment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.digitalpayment.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    @Query("SELECT t FROM Transaction t WHERE t.senderId = ?1 OR t.receiverId = ?1 ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    List<Transaction> findBySenderIdAndCreatedAtBetween(String senderId, LocalDateTime start, LocalDateTime end);
    
    List<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderId = ?1 AND t.createdAt >= ?2")
    long countTransactionsByUserSince(String userId, LocalDateTime since);
}