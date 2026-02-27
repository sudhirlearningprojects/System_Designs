package org.sudhir512kj.netflix.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Table("user_profiles")
public class UserProfile {
    @PrimaryKey
    private UUID userId;
    private String name;
    private String email;
    private List<String> preferences;
    private String subscriptionTier;
    private Map<String, Double> genreAffinityScores;
    
    public UserProfile() {}
    
    public UserProfile(UUID userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public List<String> getPreferences() { return preferences; }
    public void setPreferences(List<String> preferences) { this.preferences = preferences; }
    
    public String getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    
    public Map<String, Double> getGenreAffinityScores() { return genreAffinityScores; }
    public void setGenreAffinityScores(Map<String, Double> genreAffinityScores) { this.genreAffinityScores = genreAffinityScores; }
}