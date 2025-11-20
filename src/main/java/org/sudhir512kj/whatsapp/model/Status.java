package org.sudhir512kj.whatsapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "statuses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    private StatusType type;
    
    private String mediaUrl;
    private String backgroundColor;
    private String textColor;
    
    @ManyToMany
    @JoinTable(
        name = "status_viewers",
        joinColumns = @JoinColumn(name = "status_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> viewers;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime expiresAt; // 24 hours from creation
    
    public enum StatusType {
        TEXT, IMAGE, VIDEO
    }
}