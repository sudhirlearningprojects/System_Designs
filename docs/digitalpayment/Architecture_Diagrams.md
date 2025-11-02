# Digital Payment Platform - Architecture Diagrams

## High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        MA[Mobile App]
        WA[Web App]
    end
    
    subgraph "API Gateway Layer"
        AG[API Gateway<br/>Authentication<br/>Rate Limiting<br/>Load Balancing]
    end
    
    subgraph "Microservices Layer"
        PS[Payment Service]
        LS[Ledger Service]
        FDS[Fraud Detection Service]
        US[User Service]
        THS[Transaction History Service]
    end
    
    subgraph "External Services"
        UPI[UPI Gateway]
        CARD[Card Gateway]
        NB[Net Banking Gateway]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Transactions)]
        RD[(Redis<br/>Cache)]
        CS[(Cassandra<br/>Logs)]
    end
    
    subgraph "Message Queue"
        KF[Apache Kafka]
    end
    
    MA --> AG
    WA --> AG
    AG --> PS
    AG --> US
    AG --> THS
    
    PS --> LS
    PS --> FDS
    PS --> UPI
    PS --> CARD
    PS --> NB
    
    PS --> PG
    LS --> PG
    FDS --> RD
    THS --> CS
    
    PS --> KF
    KF --> FDS
    KF --> THS
```

## Payment Flow Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Payment_Service
    participant Fraud_Detection
    participant Ledger_Service
    participant PSP_Gateway
    participant Database
    participant Redis
    
    Client->>API_Gateway: Initiate Payment Request
    API_Gateway->>Payment_Service: Route Request
    
    Payment_Service->>Redis: Check Idempotency Key
    Redis-->>Payment_Service: Key Status
    
    Payment_Service->>Fraud_Detection: Validate Transaction
    Fraud_Detection->>Redis: Check Limits & Patterns
    Redis-->>Fraud_Detection: User Activity Data
    Fraud_Detection-->>Payment_Service: Validation Result
    
    alt Fraud Check Passed
        Payment_Service->>Database: Create Transaction Record
        Database-->>Payment_Service: Transaction Created
        
        alt Wallet Payment
            Payment_Service->>Ledger_Service: Transfer Funds
            Ledger_Service->>Database: Atomic Balance Update
            Database-->>Ledger_Service: Update Successful
            Ledger_Service-->>Payment_Service: Transfer Complete
        else External PSP
            Payment_Service->>PSP_Gateway: Process Payment
            PSP_Gateway-->>Payment_Service: PSP Response
        end
        
        Payment_Service->>Database: Update Transaction Status
        Payment_Service->>Redis: Cache Result (Idempotency)
        Payment_Service-->>Client: Payment Response
    else Fraud Check Failed
        Payment_Service-->>Client: Transaction Blocked
    end
```

## Database Schema Design

```mermaid
erDiagram
    USER {
        string user_id PK
        string name
        string phone_number UK
        string email UK
        string pin
        enum status
        timestamp created_at
    }
    
    ACCOUNT {
        string account_id PK
        string user_id FK
        string bank_name
        string account_number
        string ifsc_code
        decimal balance
        enum type
        enum status
        long version
        timestamp created_at
    }
    
    WALLET {
        string wallet_id PK
        string user_id FK
        decimal balance
        long version
        timestamp last_updated
    }
    
    TRANSACTION {
        string transaction_id PK
        string sender_id
        string receiver_id
        decimal amount
        string currency
        enum type
        enum status
        enum payment_method
        string psp_transaction_id
        string description
        string idempotency_key UK
        timestamp created_at
        timestamp updated_at
    }
    
    USER ||--o{ ACCOUNT : has
    USER ||--|| WALLET : owns
    USER ||--o{ TRANSACTION : sends
    USER ||--o{ TRANSACTION : receives
```

## Concurrency Control Architecture

```mermaid
graph TB
    subgraph "Concurrent Requests"
        R1[Request 1<br/>Transfer ₹100]
        R2[Request 2<br/>Transfer ₹200]
        R3[Request 3<br/>Check Balance]
    end
    
    subgraph "Ledger Service"
        LS[Ledger Service<br/>@Transactional<br/>SERIALIZABLE]
    end
    
    subgraph "Database Layer"
        PL[Pessimistic Lock<br/>SELECT FOR UPDATE]
        WR[Wallet Record<br/>Balance: ₹500<br/>Version: 1]
    end
    
    R1 --> LS
    R2 --> LS
    R3 --> LS
    
    LS --> PL
    PL --> WR
    
    note1[Request 1 acquires lock first]
    note2[Request 2 waits for lock release]
    note3[Request 3 reads after updates]
```

## Fraud Detection Flow

```mermaid
graph TD
    TR[Transaction Request] --> FD[Fraud Detection Service]
    
    FD --> DL{Daily Limit<br/>Check}
    FD --> FL{Frequency<br/>Check}
    FD --> AL{Amount<br/>Threshold}
    
    DL -->|< ₹1,00,000| DL_OK[✓ Pass]
    DL -->|≥ ₹1,00,000| DL_FAIL[✗ Block]
    
    FL -->|< 50/hour| FL_OK[✓ Pass]
    FL -->|≥ 50/hour| FL_FAIL[✗ Block]
    
    AL -->|< ₹50,000| AL_OK[✓ Pass]
    AL -->|≥ ₹50,000| AL_FLAG[⚠ Flag]
    
    DL_OK --> ALLOW[Allow Transaction]
    FL_OK --> ALLOW
    AL_OK --> ALLOW
    AL_FLAG --> ALLOW
    
    DL_FAIL --> BLOCK[Block Transaction]
    FL_FAIL --> BLOCK
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[NGINX/AWS ALB]
    end
    
    subgraph "Application Tier (Kubernetes)"
        subgraph "Payment Service Pods"
            PS1[Payment Service 1]
            PS2[Payment Service 2]
            PS3[Payment Service 3]
        end
        
        subgraph "Ledger Service Pods"
            LS1[Ledger Service 1]
            LS2[Ledger Service 2]
        end
        
        subgraph "Fraud Detection Pods"
            FD1[Fraud Detection 1]
            FD2[Fraud Detection 2]
        end
    end
    
    subgraph "Data Tier"
        subgraph "PostgreSQL Cluster"
            PG_M[(Primary)]
            PG_R1[(Replica 1)]
            PG_R2[(Replica 2)]
        end
        
        subgraph "Redis Cluster"
            RD_M[(Redis Master)]
            RD_S1[(Redis Slave 1)]
            RD_S2[(Redis Slave 2)]
        end
    end
    
    LB --> PS1
    LB --> PS2
    LB --> PS3
    
    PS1 --> LS1
    PS2 --> LS2
    PS3 --> FD1
    
    LS1 --> PG_M
    LS2 --> PG_M
    FD1 --> RD_M
    FD2 --> RD_M
    
    PG_M --> PG_R1
    PG_M --> PG_R2
    RD_M --> RD_S1
    RD_M --> RD_S2
```

These diagrams illustrate the comprehensive architecture of the digital payment platform, showing the flow of data, security measures, and scalability considerations.