package org.sudhir512kj.dropbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.sudhir512kj.dropbox.model.FileEntity;
import org.sudhir512kj.dropbox.model.User;
import org.sudhir512kj.dropbox.repository.FileRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final DeduplicationService deduplicationService;
    private final SyncService syncService;
    
    public FileService(FileRepository fileRepository, StorageService storageService,
                      DeduplicationService deduplicationService, SyncService syncService) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.deduplicationService = deduplicationService;
        this.syncService = syncService;
    }
    
    @Transactional
    public FileEntity uploadFile(MultipartFile file, String path, User owner) throws IOException {
        String contentHash = calculateHash(file.getBytes());
        
        Optional<FileEntity> existingFile = deduplicationService.findExistingFile(contentHash);
        
        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(file.getOriginalFilename());
        fileEntity.setPath(path);
        fileEntity.setSize(file.getSize());
        fileEntity.setContentHash(contentHash);
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setOwner(owner);
        
        if (existingFile.isEmpty()) {
            storageService.storeFile(file, contentHash);
            log.info("Stored new file with hash: {}", contentHash);
        } else {
            log.info("File deduplicated: {}", contentHash);
        }
        
        FileEntity savedFile = fileRepository.save(fileEntity);
        syncService.notifyFileChange(savedFile, "UPLOAD");
        
        return savedFile;
    }
    
    public byte[] downloadFile(UUID fileId, User user) throws IOException {
        FileEntity file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
            
        if (!hasReadPermission(file, user)) {
            throw new RuntimeException("Access denied");
        }
        
        return storageService.retrieveFile(file.getContentHash());
    }
    
    @Transactional
    public void deleteFile(UUID fileId, User user) {
        FileEntity file = fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));
            
        if (!hasWritePermission(file, user)) {
            throw new RuntimeException("Access denied");
        }
        
        file.setIsDeleted(true);
        fileRepository.save(file);
        syncService.notifyFileChange(file, "DELETE");
    }
    
    public List<FileEntity> getChangedFiles(User user, LocalDateTime since) {
        return fileRepository.findChangedFilesSince(user, since);
    }
    
    private String calculateHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private boolean hasReadPermission(FileEntity file, User user) {
        return file.getOwner().equals(user);
    }
    
    private boolean hasWritePermission(FileEntity file, User user) {
        return file.getOwner().equals(user);
    }
}