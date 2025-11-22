package org.sudhir512kj.instagram.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.dto.CommentRequest;
import org.sudhir512kj.instagram.dto.CommentResponse;
import org.sudhir512kj.instagram.service.CommentService;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable String postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        CommentResponse response = commentService.addComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Comment added successfully"));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getPostComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Page<CommentResponse> comments = commentService.getPostComments(postId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments, "Comments retrieved successfully"));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentReplies(
            @PathVariable String commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Page<CommentResponse> replies = commentService.getCommentReplies(commentId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(replies, "Replies retrieved successfully"));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> likeComment(
            @PathVariable String commentId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        commentService.likeComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment liked successfully"));
    }

    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @PathVariable String commentId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        commentService.unlikeComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment unliked successfully"));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted successfully"));
    }
}
