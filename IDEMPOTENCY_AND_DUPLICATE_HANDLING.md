# Idempotency and Duplicate Handling - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [What is Idempotency?](#what-is-idempotency)
3. [Why Idempotency Matters](#why-idempotency-matters)
4. [Implementation Across Systems](#implementation-across-systems)
5. [Payment Service](#payment-service)
6. [Digital Payment Platform](#digital-payment-platform)
7. [Ticket Booking System](#ticket-booking-system)
8. [Notification System](#notification-system)

---

## Introduction

This document provides a comprehensive explanation of how **idempotency** and **duplicate handling** are implemented across all system designs in this repository. Idempotency is critical for building reliable distributed systems that can handle network failures, retries, and concurrent requests without causing unintended side effects.

---

## What is Idempotency?

**Idempotency** is a property of operations where performing the same operation multiple times produces the same result as performing it once.

### Mathematical Definition
```
f(f(x)) = f(x)
```

### Real-World Examples

#### Idempotent Operations ✅
- **HTTP GET**: Reading data multiple times doesn't change the data
- **HTTP PUT**: Updating a resource to the same value multiple times
- **HTTP DELETE**: Deleting an already deleted resource
- **Setting a value**: `x = 5` (executing multiple times still results in x = 5)

#### Non-Idempotent Operations ❌
- **HTTP POST**: Creating a resource multiple times creates multiple resources
- **Incrementing**: `x = x + 1` (executing multiple times changes the result)
- **Charging a credit card**: Multiple charges result in multiple debits
- **Sending notifications**: Multiple sends result in duplicate messages

---

## Why Idempotency Matters

### Problem Scenarios

#### 1. Network Failures
```
Client                    Server
  |                         |
  |---Payment Request------>|
  |                         | (Processing...)
  |                         | (Payment successful)
  |<----Response------------|
  X (Network timeout)       |
  |                         |
  |---Retry Request-------->|
  |                         | ❌ Double charge without idempotency!
```

#### 2. Client Retries
```
User clicks "Pay" button multiple times
→ Multiple payment requests
→ Without idempotency: Multiple charges
→ With idempotency: Single charge, cached responses
```

#### 3. Concurrent Requests
```
Request 1: Create booking for Event A
Request 2: Create booking for Event A (duplicate)
→ Without idempotency: Double booking
→ With idempotency: Single booking, second request returns cached result
```

### Business Impact

| Scenario | Without Idempotency | With Idempotency |
|----------|---------------------|------------------|
| Payment retry | Double charge ($100 → $200) | Single charge ($100) |
| Ticket booking | Overselling (2 tickets sold for 1 seat) | Correct inventory (1 ticket) |
| Notification | Spam (10 duplicate emails) | Single notification |
| Account creation | Duplicate accounts | Single account |

**Cost of Failure:**
- **Financial Loss**: Refunds, chargebacks, customer compensation
- **Customer Trust**: Poor user experience, negative reviews
- **Legal Issues**: Compliance violations, lawsuits
- **Operational Overhead**: Manual reconciliation, support tickets

---

## Implementation Strategies

### 1. Idempotency Keys

**Concept**: Client generates a unique key for each logical operation. Server uses this key to detect duplicates.

```java
// Client generates idempotency key
String idempotencyKey = UUID.randomUUID().toString();

// Client sends request with key
POST /api/payments
Headers:
  Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Body:
  { "amount": 100, "currency": "USD" }

// Server checks if key exists
if (cache.contains(idempotencyKey)) {
    return cache.get(idempotencyKey); // Return cached response
}

// Process request and cache result
result = processPayment(request);
cache.put(idempotencyKey, result, TTL=24h);
return result;
```

**Key Properties:**
- **Uniqueness**: Must be unique per logical operation
- **Client-generated**: Client controls retry semantics
- **TTL**: Expire after reasonable time (24-48 hours)
- **Storage**: Redis (fast) + Database (durable)

### 2. Database Constraints

**Concept**: Use unique constraints to prevent duplicate records.

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    amount DECIMAL(10,2),
    status VARCHAR(20)
);

-- Attempt to insert duplicate
INSERT INTO payments (id, idempotency_key, amount, status)
VALUES (uuid(), 'key-123', 100.00, 'COMPLETED');
-- ✅ Success

INSERT INTO payments (id, idempotency_key, amount, status)
VALUES (uuid(), 'key-123', 100.00, 'COMPLETED');
-- ❌ Error: Duplicate key violation
```

### 3. Distributed Locks

**Concept**: Acquire a lock before processing to prevent concurrent duplicates.

```java
String lockKey = "lock:payment:" + idempotencyKey;

// Try to acquire lock
boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);

if (!acquired) {
    // Another request is processing, wait or return error
    throw new ConcurrentModificationException("Request already processing");
}

try {
    // Process payment
    result = processPayment(request);
    return result;
} finally {
    // Release lock
    redisTemplate.delete(lockKey);
}
```

### 4. State Machines

**Concept**: Use state transitions to ensure operations are idempotent.

```java
// Payment state machine
PENDING → PROCESSING → COMPLETED
        ↓
      FAILED

// Idempotent transitions
PENDING → PROCESSING: ✅ Allowed
PROCESSING → PROCESSING: ✅ Idempotent (no-op)
COMPLETED → COMPLETED: ✅ Idempotent (no-op)
COMPLETED → PROCESSING: ❌ Not allowed
```

---

## Implementation Across Systems

### Summary Table

| System | Idempotency Method | Storage | TTL | Use Case |
|--------|-------------------|---------|-----|----------|
| Payment Service | Idempotency Key + Cache | Redis + PostgreSQL | 24h | Prevent double charges |
| Digital Payment | Idempotency Key + Cache | Redis | 24h | Prevent duplicate transfers |
| Ticket Booking | Hold Mechanism + Lock | Redis | 10min | Prevent overselling |
| Notification | Idempotency Key + Dedup | Redis | 24h | Prevent duplicate messages |
| Google Docs | Operation Versioning | Redis | N/A | Conflict resolution |
| Uber | Request Deduplication | Redis | 1h | Prevent duplicate rides |
| Instagram | Post ID Uniqueness | Database | N/A | Prevent duplicate posts |

---

## Payment Service

### Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /payments
       │ Idempotency-Key: abc-123
       ▼
┌─────────────────────────────────┐
│   Payment Service               │
│                                 │
│  1. Check Idempotency Cache     │
│     ├─ Redis (fast lookup)      │
│     └─ PostgreSQL (fallback)    │
│                                 │
│  2. If cached → Return response │
│                                 │
│  3. If not cached:              │
│     ├─ Create transaction       │
│     ├─ Process with PSP         │
│     ├─ Update status            │
│     └─ Cache response           │
└─────────────────────────────────┘
```

### Implementation Details

#### 1. Idempotency Cache Model

```java
@Entity
@Table(name = "payment_idempotency_cache")
public class IdempotencyCache {
    @Id
    private String key;                    // Idempotency key
    
    private UUID transactionId;            // Associated transaction
    
    @Column(columnDefinition = "TEXT")
    private String responseData;           // Cached JSON response
    
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;       // TTL: 24 hours
}
```

**Why this design?**
- **Primary Key on idempotency_key**: Ensures uniqueness at database level
- **JSON response storage**: Allows returning exact same response
- **TTL field**: Enables cleanup of old entries
- **Transaction ID link**: Allows querying original transaction

#### 2. Idempotency Service

```java
@Service
public class IdempotencyService {
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String REDIS_PREFIX = "payment:idempotency:";
    
    public PaymentResponse getCachedResponse(String idempotencyKey) {
        // L1: Check Redis (fast)
        String redisKey = REDIS_PREFIX + idempotencyKey;
        String cachedJson = redisTemplate.opsForValue().get(redisKey);
        
        if (cachedJson != null) {
            return deserialize(cachedJson);
        }
        
        // L2: Check Database (durable)
        Optional<IdempotencyCache> dbCache = 
            cacheRepository.findById(idempotencyKey);
        
        if (dbCache.isPresent() && !isExpired(dbCache.get())) {
            // Populate Redis for future requests
            redisTemplate.opsForValue()
                .set(redisKey, dbCache.get().getResponseData(), CACHE_TTL);
            return deserialize(dbCache.get().getResponseData());
        }
        
        return null; // Not cached
    }
    
    public void cacheResponse(String idempotencyKey, 
                               PaymentResponse response, 
                               UUID transactionId) {
        String responseJson = serialize(response);
        
        // Cache in Redis (fast access)
        redisTemplate.opsForValue()
            .set(REDIS_PREFIX + idempotencyKey, responseJson, CACHE_TTL);
        
        // Cache in Database (durability)
        IdempotencyCache dbCache = new IdempotencyCache();
        dbCache.setKey(idempotencyKey);
        dbCache.setTransactionId(transactionId);
        dbCache.setResponseData(responseJson);
        dbCache.setExpiresAt(LocalDateTime.now().plus(CACHE_TTL));
        
        cacheRepository.save(dbCache);
    }
}
```

**Design Decisions:**

1. **Two-Layer Caching**
   - **Redis**: Fast in-memory lookup (< 1ms)
   - **PostgreSQL**: Durable storage survives Redis restart
   - **Fallback**: If Redis miss, check database and repopulate Redis

2. **24-Hour TTL**
   - **Why 24 hours?** Balances storage cost vs. retry window
   - **Client retries**: Most retries happen within minutes
   - **Reconciliation**: Gives time for manual investigation
   - **Cleanup**: Automatic expiry prevents unbounded growth

3. **JSON Serialization**
   - **Exact response**: Return identical response to client
   - **Flexibility**: Can cache any response structure
   - **Debugging**: Human-readable in database

#### 3. Payment Service Integration

```java
@Service
public class PaymentService {
    
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, 
                                          String idempotencyKey) {
        // Step 1: Check for duplicate request
        PaymentResponse cachedResponse = 
            idempotencyService.getCachedResponse(idempotencyKey);
        
        if (cachedResponse != null) {
            log.info("Returning cached response for key: {}", idempotencyKey);
            return cachedResponse; // ✅ Idempotent return
        }
        
        // Step 2: Create transaction record
        PaymentTransaction transaction = createTransaction(request, idempotencyKey);
        transaction = transactionRepository.save(transaction);
        
        try {
            // Step 3: Process with external processor
            transaction.setStatus(PROCESSING);
            transactionRepository.save(transaction);
            
            String processorTxnId = processorService.processPayment(transaction);
            
            // Step 4: Update transaction on success
            transaction.setProcessorTransactionId(processorTxnId);
            transaction.setStatus(COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            
            // Step 5: Build response
            PaymentResponse response = mapToResponse(transaction);
            
            // Step 6: Cache successful response
            idempotencyService.cacheResponse(
                idempotencyKey, response, transaction.getId());
            
            return response;
            
        } catch (Exception e) {
            // Handle failure (don't cache failures)
            transaction.setStatus(FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            
            throw new PaymentException("Payment failed: " + e.getMessage());
        }
    }
}
```

**Flow Diagram:**

```
Request with Idempotency-Key: "abc-123"
│
├─ Check Redis: "payment:idempotency:abc-123"
│  ├─ Found? → Return cached response ✅
│  └─ Not found? → Continue
│
├─ Check Database: idempotency_key = "abc-123"
│  ├─ Found & not expired? → Return cached response ✅
│  └─ Not found? → Continue
│
├─ Create Transaction (status: PENDING)
│  └─ Save to database
│
├─ Process Payment
│  ├─ Call external PSP (Stripe/PayPal)
│  ├─ Update status: PROCESSING → COMPLETED
│  └─ Save transaction
│
├─ Build Response
│  └─ { transactionId, status, amount, ... }
│
└─ Cache Response
   ├─ Redis: TTL 24h
   └─ Database: expires_at = now + 24h
```

### Edge Cases Handled

#### 1. Concurrent Requests with Same Key

```
Time    Request 1                Request 2
────────────────────────────────────────────
T0      Check cache (miss)       
T1      Create transaction       Check cache (miss)
T2      Process payment          Create transaction ❌
T3      Cache response           (Duplicate key error)
```

**Solution**: Database unique constraint on `idempotency_key`

```sql
CREATE UNIQUE INDEX idx_transaction_idempotency 
ON payment_transactions(idempotency_key);
```

Request 2 will fail with duplicate key error, forcing retry which will find cached response.

#### 2. Redis Failure

```
Scenario: Redis is down
│
├─ Check Redis → Error/Timeout
│  └─ Fallback to Database ✅
│
└─ Process payment
   └─ Cache in Database only
```

**Resilience**: System continues to work, just slower.

#### 3. Partial Failure

```
Scenario: Payment succeeds but caching fails
│
├─ Process payment → Success ✅
├─ Cache in Redis → Failure ❌
└─ Cache in Database → Failure ❌

Result: Transaction completed but not cached
Impact: Retry will create duplicate transaction ❌
```

**Solution**: Wrap caching in try-catch, log error but don't fail request.

```java
try {
    idempotencyService.cacheResponse(key, response, txnId);
} catch (Exception e) {
    log.error("Failed to cache response for key: {}", key, e);
    // Don't throw - payment already succeeded
}
```

**Better Solution**: Use database transaction to ensure atomicity.

```java
@Transactional
public PaymentResponse processPayment(...) {
    // Transaction and cache save in same transaction
    transaction = transactionRepository.save(transaction);
    cacheRepository.save(idempotencyCache);
    // Both commit together or both rollback
}
```

---

## Digital Payment Platform

### Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /payments/initiate
       │ { idempotencyKey: "xyz-789", ... }
       ▼
┌─────────────────────────────────┐
│   Digital Payment Service       │
│                                 │
│  1. Check Redis Cache           │
│     Key: "idempotency:xyz-789"  │
│                                 │
│  2. If cached → Return response │
│                                 │
│  3. If not cached:              │
│     ├─ Fraud detection          │
│     ├─ Create transaction       │
│     ├─ Process payment          │
│     │  ├─ Wallet (atomic)       │
│     │  └─ PSP (Stripe/PayPal)   │
│     └─ Cache response (24h)     │
└─────────────────────────────────┘
```

### Implementation Details

#### 1. Request Model with Idempotency

```java
public class PaymentInitiationRequest {
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private String type;              // P2P, P2M
    private String paymentMethod;     // WALLET, UPI, CARD
    private String idempotencyKey;    // ✅ Client-provided
    private String description;
}
```

#### 2. Payment Service with Idempotency

```java
@Service
public class PaymentService {
    
    @Transactional
    public PaymentInitiationResponse initiatePayment(
            PaymentInitiationRequest request) {
        
        // Step 1: Check idempotency
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null) {
            Object cachedResult = redisTemplate.opsForValue()
                .get("idempotency:" + idempotencyKey);
            
            if (cachedResult != null) {
                log.info("Returning cached payment for key: {}", idempotencyKey);
                return (PaymentInitiationResponse) cachedResult;
            }
        }
        
        // Step 2: Fraud detection
        if (fraudDetectionService.isSuspicious(request)) {
            return new PaymentInitiationResponse(
                null, "FAILED", "Blocked by fraud detection");
        }
        
        // Step 3: Create transaction
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(
            transactionId,
            request.getSenderId(),
            request.getReceiverId(),
            request.getAmount(),
            TransactionType.valueOf(request.getType()),
            PaymentMethod.valueOf(request.getPaymentMethod()),
            idempotencyKey  // ✅ Store idempotency key
        );
        
        transactionRepository.save(transaction);
        
        // Step 4: Process payment
        PaymentResponse pspResponse;
        if ("WALLET".equals(request.getPaymentMethod())) {
            pspResponse = processWalletPayment(request);
        } else {
            PaymentGatewayStrategy gateway = 
                gatewayFactory.getPaymentGateway(request.getPaymentMethod());
            pspResponse = gateway.processPayment(request);
        }
        
        // Step 5: Update transaction status
        transaction.setPspTransactionId(pspResponse.getPspTransactionId());
        transaction.setStatus(TransactionStatus.valueOf(pspResponse.getStatus()));
        transactionRepository.save(transaction);
        
        // Step 6: Build response
        PaymentInitiationResponse response = new PaymentInitiationResponse(
            transactionId, pspResponse.getStatus(), pspResponse.getMessage());
        
        // Step 7: Cache result for idempotency
        if (idempotencyKey != null) {
            redisTemplate.opsForValue().set(
                "idempotency:" + idempotencyKey, 
                response, 
                24, TimeUnit.HOURS);
        }
        
        return response;
    }
}
```

### Wallet Payment Idempotency

**Challenge**: Wallet transfers must be atomic and idempotent.

```java
@Service
public class LedgerService {
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean transferFunds(String senderId, 
                                 String receiverId, 
                                 BigDecimal amount) {
        // Pessimistic locking to prevent concurrent modifications
        Wallet senderWallet = walletRepository
            .findByUserIdWithLock(senderId);
        Wallet receiverWallet = walletRepository
            .findByUserIdWithLock(receiverId);
        
        // Check balance
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            return false; // Insufficient balance
        }
        
        // Atomic debit and credit
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);
        
        return true;
    }
}
```

**SQL Implementation:**

```sql
-- Pessimistic locking
SELECT * FROM wallets 
WHERE user_id = 'sender-123' 
FOR UPDATE;

-- Atomic update
UPDATE wallets 
SET balance = balance - 100.00 
WHERE user_id = 'sender-123' 
  AND balance >= 100.00;  -- ✅ Prevents negative balance

UPDATE wallets 
SET balance = balance + 100.00 
WHERE user_id = 'receiver-456';
```

### Edge Cases

#### 1. Duplicate Wallet Transfer

```
Scenario: Same idempotency key, two requests
│
Request 1:
├─ Check cache → Miss
├─ Transfer funds: $100 (sender: $500 → $400)
└─ Cache response

Request 2 (duplicate):
├─ Check cache → Hit ✅
└─ Return cached response (no transfer)

Result: Single transfer, both requests get same response
```

#### 2. Retry After Partial Failure

```
Scenario: Transfer succeeds but response fails
│
Request 1:
├─ Check cache → Miss
├─ Transfer funds: $100 ✅
├─ Cache response → Network error ❌
└─ Client receives error

Request 2 (retry):
├─ Check cache → Miss (caching failed)
├─ Transfer funds: $100 → Duplicate! ❌

Problem: Double transfer
```

**Solution**: Cache before external call or use database transaction.

```java
@Transactional
public PaymentInitiationResponse initiatePayment(...) {
    // Check cache
    if (cached) return cached;
    
    // Create transaction record
    transaction = transactionRepository.save(transaction);
    
    // Cache immediately (before external call)
    PaymentInitiationResponse response = 
        new PaymentInitiationResponse(transaction.getId(), "PROCESSING", "");
    
    if (idempotencyKey != null) {
        redisTemplate.opsForValue().set(
            "idempotency:" + idempotencyKey, response, 24, TimeUnit.HOURS);
    }
    
    // Now process payment
    // Even if this fails, retry will get cached "PROCESSING" response
    pspResponse = gateway.processPayment(request);
    
    // Update cached response
    response.setStatus(pspResponse.getStatus());
    redisTemplate.opsForValue().set(
        "idempotency:" + idempotencyKey, response, 24, TimeUnit.HOURS);
    
    return response;
}
```

---

## Ticket Booking System

### Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /bookings/hold
       ▼
┌─────────────────────────────────┐
│   Booking Service               │
│                                 │
│  1. Hold Tickets (Redis)        │
│     ├─ Check availability       │
│     ├─ Atomic decrement         │
│     └─ Set hold expiry (10min)  │
│                                 │
│  2. Create Booking (DB)         │
│     └─ Status: HELD             │
│                                 │
│  3. Return hold ID              │
└─────────────────────────────────┘
       │
       │ POST /bookings/confirm
       ▼
┌─────────────────────────────────┐
│   Booking Service               │
│                                 │
│  1. Validate hold not expired   │
│  2. Process payment             │
│  3. Confirm hold (Redis)        │
│  4. Update booking: CONFIRMED   │
└─────────────────────────────────┘
```

### Implementation Details

#### 1. Inventory Service with Redis

```java
@Service
public class InventoryService {
    
    public boolean holdTickets(Long ticketTypeId, 
                               Integer quantity, 
                               String holdId) {
        String availKey = "inventory:available:" + ticketTypeId;
        String holdKey = "inventory:hold:" + holdId;
        
        // Atomic check and decrement
        Long available = redisTemplate.opsForValue()
            .decrement(availKey, quantity);
        
        if (available < 0) {
            // Rollback
            redisTemplate.opsForValue().increment(availKey, quantity);
            return false; // Insufficient tickets
        }
        
        // Store hold information
        HoldInfo holdInfo = new HoldInfo(ticketTypeId, quantity, holdId);
        redisTemplate.opsForValue().set(
            holdKey, 
            holdInfo, 
            10, TimeUnit.MINUTES);  // ✅ 10-minute expiry
        
        return true;
    }
    
    public void confirmHold(String holdId, Long ticketTypeId) {
        String holdKey = "inventory:hold:" + holdId;
        String soldKey = "inventory:sold:" + ticketTypeId;
        
        HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);
        if (holdInfo != null) {
            // Move from held to sold
            redisTemplate.opsForValue().increment(soldKey, holdInfo.getQuantity());
            redisTemplate.delete(holdKey);
        }
    }
    
    public void releaseHold(String holdId, Long ticketTypeId) {
        String holdKey = "inventory:hold:" + holdId;
        String availKey = "inventory:available:" + ticketTypeId;
        
        HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);
        if (holdInfo != null) {
            // Return tickets to available pool
            redisTemplate.opsForValue().increment(availKey, holdInfo.getQuantity());
            redisTemplate.delete(holdKey);
        }
    }
}
```

#### 2. Booking Service

```java
@Service
public class BookingService {
    
    @Transactional
    public BookingResponse holdTickets(BookingRequest request) {
        // Step 1: Validate event and ticket type
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new RuntimeException("Event not found"));
        
        TicketType ticketType = ticketTypeRepository
            .findById(request.getTicketTypeId())
            .orElseThrow(() -> new RuntimeException("Ticket type not found"));
        
        // Step 2: Generate unique hold ID
        String holdId = UUID.randomUUID().toString();
        
        // Step 3: Hold tickets in Redis (atomic)
        boolean holdSuccess = inventoryService.holdTickets(
            request.getTicketTypeId(), 
            request.getQuantity(), 
            holdId);
        
        if (!holdSuccess) {
            throw new RuntimeException("Insufficient tickets available");
        }
        
        // Step 4: Create booking record
        BigDecimal totalAmount = ticketType.getPrice()
            .multiply(BigDecimal.valueOf(request.getQuantity()));
        
        Booking booking = new Booking(
            request.getUserId(), 
            event, 
            ticketType, 
            request.getQuantity(), 
            totalAmount);
        
        booking.setStatus(BookingStatus.HELD);
        booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10));
        booking = bookingRepository.save(booking);
        
        return new BookingResponse(
            booking.getId(), 
            holdId, 
            booking.getStatus(), 
            booking.getHoldExpiresAt(), 
            totalAmount);
    }
    
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, String paymentId) {
        // Step 1: Validate booking
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.HELD) {
            throw new RuntimeException("Invalid booking status");
        }
        
        if (booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new RuntimeException("Booking hold expired");
        }
        
        // Step 2: Process payment (idempotent)
        boolean paymentSuccess = paymentService.processPayment(
            paymentId, booking.getTotalAmount());
        
        if (!paymentSuccess) {
            throw new RuntimeException("Payment failed");
        }
        
        // Step 3: Confirm inventory hold
        inventoryService.confirmHold(paymentId, booking.getTicketType().getId());
        
        // Step 4: Update booking status
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking = bookingRepository.save(booking);
        
        return new BookingResponse(
            booking.getId(), 
            null, 
            booking.getStatus(), 
            null, 
            booking.getTotalAmount());
    }
}
```

#### 3. Automatic Hold Cleanup

```java
@Service
public class BookingService {
    
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cleanupExpiredHolds() {
        List<Booking> expiredBookings = bookingRepository
            .findExpiredHolds(BookingStatus.HELD, LocalDateTime.now());
        
        for (Booking booking : expiredBookings) {
            // Update booking status
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            
            // Release inventory hold
            inventoryService.releaseHold(
                booking.getId().toString(), 
                booking.getTicketType().getId());
            
            log.info("Expired booking: {}", booking.getId());
        }
    }
}
```

### Idempotency Guarantees

#### 1. Hold Operation

```
Scenario: Client retries hold request
│
Request 1:
├─ Hold 2 tickets (available: 100 → 98)
├─ Create booking (ID: 123, status: HELD)
└─ Return holdId: "abc-123"

Request 2 (retry with same data):
├─ Hold 2 tickets (available: 98 → 96) ❌ Double hold!
└─ Create booking (ID: 124, status: HELD)

Problem: Overselling
```

**Solution**: Add idempotency key to hold request.

```java
public BookingResponse holdTickets(BookingRequest request) {
    // Check if hold already exists
    String idempotencyKey = request.getIdempotencyKey();
    if (idempotencyKey != null) {
        Booking existingBooking = bookingRepository
            .findByIdempotencyKey(idempotencyKey);
        
        if (existingBooking != null) {
            return mapToResponse(existingBooking); // ✅ Return existing
        }
    }
    
    // Proceed with hold...
}
```

#### 2. Confirm Operation

```
Scenario: Client retries confirm request
│
Request 1:
├─ Validate booking (status: HELD)
├─ Process payment ($100 charged)
├─ Confirm hold (held → sold)
└─ Update booking (status: CONFIRMED)

Request 2 (retry):
├─ Validate booking (status: CONFIRMED)
└─ Return error: "Already confirmed" ✅

Result: Idempotent (no double charge)
```

**State Machine Protection:**

```java
if (booking.getStatus() == BookingStatus.CONFIRMED) {
    // Already confirmed, return success
    return mapToResponse(booking);
}

if (booking.getStatus() != BookingStatus.HELD) {
    throw new RuntimeException("Invalid booking status");
}
```

---

## Notification System

### Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /notifications
       │ { idempotencyKey: "notif-123", ... }
       ▼
┌─────────────────────────────────┐
│   Notification Service          │
│                                 │
│  1. Check Duplicate (Redis)     │
│     Key: "idempotency:notif-123"│
│                                 │
│  2. If duplicate → Return 200   │
│                                 │
│  3. If not duplicate:           │
│     ├─ Create notification      │
│     ├─ Mark as processed        │
│     ├─ Send to channels         │
│     └─ Return 200               │
└─────────────────────────────────┘
```

### Implementation Details

#### 1. Idempotency Service

```java
@Service
public class IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    
    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null) return false;
        
        String key = "idempotency:" + idempotencyKey;
        
        // Try to set key (returns false if already exists)
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        
        return result == null || !result; // true if duplicate
    }
    
    public void markAsProcessed(String notificationId) {
        String key = "processed:" + notificationId;
        redisTemplate.opsForValue()
            .set(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
    }
}
```

**Key Method: `setIfAbsent`**

```java
// Redis SETNX (SET if Not eXists)
Boolean result = redisTemplate.opsForValue()
    .setIfAbsent("idempotency:key-123", "1", 24, TimeUnit.HOURS);

// result = true  → Key was set (first request)
// result = false → Key already exists (duplicate request)
```

#### 2. Notification Service

```java
@Service
public class NotificationService {
    
    public NotificationResponse sendNotification(NotificationRequest request) {
        // Step 1: Check for duplicate
        if (idempotencyService.isDuplicate(request.getIdempotencyKey())) {
            log.info("Duplicate notification request: {}", 
                request.getIdempotencyKey());
            return new NotificationResponse("DUPLICATE", "Already processed");
        }
        
        // Step 2: Create notification record
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setChannels(request.getChannels());
        notification.setTemplateId(request.getTemplateId());
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);
        
        // Step 3: Mark as processed
        idempotencyService.markAsProcessed(notification.getId());
        
        // Step 4: Send to channels (async)
        for (String channel : request.getChannels()) {
            channelWorkerService.sendAsync(notification, channel);
        }
        
        return new NotificationResponse("SUCCESS", "Notification queued");
    }
}
```

### Edge Cases

#### 1. Concurrent Duplicate Requests

```
Time    Request 1                Request 2
────────────────────────────────────────────
T0      Check duplicate (false)  
T1      Create notification      Check duplicate (false)
T2      Mark processed           Create notification ❌
T3      Send notification        Mark processed
T4                               Send notification ❌

Result: Duplicate notifications sent
```

**Solution**: Use Redis `SETNX` atomically.

```java
// Atomic check-and-set
Boolean isFirst = redisTemplate.opsForValue()
    .setIfAbsent(key, "1", 24, TimeUnit.HOURS);

if (!isFirst) {
    return; // Duplicate, exit immediately
}

// Only first request reaches here
createAndSendNotification();
```

#### 2. Retry After Send Failure

```
Scenario: Notification sent but DB update fails
│
Request 1:
├─ Check duplicate → false
├─ Create notification
├─ Send email → Success ✅
└─ Mark processed → DB error ❌

Request 2 (retry):
├─ Check duplicate → false (marking failed)
├─ Send email → Duplicate email! ❌

Problem: User receives duplicate email
```

**Solution**: Mark as processed before sending.

```java
public NotificationResponse sendNotification(NotificationRequest request) {
    // Check duplicate
    if (isDuplicate(request.getIdempotencyKey())) {
        return success();
    }
    
    // Create notification
    notification = notificationRepository.save(notification);
    
    // Mark as processed BEFORE sending
    idempotencyService.markAsProcessed(notification.getId());
    
    // Now send (even if this fails, retry won't resend)
    try {
        channelWorkerService.send(notification);
    } catch (Exception e) {
        // Log error but don't throw
        // Notification is marked as processed
        log.error("Failed to send notification: {}", notification.getId(), e);
    }
    
    return success();
}
```

---

## Google Docs - Operational Transformation

### Architecture

```
┌─────────────┐
│   User A    │
└──────┬──────┘
       │ Edit: INSERT("hello", pos=5)
       ▼
┌─────────────────────────────────┐
│   Collaboration Service         │
│                                 │
│  1. Receive operation           │
│  2. Get pending operations      │
│  3. Transform against pending   │
│  4. Apply to document           │
│  5. Store in Redis queue        │
│  6. Broadcast to all users      │
└─────────────────────────────────┘
       │
       │ Broadcast
       ▼
┌─────────────┐
│   User B    │
└─────────────┘
```

### Implementation Details

#### 1. Operation Model

```java
public class Operation {
    private OperationType type;      // INSERT, DELETE, RETAIN
    private Integer position;        // Position in document
    private String text;             // Text to insert
    private Integer length;          // Length to delete
    private String userId;           // Who made the change
    private Long timestamp;          // When it was made
    private Integer version;         // Document version
}
```

#### 2. Operational Transformation

```java
@Component
public class OperationalTransform {
    
    public Operation transform(Operation op1, Operation op2) {
        if (op1.getType() == INSERT && op2.getType() == INSERT) {
            return transformInsertInsert(op1, op2);
        } else if (op1.getType() == INSERT && op2.getType() == DELETE) {
            return transformInsertDelete(op1, op2);
        } else if (op1.getType() == DELETE && op2.getType() == INSERT) {
            return transformDeleteInsert(op1, op2);
        } else if (op1.getType() == DELETE && op2.getType() == DELETE) {
            return transformDeleteDelete(op1, op2);
        }
        return op1;
    }
    
    private Operation transformInsertInsert(Operation op1, Operation op2) {
        if (op1.getPosition() < op2.getPosition()) {
            return op1; // No transformation needed
        } else if (op1.getPosition() > op2.getPosition()) {
            // Shift op1's position by op2's text length
            return Operation.builder()
                .type(INSERT)
                .position(op1.getPosition() + op2.getText().length())
                .text(op1.getText())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        } else {
            // Same position - use timestamp to break tie
            if (op1.getTimestamp() < op2.getTimestamp()) {
                return op1;
            } else {
                return Operation.builder()
                    .type(INSERT)
                    .position(op1.getPosition() + op2.getText().length())
                    .text(op1.getText())
                    .userId(op1.getUserId())
                    .timestamp(op1.getTimestamp())
                    .version(op1.getVersion())
                    .build();
            }
        }
    }
}
```

### Idempotency Through Versioning

```java
@Service
public class DocumentService {
    
    @Transactional
    public DocumentDTO updateDocument(String documentId, Operation operation) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // Get current content
        String cachedContent = (String) redisTemplate.opsForValue()
            .get("doc:" + documentId);
        String currentContent = cachedContent != null ? 
            cachedContent : document.getContent();
        
        // Get pending operations
        List<Operation> pendingOps = getPendingOperations(documentId);
        
        // Transform operation against pending operations
        for (Operation pendingOp : pendingOps) {
            if (!pendingOp.getUserId().equals(operation.getUserId())) {
                operation = operationalTransform.transform(operation, pendingOp);
            }
        }
        
        // Apply operation
        String newContent = operationalTransform.applyOperation(
            currentContent, operation);
        
        // Update document
        document.setContent(newContent);
        document.setUpdatedAt(LocalDateTime.now());
        document.setVersion(document.getVersion() + 1);
        document = documentRepository.save(document);
        
        // Cache new content
        redisTemplate.opsForValue().set("doc:" + documentId, newContent);
        
        // Store operation in queue
        redisTemplate.opsForList().rightPush("ops:" + documentId, operation);
        
        return toDTO(document);
    }
}
```

### Idempotency Guarantees

**Scenario: Duplicate Operation**

```
User A sends: INSERT("hello", pos=5, timestamp=1000)
│
├─ Server receives and applies
├─ Broadcasts to all users
└─ Network error, client doesn't receive ACK

User A retries: INSERT("hello", pos=5, timestamp=1000)
│
├─ Server receives duplicate
├─ Check pending operations
├─ Find operation with same timestamp and userId
└─ Skip application (idempotent) ✅
```

**Implementation:**

```java
public DocumentDTO updateDocument(String documentId, Operation operation) {
    // Check if operation already applied
    List<Operation> pendingOps = getPendingOperations(documentId);
    
    for (Operation pendingOp : pendingOps) {
        if (pendingOp.getUserId().equals(operation.getUserId()) &&
            pendingOp.getTimestamp().equals(operation.getTimestamp())) {
            // Duplicate operation, return current state
            return toDTO(document);
        }
    }
    
    // Apply operation...
}
```

---

## Uber - Ride Request Deduplication

### Architecture

```
┌─────────────┐
│   Rider     │
└──────┬──────┘
       │ POST /rides/request
       │ { riderId, pickup, dropoff }
       ▼
┌─────────────────────────────────┐
│   Ride Service                  │
│                                 │
│  1. Generate request hash       │
│     Hash(riderId + pickup +     │
│          dropoff + timestamp)   │
│                                 │
│  2. Check Redis for duplicate   │
│     Key: "ride:request:{hash}"  │
│                                 │
│  3. If duplicate → Return ride  │
│                                 │
│  4. If not duplicate:           │
│     ├─ Create ride request      │
│     ├─ Match with driver        │
│     ├─ Cache request (1h)       │
│     └─ Return ride              │
└─────────────────────────────────┘
```

### Implementation

```java
@Service
public class RideService {
    
    public RideResponse requestRide(RideRequest request) {
        // Generate request hash
        String requestHash = generateRequestHash(request);
        String cacheKey = "ride:request:" + requestHash;
        
        // Check for duplicate
        String cachedRideId = (String) redisTemplate.opsForValue()
            .get(cacheKey);
        
        if (cachedRideId != null) {
            // Return existing ride
            Ride ride = rideRepository.findById(cachedRideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
            return mapToResponse(ride);
        }
        
        // Create new ride request
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropoffLocation(request.getDropoffLocation());
        ride.setStatus(RideStatus.REQUESTED);
        ride = rideRepository.save(ride);
        
        // Cache request
        redisTemplate.opsForValue().set(
            cacheKey, 
            ride.getId(), 
            1, TimeUnit.HOURS);
        
        // Match with driver (async)
        matchingService.findDriver(ride);
        
        return mapToResponse(ride);
    }
    
    private String generateRequestHash(RideRequest request) {
        // Hash based on rider, locations, and time window
        String data = request.getRiderId() + 
                      request.getPickupLocation().toString() +
                      request.getDropoffLocation().toString() +
                      (System.currentTimeMillis() / 60000); // 1-minute window
        
        return DigestUtils.sha256Hex(data);
    }
}
```

---

## Instagram - Post Deduplication

### Architecture

```
┌─────────────┐
│   User      │
└──────┬──────┘
       │ POST /posts
       │ { userId, imageUrl, caption }
       ▼
┌─────────────────────────────────┐
│   Post Service                  │
│                                 │
│  1. Generate content hash       │
│     Hash(userId + imageUrl)     │
│                                 │
│  2. Check database for duplicate│
│     WHERE user_id = ? AND       │
│           content_hash = ?      │
│                                 │
│  3. If duplicate → Return post  │
│                                 │
│  4. If not duplicate:           │
│     ├─ Upload image to S3       │
│     ├─ Create post record       │
│     ├─ Generate feed entries    │
│     └─ Return post              │
└─────────────────────────────────┘
```

### Implementation

```java
@Service
public class PostService {
    
    @Transactional
    public PostResponse createPost(PostRequest request) {
        // Generate content hash
        String contentHash = generateContentHash(
            request.getUserId(), 
            request.getImageUrl());
        
        // Check for duplicate post
        Post existingPost = postRepository
            .findByUserIdAndContentHash(request.getUserId(), contentHash);
        
        if (existingPost != null) {
            // Return existing post
            return mapToResponse(existingPost);
        }
        
        // Upload image to S3
        String s3Url = s3Service.uploadImage(request.getImageUrl());
        
        // Create post
        Post post = new Post();
        post.setUserId(request.getUserId());
        post.setImageUrl(s3Url);
        post.setCaption(request.getCaption());
        post.setContentHash(contentHash);
        post = postRepository.save(post);
        
        // Generate feed entries (async)
        feedService.generateFeedEntries(post);
        
        return mapToResponse(post);
    }
    
    private String generateContentHash(String userId, String imageUrl) {
        return DigestUtils.sha256Hex(userId + imageUrl);
    }
}
```

**Database Schema:**

```sql
CREATE TABLE posts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    image_url VARCHAR(500),
    caption TEXT,
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, content_hash)  -- ✅ Prevents duplicate posts
);

CREATE INDEX idx_posts_user_hash ON posts(user_id, content_hash);
```

---

## Best Practices Summary

### 1. Always Use Idempotency Keys for Critical Operations

```java
// ✅ Good: Client-provided idempotency key
POST /payments
Headers:
  Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Body:
  { "amount": 100 }

// ❌ Bad: No idempotency protection
POST /payments
Body:
  { "amount": 100 }
```

### 2. Cache Responses, Not Just Flags

```java
// ✅ Good: Cache full response
cache.put(key, {
    transactionId: "txn-123",
    status: "COMPLETED",
    amount: 100.00
});

// ❌ Bad: Cache only flag
cache.put(key, "processed");
// Client can't get transaction details on retry
```

### 3. Use Appropriate TTL

```java
// ✅ Good: 24-hour TTL for payments
cache.put(key, response, 24, TimeUnit.HOURS);

// ❌ Bad: No TTL (unbounded growth)
cache.put(key, response);

// ❌ Bad: Too short TTL (5 minutes)
cache.put(key, response, 5, TimeUnit.MINUTES);
// Client retry after 10 minutes → duplicate charge
```

### 4. Two-Layer Caching for Durability

```java
// ✅ Good: Redis + Database
redisTemplate.opsForValue().set(key, response, 24, TimeUnit.HOURS);
cacheRepository.save(new IdempotencyCache(key, response));

// ❌ Bad: Redis only
redisTemplate.opsForValue().set(key, response, 24, TimeUnit.HOURS);
// Redis restart → lost cache → duplicate processing
```

### 5. Database Constraints as Last Line of Defense

```sql
-- ✅ Good: Unique constraint
CREATE UNIQUE INDEX idx_transaction_idempotency 
ON transactions(idempotency_key);

-- ✅ Good: Composite unique constraint
CREATE UNIQUE INDEX idx_booking_user_event 
ON bookings(user_id, event_id, created_date);
```

### 6. Atomic Operations with Redis

```java
// ✅ Good: Atomic check-and-set
Boolean isFirst = redisTemplate.opsForValue()
    .setIfAbsent(key, "1", 24, TimeUnit.HOURS);

if (!isFirst) {
    return cachedResponse;
}

// ❌ Bad: Non-atomic check-then-set
if (redisTemplate.hasKey(key)) {
    return cachedResponse;
}
redisTemplate.opsForValue().set(key, "1");
// Race condition: Two requests can both pass the check
```

### 7. Handle Partial Failures

```java
// ✅ Good: Cache before external call
cache.put(key, "PROCESSING");
result = externalService.call();
cache.put(key, result);

// ❌ Bad: Cache after external call
result = externalService.call();
cache.put(key, result);
// If caching fails, retry will duplicate external call
```

### 8. State Machine Protection

```java
// ✅ Good: Check current state
if (transaction.getStatus() == COMPLETED) {
    return success(); // Already completed, idempotent
}

if (transaction.getStatus() != PENDING) {
    throw new InvalidStateException();
}

// ❌ Bad: No state check
transaction.setStatus(COMPLETED);
// Can transition from any state to COMPLETED
```

### 9. Pessimistic Locking for Critical Sections

```java
// ✅ Good: Lock before modification
@Lock(LockModeType.PESSIMISTIC_WRITE)
Wallet wallet = walletRepository.findById(id);
wallet.setBalance(wallet.getBalance().subtract(amount));

// ❌ Bad: No locking
Wallet wallet = walletRepository.findById(id);
wallet.setBalance(wallet.getBalance().subtract(amount));
// Concurrent requests can cause lost updates
```

### 10. Idempotent Cleanup Jobs

```java
// ✅ Good: Idempotent cleanup
@Scheduled(fixedRate = 60000)
public void cleanupExpiredHolds() {
    List<Booking> expired = bookingRepository
        .findExpiredHolds(HELD, LocalDateTime.now());
    
    for (Booking booking : expired) {
        if (booking.getStatus() == HELD) {  // ✅ Check state
            booking.setStatus(EXPIRED);
            inventoryService.releaseHold(booking.getId());
        }
    }
}

// ❌ Bad: Non-idempotent cleanup
@Scheduled(fixedRate = 60000)
public void cleanupExpiredHolds() {
    List<Booking> expired = bookingRepository.findExpiredHolds();
    
    for (Booking booking : expired) {
        booking.setStatus(EXPIRED);  // ❌ No state check
        inventoryService.releaseHold(booking.getId());
        // If job runs twice, releases hold twice
    }
}
```

---

## Testing Idempotency

### Unit Tests

```java
@Test
public void testPaymentIdempotency() {
    PaymentRequest request = new PaymentRequest(100.00, "USD");
    String idempotencyKey = "test-key-123";
    
    // First request
    PaymentResponse response1 = paymentService.processPayment(
        request, idempotencyKey);
    
    assertEquals("COMPLETED", response1.getStatus());
    UUID txnId1 = response1.getTransactionId();
    
    // Second request (duplicate)
    PaymentResponse response2 = paymentService.processPayment(
        request, idempotencyKey);
    
    assertEquals("COMPLETED", response2.getStatus());
    UUID txnId2 = response2.getTransactionId();
    
    // Should return same transaction
    assertEquals(txnId1, txnId2);
    
    // Verify only one transaction in database
    long count = transactionRepository.count();
    assertEquals(1, count);
}
```

### Integration Tests

```java
@Test
public void testConcurrentPaymentRequests() throws Exception {
    PaymentRequest request = new PaymentRequest(100.00, "USD");
    String idempotencyKey = "concurrent-test-123";
    
    int numThreads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    
    List<Future<PaymentResponse>> futures = new ArrayList<>();
    
    // Submit concurrent requests
    for (int i = 0; i < numThreads; i++) {
        Future<PaymentResponse> future = executor.submit(() -> {
            latch.countDown();
            latch.await(); // Wait for all threads to be ready
            return paymentService.processPayment(request, idempotencyKey);
        });
        futures.add(future);
    }
    
    // Collect responses
    Set<UUID> transactionIds = new HashSet<>();
    for (Future<PaymentResponse> future : futures) {
        PaymentResponse response = future.get();
        transactionIds.add(response.getTransactionId());
    }
    
    // All requests should return same transaction ID
    assertEquals(1, transactionIds.size());
    
    // Verify only one transaction in database
    long count = transactionRepository.count();
    assertEquals(1, count);
    
    executor.shutdown();
}
```

### Load Tests

```javascript
// k6 load test script
import http from 'k6/http';
import { check } from 'k6';

export let options = {
    vus: 100,
    duration: '30s',
};

export default function() {
    let idempotencyKey = `key-${__VU}-${__ITER}`;
    
    let payload = JSON.stringify({
        amount: 100.00,
        currency: 'USD'
    });
    
    let params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': idempotencyKey
        }
    };
    
    // First request
    let res1 = http.post('http://localhost:8082/api/payments', payload, params);
    check(res1, {
        'status is 200': (r) => r.status === 200,
    });
    
    // Duplicate request
    let res2 = http.post('http://localhost:8082/api/payments', payload, params);
    check(res2, {
        'status is 200': (r) => r.status === 200,
        'same transaction ID': (r) => {
            let txn1 = JSON.parse(res1.body).transactionId;
            let txn2 = JSON.parse(r.body).transactionId;
            return txn1 === txn2;
        }
    });
}
```

---

## Monitoring and Alerting

### Metrics to Track

```java
@Service
public class PaymentService {
    
    private final Counter duplicateRequestCounter = Counter.builder("payment.duplicate.requests")
        .description("Number of duplicate payment requests")
        .register(meterRegistry);
    
    private final Timer cacheHitTimer = Timer.builder("payment.cache.hit")
        .description("Time to retrieve cached response")
        .register(meterRegistry);
    
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        PaymentResponse cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);
        
        if (cachedResponse != null) {
            duplicateRequestCounter.increment();
            cacheHitTimer.record(() -> {
                // Track cache hit latency
            });
            return cachedResponse;
        }
        
        // Process payment...
    }
}
```

### Alerts

```yaml
# Prometheus alert rules
groups:
  - name: idempotency
    rules:
      - alert: HighDuplicateRequestRate
        expr: rate(payment_duplicate_requests_total[5m]) > 100
        for: 5m
        annotations:
          summary: "High rate of duplicate payment requests"
          description: "{{ $value }} duplicate requests per second"
      
      - alert: CacheMissRate
        expr: rate(payment_cache_miss_total[5m]) / rate(payment_requests_total[5m]) > 0.5
        for: 10m
        annotations:
          summary: "High cache miss rate"
          description: "Cache miss rate is {{ $value }}"
```

---

## Conclusion

Idempotency is critical for building reliable distributed systems. Key takeaways:

1. **Always use idempotency keys** for critical operations (payments, bookings, notifications)
2. **Cache full responses**, not just flags
3. **Use appropriate TTL** (24 hours for financial operations)
4. **Two-layer caching** (Redis + Database) for durability
5. **Database constraints** as last line of defense
6. **Atomic operations** with Redis SETNX
7. **Handle partial failures** gracefully
8. **State machine protection** for status transitions
9. **Pessimistic locking** for critical sections
10. **Test thoroughly** with unit, integration, and load tests

Implementing idempotency correctly prevents:
- Double charges ($100 → $200)
- Overselling (2 tickets for 1 seat)
- Duplicate notifications (spam)
- Data inconsistencies
- Customer complaints and refunds

The cost of implementing idempotency is minimal compared to the cost of failures.

---

**Document Version**: 1.0  
**Last Updated**: 2024-01-15  
**Author**: Sudhir Meena
