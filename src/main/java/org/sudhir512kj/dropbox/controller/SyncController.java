package org.sudhir512kj.dropbox.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.sudhir512kj.dropbox.dto.SyncEvent;
import org.sudhir512kj.dropbox.service.SyncService;

@Controller
@RequiredArgsConstructor
public class SyncController {
    private final SyncService syncService;
    
    @MessageMapping("/dropbox/sync")
    @SendToUser("/topic/dropbox/sync")
    public SyncEvent handleSyncMessage(@Payload SyncEvent syncEvent) {
        return syncEvent;
    }
}