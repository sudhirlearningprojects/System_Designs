package org.sudhir512kj.instagram.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.dto.PostCreateRequest;
import org.sudhir512kj.instagram.model.Post;
import org.sudhir512kj.instagram.service.PostService;
import org.sudhir512kj.instagram.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Post>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        try {
            Post post = postService.createPost(userId, request);
            return ResponseEntity.ok(ApiResponse.success(post, "Post created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Post creation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPost(
            @PathVariable String postId,
            @AuthenticationPrincipal Long currentUserId) {
        Optional<Post> postOpt = postService.getPostById(postId);
        
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Post post = postOpt.get();
        Optional<org.sudhir512kj.instagram.model.User> userOpt = userService.getUserById(post.getUserId());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        org.sudhir512kj.instagram.model.User user = userOpt.get();
        boolean isLiked = currentUserId != null && 
            postService.isPostLikedByUser(postId, currentUserId);
        
        Map<String, Object> postDetails = Map.of(
            "postId", post.getPostId(),
            "userId", post.getUserId(),
            "username", user.getUsername(),
            "userProfilePicture", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "",
            "isVerified", user.getIsVerified(),
            "content", post.getContent() != null ? post.getContent() : "",
            "mediaUrls", post.getMediaUrls() != null ? post.getMediaUrls() : List.of(),
            "location", post.getLocation() != null ? post.getLocation() : "",
            "hashtags", post.getHashtags() != null ? post.getHashtags() : Set.of(),
            "likeCount", post.getLikeCount(),
            "commentCount", post.getCommentCount(),
            "shareCount", post.getShareCount(),
            "isLiked", isLiked,
            "createdAt", post.getCreatedAt()
        );
        
        return ResponseEntity.ok(ApiResponse.success(postDetails));
    }
    
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Long userId) {
        try {
            boolean liked = postService.likePost(postId, userId);
            
            if (!liked) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Post already liked"));
            }
            
            Optional<Post> postOpt = postService.getPostById(postId);
            int likeCount = postOpt.map(Post::getLikeCount).orElse(0);
            
            Map<String, Object> response = Map.of(
                "isLiked", true,
                "likeCount", likeCount
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Post liked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Like failed: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unlikePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Long userId) {
        try {
            boolean unliked = postService.unlikePost(postId, userId);
            
            if (!unliked) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Post not liked"));
            }
            
            Optional<Post> postOpt = postService.getPostById(postId);
            int likeCount = postOpt.map(Post::getLikeCount).orElse(0);
            
            Map<String, Object> response = Map.of(
                "isLiked", false,
                "likeCount", likeCount
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Post unliked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Unlike failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<Post>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Post> posts = postService.getUserPosts(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
    
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<Page<Post>>> getTrendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Post> posts = postService.getTrendingPosts(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
    
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Long userId) {
        try {
            postService.deletePost(postId, userId);
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Delete failed: " + e.getMessage()));
        }
    }
}