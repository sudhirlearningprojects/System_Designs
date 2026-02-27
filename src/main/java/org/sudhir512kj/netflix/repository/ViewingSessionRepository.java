package org.sudhir512kj.netflix.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.ViewingSession;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ViewingSessionRepository extends CassandraRepository<ViewingSession, UUID> {
    Flux<ViewingSession> findByUserId(UUID userId);
    Flux<ViewingSession> findByContentId(UUID contentId);
}
