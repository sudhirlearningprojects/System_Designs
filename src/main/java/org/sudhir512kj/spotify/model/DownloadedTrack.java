package org.sudhir512kj.spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "downloaded_tracks", indexes = {
    @Index(name = "idx_user_track", columnList = "userId,trackId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadedTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String trackId;
    
    @Column(nullable = false)
    private String deviceId;
    
    @Enumerated(EnumType.STRING)
    private AudioQuality audioQuality;
    
    @Column(nullable = false)
    private LocalDateTime downloadedAt;
    
    private LocalDateTime lastAccessedAt;
    
    @Column(nullable = false)
    private Long fileSizeBytes;
    
    public enum AudioQuality {
        LOW, MEDIUM, HIGH, LOSSLESS
    }
}
