package org.sudhir512kj.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.urlshortener.model.URL;

@Repository
public interface UrlRepository extends JpaRepository<URL, Long> {
    
    URL findByShortUrl(String shortUrl);
    
    boolean existsByShortUrl(String shortUrl);
}