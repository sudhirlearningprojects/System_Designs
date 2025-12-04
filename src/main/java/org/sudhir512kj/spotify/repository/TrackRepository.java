package org.sudhir512kj.spotify.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.spotify.model.Track;
import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, String> {
    Page<Track> findByArtistId(String artistId, Pageable pageable);
    Page<Track> findByAlbumId(String albumId, Pageable pageable);
    List<Track> findByGenre(String genre);
    
    @Query("SELECT t FROM Track t WHERE t.isActive = true ORDER BY t.playCount DESC")
    Page<Track> findTopTracks(Pageable pageable);
}
