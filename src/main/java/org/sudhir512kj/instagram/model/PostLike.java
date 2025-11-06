package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"postId", "userId"}),
    indexes = {
        @Index(name = "idx_post_likes", columnList = "postId"),
        @Index(name = "idx_user_likes", columnList = "userId")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String postId;
    
    @Column(nullable = false)
    private Long userId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}