# Multi-Region Payment System with Strong Consistency

## Problem Statement
You're designing a payment system (like Stripe). Transactions must be consistent across multiple regions. How would you scale while keeping consistency?

## Requirements
- **Strong consistency** - No double charges, no lost payments
- **Multi-region** - Low latency globally
- **High availability** - 99.99% uptime
- **ACID guarantees** - Atomic transactions
- **Idempotency** - Duplicate request handling
- **Audit trail** - Complete transaction history

---

## Key Challenges

1. **CAP Theorem** - Can't have all three (Consistency, Availability, Partition tolerance)
2. **Network latency** - Cross-region writes are slow (100-300ms)
3. **Split-brain** - Multiple regions accepting writes
4. **Eventual consistency** - Not acceptable for payments
5. **Distributed transactions** - 2PC/3PC complexity

---

## Solution Architecture

### Strategy: Single-Region Writes + Multi-Region Reads

```
Write Path (Strong Consistency):
User → Nearest Region → Primary Region (US-EAST) → Sync Replication → Response

Read Path (Low Latency):
User → Nearest Region → Local Read Replica → Response
```

**Key Principle**: Sacrifice write latency for consistency, optimize read latency

---

## 1. Primary-Replica Architecture with Synchronous Replication

### Architecture

```
                    ┌─────────────────┐
                    │   US-EAST-1     │
                    │  (Primary DB)   │
                    │  Write Master   │
                    └─────────────────┘
                            ↓ (sync replication)
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  US-WEST-1   │    │   EU-WEST-1  │    │  AP-SOUTH-1  │
│ (Read Replica)│    │(Read Replica)│    │(Read Replica)│
└──────────────┘    └──────────────┘    └──────────────┘
```

### Implementation

```java
@Configuration
public class MultiRegionDataSourceConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://primary-us-east.rds.amazonaws.com:5432/payments");
        config.setUsername("admin");
        config.setPassword(System.getenv("DB_PASSWORD"));
        
        // Synchronous replication settings
        config.addDataSourceProperty("synchronous_commit", "remote_apply");
        config.addDataSourceProperty("synchronous_standby_names", "replica1,replica2");
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public DataSource readReplicaDataSource() {
        // Route to nearest read replica based on user location
        return new RoutingDataSource(Map.of(
            "US-WEST", usWestReplica(),
            "EU-WEST", euWestReplica(),
            "AP-SOUTH", apSouthReplica()
        ));
    }
}

@Service
public class PaymentService {
    
    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;
    
    @Autowired
    @Qualifier("readReplicaDataSource")
    private DataSource readReplicaDataSource;
    
    // All writes go to primary
    @Transactional
    public Payment processPayment(PaymentRequest request) {
        JdbcTemplate primary = new JdbcTemplate(primaryDataSource);
        
        // Idempotency check
        Payment existing = checkIdempotency(request.getIdempotencyKey());
        if (existing != null) {
            return existing;
        }
        
        // Create payment record
        Payment payment = primary.queryForObject(
            "INSERT INTO payments (id, user_id, amount, currency, status, idempotency_key, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW()) RETURNING *",
            paymentRowMapper,
            UUID.randomUUID(), request.getUserId(), request.getAmount(), 
            request.getCurrency(), "PENDING", request.getIdempotencyKey()
        );
        
        return payment;
    }
    
    // Reads from nearest replica
    @Transactional(readOnly = true)
    public Payment getPayment(String paymentId) {
        JdbcTemplate replica = new JdbcTemplate(readReplicaDataSource);
        return replica.queryForObject(
            "SELECT * FROM payments WHERE id = ?",
            paymentRowMapper,
            paymentId
        );
    }
}
```

### PostgreSQL Synchronous Replication

```sql
-- postgresql.conf on primary
synchronous_commit = remote_apply
synchronous_standby_names = 'replica1,replica2'

-- This ensures:
-- 1. Transaction commits only after replicas acknowledge
-- 2. Zero data loss on primary failure
-- 3. Strong consistency across regions
```

---

## 2. Distributed Transactions with Two-Phase Commit (2PC)

For operations spanning multiple databases (e.g., debit account + credit account):

### Implementation

```java
@Service
public class DistributedPaymentService {
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Transactional
    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        
        txTemplate.execute(status -> {
            try {
                // Phase 1: Prepare
                boolean debitPrepared = prepareDebit(fromAccountId, amount);
                boolean creditPrepared = prepareCredit(toAccountId, amount);
                
                if (!debitPrepared || !creditPrepared) {
                    status.setRollbackOnly();
                    return null;
                }
                
                // Phase 2: Commit
                commitDebit(fromAccountId, amount);
                commitCredit(toAccountId, amount);
                
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
    
    private boolean prepareDebit(String accountId, BigDecimal amount) {
        // Lock account and verify balance
        Integer updated = jdbcTemplate.update(
            "UPDATE accounts SET reserved_balance = reserved_balance + ? " +
            "WHERE account_id = ? AND balance >= ?",
            amount, accountId, amount
        );
        return updated > 0;
    }
    
    private void commitDebit(String accountId, BigDecimal amount) {
        jdbcTemplate.update(
            "UPDATE accounts SET balance = balance - ?, reserved_balance = reserved_balance - ? " +
            "WHERE account_id = ?",
            amount, amount, accountId
        );
    }
}
```

---

## 3. Saga Pattern for Long-Running Transactions

For complex payment flows (payment → fraud check → settlement):

### Choreography-Based Saga

```java
@Service
public class PaymentSagaOrchestrator {
    
    @Autowired
    private KafkaTemplate<String, SagaEvent> kafkaTemplate;
    
    public void initiatePayment(PaymentRequest request) {
        // Step 1: Create payment
        Payment payment = createPayment(request);
        
        // Publish event
        kafkaTemplate.send("payment-events", new PaymentCreatedEvent(payment));
    }
    
    @KafkaListener(topics = "payment-events")
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        try {
            // Step 2: Fraud check
            FraudCheckResult result = fraudCheckService.check(event.getPayment());
            
            if (result.isPassed()) {
                kafkaTemplate.send("payment-events", new FraudCheckPassedEvent(event.getPayment()));
            } else {
                // Compensating transaction
                kafkaTemplate.send("payment-events", new PaymentFailedEvent(event.getPayment()));
            }
        } catch (Exception e) {
            // Compensating transaction
            kafkaTemplate.send("payment-events", new PaymentFailedEvent(event.getPayment()));
        }
    }
    
    @KafkaListener(topics = "payment-events")
    public void handleFraudCheckPassed(FraudCheckPassedEvent event) {
        // Step 3: Charge payment processor
        try {
            ProcessorResponse response = paymentProcessor.charge(event.getPayment());
            kafkaTemplate.send("payment-events", new PaymentSucceededEvent(event.getPayment()));
        } catch (Exception e) {
            // Compensating transaction - refund
            kafkaTemplate.send("payment-events", new PaymentFailedEvent(event.getPayment()));
        }
    }
    
    @KafkaListener(topics = "payment-events")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Compensating transaction - rollback
        jdbcTemplate.update(
            "UPDATE payments SET status = 'FAILED' WHERE id = ?",
            event.getPayment().getId()
        );
    }
}
```

---

## 4. Idempotency for Exactly-Once Processing

### Database Schema

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_idempotency_key ON payments(idempotency_key);

CREATE TABLE idempotency_cache (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    response_body TEXT NOT NULL,
    status_code INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_cache(expires_at);
```

### Implementation

```java
@Service
public class IdempotencyService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final int CACHE_TTL_HOURS = 24;
    
    public <T> T executeIdempotent(String idempotencyKey, Supplier<T> operation) {
        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        
        // Check Redis cache first
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);
        }
        
        // Check database
        try {
            String dbCached = jdbcTemplate.queryForObject(
                "SELECT response_body FROM idempotency_cache WHERE idempotency_key = ? AND expires_at > NOW()",
                String.class,
                idempotencyKey
            );
            if (dbCached != null) {
                // Restore to Redis
                redisTemplate.opsForValue().set(cacheKey, dbCached, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return deserialize(dbCached);
            }
        } catch (EmptyResultDataAccessException e) {
            // Not found - proceed with operation
        }
        
        // Execute operation
        T result = operation.get();
        
        // Cache result
        String serialized = serialize(result);
        redisTemplate.opsForValue().set(cacheKey, serialized, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        // Persist to database
        jdbcTemplate.update(
            "INSERT INTO idempotency_cache (idempotency_key, response_body, status_code, created_at, expires_at) " +
            "VALUES (?, ?, ?, NOW(), NOW() + INTERVAL '24 hours')",
            idempotencyKey, serialized, 200
        );
        
        return result;
    }
}

@RestController
public class PaymentController {
    
    @PostMapping("/payments")
    public ResponseEntity<Payment> createPayment(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody PaymentRequest request
    ) {
        Payment payment = idempotencyService.executeIdempotent(
            idempotencyKey,
            () -> paymentService.processPayment(request)
        );
        
        return ResponseEntity.ok(payment);
    }
}
```

---

## 5. Event Sourcing for Audit Trail

### Schema

```sql
CREATE TABLE payment_events (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL
);

CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id, created_at);

-- Materialized view for current state
CREATE MATERIALIZED VIEW payment_current_state AS
SELECT 
    payment_id,
    (event_data->>'status') as status,
    (event_data->>'amount')::DECIMAL as amount,
    MAX(created_at) as last_updated
FROM payment_events
GROUP BY payment_id, event_data->>'status', event_data->>'amount';
```

### Implementation

```java
@Service
public class PaymentEventStore {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void appendEvent(UUID paymentId, String eventType, Map<String, Object> eventData) {
        jdbcTemplate.update(
            "INSERT INTO payment_events (event_id, payment_id, event_type, event_data, created_at, created_by) " +
            "VALUES (?, ?, ?, ?::jsonb, NOW(), ?)",
            UUID.randomUUID(), paymentId, eventType, 
            new ObjectMapper().writeValueAsString(eventData),
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }
    
    public List<PaymentEvent> getEventHistory(UUID paymentId) {
        return jdbcTemplate.query(
            "SELECT * FROM payment_events WHERE payment_id = ? ORDER BY created_at",
            paymentEventRowMapper,
            paymentId
        );
    }
    
    public Payment reconstructState(UUID paymentId) {
        List<PaymentEvent> events = getEventHistory(paymentId);
        
        Payment payment = new Payment();
        events.forEach(event -> {
            switch (event.getEventType()) {
                case "PAYMENT_CREATED":
                    payment.setId(paymentId);
                    payment.setAmount(event.getData().get("amount"));
                    payment.setStatus("PENDING");
                    break;
                case "PAYMENT_AUTHORIZED":
                    payment.setStatus("AUTHORIZED");
                    break;
                case "PAYMENT_CAPTURED":
                    payment.setStatus("SUCCEEDED");
                    break;
                case "PAYMENT_FAILED":
                    payment.setStatus("FAILED");
                    break;
            }
        });
        
        return payment;
    }
}
```

---

## 6. Multi-Region Failover Strategy

### Active-Passive Failover

```java
@Service
public class FailoverService {
    
    @Autowired
    private HealthCheckService healthCheckService;
    
    private volatile String activePrimaryRegion = "US-EAST-1";
    private volatile String standbyRegion = "US-WEST-1";
    
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void monitorPrimaryHealth() {
        if (!healthCheckService.isPrimaryHealthy(activePrimaryRegion)) {
            log.error("Primary region {} is unhealthy. Initiating failover...", activePrimaryRegion);
            failover();
        }
    }
    
    private synchronized void failover() {
        // Promote standby to primary
        String newPrimary = standbyRegion;
        String newStandby = activePrimaryRegion;
        
        // Update DNS to point to new primary
        dnsService.updatePrimaryEndpoint(newPrimary);
        
        // Promote replica to master
        databaseService.promoteReplica(newPrimary);
        
        // Update routing
        activePrimaryRegion = newPrimary;
        standbyRegion = newStandby;
        
        log.info("Failover completed. New primary: {}", newPrimary);
    }
}
```

---

## 7. Optimistic Locking for Concurrent Updates

```sql
CREATE TABLE accounts (
    account_id UUID PRIMARY KEY,
    balance DECIMAL(19, 4) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);
```

```java
@Service
public class AccountService {
    
    @Transactional
    public void debitAccount(UUID accountId, BigDecimal amount) {
        Account account = jdbcTemplate.queryForObject(
            "SELECT * FROM accounts WHERE account_id = ? FOR UPDATE",
            accountRowMapper,
            accountId
        );
        
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        // Optimistic locking - update only if version matches
        int updated = jdbcTemplate.update(
            "UPDATE accounts SET balance = balance - ?, version = version + 1, updated_at = NOW() " +
            "WHERE account_id = ? AND version = ?",
            amount, accountId, account.getVersion()
        );
        
        if (updated == 0) {
            throw new OptimisticLockException("Account was modified by another transaction");
        }
    }
}
```

---

## 8. Complete Multi-Region Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Global Load Balancer                      │
│              (Route 53 / CloudFlare DNS)                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│   US-EAST-1      │                  │   EU-WEST-1      │
│  (Primary Write) │                  │  (Read Replica)  │
│                  │                  │                  │
│  API Gateway     │                  │  API Gateway     │
│       ↓          │                  │       ↓          │
│  Payment Service │                  │  Payment Service │
│       ↓          │                  │       ↓          │
│  PostgreSQL      │──sync repl──────→│  PostgreSQL      │
│  (Master)        │                  │  (Replica)       │
│       ↓          │                  │                  │
│  Redis Cache     │                  │  Redis Cache     │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
        └───────────────────┬───────────────────┘
                            ↓
                  ┌──────────────────┐
                  │   Kafka Cluster  │
                  │  (Event Stream)  │
                  └──────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│  Event Store     │                  │  Analytics DB    │
│  (Audit Trail)   │                  │  (TimescaleDB)   │
└──────────────────┘                  └──────────────────┘
```

---

## 9. Consistency Guarantees

### Strong Consistency (Synchronous Replication)

```yaml
# Trade-offs
Pros:
  - Zero data loss
  - Immediate consistency across regions
  - Simple application logic

Cons:
  - Higher write latency (100-300ms)
  - Reduced availability during network partitions
  - Single point of failure (primary region)
```

### Eventual Consistency (Asynchronous Replication)

```yaml
# Trade-offs
Pros:
  - Low write latency (<10ms)
  - High availability
  - Multi-region writes

Cons:
  - Possible data loss on primary failure
  - Conflict resolution needed
  - NOT suitable for payments
```

**Decision: Use Strong Consistency for payments**

---

## 10. Performance Optimization

### Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100
      minimum-idle: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Read-Write Splitting

```java
@Configuration
public class DataSourceRoutingConfig {
    
    @Bean
    public DataSource routingDataSource() {
        RoutingDataSource routing = new RoutingDataSource();
        
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("write", primaryDataSource());
        dataSources.put("read", readReplicaDataSource());
        
        routing.setTargetDataSources(dataSources);
        routing.setDefaultTargetDataSource(primaryDataSource());
        
        return routing;
    }
}

// Automatically route based on @Transactional(readOnly = true)
public class TransactionRoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() 
            ? "read" : "write";
    }
}
```

---

## 11. Monitoring & Observability

```java
@Component
public class PaymentMetrics {
    
    @Autowired
    private MeterRegistry registry;
    
    public void recordPaymentLatency(String region, long latency) {
        registry.timer("payment.latency", "region", region)
            .record(latency, TimeUnit.MILLISECONDS);
    }
    
    public void recordReplicationLag(String replica, long lagMs) {
        registry.gauge("db.replication.lag", Tags.of("replica", replica), lagMs);
    }
    
    public void recordPaymentStatus(String status) {
        registry.counter("payment.status", "status", status).increment();
    }
}
```

### Critical Alerts

- Replication lag > 1 second
- Primary database down
- Payment success rate < 99%
- Write latency p99 > 500ms
- Idempotency key collision

---

## 12. Cost Analysis

| Component | Configuration | Cost/Month |
|-----------|--------------|------------|
| Primary DB (US-EAST) | db.r6g.2xlarge | $800 |
| Read Replicas (3 regions) | db.r6g.xlarge × 3 | $1,200 |
| Redis Cache (3 regions) | cache.r6g.large × 3 | $600 |
| Kafka Cluster | 6 brokers | $1,500 |
| Load Balancers | ALB × 3 | $150 |
| Data Transfer | Cross-region | $500 |

**Total: ~$4,750/month**

---

## Key Takeaways

1. **Single-region writes** with synchronous replication for strong consistency
2. **Multi-region reads** from local replicas for low latency
3. **Idempotency** for exactly-once processing
4. **Event sourcing** for complete audit trail
5. **Saga pattern** for distributed transactions
6. **Optimistic locking** for concurrent updates
7. **Active-passive failover** for high availability
8. **Trade write latency** (100-300ms) for consistency
9. **Monitor replication lag** continuously
10. **Never compromise** on payment consistency

---

## Interview Answer Summary

**Question**: How to scale payment system across regions while keeping consistency?

**Answer**:
1. **Single-region writes** (primary in US-EAST) with synchronous replication
2. **Multi-region reads** from local replicas for low latency
3. **Strong consistency** via PostgreSQL synchronous_commit = remote_apply
4. **Idempotency keys** for exactly-once processing
5. **Event sourcing** for audit trail and state reconstruction
6. **Saga pattern** for complex distributed transactions
7. **Active-passive failover** for high availability
8. **Trade-off**: Accept 100-300ms write latency for zero data loss
9. **Monitor**: Replication lag, payment success rate, write latency
10. **Result**: 99.99% availability with ACID guarantees

This ensures **no double charges, no lost payments** while serving global users with low read latency.
