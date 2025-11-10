package org.sudhir512kj.googledocs.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {
    private String id;
    private String title;
    private String content;
    private String ownerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String watermark;
    private Set<String> tags;
    private Integer version;
    private List<PermissionDTO> permissions;
    private List<ActiveUserDTO> activeUsers;
}
