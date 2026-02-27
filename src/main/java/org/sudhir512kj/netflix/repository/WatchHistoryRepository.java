package org.sudhir512kj.netflix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.WatchHistory;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, String> {
    
    List<WatchHistory> findByUserIdOrderByWatchedAtDesc(String userId);
    
    List<WatchHistory> findByContentId(String contentId);
    
    Optional<WatchHistory> findByUserIdAndContentId(String userId, String contentId);
    
    List<WatchHistory> findByUserIdAndIsCompletedTrue(String userId);
    
    List<WatchHistory> findByUserIdAndCompletionPercentageGreaterThan(String userId, Double percentage);
}