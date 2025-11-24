package org.sudhir512kj.tiktok.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.sudhir512kj.tiktok.model.Follow;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = ?1")
    List<Long> findFollowerIdsByFollowingId(Long followingId);
}
