package org.sudhir512kj.dropbox.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.dropbox.model.FileEntity;
import org.sudhir512kj.dropbox.model.User;
import org.sudhir512kj.dropbox.service.FileService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dropbox/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    
    @PostMapping("/upload")
    public ResponseEntity<FileEntity> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path,
            @AuthenticationPrincipal User user) throws IOException {
        
        FileEntity uploadedFile = fileService.uploadFile(file, path, user);
        return ResponseEntity.ok(uploadedFile);
    }
    
    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal User user) throws IOException {
        
        byte[] fileContent = fileService.downloadFile(fileId, user);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
    }
    
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal User user) {
        
        fileService.deleteFile(fileId, user);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/changes")
    public ResponseEntity<List<FileEntity>> getChanges(
            @RequestParam("since") LocalDateTime since,
            @AuthenticationPrincipal User user) {
        
        List<FileEntity> changes = fileService.getChangedFiles(user, since);
        return ResponseEntity.ok(changes);
    }
}