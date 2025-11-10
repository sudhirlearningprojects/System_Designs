package org.sudhir512kj.googledocs.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveUserDTO {
    private String userId;
    private String userName;
    private Integer cursorPosition;
}
