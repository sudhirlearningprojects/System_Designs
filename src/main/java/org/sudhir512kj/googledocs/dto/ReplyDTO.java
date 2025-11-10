package org.sudhir512kj.googledocs.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyDTO {
    private String id;
    private String content;
    private String userId;
    private LocalDateTime createdAt;
}
