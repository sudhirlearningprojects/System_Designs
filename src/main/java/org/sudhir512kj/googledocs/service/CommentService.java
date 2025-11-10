package org.sudhir512kj.googledocs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.googledocs.dto.*;
import org.sudhir512kj.googledocs.model.*;
import org.sudhir512kj.googledocs.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    
    @Transactional
    public CommentDTO addComment(String documentId, String userId, String content, 
                                  Integer startPosition, Integer endPosition) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        Comment comment = Comment.builder()
            .content(content)
            .userId(userId)
            .startPosition(startPosition)
            .endPosition(endPosition)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .status(Comment.CommentStatus.OPEN)
            .build();
        comment.setDocument(document);
        
        comment = commentRepository.save(comment);
        return toDTO(comment);
    }
    
    @Transactional
    public CommentDTO addReply(String commentId, String userId, String content) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        Reply reply = Reply.builder()
            .content(content)
            .userId(userId)
            .createdAt(LocalDateTime.now())
            .build();
        reply.setComment(comment);
        
        comment.getReplies().add(reply);
        comment = commentRepository.save(comment);
        
        return toDTO(comment);
    }
    
    @Transactional
    public CommentDTO addReaction(String commentId, String userId, String emoji) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        comment.getReactions().put(userId, emoji);
        comment = commentRepository.save(comment);
        
        return toDTO(comment);
    }
    
    @Transactional
    public CommentDTO resolveComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        comment.setStatus(Comment.CommentStatus.RESOLVED);
        comment = commentRepository.save(comment);
        
        return toDTO(comment);
    }
    
    public List<CommentDTO> getDocumentComments(String documentId) {
        return commentRepository.findByDocumentId(documentId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    private CommentDTO toDTO(Comment comment) {
        return CommentDTO.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .userId(comment.getUserId())
            .startPosition(comment.getStartPosition())
            .endPosition(comment.getEndPosition())
            .createdAt(comment.getCreatedAt())
            .status(comment.getStatus().name())
            .replies(comment.getReplies().stream()
                .map(r -> ReplyDTO.builder()
                    .id(r.getId())
                    .content(r.getContent())
                    .userId(r.getUserId())
                    .createdAt(r.getCreatedAt())
                    .build())
                .collect(Collectors.toList()))
            .reactions(comment.getReactions())
            .build();
    }
}
