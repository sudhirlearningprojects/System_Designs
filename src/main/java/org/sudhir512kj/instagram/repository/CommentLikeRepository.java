package org.sudhir512kj.instagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.CommentLike;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, String> {
    Optional<CommentLike> findByCommentIdAndUserId(String commentId, Long userId);
    
    boolean existsByCommentIdAndUserId(String commentId, Long userId);
    
    @Query("SELECT cl.commentId FROM CommentLike cl WHERE cl.commentId IN :commentIds AND cl.userId = :userId")
    Set<String> findLikedCommentIdsByUserIdAndCommentIdIn(Long userId, List<String> commentIds);
}
