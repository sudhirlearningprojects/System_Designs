package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.instagram.dto.UserRegistrationRequest;
import org.sudhir512kj.instagram.dto.UserLoginRequest;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.repository.UserRepository;
import org.sudhir512kj.instagram.repository.FollowRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final org.sudhir512kj.instagram.elasticsearch.ElasticsearchService elasticsearchService;
    
    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        
        User savedUser = userRepository.save(user);
        
        // Index in Elasticsearch
        try {
            elasticsearchService.indexUser(savedUser);
        } catch (Exception e) {
            log.warn("Failed to index user in Elasticsearch", e);
        }
        
        return savedUser;
    }
    
    public Optional<User> authenticateUser(UserLoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        
        if (userOpt.isPresent() && 
            passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return userOpt;
        }
        
        return Optional.empty();
    }
    
    @Cacheable(value = "users", key = "#userId")
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    @Cacheable(value = "users", key = "#username")
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Transactional
    public User updateUser(Long userId, User updatedUser) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (updatedUser.getFullName() != null) {
            user.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getBio() != null) {
            user.setBio(updatedUser.getBio());
        }
        if (updatedUser.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(updatedUser.getProfilePictureUrl());
        }
        if (updatedUser.getIsPrivate() != null) {
            user.setIsPrivate(updatedUser.getIsPrivate());
        }
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new RuntimeException("Cannot follow yourself");
        }
        
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new RuntimeException("Already following this user");
        }
        
        // Update follower counts
        userRepository.findById(followerId).ifPresent(follower -> {
            follower.setFollowingCount(follower.getFollowingCount() + 1);
            userRepository.save(follower);
        });
        
        userRepository.findById(followeeId).ifPresent(followee -> {
            followee.setFollowerCount(followee.getFollowerCount() + 1);
            userRepository.save(followee);
        });
        
        // Create follow relationship
        org.sudhir512kj.instagram.model.Follow follow = 
            new org.sudhir512kj.instagram.model.Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        followRepository.save(follow);
        
        // Create notification
        notificationService.createNotification(followeeId, followerId,
            org.sudhir512kj.instagram.model.Notification.NotificationType.FOLLOW, null);
    }
    
    @Transactional
    public void unfollowUser(Long followerId, Long followeeId) {
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
            .ifPresentOrElse(
                follow -> {
                    followRepository.delete(follow);
                    
                    // Update follower counts
                    userRepository.findById(followerId).ifPresent(follower -> {
                        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
                        userRepository.save(follower);
                    });
                    
                    userRepository.findById(followeeId).ifPresent(followee -> {
                        followee.setFollowerCount(Math.max(0, followee.getFollowerCount() - 1));
                        userRepository.save(followee);
                    });
                },
                () -> {
                    throw new RuntimeException("Not following this user");
                }
            );
    }
    
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }
    
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}