package org.sudhir512kj.digitalpayment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private String accountId;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String bankName;
    
    @Column(nullable = false)
    private String accountNumber;
    
    @Column(nullable = false)
    private String ifscCode;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private AccountType type;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Version
    private Long version; // For optimistic locking
    
    // Constructors
    public Account() {}
    
    public Account(String accountId, User user, String bankName, String accountNumber, String ifscCode, AccountType type) {
        this.accountId = accountId;
        this.user = user;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.type = type;
    }
    
    // Getters and setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }
    
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

enum AccountType {
    SAVINGS, CURRENT, CREDIT_CARD
}

enum AccountStatus {
    ACTIVE, INACTIVE, BLOCKED
}