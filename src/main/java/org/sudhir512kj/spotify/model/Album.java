package org.sudhir512kj.spotify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "albums", indexes = {
    @Index(name = "idx_artist_id", columnList = "artistId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String artistId;
    
    @Enumerated(EnumType.STRING)
    private AlbumType albumType;
    
    private LocalDateTime releaseDate;
    private String coverImageUrl;
    private String genre;
    private Integer totalTracks;
    private String label;
    private String copyright;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum AlbumType {
        SINGLE, EP, ALBUM, COMPILATION
    }
}
