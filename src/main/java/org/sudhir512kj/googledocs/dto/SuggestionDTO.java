package org.sudhir512kj.googledocs.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionDTO {
    private String id;
    private String userId;
    private Integer startPosition;
    private Integer endPosition;
    private String originalText;
    private String suggestedText;
    private String status;
    private LocalDateTime createdAt;
}
