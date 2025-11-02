package org.sudhir512kj.digitalpayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.digitalpayment.model.Wallet;
import jakarta.persistence.LockModeType;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = ?1")
    Wallet findByUserId(String userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = ?1")
    Wallet findByUserIdWithLock(String userId);
}