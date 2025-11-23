package org.sudhir512kj.tiktok.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.tiktok.dto.LiveStreamRequest;
import org.sudhir512kj.tiktok.model.LiveStream;
import org.sudhir512kj.tiktok.service.LiveStreamService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
public class LiveStreamController {
    private final LiveStreamService liveStreamService;
    
    @PostMapping("/create")
    public ResponseEntity<LiveStream> createStream(
            @RequestParam Long userId,
            @RequestBody LiveStreamRequest request) {
        return ResponseEntity.ok(liveStreamService.createLiveStream(userId, request));
    }
    
    @PostMapping("/start")
    public ResponseEntity<Void> startStream(@RequestParam String streamKey) {
        liveStreamService.startStream(streamKey);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/end")
    public ResponseEntity<Void> endStream(@RequestParam String streamKey) {
        liveStreamService.endStream(streamKey);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<LiveStream>> getActiveStreams() {
        return ResponseEntity.ok(liveStreamService.getActiveLiveStreams());
    }
    
    @PostMapping("/{streamId}/join")
    public ResponseEntity<Void> joinStream(
            @PathVariable Long streamId,
            @RequestParam Long userId) {
        liveStreamService.joinStream(streamId, userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{streamId}/leave")
    public ResponseEntity<Void> leaveStream(
            @PathVariable Long streamId,
            @RequestParam Long userId) {
        liveStreamService.leaveStream(streamId, userId);
        return ResponseEntity.ok().build();
    }
}
