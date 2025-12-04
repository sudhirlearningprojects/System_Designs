package org.sudhir512kj.spotify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.spotify.model.Playlist;
import org.sudhir512kj.spotify.model.PlaylistTrack;
import org.sudhir512kj.spotify.repository.PlaylistRepository;
import org.sudhir512kj.spotify.repository.PlaylistTrackRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    
    @Transactional
    public Playlist createPlaylist(String userId, String name, String description, Boolean isPublic) {
        Playlist playlist = Playlist.builder()
            .name(name)
            .description(description)
            .userId(userId)
            .isPublic(isPublic)
            .isCollaborative(false)
            .trackCount(0)
            .followerCount(0L)
            .createdAt(LocalDateTime.now())
            .build();
        
        return playlistRepository.save(playlist);
    }
    
    @Transactional
    public void addTrackToPlaylist(String playlistId, String trackId, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found"));
        
        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, trackId)) {
            throw new RuntimeException("Track already in playlist");
        }
        
        int position = playlist.getTrackCount();
        
        PlaylistTrack playlistTrack = PlaylistTrack.builder()
            .playlistId(playlistId)
            .trackId(trackId)
            .position(position)
            .addedBy(userId)
            .addedAt(LocalDateTime.now())
            .build();
        
        playlistTrackRepository.save(playlistTrack);
        
        playlist.setTrackCount(playlist.getTrackCount() + 1);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
    }
    
    @Transactional
    public void removeTrackFromPlaylist(String playlistId, String trackId) {
        playlistTrackRepository.deleteByPlaylistIdAndTrackId(playlistId, trackId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found"));
        playlist.setTrackCount(Math.max(0, playlist.getTrackCount() - 1));
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
    }
    
    public List<Playlist> getUserPlaylists(String userId) {
        return playlistRepository.findByUserId(userId);
    }
    
    public List<PlaylistTrack> getPlaylistTracks(String playlistId) {
        return playlistTrackRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
    }
}
