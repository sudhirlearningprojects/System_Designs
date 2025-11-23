package org.sudhir512kj.tiktok.dto;

import lombok.Data;
import org.sudhir512kj.tiktok.model.Video;
import java.util.List;

@Data
public class FeedResponse {
    private List<VideoDTO> videos;
    private String nextCursor;
    
    @Data
    public static class VideoDTO {
        private Long videoId;
        private Long userId;
        private String username;
        private String profilePictureUrl;
        private String videoUrl;
        private String thumbnailUrl;
        private String caption;
        private Integer durationSeconds;
        private Long viewCount;
        private Long likeCount;
        private Long commentCount;
        private Long shareCount;
        private Boolean isLiked;
        private Boolean isFollowing;
    }
}
