package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.spotify.dto.TrackDTO;
import org.sudhir512kj.spotify.dto.UploadTrackRequest;
import org.sudhir512kj.spotify.model.Track;
import org.sudhir512kj.spotify.repository.TrackRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {
    private final TrackRepository trackRepository;
    private final StorageService storageService;
    private final CacheService cacheService;
    
    @Transactional
    public Track uploadTrack(UploadTrackRequest request, MultipartFile audioFile) {
        log.info("Uploading track: {}", request.getTitle());
        
        Track track = Track.builder()
            .title(request.getTitle())
            .artistId(request.getArtistId())
            .albumId(request.getAlbumId())
            .isrc(request.getIsrc())
            .durationMs(request.getDurationMs())
            .lyrics(request.getLyrics())
            .trackType(Track.TrackType.valueOf(request.getTrackType()))
            .isExplicit(request.getIsExplicit())
            .releaseDate(request.getReleaseDate())
            .genre(request.getGenre())
            .language(request.getLanguage())
            .playCount(0L)
            .createdAt(LocalDateTime.now())
            .isActive(true)
            .build();
        
        // Upload audio file in multiple qualities
        String trackId = java.util.UUID.randomUUID().toString();
        track.setId(trackId);
        
        // Store original file and transcode to different qualities
        storageService.uploadAudioFile(audioFile, trackId);
        
        return trackRepository.save(track);
    }
    
    public Track getTrack(String trackId) {
        return cacheService.getTrack(trackId)
            .orElseGet(() -> trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Track not found")));
    }
    
    public Page<Track> getArtistTracks(String artistId, Pageable pageable) {
        return trackRepository.findByArtistId(artistId, pageable);
    }
    
    public Page<Track> getTopTracks(Pageable pageable) {
        return trackRepository.findTopTracks(pageable);
    }
    
    @Transactional
    public void incrementPlayCount(String trackId) {
        Track track = getTrack(trackId);
        track.setPlayCount(track.getPlayCount() + 1);
        trackRepository.save(track);
        cacheService.invalidateTrack(trackId);
    }
    
    @Transactional
    public Track updateTrack(String trackId, UploadTrackRequest request) {
        Track track = getTrack(trackId);
        track.setTitle(request.getTitle());
        track.setGenre(request.getGenre());
        track.setLyrics(request.getLyrics());
        track.setUpdatedAt(LocalDateTime.now());
        
        Track updated = trackRepository.save(track);
        cacheService.invalidateTrack(trackId);
        return updated;
    }
    
    @Transactional
    public void deleteTrack(String trackId) {
        Track track = getTrack(trackId);
        track.setIsActive(false);
        trackRepository.save(track);
        cacheService.invalidateTrack(trackId);
    }
}
