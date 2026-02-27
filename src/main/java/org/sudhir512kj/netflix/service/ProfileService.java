package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.UserProfile;
import org.sudhir512kj.netflix.repository.UserProfileRepository;
import java.util.*;

@Service
public class ProfileService {
    
    @Autowired
    private UserProfileRepository profileRepository;
    
    @Autowired
    private EVCacheService cacheService;
    
    public UserProfile getUserProfile(UUID userId) {
        return (UserProfile) cacheService.getOrCompute(
            "profile:" + userId,
            3600,
            () -> profileRepository.findById(userId).orElse(null)
        );
    }
    
    public void updateGenreAffinity(UUID userId, String genre, double score) {
        UserProfile profile = getUserProfile(userId);
        if (profile != null) {
            Map<String, Double> affinities = profile.getGenreAffinityScores();
            if (affinities == null) {
                affinities = new HashMap<>();
            }
            affinities.put(genre, affinities.getOrDefault(genre, 0.0) + score);
            profile.setGenreAffinityScores(affinities);
            profileRepository.save(profile);
            cacheService.delete("profile:" + userId);
        }
    }
}
