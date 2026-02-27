package org.sudhir512kj.netflix.dto;

public class WatchProgressRequest {
    private String contentId;
    private int currentPosition;
    private String quality;
    
    public WatchProgressRequest() {}
    
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    
    public int getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }
    
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}