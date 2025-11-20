package org.sudhir512kj.whatsapp.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.service.UserService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    
    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(@DestinationVariable String chatId, @Payload TypingEvent event) {
        log.info("User {} is typing in chat {}", event.getUserId(), chatId);
        
        // Update user status to typing
        userService.updateUserStatus(event.getUserId(), User.UserStatus.TYPING);
        
        // Broadcast typing indicator to other participants
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/typing", event);
    }
    
    @MessageMapping("/chat/{chatId}/stop-typing")
    public void handleStopTyping(@DestinationVariable String chatId, @Payload TypingEvent event) {
        log.info("User {} stopped typing in chat {}", event.getUserId(), chatId);
        
        // Update user status to online
        userService.updateUserStatus(event.getUserId(), User.UserStatus.ONLINE);
        
        // Broadcast stop typing to other participants
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/stop-typing", event);
    }
    
    @MessageMapping("/user/{userId}/status")
    public void handleUserStatus(@DestinationVariable String userId, @Payload UserStatusEvent event) {
        log.info("User {} status changed to {}", userId, event.getStatus());
        
        userService.updateUserStatus(userId, event.getStatus());
        
        // Broadcast status change to contacts
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", event);
    }
    
    public static class TypingEvent {
        private String userId;
        private String userName;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
    }
    
    public static class UserStatusEvent {
        private User.UserStatus status;
        
        public User.UserStatus getStatus() { return status; }
        public void setStatus(User.UserStatus status) { this.status = status; }
    }
}