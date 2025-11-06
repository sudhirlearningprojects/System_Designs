# Digital Payment Platform System Design (PhonePe/GPay Clone)

## Understanding Digital Payment Systems

### What is a Digital Payment Platform?
A digital payment platform is a financial technology system that enables electronic transactions between users, merchants, and financial institutions. Unlike traditional banking, these platforms focus on user experience, instant transfers, and mobile-first design.

### Key Challenges in Payment Systems
1. **Financial Accuracy**: Never lose or duplicate money
2. **Regulatory Compliance**: Follow banking and financial regulations
3. **Security**: Protect against fraud and unauthorized access
4. **Performance**: Handle millions of transactions per day
5. **Availability**: 99.99% uptime for financial operations

### Payment System Fundamentals

#### ACID Properties in Finance
- **Atomicity**: Transfer either completes fully or fails completely
- **Consistency**: Account balances always remain accurate
- **Isolation**: Concurrent transactions don't interfere
- **Durability**: Completed transactions are permanently recorded

#### Double-Entry Bookkeeping
```
Transfer ₹100 from Alice to Bob:
- Debit Alice's account: -₹100
- Credit Bob's account: +₹100
- Net change: ₹0 (money is conserved)
```

#### Idempotency in Payments
- **Problem**: Network failures can cause duplicate requests
- **Solution**: Each request has unique ID, duplicate requests return same result
- **Implementation**: Store request IDs in Redis cache

## Overview
A highly scalable and reliable digital payment platform enabling instant money transfers, bill payments, and merchant transactions with strong consistency and fault tolerance.

### Payment Flow Types

#### P2P (Person-to-Person)
- **Use Case**: Send money to friends/family
- **Example**: Split restaurant bill among friends
- **Requirements**: Instant transfer, low fees

#### P2M (Person-to-Merchant)
- **Use Case**: Pay for goods/services
- **Example**: Pay at grocery store using QR code
- **Requirements**: Fast processing, receipt generation

#### B2B (Business-to-Business)
- **Use Case**: Supplier payments, bulk transfers
- **Example**: Company paying vendor invoices
- **Requirements**: High value limits, audit trails

## High-Level Design (HLD)

### Design Philosophy
Digital payment systems require different architectural decisions compared to other systems:

#### Consistency Over Availability (CP in CAP Theorem)
- **Choice**: Strong consistency over high availability
- **Reason**: Financial accuracy is more important than uptime
- **Implementation**: Synchronous database transactions, locks

#### Synchronous vs Asynchronous Processing
- **Synchronous**: Core payment flow (debit/credit operations)
- **Asynchronous**: Notifications, analytics, reporting
- **Reason**: User must know immediately if payment succeeded

#### Database Selection Strategy
- **PostgreSQL**: ACID compliance for financial data
- **Redis**: Fast caching and session management
- **Kafka**: Reliable event streaming for audit logs

### System Architecture
- **Microservices Architecture**: Modular design for high availability and scalability
- **API Gateway**: Single entry point with authentication, rate limiting, and routing
- **Event-Driven Architecture**: Asynchronous processing with message queues
- **Multi-Database Strategy**: SQL for transactions, NoSQL for logs, Redis for caching

### Core Services
1. **Payment Service**: Orchestrates payment lifecycle and PSP integration
2. **Ledger Service**: Manages wallet balances with atomic operations
3. **Fraud Detection Service**: Real-time transaction monitoring and risk assessment
4. **User Service**: User management, authentication, and account linking
5. **Transaction History Service**: Efficient storage and retrieval of transaction records

### Key Design Patterns
- **Strategy Pattern**: PaymentGatewayStrategy for different PSPs (UPI, Cards, Net Banking)
- **Factory Pattern**: PaymentGatewayFactory for gateway instantiation
- **Singleton Pattern**: FraudDetectionEngine for centralized risk assessment
- **Saga Pattern**: Distributed transaction management across services

### Payment Service Patterns

#### Strategy Pattern for Payment Methods
```java
interface PaymentStrategy {
    PaymentResult processPayment(PaymentRequest request);
}

class UPIPaymentStrategy implements PaymentStrategy {
    public PaymentResult processPayment(PaymentRequest request) {
        // UPI-specific processing
        return callUPIGateway(request);
    }
}

class CardPaymentStrategy implements PaymentStrategy {
    public PaymentResult processPayment(PaymentRequest request) {
        // Card-specific processing
        return callCardGateway(request);
    }
}
```

#### Saga Pattern for Distributed Transactions
```java
class PaymentSaga {
    public void executePayment(PaymentRequest request) {
        try {
            // Step 1: Reserve money from sender
            reserveFunds(request.getSenderId(), request.getAmount());
            
            // Step 2: Process with external gateway
            PaymentResult result = processWithGateway(request);
            
            if (result.isSuccess()) {
                // Step 3: Complete the transfer
                completeFundsTransfer(request);
            } else {
                // Compensate: Release reserved funds
                releaseFunds(request.getSenderId(), request.getAmount());
            }
        } catch (Exception e) {
            // Compensate all completed steps
            compensateTransaction(request);
        }
    }
}
```

## Low-Level Design (LLD)

### Understanding Concurrency in Payments

#### The Race Condition Problem
```
Scenario: Alice has ₹100, two simultaneous ₹50 transfers

Thread 1: Read balance (₹100) → Check sufficient funds → Deduct ₹50
Thread 2: Read balance (₹100) → Check sufficient funds → Deduct ₹50

Result: Alice's balance becomes ₹0 (should be ₹0 or ₹50)
Problem: Both threads saw ₹100 and approved the transfer
```

#### Solution: Pessimistic Locking
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public boolean transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
    // Lock sender's account first (prevents race conditions)
    Account fromAccount = accountRepository.findByUserIdForUpdate(fromUserId);
    
    if (fromAccount.getBalance().compareTo(amount) < 0) {
        return false; // Insufficient funds
    }
    
    // Lock receiver's account
    Account toAccount = accountRepository.findByUserIdForUpdate(toUserId);
    
    // Perform atomic transfer
    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    toAccount.setBalance(toAccount.getBalance().add(amount));
    
    accountRepository.save(fromAccount);
    accountRepository.save(toAccount);
    
    return true;
}
```

### Core Entities
- **User**: User profiles with authentication and linked accounts
- **Account**: Bank account details with balance tracking
- **Wallet**: Digital wallet with atomic balance operations
- **Transaction**: Complete transaction lifecycle with status management

### Concurrency Control
- **Pessimistic Locking**: SELECT FOR UPDATE on wallet operations
- **Optimistic Locking**: Version-based concurrency control
- **Database Transactions**: SERIALIZABLE isolation for financial operations
- **Synchronized Methods**: Thread-safe wallet balance operations

### Consistency Guarantees
- **Atomic Transfers**: All-or-nothing wallet operations
- **Idempotency**: Duplicate request prevention with Redis caching
- **Strong Consistency**: ACID properties for all financial data
- **Eventual Consistency**: Non-critical data like transaction history

### Idempotency Implementation

#### The Duplicate Request Problem
```
User clicks "Pay" button → Network timeout → User clicks again
Result: Two identical payment requests
Solution: Idempotency keys
```

#### Idempotency Implementation
```java
@Service
public class PaymentService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public PaymentResult processPayment(PaymentRequest request) {
        String idempotencyKey = request.getIdempotencyKey();
        
        // Check if we've seen this request before
        String cachedResult = redisTemplate.opsForValue().get(idempotencyKey);
        if (cachedResult != null) {
            return deserialize(cachedResult); // Return cached result
        }
        
        // Process payment for first time
        PaymentResult result = executePayment(request);
        
        // Cache result for 24 hours
        redisTemplate.opsForValue().set(idempotencyKey, serialize(result), 
                                      Duration.ofHours(24));
        
        return result;
    }
}
```

### Fraud Detection Algorithms

#### Rule-Based Detection
```java
class FraudDetectionEngine {
    public FraudScore calculateFraudScore(Transaction transaction) {
        FraudScore score = new FraudScore();
        
        // Rule 1: High amount transactions
        if (transaction.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            score.addRisk("HIGH_AMOUNT", 30);
        }
        
        // Rule 2: Unusual time patterns
        if (isUnusualTime(transaction.getTimestamp())) {
            score.addRisk("UNUSUAL_TIME", 20);
        }
        
        // Rule 3: Velocity checks
        int recentTransactionCount = getRecentTransactionCount(
            transaction.getUserId(), Duration.ofHours(1));
        if (recentTransactionCount > 10) {
            score.addRisk("HIGH_VELOCITY", 40);
        }
        
        return score;
    }
}
```

## Scalability Features

### Scaling Payment Systems

#### Database Sharding Strategy
```java
class PaymentShardingStrategy {
    private static final int NUM_SHARDS = 100;
    
    public int getShardId(Long userId) {
        return (int) (userId % NUM_SHARDS);
    }
    
    public DataSource getDataSource(Long userId) {
        int shardId = getShardId(userId);
        return dataSourceMap.get(shardId);
    }
}
```

#### Why Shard by User ID?
- **User transactions**: Usually query by user ID
- **Hot partitions**: Distribute load evenly
- **Cross-shard queries**: Minimize need for joins across shards

### Performance Optimizations
- **Redis Caching**: Session tokens, idempotency keys, fraud detection counters
- **Database Sharding**: Partition by user ID for horizontal scaling
- **Connection Pooling**: Optimized database connection management
- **Asynchronous Processing**: Non-blocking operations with message queues

### High Availability
- **Circuit Breaker**: PSP failure handling with graceful degradation
- **Load Balancing**: Multiple service instances across availability zones
- **Database Replication**: Primary-replica setup with automatic failover
- **Health Checks**: Proactive monitoring and alerting

## Security & Fraud Prevention

### Fraud Detection
- **Daily Transaction Limits**: ₹1,00,000 per user per day
- **Rate Limiting**: Maximum 50 transactions per hour per user
- **High-Value Alerts**: Automatic flagging of transactions > ₹50,000
- **Real-time Monitoring**: Suspicious pattern detection

### Security Measures
- **PIN-based Authentication**: Secure transaction authorization
- **Data Encryption**: Sensitive information protection
- **Audit Logging**: Complete transaction trail for compliance
- **PCI DSS Compliance**: Industry-standard security practices

## API Endpoints
- `POST /api/payments/initiate` - Initiate payment with idempotency
- `GET /api/payments/status/{id}` - Get transaction status
- `POST /api/payments/callback` - Handle PSP callbacks
- `GET /api/payments/balance/{userId}` - Get wallet balance
- `GET /api/payments/history/{userId}` - Get transaction history

## Technology Stack
- **Backend**: Spring Boot 3.2, Java 17
- **Database**: PostgreSQL (transactions), Redis (caching)
- **Messaging**: Apache Kafka for event streaming
- **Monitoring**: Micrometer, Prometheus, Grafana
- **Security**: Spring Security, JWT tokens

## Performance Metrics
- **Throughput**: 50,000 transactions per minute
- **Latency**: Sub-2 second response times
- **Availability**: 99.99% uptime SLA
- **Consistency**: Zero money loss or duplication
- **Scalability**: Millions of concurrent users

## Deployment Architecture
- **Containerization**: Docker containers with Kubernetes orchestration
- **Load Balancing**: NGINX/AWS ALB with health checks
- **Database**: PostgreSQL cluster with read replicas
- **Caching**: Redis cluster for high availability
- **Monitoring**: Comprehensive observability stack

This design ensures financial accuracy, regulatory compliance, and exceptional user experience while handling massive scale and maintaining system reliability.