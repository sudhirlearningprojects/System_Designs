package org.sudhir512kj.uber.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.sudhir512kj.uber.model.Location;
import org.sudhir512kj.uber.service.H3GeoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocationWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(LocationWebSocketHandler.class);
    private final H3GeoService h3GeoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    public LocationWebSocketHandler(H3GeoService h3GeoService) {
        this.h3GeoService = h3GeoService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String driverId = getDriverId(session);
        sessions.put(driverId, session);
        log.info("WebSocket connected: {}", driverId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            String payload = message.getPayload().toString();
            Location location = objectMapper.readValue(payload, Location.class);
            String driverId = getDriverId(session);
            
            h3GeoService.updateDriverLocation(UUID.fromString(driverId), location);
            log.debug("Location updated for driver: {}", driverId);
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String driverId = getDriverId(session);
        sessions.remove(driverId);
        log.info("WebSocket disconnected: {}", driverId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error", exception);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getDriverId(WebSocketSession session) {
        return session.getUri().getQuery().split("=")[1];
    }
}
