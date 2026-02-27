package org.sudhir512kj.netflix.model;

import java.util.List;
import java.util.UUID;

public class ContentMetadata {
    private UUID contentId;
    private String title;
    private ContentType type;
    private List<String> genres;
    private Integer duration;
    private String description;
    private Double rating;
    private String thumbnailUrl;
    private String trailerUrl;
    private Long viewCount;
    
    public ContentMetadata() {}
    
    public ContentMetadata(UUID contentId, String title, ContentType type) {
        this.contentId = contentId;
        this.title = title;
        this.type = type;
    }
    
    // Getters and setters
    public UUID getContentId() { return contentId; }
    public void setContentId(UUID contentId) { this.contentId = contentId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public ContentType getType() { return type; }
    public void setType(ContentType type) { this.type = type; }
    
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
}