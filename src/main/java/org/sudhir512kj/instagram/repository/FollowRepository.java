package org.sudhir512kj.instagram.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.Follow;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    
    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :userId")
    List<Long> findFollowingIds(@Param("userId") Long userId);
    
    @Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :userId")
    List<Long> findFollowerIds(@Param("userId") Long userId);
    
    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :userId")
    Page<Long> findFollowingIdsPaged(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :userId")
    Page<Long> findFollowerIdsPaged(@Param("userId") Long userId, Pageable pageable);
    
    long countByFollowerId(Long followerId);
    long countByFolloweeId(Long followeeId);
}