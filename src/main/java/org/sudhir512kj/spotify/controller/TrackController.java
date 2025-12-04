package org.sudhir512kj.spotify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.spotify.dto.UploadTrackRequest;
import org.sudhir512kj.spotify.model.Track;
import org.sudhir512kj.spotify.service.SearchService;
import org.sudhir512kj.spotify.service.TrackService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tracks")
@RequiredArgsConstructor
public class TrackController {
    private final TrackService trackService;
    private final SearchService searchService;
    
    @PostMapping("/upload")
    public ResponseEntity<Track> uploadTrack(
            @RequestPart("metadata") UploadTrackRequest request,
            @RequestPart("audioFile") MultipartFile audioFile) {
        Track track = trackService.uploadTrack(request, audioFile);
        searchService.indexTrack(track);
        return ResponseEntity.status(HttpStatus.CREATED).body(track);
    }
    
    @GetMapping("/{trackId}")
    public ResponseEntity<Track> getTrack(@PathVariable String trackId) {
        return ResponseEntity.ok(trackService.getTrack(trackId));
    }
    
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<Page<Track>> getArtistTracks(
            @PathVariable String artistId,
            Pageable pageable) {
        return ResponseEntity.ok(trackService.getArtistTracks(artistId, pageable));
    }
    
    @GetMapping("/top")
    public ResponseEntity<Page<Track>> getTopTracks(Pageable pageable) {
        return ResponseEntity.ok(trackService.getTopTracks(pageable));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Track>> searchTracks(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchTracks(query));
    }
    
    @PutMapping("/{trackId}")
    public ResponseEntity<Track> updateTrack(
            @PathVariable String trackId,
            @RequestBody UploadTrackRequest request) {
        return ResponseEntity.ok(trackService.updateTrack(trackId, request));
    }
    
    @DeleteMapping("/{trackId}")
    public ResponseEntity<Void> deleteTrack(@PathVariable String trackId) {
        trackService.deleteTrack(trackId);
        return ResponseEntity.noContent().build();
    }
}
