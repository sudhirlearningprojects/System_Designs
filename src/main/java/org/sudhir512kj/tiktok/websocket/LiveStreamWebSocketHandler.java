package org.sudhir512kj.tiktok.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LiveStreamWebSocketHandler implements WebSocketHandler {
    private final Map<String, Set<WebSocketSession>> streamSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String streamId = getStreamId(session);
        streamSessions.computeIfAbsent(streamId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String streamId = getStreamId(session);
        Map<String, Object> payload = objectMapper.readValue(message.getPayload().toString(), Map.class);
        
        String type = (String) payload.get("type");
        
        if ("COMMENT".equals(type)) {
            broadcastToStream(streamId, payload);
        } else if ("LIKE".equals(type)) {
            broadcastToStream(streamId, payload);
        } else if ("GIFT".equals(type)) {
            broadcastToStream(streamId, payload);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String streamId = getStreamId(session);
        Set<WebSocketSession> sessions = streamSessions.get(streamId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                streamSessions.remove(streamId);
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        // Log error
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void broadcastToStream(String streamId, Object message) {
        Set<WebSocketSession> sessions = streamSessions.get(streamId);
        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                } catch (Exception e) {
                    // Log error
                }
            });
        }
    }
    
    private String getStreamId(WebSocketSession session) {
        return session.getUri().getQuery().split("=")[1];
    }
}
