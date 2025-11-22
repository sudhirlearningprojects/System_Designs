package org.sudhir512kj.instagram.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_views",
    uniqueConstraints = @UniqueConstraint(columnNames = {"storyId", "viewerId"}),
    indexes = @Index(name = "idx_story_views", columnList = "storyId, viewedAt"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String viewId;
    
    @Column(nullable = false)
    private String storyId;
    
    @Column(nullable = false)
    private Long viewerId;
    
    @CreationTimestamp
    private LocalDateTime viewedAt;
}
