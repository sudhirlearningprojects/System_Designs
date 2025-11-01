# Cloud Storage System - Architecture Diagrams

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

## 3. File Synchronization Architecture

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