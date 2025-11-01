package org.sudhir512kj.dropbox.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class StorageService {
    
    @Value("${app.dropbox.storage.path:/tmp/dropbox}")
    private String storagePath;
    
    public String storeFile(MultipartFile file, String contentHash) throws IOException {
        Path storageDir = Paths.get(storagePath);
        Files.createDirectories(storageDir);
        
        Path targetPath = storageDir.resolve(contentHash);
        
        if (!Files.exists(targetPath)) {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored at: {}", targetPath);
        }
        
        return targetPath.toString();
    }
    
    public byte[] retrieveFile(String contentHash) throws IOException {
        Path filePath = Paths.get(storagePath, contentHash);
        
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found in storage: " + contentHash);
        }
        
        return Files.readAllBytes(filePath);
    }
    
    public boolean fileExists(String contentHash) {
        Path filePath = Paths.get(storagePath, contentHash);
        return Files.exists(filePath);
    }
}