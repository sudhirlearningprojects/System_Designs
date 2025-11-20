package org.sudhir512kj.whatsapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.whatsapp.dto.MessageDTO;
import org.sudhir512kj.whatsapp.dto.SendMessageRequest;
import org.sudhir512kj.whatsapp.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestParam String senderId, @RequestBody SendMessageRequest request) {
        MessageDTO message = messageService.sendMessage(senderId, request);
        return ResponseEntity.ok(message);
    }
    
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<Page<MessageDTO>> getChatMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MessageDTO> messages = messageService.getChatMessages(chatId, page, size);
        return ResponseEntity.ok(messages);
    }
    
    @PutMapping("/chat/{chatId}/read")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable String chatId, @RequestParam String userId) {
        messageService.markMessagesAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId, @RequestParam String userId) {
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<MessageDTO>> searchMessages(@RequestParam String chatId, @RequestParam String query) {
        List<MessageDTO> messages = messageService.searchMessages(chatId, query);
        return ResponseEntity.ok(messages);
    }
}