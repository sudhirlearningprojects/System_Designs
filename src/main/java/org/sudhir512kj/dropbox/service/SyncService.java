package org.sudhir512kj.dropbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.dropbox.dto.SyncEvent;
import org.sudhir512kj.dropbox.model.FileEntity;
import org.sudhir512kj.dropbox.model.User;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    private final SimpMessagingTemplate messagingTemplate;
    private final FileService fileService;
    
    public void notifyFileChange(FileEntity file, String operation) {
        SyncEvent event = new SyncEvent();
        event.setFileId(file.getId());
        event.setFileName(file.getName());
        event.setPath(file.getPath());
        event.setOperation(operation);
        event.setTimestamp(LocalDateTime.now());
        event.setUserId(file.getOwner().getId());
        
        String destination = "/topic/dropbox/sync/" + file.getOwner().getId();
        messagingTemplate.convertAndSend(destination, event);
        
        log.info("Sent sync notification for file: {} to user: {}", 
                file.getName(), file.getOwner().getId());
    }
    
    public List<FileEntity> getChangesSince(User user, LocalDateTime since) {
        return fileService.getChangedFiles(user, since);
    }
    
    public void resolveConflict(FileEntity file1, FileEntity file2, String resolution) {
        log.info("Resolving conflict between versions of file: {}", file1.getName());
        
        switch (resolution.toLowerCase()) {
            case "keep_both":
                String newName = file2.getName() + "_conflict_" + System.currentTimeMillis();
                file2.setName(newName);
                break;
            case "keep_latest":
                if (file1.getUpdatedAt().isAfter(file2.getUpdatedAt())) {
                    file2.setIsDeleted(true);
                } else {
                    file1.setIsDeleted(true);
                }
                break;
            case "manual":
                log.info("Manual conflict resolution required for: {}", file1.getName());
                break;
        }
    }
}