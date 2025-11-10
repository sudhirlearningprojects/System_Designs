package org.sudhir512kj.dropbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.dropbox.model.FileEntity;
import org.sudhir512kj.dropbox.repository.FileRepository;
import java.util.List;
import java.util.Optional;

@Service
public class DeduplicationService {
    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);
    private final FileRepository fileRepository;
    private final StorageService storageService;
    
    public DeduplicationService(FileRepository fileRepository, StorageService storageService) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
    }
    
    public Optional<FileEntity> findExistingFile(String contentHash) {
        List<FileEntity> existingFiles = fileRepository.findByContentHash(contentHash);
        
        if (!existingFiles.isEmpty()) {
            if (storageService.fileExists(contentHash)) {
                log.info("Found existing file with hash: {}", contentHash);
                return Optional.of(existingFiles.get(0));
            } else {
                log.warn("File hash exists in DB but not in storage: {}", contentHash);
            }
        }
        
        return Optional.empty();
    }
    
    public long calculateStorageSavings(String contentHash) {
        List<FileEntity> duplicateFiles = fileRepository.findByContentHash(contentHash);
        
        if (duplicateFiles.size() <= 1) {
            return 0L;
        }
        
        long fileSize = duplicateFiles.get(0).getSize() != null ? duplicateFiles.get(0).getSize() : 0L;
        return (duplicateFiles.size() - 1) * fileSize;
    }
}