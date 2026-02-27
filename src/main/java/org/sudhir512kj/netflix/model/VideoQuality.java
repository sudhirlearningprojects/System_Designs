package org.sudhir512kj.netflix.model;

public enum VideoQuality {
    SD_360P("360p", 800),
    SD_480P("480p", 1000),
    HD_720P("720p", 2500),
    FHD_1080P("1080p", 5000),
    UHD_4K("4K", 15000);
    
    private final String resolution;
    private final int bitrate;
    
    VideoQuality(String resolution, int bitrate) {
        this.resolution = resolution;
        this.bitrate = bitrate;
    }
    
    public String getResolution() { return resolution; }
    public int getBitrate() { return bitrate; }
}