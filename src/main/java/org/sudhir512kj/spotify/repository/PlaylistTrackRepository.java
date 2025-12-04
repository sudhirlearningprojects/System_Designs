package org.sudhir512kj.spotify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.spotify.model.PlaylistTrack;
import java.util.List;

@Repository
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, String> {
    List<PlaylistTrack> findByPlaylistIdOrderByPositionAsc(String playlistId);
    void deleteByPlaylistIdAndTrackId(String playlistId, String trackId);
    boolean existsByPlaylistIdAndTrackId(String playlistId, String trackId);
}
