# Ticket Booking Platform - Architecture Diagrams

## Understanding Ticket Booking Architecture

### What Makes Ticket Booking Complex?
Ticket booking systems face unique challenges that require specialized architectural solutions:

1. **Flash Sale Handling**: Thousands of users trying to book limited tickets simultaneously
2. **Inventory Consistency**: Prevent overselling while maintaining high performance
3. **Hold Management**: Reserve tickets temporarily during checkout process
4. **Payment Integration**: Handle payment failures and ensure atomic transactions
5. **Real-time Updates**: Show accurate availability across all channels

### Key Architectural Decisions

#### Redis-First Inventory Management

**Why Redis for Inventory?**
```java
// Traditional database approach (slow and doesn't scale)
public boolean bookTicketDB(Long ticketTypeId, int quantity) {
    TicketType ticketType = repository.findById(ticketTypeId);
    if (ticketType.getAvailableQuantity() >= quantity) {
        ticketType.setAvailableQuantity(
            ticketType.getAvailableQuantity() - quantity);
        repository.save(ticketType);
        return true;
    }
    return false;
}

// Redis approach (fast and scalable)
public boolean bookTicketRedis(Long ticketTypeId, int quantity) {
    String key = "inventory:" + ticketTypeId;
    
    // Atomic operation - prevents race conditions
    Long remaining = redisTemplate.opsForValue().decrement(key, quantity);
    
    if (remaining < 0) {
        // Rollback if insufficient inventory
        redisTemplate.opsForValue().increment(key, quantity);
        return false;
    }
    
    // Async update to database
    asyncUpdateDatabase(ticketTypeId, quantity);
    return true;
}
```

**Benefits of Redis-First Approach**:
- **Sub-millisecond Response**: Redis operations are extremely fast
- **Atomic Operations**: DECR/INCR operations are atomic
- **High Concurrency**: Handle thousands of simultaneous requests
- **Eventual Consistency**: Database updated asynchronously

#### Hold Mechanism Implementation

**The Hold Problem**
```
Scenario: User selects tickets → Goes to payment → Payment takes 30 seconds
Problem: Other users might book the same tickets during payment
Solution: Hold tickets temporarily with TTL (Time To Live)
```

**Hold Implementation**
```java
@Service
public class TicketHoldService {
    private static final int HOLD_DURATION_MINUTES = 10;
    
    public String holdTickets(Long ticketTypeId, int quantity, Long userId) {
        String holdId = generateHoldId();
        String inventoryKey = "inventory:" + ticketTypeId;
        String holdKey = "hold:" + holdId;
        
        // Atomic decrement from available inventory
        Long remaining = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
        
        if (remaining < 0) {
            // Insufficient inventory, rollback
            redisTemplate.opsForValue().increment(inventoryKey, quantity);
            return null;
        }
        
        // Create hold with automatic expiry
        HoldInfo holdInfo = new HoldInfo(ticketTypeId, quantity, userId);
        redisTemplate.opsForValue().set(holdKey, holdInfo, 
                                      Duration.ofMinutes(HOLD_DURATION_MINUTES));
        
        return holdId;
    }
    
    public void confirmHold(String holdId) {
        String holdKey = "hold:" + holdId;
        HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);
        
        if (holdInfo != null) {
            // Permanently reduce inventory in database
            ticketTypeRepository.decrementAvailableQuantity(
                holdInfo.getTicketTypeId(), 
                holdInfo.getQuantity()
            );
            
            // Remove hold
            redisTemplate.delete(holdKey);
        }
    }
    
    public void releaseHold(String holdId) {
        String holdKey = "hold:" + holdId;
        HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);
        
        if (holdInfo != null) {
            // Return inventory to available pool
            String inventoryKey = "inventory:" + holdInfo.getTicketTypeId();
            redisTemplate.opsForValue().increment(inventoryKey, holdInfo.getQuantity());
            
            // Remove hold
            redisTemplate.delete(holdKey);
        }
    }
}
```

#### Microservices Communication Patterns

**Synchronous vs Asynchronous Communication**

```java
// Synchronous - for critical path operations
@RestController
public class BookingController {
    
    @PostMapping("/bookings/hold")
    public ResponseEntity<BookingResponse> holdTickets(@RequestBody HoldRequest request) {
        // Synchronous call - user waits for response
        String holdId = inventoryService.holdTickets(
            request.getTicketTypeId(), 
            request.getQuantity(), 
            request.getUserId()
        );
        
        if (holdId != null) {
            Booking booking = bookingService.createHoldBooking(request, holdId);
            return ResponseEntity.ok(BookingResponse.success(booking));
        } else {
            return ResponseEntity.badRequest()
                .body(BookingResponse.error("No tickets available"));
        }
    }
}

// Asynchronous - for non-critical operations
@EventListener
public class BookingEventHandler {
    
    @Async
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        // Send confirmation email (async - don't block user)
        emailService.sendBookingConfirmation(
            event.getUserEmail(), 
            event.getBookingDetails()
        );
        
        // Update analytics (async)
        analyticsService.recordBooking(event.getBooking());
        
        // Send SMS notification (async)
        smsService.sendBookingConfirmation(
            event.getUserPhone(), 
            event.getBookingReference()
        );
    }
}
```

### Flash Sale Architecture Patterns

#### Queue-Based Processing
```java
@Component
public class FlashSaleManager {
    
    @EventListener
    public void handleHighTraffic(HighTrafficEvent event) {
        if (event.getConcurrentUsers() > FLASH_SALE_THRESHOLD) {
            // Switch to queue-based processing
            enableQueueMode(event.getEventId());
        }
    }
    
    private void enableQueueMode(Long eventId) {
        // Add users to virtual queue
        String queueKey = "queue:" + eventId;
        
        // Process queue with controlled rate
        scheduler.scheduleAtFixedRate(() -> {
            String userId = redisTemplate.opsForList().leftPop(queueKey);
            if (userId != null) {
                processBookingRequest(userId, eventId);
            }
        }, 0, 100, TimeUnit.MILLISECONDS); // Process 10 requests per second
    }
}
```

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

### Database Consistency Strategies

#### Write-Through vs Write-Behind Caching

**Write-Through (Strong Consistency)**
```java
public void updateInventoryWriteThrough(Long ticketTypeId, int quantity) {
    // Update Redis first
    String key = "inventory:" + ticketTypeId;
    redisTemplate.opsForValue().decrement(key, quantity);
    
    // Immediately update database (synchronous)
    ticketTypeRepository.decrementAvailableQuantity(ticketTypeId, quantity);
    
    // Both Redis and DB are consistent
}
```
**Pros**: Strong consistency between cache and database
**Cons**: Slower writes, database becomes bottleneck during flash sales

**Write-Behind (Eventual Consistency)**
```java
public void updateInventoryWriteBehind(Long ticketTypeId, int quantity) {
    // Update Redis immediately (fast response to user)
    String key = "inventory:" + ticketTypeId;
    redisTemplate.opsForValue().decrement(key, quantity);
    
    // Queue database update for later (asynchronous)
    inventoryUpdateQueue.send(new InventoryUpdate(ticketTypeId, quantity));
}

@EventListener
public void processInventoryUpdate(InventoryUpdate update) {
    // Process in background
    ticketTypeRepository.decrementAvailableQuantity(
        update.getTicketTypeId(), 
        update.getQuantity()
    );
}
```
**Pros**: Fast writes, high throughput during flash sales
**Cons**: Temporary inconsistency between cache and database

#### Reconciliation Strategy
```java
@Scheduled(fixedRate = 60000) // Every minute
public void reconcileInventory() {
    List<TicketType> ticketTypes = ticketTypeRepository.findAll();
    
    for (TicketType ticketType : ticketTypes) {
        String key = "inventory:" + ticketType.getId();
        Integer redisCount = (Integer) redisTemplate.opsForValue().get(key);
        
        if (redisCount == null) {
            // Initialize Redis from database
            redisTemplate.opsForValue().set(key, ticketType.getAvailableQuantity());
        } else if (!redisCount.equals(ticketType.getAvailableQuantity())) {
            // Reconcile differences
            log.warn("Inventory mismatch for ticket type {}: Redis={}, DB={}", 
                    ticketType.getId(), redisCount, ticketType.getAvailableQuantity());
            
            // During active sales, Redis is source of truth
            if (isActiveSalePeriod(ticketType.getEventId())) {
                ticketType.setAvailableQuantity(redisCount);
                ticketTypeRepository.save(ticketType);
            } else {
                // During quiet periods, database is source of truth
                redisTemplate.opsForValue().set(key, ticketType.getAvailableQuantity());
            }
        }
    }
}
```

## 3. Inventory Management Architecture

### Understanding Inventory Management Complexity

This diagram shows the multi-layered approach to inventory management:

1. **Redis Layer**: Real-time inventory tracking with atomic operations
2. **Database Layer**: Persistent storage and source of truth
3. **Background Jobs**: Cleanup and synchronization processes

#### Critical Operations Explained

**Hold Operation Flow**
```java
public class InventoryService {
    
    @Transactional
    public HoldResult holdTickets(Long ticketTypeId, int quantity, Long userId) {
        String inventoryKey = "inventory:" + ticketTypeId;
        
        // Step 1: Atomic decrement in Redis
        Long remaining = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
        
        if (remaining < 0) {
            // Step 2: Rollback if insufficient inventory
            redisTemplate.opsForValue().increment(inventoryKey, quantity);
            return HoldResult.failure("Insufficient inventory");
        }
        
        // Step 3: Create hold with TTL
        String holdId = UUID.randomUUID().toString();
        String holdKey = "hold:" + holdId;
        
        HoldInfo holdInfo = new HoldInfo(
            ticketTypeId, 
            quantity, 
            userId, 
            System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()
        );
        
        redisTemplate.opsForValue().set(holdKey, holdInfo, Duration.ofMinutes(10));
        
        // Step 4: Create booking record
        Booking booking = new Booking(
            userId, 
            ticketTypeId, 
            quantity, 
            BookingStatus.HELD, 
            holdId
        );
        bookingRepository.save(booking);
        
        return HoldResult.success(holdId, booking.getId());
    }
}
```

**Expired Hold Cleanup**
```java
@Component
public class HoldCleanupService {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupExpiredHolds() {
        // Find all hold keys
        Set<String> holdKeys = redisTemplate.keys("hold:*");
        
        for (String holdKey : holdKeys) {
            HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);
            
            if (holdInfo == null) {
                // Hold already expired, check for orphaned bookings
                String holdId = holdKey.substring(5); // Remove "hold:" prefix
                
                List<Booking> orphanedBookings = bookingRepository
                    .findByHoldIdAndStatus(holdId, BookingStatus.HELD);
                
                for (Booking booking : orphanedBookings) {
                    // Release inventory back to pool
                    String inventoryKey = "inventory:" + booking.getTicketTypeId();
                    redisTemplate.opsForValue().increment(inventoryKey, booking.getQuantity());
                    
                    // Mark booking as expired
                    booking.setStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);
                    
                    log.info("Released {} tickets for expired booking: {}", 
                            booking.getQuantity(), booking.getId());
                }
            }
        }
    }
}
```

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