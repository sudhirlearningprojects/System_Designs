package org.sudhir512kj.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 2200, message = "Comment cannot exceed 2200 characters")
    private String content;
    
    private String parentCommentId;
}
