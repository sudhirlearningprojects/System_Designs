package org.sudhir512kj.netflix.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Table("encoded_videos")
public class EncodedVideo {
    @PrimaryKey
    private UUID videoId;
    private UUID contentId;
    private VideoQuality quality;
    private String videoUrl;
    private Long fileSize;
    private EncodingStatus status;
    private String sourceUrl;
    private Instant createdAt;
    private Instant completedAt;
    private Map<VideoQuality, String> encodedUrls;
    
    public EncodedVideo() {}
    
    public EncodedVideo(UUID videoId, UUID contentId, VideoQuality quality) {
        this.videoId = videoId;
        this.contentId = contentId;
        this.quality = quality;
        this.status = EncodingStatus.PENDING;
        this.createdAt = Instant.now();
    }
    
    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }
    
    public UUID getEncodingId() { return videoId; }
    public void setEncodingId(UUID encodingId) { this.videoId = encodingId; }
    
    public UUID getContentId() { return contentId; }
    public void setContentId(UUID contentId) { this.contentId = contentId; }
    
    public VideoQuality getQuality() { return quality; }
    public void setQuality(VideoQuality quality) { this.quality = quality; }
    
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public EncodingStatus getStatus() { return status; }
    public void setStatus(EncodingStatus status) { this.status = status; }
    
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    
    public Map<VideoQuality, String> getEncodedUrls() { return encodedUrls; }
    public void setEncodedUrls(Map<VideoQuality, String> encodedUrls) { this.encodedUrls = encodedUrls; }
}