package org.sudhir512kj.googledocs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    private String userId;
    private Integer startPosition;
    private Integer endPosition;
    
    @Column(columnDefinition = "TEXT")
    private String originalText;
    
    @Column(columnDefinition = "TEXT")
    private String suggestedText;
    
    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    
    public enum SuggestionStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
