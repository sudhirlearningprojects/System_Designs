package org.sudhir512kj.spotify.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Table("listening_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListeningHistory {
    @PrimaryKey
    private UUID id;
    
    private String userId;
    private String trackId;
    private Instant playedAt;
    private Integer durationPlayedMs;
    private String deviceType;
    private String country;
    private Boolean isOffline;
    private String audioQuality;
}
