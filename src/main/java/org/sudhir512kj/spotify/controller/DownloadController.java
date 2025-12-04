package org.sudhir512kj.spotify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.spotify.model.DownloadedTrack;
import org.sudhir512kj.spotify.service.DownloadService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/downloads")
@RequiredArgsConstructor
public class DownloadController {
    private final DownloadService downloadService;
    
    @PostMapping
    public ResponseEntity<DownloadedTrack> downloadTrack(
            @RequestParam String userId,
            @RequestParam String trackId,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "MEDIUM") String quality) {
        DownloadedTrack download = downloadService.downloadTrack(userId, trackId, deviceId, quality);
        return ResponseEntity.ok(download);
    }
    
    @GetMapping
    public ResponseEntity<List<DownloadedTrack>> getUserDownloads(
            @RequestParam String userId,
            @RequestParam String deviceId) {
        return ResponseEntity.ok(downloadService.getUserDownloads(userId, deviceId));
    }
    
    @DeleteMapping
    public ResponseEntity<Void> deleteDownload(
            @RequestParam String userId,
            @RequestParam String trackId,
            @RequestParam String deviceId) {
        downloadService.deleteDownload(userId, trackId, deviceId);
        return ResponseEntity.noContent().build();
    }
}
