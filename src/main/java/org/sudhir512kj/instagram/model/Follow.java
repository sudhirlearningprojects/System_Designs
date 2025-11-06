package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "follows", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"followerId", "followeeId"}),
    indexes = {
        @Index(name = "idx_follower", columnList = "followerId"),
        @Index(name = "idx_followee", columnList = "followeeId")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long followerId;
    
    @Column(nullable = false)
    private Long followeeId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}