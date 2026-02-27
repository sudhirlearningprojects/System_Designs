package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.Content;
import org.sudhir512kj.netflix.model.User;
import org.sudhir512kj.netflix.model.WatchHistory;
import org.sudhir512kj.netflix.repository.ContentRepository;
import org.sudhir512kj.netflix.repository.WatchHistoryRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    
    @Cacheable(value = "recommendations", key = "#userId")
    public List<Content> getPersonalizedRecommendations(String userId, User user) {
        List<Content> recommendations = new ArrayList<>();
        
        // 1. Content-based filtering (40% weight)
        recommendations.addAll(getContentBasedRecommendations(userId, user));
        
        // 2. Collaborative filtering (30% weight)
        recommendations.addAll(getCollaborativeRecommendations(userId));
        
        // 3. Trending content (20% weight)
        recommendations.addAll(getTrendingContent());
        
        // 4. New releases (10% weight)
        recommendations.addAll(getNewReleases());
        
        return recommendations.stream()
            .distinct()
            .limit(50)
            .collect(Collectors.toList());
    }
    
    private List<Content> getContentBasedRecommendations(String userId, User user) {
        List<WatchHistory> userHistory = watchHistoryRepository.findByUserIdOrderByWatchedAtDesc(userId);
        
        if (userHistory.isEmpty()) {
            // New user - recommend based on preferred genres
            return contentRepository.findByGenresInOrderByImdbScoreDesc(
                user.getPreferredGenres() != null ? user.getPreferredGenres() : Arrays.asList("Action", "Drama")
            ).stream().limit(10).collect(Collectors.toList());
        }
        
        // Get genres from watch history
        Set<String> watchedGenres = new HashSet<>();
        for (WatchHistory history : userHistory) {
            Content content = contentRepository.findById(history.getContentId()).orElse(null);
            if (content != null && content.getGenres() != null) {
                watchedGenres.addAll(content.getGenres());
            }
        }
        
        return contentRepository.findByGenresInOrderByImdbScoreDesc(new ArrayList<>(watchedGenres))
            .stream().limit(15).collect(Collectors.toList());
    }
    
    private List<Content> getCollaborativeRecommendations(String userId) {
        // Find users with similar viewing patterns
        List<String> similarUsers = findSimilarUsers(userId);
        
        Set<String> recommendedContentIds = new HashSet<>();
        for (String similarUserId : similarUsers) {
            List<WatchHistory> similarUserHistory = watchHistoryRepository.findByUserIdOrderByWatchedAtDesc(similarUserId);
            recommendedContentIds.addAll(
                similarUserHistory.stream()
                    .filter(h -> h.getCompletionPercentage() > 70) // Only well-watched content
                    .map(WatchHistory::getContentId)
                    .collect(Collectors.toSet())
            );
        }
        
        // Remove already watched content
        List<WatchHistory> userHistory = watchHistoryRepository.findByUserIdOrderByWatchedAtDesc(userId);
        Set<String> watchedContentIds = userHistory.stream()
            .map(WatchHistory::getContentId)
            .collect(Collectors.toSet());
        
        recommendedContentIds.removeAll(watchedContentIds);
        
        return contentRepository.findAllById(recommendedContentIds)
            .stream().limit(10).collect(Collectors.toList());
    }
    
    private List<Content> getTrendingContent() {
        return contentRepository.findTop10ByOrderByViewCountDesc();
    }
    
    private List<Content> getNewReleases() {
        return contentRepository.findTop10ByOrderByCreatedAtDesc();
    }
    
    private List<String> findSimilarUsers(String userId) {
        List<WatchHistory> userHistory = watchHistoryRepository.findByUserIdOrderByWatchedAtDesc(userId);
        Set<String> userContentIds = userHistory.stream()
            .map(WatchHistory::getContentId)
            .collect(Collectors.toSet());
        
        if (userContentIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Find users who watched similar content
        Map<String, Integer> userSimilarityScore = new HashMap<>();
        
        for (String contentId : userContentIds) {
            List<WatchHistory> otherViewers = watchHistoryRepository.findByContentId(contentId);
            for (WatchHistory viewer : otherViewers) {
                if (!viewer.getUserId().equals(userId)) {
                    userSimilarityScore.merge(viewer.getUserId(), 1, Integer::sum);
                }
            }
        }
        
        return userSimilarityScore.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    public List<Content> searchContent(String query, String genre, Integer year) {
        if (query != null && !query.trim().isEmpty()) {
            return contentRepository.findByTitleContainingIgnoreCase(query);
        }
        
        if (genre != null && year != null) {
            return contentRepository.findByGenresContainingAndReleaseYear(genre, year);
        }
        
        if (genre != null) {
            return contentRepository.findByGenresContaining(genre);
        }
        
        if (year != null) {
            return contentRepository.findByReleaseYear(year);
        }
        
        return contentRepository.findAll();
    }
}