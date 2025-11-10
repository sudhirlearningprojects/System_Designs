package org.sudhir512kj.googledocs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.googledocs.model.Version;
import java.util.List;

@Repository
public interface VersionRepository extends JpaRepository<Version, String> {
    List<Version> findByDocumentIdOrderByVersionNumberDesc(String documentId);
}
