package org.sudhir512kj.instagram.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private String commentId;
    private String postId;
    private Long userId;
    private String username;
    private String profilePictureUrl;
    private String content;
    private String parentCommentId;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isLikedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
