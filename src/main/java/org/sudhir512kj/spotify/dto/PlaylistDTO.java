package org.sudhir512kj.spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDTO {
    private String id;
    private String name;
    private String description;
    private String userId;
    private String ownerName;
    private Boolean isPublic;
    private Boolean isCollaborative;
    private String coverImageUrl;
    private Integer trackCount;
    private Long followerCount;
    private LocalDateTime createdAt;
    private List<TrackDTO> tracks;
}
