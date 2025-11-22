package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.instagram.dto.StoryCreateRequest;
import org.sudhir512kj.instagram.dto.StoryResponse;
import org.sudhir512kj.instagram.dto.StoryViewerResponse;
import org.sudhir512kj.instagram.model.Story;
import org.sudhir512kj.instagram.model.StoryView;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.repository.FollowRepository;
import org.sudhir512kj.instagram.repository.StoryRepository;
import org.sudhir512kj.instagram.repository.StoryViewRepository;
import org.sudhir512kj.instagram.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public StoryResponse createStory(Long userId, StoryCreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Story story = new Story();
        story.setUserId(userId);
        story.setMediaUrl(request.getMediaUrl());
        story.setMediaType(request.getMediaType());
        story.setDuration(request.getDuration());
        story.setBackgroundColor(request.getBackgroundColor());
        story.setExpiresAt(LocalDateTime.now().plusHours(24));
        story = storyRepository.save(story);
        
        return buildStoryResponse(story, user, false);
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getUserStories(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Story> stories = storyRepository.findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(
            userId, LocalDateTime.now());
        
        List<String> storyIds = stories.stream().map(Story::getStoryId).toList();
        Set<String> viewedStoryIds = storyViewRepository.findViewedStoryIdsByViewerIdAndStoryIdIn(
            currentUserId, storyIds);
        
        return stories.stream()
            .map(story -> buildStoryResponse(story, user, viewedStoryIds.contains(story.getStoryId())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<StoryResponse>> getFollowingStories(Long currentUserId) {
        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(currentUserId);
        
        List<Story> stories = storyRepository.findActiveStoriesByUserIds(followingIds, LocalDateTime.now());
        
        List<Long> userIds = stories.stream().map(Story::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getUserId, u -> u));
        
        List<String> storyIds = stories.stream().map(Story::getStoryId).toList();
        Set<String> viewedStoryIds = storyViewRepository.findViewedStoryIdsByViewerIdAndStoryIdIn(
            currentUserId, storyIds);
        
        return stories.stream()
            .collect(Collectors.groupingBy(
                Story::getUserId,
                Collectors.mapping(story -> buildStoryResponse(
                    story, 
                    userMap.get(story.getUserId()), 
                    viewedStoryIds.contains(story.getStoryId())
                ), Collectors.toList())
            ));
    }

    @Transactional
    public void viewStory(String storyId, Long viewerId) {
        if (storyViewRepository.existsByStoryIdAndViewerId(storyId, viewerId)) {
            return;
        }
        
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        if (story.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Story has expired");
        }
        
        StoryView view = new StoryView();
        view.setStoryId(storyId);
        view.setViewerId(viewerId);
        storyViewRepository.save(view);
        
        storyRepository.incrementViewCount(storyId);
    }

    @Transactional(readOnly = true)
    public List<StoryViewerResponse> getStoryViewers(String storyId, Long userId) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        if (!story.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to view story viewers");
        }
        
        List<StoryView> views = storyViewRepository.findByStoryIdOrderByViewedAtDesc(storyId);
        
        List<Long> viewerIds = views.stream().map(StoryView::getViewerId).toList();
        Map<Long, User> userMap = userRepository.findAllById(viewerIds).stream()
            .collect(Collectors.toMap(User::getUserId, u -> u));
        
        return views.stream()
            .map(view -> {
                User viewer = userMap.get(view.getViewerId());
                return StoryViewerResponse.builder()
                    .userId(viewer.getUserId())
                    .username(viewer.getUsername())
                    .profilePictureUrl(viewer.getProfilePictureUrl())
                    .viewedAt(view.getViewedAt())
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteStory(String storyId, Long userId) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        if (!story.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this story");
        }
        
        storyRepository.delete(story);
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void cleanupExpiredStories() {
        log.info("Cleaning up expired stories");
        storyRepository.deleteExpiredStories(LocalDateTime.now());
    }

    private StoryResponse buildStoryResponse(Story story, User user, boolean isViewed) {
        return StoryResponse.builder()
            .storyId(story.getStoryId())
            .userId(story.getUserId())
            .username(user.getUsername())
            .profilePictureUrl(user.getProfilePictureUrl())
            .mediaUrl(story.getMediaUrl())
            .mediaType(story.getMediaType())
            .duration(story.getDuration())
            .backgroundColor(story.getBackgroundColor())
            .viewCount(story.getViewCount())
            .isViewedByCurrentUser(isViewed)
            .createdAt(story.getCreatedAt())
            .expiresAt(story.getExpiresAt())
            .build();
    }
}
