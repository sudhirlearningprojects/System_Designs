package org.sudhir512kj.netflix.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import org.sudhir512kj.netflix.model.ViewingSession;
import java.util.UUID;

@Repository
public interface ViewingSessionRepository extends ReactiveCassandraRepository<ViewingSession, UUID> {
    
    Flux<ViewingSession> findByUserId(Long userId);
    
    Flux<ViewingSession> findByContentId(String contentId);
    
    Flux<ViewingSession> findByUserIdAndContentId(Long userId, String contentId);
}