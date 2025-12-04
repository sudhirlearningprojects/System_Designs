package org.sudhir512kj.spotify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.spotify.model.Playlist;
import org.sudhir512kj.spotify.model.PlaylistTrack;
import org.sudhir512kj.spotify.service.PlaylistService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;
    
    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(
            @RequestParam String userId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") Boolean isPublic) {
        Playlist playlist = playlistService.createPlaylist(userId, name, description, isPublic);
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }
    
    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<Void> addTrack(
            @PathVariable String playlistId,
            @RequestParam String trackId,
            @RequestParam String userId) {
        playlistService.addTrackToPlaylist(playlistId, trackId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrack(
            @PathVariable String playlistId,
            @PathVariable String trackId) {
        playlistService.removeTrackFromPlaylist(playlistId, trackId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Playlist>> getUserPlaylists(@PathVariable String userId) {
        return ResponseEntity.ok(playlistService.getUserPlaylists(userId));
    }
    
    @GetMapping("/{playlistId}/tracks")
    public ResponseEntity<List<PlaylistTrack>> getPlaylistTracks(@PathVariable String playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylistTracks(playlistId));
    }
}
