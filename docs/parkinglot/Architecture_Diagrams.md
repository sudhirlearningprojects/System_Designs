# Parking Lot Management System - Architecture Diagrams

## Understanding Parking System Architecture

### What Makes Parking Systems Unique?
Parking systems have specific challenges that require careful architectural design:

1. **Real-time Inventory**: Track spot availability in real-time
2. **Concurrency Control**: Handle multiple vehicles entering simultaneously
3. **Atomic Operations**: Prevent double-booking of parking spots
4. **Payment Integration**: Handle various payment methods reliably
5. **Physical Integration**: Interface with gates, sensors, and display boards

### Key Architectural Decisions

#### Cache-First vs Database-First Architecture

**Database-First Approach (Traditional)**
```java
// Slow and doesn't scale
public boolean assignSpot(Vehicle vehicle) {
    ParkingSpot spot = database.findAvailableSpot(vehicle.getType());
    if (spot != null) {
        spot.setOccupied(true);
        database.save(spot);
        return true;
    }
    return false;
}
```
**Problems**: 
- Database becomes bottleneck
- Slow response times
- Poor user experience

**Cache-First Approach (Optimized)**
```java
// Fast and scalable
public boolean assignSpotOptimized(Vehicle vehicle) {
    String cacheKey = "available_spots:" + vehicle.getType();
    
    // Atomic operation in Redis
    String spotId = redisTemplate.opsForList().leftPop(cacheKey);
    
    if (spotId != null) {
        // Async update to database
        asyncUpdateDatabase(spotId, true);
        return true;
    }
    return false;
}
```
**Benefits**:
- Sub-millisecond response times
- Handles high concurrency
- Better user experience

#### Microservices for Parking Systems
- **Parking Service**: Core spot allocation logic
- **Ticket Service**: Ticket lifecycle management
- **Payment Service**: Fee calculation and payment processing
- **Display Service**: Real-time availability updates

## 1. High-Level System Architecture

```mermaid
graph TB
    subgraph "Physical Layer"
        GATE1[Entry Gate 1]
        GATE2[Entry Gate 2]
        EXIT1[Exit Gate 1]
        EXIT2[Exit Gate 2]
        DISPLAY[Display Boards]
        SENSORS[Spot Sensors]
    end
    
    subgraph "API Gateway"
        LB[Load Balancer]
        AG[API Gateway]
        AUTH[Authentication]
    end
    
    subgraph "Microservices"
        PS[Parking Service]
        TS[Ticket Service]
        PAYS[Payment Service]
        DS[Display Service]
        NS[Notification Service]
    end
    
    subgraph "Caching Layer"
        REDIS[Redis Cluster]
        SPOT_CACHE[Spot Availability Cache]
        SESSION_CACHE[Session Cache]
    end
    
    subgraph "Data Layer"
        POSTGRES[PostgreSQL]
        TICKETS_DB[Tickets Database]
        SPOTS_DB[Spots Database]
    end
    
    subgraph "External Services"
        PAYMENT_GW[Payment Gateway]
        SMS[SMS Service]
        EMAIL[Email Service]
    end
    
    GATE1 --> LB
    GATE2 --> LB
    EXIT1 --> LB
    EXIT2 --> LB
    
    LB --> AG
    AG --> AUTH
    AUTH --> PS
    AUTH --> TS
    AUTH --> PAYS
    
    PS --> REDIS
    TS --> REDIS
    PS --> POSTGRES
    TS --> POSTGRES
    
    PAYS --> PAYMENT_GW
    NS --> SMS
    NS --> EMAIL
    
    DS --> DISPLAY
    SENSORS --> PS
    
    REDIS --> SPOT_CACHE
    REDIS --> SESSION_CACHE
    POSTGRES --> TICKETS_DB
    POSTGRES --> SPOTS_DB
```

### Architecture Explanation
This diagram shows a distributed parking system with:

1. **Physical Integration**: Gates and sensors connected to the system
2. **High Availability**: Load balancer distributes traffic across multiple instances
3. **Caching Strategy**: Redis for real-time data, PostgreSQL for persistent data
4. **Microservices**: Specialized services for different functions
5. **External Integration**: Payment gateways and notification services

## 2. Vehicle Entry Flow

```mermaid
sequenceDiagram
    participant Vehicle
    participant Gate as Entry Gate
    participant API as API Gateway
    participant PS as Parking Service
    participant Redis
    participant DB as Database
    participant TS as Ticket Service
    participant Display
    
    Vehicle->>Gate: Approach Entry Gate
    Gate->>API: Request Spot Assignment
    API->>PS: Find Available Spot
    
    PS->>Redis: Check Spot Availability
    Redis-->>PS: Available Spots List
    
    alt Spots Available
        PS->>Redis: Atomic Spot Reservation
        Redis-->>PS: Spot Reserved
        
        PS->>TS: Generate Ticket
        TS->>DB: Save Ticket Record
        DB-->>TS: Ticket Saved
        
        TS-->>PS: Ticket Generated
        PS-->>API: Spot Assigned
        API-->>Gate: Open Gate + Ticket
        
        PS->>Display: Update Availability
        Display->>Display: Show New Count
        
        Gate-->>Vehicle: Gate Opens + Ticket Issued
    else No Spots Available
        PS-->>API: Parking Full
        API-->>Gate: Display Full Message
        Gate-->>Vehicle: Entry Denied
    end
```

### Flow Analysis
This sequence shows the critical path for vehicle entry:

1. **Atomic Reservation**: Redis ensures no double-booking
2. **Async Updates**: Display boards updated without blocking entry
3. **Graceful Failure**: Clear messaging when parking is full
4. **Ticket Generation**: Immediate ticket creation for tracking

## 3. Concurrency Control Architecture

```mermaid
graph TB
    subgraph "Concurrent Requests"
        R1[Vehicle 1<br/>Compact Car]
        R2[Vehicle 2<br/>SUV]
        R3[Vehicle 3<br/>Motorcycle]
    end
    
    subgraph "Parking Service"
        PS[Parking Service<br/>Thread-Safe Operations]
    end
    
    subgraph "Redis Atomic Operations"
        COMPACT_LIST[Compact Spots List<br/>A1, A2, A3, A4]
        REGULAR_LIST[Regular Spots List<br/>B1, B2, B3, B4]
        LARGE_LIST[Large Spots List<br/>C1, C2, C3, C4]
    end
    
    subgraph "Spot Assignment Logic"
        ATOMIC_POP[LPOP Operation<br/>Atomic List Pop]
        FALLBACK[Fallback Logic<br/>Try Larger Spots]
    end
    
    R1 --> PS
    R2 --> PS
    R3 --> PS
    
    PS --> ATOMIC_POP
    ATOMIC_POP --> COMPACT_LIST
    ATOMIC_POP --> REGULAR_LIST
    ATOMIC_POP --> LARGE_LIST
    
    ATOMIC_POP --> FALLBACK
    
    note1[Redis LPOP is atomic<br/>Only one request gets each spot]
    note2[Compact cars can use any spot<br/>SUVs need regular or large<br/>Motorcycles use compact only]
```

### Concurrency Control Implementation
```java
@Service
public class ParkingService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public ParkingResult assignSpot(Vehicle vehicle) {
        VehicleType type = vehicle.getType();
        
        // Try spots in order of preference
        String[] spotTypes = getSpotTypesForVehicle(type);
        
        for (String spotType : spotTypes) {
            String listKey = "available_spots:" + spotType;
            
            // Atomic operation - only one thread gets the spot
            String spotId = redisTemplate.opsForList().leftPop(listKey);
            
            if (spotId != null) {
                // Successfully reserved spot
                ParkingSpot spot = new ParkingSpot(spotId, spotType);
                
                // Async database update
                asyncUpdateSpotStatus(spotId, true);
                
                // Generate ticket
                Ticket ticket = ticketService.generateTicket(vehicle, spot);
                
                return ParkingResult.success(ticket, spot);
            }
        }
        
        return ParkingResult.failure("No available spots");
    }
    
    private String[] getSpotTypesForVehicle(VehicleType type) {
        switch (type) {
            case MOTORCYCLE:
                return new String[]{"compact"};
            case COMPACT_CAR:
                return new String[]{"compact", "regular", "large"};
            case SUV:
                return new String[]{"regular", "large"};
            case TRUCK:
                return new String[]{"large"};
            default:
                return new String[]{"regular"};
        }
    }
}
```

## 4. Payment Processing Flow

```mermaid
flowchart TD
    A[Vehicle at Exit Gate] --> B[Scan Ticket/License Plate]
    B --> C[Calculate Parking Fee]
    C --> D{Payment Method?}
    
    D -->|Cash| E[Cash Payment]
    D -->|Card| F[Card Payment]
    D -->|Mobile| G[Mobile Payment]
    D -->|Prepaid| H[Prepaid Account]
    
    E --> I[Update Ticket Status]
    F --> J{Payment Success?}
    G --> J
    H --> K{Sufficient Balance?}
    
    J -->|Yes| I
    J -->|No| L[Payment Failed]
    K -->|Yes| I
    K -->|No| M[Insufficient Funds]
    
    I --> N[Release Parking Spot]
    N --> O[Update Availability Cache]
    O --> P[Open Exit Gate]
    
    L --> Q[Retry Payment]
    M --> Q
    Q --> D
    
    P --> R[Vehicle Exits]
    
    style I fill:#c8e6c9
    style L fill:#ffcdd2
    style M fill:#ffcdd2
```

### Payment Integration Architecture
```java
@Service
public class PaymentService {
    
    private final Map<PaymentMethod, PaymentProcessor> processors;
    private final CircuitBreaker circuitBreaker;
    
    public PaymentResult processPayment(PaymentRequest request) {
        PaymentProcessor processor = processors.get(request.getMethod());
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                // Process payment with external gateway
                PaymentResponse response = processor.processPayment(request);
                
                if (response.isSuccess()) {
                    // Update ticket status
                    ticketService.markPaid(request.getTicketId(), response.getTransactionId());
                    
                    // Release parking spot
                    releaseSpot(request.getTicketId());
                    
                    return PaymentResult.success(response.getTransactionId());
                } else {
                    return PaymentResult.failure(response.getErrorMessage());
                }
                
            } catch (PaymentGatewayException e) {
                log.error("Payment gateway error for ticket: {}", request.getTicketId(), e);
                return PaymentResult.failure("Payment service temporarily unavailable");
            }
        });
    }
    
    private void releaseSpot(String ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        ParkingSpot spot = ticket.getSpot();
        
        // Add spot back to available list
        String listKey = "available_spots:" + spot.getType();
        redisTemplate.opsForList().rightPush(listKey, spot.getId());
        
        // Update display boards
        displayService.updateAvailability(spot.getFloor(), spot.getType(), 1);
        
        // Async database update
        asyncUpdateSpotStatus(spot.getId(), false);
    }
}
```

## 5. Real-time Display Board Architecture

```mermaid
graph LR
    subgraph "Event Sources"
        ENTRY[Vehicle Entry]
        EXIT[Vehicle Exit]
        SENSOR[Spot Sensors]
    end
    
    subgraph "Event Processing"
        KAFKA[Apache Kafka]
        STREAM[Event Stream Processor]
    end
    
    subgraph "Display Service"
        DS[Display Service]
        AGGREGATOR[Count Aggregator]
        FORMATTER[Display Formatter]
    end
    
    subgraph "Display Boards"
        FLOOR1[Floor 1 Display]
        FLOOR2[Floor 2 Display]
        FLOOR3[Floor 3 Display]
        ENTRANCE[Entrance Display]
    end
    
    ENTRY --> KAFKA
    EXIT --> KAFKA
    SENSOR --> KAFKA
    
    KAFKA --> STREAM
    STREAM --> DS
    
    DS --> AGGREGATOR
    AGGREGATOR --> FORMATTER
    
    FORMATTER --> FLOOR1
    FORMATTER --> FLOOR2
    FORMATTER --> FLOOR3
    FORMATTER --> ENTRANCE
    
    note1[Real-time updates<br/>Sub-second latency]
    note2[WebSocket connections<br/>to display boards]
```

### Display Update Implementation
```java
@Component
public class DisplayService {
    
    @Autowired
    private WebSocketTemplate webSocketTemplate;
    
    @EventListener
    public void handleSpotChange(SpotChangeEvent event) {
        // Update availability count
        String cacheKey = String.format("availability:%s:%s", 
                                       event.getFloor(), 
                                       event.getSpotType());
        
        Long newCount = redisTemplate.opsForValue().increment(cacheKey, event.getChange());
        
        // Create display message
        DisplayMessage message = new DisplayMessage(
            event.getFloor(),
            event.getSpotType(),
            newCount.intValue(),
            System.currentTimeMillis()
        );
        
        // Send to all display boards on the floor
        String topic = "/topic/floor/" + event.getFloor();
        webSocketTemplate.convertAndSend(topic, message);
        
        // Send to entrance displays
        webSocketTemplate.convertAndSend("/topic/entrance", message);
        
        log.info("Updated display for floor {} {}: {} spots available", 
                event.getFloor(), event.getSpotType(), newCount);
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void sendHeartbeat() {
        // Send heartbeat to all displays to detect disconnections
        HeartbeatMessage heartbeat = new HeartbeatMessage(System.currentTimeMillis());
        webSocketTemplate.convertAndSend("/topic/heartbeat", heartbeat);
    }
}
```

## 6. Database Schema Relationships

```mermaid
erDiagram
    PARKING_LOTS ||--o{ FLOORS : contains
    FLOORS ||--o{ PARKING_SPOTS : has
    PARKING_SPOTS ||--o{ TICKETS : assigned_to
    VEHICLES ||--o{ TICKETS : owns
    TICKETS ||--o{ PAYMENTS : has
    
    PARKING_LOTS {
        uuid id PK
        string name
        string address
        int total_floors
        timestamp created_at
    }
    
    FLOORS {
        uuid id PK
        uuid parking_lot_id FK
        int floor_number
        int total_spots
        int available_spots
    }
    
    PARKING_SPOTS {
        uuid id PK
        uuid floor_id FK
        string spot_number
        enum spot_type
        boolean is_occupied
        boolean is_reserved
        timestamp last_updated
    }
    
    VEHICLES {
        uuid id PK
        string license_plate UK
        enum vehicle_type
        string owner_name
        string phone_number
    }
    
    TICKETS {
        uuid id PK
        uuid vehicle_id FK
        uuid spot_id FK
        timestamp entry_time
        timestamp exit_time
        enum status
        decimal fee_amount
        string qr_code
    }
    
    PAYMENTS {
        uuid id PK
        uuid ticket_id FK
        decimal amount
        enum payment_method
        string transaction_id
        enum status
        timestamp created_at
    }
```

## 7. Caching Strategy Architecture

```mermaid
graph TB
    subgraph "Cache Layers"
        L1[L1: Application Cache<br/>In-Memory Maps]
        L2[L2: Redis Cache<br/>Distributed Cache]
        L3[L3: Database<br/>Persistent Storage]
    end
    
    subgraph "Cache Types"
        SPOT_AVAIL[Spot Availability<br/>Real-time Lists]
        SESSION[User Sessions<br/>Authentication]
        CONFIG[Configuration<br/>Parking Rules]
        STATS[Statistics<br/>Usage Metrics]
    end
    
    subgraph "Cache Patterns"
        WRITE_THROUGH[Write-Through<br/>Immediate Consistency]
        WRITE_BEHIND[Write-Behind<br/>Eventual Consistency]
        CACHE_ASIDE[Cache-Aside<br/>Lazy Loading]
    end
    
    L1 --> L2
    L2 --> L3
    
    SPOT_AVAIL --> WRITE_THROUGH
    SESSION --> CACHE_ASIDE
    CONFIG --> CACHE_ASIDE
    STATS --> WRITE_BEHIND
    
    WRITE_THROUGH --> L2
    WRITE_BEHIND --> L2
    CACHE_ASIDE --> L2
```

### Cache Implementation Strategy
```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}

@Service
public class SpotAvailabilityService {
    
    // Cache spot availability with short TTL
    @Cacheable(value = "spot-availability", key = "#floor + ':' + #spotType")
    public int getAvailableSpots(String floor, SpotType spotType) {
        return spotRepository.countAvailableSpots(floor, spotType);
    }
    
    // Evict cache when spots are assigned/released
    @CacheEvict(value = "spot-availability", key = "#floor + ':' + #spotType")
    public void updateSpotAvailability(String floor, SpotType spotType, int change) {
        // Update will be reflected in next cache miss
    }
}
```

## 8. Monitoring and Alerting Architecture

```mermaid
graph TB
    subgraph "Metrics Sources"
        APP[Application Metrics]
        INFRA[Infrastructure Metrics]
        BUSINESS[Business Metrics]
        LOGS[Application Logs]
    end
    
    subgraph "Collection & Processing"
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
        ELK[ELK Stack]
        ALERTS[Alert Manager]
    end
    
    subgraph "Key Metrics"
        OCCUPANCY[Occupancy Rate %]
        RESPONSE_TIME[API Response Time]
        PAYMENT_SUCCESS[Payment Success Rate]
        GATE_UPTIME[Gate Uptime %]
    end
    
    subgraph "Alerting Channels"
        EMAIL[Email Alerts]
        SMS[SMS Alerts]
        SLACK[Slack Notifications]
        DASHBOARD[Operations Dashboard]
    end
    
    APP --> PROMETHEUS
    INFRA --> PROMETHEUS
    BUSINESS --> PROMETHEUS
    LOGS --> ELK
    
    PROMETHEUS --> GRAFANA
    PROMETHEUS --> ALERTS
    ELK --> GRAFANA
    
    OCCUPANCY --> BUSINESS
    RESPONSE_TIME --> APP
    PAYMENT_SUCCESS --> APP
    GATE_UPTIME --> INFRA
    
    ALERTS --> EMAIL
    ALERTS --> SMS
    ALERTS --> SLACK
    ALERTS --> DASHBOARD
```

This comprehensive architecture ensures the parking system can handle high concurrency, provide real-time updates, and maintain data consistency while offering excellent user experience through fast response times and reliable payment processing.