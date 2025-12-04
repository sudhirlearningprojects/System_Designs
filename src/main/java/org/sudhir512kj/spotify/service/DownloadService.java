package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.spotify.model.DownloadedTrack;
import org.sudhir512kj.spotify.repository.DownloadedTrackRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadService {
    private final DownloadedTrackRepository downloadedTrackRepository;
    private final StorageService storageService;
    
    @Transactional
    public DownloadedTrack downloadTrack(String userId, String trackId, String deviceId, String quality) {
        if (downloadedTrackRepository.existsByUserIdAndTrackIdAndDeviceId(userId, trackId, deviceId)) {
            throw new RuntimeException("Track already downloaded");
        }
        
        byte[] audioData = storageService.getAudioFile(trackId, quality);
        
        DownloadedTrack download = DownloadedTrack.builder()
            .userId(userId)
            .trackId(trackId)
            .deviceId(deviceId)
            .audioQuality(DownloadedTrack.AudioQuality.valueOf(quality))
            .downloadedAt(LocalDateTime.now())
            .lastAccessedAt(LocalDateTime.now())
            .fileSizeBytes((long) audioData.length)
            .build();
        
        return downloadedTrackRepository.save(download);
    }
    
    public List<DownloadedTrack> getUserDownloads(String userId, String deviceId) {
        return downloadedTrackRepository.findByUserIdAndDeviceId(userId, deviceId);
    }
    
    @Transactional
    public void deleteDownload(String userId, String trackId, String deviceId) {
        DownloadedTrack download = downloadedTrackRepository
            .findByUserIdAndTrackIdAndDeviceId(userId, trackId, deviceId)
            .orElseThrow(() -> new RuntimeException("Download not found"));
        
        downloadedTrackRepository.delete(download);
    }
}
