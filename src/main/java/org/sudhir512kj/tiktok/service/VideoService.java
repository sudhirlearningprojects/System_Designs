package org.sudhir512kj.tiktok.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.tiktok.dto.FeedResponse;
import org.sudhir512kj.tiktok.dto.VideoUploadRequest;
import org.sudhir512kj.tiktok.model.*;
import org.sudhir512kj.tiktok.repository.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoProcessingService videoProcessingService;
    
    @Transactional
    public Video uploadVideo(Long userId, MultipartFile file, VideoUploadRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Process video asynchronously
        Map<String, String> urls = videoProcessingService.processVideo(file);
        
        Video video = new Video();
        video.setUserId(userId);
        video.setVideoUrl(urls.get("videoUrl"));
        video.setThumbnailUrl(urls.get("thumbnailUrl"));
        video.setCaption(request.getCaption());
        video.setDurationSeconds(videoProcessingService.getVideoDuration(file));
        video.setFileSize(file.getSize());
        video.setIsPublic(request.getIsPublic());
        video.setAllowComments(request.getAllowComments());
        video.setAllowDuet(request.getAllowDuet());
        video.setAllowStitch(request.getAllowStitch());
        
        video = videoRepository.save(video);
        
        user.setVideoCount(user.getVideoCount() + 1);
        userRepository.save(user);
        
        return video;
    }
    
    public FeedResponse getForYouFeed(Long userId, int page, int size) {
        String cacheKey = "feed:foryou:" + userId + ":" + page;
        FeedResponse cached = (FeedResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;
        
        // Recommendation algorithm: mix of trending + following + personalized
        List<Video> videos = videoRepository.findPublicVideos(PageRequest.of(page, size));
        
        FeedResponse response = buildFeedResponse(videos, userId);
        redisTemplate.opsForValue().set(cacheKey, response, 5, TimeUnit.MINUTES);
        
        return response;
    }
    
    public FeedResponse getFollowingFeed(Long userId, int page, int size) {
        // Get videos from users that current user follows
        List<Video> videos = videoRepository.findPublicVideos(PageRequest.of(page, size));
        return buildFeedResponse(videos, userId);
    }
    
    @Transactional
    public void likeVideo(Long userId, Long videoId) {
        if (likeRepository.existsByUserIdAndVideoId(userId, videoId)) {
            throw new RuntimeException("Already liked");
        }
        
        Like like = new Like();
        like.setUserId(userId);
        like.setVideoId(videoId);
        likeRepository.save(like);
        
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setLikeCount(video.getLikeCount() + 1);
        videoRepository.save(video);
        
        // Invalidate cache
        redisTemplate.delete("video:" + videoId);
    }
    
    @Transactional
    public void unlikeVideo(Long userId, Long videoId) {
        Like like = likeRepository.findByUserIdAndVideoId(userId, videoId)
            .orElseThrow(() -> new RuntimeException("Like not found"));
        
        likeRepository.delete(like);
        
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setLikeCount(Math.max(0, video.getLikeCount() - 1));
        videoRepository.save(video);
        
        redisTemplate.delete("video:" + videoId);
    }
    
    @Transactional
    public void incrementViewCount(Long videoId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setViewCount(video.getViewCount() + 1);
        videoRepository.save(video);
    }
    
    private FeedResponse buildFeedResponse(List<Video> videos, Long userId) {
        FeedResponse response = new FeedResponse();
        
        Set<Long> userIds = videos.stream().map(Video::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getUserId, u -> u));
        
        Set<Long> videoIds = videos.stream().map(Video::getVideoId).collect(Collectors.toSet());
        Set<Long> likedVideoIds = likeRepository.findAll().stream()
            .filter(l -> l.getUserId().equals(userId) && videoIds.contains(l.getVideoId()))
            .map(Like::getVideoId)
            .collect(Collectors.toSet());
        
        Set<Long> followingIds = followRepository.findAll().stream()
            .filter(f -> f.getFollowerId().equals(userId))
            .map(Follow::getFollowingId)
            .collect(Collectors.toSet());
        
        List<FeedResponse.VideoDTO> videoDTOs = videos.stream().map(v -> {
            FeedResponse.VideoDTO dto = new FeedResponse.VideoDTO();
            User user = userMap.get(v.getUserId());
            
            dto.setVideoId(v.getVideoId());
            dto.setUserId(v.getUserId());
            dto.setUsername(user.getUsername());
            dto.setProfilePictureUrl(user.getProfilePictureUrl());
            dto.setVideoUrl(v.getVideoUrl());
            dto.setThumbnailUrl(v.getThumbnailUrl());
            dto.setCaption(v.getCaption());
            dto.setDurationSeconds(v.getDurationSeconds());
            dto.setViewCount(v.getViewCount());
            dto.setLikeCount(v.getLikeCount());
            dto.setCommentCount(v.getCommentCount());
            dto.setShareCount(v.getShareCount());
            dto.setIsLiked(likedVideoIds.contains(v.getVideoId()));
            dto.setIsFollowing(followingIds.contains(v.getUserId()));
            
            return dto;
        }).collect(Collectors.toList());
        
        response.setVideos(videoDTOs);
        return response;
    }
}
