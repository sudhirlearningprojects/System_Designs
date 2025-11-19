package org.sudhir512kj.ticketbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private String preferredCities; // JSON array
    private String preferredGenres; // JSON array
    private String preferredLanguages; // JSON array
    private String preferredVenueTypes; // JSON array
    
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;
    
    @Column(name = "sms_notifications")
    private Boolean smsNotifications = true;
    
    @Column(name = "push_notifications")
    private Boolean pushNotifications = true;
    
    @Column(name = "marketing_emails")
    private Boolean marketingEmails = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public UserPreference() {}
    
    public UserPreference(User user) {
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getPreferredCities() { return preferredCities; }
    public void setPreferredCities(String preferredCities) { this.preferredCities = preferredCities; }
    
    public String getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(String preferredGenres) { this.preferredGenres = preferredGenres; }
    
    public String getPreferredLanguages() { return preferredLanguages; }
    public void setPreferredLanguages(String preferredLanguages) { this.preferredLanguages = preferredLanguages; }
    
    public String getPreferredVenueTypes() { return preferredVenueTypes; }
    public void setPreferredVenueTypes(String preferredVenueTypes) { this.preferredVenueTypes = preferredVenueTypes; }
    
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    
    public Boolean getSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(Boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    
    public Boolean getPushNotifications() { return pushNotifications; }
    public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }
    
    public Boolean getMarketingEmails() { return marketingEmails; }
    public void setMarketingEmails(Boolean marketingEmails) { this.marketingEmails = marketingEmails; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}