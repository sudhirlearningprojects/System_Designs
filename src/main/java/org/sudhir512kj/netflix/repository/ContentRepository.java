package org.sudhir512kj.netflix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.Content;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, String> {
    
    List<Content> findByTitleContainingIgnoreCase(String title);
    
    List<Content> findByGenresContaining(String genre);
    
    List<Content> findByGenresContainingAndReleaseYear(String genre, Integer year);
    
    List<Content> findByReleaseYear(Integer year);
    
    List<Content> findByType(Content.ContentType type);
    
    @Query("SELECT c FROM Content c WHERE c.genres IN :genres ORDER BY c.imdbScore DESC")
    List<Content> findByGenresInOrderByImdbScoreDesc(List<String> genres);
    
    List<Content> findTop10ByOrderByViewCountDesc();
    
    List<Content> findTop10ByOrderByCreatedAtDesc();
    
    @Query("SELECT c FROM Content c WHERE c.imdbScore >= :minScore ORDER BY c.imdbScore DESC")
    List<Content> findByImdbScoreGreaterThanEqualOrderByImdbScoreDesc(Double minScore);
}