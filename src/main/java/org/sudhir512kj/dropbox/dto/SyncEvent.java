package org.sudhir512kj.dropbox.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SyncEvent {
    private UUID fileId;
    private String fileName;
    private String path;
    private String operation; // UPLOAD, DELETE, UPDATE, RENAME
    private LocalDateTime timestamp;
    private UUID userId;
    private String deviceId;
}