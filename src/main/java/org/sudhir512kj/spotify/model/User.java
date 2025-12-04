package org.sudhir512kj.spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    private UserType userType;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan subscriptionPlan;
    
    private LocalDateTime subscriptionExpiryDate;
    private String displayName;
    private String profileImageUrl;
    private String country;
    private LocalDateTime dateOfBirth;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Boolean isActive = true;
    private Boolean isVerified = false;
    private String artistBio;
    private Integer monthlyListeners;
    private Boolean isVerifiedArtist = false;
    
    public enum UserType {
        FREE, PREMIUM, ARTIST
    }
    
    public enum SubscriptionPlan {
        FREE, INDIVIDUAL, DUO, FAMILY, STUDENT
    }
}
