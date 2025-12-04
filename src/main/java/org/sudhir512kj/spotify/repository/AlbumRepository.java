package org.sudhir512kj.spotify.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.spotify.model.Album;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, String> {
    Page<Album> findByArtistId(String artistId, Pageable pageable);
    List<Album> findByGenre(String genre);
}
