package org.sudhir512kj.instagram.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.model.Post;
import org.sudhir512kj.instagram.service.FeedService;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Post>>> getNewsFeed(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Post> feed = feedService.generateFeed(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(feed));
    }
    
    @GetMapping("/explore")
    public ResponseEntity<ApiResponse<Page<Post>>> getExploreFeed(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Post> exploreFeed = feedService.getExploreFeed(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(exploreFeed));
    }
}