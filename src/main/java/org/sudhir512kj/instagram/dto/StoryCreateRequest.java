package org.sudhir512kj.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.sudhir512kj.instagram.model.Story;

@Data
public class StoryCreateRequest {
    @NotBlank(message = "Media URL is required")
    private String mediaUrl;
    
    @NotNull(message = "Media type is required")
    private Story.MediaType mediaType;
    
    private Integer duration = 15;
    private String backgroundColor;
}
