package org.sudhir512kj.dropbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.dropbox.model.FileEntity;
import org.sudhir512kj.dropbox.repository.FileRepository;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {
    private final FileRepository fileRepository;
    private final StorageService storageService;
    
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
        
        long fileSize = duplicateFiles.get(0).getSize();
        return (duplicateFiles.size() - 1) * fileSize;
    }
}