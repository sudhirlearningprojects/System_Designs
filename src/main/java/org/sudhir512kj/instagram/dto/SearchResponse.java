package org.sudhir512kj.instagram.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SearchResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private Boolean isVerified;
    private Integer followerCount;
}
