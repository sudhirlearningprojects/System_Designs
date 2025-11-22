package org.sudhir512kj.instagram.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.dto.StoryCreateRequest;
import org.sudhir512kj.instagram.dto.StoryResponse;
import org.sudhir512kj.instagram.dto.StoryViewerResponse;
import org.sudhir512kj.instagram.service.StoryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {
    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @Valid @RequestBody StoryCreateRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        StoryResponse response = storyService.createStory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Story created successfully"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getUserStories(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getName());
        List<StoryResponse> stories = storyService.getUserStories(userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(stories, "Stories retrieved successfully"));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<Map<Long, List<StoryResponse>>>> getFollowingStories(
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Map<Long, List<StoryResponse>> stories = storyService.getFollowingStories(userId);
        return ResponseEntity.ok(ApiResponse.success(stories, "Following stories retrieved successfully"));
    }

    @PostMapping("/{storyId}/view")
    public ResponseEntity<ApiResponse<Void>> viewStory(
            @PathVariable String storyId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        storyService.viewStory(storyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Story viewed successfully"));
    }

    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<ApiResponse<List<StoryViewerResponse>>> getStoryViewers(
            @PathVariable String storyId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<StoryViewerResponse> viewers = storyService.getStoryViewers(storyId, userId);
        return ResponseEntity.ok(ApiResponse.success(viewers, "Story viewers retrieved successfully"));
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @PathVariable String storyId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        storyService.deleteStory(storyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Story deleted successfully"));
    }
}
