package org.sudhir512kj.googledocs.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private String id;
    private String content;
    private String userId;
    private Integer startPosition;
    private Integer endPosition;
    private LocalDateTime createdAt;
    private String status;
    private List<ReplyDTO> replies;
    private Map<String, String> reactions;
}
