package org.sudhir512kj.digitalpayment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    private String walletId;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    @Version
    private Long version; // For optimistic locking
    
    // Constructors
    public Wallet() {}
    
    public Wallet(String walletId, User user) {
        this.walletId = walletId;
        this.user = user;
    }
    
    // Thread-safe balance operations
    public synchronized boolean debit(BigDecimal amount) {
        if (balance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
            lastUpdated = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    public synchronized void credit(BigDecimal amount) {
        balance = balance.add(amount);
        lastUpdated = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}