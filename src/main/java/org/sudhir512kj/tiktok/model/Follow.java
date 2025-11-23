package org.sudhir512kj.tiktok.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"followerId", "followingId"}),
    indexes = {
        @Index(name = "idx_follower", columnList = "followerId"),
        @Index(name = "idx_following", columnList = "followingId")
    }
)
@Data
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followId;
    
    @Column(nullable = false)
    private Long followerId;
    
    @Column(nullable = false)
    private Long followingId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
