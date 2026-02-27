package org.sudhir512kj.netflix.dto;

public class StreamingRequest {
    private String contentId;
    private String deviceType;
    private String bandwidth;
    
    public StreamingRequest() {}
    
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public String getBandwidth() { return bandwidth; }
    public void setBandwidth(String bandwidth) { this.bandwidth = bandwidth; }
}