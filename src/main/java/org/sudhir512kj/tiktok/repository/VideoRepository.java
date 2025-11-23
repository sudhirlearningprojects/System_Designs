package org.sudhir512kj.tiktok.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.sudhir512kj.tiktok.model.Video;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.isPublic = true ORDER BY v.createdAt DESC")
    List<Video> findPublicVideos(Pageable pageable);
}
