package org.sudhir512kj.netflix.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private UUID userId;
    private String name;
    private String email;
    
    @ElementCollection
    @CollectionTable(name = "user_profile_preferences")
    private List<String> preferences;
    
    private String subscriptionTier;
    
    @ElementCollection
    @CollectionTable(name = "user_genre_affinity")
    @MapKeyColumn(name = "genre")
    @Column(name = "score")
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
