package org.sudhir512kj.googledocs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String userId;
    private Integer startPosition;
    private Integer endPosition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    private CommentStatus status;
    
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reply> replies = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "comment_reactions", joinColumns = @JoinColumn(name = "comment_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "emoji")
    private Map<String, String> reactions = new HashMap<>();
    
    public enum CommentStatus {
        OPEN, RESOLVED
    }
}
