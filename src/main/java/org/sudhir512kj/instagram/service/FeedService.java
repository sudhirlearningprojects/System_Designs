package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.instagram.model.Post;
import org.sudhir512kj.instagram.repository.FollowRepository;
import org.sudhir512kj.instagram.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String FEED_CACHE_KEY = "feed:user:";
    private static final int CELEBRITY_THRESHOLD = 1000000; // 1M followers
    
    @Cacheable(value = "userFeed", key = "#userId + '_' + #pageable.pageNumber")
    public Page<Post> generateFeed(Long userId, Pageable pageable) {
        // Get user's following list
        List<Long> followingIds = followRepository.findFollowingIds(userId);
        
        if (followingIds.isEmpty()) {
            // Return trending posts for new users
            return getTrendingFeed(pageable);
        }
        
        // Check if following any celebrities (users with > 1M followers)
        List<Long> regularUsers = followingIds.stream()
            .filter(this::isRegularUser)
            .collect(Collectors.toList());
        
        List<Long> celebrities = followingIds.stream()
            .filter(id -> !isRegularUser(id))
            .collect(Collectors.toList());
        
        // Use hybrid approach: pull for celebrities, push for regular users
        return generateHybridFeed(userId, regularUsers, celebrities, pageable);
    }
    
    private Page<Post> generateHybridFeed(Long userId, List<Long> regularUsers, 
                                        List<Long> celebrities, Pageable pageable) {
        // Get cached feed for regular users (push model)
        Set<Object> cachedPostIds = redisTemplate.opsForZSet()
            .reverseRange(FEED_CACHE_KEY + userId, 0, pageable.getPageSize() - 1);
        
        if (cachedPostIds != null && !cachedPostIds.isEmpty()) {
            List<String> postIds = cachedPostIds.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
            
            // Fetch posts from database
            return postRepository.findAllById(postIds)
                .stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    posts -> new PageImpl<>(posts, pageable, posts.size())
                ));
        }
        
        // Fallback to pull model for all users
        return postRepository.findPostsByUserIds(regularUsers, pageable);
    }
    
    private Page<Post> getTrendingFeed(Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return postRepository.findTrendingPosts(since, pageable);
    }
    
    private boolean isRegularUser(Long userId) {
        // In a real implementation, this would check user's follower count
        // For now, assume all users are regular users
        return true;
    }
    
    public void addPostToFollowerFeeds(String postId, Long authorId) {
        List<Long> followerIds = followRepository.findFollowerIds(authorId);
        
        // Only add to feeds if author is not a celebrity
        if (followerIds.size() <= CELEBRITY_THRESHOLD) {
            for (Long followerId : followerIds) {
                String feedKey = FEED_CACHE_KEY + followerId;
                
                // Add post to feed with timestamp as score
                redisTemplate.opsForZSet().add(feedKey, postId, System.currentTimeMillis());
                
                // Keep only recent 1000 posts in feed
                redisTemplate.opsForZSet().removeRange(feedKey, 0, -1001);
            }
        }
    }
    
    public Page<Post> getExploreFeed(Long userId, Pageable pageable) {
        // Get posts from users not followed by the current user
        List<Long> followingIds = followRepository.findFollowingIds(userId);
        followingIds.add(userId); // Exclude own posts
        
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return postRepository.findTrendingPosts(since, pageable);
    }
}