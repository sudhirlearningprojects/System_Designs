package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.instagram.dto.CommentRequest;
import org.sudhir512kj.instagram.dto.CommentResponse;
import org.sudhir512kj.instagram.model.Comment;
import org.sudhir512kj.instagram.model.CommentLike;
import org.sudhir512kj.instagram.model.Post;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.repository.CommentLikeRepository;
import org.sudhir512kj.instagram.repository.CommentRepository;
import org.sudhir512kj.instagram.repository.PostRepository;
import org.sudhir512kj.instagram.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponse addComment(String postId, Long userId, CommentRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            commentRepository.incrementReplyCount(parentComment.getCommentId());
        }
        
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentCommentId(request.getParentCommentId());
        comment = commentRepository.save(comment);
        
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        
        // Create notification
        notificationService.createNotification(post.getUserId(), userId,
            org.sudhir512kj.instagram.model.Notification.NotificationType.COMMENT, comment.getCommentId());
        
        return buildCommentResponse(comment, user, false);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getPostComments(String postId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId, pageable);
        
        return mapCommentsToResponses(comments, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentReplies(String commentId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId, pageable);
        
        return mapCommentsToResponses(replies, currentUserId);
    }

    @Transactional
    public void likeComment(String commentId, Long userId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("Comment already liked");
        }
        
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        commentLikeRepository.save(like);
        
        commentRepository.incrementLikeCount(commentId);
    }

    @Transactional
    public void unlikeComment(String commentId, Long userId) {
        CommentLike like = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
            .orElseThrow(() -> new RuntimeException("Like not found"));
        
        commentLikeRepository.delete(like);
        commentRepository.decrementLikeCount(commentId);
    }

    @Transactional
    public void deleteComment(String commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }
        
        Post post = postRepository.findById(comment.getPostId())
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (comment.getParentCommentId() != null) {
            commentRepository.findById(comment.getParentCommentId())
                .ifPresent(parent -> {
                    parent.setReplyCount(Math.max(0, parent.getReplyCount() - 1));
                    commentRepository.save(parent);
                });
        }
        
        commentRepository.delete(comment);
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    private Page<CommentResponse> mapCommentsToResponses(Page<Comment> comments, Long currentUserId) {
        List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getUserId, u -> u));
        
        List<String> commentIds = comments.stream().map(Comment::getCommentId).toList();
        Set<String> likedCommentIds = commentLikeRepository.findLikedCommentIdsByUserIdAndCommentIdIn(currentUserId, commentIds);
        
        return comments.map(comment -> {
            User user = userMap.get(comment.getUserId());
            boolean isLiked = likedCommentIds.contains(comment.getCommentId());
            return buildCommentResponse(comment, user, isLiked);
        });
    }

    private CommentResponse buildCommentResponse(Comment comment, User user, boolean isLiked) {
        return CommentResponse.builder()
            .commentId(comment.getCommentId())
            .postId(comment.getPostId())
            .userId(comment.getUserId())
            .username(user.getUsername())
            .profilePictureUrl(user.getProfilePictureUrl())
            .content(comment.getContent())
            .parentCommentId(comment.getParentCommentId())
            .likeCount(comment.getLikeCount())
            .replyCount(comment.getReplyCount())
            .isLikedByCurrentUser(isLiked)
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
