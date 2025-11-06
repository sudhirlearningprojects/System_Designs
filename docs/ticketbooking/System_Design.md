# Ticket Booking Platform - System Design

## Understanding Ticket Booking Systems

### What is a Ticket Booking Platform?
A ticket booking platform is a system that manages event tickets, handles reservations, and processes payments while ensuring no overselling occurs. The core challenge is managing inventory under high concurrency during flash sales.

### Key Challenges in Ticket Booking
1. **Overselling Prevention**: Never sell more tickets than available
2. **Flash Sale Handling**: Manage thousands of concurrent booking requests
3. **Inventory Consistency**: Real-time accurate ticket counts
4. **Hold Management**: Reserve tickets temporarily during checkout
5. **Payment Integration**: Reliable payment processing
6. **Scalability**: Handle millions of users and events

### Ticket Booking Fundamentals

#### The Overselling Problem
```
Scenario: Concert has 1 ticket left, 1000 users try to book simultaneously

Without proper concurrency control:
User 1: Check availability (1 ticket) → Book ticket → Success
User 2: Check availability (1 ticket) → Book ticket → Success
...
User 1000: Check availability (1 ticket) → Book ticket → Success

Result: 1000 tickets sold instead of 1!
```

#### Solution: Atomic Inventory Management
```java
// Wrong approach - Race condition
public boolean bookTicket(Long ticketTypeId, int quantity) {
    TicketType ticketType = repository.findById(ticketTypeId);
    if (ticketType.getAvailableQuantity() >= quantity) {
        ticketType.setAvailableQuantity(
            ticketType.getAvailableQuantity() - quantity);
        repository.save(ticketType);
        return true;
    }
    return false;
}

// Correct approach - Atomic operation
public boolean bookTicketAtomic(Long ticketTypeId, int quantity) {
    String key = "inventory:" + ticketTypeId;
    Long remaining = redisTemplate.opsForValue().decrement(key, quantity);
    
    if (remaining < 0) {
        // Rollback if insufficient inventory
        redisTemplate.opsForValue().increment(key, quantity);
        return false;
    }
    return true;
}
```

#### Ticket Hold Mechanism

##### Why Hold Tickets?
```
Problem: User selects tickets → Goes to payment → Someone else books same tickets
Result: Payment fails, poor user experience

Solution: Hold tickets temporarily during checkout process
```

##### Hold Implementation
```java
public class TicketHoldService {
    private static final int HOLD_DURATION_MINUTES = 10;
    
    public String holdTickets(Long ticketTypeId, int quantity, Long userId) {
        String holdId = generateHoldId();
        String inventoryKey = "inventory:" + ticketTypeId;
        String holdKey = "hold:" + holdId;
        
        // Atomic decrement
        Long remaining = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
        
        if (remaining < 0) {
            // Insufficient inventory, rollback
            redisTemplate.opsForValue().increment(inventoryKey, quantity);
            return null;
        }
        
        // Create hold with expiry
        HoldInfo holdInfo = new HoldInfo(ticketTypeId, quantity, userId);
        redisTemplate.opsForValue().set(holdKey, holdInfo, 
                                      Duration.ofMinutes(HOLD_DURATION_MINUTES));
        
        return holdId;
    }
    
    public void releaseHold(String holdId) {
        String holdKey = "hold:" + holdId;
        HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);
        
        if (holdInfo != null) {
            // Release inventory back
            String inventoryKey = "inventory:" + holdInfo.getTicketTypeId();
            redisTemplate.opsForValue().increment(inventoryKey, holdInfo.getQuantity());
            
            // Remove hold
            redisTemplate.delete(holdKey);
        }
    }
}
```

### Database vs Cache for Inventory

#### Database-Only Approach (Slow)
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public boolean bookTicketDB(Long ticketTypeId, int quantity) {
    TicketType ticketType = repository.findByIdWithLock(ticketTypeId);
    
    if (ticketType.getAvailableQuantity() >= quantity) {
        ticketType.setAvailableQuantity(
            ticketType.getAvailableQuantity() - quantity);
        repository.save(ticketType);
        return true;
    }
    return false;
}
```
**Problems**: 
- Database locks cause bottlenecks
- Slow response times during high load
- Limited concurrent transactions

#### Redis-First Approach (Fast)
```java
public boolean bookTicketRedis(Long ticketTypeId, int quantity) {
    String key = "inventory:" + ticketTypeId;
    
    // Atomic operation in Redis
    Long remaining = redisTemplate.opsForValue().decrement(key, quantity);
    
    if (remaining < 0) {
        redisTemplate.opsForValue().increment(key, quantity);
        return false;
    }
    
    // Async update to database for persistence
    asyncUpdateDatabase(ticketTypeId, quantity);
    return true;
}
```
**Benefits**:
- Sub-millisecond response times
- Handles thousands of concurrent requests
- Atomic operations prevent race conditions

### Flash Sale Architecture

#### Traditional Approach (Fails at Scale)
```
User Request → Load Balancer → App Server → Database
                                    ↓
                              Bottleneck!
```

#### Optimized Approach
```
User Request → CDN → Load Balancer → App Server → Redis → Async DB Update
                                              ↓
                                        Fast Response
```

#### Queue-Based Processing
```java
@Component
public class BookingQueueProcessor {
    
    @EventListener
    public void handleBookingRequest(BookingRequestEvent event) {
        // Process booking requests in order
        BookingRequest request = event.getBookingRequest();
        
        boolean success = inventoryService.reserveTickets(
            request.getTicketTypeId(), 
            request.getQuantity()
        );
        
        if (success) {
            // Send to payment processing
            paymentQueue.send(new PaymentRequest(request));
        } else {
            // Notify user of failure
            notificationService.sendFailureNotification(request.getUserId());
        }
    }
}
```

## 1. Requirements

### Functional Requirements
- **Event Management**: Admins can add, update, and remove events with details like date, venue, ticket types (VIP, General), and pricing
- **User Management**: Users can register, log in, view available events, and manage their profile/bookings
- **Search & Browse**: Users can search for events by city, genre, date, and name
- **Booking Workflow**:
  - Users can select seats/tickets for an event
  - Selected tickets are held for a limited time (10 minutes) to prevent others from booking them
  - Users must complete payment within the hold time, or tickets are released
- **Payment Processing**: Integration with third-party payment gateway
- **Order Management**: Users can view their booking history

### Non-Functional Requirements
- **High Availability**: 99.99% uptime (4.38 minutes downtime per month)
- **Scalability**: Handle millions of users and 100K+ concurrent requests during flash sales
- **Consistency**: Strong consistency for ticket inventory to prevent overselling
- **Low Latency**: Sub-second response times for browsing events and booking tickets
- **Reliability**: Ensure all booking transactions are reliable (ACID properties)

## 2. High-Level Design (HLD)

### 2.1 System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │   Web Client    │    │   Admin Panel   │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   API Gateway   │
                    │  (Rate Limiting,│
                    │ Authentication) │
                    └─────────┬───────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
┌─────────▼───────┐ ┌─────────▼───────┐ ┌─────────▼───────┐
│  Event Service  │ │ Booking Service │ │  User Service   │
│   (Events,      │ │  (Inventory,    │ │ (Authentication,│
│ Ticket Types)   │ │   Bookings)     │ │   Profiles)     │
└─────────┬───────┘ └─────────┬───────┘ └─────────┬───────┘
          │                   │                   │
          │         ┌─────────▼───────┐           │
          │         │ Payment Service │           │
          │         │  (Gateway       │           │
          │         │  Integration)   │           │
          │         └─────────────────┘           │
          │                                       │
    ┌─────▼─────┐              ┌─────────────────▼─────────────────┐
    │PostgreSQL │              │              Redis               │
    │(Events,   │              │        (Inventory Cache,         │
    │Bookings,  │              │         Hold Management)         │
    │Users)     │              │                                  │
    └───────────┘              └──────────────────────────────────┘
```

### 2.2 Key Services

1. **API Gateway**: Entry point for all client requests, handles authentication, rate limiting, and routing
2. **Event Service**: Manages event data, venues, dates, and ticket types
3. **Booking Service**: Core service managing ticket availability, holds, and bookings
4. **User Service**: Manages user authentication, profiles, and sessions
5. **Payment Service**: Orchestrates communication with third-party payment gateways

### 2.3 Data Management

**Database Choices:**
- **PostgreSQL**: Primary database for Users, Events, Bookings, and TicketTypes
- **Redis**: In-memory store for real-time inventory management and temporary holds
- **Elasticsearch**: (Future) For advanced search capabilities

**Caching Strategy:**
- Redis caching for popular events and ticket availability
- Application-level caching for user profiles and event metadata

## 3. Low-Level Design (LLD)

### 3.1 Database Schema

#### PostgreSQL Tables

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Events table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255),
    city VARCHAR(100),
    genre VARCHAR(100),
    event_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ticket Types table
CREATE TABLE ticket_types (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    name VARCHAR(100) NOT NULL, -- VIP, General, Premium
    price DECIMAL(10,2) NOT NULL,
    total_quantity INTEGER NOT NULL,
    available_quantity INTEGER NOT NULL,
    CONSTRAINT check_available_quantity CHECK (available_quantity >= 0)
);

-- Bookings table
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    ticket_type_id BIGINT NOT NULL REFERENCES ticket_types(id),
    quantity INTEGER NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- HELD, CONFIRMED, CANCELLED, EXPIRED
    hold_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_id VARCHAR(255)
);

-- Indexes for performance
CREATE INDEX idx_events_city_date ON events(city, event_date);
CREATE INDEX idx_events_genre ON events(genre);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status_expires ON bookings(status, hold_expires_at);
```

#### Redis Data Structures

```
# Inventory management
inventory:{ticket_type_id} -> available_count (INTEGER)

# Hold management
hold:{hold_id} -> quantity (INTEGER with TTL of 10 minutes)

# Event caching
event:{event_id} -> event_data (JSON)
events:search:{hash} -> search_results (JSON)
```

### Inventory Synchronization Strategies

#### Strategy 1: Write-Through Cache
```java
public void updateInventory(Long ticketTypeId, int quantity) {
    // Update Redis first
    String key = "inventory:" + ticketTypeId;
    redisTemplate.opsForValue().decrement(key, quantity);
    
    // Immediately update database
    ticketTypeRepository.decrementAvailableQuantity(ticketTypeId, quantity);
}
```
**Pros**: Strong consistency
**Cons**: Slower writes, database bottleneck

#### Strategy 2: Write-Behind Cache (Recommended)
```java
public void updateInventoryAsync(Long ticketTypeId, int quantity) {
    // Update Redis immediately
    String key = "inventory:" + ticketTypeId;
    redisTemplate.opsForValue().decrement(key, quantity);
    
    // Queue database update
    inventoryUpdateQueue.send(new InventoryUpdate(ticketTypeId, quantity));
}

@EventListener
public void processInventoryUpdate(InventoryUpdate update) {
    // Batch updates for efficiency
    ticketTypeRepository.decrementAvailableQuantity(
        update.getTicketTypeId(), 
        update.getQuantity()
    );
}
```
**Pros**: Fast writes, high throughput
**Cons**: Eventual consistency

#### Strategy 3: Periodic Reconciliation
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
            
            // Use Redis as source of truth during active sales
            ticketType.setAvailableQuantity(redisCount);
            ticketTypeRepository.save(ticketType);
        }
    }
}
```

### Payment Integration Patterns

#### Synchronous Payment (Simple but Risky)
```java
public BookingResult processBooking(BookingRequest request) {
    // Hold tickets
    String holdId = holdService.holdTickets(request.getTicketTypeId(), 
                                          request.getQuantity(), 
                                          request.getUserId());
    
    if (holdId == null) {
        return BookingResult.failure("No tickets available");
    }
    
    try {
        // Process payment synchronously
        PaymentResult paymentResult = paymentService.processPayment(
            request.getPaymentInfo(), request.getTotalAmount());
        
        if (paymentResult.isSuccess()) {
            // Confirm booking
            Booking booking = confirmBooking(request, holdId, paymentResult.getTransactionId());
            return BookingResult.success(booking);
        } else {
            // Release hold
            holdService.releaseHold(holdId);
            return BookingResult.failure("Payment failed");
        }
    } catch (Exception e) {
        // Release hold on any error
        holdService.releaseHold(holdId);
        throw e;
    }
}
```
**Problems**: 
- Payment gateway timeouts affect user experience
- Holds may expire during slow payment processing
- Blocking operations reduce throughput

#### Asynchronous Payment (Recommended)
```java
public BookingResult initiateBooking(BookingRequest request) {
    // Hold tickets
    String holdId = holdService.holdTickets(request.getTicketTypeId(), 
                                          request.getQuantity(), 
                                          request.getUserId());
    
    if (holdId == null) {
        return BookingResult.failure("No tickets available");
    }
    
    // Create pending booking
    Booking booking = createPendingBooking(request, holdId);
    
    // Initiate async payment
    paymentService.initiatePaymentAsync(
        booking.getId(), 
        request.getPaymentInfo(), 
        request.getTotalAmount()
    );
    
    return BookingResult.pending(booking);
}

@EventListener
public void handlePaymentResult(PaymentResultEvent event) {
    Booking booking = bookingRepository.findById(event.getBookingId());
    
    if (event.isSuccess()) {
        // Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentTransactionId(event.getTransactionId());
        
        // Convert hold to permanent reservation
        holdService.confirmHold(booking.getHoldId());
    } else {
        // Cancel booking and release hold
        booking.setStatus(BookingStatus.CANCELLED);
        holdService.releaseHold(booking.getHoldId());
    }
    
    bookingRepository.save(booking);
    
    // Notify user
    notificationService.sendBookingUpdate(booking);
}
```

### 3.2 Core Algorithms

#### Ticket Hold Algorithm

```java
@Transactional
public boolean holdTickets(Long ticketTypeId, Integer quantity, String holdId) {
    String inventoryKey = "inventory:" + ticketTypeId;
    String holdKey = "hold:" + holdId;
    
    // Atomic decrement in Redis
    Long newAvailable = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
    if (newAvailable < 0) {
        // Rollback if insufficient inventory
        redisTemplate.opsForValue().increment(inventoryKey, quantity);
        return false;
    }
    
    // Set hold with TTL (10 minutes)
    redisTemplate.opsForValue().set(holdKey, quantity, 10, TimeUnit.MINUTES);
    return true;
}
```

#### Booking Workflow

1. **Hold Phase**: User selects tickets → System holds inventory in Redis
2. **Payment Phase**: User pays → Payment gateway processes transaction
3. **Confirmation Phase**: Payment success → Update database, confirm hold
4. **Cleanup Phase**: Expired holds → Background job releases inventory

### 3.3 Concurrency Control

**Pessimistic Locking**: Used for critical inventory updates in PostgreSQL
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT tt FROM TicketType tt WHERE tt.id = :id")
Optional<TicketType> findByIdWithLock(@Param("id") Long id);
```

**Redis Atomic Operations**: Ensures thread-safe inventory management
```java
// Atomic decrement prevents race conditions
Long newCount = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
```

## 4. Scalability and Performance

### 4.1 Bottlenecks and Solutions

**Bottleneck 1: Database Writes During Flash Sales**
- **Solution**: Redis-based inventory management with eventual consistency to PostgreSQL
- **Impact**: Reduces database load by 90% during peak traffic

**Bottleneck 2: Payment Gateway Latency**
- **Solution**: Asynchronous payment processing with callback handling
- **Impact**: Improves user experience, prevents timeout issues

**Bottleneck 3: Search Performance**
- **Solution**: Redis caching for popular searches, Elasticsearch for complex queries
- **Impact**: Sub-100ms search response times

### 4.2 Scaling Strategies

**Horizontal Scaling**:
- Microservices architecture allows independent scaling
- Database sharding by event_id or user_id for large datasets
- Redis clustering for high-availability caching

**Vertical Scaling**:
- CPU-optimized instances for booking service
- Memory-optimized instances for Redis cache
- I/O-optimized instances for PostgreSQL

### 4.3 Performance Optimizations

1. **Connection Pooling**: HikariCP for database connections
2. **Async Processing**: Spring @Async for non-critical operations
3. **Batch Operations**: Bulk database updates for efficiency
4. **CDN**: Static content delivery for event images and assets

## 5. Reliability and Fault Tolerance

### 5.1 High Availability Design

**Database Replication**:
- Master-slave PostgreSQL setup with automatic failover
- Redis Sentinel for cache high availability

**Service Redundancy**:
- Multiple instances of each microservice
- Load balancer health checks and automatic failover

**Circuit Breaker Pattern**:
- Prevents cascade failures during payment gateway outages
- Graceful degradation of non-critical features

### 5.2 Data Consistency

**Strong Consistency**: Critical for inventory management
- Redis atomic operations for real-time inventory
- PostgreSQL ACID transactions for booking confirmation

**Eventual Consistency**: Acceptable for non-critical data
- Event metadata caching with TTL-based refresh
- User profile updates with async propagation

### 5.3 Disaster Recovery

**Backup Strategy**:
- Daily PostgreSQL backups with point-in-time recovery
- Redis persistence with AOF (Append Only File)

**Recovery Procedures**:
- RTO (Recovery Time Objective): 15 minutes
- RPO (Recovery Point Objective): 1 hour

## 6. Security

### 6.1 Authentication and Authorization

**JWT-based Authentication**:
- Stateless token-based authentication
- Role-based access control (User, Admin)

**API Security**:
- Rate limiting to prevent abuse
- Input validation and sanitization
- HTTPS encryption for all communications

### 6.2 Data Protection

**PCI DSS Compliance**:
- No storage of sensitive payment data
- Tokenization for payment references
- Secure communication with payment gateways

**Data Encryption**:
- Encryption at rest for sensitive user data
- TLS 1.3 for data in transit

## 7. Monitoring and Observability

### 7.1 Metrics and Alerting

**Key Metrics**:
- Booking success rate (target: >99%)
- Average response time (target: <500ms)
- Inventory accuracy (target: 100%)
- Payment success rate (target: >98%)

**Alerting Thresholds**:
- High error rates (>1%)
- Slow response times (>1s)
- Low inventory accuracy (<99.9%)

### 7.2 Logging and Tracing

**Structured Logging**:
- JSON format with correlation IDs
- Centralized log aggregation with ELK stack

**Distributed Tracing**:
- Request tracing across microservices
- Performance bottleneck identification

## 8. Future Enhancements

### 8.1 Advanced Features

1. **Dynamic Pricing**: AI-based pricing optimization
2. **Recommendation Engine**: Personalized event suggestions
3. **Mobile Push Notifications**: Real-time booking updates
4. **Seat Selection**: Interactive venue maps
5. **Waitlist Management**: Automatic booking when tickets become available

### 8.2 Technical Improvements

1. **Event Sourcing**: Complete audit trail of all booking events
2. **CQRS**: Separate read/write models for better performance
3. **GraphQL**: Flexible API for mobile applications
4. **Machine Learning**: Fraud detection and demand forecasting
5. **Blockchain**: Ticket authenticity and transfer verification

## 9. Deployment and DevOps

### 9.1 Infrastructure

**Containerization**: Docker containers with Kubernetes orchestration
**CI/CD Pipeline**: Automated testing, building, and deployment
**Infrastructure as Code**: Terraform for cloud resource management

### 9.2 Environment Strategy

- **Development**: Local Docker Compose setup
- **Staging**: Kubernetes cluster with production-like data
- **Production**: Multi-AZ Kubernetes deployment with auto-scaling

This system design provides a robust, scalable, and reliable ticket booking platform capable of handling millions of users and preventing ticket overselling through careful inventory management and strong consistency guarantees.