package org.sudhir512kj.instagram.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    Page<Comment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(String postId, Pageable pageable);
    
    Page<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.commentId = :commentId")
    void incrementLikeCount(String commentId);
    
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.commentId = :commentId AND c.likeCount > 0")
    void decrementLikeCount(String commentId);
    
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.commentId = :commentId")
    void incrementReplyCount(String commentId);
}
