package org.sudhir512kj.dropbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.dropbox.model.FileEntity;
import org.sudhir512kj.dropbox.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByOwnerAndIsDeletedFalse(User owner);
    List<FileEntity> findByOwnerAndPathStartingWithAndIsDeletedFalse(User owner, String path);
    Optional<FileEntity> findByOwnerAndPathAndIsDeletedFalse(User owner, String path);
    List<FileEntity> findByContentHash(String contentHash);
    
    @Query("SELECT f FROM FileEntity f WHERE f.owner = :owner AND f.updatedAt > :since AND f.isDeleted = false")
    List<FileEntity> findChangedFilesSince(User owner, LocalDateTime since);
}