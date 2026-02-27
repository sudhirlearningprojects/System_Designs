package org.sudhir512kj.netflix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;
    
    @Column(name = "region")
    private String region;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @ElementCollection
    @CollectionTable(name = "user_preferences")
    private List<String> preferredGenres;
    
    @ElementCollection
    @CollectionTable(name = "user_watch_history")
    private List<String> watchHistory;
    
    public enum SubscriptionPlan {
        BASIC, STANDARD, PREMIUM
    }
    
    // Constructors
    public User() {}
    
    public User(String email, String password, String name, String region) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.region = region;
        this.plan = SubscriptionPlan.BASIC;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public List<String> getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(List<String> preferredGenres) { this.preferredGenres = preferredGenres; }
    
    public List<String> getWatchHistory() { return watchHistory; }
    public void setWatchHistory(List<String> watchHistory) { this.watchHistory = watchHistory; }
}