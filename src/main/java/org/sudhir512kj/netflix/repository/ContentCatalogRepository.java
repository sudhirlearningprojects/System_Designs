package org.sudhir512kj.netflix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.Content;
import java.util.List;

@Repository
public interface ContentCatalogRepository extends JpaRepository<Content, String> {
    List<Content> findByGenresContaining(String genre);
    List<Content> findByTitleContainingIgnoreCase(String title);
}
