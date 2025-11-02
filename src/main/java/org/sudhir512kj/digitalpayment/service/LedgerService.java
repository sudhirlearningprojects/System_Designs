package org.sudhir512kj.digitalpayment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.digitalpayment.model.Wallet;
import org.sudhir512kj.digitalpayment.repository.WalletRepository;
import java.math.BigDecimal;

@Service
public class LedgerService {
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean transferFunds(String fromUserId, String toUserId, BigDecimal amount) {
        // Get wallets with pessimistic locking
        Wallet fromWallet = walletRepository.findByUserIdWithLock(fromUserId);
        Wallet toWallet = walletRepository.findByUserIdWithLock(toUserId);
        
        if (fromWallet == null || toWallet == null) {
            throw new IllegalArgumentException("Invalid wallet(s)");
        }
        
        // Check sufficient balance
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            return false;
        }
        
        // Perform atomic transfer
        fromWallet.debit(amount);
        toWallet.credit(amount);
        
        // Save both wallets
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        
        return true;
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean debitWallet(String userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId);
        if (wallet == null || wallet.getBalance().compareTo(amount) < 0) {
            return false;
        }
        
        wallet.debit(amount);
        walletRepository.save(wallet);
        return true;
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void creditWallet(String userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId);
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found for user: " + userId);
        }
        
        wallet.credit(amount);
        walletRepository.save(wallet);
    }
    
    public BigDecimal getWalletBalance(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        return wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
    }
}