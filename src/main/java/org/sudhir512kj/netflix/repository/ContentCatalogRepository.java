package org.sudhir512kj.netflix.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.Content;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentCatalogRepository extends CassandraRepository<Content, UUID> {
    List<Content> findByGenresContaining(String genre);
    List<Content> findByTitleContainingIgnoreCase(String title);
}
