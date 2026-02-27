package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.sudhir512kj.netflix.model.Content;
import org.sudhir512kj.netflix.repository.ContentCatalogRepository;
import java.util.UUID;

@Service
public class ContentService {
    
    @Autowired
    private ContentCatalogRepository contentRepository;
    
    public Flux<Content> searchContent(String query) {
        return Flux.fromIterable(contentRepository.findByTitleContainingIgnoreCase(query));
    }
    
    public Flux<Content> getContentByGenre(String genre) {
        return Flux.fromIterable(contentRepository.findByGenresContaining(genre));
    }
    
    public Mono<Content> getContentById(UUID contentId) {
        return Mono.justOrEmpty(contentRepository.findById(contentId));
    }
    
    public Mono<Content> saveContent(Content content) {
        return Mono.just(contentRepository.save(content));
    }
}