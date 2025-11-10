package org.sudhir512kj.googledocs.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.sudhir512kj.googledocs.model.ActiveSession;
import org.sudhir512kj.googledocs.ot.Operation;
import org.sudhir512kj.googledocs.service.DocumentService;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class CollaborationController {
    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentService documentService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @MessageMapping("/document/{documentId}/edit")
    @SendTo("/topic/document/{documentId}")
    public Operation handleEdit(@DestinationVariable String documentId, Operation operation) {
        documentService.updateDocument(documentId, operation);
        return operation;
    }
    
    @MessageMapping("/document/{documentId}/cursor")
    @SendTo("/topic/document/{documentId}/cursors")
    public Map<String, Object> handleCursorMove(@DestinationVariable String documentId, 
                                                 @Payload Map<String, Object> cursorData) {
        String userId = (String) cursorData.get("userId");
        Integer position = (Integer) cursorData.get("position");
        
        ActiveSession session = ActiveSession.builder()
            .documentId(documentId)
            .userId(userId)
            .userName((String) cursorData.get("userName"))
            .cursorPosition(position)
            .lastActivity(LocalDateTime.now())
            .build();
        
        redisTemplate.opsForHash().put("sessions:" + documentId, userId, session);
        
        return cursorData;
    }
    
    @MessageMapping("/document/{documentId}/join")
    public void handleJoin(@DestinationVariable String documentId, @Payload Map<String, String> userData) {
        String userId = userData.get("userId");
        String userName = userData.get("userName");
        
        ActiveSession session = ActiveSession.builder()
            .documentId(documentId)
            .userId(userId)
            .userName(userName)
            .cursorPosition(0)
            .lastActivity(LocalDateTime.now())
            .build();
        
        redisTemplate.opsForHash().put("sessions:" + documentId, userId, session);
        
        Map<Object, Object> allSessions = redisTemplate.opsForHash().entries("sessions:" + documentId);
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/users", allSessions.values());
    }
    
    @MessageMapping("/document/{documentId}/leave")
    public void handleLeave(@DestinationVariable String documentId, @Payload Map<String, String> userData) {
        String userId = userData.get("userId");
        redisTemplate.opsForHash().delete("sessions:" + documentId, userId);
        
        Map<Object, Object> allSessions = redisTemplate.opsForHash().entries("sessions:" + documentId);
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/users", allSessions.values());
    }
}
