package org.sudhir512kj.tiktok.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.tiktok.dto.NotificationEvent;
import org.sudhir512kj.tiktok.repository.FollowRepository;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FollowRepository followRepository;
    
    public void notifyFollowers(Long userId, String message) {
        List<Long> followerIds = followRepository.findFollowerIdsByFollowingId(userId);
        
        followerIds.forEach(followerId -> {
            NotificationEvent event = NotificationEvent.builder()
                .type("LIVE_STARTED")
                .userId(userId)
                .targetUserId(followerId)
                .message(message)
                .build();
            
            kafkaTemplate.send("tiktok-notifications", event);
        });
        
        log.info("Sent {} notifications for user {}", followerIds.size(), userId);
    }
    
    public void notifyNewVideo(Long userId, Long videoId) {
        List<Long> followerIds = followRepository.findFollowerIdsByFollowingId(userId);
        
        followerIds.forEach(followerId -> {
            NotificationEvent event = NotificationEvent.builder()
                .type("NEW_VIDEO")
                .userId(userId)
                .targetUserId(followerId)
                .message("posted a new video")
                .metadata(String.valueOf(videoId))
                .build();
            
            kafkaTemplate.send("tiktok-notifications", event);
        });
        
        log.info("Sent new video notifications to {} followers", followerIds.size());
    }
    
    public void notifyNewFollower(Long userId, Long followerId) {
        NotificationEvent event = NotificationEvent.builder()
            .type("NEW_FOLLOWER")
            .userId(followerId)
            .targetUserId(userId)
            .message("started following you")
            .build();
        
        kafkaTemplate.send("tiktok-notifications", event);
    }
    
    public void notifyNewLike(Long userId, Long videoId, Long likerId) {
        NotificationEvent event = NotificationEvent.builder()
            .type("NEW_LIKE")
            .userId(likerId)
            .targetUserId(userId)
            .message("liked your video")
            .metadata(String.valueOf(videoId))
            .build();
        
        kafkaTemplate.send("tiktok-notifications", event);
    }
    
    public void notifyNewComment(Long userId, Long videoId, Long commenterId, String comment) {
        NotificationEvent event = NotificationEvent.builder()
            .type("NEW_COMMENT")
            .userId(commenterId)
            .targetUserId(userId)
            .message("commented: " + comment)
            .metadata(String.valueOf(videoId))
            .build();
        
        kafkaTemplate.send("tiktok-notifications", event);
    }
}
