package org.sudhir512kj.netflix.model;

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.annotation.Id;
import java.util.List;
import java.util.Map;

@RedisHash("content")
public class ContentMetadata {
    @Id
    private String contentId;
    private String title;
    private String type;
    private List<String> genres;
    private Integer duration;
    private String thumbnailUrl;
    private Map<String, String> videoUrls;
    private Double rating;
    private Long viewCount;
    
    public ContentMetadata() {}
    
    public ContentMetadata(String contentId, String title, String type) {
        this.contentId = contentId;
        this.title = title;
        this.type = type;
        this.viewCount = 0L;
    }
    
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public Map<String, String> getVideoUrls() { return videoUrls; }
    public void setVideoUrls(Map<String, String> videoUrls) { this.videoUrls = videoUrls; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
}