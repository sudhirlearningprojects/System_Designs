package org.sudhir512kj.instagram.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class StoryViewerResponse {
    private Long userId;
    private String username;
    private String profilePictureUrl;
    private LocalDateTime viewedAt;
}
