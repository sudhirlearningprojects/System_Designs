package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {
    
    @Value("${spotify.storage.path:/tmp/spotify}")
    private String storagePath;
    
    public void uploadAudioFile(MultipartFile file, String trackId) {
        try {
            Path trackDir = Paths.get(storagePath, trackId);
            Files.createDirectories(trackDir);
            
            // Save original file
            Path originalPath = trackDir.resolve("original.mp3");
            file.transferTo(originalPath.toFile());
            
            // Transcode to different qualities (simulated)
            transcodeAudio(originalPath, trackDir);
            
            log.info("Audio file uploaded for track: {}", trackId);
        } catch (IOException e) {
            log.error("Failed to upload audio file", e);
            throw new RuntimeException("Failed to upload audio file", e);
        }
    }
    
    private void transcodeAudio(Path originalPath, Path trackDir) {
        // In production: use FFmpeg to transcode to 96kbps, 160kbps, 320kbps, FLAC
        // For now, simulate by copying files
        try {
            Files.copy(originalPath, trackDir.resolve("96kbps.mp3"));
            Files.copy(originalPath, trackDir.resolve("160kbps.mp3"));
            Files.copy(originalPath, trackDir.resolve("320kbps.mp3"));
            Files.copy(originalPath, trackDir.resolve("lossless.flac"));
        } catch (IOException e) {
            log.error("Failed to transcode audio", e);
        }
    }
    
    public byte[] getAudioFile(String trackId, String quality) {
        try {
            Path filePath = Paths.get(storagePath, trackId, quality + ".mp3");
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read audio file", e);
            throw new RuntimeException("Failed to read audio file", e);
        }
    }
}
