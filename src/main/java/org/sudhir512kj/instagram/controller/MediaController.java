package org.sudhir512kj.instagram.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.service.MediaService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        Long userId = Long.parseLong(authentication.getName());
        String url = mediaService.uploadImage(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(url, "Image uploaded successfully"));
    }

    @PostMapping("/upload/video")
    public ResponseEntity<ApiResponse<String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        Long userId = Long.parseLong(authentication.getName());
        String url = mediaService.uploadVideo(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(url, "Video uploaded successfully"));
    }
}
