package org.sudhir512kj.googledocs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.googledocs.dto.SuggestionDTO;
import org.sudhir512kj.googledocs.service.SuggestionService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {
    private final SuggestionService suggestionService;
    
    @PostMapping
    public ResponseEntity<SuggestionDTO> createSuggestion(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(suggestionService.createSuggestion(
            (String) request.get("documentId"),
            (String) request.get("userId"),
            (Integer) request.get("startPosition"),
            (Integer) request.get("endPosition"),
            (String) request.get("originalText"),
            (String) request.get("suggestedText")
        ));
    }
    
    @PutMapping("/{suggestionId}/accept")
    public ResponseEntity<SuggestionDTO> acceptSuggestion(@PathVariable String suggestionId, 
                                                           @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(suggestionService.acceptSuggestion(suggestionId, request.get("userId")));
    }
    
    @PutMapping("/{suggestionId}/reject")
    public ResponseEntity<SuggestionDTO> rejectSuggestion(@PathVariable String suggestionId, 
                                                           @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(suggestionService.rejectSuggestion(suggestionId, request.get("userId")));
    }
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<SuggestionDTO>> getDocumentSuggestions(@PathVariable String documentId) {
        return ResponseEntity.ok(suggestionService.getDocumentSuggestions(documentId));
    }
}
