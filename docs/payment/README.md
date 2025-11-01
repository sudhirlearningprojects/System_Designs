# Payment Service - Highly Available & Fault-Tolerant Payment System

A production-ready payment service that guarantees exactly-once processing, prevents duplicate payments, and handles failures from external payment processors and internal services without data loss.

## 📋 Overview

This payment service addresses critical challenges in financial systems:
- **Exactly-once processing** to prevent duplicate payments
- **Fault tolerance** for external payment processor failures
- **Distributed transaction management** with saga patterns
- **High availability** with 99.99% uptime guarantee
- **Scalability** to handle 100K+ transactions per second

## 🎯 Key Features

### ✅ **Idempotency & Duplicate Prevention**
- Unique transaction IDs for each payment request
- Redis + Database dual-layer caching
- Automatic duplicate detection and response caching
- 24-hour idempotency key retention

### ✅ **Fault Tolerance & Resilience**
- Circuit breaker pattern for external services
- Exponential backoff with jitter for retries
- Multiple payment processor support (Stripe, PayPal, Square)
- Automatic failover to backup processors

### ✅ **Distributed Transaction Management**
- Saga orchestration pattern
- Compensation actions for rollbacks
- Event sourcing for complete audit trail
- Two-phase commit for critical operations

### ✅ **High Availability Architecture**
- Multi-region deployment
- Auto-scaling with load balancers
- Database clustering with read replicas
- Redis cluster for caching layer

### ✅ **Security & Compliance**
- PCI DSS compliant architecture
- End-to-end encryption (TLS 1.3)
- JWT authentication with OAuth 2.0
- Comprehensive audit logging

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Client Applications                          │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                 API Gateway                                     │
│            (Auth, Rate Limiting)                                │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Payment Service Cluster                            │
├─────────────────────┼───────────────────────────────────────────┤
│ Payment API │ Idempotency │ Transaction │ Retry Engine │ Audit  │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                Apache Kafka                                     │
├─────────────────────┼───────────────────────────────────────────┤
│ Payment Events │ Retry Queue │ Dead Letter │ Audit Events      │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│ PostgreSQL   │ │External │ │    Redis    │
│  Cluster     │ │Payment  │ │   Cache     │
│              │ │Processors│ │             │
└──────────────┘ └─────────┘ └─────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+

### Configuration
```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/payment_db
export DB_USERNAME=payment_user
export DB_PASSWORD=payment_pass

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Kafka
export KAFKA_SERVERS=localhost:9092

# Security
export JWT_SECRET=your-secret-key
```

### Run the Service
```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.main-class=org.sudhir512kj.payment.PaymentApplication
```

### Health Check
```bash
curl http://localhost:8080/api/v1/payment/health
```

## 📊 Performance & Scale

### Scale Targets
- **Peak TPS**: 100,000 transactions per second
- **Average Latency**: <200ms for payment processing
- **Availability**: 99.99% uptime (52.6 minutes downtime/year)
- **Data Durability**: 99.999999999% (11 9's)

### Cost Analysis
- **Monthly AWS Cost**: ~$168,540 for full scale
- **Cost per Transaction**: $0.002 at 100K TPS
- **Annual Processing**: 950 billion transactions

## 🔧 Key Components

### 1. **PaymentService**
Core business logic with idempotency checks and transaction management.

### 2. **IdempotencyService**
Prevents duplicate payments using Redis + Database caching with 24-hour retention.

### 3. **PaymentProcessorService**
Handles external payment processors with circuit breaker and retry logic.

### 4. **TransactionManagerService**
Manages distributed transactions using saga pattern with compensation.

### 5. **RetryEngine**
Exponential backoff retry mechanism with dead letter queue for failed transactions.

### 6. **AuditService**
Comprehensive logging and audit trail for all payment operations.

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD and LLD with critical challenges
- [Architecture Diagrams](Architecture_Diagrams.md) - 10 detailed Mermaid diagrams
- [API Documentation](API_Documentation.md) - Complete REST API reference
- [Scale Calculations](Scale_Calculations.md) - Performance analysis and cost breakdown

## 🔒 Security Features

### Authentication & Authorization
- JWT-based authentication with RS256 signing
- OAuth 2.0 integration support
- API key management for merchants
- Role-based access control (RBAC)

### Data Protection
- TLS 1.3 for all communications
- AES-256 encryption at rest
- Payment card tokenization
- PCI DSS compliance architecture

### Fraud Prevention
- Real-time transaction monitoring
- Velocity checks and limits
- Geolocation validation
- Machine learning-based fraud detection

## 🎯 Critical Design Decisions

### 1. **Idempotency Strategy**
```
Request → Generate Key → Check Cache → Process/Return Cached
```
- Dual-layer caching (Redis + Database)
- 24-hour retention period
- Automatic cleanup of expired entries

### 2. **Circuit Breaker Implementation**
```
Closed → Open (50% failure rate) → Half-Open → Closed/Open
```
- 30-second wait duration in open state
- 10-call sliding window for failure calculation
- Automatic fallback to alternative processors

### 3. **Retry Mechanism**
```
Exponential Backoff: base_delay * (2^retry_count) + jitter
```
- Maximum 5 retry attempts
- Jitter to prevent thundering herd
- Dead letter queue for permanent failures

### 4. **Saga Pattern for Distributed Transactions**
```
Reserve Funds → Process Payment → Update Ledger → Send Notification
     ↓              ↓               ↓              ↓
Release Funds ← Rollback ← Revert Ledger ← Cancel Notification
```

## 🧪 Testing Strategy

### Unit Tests
- Service layer business logic
- Idempotency key validation
- Retry mechanism logic
- Circuit breaker behavior

### Integration Tests
- Database transaction handling
- External API integration
- Kafka message processing
- Redis caching behavior

### Load Testing
- 100K TPS sustained load
- Failure scenario testing
- Memory leak detection
- Database performance under load

## 📈 Monitoring & Alerting

### Key Metrics
- **Transaction Success Rate**: >99.5%
- **Average Response Time**: <200ms
- **P99 Response Time**: <500ms
- **Error Rate**: <0.5%
- **Circuit Breaker Status**: Real-time monitoring

### Alerts
- Payment failure rate >1%
- Response time >500ms for 2 minutes
- Database connection pool >80%
- External processor downtime
- Unusual transaction patterns

## 🔄 Deployment Strategy

### Blue-Green Deployment
- Zero-downtime deployments
- Automatic rollback on failure
- Database migration handling
- Feature flag support

### Multi-Region Setup
- Primary region: us-east-1 (60% traffic)
- Secondary region: us-west-2 (40% traffic)
- DR region: eu-west-1 (cold standby)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Update documentation
5. Submit pull request

### Code Standards
- Java 17 features and best practices
- Comprehensive error handling
- Security-first approach
- Performance optimization
- Complete test coverage

---

**Built for mission-critical financial systems with zero tolerance for data loss and duplicate payments.**