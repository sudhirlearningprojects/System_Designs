package org.sudhir512kj.whatsapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.service.ConnectionManagerService;
import org.sudhir512kj.whatsapp.service.PresenceService;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {
    
    private final PresenceService presenceService;
    private final ConnectionManagerService connectionManager;
    
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestParam String userId) {
        presenceService.heartbeat(userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<String> getUserPresence(@PathVariable String userId) {
        User.UserStatus status = presenceService.getUserPresence(userId);
        return ResponseEntity.ok(status.name());
    }
    
    @PostMapping("/{userId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String userId, @RequestParam User.UserStatus status) {
        presenceService.updateUserPresence(userId, status);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/online/{userId}")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable String userId) {
        boolean isOnline = connectionManager.isUserOnline(userId);
        return ResponseEntity.ok(isOnline);
    }
}