package org.sudhir512kj.tiktok.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.tiktok.dto.FeedResponse;
import org.sudhir512kj.tiktok.dto.VideoUploadRequest;
import org.sudhir512kj.tiktok.model.Video;
import org.sudhir512kj.tiktok.service.VideoService;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;
    
    @PostMapping("/upload")
    public ResponseEntity<Video> uploadVideo(
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file,
            @ModelAttribute VideoUploadRequest request) {
        return ResponseEntity.ok(videoService.uploadVideo(userId, file, request));
    }
    
    @GetMapping("/feed/foryou")
    public ResponseEntity<FeedResponse> getForYouFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(videoService.getForYouFeed(userId, page, size));
    }
    
    @GetMapping("/feed/following")
    public ResponseEntity<FeedResponse> getFollowingFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(videoService.getFollowingFeed(userId, page, size));
    }
    
    @PostMapping("/{videoId}/like")
    public ResponseEntity<Void> likeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        videoService.likeVideo(userId, videoId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{videoId}/like")
    public ResponseEntity<Void> unlikeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId) {
        videoService.unlikeVideo(userId, videoId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{videoId}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long videoId) {
        videoService.incrementViewCount(videoId);
        return ResponseEntity.ok().build();
    }
}
