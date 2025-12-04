package org.sudhir512kj.spotify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.spotify.model.UserLibrary;
import java.util.List;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, String> {
    List<UserLibrary> findByUserIdAndEntityType(String userId, UserLibrary.EntityType entityType);
    boolean existsByUserIdAndEntityTypeAndEntityId(String userId, UserLibrary.EntityType entityType, String entityId);
    void deleteByUserIdAndEntityTypeAndEntityId(String userId, UserLibrary.EntityType entityType, String entityId);
}
