package org.sudhir512kj.instagram.dto;

import lombok.Data;
import lombok.Builder;
import org.sudhir512kj.instagram.model.Story;
import java.time.LocalDateTime;

@Data
@Builder
public class StoryResponse {
    private String storyId;
    private Long userId;
    private String username;
    private String profilePictureUrl;
    private String mediaUrl;
    private Story.MediaType mediaType;
    private Integer duration;
    private String backgroundColor;
    private Integer viewCount;
    private Boolean isViewedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
