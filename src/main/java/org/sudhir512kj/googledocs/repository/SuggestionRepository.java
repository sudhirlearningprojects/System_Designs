package org.sudhir512kj.googledocs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.googledocs.model.Suggestion;
import java.util.List;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, String> {
    List<Suggestion> findByDocumentId(String documentId);
    List<Suggestion> findByDocumentIdAndStatus(String documentId, Suggestion.SuggestionStatus status);
}
