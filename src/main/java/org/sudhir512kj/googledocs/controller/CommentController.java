package org.sudhir512kj.googledocs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.googledocs.dto.CommentDTO;
import org.sudhir512kj.googledocs.service.CommentService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    
    @PostMapping
    public ResponseEntity<CommentDTO> addComment(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(commentService.addComment(
            (String) request.get("documentId"),
            (String) request.get("userId"),
            (String) request.get("content"),
            (Integer) request.get("startPosition"),
            (Integer) request.get("endPosition")
        ));
    }
    
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentDTO> addReply(@PathVariable String commentId, 
                                                @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(commentService.addReply(
            commentId,
            request.get("userId"),
            request.get("content")
        ));
    }
    
    @PostMapping("/{commentId}/reactions")
    public ResponseEntity<CommentDTO> addReaction(@PathVariable String commentId, 
                                                   @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(commentService.addReaction(
            commentId,
            request.get("userId"),
            request.get("emoji")
        ));
    }
    
    @PutMapping("/{commentId}/resolve")
    public ResponseEntity<CommentDTO> resolveComment(@PathVariable String commentId) {
        return ResponseEntity.ok(commentService.resolveComment(commentId));
    }
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<CommentDTO>> getDocumentComments(@PathVariable String documentId) {
        return ResponseEntity.ok(commentService.getDocumentComments(documentId));
    }
}
