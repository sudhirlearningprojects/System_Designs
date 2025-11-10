package org.sudhir512kj.googledocs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Version {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    private Integer versionNumber;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String createdBy;
    private LocalDateTime createdAt;
    private String description;
}
