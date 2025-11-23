package org.sudhir512kj.tiktok.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.tiktok.model.Follow;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
