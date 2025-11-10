# Idempotency Quick Reference Guide

## What is Idempotency?

**Idempotency** = Performing the same operation multiple times produces the same result as performing it once.

```
f(f(x)) = f(x)
```

---

## Why It Matters

| Without Idempotency | With Idempotency |
|---------------------|------------------|
| Double charge: $100 → $200 | Single charge: $100 |
| Overselling: 2 tickets for 1 seat | Correct: 1 ticket |
| Spam: 10 duplicate emails | Single email |
| Duplicate accounts | Single account |

---

## Implementation Methods

### 1. Idempotency Keys (Most Common)

```java
// Client generates unique key
String idempotencyKey = UUID.randomUUID().toString();

// Server checks cache
if (cache.contains(idempotencyKey)) {
    return cache.get(idempotencyKey);
}

// Process and cache
result = process(request);
cache.put(idempotencyKey, result, TTL=24h);
```

**Used in**: Payment Service, Digital Payment, Notification System

### 2. Database Constraints

```sql
CREATE UNIQUE INDEX idx_transaction_idempotency 
ON transactions(idempotency_key);
```

**Used in**: All systems as last line of defense

### 3. Distributed Locks

```java
boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);

if (!acquired) {
    throw new ConcurrentModificationException();
}
```

**Used in**: Ticket Booking, Inventory Management

### 4. State Machines

```java
if (transaction.getStatus() == COMPLETED) {
    return success(); // Idempotent
}
```

**Used in**: All systems for status transitions

---

## System-Specific Implementations

### Payment Service

```java
@Transactional
public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
    // Check cache (Redis + Database)
    PaymentResponse cached = idempotencyService.getCachedResponse(idempotencyKey);
    if (cached != null) return cached;
    
    // Process payment
    transaction = createAndProcessTransaction(request);
    
    // Cache response (24h TTL)
    idempotencyService.cacheResponse(idempotencyKey, response, transaction.getId());
    
    return response;
}
```

**Key Features**:
- Two-layer caching (Redis + PostgreSQL)
- 24-hour TTL
- Full response caching

### Digital Payment Platform

```java
public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request) {
    // Check Redis cache
    if (idempotencyKey != null) {
        Object cached = redisTemplate.opsForValue().get("idempotency:" + idempotencyKey);
        if (cached != null) return (PaymentInitiationResponse) cached;
    }
    
    // Process payment
    response = processPayment(request);
    
    // Cache in Redis (24h)
    redisTemplate.opsForValue().set("idempotency:" + idempotencyKey, response, 24, TimeUnit.HOURS);
    
    return response;
}
```

**Key Features**:
- Redis-only caching
- Wallet transfers with pessimistic locking
- Atomic debit/credit operations

### Ticket Booking System

```java
@Transactional
public BookingResponse holdTickets(BookingRequest request) {
    // Check for existing booking
    if (request.getIdempotencyKey() != null) {
        Booking existing = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing != null) return mapToResponse(existing);
    }
    
    // Hold tickets (atomic Redis operation)
    String holdId = UUID.randomUUID().toString();
    boolean success = inventoryService.holdTickets(ticketTypeId, quantity, holdId);
    
    // Create booking
    booking = bookingRepository.save(booking);
    
    return mapToResponse(booking);
}
```

**Key Features**:
- Redis atomic operations (DECR)
- 10-minute hold expiry
- Automatic cleanup job

### Notification System

```java
public NotificationResponse sendNotification(NotificationRequest request) {
    // Atomic check-and-set
    Boolean isFirst = redisTemplate.opsForValue()
        .setIfAbsent("idempotency:" + request.getIdempotencyKey(), "1", 24, TimeUnit.HOURS);
    
    if (!isFirst) {
        return new NotificationResponse("DUPLICATE", "Already processed");
    }
    
    // Send notification
    sendToChannels(request);
    
    return new NotificationResponse("SUCCESS", "Sent");
}
```

**Key Features**:
- Redis SETNX for atomic deduplication
- Mark as processed before sending
- 24-hour TTL

### Google Docs

```java
public DocumentDTO updateDocument(String documentId, Operation operation) {
    // Check for duplicate operation (same userId + timestamp)
    List<Operation> pending = getPendingOperations(documentId);
    for (Operation op : pending) {
        if (op.getUserId().equals(operation.getUserId()) &&
            op.getTimestamp().equals(operation.getTimestamp())) {
            return toDTO(document); // Duplicate, skip
        }
    }
    
    // Transform and apply operation
    operation = transformAgainstPending(operation, pending);
    applyOperation(document, operation);
    
    return toDTO(document);
}
```

**Key Features**:
- Operational Transformation
- Timestamp-based deduplication
- Version-based conflict resolution

---

## Best Practices Checklist

### ✅ Do's

1. **Always use idempotency keys** for critical operations
2. **Cache full responses**, not just flags
3. **Use 24-hour TTL** for financial operations
4. **Implement two-layer caching** (Redis + Database)
5. **Add database unique constraints** as last defense
6. **Use atomic operations** (Redis SETNX, database transactions)
7. **Handle partial failures** gracefully
8. **Check state before transitions**
9. **Use pessimistic locking** for critical sections
10. **Test with concurrent requests**

### ❌ Don'ts

1. **Don't skip idempotency** for "rare" scenarios
2. **Don't use short TTL** (< 1 hour) for retries
3. **Don't cache only in Redis** without database backup
4. **Don't ignore race conditions**
5. **Don't cache failures** (only success responses)
6. **Don't forget to clean up** expired entries
7. **Don't use non-atomic check-then-set**
8. **Don't allow invalid state transitions**
9. **Don't process without validation**
10. **Don't skip testing** edge cases

---

## Common Patterns

### Pattern 1: Check-Process-Cache

```java
// 1. Check cache
if (cached) return cached;

// 2. Process
result = process();

// 3. Cache
cache.put(key, result);
```

### Pattern 2: Lock-Process-Release

```java
// 1. Acquire lock
if (!lock.tryAcquire()) throw error;

try {
    // 2. Process
    result = process();
} finally {
    // 3. Release lock
    lock.release();
}
```

### Pattern 3: Atomic Check-and-Set

```java
// Single atomic operation
Boolean isFirst = redis.setIfAbsent(key, value, ttl);
if (!isFirst) return cached;
```

### Pattern 4: State Machine Guard

```java
// Check current state
if (entity.getStatus() == FINAL_STATE) {
    return success(); // Already done
}

// Validate transition
if (!isValidTransition(current, next)) {
    throw error;
}

// Update state
entity.setStatus(next);
```

---

## Testing Checklist

### Unit Tests
- [ ] Single request succeeds
- [ ] Duplicate request returns same result
- [ ] Concurrent requests handled correctly
- [ ] Cache expiry works
- [ ] State transitions validated

### Integration Tests
- [ ] Redis failure fallback to database
- [ ] Database constraint prevents duplicates
- [ ] Partial failure recovery
- [ ] Cleanup jobs work correctly

### Load Tests
- [ ] High concurrency (100+ threads)
- [ ] Retry scenarios
- [ ] Cache hit ratio > 95%
- [ ] No duplicate processing

---

## Monitoring Metrics

```java
// Track duplicate requests
Counter duplicateCounter = Counter.builder("requests.duplicate")
    .description("Number of duplicate requests")
    .register(registry);

// Track cache hits
Counter cacheHitCounter = Counter.builder("cache.hits")
    .description("Number of cache hits")
    .register(registry);

// Track processing time
Timer processingTimer = Timer.builder("processing.time")
    .description("Time to process request")
    .register(registry);
```

### Key Metrics to Monitor

1. **Duplicate Request Rate**: Should be < 5% of total requests
2. **Cache Hit Ratio**: Should be > 95%
3. **Processing Latency**: p99 < 200ms
4. **Error Rate**: Should be < 0.1%
5. **Cache Size**: Monitor for unbounded growth

---

## Troubleshooting

### Problem: Duplicate Charges

**Symptoms**: Multiple transactions for same idempotency key

**Causes**:
- Cache not checked before processing
- Cache expired too quickly
- Race condition in check-then-set

**Solutions**:
- Verify cache check logic
- Increase TTL to 24 hours
- Use atomic operations (SETNX)

### Problem: Cache Misses

**Symptoms**: High duplicate request rate but low cache hits

**Causes**:
- Redis down or restarting
- TTL too short
- Cache key mismatch

**Solutions**:
- Implement database fallback
- Increase TTL
- Verify key generation logic

### Problem: Overselling

**Symptoms**: More tickets sold than available

**Causes**:
- Non-atomic inventory check
- Race condition in hold logic
- Missing database constraints

**Solutions**:
- Use Redis atomic operations (DECR)
- Add pessimistic locking
- Add unique constraints

---

## Quick Decision Tree

```
Need idempotency?
│
├─ Financial operation (payment, transfer)?
│  └─ Use: Idempotency key + Two-layer cache + 24h TTL
│
├─ Inventory operation (booking, reservation)?
│  └─ Use: Hold mechanism + Redis atomic ops + Cleanup job
│
├─ Notification/Message?
│  └─ Use: Redis SETNX + Mark before send
│
├─ Content creation (post, document)?
│  └─ Use: Content hash + Database unique constraint
│
└─ State transition?
   └─ Use: State machine + Validation
```

---

## Code Templates

### Template 1: Payment Service

```java
@Transactional
public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
    // Check cache
    PaymentResponse cached = cache.get(idempotencyKey);
    if (cached != null) return cached;
    
    // Create transaction
    Transaction txn = new Transaction(request, idempotencyKey);
    txn = repository.save(txn);
    
    try {
        // Process
        String pspTxnId = processor.process(txn);
        txn.setStatus(COMPLETED);
        txn.setPspTransactionId(pspTxnId);
        txn = repository.save(txn);
        
        // Build response
        PaymentResponse response = mapToResponse(txn);
        
        // Cache
        cache.put(idempotencyKey, response, 24, TimeUnit.HOURS);
        
        return response;
    } catch (Exception e) {
        txn.setStatus(FAILED);
        repository.save(txn);
        throw e;
    }
}
```

### Template 2: Notification Service

```java
public NotificationResponse send(NotificationRequest request) {
    // Atomic deduplication
    Boolean isFirst = redis.setIfAbsent(
        "idempotency:" + request.getIdempotencyKey(), 
        "1", 
        24, TimeUnit.HOURS);
    
    if (!isFirst) {
        return new NotificationResponse("DUPLICATE");
    }
    
    // Create notification
    Notification notif = repository.save(new Notification(request));
    
    // Send (async)
    channelService.sendAsync(notif);
    
    return new NotificationResponse("SUCCESS");
}
```

### Template 3: Booking Service

```java
@Transactional
public BookingResponse hold(BookingRequest request) {
    // Check existing
    if (request.getIdempotencyKey() != null) {
        Booking existing = repository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing != null) return mapToResponse(existing);
    }
    
    // Hold inventory (atomic)
    String holdId = UUID.randomUUID().toString();
    boolean success = inventory.hold(ticketTypeId, quantity, holdId);
    
    if (!success) {
        throw new InsufficientInventoryException();
    }
    
    // Create booking
    Booking booking = new Booking(request, holdId);
    booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10));
    booking = repository.save(booking);
    
    return mapToResponse(booking);
}
```

---

## Summary

**Idempotency is not optional** for production systems. It prevents:
- Financial losses (double charges, refunds)
- Customer complaints (duplicate notifications)
- Data corruption (overselling, inconsistencies)
- Legal issues (compliance violations)

**Implementation cost**: Low (few hours of development)  
**Failure cost**: High (thousands to millions in losses)

**ROI**: 100x+

---

**For detailed explanations, see**: [IDEMPOTENCY_AND_DUPLICATE_HANDLING.md](IDEMPOTENCY_AND_DUPLICATE_HANDLING.md)

**Author**: Sudhir Meena  
**Last Updated**: 2024-01-15
