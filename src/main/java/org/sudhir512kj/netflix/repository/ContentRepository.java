package org.sudhir512kj.netflix.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import org.sudhir512kj.netflix.model.ContentMetadata;

@Repository
public interface ContentRepository extends ReactiveCrudRepository<ContentMetadata, String> {
    
    Flux<ContentMetadata> findByGenresContaining(String genre);
    
    Flux<ContentMetadata> findByType(String type);
}