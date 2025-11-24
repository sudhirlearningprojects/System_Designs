package org.sudhir512kj.tiktok.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String type; // LIVE_STARTED, NEW_VIDEO, NEW_FOLLOWER, NEW_LIKE, NEW_COMMENT
    private Long userId;
    private Long targetUserId;
    private String message;
    private String metadata;
}
