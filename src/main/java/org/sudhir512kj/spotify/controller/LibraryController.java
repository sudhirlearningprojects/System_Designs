package org.sudhir512kj.spotify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.spotify.model.UserLibrary;
import org.sudhir512kj.spotify.service.UserLibraryService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {
    private final UserLibraryService userLibraryService;
    
    @PostMapping
    public ResponseEntity<Void> addToLibrary(
            @RequestParam String userId,
            @RequestParam String entityId,
            @RequestParam UserLibrary.EntityType entityType) {
        userLibraryService.addToLibrary(userId, entityId, entityType);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> removeFromLibrary(
            @RequestParam String userId,
            @RequestParam String entityId,
            @RequestParam UserLibrary.EntityType entityType) {
        userLibraryService.removeFromLibrary(userId, entityId, entityType);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    public ResponseEntity<List<UserLibrary>> getUserLibrary(
            @RequestParam String userId,
            @RequestParam UserLibrary.EntityType entityType) {
        return ResponseEntity.ok(userLibraryService.getUserLibrary(userId, entityType));
    }
}
