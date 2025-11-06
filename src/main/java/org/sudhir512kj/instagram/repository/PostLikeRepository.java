package org.sudhir512kj.instagram.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.PostLike;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndUserId(String postId, Long userId);
    Optional<PostLike> findByPostIdAndUserId(String postId, Long userId);
    Page<PostLike> findByPostIdOrderByCreatedAtDesc(String postId, Pageable pageable);
    long countByPostId(String postId);
}