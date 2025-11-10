package org.sudhir512kj.googledocs.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionDTO {
    private String id;
    private Integer versionNumber;
    private String content;
    private String createdBy;
    private LocalDateTime createdAt;
    private String description;
}
