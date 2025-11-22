package org.sudhir512kj.instagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.StoryView;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, String> {
    Optional<StoryView> findByStoryIdAndViewerId(String storyId, Long viewerId);
    
    boolean existsByStoryIdAndViewerId(String storyId, Long viewerId);
    
    List<StoryView> findByStoryIdOrderByViewedAtDesc(String storyId);
    
    @Query("SELECT sv.storyId FROM StoryView sv WHERE sv.storyId IN :storyIds AND sv.viewerId = :viewerId")
    Set<String> findViewedStoryIdsByViewerIdAndStoryIdIn(Long viewerId, List<String> storyIds);
}
