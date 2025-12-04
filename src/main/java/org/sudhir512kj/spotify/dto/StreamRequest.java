package org.sudhir512kj.spotify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamRequest {
    private String trackId;
    private String userId;
    private String deviceId;
    private String audioQuality; // LOW, MEDIUM, HIGH, LOSSLESS
    private Boolean isOffline;
}
