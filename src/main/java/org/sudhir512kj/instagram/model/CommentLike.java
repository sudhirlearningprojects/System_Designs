package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment_likes", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"commentId", "userId"}),
    indexes = @Index(name = "idx_comment_likes", columnList = "commentId, userId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String likeId;
    
    @Column(nullable = false)
    private String commentId;
    
    @Column(nullable = false)
    private Long userId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
