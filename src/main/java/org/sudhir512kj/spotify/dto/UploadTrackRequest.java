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
public class UploadTrackRequest {
    private String title;
    private String artistId;
    private String albumId;
    private String isrc;
    private Integer durationMs;
    private String lyrics;
    private String trackType;
    private Boolean isExplicit;
    private LocalDateTime releaseDate;
    private String genre;
    private String language;
}
