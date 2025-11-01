# Payment Service System Design

## Table of Contents
1. [System Overview](#system-overview)
2. [High-Level Design (HLD)](#high-level-design-hld)
3. [Low-Level Design (LLD)](#low-level-design-lld)
4. [Key Requirements](#key-requirements)
5. [Critical Challenges](#critical-challenges)
6. [Architecture Components](#architecture-components)
7. [Database Design](#database-design)
8. [API Design](#api-design)
9. [Implementation](#implementation)

## System Overview

A highly available and fault-tolerant payment service that guarantees exactly-once processing, prevents duplicate payments, and handles failures gracefully without data loss.

### Key Features
- Exactly-once payment processing
- Idempotency guarantees
- Distributed transaction support
- External processor fault tolerance
- Real-time monitoring and alerting
- Horizontal scalability
- End-to-end security

## High-Level Design (HLD)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                 API Gateway                                     │
│            (Rate Limiting, Auth)                                │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Payment Service Cluster                            │
├─────────────────────┼───────────────────────────────────────────┤
│  Payment API    │ Idempotency │  Transaction  │  Retry Engine  │
│   Service       │   Service   │   Manager     │                │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                Message Queue (Kafka)                            │
├─────────────────────┼───────────────────────────────────────────┤
│ Payment Events  │ Retry Queue │ Dead Letter │  Audit Events    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│   Database   │ │External │ │ Notification│
│   Cluster    │ │Payment  │ │  Service    │
│ (PostgreSQL) │ │Processors│ │             │
└──────────────┘ └─────────┘ └─────────────┘
```

## Low-Level Design (LLD)

### Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Payment Service Core                         │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│ Payment API     │ Idempotency     │ Transaction     │ Retry     │
│ Controller      │ Manager         │ Manager         │ Engine    │
├─────────────────┼─────────────────┼─────────────────┼───────────┤
│• Process Payment│• Check Duplicate│• 2PC Coordinator│• Exp Backoff│
│• Validate Request│• Generate Keys │• Saga Pattern   │• Circuit Break│
│• Route to Processor│• Cache Results│• Rollback Logic│• Dead Letter│
└─────────────────┴─────────────────┴─────────────────┴───────────┘
                              │
┌─────────────────────────────▼─────────────────────────────────────┐
│                     Data Layer                                   │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│   PostgreSQL    │     Redis       │     Kafka       │ Monitoring│
├─────────────────┼─────────────────┼─────────────────┼───────────┤
│• Transactions   │• Idempotency    │• Event Streaming│• Metrics  │
│• Audit Logs     │• Session Cache  │• Retry Queue    │• Alerts   │
│• User Data      │• Rate Limiting  │• Dead Letter    │• Tracing  │
└─────────────────┴─────────────────┴─────────────────┴───────────┘
```

## Key Requirements

### Functional Requirements
1. **Payment Processing**: Support multiple payment methods (card, bank, wallet)
2. **Transaction Management**: Handle payment lifecycle (pending → processing → completed/failed)
3. **Refund Processing**: Support partial and full refunds
4. **Multi-currency Support**: Handle different currencies and exchange rates
5. **Merchant Management**: Support multiple merchants and their configurations

### Non-Functional Requirements
1. **Exactly-Once Processing**: Guarantee no duplicate payments
2. **High Availability**: 99.99% uptime (52.6 minutes downtime/year)
3. **Fault Tolerance**: Handle external service failures gracefully
4. **Scalability**: Support 100K+ TPS (Transactions Per Second)
5. **Security**: PCI DSS compliance, encryption, secure communication
6. **Performance**: <200ms response time for payment processing
7. **Consistency**: Strong consistency for financial data

## Critical Challenges

### 1. Idempotency and Duplicate Prevention

**Challenge**: Prevent duplicate payments due to retries, network issues, or client errors.

**Solution**:
```
Request → Generate Idempotency Key → Check Cache/DB
├─ Key Exists: Return cached result
└─ Key New: Process payment → Cache result → Return response
```

**Implementation**:
- Unique transaction ID for each payment request
- Idempotency key validation before processing
- Result caching with TTL
- Database constraints for duplicate prevention

### 2. Reliable Message Delivery

**Challenge**: Ensure messages are not lost during service failures.

**Solution**:
```
Payment Request → Kafka Producer → Partition → Consumer → External Processor
                     ↓
                 Acknowledgment ← Success/Failure ← Response
```

**Implementation**:
- Kafka with at-least-once delivery semantics
- Message acknowledgments and retries
- Dead letter queues for failed messages
- Consumer offset management

### 3. External Service Failure Handling

**Challenge**: Handle failures from payment processors (Stripe, PayPal, etc.).

**Solution**:
```
Request → Circuit Breaker → Retry with Exponential Backoff
├─ Success: Process normally
├─ Temporary Failure: Retry with jitter
└─ Permanent Failure: Route to fallback processor
```

**Implementation**:
- Circuit breaker pattern
- Exponential backoff with jitter
- Multiple payment processor support
- Fallback mechanisms and timeouts

### 4. Distributed Transaction Management

**Challenge**: Ensure atomicity across multiple services and databases.

**Solution**:
```
Saga Pattern:
Payment → Reserve Funds → Process with Processor → Update Ledger → Notify
    ↓         ↓                    ↓                  ↓           ↓
Compensate ← Release ← Rollback ← Revert ← Cancel Notification
```

**Implementation**:
- Saga orchestration pattern
- Compensation actions for rollbacks
- Event sourcing for audit trail
- Two-phase commit for critical operations

### 5. Monitoring and Alerting

**Challenge**: Detect and respond to failures quickly.

**Solution**:
- Real-time metrics and dashboards
- Automated alerting for anomalies
- Distributed tracing for debugging
- Health checks and circuit breakers

### 6. Scalability and Performance

**Challenge**: Handle high transaction volumes with low latency.

**Solution**:
- Horizontal scaling with load balancers
- Database sharding and read replicas
- Caching strategies (Redis)
- Async processing with message queues

### 7. Security

**Challenge**: Protect sensitive financial data and prevent fraud.

**Solution**:
- End-to-end encryption (TLS 1.3)
- PCI DSS compliance
- Tokenization of sensitive data
- Rate limiting and fraud detection

## Database Design

### Payment Transaction Table
```sql
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    merchant_id UUID NOT NULL,
    user_id UUID,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
    payment_method VARCHAR(50) NOT NULL,
    processor VARCHAR(50) NOT NULL,
    processor_transaction_id VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    
    INDEX idx_idempotency (idempotency_key),
    INDEX idx_merchant_status (merchant_id, status),
    INDEX idx_created_at (created_at)
);
```

### Idempotency Cache Table
```sql
CREATE TABLE idempotency_cache (
    key VARCHAR(255) PRIMARY KEY,
    transaction_id UUID NOT NULL,
    response_data JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    
    INDEX idx_expires_at (expires_at)
);
```

### Retry Queue Table
```sql
CREATE TABLE retry_queue (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 5,
    next_retry_at TIMESTAMP NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_next_retry (next_retry_at),
    INDEX idx_transaction (transaction_id)
);
```

## API Design

### Process Payment
```http
POST /api/v1/payments/process
Content-Type: application/json
Idempotency-Key: unique-key-12345

{
  "merchantId": "merchant-123",
  "amount": 100.00,
  "currency": "USD",
  "paymentMethod": {
    "type": "CARD",
    "cardToken": "tok_1234567890"
  },
  "metadata": {
    "orderId": "order-456",
    "description": "Product purchase"
  }
}
```

### Response
```json
{
  "transactionId": "txn_abc123",
  "status": "COMPLETED",
  "amount": 100.00,
  "currency": "USD",
  "processorTransactionId": "pi_1234567890",
  "createdAt": "2024-01-15T10:30:00Z",
  "completedAt": "2024-01-15T10:30:02Z"
}
```

### Get Transaction Status
```http
GET /api/v1/payments/transactions/{transactionId}
```

### Process Refund
```http
POST /api/v1/payments/refund
Content-Type: application/json
Idempotency-Key: refund-key-12345

{
  "originalTransactionId": "txn_abc123",
  "amount": 50.00,
  "reason": "Customer request"
}
```

## Implementation Highlights

The implementation includes:

1. **PaymentService**: Core business logic with idempotency checks
2. **IdempotencyManager**: Duplicate prevention and caching
3. **TransactionManager**: Distributed transaction coordination
4. **RetryEngine**: Exponential backoff and circuit breaker
5. **PaymentProcessorFactory**: Multiple processor support
6. **AuditService**: Complete transaction logging
7. **MonitoringService**: Real-time metrics and alerting

### Key Design Patterns Used

1. **Saga Pattern**: For distributed transactions
2. **Circuit Breaker**: For external service resilience
3. **Factory Pattern**: For payment processor abstraction
4. **Observer Pattern**: For event-driven notifications
5. **Strategy Pattern**: For different retry strategies
6. **Command Pattern**: For transaction operations

### Performance Optimizations

1. **Connection Pooling**: Database and HTTP connections
2. **Caching**: Redis for idempotency and session data
3. **Async Processing**: Non-blocking I/O operations
4. **Batch Processing**: For bulk operations
5. **Database Indexing**: Optimized query performance
6. **Load Balancing**: Horizontal scaling support

This design ensures exactly-once processing, handles failures gracefully, and provides the scalability and security required for a production payment system.