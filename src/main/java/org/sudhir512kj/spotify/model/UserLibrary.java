package org.sudhir512kj.spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_library", indexes = {
    @Index(name = "idx_user_entity", columnList = "userId,entityType,entityId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLibrary {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;
    
    @Column(nullable = false)
    private String entityId;
    
    @Column(nullable = false)
    private LocalDateTime addedAt;
    
    public enum EntityType {
        TRACK, ALBUM, PLAYLIST, ARTIST, PODCAST
    }
}
