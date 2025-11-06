package org.sudhir512kj.instagram.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.Post;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds ORDER BY p.createdAt DESC")
    Page<Post> findPostsByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findTrendingPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN p.hashtags h WHERE h IN :hashtags ORDER BY p.createdAt DESC")
    Page<Post> findByHashtags(@Param("hashtags") List<String> hashtags, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.location LIKE %:location% ORDER BY p.createdAt DESC")
    Page<Post> findByLocation(@Param("location") String location, Pageable pageable);
}