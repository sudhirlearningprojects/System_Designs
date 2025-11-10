package org.sudhir512kj.googledocs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.googledocs.model.Document;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByOwnerId(String ownerId);
    
    @Query("SELECT d FROM Document d JOIN d.permissions p WHERE p.userId = :userId")
    List<Document> findBySharedWithUser(String userId);
    
    List<Document> findByStatus(Document.DocumentStatus status);
}
