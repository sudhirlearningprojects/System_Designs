package org.sudhir512kj.netflix.service;

import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.VideoQuality;
import java.util.Map;

@Service
public class AdaptiveBitrateService {
    
    public VideoQuality selectOptimalQuality(int bandwidth, String deviceType, int bufferHealth) {
        if (bandwidth > 10000 && bufferHealth > 80) {
            return VideoQuality.UHD_4K;
        } else if (bandwidth > 3000 && bufferHealth > 60) {
            return VideoQuality.FHD_1080P;
        } else if (bandwidth > 1500 && bufferHealth > 40) {
            return VideoQuality.HD_720P;
        } else {
            return VideoQuality.SD_480P;
        }
    }
    
    public Map<String, Object> getStreamingManifest(String contentId, VideoQuality quality) {
        return Map.of(
            "contentId", contentId,
            "quality", quality.getResolution(),
            "bitrate", quality.getBitrate(),
            "manifestUrl", "https://cdn.netflix.com/" + contentId + "/" + quality.getResolution() + "/playlist.m3u8"
        );
    }
}