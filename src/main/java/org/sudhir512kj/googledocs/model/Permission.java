package org.sudhir512kj.googledocs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
    
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private PermissionType type;
    
    private LocalDateTime grantedAt;
    private String grantedBy;
    
    public enum PermissionType {
        OWNER, EDITOR, COMMENTER, VIEWER
    }
}
