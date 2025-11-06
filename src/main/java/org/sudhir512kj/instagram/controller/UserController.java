package org.sudhir512kj.instagram.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.instagram.dto.ApiResponse;
import org.sudhir512kj.instagram.dto.UserRegistrationRequest;
import org.sudhir512kj.instagram.dto.UserLoginRequest;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.service.UserService;
import org.sudhir512kj.instagram.service.JwtService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(request);
            String token = jwtService.generateToken(user.getUserId(), user.getUsername());
            
            Map<String, Object> response = Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "accessToken", token,
                "expiresIn", 3600
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody UserLoginRequest request) {
        Optional<User> userOpt = userService.authenticateUser(request);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid email or password"));
        }
        
        User user = userOpt.get();
        String token = jwtService.generateToken(user.getUserId(), user.getUsername());
        
        Map<String, Object> response = Map.of(
            "userId", user.getUserId(),
            "username", user.getUsername(),
            "accessToken", token,
            "expiresIn", 3600,
            "profile", Map.of(
                "fullName", user.getFullName(),
                "profilePictureUrl", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "",
                "isVerified", user.getIsVerified(),
                "followerCount", user.getFollowerCount(),
                "followingCount", user.getFollowingCount(),
                "postCount", user.getPostCount()
            )
        );
        
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long currentUserId) {
        Optional<User> userOpt = userService.getUserById(userId);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        boolean isFollowing = currentUserId != null && 
            userService.isFollowing(currentUserId, userId);
        boolean isFollowedBy = currentUserId != null && 
            userService.isFollowing(userId, currentUserId);
        
        Map<String, Object> profile = Map.of(
            "userId", user.getUserId(),
            "username", user.getUsername(),
            "fullName", user.getFullName(),
            "bio", user.getBio() != null ? user.getBio() : "",
            "profilePictureUrl", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "",
            "isVerified", user.getIsVerified(),
            "isPrivate", user.getIsPrivate(),
            "followerCount", user.getFollowerCount(),
            "followingCount", user.getFollowingCount(),
            "postCount", user.getPostCount(),
            "isFollowing", isFollowing,
            "isFollowedBy", isFollowedBy,
            "createdAt", user.getCreatedAt()
        );
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody User updateRequest) {
        try {
            User updatedUser = userService.updateUser(userId, updateRequest);
            return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Update failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> followUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long currentUserId) {
        try {
            userService.followUser(currentUserId, userId);
            
            Optional<User> userOpt = userService.getUserById(userId);
            int followerCount = userOpt.map(User::getFollowerCount).orElse(0);
            
            Map<String, Object> response = Map.of(
                "isFollowing", true,
                "followerCount", followerCount
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Successfully followed user"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Follow failed: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unfollowUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long currentUserId) {
        try {
            userService.unfollowUser(currentUserId, userId);
            
            Optional<User> userOpt = userService.getUserById(userId);
            int followerCount = userOpt.map(User::getFollowerCount).orElse(0);
            
            Map<String, Object> response = Map.of(
                "isFollowing", false,
                "followerCount", followerCount
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "Successfully unfollowed user"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Unfollow failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<User>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userService.searchUsers(q, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}