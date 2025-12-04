package org.sudhir512kj.spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackDTO {
    private String id;
    private String title;
    private String artistId;
    private String artistName;
    private String albumId;
    private String albumName;
    private Integer durationMs;
    private String genre;
    private Boolean isExplicit;
    private Long playCount;
    private String coverImageUrl;
    private LocalDateTime releaseDate;
}
