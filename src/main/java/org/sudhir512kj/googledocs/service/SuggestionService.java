package org.sudhir512kj.googledocs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.googledocs.dto.SuggestionDTO;
import org.sudhir512kj.googledocs.model.*;
import org.sudhir512kj.googledocs.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuggestionService {
    private final SuggestionRepository suggestionRepository;
    private final DocumentRepository documentRepository;
    
    @Transactional
    public SuggestionDTO createSuggestion(String documentId, String userId, Integer startPosition, 
                                          Integer endPosition, String originalText, String suggestedText) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        Suggestion suggestion = Suggestion.builder()
            .userId(userId)
            .startPosition(startPosition)
            .endPosition(endPosition)
            .originalText(originalText)
            .suggestedText(suggestedText)
            .status(Suggestion.SuggestionStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
        suggestion.setDocument(document);
        
        suggestion = suggestionRepository.save(suggestion);
        return toDTO(suggestion);
    }
    
    @Transactional
    public SuggestionDTO acceptSuggestion(String suggestionId, String userId) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        suggestion.setStatus(Suggestion.SuggestionStatus.ACCEPTED);
        suggestion.setResolvedAt(LocalDateTime.now());
        suggestion.setResolvedBy(userId);
        
        Document document = suggestion.getDocument();
        String content = document.getContent();
        content = content.substring(0, suggestion.getStartPosition()) + 
                  suggestion.getSuggestedText() + 
                  content.substring(suggestion.getEndPosition());
        document.setContent(content);
        documentRepository.save(document);
        
        suggestion = suggestionRepository.save(suggestion);
        return toDTO(suggestion);
    }
    
    @Transactional
    public SuggestionDTO rejectSuggestion(String suggestionId, String userId) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        suggestion.setStatus(Suggestion.SuggestionStatus.REJECTED);
        suggestion.setResolvedAt(LocalDateTime.now());
        suggestion.setResolvedBy(userId);
        
        suggestion = suggestionRepository.save(suggestion);
        return toDTO(suggestion);
    }
    
    public List<SuggestionDTO> getDocumentSuggestions(String documentId) {
        return suggestionRepository.findByDocumentId(documentId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    private SuggestionDTO toDTO(Suggestion suggestion) {
        return SuggestionDTO.builder()
            .id(suggestion.getId())
            .userId(suggestion.getUserId())
            .startPosition(suggestion.getStartPosition())
            .endPosition(suggestion.getEndPosition())
            .originalText(suggestion.getOriginalText())
            .suggestedText(suggestion.getSuggestedText())
            .status(suggestion.getStatus().name())
            .createdAt(suggestion.getCreatedAt())
            .build();
    }
}
