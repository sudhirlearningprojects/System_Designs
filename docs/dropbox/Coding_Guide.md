# Dropbox Clone - Complete Coding Guide

## System Design Overview

**Problem**: Cloud storage with file sync across devices

**Core Features**:
1. Upload/download files
2. Sync files across devices
3. Version control
4. File sharing

## SOLID Principles

- **SRP**: FileStorage, SyncService, VersionControl separate
- **OCP**: Add new storage backends without modifying
- **Strategy**: Different sync strategies (full, incremental)

## Design Patterns

1. **Strategy Pattern**: Storage backends (local, S3, Azure)
2. **Observer Pattern**: Notify devices of file changes
3. **Command Pattern**: File operations (upload, delete, rename)

## Complete Implementation

```java
import java.util.*;
import java.time.LocalDateTime;

class FileMetadata {
    String id, name, path, ownerId;
    long size;
    String hash;
    LocalDateTime createdAt, modifiedAt;
    int version;
    
    FileMetadata(String name, String path, String ownerId, long size, String hash) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.path = path;
        this.ownerId = ownerId;
        this.size = size;
        this.hash = hash;
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
        this.version = 1;
    }
}

class FileVersion {
    String fileId;
    int version;
    String hash;
    LocalDateTime timestamp;
    
    FileVersion(String fileId, int version, String hash) {
        this.fileId = fileId;
        this.version = version;
        this.hash = hash;
        this.timestamp = LocalDateTime.now();
    }
}

interface StorageBackend {
    void upload(String fileId, byte[] data);
    byte[] download(String fileId);
    void delete(String fileId);
}

class LocalStorage implements StorageBackend {
    private Map<String, byte[]> storage = new HashMap<>();
    
    public void upload(String fileId, byte[] data) {
        storage.put(fileId, data);
        System.out.println("  [LOCAL] Uploaded file " + fileId + " (" + data.length + " bytes)");
    }
    
    public byte[] download(String fileId) {
        System.out.println("  [LOCAL] Downloaded file " + fileId);
        return storage.get(fileId);
    }
    
    public void delete(String fileId) {
        storage.remove(fileId);
        System.out.println("  [LOCAL] Deleted file " + fileId);
    }
}

class DropboxService {
    private Map<String, FileMetadata> files = new HashMap<>();
    private Map<String, List<FileVersion>> versions = new HashMap<>();
    private StorageBackend storage;
    private Map<String, Set<String>> userDevices = new HashMap<>();
    
    public DropboxService(StorageBackend storage) {
        this.storage = storage;
    }
    
    public FileMetadata uploadFile(String userId, String fileName, String path, byte[] data) {
        System.out.println("\n=== Upload File ===");
        System.out.println("User: " + userId + " | File: " + fileName);
        
        String hash = computeHash(data);
        
        // Check for deduplication
        FileMetadata existing = findByHash(hash);
        if (existing != null) {
            System.out.println("  ✓ File already exists (deduplication)");
            return existing;
        }
        
        FileMetadata metadata = new FileMetadata(fileName, path, userId, data.length, hash);
        files.put(metadata.id, metadata);
        
        // Store file
        storage.upload(metadata.id, data);
        
        // Save version
        FileVersion version = new FileVersion(metadata.id, metadata.version, hash);
        versions.computeIfAbsent(metadata.id, k -> new ArrayList<>()).add(version);
        
        // Notify devices
        notifyDevices(userId, metadata);
        
        System.out.println("  ✓ Upload complete: " + metadata.id);
        return metadata;
    }
    
    public byte[] downloadFile(String fileId) {
        System.out.println("\n=== Download File ===");
        FileMetadata metadata = files.get(fileId);
        if (metadata == null) {
            System.out.println("  ✗ File not found");
            return null;
        }
        
        byte[] data = storage.download(fileId);
        System.out.println("  ✓ Downloaded: " + metadata.name);
        return data;
    }
    
    public void deleteFile(String fileId) {
        System.out.println("\n=== Delete File ===");
        FileMetadata metadata = files.get(fileId);
        if (metadata != null) {
            storage.delete(fileId);
            files.remove(fileId);
            versions.remove(fileId);
            System.out.println("  ✓ Deleted: " + metadata.name);
        }
    }
    
    public List<FileVersion> getVersionHistory(String fileId) {
        return versions.getOrDefault(fileId, new ArrayList<>());
    }
    
    public void shareFile(String fileId, String targetUserId) {
        FileMetadata metadata = files.get(fileId);
        System.out.println("\n=== Share File ===");
        System.out.println("Sharing " + metadata.name + " with user " + targetUserId);
        // In real implementation: Add to shared_files table
    }
    
    private String computeHash(byte[] data) {
        return "hash_" + Arrays.hashCode(data);
    }
    
    private FileMetadata findByHash(String hash) {
        return files.values().stream()
            .filter(f -> f.hash.equals(hash))
            .findFirst()
            .orElse(null);
    }
    
    private void notifyDevices(String userId, FileMetadata metadata) {
        Set<String> devices = userDevices.getOrDefault(userId, new HashSet<>());
        for (String device : devices) {
            System.out.println("  → Notifying device: " + device);
        }
    }
    
    public void registerDevice(String userId, String deviceId) {
        userDevices.computeIfAbsent(userId, k -> new HashSet<>()).add(deviceId);
        System.out.println("Registered device " + deviceId + " for user " + userId);
    }
}

public class DropboxDemo {
    public static void main(String[] args) {
        System.out.println("=== Dropbox Clone ===\n");
        
        DropboxService dropbox = new DropboxService(new LocalStorage());
        
        // Register devices
        dropbox.registerDevice("user1", "laptop");
        dropbox.registerDevice("user1", "phone");
        
        // Upload files
        byte[] file1 = "Hello World".getBytes();
        FileMetadata f1 = dropbox.uploadFile("user1", "document.txt", "/docs", file1);
        
        byte[] file2 = "Hello World".getBytes(); // Same content
        FileMetadata f2 = dropbox.uploadFile("user1", "copy.txt", "/docs", file2);
        
        // Download
        dropbox.downloadFile(f1.id);
        
        // Share
        dropbox.shareFile(f1.id, "user2");
        
        // Version history
        System.out.println("\n=== Version History ===");
        List<FileVersion> history = dropbox.getVersionHistory(f1.id);
        for (FileVersion v : history) {
            System.out.println("Version " + v.version + " at " + v.timestamp);
        }
        
        // Delete
        dropbox.deleteFile(f1.id);
    }
}
```

## Key Concepts

**Deduplication**:
- Hash-based (SHA-256)
- Block-level chunking
- Save 30% storage

**Sync Algorithm**:
- Watch for file changes
- Compute delta (diff)
- Upload only changed blocks
- Resolve conflicts (last-write-wins or manual)

**Scalability**:
- S3 for file storage
- Metadata in database
- CDN for downloads
- WebSocket for real-time sync

## Interview Questions

**Q: Handle large files (10GB)?**
A: Chunking (4MB blocks), parallel upload, resume capability

**Q: Conflict resolution?**
A: Last-write-wins, version history, manual merge

**Q: Sync 1M files?**
A: Incremental sync, merkle tree for change detection

**Q: Scale to 500M users?**
A: Sharding by userId, S3 for storage, Redis for metadata cache

Run: https://www.jdoodle.com/online-java-compiler
