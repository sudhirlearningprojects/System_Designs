package org.sudhir512kj.whatsapp.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.service.ConnectionManagerService;
import org.sudhir512kj.whatsapp.service.MessageDeliveryService;
import org.sudhir512kj.whatsapp.service.PresenceService;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final PresenceService presenceService;
    private final ConnectionManagerService connectionManager;
    private final MessageDeliveryService messageDeliveryService;
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getFirstNativeHeader("userId");
        String serverId = getServerId(); // Get current server ID
        
        if (userId != null) {
            // Register connection
            connectionManager.registerConnection(userId, serverId);
            
            // Update user presence
            presenceService.updateUserPresence(userId, User.UserStatus.ONLINE);
            
            // Deliver offline messages
            messageDeliveryService.deliverOfflineMessages(userId);
            
            log.info("User {} connected with session {}", userId, sessionId);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getFirstNativeHeader("userId");
        
        if (userId != null) {
            // Unregister connection
            connectionManager.unregisterConnection(userId);
            
            // Update user presence to offline
            presenceService.updateUserPresence(userId, User.UserStatus.OFFLINE);
            
            log.info("User {} disconnected with session {}", userId, sessionId);
        }
    }
    
    private String getServerId() {
        // In production, this would be the actual server ID
        // For now, using a simple identifier
        return "server-" + System.getProperty("server.port", "8093");
    }
}