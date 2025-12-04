package org.sudhir512kj.spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "playlist_tracks", indexes = {
    @Index(name = "idx_playlist_id", columnList = "playlistId"),
    @Index(name = "idx_track_id", columnList = "trackId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String playlistId;
    
    @Column(nullable = false)
    private String trackId;
    
    @Column(nullable = false)
    private Integer position;
    
    @Column(nullable = false)
    private String addedBy;
    
    @Column(nullable = false)
    private LocalDateTime addedAt;
}
