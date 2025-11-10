# Rate Limiter - Complete Flow Diagram

## 1. High-Level System Flow

```mermaid
graph TB
    A[Client Request] --> B[Spring Boot Application]
    B --> C[AOP Interceptor]
    C --> D{@RateLimit Annotation?}
    D -->|No| E[Execute Method Normally]
    D -->|Yes| F[Rate Limit Check]
    F --> G{Rate Limit Exceeded?}
    G -->|No| H[Execute Method]
    G -->|Yes| I[Throw RateLimitExceededException]
    H --> J[Return Response]
    I --> K[HTTP 429 Response]
    
    style A fill:#e1f5fe
    style K fill:#ffebee
    style J fill:#e8f5e8
```

## 2. Detailed Rate Limiting Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Spring Boot App
    participant A as RateLimitAspect
    participant RS as RateLimitService
    participant R as Redis
    participant M as Your Method
    
    C->>S: HTTP Request
    S->>A: Method Call Intercepted
    A->>A: Check @RateLimit annotation
    A->>RS: checkRateLimit(request, joinPoint, annotation)
    
    RS->>RS: buildClientKey()
    Note over RS: Generate unique key:<br/>"user:john123:getProducts()"
    
    RS->>RS: Select Algorithm
    Note over RS: Choose: SlidingWindow,<br/>TokenBucket, etc.
    
    RS->>R: Check Rate Limit State
    R-->>RS: Current Count/Tokens
    
    alt Rate Limit OK
        RS-->>A: Allow (remaining: 5)
        A->>M: Execute Method
        M-->>A: Method Result
        A-->>S: Return Result
        S-->>C: HTTP 200 + Headers
    else Rate Limit Exceeded
        RS-->>A: Deny (retry after: 60s)
        A->>A: Throw RateLimitExceededException
        A-->>S: Exception
        S-->>C: HTTP 429 + Headers
    end
```

## 3. Client Key Generation Flow

```mermaid
flowchart TD
    A[HTTP Request] --> B[Extract Request Info]
    B --> C{Rate Limit Scope}
    
    C -->|USER| D[Extract X-User-ID Header]
    C -->|IP| E[Extract Client IP]
    C -->|API_KEY| F[Extract X-API-Key Header]
    C -->|TENANT| G[Extract X-Tenant-ID Header]
    C -->|GLOBAL| H[Use 'global']
    C -->|CUSTOM| I[Evaluate SpEL Expression]
    
    D --> J[Build Key: user:john123:method]
    E --> K[Build Key: ip:192.168.1.1:method]
    F --> L[Build Key: key:abc123:method]
    G --> M[Build Key: tenant:company1:method]
    H --> N[Build Key: global:method]
    I --> O[Build Key: custom_result:method]
    
    J --> P[Redis Lookup]
    K --> P
    L --> P
    M --> P
    N --> P
    O --> P
    
    style P fill:#fff3e0
```

## 4. Sliding Window Algorithm Flow

```mermaid
flowchart TD
    A[Request with Client Key] --> B[Generate Redis Key]
    B --> C["Redis Key: rate_limit:sliding:user:john123:getProducts()"]
    C --> D[Get Current Timestamp]
    D --> E[Calculate Window Start]
    E --> F["Window: now - 60 seconds"]
    
    F --> G[Remove Expired Entries]
    G --> H["ZREMRANGEBYSCORE key 0 window_start"]
    
    H --> I[Count Current Requests]
    I --> J["ZCOUNT key window_start now"]
    
    J --> K{Count >= Limit?}
    K -->|Yes| L[DENY Request]
    K -->|No| M[Add Current Request]
    
    M --> N["ZADD key timestamp timestamp"]
    N --> O[Set Expiry]
    O --> P["EXPIRE key window_size"]
    P --> Q[ALLOW Request]
    
    L --> R[Calculate Retry After]
    R --> S[Return Denied Response]
    
    Q --> T[Calculate Remaining]
    T --> U[Return Allowed Response]
    
    style L fill:#ffebee
    style Q fill:#e8f5e8
```

## 5. Token Bucket Algorithm Flow

```mermaid
flowchart TD
    A[Request with Client Key] --> B[Generate Redis Key]
    B --> C["Redis Key: rate_limit:token:user:john123:getProducts()"]
    C --> D[Get Bucket State from Redis]
    
    D --> E{Bucket Exists?}
    E -->|No| F[Create New Bucket]
    E -->|Yes| G[Parse Existing Bucket]
    
    F --> H["tokens: burst_capacity<br/>last_refill: now"]
    G --> I[Calculate Tokens to Add]
    I --> J["tokens_to_add = (now - last_refill) × refill_rate"]
    
    H --> K[Check Token Availability]
    J --> L[Update Token Count]
    L --> K
    
    K --> M{Tokens >= 1?}
    M -->|Yes| N[Consume 1 Token]
    M -->|No| O[DENY Request]
    
    N --> P[Update Bucket State]
    P --> Q["SET key 'tokens:timestamp'"]
    Q --> R[ALLOW Request]
    
    O --> S[Calculate Retry After]
    S --> T[Return Denied Response]
    R --> U[Return Allowed Response]
    
    style O fill:#ffebee
    style R fill:#e8f5e8
```

## 6. Multi-Layer Rate Limiting Flow

```mermaid
flowchart TD
    A[Request] --> B[@RateLimits Annotation]
    B --> C[Extract Multiple @RateLimit]
    C --> D[Sort by Priority]
    
    D --> E[Check Layer 1: GLOBAL]
    E --> F{Global Limit OK?}
    F -->|No| G[DENY - Global Exceeded]
    F -->|Yes| H[Check Layer 2: USER]
    
    H --> I{User Limit OK?}
    I -->|No| J[DENY - User Exceeded]
    I -->|Yes| K[Check Layer 3: IP]
    
    K --> L{IP Limit OK?}
    L -->|No| M[DENY - IP Exceeded]
    L -->|Yes| N[ALL LAYERS PASSED]
    
    N --> O[Execute Method]
    
    G --> P[HTTP 429 Response]
    J --> P
    M --> P
    
    style G fill:#ffebee
    style J fill:#ffebee
    style M fill:#ffebee
    style O fill:#e8f5e8
```

## 7. Redis Data Structure Visualization

### Sliding Window (Sorted Set)
```
Redis Key: rate_limit:sliding:user:john123:getProducts()

ZRANGE key 0 -1 WITHSCORES
┌─────────────┬─────────────┐
│   Member    │    Score    │
├─────────────┼─────────────┤
│ 1704067200  │ 1704067200  │ ← Request 1
│ 1704067230  │ 1704067230  │ ← Request 2  
│ 1704067245  │ 1704067245  │ ← Request 3
│ 1704067250  │ 1704067250  │ ← Request 4
│ 1704067255  │ 1704067255  │ ← Request 5
└─────────────┴─────────────┘

Window: [1704067200 ────────────── 1704067260]
Current Time: 1704067260
Window Start: 1704067200 (60 seconds ago)
Count in Window: 5 requests
```

### Token Bucket (String)
```
Redis Key: rate_limit:token:user:john123:getProducts()

GET key
┌─────────────────────────────┐
│        Value                │
├─────────────────────────────┤
│    "7.5:1704067260"        │
│     ↑        ↑             │
│  tokens  last_refill       │
└─────────────────────────────┘

Bucket State:
- Available Tokens: 7.5
- Last Refill Time: 1704067260
- Refill Rate: 0.167 tokens/second
```

## 8. Error Handling Flow

```mermaid
flowchart TD
    A[Rate Limit Check] --> B{Exception Occurred?}
    
    B -->|RateLimitExceededException| C[Global Exception Handler]
    B -->|Redis Connection Error| D[Fallback: Allow Request]
    B -->|Algorithm Not Found| E[Log Warning + Allow]
    B -->|SpEL Evaluation Error| F[Use Default Key + Continue]
    
    C --> G[Set HTTP Status 429]
    G --> H[Add Rate Limit Headers]
    H --> I["X-RateLimit-Remaining: 0<br/>Retry-After: 60"]
    I --> J[Return Error Response]
    
    D --> K[Log Error + Continue]
    E --> K
    F --> K
    K --> L[Execute Method Normally]
    
    style C fill:#ffebee
    style D fill:#fff3e0
    style E fill:#fff3e0
    style F fill:#fff3e0
```

## 9. Complete Request Lifecycle

```mermaid
graph TB
    subgraph "Client Side"
        A[Send HTTP Request]
        Z[Receive Response]
    end
    
    subgraph "Spring Boot Application"
        B[DispatcherServlet]
        C[Controller Method]
    end
    
    subgraph "Rate Limiter (AOP)"
        D[RateLimitAspect]
        E[AnnotationRateLimitService]
        F[Algorithm Selection]
    end
    
    subgraph "Redis Cluster"
        G[Rate Limit State]
        H[Request Counters]
        I[Token Buckets]
    end
    
    subgraph "Response Generation"
        J[Success Response]
        K[Rate Limited Response]
        L[Add Headers]
    end
    
    A --> B
    B --> D
    D --> E
    E --> F
    F --> G
    G --> H
    G --> I
    
    H --> M{Limit Check}
    I --> M
    
    M -->|Allow| N[Execute Controller]
    M -->|Deny| O[Throw Exception]
    
    N --> C
    C --> J
    J --> L
    
    O --> K
    K --> L
    
    L --> Z
    
    style M fill:#fff3e0
    style N fill:#e8f5e8
    style O fill:#ffebee
```

## 10. Performance Optimization Flow

```mermaid
flowchart TD
    A[Rate Limit Request] --> B[Check Local Cache]
    B --> C{Rule Cached?}
    
    C -->|Yes| D[Use Cached Rule]
    C -->|No| E[Load from Database]
    E --> F[Cache Rule for 5 min]
    F --> D
    
    D --> G[Redis Connection Pool]
    G --> H{Connection Available?}
    
    H -->|Yes| I[Execute Redis Command]
    H -->|No| J[Wait for Connection]
    J --> I
    
    I --> K[Pipeline Multiple Commands]
    K --> L["MULTI<br/>ZREMRANGEBYSCORE<br/>ZCOUNT<br/>ZADD<br/>EXPIRE<br/>EXEC"]
    
    L --> M[Return Result]
    
    style B fill:#e3f2fd
    style G fill:#e8f5e8
    style K fill:#fff3e0
```

## 11. Monitoring and Metrics Flow

```mermaid
flowchart LR
    A[Rate Limit Decision] --> B[Metrics Collection]
    
    B --> C[Counter: Total Requests]
    B --> D[Counter: Allowed Requests]
    B --> E[Counter: Denied Requests]
    B --> F[Histogram: Response Time]
    B --> G[Gauge: Active Keys]
    
    C --> H[Prometheus/Micrometer]
    D --> H
    E --> H
    F --> H
    G --> H
    
    H --> I[Grafana Dashboard]
    H --> J[Alerting Rules]
    
    J --> K{Alert Threshold?}
    K -->|Exceeded| L[Send Alert]
    K -->|Normal| M[Continue Monitoring]
    
    style H fill:#e8f5e8
    style L fill:#ffebee
```

## 12. Configuration and Deployment Flow

```mermaid
graph TB
    subgraph "Development"
        A[@RateLimit Annotations]
        B[Application Code]
    end
    
    subgraph "Configuration"
        C[application.yml]
        D[Redis Configuration]
        E[Algorithm Settings]
    end
    
    subgraph "Runtime"
        F[Spring AOP Proxy]
        G[Rate Limiter Beans]
        H[Redis Connection]
    end
    
    subgraph "Production"
        I[Load Balancer]
        J[Multiple App Instances]
        K[Redis Cluster]
        L[Monitoring]
    end
    
    A --> F
    B --> F
    C --> G
    D --> H
    E --> G
    
    F --> I
    G --> J
    H --> K
    
    J --> L
    K --> L
    
    style A fill:#e1f5fe
    style I fill:#e8f5e8
    style K fill:#fff3e0
    style L fill:#f3e5f5
```

This comprehensive flow diagram shows exactly how the annotation-based rate limiter works from request to response, including all the internal components, decision points, and data flows.