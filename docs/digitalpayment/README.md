# Digital Payment Platform - PhonePe/GPay Clone

A highly scalable digital payment platform enabling instant money transfers with strong consistency, real-time fraud detection, and multi-payment method support.

## 📋 Overview

This digital payment platform implements a production-ready system with:
- **P2P and P2M Transactions** for peer-to-peer and merchant payments
- **Multiple Payment Methods** (UPI, Cards, Net Banking, Wallet)
- **Real-time Fraud Detection** with ML-based risk scoring
- **Strong Consistency** for financial data integrity
- **High Availability** with 99.99% uptime guarantee
- **Atomic Wallet Operations** with pessimistic locking

## 🎯 Key Features

### ✅ **Payment Methods**
- UPI (Unified Payments Interface)
- Credit/Debit Cards
- Net Banking
- Digital Wallet
- QR Code payments
- Contactless payments

### ✅ **Transaction Types**
- Peer-to-peer (P2P) money transfers
- Peer-to-merchant (P2M) payments
- Bill payments (utilities, mobile recharge)
- Merchant payments with QR codes
- Recurring payments and subscriptions
- Split payments

### ✅ **Wallet Management**
- Digital wallet with balance tracking
- Atomic balance updates with locking
- Transaction history and statements
- Auto-reload from bank accounts
- Cashback and rewards
- Multi-currency support

### ✅ **Security & Fraud Prevention**
- Real-time fraud detection with ML
- Transaction risk scoring
- Velocity checks and limits
- Device fingerprinting
- Two-factor authentication (2FA)
- Biometric authentication

### ✅ **Idempotency & Reliability**
- Duplicate transaction prevention
- Exactly-once payment guarantee
- Automatic retry with exponential backoff
- Dead letter queue for failed transactions
- Comprehensive audit logging
- Transaction reconciliation

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Mobile/Web Applications                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              API Gateway + Load Balancer                        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Payment Service Cluster                            │
├─────────────────────┼───────────────────────────────────────────┤
│ Wallet │ Transaction │ Fraud Detection │ PSP Integration        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│                Apache Kafka                                     │
├─────────────────────┼───────────────────────────────────────────┤
│ Payment Events │ Notifications │ Analytics │ Audit Trail        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│ PostgreSQL   │ │  Redis  │ │   Payment   │
│   Cluster    │ │  Cache  │ │  Processors │
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
export DB_URL=jdbc:postgresql://localhost:5432/digitalpayment_db
export DB_USERNAME=payment_user
export DB_PASSWORD=payment_pass

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Kafka
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
export JWT_SECRET=your-secret-key

# Payment Service Providers
export UPI_PSP_URL=https://upi-gateway.example.com
export CARD_PROCESSOR_URL=https://card-processor.example.com
```

### Run the Service
```bash
mvn clean install
./run-systems.sh digitalpayment
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=digitalpayment

# Service available at http://localhost:8084
```

### Make a Payment
```bash
# P2P Transfer
curl -X POST http://localhost:8084/api/v1/payments/transfer \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user123",
    "receiverId": "user456",
    "amount": 1000.00,
    "currency": "INR",
    "paymentMethod": "UPI",
    "description": "Dinner split",
    "idempotencyKey": "txn-20240115-001"
  }'

# Response
{
  "transactionId": "TXN-20240115-123456",
  "status": "SUCCESS",
  "amount": 1000.00,
  "timestamp": "2024-01-15T10:30:00",
  "senderBalance": 5000.00,
  "receiverBalance": 6000.00
}

# Check Balance
curl -X GET http://localhost:8084/api/v1/wallet/balance \
  -H "Authorization: Bearer <token>"

# Transaction History
curl -X GET "http://localhost:8084/api/v1/transactions?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

## 📊 Performance & Scale

### Scale Targets
- **Users**: 100M registered users
- **Transactions**: 50K TPS (transactions per second)
- **Latency**: <2 seconds for payment completion
- **Availability**: 99.99% uptime
- **Success Rate**: >99.5% transaction success
- **Fraud Detection**: <100ms risk scoring

### Key Metrics
- **Wallet Operations**: <500ms with pessimistic locking
- **Payment Processing**: <2 seconds end-to-end
- **Fraud Detection**: Real-time with <100ms latency
- **Database QPS**: 50K queries/second
- **Cache Hit Ratio**: >90% for user data

## 🔧 Core Components

### 1. **Wallet Service**
Manages digital wallet with atomic balance updates using pessimistic locking.

### 2. **Transaction Service**
Processes payments with idempotency and exactly-once guarantee.

### 3. **Fraud Detection Service**
Real-time ML-based fraud detection with risk scoring.

### 4. **PSP Integration Service**
Strategy pattern for multiple payment service providers.

### 5. **Notification Service**
Real-time notifications for transaction updates.

### 6. **Reconciliation Service**
Daily reconciliation with banks and payment processors.

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD/LLD with wallet management
- [Architecture Diagrams](Architecture_Diagrams.md) - Visual system architecture
- [API Documentation](API_Documentation.md) - Complete REST API reference
- [Scale Calculations](Scale_Calculations.md) - Performance analysis and cost breakdown

## 🎯 Critical Design Decisions

### 1. **Atomic Wallet Operations**
```java
@Transactional
@Lock(LockModeType.PESSIMISTIC_WRITE)
public void transferMoney(String senderId, String receiverId, BigDecimal amount) {
    Wallet senderWallet = walletRepository.findByUserId(senderId);
    Wallet receiverWallet = walletRepository.findByUserId(receiverId);
    
    // Atomic debit and credit
    senderWallet.debit(amount);
    receiverWallet.credit(amount);
    
    walletRepository.saveAll(List.of(senderWallet, receiverWallet));
}
```

### 2. **Idempotency Implementation**
```
Request → Check Idempotency Key → Process/Return Cached
         ↓ (Redis + Database)
         24-hour retention
```

### 3. **Fraud Detection Pipeline**
```
Transaction → Feature Extraction → ML Model → Risk Score → Decision
            ↓ <100ms latency
            Block/Allow/Review
```

### 4. **PSP Integration Strategy**
```java
interface PaymentProcessor {
    PaymentResult processPayment(PaymentRequest request);
}

class UPIProcessor implements PaymentProcessor { }
class CardProcessor implements PaymentProcessor { }
class NetBankingProcessor implements PaymentProcessor { }
```

## 🔒 Security Features

### Authentication & Authorization
- JWT-based authentication
- OAuth 2.0 for third-party apps
- Two-factor authentication (2FA)
- Biometric authentication (fingerprint, face)
- Device binding and verification

### Transaction Security
- End-to-end encryption (TLS 1.3)
- Payment card tokenization
- PCI DSS compliance
- Transaction PIN/password
- Transaction limits and velocity checks

### Fraud Prevention
- ML-based fraud detection
- Real-time risk scoring
- Device fingerprinting
- Geolocation validation
- Behavioral analysis
- Suspicious activity alerts

## 🧪 Testing Strategy

### Unit Tests
- Wallet operations
- Transaction processing
- Fraud detection logic
- Idempotency validation

### Integration Tests
- End-to-end payment flows
- PSP integration
- Database consistency
- Kafka message processing

### Load Tests
- 50K TPS sustained load
- Concurrent wallet operations
- Fraud detection performance
- Database connection pooling

## 📈 Monitoring & Alerting

### Key Metrics
- **Transaction Success Rate**: >99.5%
- **Payment Latency**: P50, P95, P99
- **Fraud Detection Rate**: False positives/negatives
- **Wallet Balance Consistency**: Zero discrepancies
- **PSP Availability**: Uptime percentage

### Alerts
- Transaction success rate <99%
- Payment latency >2 seconds
- Fraud detection false positive >5%
- Wallet balance mismatch detected
- PSP downtime or high error rate

## 🔄 Deployment Strategy

### High Availability
- Multi-AZ deployment
- Auto-scaling based on TPS
- Database read replicas
- Redis cluster with failover
- Circuit breaker for PSPs

### Disaster Recovery
- Real-time database replication
- Transaction log archival
- Point-in-time recovery
- RTO: 15 minutes
- RPO: 0 (zero data loss)

## 💡 Use Cases

### Personal Use
- Send money to friends and family
- Split bills and expenses
- Pay for online shopping
- Recharge mobile and DTH
- Pay utility bills

### Business Use
- Accept payments from customers
- QR code-based payments
- Invoice generation and tracking
- Settlement and reconciliation
- Business analytics

### Merchant Use
- Point-of-sale (POS) integration
- Online payment gateway
- Subscription management
- Refund processing
- Multi-store management

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/payment-enhancement`
3. Implement changes with tests
4. Update documentation
5. Submit pull request

---

**Built for 100M users with 50K TPS, strong consistency, and real-time fraud detection.**
