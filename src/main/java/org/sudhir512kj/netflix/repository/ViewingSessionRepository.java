package org.sudhir512kj.netflix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.ViewingSession;
import java.util.List;
import java.util.UUID;

@Repository
public interface ViewingSessionRepository extends JpaRepository<ViewingSession, UUID> {
    List<ViewingSession> findByUserId(UUID userId);
    List<ViewingSession> findByContentId(UUID contentId);
}
