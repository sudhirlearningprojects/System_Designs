package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email")
})
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    private String fullName;
    private String bio;
    private String profilePictureUrl;
    
    @Column(nullable = false)
    private Boolean isVerified = false;
    
    @Column(nullable = false)
    private Long followerCount = 0L;
    
    @Column(nullable = false)
    private Long followingCount = 0L;
    
    @Column(nullable = false)
    private Long videoCount = 0L;
    
    @Column(nullable = false)
    private Long totalLikes = 0L;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
