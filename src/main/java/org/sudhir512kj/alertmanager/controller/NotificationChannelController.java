package org.sudhir512kj.alertmanager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.alertmanager.dto.NotificationChannelRequest;
import org.sudhir512kj.alertmanager.model.NotificationChannel;
import org.sudhir512kj.alertmanager.service.NotificationChannelService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class NotificationChannelController {
    private final NotificationChannelService channelService;

    @PostMapping
    public ResponseEntity<NotificationChannel> createChannel(@RequestBody NotificationChannelRequest request) {
        return ResponseEntity.ok(channelService.createChannel(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationChannel> updateChannel(@PathVariable String id, @RequestBody NotificationChannelRequest request) {
        return ResponseEntity.ok(channelService.updateChannel(id, request));
    }

    @GetMapping
    public ResponseEntity<List<NotificationChannel>> getAllChannels() {
        return ResponseEntity.ok(channelService.getAllChannels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationChannel> getChannel(@PathVariable String id) {
        return ResponseEntity.ok(channelService.getChannel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable String id) {
        channelService.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }
}
