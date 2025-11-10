package org.sudhir512kj.googledocs.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveSession {
    private String sessionId;
    private String documentId;
    private String userId;
    private String userName;
    private Integer cursorPosition;
    private LocalDateTime lastActivity;
}
