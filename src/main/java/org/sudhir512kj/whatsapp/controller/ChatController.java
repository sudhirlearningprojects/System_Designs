package org.sudhir512kj.whatsapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.whatsapp.dto.ChatDTO;
import org.sudhir512kj.whatsapp.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    @PostMapping("/individual")
    public ResponseEntity<ChatDTO> createIndividualChat(@RequestParam String userId1, @RequestParam String userId2) {
        ChatDTO chat = chatService.createIndividualChat(userId1, userId2);
        return ResponseEntity.ok(chat);
    }
    
    @PostMapping("/group")
    public ResponseEntity<ChatDTO> createGroupChat(
            @RequestParam String creatorId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam List<String> participantIds) {
        ChatDTO chat = chatService.createGroupChat(creatorId, name, description, participantIds);
        return ResponseEntity.ok(chat);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatDTO>> getUserChats(@PathVariable String userId) {
        List<ChatDTO> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }
    
    @PostMapping("/{chatId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable String chatId,
            @RequestParam String userId,
            @RequestParam String adminId) {
        chatService.addParticipant(chatId, userId, adminId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String chatId,
            @PathVariable String userId,
            @RequestParam String adminId) {
        chatService.removeParticipant(chatId, userId, adminId);
        return ResponseEntity.ok().build();
    }
}