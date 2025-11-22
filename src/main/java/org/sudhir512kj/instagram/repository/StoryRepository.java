package org.sudhir512kj.instagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.Story;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, String> {
    List<Story> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now);
    
    @Query("SELECT s FROM Story s WHERE s.userId IN :userIds AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUserIds(List<Long> userIds, LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Story s SET s.viewCount = s.viewCount + 1 WHERE s.storyId = :storyId")
    void incrementViewCount(String storyId);
    
    @Modifying
    @Query("DELETE FROM Story s WHERE s.expiresAt < :now")
    void deleteExpiredStories(LocalDateTime now);
}
