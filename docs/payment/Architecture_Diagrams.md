# Payment Service - Architecture Diagrams

## 1. High-Level System Architecture

![High-Level System Architecture](./images/high-level-system-architecture.png)

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Application]
        MOBILE[Mobile App]
        API_CLIENT[API Client]
    end
    
    subgraph "API Gateway"
        GATEWAY[API Gateway]
        RATE_LIMIT[Rate Limiter]
        AUTH[Authentication]
    end
    
    subgraph "Payment Service Cluster"
        PS1[Payment Service 1]
        PS2[Payment Service 2]
        PS3[Payment Service N]
    end
    
    subgraph "Core Services"
        IDEMPOTENCY[Idempotency Service]
        PROCESSOR[Payment Processor Service]
        TRANSACTION_MGR[Transaction Manager]
        RETRY_ENGINE[Retry Engine]
        AUDIT[Audit Service]
    end
    
    subgraph "Message Queue"
        KAFKA[Apache Kafka]
        RETRY_QUEUE[Retry Queue]
        DLQ[Dead Letter Queue]
        AUDIT_QUEUE[Audit Events]
    end
    
    subgraph "External Services"
        STRIPE[Stripe API]
        PAYPAL[PayPal API]
        SQUARE[Square API]
    end
    
    subgraph "Data Layer"
        POSTGRES[PostgreSQL Cluster]
        REDIS[Redis Cache]
        MONITORING[Monitoring & Metrics]
    end
    
    WEB --> GATEWAY
    MOBILE --> GATEWAY
    API_CLIENT --> GATEWAY
    
    GATEWAY --> RATE_LIMIT
    RATE_LIMIT --> AUTH
    AUTH --> PS1
    AUTH --> PS2
    AUTH --> PS3
    
    PS1 --> IDEMPOTENCY
    PS1 --> PROCESSOR
    PS1 --> TRANSACTION_MGR
    
    PROCESSOR --> STRIPE
    PROCESSOR --> PAYPAL
    PROCESSOR --> SQUARE
    
    TRANSACTION_MGR --> RETRY_ENGINE
    RETRY_ENGINE --> KAFKA
    
    KAFKA --> RETRY_QUEUE
    KAFKA --> DLQ
    KAFKA --> AUDIT_QUEUE
    
    IDEMPOTENCY --> REDIS
    PS1 --> POSTGRES
    AUDIT --> KAFKA
```

## 2. Payment Processing Flow

![Payment Processing Flow](./images/payment-processing-flow.png)

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant PS as Payment Service
    participant IDS as Idempotency Service
    participant PP as Payment Processor
    participant DB as Database
    participant Kafka
    participant Audit as Audit Service
    
    Client->>Gateway: POST /payment/process + Idempotency-Key
    Gateway->>PS: Forward Request
    
    PS->>IDS: Check Idempotency Key
    alt Key Exists
        IDS-->>PS: Return Cached Response
        PS-->>Client: Return Cached Result
    else New Key
        PS->>DB: Create Transaction (PENDING)
        PS->>PP: Process Payment
        
        alt Success
            PP->>External: Call Payment API
            External-->>PP: Success Response
            PP-->>PS: Processor Transaction ID
            PS->>DB: Update Status (COMPLETED)
            PS->>IDS: Cache Response
            PS->>Kafka: Send Success Event
            Kafka->>Audit: Log Success
            PS-->>Client: Success Response
        else Failure
            PP->>External: Call Payment API
            External-->>PP: Error Response
            PP-->>PS: Error
            PS->>DB: Update Status (FAILED)
            PS->>Kafka: Send Retry Event
            PS->>Kafka: Send Failure Event
            Kafka->>Audit: Log Failure
            PS-->>Client: Error Response
        end
    end
```

## 3. Idempotency Management

![Idempotency Management](./images/idempotency-management.png)

```mermaid
flowchart TD
    A[Payment Request] --> B{Idempotency Key Valid?}
    B -->|No| C[Return 400 Bad Request]
    B -->|Yes| D[Check Redis Cache]
    D --> E{Key Exists in Redis?}
    E -->|Yes| F[Return Cached Response]
    E -->|No| G[Check Database Cache]
    G --> H{Key Exists in DB?}
    H -->|Yes| I[Load to Redis & Return]
    H -->|No| J[Process New Payment]
    J --> K[Save to Database]
    K --> L[Cache in Redis]
    L --> M[Return Response]
    
    style F fill:#90EE90
    style I fill:#90EE90
    style M fill:#90EE90
    style C fill:#FFB6C1
```

## 4. Circuit Breaker Pattern

![Circuit Breaker Pattern](./images/circuit-breaker-pattern.png)

```mermaid
stateDiagram-v2
    [*] --> Closed
    Closed --> Open: Failure Rate > Threshold
    Open --> HalfOpen: Wait Duration Elapsed
    HalfOpen --> Closed: Success Rate > Threshold
    HalfOpen --> Open: Failure Rate > Threshold
    
    Closed: Normal Operation\nRequests Pass Through
    Open: Fail Fast\nRequests Rejected
    HalfOpen: Test Mode\nLimited Requests
```

## 5. Retry Mechanism with Exponential Backoff

![Retry Mechanism with Exponential Backoff](./images/retry-mechanism-exponential-backoff.png)

```mermaid
graph TD
    A[Payment Fails] --> B[Add to Retry Queue]
    B --> C[Calculate Next Retry Time]
    C --> D[Exponential Backoff + Jitter]
    D --> E[Wait for Retry Time]
    E --> F[Retry Payment]
    F --> G{Success?}
    G -->|Yes| H[Mark Complete]
    G -->|No| I{Max Retries Reached?}
    I -->|No| J[Increment Retry Count]
    J --> C
    I -->|Yes| K[Move to Dead Letter Queue]
    
    style H fill:#90EE90
    style K fill:#FFB6C1
```

## 6. Database Schema Relationships

![Database Schema Relationships](./images/database-schema-relationships.png)

```mermaid
erDiagram
    PAYMENT_TRANSACTIONS ||--o{ RETRY_QUEUE : has
    PAYMENT_TRANSACTIONS ||--o{ IDEMPOTENCY_CACHE : cached_by
    
    PAYMENT_TRANSACTIONS {
        uuid id PK
        string idempotency_key UK
        uuid merchant_id
        uuid user_id
        decimal amount
        string currency
        enum status
        string payment_method
        string processor
        string processor_transaction_id
        text failure_reason
        timestamp created_at
        timestamp updated_at
        timestamp completed_at
    }
    
    IDEMPOTENCY_CACHE {
        string key PK
        uuid transaction_id FK
        text response_data
        timestamp created_at
        timestamp expires_at
    }
    
    RETRY_QUEUE {
        uuid id PK
        uuid transaction_id FK
        integer retry_count
        integer max_retries
        timestamp next_retry_at
        text error_message
        timestamp created_at
    }
```

## 7. Distributed Transaction Saga Pattern

![Distributed Transaction Saga Pattern](./images/distributed-transaction-saga-pattern.png)

```mermaid
sequenceDiagram
    participant Client
    participant PS as Payment Service
    participant WS as Wallet Service
    participant NS as Notification Service
    participant LS as Ledger Service
    
    Client->>PS: Process Payment
    PS->>WS: Reserve Funds
    WS-->>PS: Funds Reserved
    
    PS->>External: Process with Processor
    alt Success
        External-->>PS: Payment Successful
        PS->>LS: Record Transaction
        LS-->>PS: Transaction Recorded
        PS->>NS: Send Notification
        NS-->>PS: Notification Sent
        PS->>WS: Confirm Funds Transfer
        WS-->>PS: Transfer Confirmed
        PS-->>Client: Success Response
    else Failure
        External-->>PS: Payment Failed
        PS->>WS: Release Reserved Funds (Compensate)
        WS-->>PS: Funds Released
        PS->>NS: Send Failure Notification
        NS-->>PS: Notification Sent
        PS-->>Client: Failure Response
    end
```

## 8. Monitoring and Alerting Architecture

![Monitoring and Alerting Architecture](./images/monitoring-alerting-architecture.png)

```mermaid
graph TB
    subgraph "Application Metrics"
        APP_METRICS[Application Metrics]
        BUSINESS_METRICS[Business Metrics]
        ERROR_METRICS[Error Metrics]
    end
    
    subgraph "Infrastructure Metrics"
        CPU[CPU Usage]
        MEMORY[Memory Usage]
        DISK[Disk I/O]
        NETWORK[Network I/O]
    end
    
    subgraph "Database Metrics"
        DB_CONN[Connection Pool]
        DB_LATENCY[Query Latency]
        DB_THROUGHPUT[Throughput]
    end
    
    subgraph "External Service Metrics"
        EXT_LATENCY[API Latency]
        EXT_SUCCESS[Success Rate]
        EXT_ERRORS[Error Rate]
    end
    
    subgraph "Monitoring Stack"
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
        ALERTMANAGER[Alert Manager]
    end
    
    subgraph "Alerting Channels"
        SLACK[Slack]
        EMAIL[Email]
        PAGERDUTY[PagerDuty]
    end
    
    APP_METRICS --> PROMETHEUS
    BUSINESS_METRICS --> PROMETHEUS
    ERROR_METRICS --> PROMETHEUS
    CPU --> PROMETHEUS
    MEMORY --> PROMETHEUS
    DB_CONN --> PROMETHEUS
    EXT_LATENCY --> PROMETHEUS
    
    PROMETHEUS --> GRAFANA
    PROMETHEUS --> ALERTMANAGER
    
    ALERTMANAGER --> SLACK
    ALERTMANAGER --> EMAIL
    ALERTMANAGER --> PAGERDUTY
```

## 9. Security Architecture

![Security Architecture](./images/security-architecture.png)

```mermaid
graph TB
    subgraph "Client Security"
        TLS[TLS 1.3 Encryption]
        CLIENT_CERT[Client Certificates]
    end
    
    subgraph "API Security"
        JWT[JWT Authentication]
        OAUTH[OAuth 2.0]
        RATE_LIMIT[Rate Limiting]
        WAF[Web Application Firewall]
    end
    
    subgraph "Data Security"
        ENCRYPTION[Data Encryption at Rest]
        TOKENIZATION[Card Tokenization]
        PCI_DSS[PCI DSS Compliance]
    end
    
    subgraph "Network Security"
        VPC[Virtual Private Cloud]
        SECURITY_GROUPS[Security Groups]
        PRIVATE_SUBNETS[Private Subnets]
    end
    
    subgraph "Audit & Compliance"
        AUDIT_LOGS[Audit Logging]
        COMPLIANCE[Compliance Monitoring]
        FRAUD_DETECTION[Fraud Detection]
    end
    
    TLS --> JWT
    CLIENT_CERT --> OAUTH
    JWT --> RATE_LIMIT
    OAUTH --> WAF
    
    WAF --> ENCRYPTION
    RATE_LIMIT --> TOKENIZATION
    ENCRYPTION --> PCI_DSS
    
    PCI_DSS --> VPC
    TOKENIZATION --> SECURITY_GROUPS
    VPC --> PRIVATE_SUBNETS
    
    PRIVATE_SUBNETS --> AUDIT_LOGS
    SECURITY_GROUPS --> COMPLIANCE
    AUDIT_LOGS --> FRAUD_DETECTION
```

## 10. Scalability Architecture

![Scalability Architecture](./images/scalability-architecture.png)

```mermaid
graph TB
    subgraph "Load Balancing"
        ALB[Application Load Balancer]
        NLB[Network Load Balancer]
    end
    
    subgraph "Auto Scaling"
        ASG[Auto Scaling Group]
        HPA[Horizontal Pod Autoscaler]
    end
    
    subgraph "Database Scaling"
        MASTER[Master DB]
        REPLICA1[Read Replica 1]
        REPLICA2[Read Replica 2]
        SHARDING[Database Sharding]
    end
    
    subgraph "Cache Scaling"
        REDIS_CLUSTER[Redis Cluster]
        REDIS_SENTINEL[Redis Sentinel]
    end
    
    subgraph "Message Queue Scaling"
        KAFKA_CLUSTER[Kafka Cluster]
        PARTITIONING[Topic Partitioning]
    end
    
    ALB --> ASG
    NLB --> HPA
    
    ASG --> MASTER
    HPA --> REPLICA1
    ASG --> REPLICA2
    
    MASTER --> SHARDING
    REPLICA1 --> REDIS_CLUSTER
    REPLICA2 --> REDIS_SENTINEL
    
    REDIS_CLUSTER --> KAFKA_CLUSTER
    REDIS_SENTINEL --> PARTITIONING
```