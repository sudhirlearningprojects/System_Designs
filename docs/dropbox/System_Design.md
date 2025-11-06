# Cloud Storage System Design (Dropbox/Google Drive)

## Understanding Cloud Storage Systems

### What is Cloud Storage?
Cloud storage is a service that allows users to store, access, and manage files over the internet instead of on local devices. The system must handle file synchronization across multiple devices while ensuring data durability and availability.

### Key Challenges in Cloud Storage
1. **File Synchronization**: Keep files consistent across multiple devices
2. **Conflict Resolution**: Handle simultaneous edits to the same file
3. **Data Deduplication**: Avoid storing duplicate content
4. **Scalability**: Handle billions of files and petabytes of data
5. **Consistency**: Balance between performance and data consistency

### Cloud Storage Fundamentals

#### File vs Object Storage
- **File Storage**: Hierarchical structure (folders/files)
- **Object Storage**: Flat namespace with metadata
- **Dropbox Approach**: File interface with object storage backend

#### Synchronization Models

##### Client-Server Sync
```
Client A → Server → Client B
- Centralized conflict resolution
- Server is source of truth
- Simpler consistency model
```

##### Peer-to-Peer Sync
```
Client A ↔ Client B
- Distributed conflict resolution
- No single point of failure
- Complex consistency model
```

#### Consistency Models
- **Strong Consistency**: All clients see same data immediately
- **Eventual Consistency**: Clients eventually see same data
- **Causal Consistency**: Preserve cause-effect relationships

### File Chunking Strategy

#### Why Chunking?
1. **Resume Uploads**: Continue from where it left off
2. **Parallel Transfer**: Upload/download multiple chunks simultaneously
3. **Deduplication**: Share common chunks between files
4. **Delta Sync**: Only transfer changed chunks

#### Chunking Algorithms

##### Fixed-Size Chunking
```python
def fixed_chunk(file_data, chunk_size=4*1024*1024):  # 4MB chunks
    chunks = []
    for i in range(0, len(file_data), chunk_size):
        chunk = file_data[i:i+chunk_size]
        chunk_hash = sha256(chunk).hexdigest()
        chunks.append({'data': chunk, 'hash': chunk_hash})
    return chunks
```

##### Content-Defined Chunking (Better for Deduplication)
```python
def content_defined_chunk(file_data):
    chunks = []
    start = 0
    
    for i in range(len(file_data)):
        # Rolling hash to find chunk boundaries
        if rolling_hash(file_data[start:i]) % 8192 == 0:
            chunk = file_data[start:i]
            chunk_hash = sha256(chunk).hexdigest()
            chunks.append({'data': chunk, 'hash': chunk_hash})
            start = i
    
    return chunks
```

## Table of Contents
1. [System Overview](#system-overview)
2. [High-Level Design (HLD)](#high-level-design-hld)
3. [Low-Level Design (LLD)](#low-level-design-lld)
4. [Functional Requirements](#functional-requirements)
5. [Non-Functional Requirements](#non-functional-requirements)
6. [Deep-Dive Scenarios](#deep-dive-scenarios)
7. [Architecture Components](#architecture-components)
8. [Database Design](#database-design)
9. [API Design](#api-design)
10. [Implementation](#implementation)

## System Overview

A distributed cloud storage system that allows users to store, sync, and share files across multiple devices with high availability, durability, and security.

### Key Features
- File upload/download
- Multi-device synchronization
- File sharing and permissions
- Version control and history
- Real-time notifications
- Conflict resolution
- Data deduplication
- Thumbnail generation

## High-Level Design (HLD)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Load Balancer                            │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                 API Gateway                                     │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│ Auth Service │ │File API │ │ Sync Service│
└──────────────┘ └─────────┘ └─────────────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Message Queue (Kafka)                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    │                 │                 │
┌───▼────┐ ┌─────────▼──────┐ ┌────────▼────────┐
│Metadata│ │  File Storage  │ │ Notification    │
│Database│ │   (S3/HDFS)    │ │   Service       │
└────────┘ └────────────────┘ └─────────────────┘
    │
┌───▼────┐
│ Cache  │
│(Redis) │
└────────┘
```

## Low-Level Design (LLD)

### Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Client Layer                               │
├─────────────────────────────────────────────────────────────────┤
│  Web Client  │  Mobile App  │  Desktop Client  │  API Client   │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼─────────────────────────────────────┐
│                    API Gateway                                   │
├─────────────────────────────────────────────────────────────────┤
│ • Rate Limiting  • Authentication  • Request Routing           │
└─────────────────────────────▼─────────────────────────────────────┘
                              │
┌─────────────────────────────▼─────────────────────────────────────┐
│                   Microservices Layer                           │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│  User Service   │  File Service   │  Sync Service   │Auth Service│
├─────────────────┼─────────────────┼─────────────────┼───────────┤
│• User Management│• Upload/Download│• Device Sync    │• JWT Auth │
│• Permissions    │• Metadata Mgmt  │• Conflict Res   │• OAuth    │
│• Sharing        │• Versioning     │• Real-time Sync │• Sessions │
└─────────────────┴─────────────────┴─────────────────┴───────────┘
                              │
┌─────────────────────────────▼─────────────────────────────────────┐
│                     Data Layer                                   │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│   PostgreSQL    │     Redis       │      S3         │  Kafka    │
├─────────────────┼─────────────────┼─────────────────┼───────────┤
│• User Data      │• Session Cache  │• File Storage   │• Events   │
│• File Metadata  │• File Cache     │• Thumbnails     │• Sync Msgs│
│• Permissions    │• Sync State     │• Backups        │• Notifs   │
└─────────────────┴─────────────────┴─────────────────┴───────────┘
```

## Functional Requirements

### 1. File Upload/Download
- Chunked upload for large files
- Resume capability
- Progress tracking
- Multiple file formats support

### 2. Multi-device Synchronization
- Real-time sync across devices
- Offline capability
- Conflict resolution
- Delta sync for efficiency

### 3. File Sharing
- Share files/folders with users
- Permission levels (read, write, admin)
- Public links with expiration
- Corporate sharing policies

### 4. Version Control
- File history tracking
- Version comparison
- Rollback capability
- Storage optimization

### 5. File Operations
- Create, rename, delete files/folders
- Move operations
- Bulk operations
- Atomic transactions

## Non-Functional Requirements

### 1. High Availability (99.99%)
- Multi-region deployment
- Auto-failover mechanisms
- Circuit breakers
- Health monitoring

### 2. Durability
- 99.999999999% (11 9's) durability
- Cross-region replication
- Regular backups
- Data integrity checks

### 3. Scale Estimation

#### Back-of-the-envelope Calculations:

**Assumptions:**
- 500M users
- 100 files per user on average
- Average file size: 1MB
- 20% daily active users
- Each user uploads 2 files per day

**Storage:**
- Total files: 500M × 100 = 50B files
- Total storage: 50B × 1MB = 50PB
- With 3x replication: 150PB
- With versioning (2x): 300PB

**Bandwidth:**
- Daily uploads: 500M × 0.2 × 2 = 200M files/day
- Upload bandwidth: 200M × 1MB / 86400s = 2.3GB/s
- Download (3x upload): 7GB/s
- Peak (3x average): 21GB/s

**QPS:**
- File operations: 200M / 86400 = 2,315 QPS
- Metadata operations: 10x = 23,150 QPS
- Peak: 70,000 QPS

### 4. Consistency
- Strong consistency for metadata
- Eventual consistency for file content
- Conflict-free replicated data types (CRDTs)

### 5. Security
- End-to-end encryption
- Data at rest encryption
- Access control lists (ACL)
- Audit logging

### Data Deduplication Deep Dive

#### The Deduplication Problem
```
Scenario: 1000 users upload the same 100MB video
Without deduplication: 1000 × 100MB = 100GB storage
With deduplication: 1 × 100MB = 100MB storage
Savings: 99.9% storage reduction
```

#### Hash-Based Deduplication Implementation
```java
@Service
public class DeduplicationService {
    
    @Autowired
    private ChunkRepository chunkRepository;
    
    public List<ChunkInfo> deduplicateFile(byte[] fileData) {
        List<ChunkInfo> chunks = new ArrayList<>();
        
        // Split file into chunks
        List<byte[]> fileChunks = chunkFile(fileData);
        
        for (byte[] chunkData : fileChunks) {
            String chunkHash = calculateSHA256(chunkData);
            
            // Check if chunk already exists
            Optional<Chunk> existingChunk = chunkRepository.findByHash(chunkHash);
            
            if (existingChunk.isPresent()) {
                // Increment reference count
                existingChunk.get().incrementRefCount();
                chunks.add(new ChunkInfo(chunkHash, existingChunk.get().getStoragePath()));
            } else {
                // Store new chunk
                String storagePath = storeChunk(chunkData, chunkHash);
                Chunk newChunk = new Chunk(chunkHash, storagePath, 1);
                chunkRepository.save(newChunk);
                chunks.add(new ChunkInfo(chunkHash, storagePath));
            }
        }
        
        return chunks;
    }
}
```

### Conflict Resolution Strategies

#### Types of Conflicts
1. **Edit-Edit Conflict**: Two users edit same file simultaneously
2. **Edit-Delete Conflict**: One user edits, another deletes
3. **Move-Move Conflict**: File moved to different locations
4. **Name Conflict**: Two files with same name in folder

#### Conflict Resolution Algorithms

##### Last Writer Wins (LWW)
```java
public FileVersion resolveConflict(FileVersion version1, FileVersion version2) {
    if (version1.getTimestamp().isAfter(version2.getTimestamp())) {
        return version1;
    } else {
        return version2;
    }
}
```

##### Operational Transformation (for text files)
```java
public class OperationalTransform {
    public Operation transform(Operation op1, Operation op2) {
        // Transform op1 against op2
        if (op1.getPosition() <= op2.getPosition()) {
            return op1; // No transformation needed
        } else {
            // Adjust position based on op2's effect
            return new Operation(
                op1.getType(),
                op1.getPosition() + op2.getLength(),
                op1.getContent()
            );
        }
    }
}
```

##### Vector Clocks (for causality)
```java
public class VectorClock {
    private Map<String, Integer> clock = new HashMap<>();
    
    public void increment(String nodeId) {
        clock.put(nodeId, clock.getOrDefault(nodeId, 0) + 1);
    }
    
    public boolean happensBefore(VectorClock other) {
        boolean strictlyLess = false;
        
        for (String nodeId : getAllNodes()) {
            int thisValue = clock.getOrDefault(nodeId, 0);
            int otherValue = other.clock.getOrDefault(nodeId, 0);
            
            if (thisValue > otherValue) {
                return false; // Not happens-before
            }
            if (thisValue < otherValue) {
                strictlyLess = true;
            }
        }
        
        return strictlyLess;
    }
}
```

## Deep-Dive Scenarios

### 1. Conflict Resolution
```
User A (Device 1): Edits file.txt → Version 2a
User A (Device 2): Edits file.txt → Version 2b (offline)

Resolution Strategy:
1. Last-writer-wins with timestamp
2. Create conflict copies
3. User manual resolution
4. Operational transformation for text files
```

### 2. Data Deduplication
```
Hash-based deduplication:
1. Calculate SHA-256 of file chunks
2. Store unique chunks only
3. Reference counting for shared chunks
4. Garbage collection for unused chunks
```

### 3. File Chunking
```
Chunking Strategy:
- Fixed size chunks (4MB)
- Content-defined chunking for better dedup
- Delta sync using rsync algorithm
- Parallel upload/download of chunks
```

### 4. Metadata Management
```
Separate metadata storage:
- File attributes in database
- Content in object storage
- Metadata caching
- Eventual consistency handling
```

### 5. Real-time Notifications
```
WebSocket connections for real-time updates
Push notifications for mobile
Long polling fallback
Event-driven architecture with Kafka
```

### 6. Thumbnail Generation
```
Async thumbnail generation:
1. Upload triggers thumbnail job
2. Queue processing with workers
3. Multiple resolution thumbnails
4. CDN distribution
```

### 7. Corporate Permissions
```
Hierarchical permission model:
- Organization → Team → Project → File
- Role-based access control (RBAC)
- Inheritance and overrides
- Audit trails
```

## Database Design

### User Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    storage_quota BIGINT DEFAULT 15000000000, -- 15GB
    storage_used BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### File Metadata Table
```sql
CREATE TABLE files (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    path TEXT NOT NULL,
    size BIGINT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    mime_type VARCHAR(100),
    owner_id UUID REFERENCES users(id),
    parent_folder_id UUID REFERENCES files(id),
    version INTEGER DEFAULT 1,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_owner_path (owner_id, path),
    INDEX idx_content_hash (content_hash)
);
```

### File Chunks Table
```sql
CREATE TABLE file_chunks (
    id UUID PRIMARY KEY,
    file_id UUID REFERENCES files(id),
    chunk_index INTEGER NOT NULL,
    chunk_hash VARCHAR(64) NOT NULL,
    chunk_size INTEGER NOT NULL,
    storage_path TEXT NOT NULL,
    UNIQUE(file_id, chunk_index)
);
```

### Sharing Table
```sql
CREATE TABLE file_shares (
    id UUID PRIMARY KEY,
    file_id UUID REFERENCES files(id),
    shared_with_user_id UUID REFERENCES users(id),
    permission_level VARCHAR(20) NOT NULL, -- read, write, admin
    shared_by_user_id UUID REFERENCES users(id),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## API Design

### File Operations
```
POST /api/v1/files/upload
GET /api/v1/files/{fileId}/download
PUT /api/v1/files/{fileId}
DELETE /api/v1/files/{fileId}
GET /api/v1/files/{fileId}/versions
POST /api/v1/files/{fileId}/share
```

### Sync Operations
```
GET /api/v1/sync/changes?since={timestamp}
POST /api/v1/sync/conflicts/resolve
WebSocket: /ws/sync/{userId}
```

### User Operations
```
POST /api/v1/auth/login
POST /api/v1/auth/register
GET /api/v1/users/profile
PUT /api/v1/users/profile
```

## Implementation

The implementation includes:
1. Core domain models
2. Service layer with business logic
3. Repository pattern for data access
4. REST API controllers
5. WebSocket handlers for real-time sync
6. Background job processors
7. Configuration and security setup

See the `src/main/java/org/sudhir512kj/` package for complete implementation.