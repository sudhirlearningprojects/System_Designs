package org.sudhir512kj.tiktok.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.sudhir512kj.tiktok.dto.ChatMessageDTO;
import org.sudhir512kj.tiktok.model.ChatMessage;
import org.sudhir512kj.tiktok.service.AnalyticsService;
import org.sudhir512kj.tiktok.service.ChatModerationService;
import org.sudhir512kj.tiktok.service.ChatService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStreamWebSocketHandler implements WebSocketHandler {
    private final Map<String, Set<WebSocketSession>> streamSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatService chatService;
    private final ChatModerationService moderationService;
    private final AnalyticsService analyticsService;
    private final RateLimiter chatRateLimiter = RateLimiter.create(10.0);
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String streamId = getStreamId(session);
        Long userId = getUserId(session);
        
        streamSessions.computeIfAbsent(streamId, k -> ConcurrentHashMap.newKeySet()).add(session);
        
        // Send recent chat history
        List<ChatMessage> history = chatService.getRecentMessages(Long.parseLong(streamId), 50);
        sendToSession(session, Map.of("type", "HISTORY", "messages", history));
        
        // Broadcast join notification
        broadcastToStream(streamId, Map.of("type", "JOIN", "userId", userId));
        
        log.info("User {} joined stream {}", userId, streamId);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String streamId = getStreamId(session);
        Long userId = getUserId(session);
        
        // Rate limiting
        if (!chatRateLimiter.tryAcquire()) {
            sendToSession(session, Map.of("type", "ERROR", "message", "Rate limit exceeded"));
            return;
        }
        
        Map<String, Object> payload = objectMapper.readValue(message.getPayload().toString(), Map.class);
        String type = (String) payload.get("type");
        
        if ("COMMENT".equals(type)) {
            String content = (String) payload.get("content");
            
            // Content moderation
            if (moderationService.containsInappropriateContent(content)) {
                sendToSession(session, Map.of("type", "ERROR", "message", "Message blocked by moderation"));
                return;
            }
            
            // Save to database
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.setContent(content);
            ChatMessage chatMessage = chatService.saveMessage(Long.parseLong(streamId), userId, dto);
            
            // Broadcast to all viewers
            broadcastToStream(streamId, Map.of(
                "type", "COMMENT",
                "userId", userId,
                "content", content,
                "timestamp", chatMessage.getTimestamp()
            ));
            
            // Update analytics
            analyticsService.incrementChatCount(Long.parseLong(streamId));
            
        } else if ("LIKE".equals(type)) {
            broadcastToStream(streamId, Map.of("type", "LIKE", "userId", userId));
        } else if ("GIFT".equals(type)) {
            broadcastToStream(streamId, payload);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String streamId = getStreamId(session);
        Long userId = getUserId(session);
        
        Set<WebSocketSession> sessions = streamSessions.get(streamId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                streamSessions.remove(streamId);
            }
        }
        
        // Broadcast leave notification
        broadcastToStream(streamId, Map.of("type", "LEAVE", "userId", userId));
        
        log.info("User {} left stream {}", userId, streamId);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error", exception);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void broadcastToStream(String streamId, Object message) {
        Set<WebSocketSession> sessions = streamSessions.get(streamId);
        if (sessions == null) return;
        
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message", e);
            return;
        }
        
        sessions.parallelStream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("Failed to send message to session", e);
                }
            });
    }
    
    private void sendToSession(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send message to session", e);
        }
    }
    
    private String getStreamId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return Arrays.stream(query.split("&"))
            .filter(param -> param.startsWith("streamId="))
            .map(param -> param.split("=")[1])
            .findFirst()
            .orElse("0");
    }
    
    private Long getUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return Arrays.stream(query.split("&"))
            .filter(param -> param.startsWith("userId="))
            .map(param -> param.split("=")[1])
            .findFirst()
            .map(Long::parseLong)
            .orElse(0L);
    }
}
