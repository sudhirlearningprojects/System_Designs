package org.sudhir512kj.instagram.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class PostCreateRequest {
    @Size(max = 2200, message = "Content must not exceed 2200 characters")
    private String content;
    
    private List<String> mediaUrls;
    
    private Set<String> hashtags;
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
}