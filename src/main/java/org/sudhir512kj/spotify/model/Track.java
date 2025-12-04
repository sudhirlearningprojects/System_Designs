package org.sudhir512kj.spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracks", indexes = {
    @Index(name = "idx_artist_id", columnList = "artistId"),
    @Index(name = "idx_album_id", columnList = "albumId"),
    @Index(name = "idx_isrc", columnList = "isrc")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artistId;
    
    private String albumId;
    
    @Column(unique = true)
    private String isrc; // International Standard Recording Code
    
    private Integer durationMs;
    
    @Column(columnDefinition = "TEXT")
    private String lyrics;
    
    @Enumerated(EnumType.STRING)
    private TrackType trackType;
    
    @Column(nullable = false)
    private Boolean isExplicit = false;
    
    private LocalDateTime releaseDate;
    
    @Column(nullable = false)
    private Long playCount = 0L;
    
    private String genre;
    private String language;
    
    // Audio file storage paths (S3 keys)
    private String audioFileLowQuality;    // 96 kbps
    private String audioFileMediumQuality; // 160 kbps
    private String audioFileHighQuality;   // 320 kbps
    private String audioFileLossless;      // FLAC
    
    private String coverImageUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    public enum TrackType {
        SONG, PODCAST, AUDIOBOOK
    }
}
