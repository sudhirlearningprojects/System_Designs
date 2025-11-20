package org.sudhir512kj.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class RedirectResponse {
    
    private String longUrl;
    private HttpStatus statusCode;
}