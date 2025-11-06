# Ticket Booking Platform - Architecture Diagrams

## 1. System Overview

```mermaid
graph TB
    subgraph "Client Layer"
        MA[Mobile App]
        WC[Web Client]
        AP[Admin Panel]
    end
    
    subgraph "API Gateway Layer"
        AG[API Gateway<br/>Rate Limiting<br/>Authentication<br/>Load Balancing]
    end
    
    subgraph "Microservices Layer"
        ES[Event Service]
        BS[Booking Service]
        US[User Service]
        PS[Payment Service]
        NS[Notification Service]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Events, Users<br/>Bookings)]
        RD[(Redis<br/>Inventory Cache<br/>Session Store)]
        ES_DB[(Elasticsearch<br/>Search Index)]
    end
    
    subgraph "External Services"
        PGW[Payment Gateway<br/>Stripe/PayPal]
        SMS[SMS Service]
        EMAIL[Email Service]
    end
    
    MA --> AG
    WC --> AG
    AP --> AG
    
    AG --> ES
    AG --> BS
    AG --> US
    
    BS --> PS
    BS --> NS
    
    ES --> PG
    ES --> RD
    ES --> ES_DB
    
    BS --> PG
    BS --> RD
    
    US --> PG
    US --> RD
    
    PS --> PGW
    NS --> SMS
    NS --> EMAIL
```

## 2. Booking Flow Architecture

```mermaid
sequenceDiagram
    participant U as User
    participant AG as API Gateway
    participant BS as Booking Service
    participant IS as Inventory Service
    participant PS as Payment Service
    participant R as Redis
    participant DB as PostgreSQL
    
    U->>AG: Search Events
    AG->>BS: GET /events/search
    BS->>R: Check cache
    R-->>BS: Cached results
    BS-->>AG: Event list
    AG-->>U: Display events
    
    U->>AG: Hold tickets
    AG->>BS: POST /bookings/hold
    BS->>IS: holdTickets(typeId, qty)
    IS->>R: DECR inventory:typeId qty
    R-->>IS: New count
    IS->>R: SET hold:holdId qty TTL 10min
    IS-->>BS: Hold successful
    BS->>DB: INSERT booking (HELD)
    BS-->>AG: Booking response
    AG-->>U: Hold confirmation
    
    U->>AG: Make payment
    AG->>PS: Process payment
    PS->>External: Payment gateway
    External-->>PS: Payment success
    PS-->>AG: Payment confirmed
    
    AG->>BS: POST /bookings/confirm
    BS->>IS: confirmHold(holdId)
    IS->>DB: UPDATE ticket_types
    IS->>R: DEL hold:holdId
    BS->>DB: UPDATE booking (CONFIRMED)
    BS-->>AG: Booking confirmed
    AG-->>U: Success notification
```

## 3. Inventory Management Architecture

```mermaid
graph TB
    subgraph "Inventory Management"
        subgraph "Redis Layer (Real-time)"
            INV[inventory:ticket_type_id<br/>Available Count]
            HOLD[hold:hold_id<br/>Quantity + TTL]
        end
        
        subgraph "Database Layer (Persistent)"
            TT[ticket_types table<br/>available_quantity]
            BK[bookings table<br/>status, quantity]
        end
        
        subgraph "Background Jobs"
            EXP[Expired Hold Cleanup<br/>Every 1 minute]
            SYNC[DB Sync Job<br/>Every 5 minutes]
        end
    end
    
    subgraph "Operations"
        HOLD_OP[Hold Tickets<br/>DECR + SET TTL]
        CONF_OP[Confirm Booking<br/>UPDATE DB + DEL hold]
        REL_OP[Release Hold<br/>INCR + DEL hold]
    end
    
    HOLD_OP --> INV
    HOLD_OP --> HOLD
    
    CONF_OP --> TT
    CONF_OP --> BK
    CONF_OP --> HOLD
    
    REL_OP --> INV
    REL_OP --> HOLD
    
    EXP --> HOLD
    EXP --> INV
    
    SYNC --> INV
    SYNC --> TT
```

## 4. Database Schema Relationships

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK
        varchar name
        varchar password_hash
        varchar phone_number
        timestamp created_at
    }
    
    EVENTS {
        bigint id PK
        varchar name
        text description
        varchar venue
        varchar city
        varchar genre
        timestamp event_date
        timestamp created_at
    }
    
    TICKET_TYPES {
        bigint id PK
        bigint event_id FK
        varchar name
        decimal price
        integer total_quantity
        integer available_quantity
    }
    
    BOOKINGS {
        bigint id PK
        bigint user_id FK
        bigint event_id FK
        bigint ticket_type_id FK
        integer quantity
        decimal total_amount
        varchar status
        timestamp hold_expires_at
        timestamp created_at
        varchar payment_id
    }
    
    USERS ||--o{ BOOKINGS : "makes"
    EVENTS ||--o{ TICKET_TYPES : "has"
    EVENTS ||--o{ BOOKINGS : "for"
    TICKET_TYPES ||--o{ BOOKINGS : "books"
```

## 5. Microservices Communication

```mermaid
graph LR
    subgraph "Event Service"
        ES_API[REST API]
        ES_CACHE[Event Cache]
        ES_SEARCH[Search Logic]
    end
    
    subgraph "Booking Service"
        BS_API[REST API]
        BS_HOLD[Hold Management]
        BS_CONFIRM[Booking Confirmation]
    end
    
    subgraph "User Service"
        US_API[REST API]
        US_AUTH[Authentication]
        US_PROFILE[Profile Management]
    end
    
    subgraph "Payment Service"
        PS_API[REST API]
        PS_GATEWAY[Gateway Integration]
        PS_CALLBACK[Callback Handler]
    end
    
    ES_API -.->|Event Data| BS_API
    BS_API -.->|Payment Request| PS_API
    US_API -.->|User Validation| BS_API
    PS_API -.->|Payment Status| BS_API
```

## 6. Caching Strategy

```mermaid
graph TB
    subgraph "Cache Layers"
        subgraph "Application Cache"
            AC[Spring Cache<br/>@Cacheable]
        end
        
        subgraph "Redis Cache"
            RC[Distributed Cache<br/>Events, Users]
            INV_CACHE[Inventory Cache<br/>Real-time counts]
            SESS[Session Store<br/>User sessions]
        end
        
        subgraph "Database"
            DB[(PostgreSQL<br/>Source of Truth)]
        end
    end
    
    subgraph "Cache Patterns"
        CT[Cache-Through<br/>Events, Users]
        CA[Cache-Aside<br/>Search results]
        WB[Write-Behind<br/>Inventory sync]
    end
    
    AC --> RC
    RC --> DB
    
    CT --> RC
    CA --> RC
    WB --> INV_CACHE
```

## 7. Load Balancing and Scaling

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Application Load Balancer<br/>Health Checks<br/>Auto Scaling]
    end
    
    subgraph "API Gateway Cluster"
        AG1[API Gateway 1]
        AG2[API Gateway 2]
        AG3[API Gateway 3]
    end
    
    subgraph "Service Instances"
        subgraph "Event Service"
            ES1[Instance 1]
            ES2[Instance 2]
        end
        
        subgraph "Booking Service"
            BS1[Instance 1]
            BS2[Instance 2]
            BS3[Instance 3]
        end
        
        subgraph "User Service"
            US1[Instance 1]
            US2[Instance 2]
        end
    end
    
    LB --> AG1
    LB --> AG2
    LB --> AG3
    
    AG1 --> ES1
    AG1 --> BS1
    AG1 --> US1
    
    AG2 --> ES2
    AG2 --> BS2
    AG2 --> US2
    
    AG3 --> ES1
    AG3 --> BS3
    AG3 --> US1
```

## 8. Security Architecture

```mermaid
graph TB
    subgraph "Security Layers"
        subgraph "Network Security"
            WAF[Web Application Firewall]
            DDoS[DDoS Protection]
        end
        
        subgraph "API Security"
            RL[Rate Limiting]
            JWT[JWT Authentication]
            RBAC[Role-Based Access Control]
        end
        
        subgraph "Data Security"
            ENC[Encryption at Rest]
            TLS[TLS 1.3 in Transit]
            HASH[Password Hashing]
        end
        
        subgraph "Payment Security"
            PCI[PCI DSS Compliance]
            TOK[Payment Tokenization]
            VAULT[Secure Key Vault]
        end
    end
    
    WAF --> RL
    DDoS --> RL
    RL --> JWT
    JWT --> RBAC
    
    RBAC --> ENC
    RBAC --> TLS
    RBAC --> HASH
    
    TLS --> PCI
    ENC --> TOK
    HASH --> VAULT
```

## 9. Monitoring and Observability

```mermaid
graph TB
    subgraph "Application Metrics"
        AM[Business Metrics<br/>Booking Rate<br/>Revenue<br/>Conversion]
        TM[Technical Metrics<br/>Response Time<br/>Error Rate<br/>Throughput]
    end
    
    subgraph "Infrastructure Metrics"
        CPU[CPU Usage]
        MEM[Memory Usage]
        DISK[Disk I/O]
        NET[Network I/O]
    end
    
    subgraph "Monitoring Stack"
        PROM[Prometheus<br/>Metrics Collection]
        GRAF[Grafana<br/>Dashboards]
        ALERT[AlertManager<br/>Notifications]
    end
    
    subgraph "Logging Stack"
        LOG[Application Logs]
        ELK[ELK Stack<br/>Centralized Logging]
        TRACE[Distributed Tracing<br/>Jaeger]
    end
    
    AM --> PROM
    TM --> PROM
    CPU --> PROM
    MEM --> PROM
    DISK --> PROM
    NET --> PROM
    
    PROM --> GRAF
    PROM --> ALERT
    
    LOG --> ELK
    ELK --> TRACE
```

## 10. Deployment Architecture

```mermaid
graph TB
    subgraph "Production Environment"
        subgraph "Kubernetes Cluster"
            subgraph "Namespace: ticketbooking"
                POD1[Event Service Pods]
                POD2[Booking Service Pods]
                POD3[User Service Pods]
                POD4[Payment Service Pods]
            end
            
            subgraph "Ingress"
                ING[Nginx Ingress Controller]
            end
            
            subgraph "Storage"
                PV[Persistent Volumes]
                SC[Storage Classes]
            end
        end
        
        subgraph "Managed Services"
            RDS[Amazon RDS<br/>PostgreSQL]
            REDIS[Amazon ElastiCache<br/>Redis]
            ALB[Application Load Balancer]
        end
    end
    
    subgraph "CI/CD Pipeline"
        GIT[Git Repository]
        BUILD[Build & Test]
        DEPLOY[Deploy to K8s]
    end
    
    ING --> POD1
    ING --> POD2
    ING --> POD3
    ING --> POD4
    
    POD1 --> RDS
    POD2 --> RDS
    POD2 --> REDIS
    POD3 --> RDS
    
    ALB --> ING
    
    GIT --> BUILD
    BUILD --> DEPLOY
    DEPLOY --> POD1
    DEPLOY --> POD2
    DEPLOY --> POD3
    DEPLOY --> POD4
```

These architecture diagrams provide a comprehensive visual representation of the ticket booking platform's design, covering system overview, data flow, microservices communication, caching strategies, security layers, and deployment architecture.