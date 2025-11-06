package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.instagram.dto.PostCreateRequest;
import org.sudhir512kj.instagram.model.Post;
import org.sudhir512kj.instagram.model.PostLike;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.repository.PostRepository;
import org.sudhir512kj.instagram.repository.PostLikeRepository;
import org.sudhir512kj.instagram.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public Post createPost(Long userId, PostCreateRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setMediaUrls(request.getMediaUrls());
        post.setHashtags(request.getHashtags());
        post.setLocation(request.getLocation());
        
        Post savedPost = postRepository.save(post);
        
        // Update user post count
        userRepository.findById(userId).ifPresent(user -> {
            user.setPostCount(user.getPostCount() + 1);
            userRepository.save(user);
        });
        
        // Publish post creation event for feed generation
        kafkaTemplate.send("post-created", savedPost);
        
        log.info("Created post {} for user {}", savedPost.getPostId(), userId);
        return savedPost;
    }
    
    @Cacheable(value = "posts", key = "#postId")
    public Optional<Post> getPostById(String postId) {
        return postRepository.findById(postId);
    }
    
    public Page<Post> getUserPosts(Long userId, Pageable pageable) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    public Page<Post> getTrendingPosts(Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return postRepository.findTrendingPosts(since, pageable);
    }
    
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public boolean likePost(String postId, Long userId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return false; // Already liked
        }
        
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        postLikeRepository.save(like);
        
        // Update like count
        postRepository.findById(postId).ifPresent(post -> {
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            
            // Publish like event for notifications
            kafkaTemplate.send("post-liked", Map.of(
                "postId", postId,
                "userId", userId,
                "postOwnerId", post.getUserId()
            ));
        });
        
        return true;
    }
    
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public boolean unlikePost(String postId, Long userId) {
        Optional<PostLike> likeOpt = postLikeRepository.findByPostIdAndUserId(postId, userId);
        
        if (likeOpt.isEmpty()) {
            return false; // Not liked
        }
        
        postLikeRepository.delete(likeOpt.get());
        
        // Update like count
        postRepository.findById(postId).ifPresent(post -> {
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
        });
        
        return true;
    }
    
    public boolean isPostLikedByUser(String postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
    
    @Transactional
    public void deletePost(String postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this post");
        }
        
        postRepository.delete(post);
        
        // Update user post count
        userRepository.findById(userId).ifPresent(user -> {
            user.setPostCount(Math.max(0, user.getPostCount() - 1));
            userRepository.save(user);
        });
        
        log.info("Deleted post {} by user {}", postId, userId);
    }
}