package org.sudhir512kj.spotify.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.spotify.model.ListeningHistory;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Repository
public interface ListeningHistoryRepository extends ReactiveCassandraRepository<ListeningHistory, UUID> {
    Flux<ListeningHistory> findByUserId(String userId);
}
