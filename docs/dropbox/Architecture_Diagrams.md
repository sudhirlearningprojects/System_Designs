# Cloud Storage System - Architecture Diagrams

## Understanding Cloud Storage Architecture

### What Makes Cloud Storage Complex?
Cloud storage systems like Dropbox face unique challenges:

1. **File Synchronization**: Keep files consistent across multiple devices
2. **Conflict Resolution**: Handle simultaneous edits to the same file
3. **Data Deduplication**: Avoid storing duplicate content
4. **Version Control**: Track file changes over time
5. **Real-time Updates**: Notify all devices of changes instantly

### Key Architectural Decisions

#### Microservices vs Monolith for File Storage

**Why Microservices for Dropbox?**
- **Independent Scaling**: File service needs different scaling than user service
- **Technology Diversity**: Use specialized databases for different needs
- **Team Autonomy**: Different teams can work on sync vs storage
- **Fault Isolation**: Thumbnail generation failure doesn't affect file uploads

#### Service Responsibilities
```java
// User Service - Handles authentication and user management
@RestController
public class UserController {
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getStorageQuota(), user.getStorageUsed());
    }
}

// File Service - Handles file operations and metadata
@RestController
public class FileController {
    @PostMapping("/upload")
    public FileResponse uploadFile(@RequestParam MultipartFile file, 
                                 @RequestHeader("Authorization") String token) {
        Long userId = jwtService.extractUserId(token);
        
        // Calculate hash for deduplication
        String fileHash = hashService.calculateSHA256(file.getBytes());
        
        // Check if file already exists
        Optional<FileMetadata> existing = fileService.findByHash(fileHash);
        if (existing.isPresent()) {
            return fileService.createReference(userId, file.getOriginalFilename(), existing.get());
        }
        
        // Store new file
        return fileService.storeNewFile(userId, file, fileHash);
    }
}

// Sync Service - Handles real-time synchronization
@Component
public class SyncService {
    @EventListener
    public void handleFileChange(FileChangeEvent event) {
        // Get all devices for the user
        List<String> deviceIds = deviceService.getActiveDevices(event.getUserId());
        
        // Send real-time notifications
        for (String deviceId : deviceIds) {
            webSocketService.sendToDevice(deviceId, new SyncNotification(
                event.getFileId(), 
                event.getChangeType(), 
                event.getTimestamp()
            ));
        }
    }
}
```

#### CDN Strategy for File Delivery
```
Traditional Approach:
User in Tokyo → US Server → File Storage (300ms latency)

CDN Approach:
User in Tokyo → Tokyo Edge Server → Cached File (50ms latency)
                      ↓ (Cache Miss)
                 US Origin Server
```

### Data Flow Patterns

#### Upload Flow Design Decisions
1. **Chunked Upload**: Handle large files and resume capability
2. **Hash Calculation**: Client-side to reduce server load
3. **Deduplication Check**: Before storage to save space
4. **Async Processing**: Thumbnail generation doesn't block upload

#### Sync Flow Patterns
```java
// Push-based Sync (Real-time)
public class PushSyncService {
    public void notifyFileChange(FileChangeEvent event) {
        // Immediate notification to all connected devices
        List<WebSocketSession> sessions = getActiveSessionsForUser(event.getUserId());
        
        SyncMessage message = new SyncMessage(
            event.getFileId(),
            event.getChangeType(),
            event.getFileVersion(),
            event.getTimestamp()
        );
        
        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (Exception e) {
                log.error("Failed to send sync message to session: {}", session.getId(), e);
            }
        });
    }
}

// Pull-based Sync (Polling fallback)
public class PullSyncService {
    public List<FileChange> getChangesSince(Long userId, Long timestamp) {
        return fileChangeRepository.findByUserIdAndTimestampAfter(userId, timestamp);
    }
}
```

## 1. High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        WC[Web Client]
        MC[Mobile Client]
        DC[Desktop Client]
    end
    
    subgraph "CDN & Load Balancer"
        CDN[CloudFront CDN]
        LB[Application Load Balancer]
    end
    
    subgraph "API Gateway"
        AG[API Gateway]
        RL[Rate Limiting]
        AUTH[Authentication]
    end
    
    subgraph "Microservices"
        US[User Service]
        FS[File Service]
        SS[Sync Service]
        NS[Notification Service]
        TS[Thumbnail Service]
    end
    
    subgraph "Message Queue"
        KAFKA[Apache Kafka]
        REDIS[Redis Cache]
    end
    
    subgraph "Storage Layer"
        S3[Amazon S3]
        RDS[PostgreSQL RDS]
        ES[Elasticsearch]
    end
    
    WC --> CDN
    MC --> CDN
    DC --> CDN
    CDN --> LB
    LB --> AG
    AG --> RL
    AG --> AUTH
    AG --> US
    AG --> FS
    AG --> SS
    
    FS --> KAFKA
    SS --> KAFKA
    NS --> KAFKA
    
    US --> RDS
    FS --> RDS
    FS --> S3
    SS --> REDIS
    
    KAFKA --> NS
    KAFKA --> TS
    
    TS --> S3
```

## 2. File Upload Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AG as API Gateway
    participant FS as File Service
    participant DD as Dedup Service
    participant S3 as Storage
    participant DB as Database
    participant SYNC as Sync Service
    
    C->>AG: Upload File Request
    AG->>FS: Process Upload
    FS->>FS: Calculate SHA-256 Hash
    FS->>DD: Check Deduplication
    
    alt File Exists
        DD-->>FS: Return Existing Reference
    else New File
        FS->>S3: Store File Chunks
        S3-->>FS: Storage Confirmation
    end
    
    FS->>DB: Save Metadata
    DB-->>FS: Metadata Saved
    FS->>SYNC: Trigger Sync Event
    SYNC->>C: Real-time Notification
    FS-->>AG: Upload Success
    AG-->>C: Response
```

### File Deduplication Deep Dive

#### Why Deduplication Matters
```
Scenario: 1 million users upload the same 10MB video
Without deduplication: 1M × 10MB = 10TB storage
With deduplication: 1 × 10MB = 10MB storage
Savings: 99.9999% storage reduction
```

#### Deduplication Strategies

##### File-Level Deduplication
```java
public class FileLevelDeduplication {
    public FileUploadResult uploadFile(MultipartFile file, Long userId) {
        // Calculate hash of entire file
        String fileHash = DigestUtils.sha256Hex(file.getBytes());
        
        // Check if file already exists
        Optional<StoredFile> existingFile = fileRepository.findByHash(fileHash);
        
        if (existingFile.isPresent()) {
            // Create reference to existing file
            FileMetadata metadata = new FileMetadata();
            metadata.setUserId(userId);
            metadata.setFileName(file.getOriginalFilename());
            metadata.setStoredFileId(existingFile.get().getId());
            
            // Increment reference count
            existingFile.get().incrementReferenceCount();
            
            return FileUploadResult.deduplicated(metadata);
        }
        
        // Store new file
        return storeNewFile(file, fileHash, userId);
    }
}
```

##### Block-Level Deduplication (More Efficient)
```java
public class BlockLevelDeduplication {
    private static final int BLOCK_SIZE = 4 * 1024 * 1024; // 4MB blocks
    
    public FileUploadResult uploadFileWithBlockDedup(MultipartFile file, Long userId) {
        List<FileBlock> blocks = new ArrayList<>();
        byte[] fileData = file.getBytes();
        
        // Split file into blocks
        for (int i = 0; i < fileData.length; i += BLOCK_SIZE) {
            int blockSize = Math.min(BLOCK_SIZE, fileData.length - i);
            byte[] blockData = Arrays.copyOfRange(fileData, i, i + blockSize);
            String blockHash = DigestUtils.sha256Hex(blockData);
            
            // Check if block already exists
            Optional<StoredBlock> existingBlock = blockRepository.findByHash(blockHash);
            
            if (existingBlock.isPresent()) {
                // Reference existing block
                blocks.add(new FileBlock(blockHash, existingBlock.get().getStoragePath(), i));
                existingBlock.get().incrementReferenceCount();
            } else {
                // Store new block
                String storagePath = storageService.storeBlock(blockData, blockHash);
                StoredBlock newBlock = new StoredBlock(blockHash, storagePath, 1);
                blockRepository.save(newBlock);
                blocks.add(new FileBlock(blockHash, storagePath, i));
            }
        }
        
        // Create file metadata with block references
        FileMetadata metadata = new FileMetadata();
        metadata.setUserId(userId);
        metadata.setFileName(file.getOriginalFilename());
        metadata.setBlocks(blocks);
        
        return FileUploadResult.success(metadata);
    }
}
```

## 3. File Synchronization Architecture

### Understanding Sync Complexity

This diagram shows how multiple devices stay synchronized. The key challenges are:

1. **Real-time Communication**: WebSocket connections for instant updates
2. **Offline Handling**: Queue changes when devices are offline
3. **Conflict Detection**: Identify when same file is modified on multiple devices
4. **Bandwidth Optimization**: Only sync changed parts of files

#### Sync Agent Responsibilities
```java
public class SyncAgent {
    private final WebSocketClient webSocketClient;
    private final LocalFileWatcher fileWatcher;
    private final ConflictResolver conflictResolver;
    
    @PostConstruct
    public void initialize() {
        // Watch local file system for changes
        fileWatcher.onFileChanged(this::handleLocalFileChange);
        
        // Connect to sync service
        webSocketClient.connect(syncServiceUrl);
        webSocketClient.onMessage(this::handleRemoteFileChange);
    }
    
    private void handleLocalFileChange(FileChangeEvent event) {
        // Calculate file hash
        String newHash = calculateFileHash(event.getFilePath());
        
        // Send change to server
        SyncMessage message = new SyncMessage(
            event.getFilePath(),
            newHash,
            System.currentTimeMillis(),
            getDeviceId()
        );
        
        webSocketClient.send(message);
    }
    
    private void handleRemoteFileChange(SyncMessage message) {
        String localPath = message.getFilePath();
        File localFile = new File(localPath);
        
        if (localFile.exists()) {
            String localHash = calculateFileHash(localPath);
            
            if (!localHash.equals(message.getFileHash())) {
                // Conflict detected
                ConflictResolution resolution = conflictResolver.resolve(
                    localFile, message);
                
                switch (resolution.getStrategy()) {
                    case KEEP_LOCAL:
                        // Do nothing
                        break;
                    case KEEP_REMOTE:
                        downloadAndReplaceFile(message);
                        break;
                    case KEEP_BOTH:
                        createConflictCopy(localFile);
                        downloadAndReplaceFile(message);
                        break;
                }
            }
        } else {
            // File doesn't exist locally, download it
            downloadFile(message);
        }
    }
}
```

```mermaid
graph LR
    subgraph "Device A"
        DA[Desktop App]
        DAS[Sync Agent]
    end
    
    subgraph "Device B"
        MB[Mobile App]
        MBS[Sync Agent]
    end
    
    subgraph "Cloud Services"
        SS[Sync Service]
        WS[WebSocket Server]
        MQ[Message Queue]
        FS[File Service]
    end
    
    subgraph "Storage"
        META[Metadata DB]
        FILES[File Storage]
    end
    
    DAS <--> WS
    MBS <--> WS
    WS <--> SS
    SS <--> MQ
    SS <--> FS
    FS <--> META
    FS <--> FILES
    
    DA --> DAS
    MB --> MBS
```

## 4. Data Deduplication Process

```mermaid
flowchart TD
    A[File Upload] --> B[Calculate SHA-256]
    B --> C{Hash Exists?}
    C -->|Yes| D[Reference Existing]
    C -->|No| E[Store New File]
    D --> F[Update Metadata]
    E --> G[Store Chunks]
    G --> F
    F --> H[Increment Reference Count]
    H --> I[Notify Sync Service]
```

## 5. Conflict Resolution Flow

```mermaid
stateDiagram-v2
    [*] --> FileModified
    FileModified --> ConflictDetected: Same file modified on multiple devices
    ConflictDetected --> LastWriterWins: Timestamp comparison
    ConflictDetected --> CreateConflictCopy: User preference
    ConflictDetected --> ManualResolution: Complex conflicts
    
    LastWriterWins --> Resolved
    CreateConflictCopy --> Resolved
    ManualResolution --> UserAction
    UserAction --> Resolved
    
    Resolved --> [*]
```

## 6. Database Schema Relationships

```mermaid
erDiagram
    USERS ||--o{ FILES : owns
    USERS ||--o{ FILE_SHARES : shares
    FILES ||--o{ FILE_CHUNKS : contains
    FILES ||--o{ FILE_SHARES : shared
    FILES ||--o{ FILE_VERSIONS : versioned
    
    USERS {
        uuid id PK
        string email UK
        string password_hash
        bigint storage_quota
        bigint storage_used
        timestamp created_at
    }
    
    FILES {
        uuid id PK
        string name
        text path
        bigint size
        string content_hash
        string mime_type
        uuid owner_id FK
        uuid parent_folder_id FK
        integer version
        boolean is_deleted
        timestamp created_at
        timestamp updated_at
    }
    
    FILE_CHUNKS {
        uuid id PK
        uuid file_id FK
        integer chunk_index
        string chunk_hash
        integer chunk_size
        text storage_path
    }
    
    FILE_SHARES {
        uuid id PK
        uuid file_id FK
        uuid shared_with_user_id FK
        string permission_level
        uuid shared_by_user_id FK
        timestamp expires_at
        timestamp created_at
    }
```

## 7. Microservices Communication

```mermaid
graph TB
    subgraph "Synchronous Communication"
        API[API Gateway] --> US[User Service]
        API --> FS[File Service]
        API --> SS[Sync Service]
    end
    
    subgraph "Asynchronous Communication"
        FS --> KAFKA[Kafka Topics]
        SS --> KAFKA
        KAFKA --> NS[Notification Service]
        KAFKA --> TS[Thumbnail Service]
        KAFKA --> AS[Analytics Service]
    end
    
    subgraph "Event Topics"
        KAFKA --> FU[file.uploaded]
        KAFKA --> FD[file.deleted]
        KAFKA --> FS_TOPIC[file.shared]
        KAFKA --> SY[sync.required]
    end
```

## 8. Caching Strategy

```mermaid
graph LR
    subgraph "Client Side"
        CC[Client Cache]
        LC[Local Storage]
    end
    
    subgraph "CDN Layer"
        CF[CloudFront]
        EC[Edge Cache]
    end
    
    subgraph "Application Layer"
        RC[Redis Cache]
        MC[Memory Cache]
    end
    
    subgraph "Database Layer"
        QC[Query Cache]
        BC[Buffer Cache]
    end
    
    CC --> CF
    CF --> RC
    RC --> QC
    LC --> CC
    EC --> RC
    MC --> BC
```

## 9. Security Architecture

```mermaid
graph TB
    subgraph "Authentication & Authorization"
        JWT[JWT Tokens]
        OAUTH[OAuth 2.0]
        RBAC[Role-Based Access Control]
    end
    
    subgraph "Data Protection"
        EIR[Encryption in Transit]
        EAR[Encryption at Rest]
        KMS[Key Management Service]
    end
    
    subgraph "Network Security"
        WAF[Web Application Firewall]
        VPC[Virtual Private Cloud]
        SG[Security Groups]
    end
    
    subgraph "Monitoring & Auditing"
        CT[CloudTrail]
        CW[CloudWatch]
        AL[Audit Logs]
    end
    
    JWT --> RBAC
    OAUTH --> JWT
    EIR --> KMS
    EAR --> KMS
    WAF --> VPC
    VPC --> SG
    CT --> AL
    CW --> AL
```

## 10. Scalability Patterns

```mermaid
graph TB
    subgraph "Horizontal Scaling"
        LB[Load Balancer] --> MS1[Microservice Instance 1]
        LB --> MS2[Microservice Instance 2]
        LB --> MS3[Microservice Instance N]
    end
    
    subgraph "Database Scaling"
        MASTER[Master DB] --> SLAVE1[Read Replica 1]
        MASTER --> SLAVE2[Read Replica 2]
        MASTER --> SLAVE3[Read Replica N]
    end
    
    subgraph "Storage Scaling"
        S3[S3 Storage] --> PART1[Partition 1]
        S3 --> PART2[Partition 2]
        S3 --> PART3[Partition N]
    end
    
    subgraph "Caching Scaling"
        REDIS[Redis Cluster] --> SHARD1[Shard 1]
        REDIS --> SHARD2[Shard 2]
        REDIS --> SHARD3[Shard N]
    end
```