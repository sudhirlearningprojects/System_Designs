package org.sudhir512kj.googledocs.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDTO {
    private String userId;
    private String type;
}
