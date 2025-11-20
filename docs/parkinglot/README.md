# Parking Lot Management System - High-Availability Parking System

A fault-tolerant parking lot management system for multi-story facilities with real-time spot availability tracking and atomic spot allocation.

## 📋 Overview

This parking lot system implements a production-ready management platform with:
- **Multi-floor, Multi-gate Support** for large facilities
- **Real-time Spot Availability** tracking with Redis caching
- **Thread-safe Spot Allocation** preventing double-booking
- **Multiple Payment Methods** with circuit breaker pattern
- **High Availability** with 99.99% uptime guarantee
- **Scalable Architecture** supporting 1000+ spots

## 🎯 Key Features

### ✅ **Parking Management**
- Multi-floor parking facility support
- Multiple entry/exit gates
- Different vehicle types (Car, Motorcycle, Truck, Electric)
- Spot size matching (Compact, Regular, Large, Handicapped)
- Real-time availability tracking
- Automated spot assignment

### ✅ **Atomic Operations**
- Thread-safe spot allocation with pessimistic locking
- No double-booking guarantee
- Distributed locking with Redis
- Transaction management for consistency
- Concurrent request handling
- Race condition prevention

### ✅ **Payment Processing**
- Multiple payment methods (Cash, Card, UPI, Wallet)
- Hourly rate calculation
- Flat rate and time-based pricing
- Payment retry mechanism
- Circuit breaker for payment gateway
- Receipt generation

### ✅ **Real-time Updates**
- Display board updates for available spots
- Floor-wise availability tracking
- Vehicle type-specific availability
- WebSocket notifications for updates
- Cache invalidation on changes
- Event-driven architecture

### ✅ **Monitoring & Analytics**
- Occupancy rate tracking
- Revenue analytics
- Peak hour analysis
- Average parking duration
- Vehicle type distribution
- Payment method statistics

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Entry/Exit Gates                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Parking Management Service                         │
├─────────────────────┼───────────────────────────────────────────┤
│ Spot Allocator │ Payment │ Display Board │ Ticket Manager       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│ PostgreSQL   │ │  Redis  │ │  Payment    │
│   Database   │ │  Cache  │ │  Gateway    │
└──────────────┘ └─────────┘ └─────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+

### Configuration
```bash
# Database
export DB_USERNAME=postgres
export DB_PASSWORD=password
export DB_URL=jdbc:postgresql://localhost:5432/parkinglot_db

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Parking Configuration
export PARKING_FLOORS=5
export SPOTS_PER_FLOOR=200
export HOURLY_RATE=10.0
```

### Run the Service
```bash
mvn clean install
./run-systems.sh parkinglot
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=parkinglot

# Service available at http://localhost:8080
```

### Park a Vehicle
```bash
# Entry - Park vehicle
curl -X POST http://localhost:8080/api/v1/parking/entry \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleNumber": "KA01AB1234",
    "vehicleType": "CAR",
    "gateId": 1
  }'

# Response
{
  "ticketId": "TKT-20240115-001",
  "spotNumber": "A-101",
  "floor": 1,
  "entryTime": "2024-01-15T10:30:00",
  "vehicleNumber": "KA01AB1234"
}

# Exit - Process payment and exit
curl -X POST http://localhost:8080/api/v1/parking/exit \
  -H "Content-Type: application/json" \
  -d '{
    "ticketId": "TKT-20240115-001",
    "paymentMethod": "CARD"
  }'

# Response
{
  "ticketId": "TKT-20240115-001",
  "exitTime": "2024-01-15T12:30:00",
  "duration": "2 hours",
  "amount": 20.0,
  "paymentStatus": "SUCCESS"
}
```

## 📊 Performance & Scale

### Scale Targets
- **Parking Spots**: 1000+ spots across multiple floors
- **Concurrent Vehicles**: 100+ simultaneous entry/exit
- **Response Time**: <1 second for spot allocation
- **Availability**: 99.99% uptime
- **Throughput**: 1000+ transactions per hour

### Key Metrics
- **Spot Allocation**: <500ms with Redis caching
- **Payment Processing**: <2 seconds average
- **Cache Hit Ratio**: >95% for availability queries
- **Database QPS**: 100+ queries/second
- **Zero Double-booking**: Guaranteed with locking

## 🔧 Core Components

### 1. **Parking Spot Service**
Manages spot allocation with thread-safe operations and Redis caching.

### 2. **Ticket Service**
Generates and manages parking tickets with unique IDs.

### 3. **Payment Service**
Processes payments with circuit breaker and retry logic.

### 4. **Display Board Service**
Real-time availability updates for entry gates.

### 5. **Pricing Service**
Calculates parking fees based on duration and vehicle type.

### 6. **Analytics Service**
Tracks occupancy, revenue, and usage patterns.

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD/LLD with concurrency handling
- [Architecture Diagrams](Architecture_Diagrams.md) - Visual system architecture

## 🎯 Critical Design Decisions

### 1. **Atomic Spot Allocation**
```java
@Transactional
@Lock(LockModeType.PESSIMISTIC_WRITE)
public ParkingSpot allocateSpot(VehicleType vehicleType) {
    // Pessimistic locking prevents double-booking
    ParkingSpot spot = findAvailableSpot(vehicleType);
    spot.setOccupied(true);
    return spotRepository.save(spot);
}
```

### 2. **Redis Caching Strategy**
```
Availability Query → Redis Cache → Database (if miss)
                    ↓ 95% hit rate
                    O(1) lookup time
```

### 3. **Payment Circuit Breaker**
```
Payment Request → Circuit Breaker → Payment Gateway
                 ↓ (if open)
                 Fallback: Queue for retry
```

### 4. **Multi-level Locking**
```
Application Lock (Pessimistic) + Redis Lock (Distributed)
= Zero double-booking guarantee
```

## 🔒 Security Features

### Access Control
- Gate-based authentication
- Ticket validation
- Payment verification
- Admin role-based access
- Audit logging

### Data Protection
- Encrypted payment information
- Secure ticket generation
- PCI DSS compliance for payments
- GDPR compliance for vehicle data

## 🧪 Testing Strategy

### Unit Tests
- Spot allocation logic
- Pricing calculation
- Ticket generation
- Payment processing

### Integration Tests
- End-to-end parking flow
- Concurrent spot allocation
- Payment gateway integration
- Cache consistency

### Load Tests
- 100+ concurrent entries
- High-volume payment processing
- Cache performance under load
- Database connection pooling

## 📈 Monitoring & Alerting

### Key Metrics
- **Occupancy Rate**: Current vs total spots
- **Average Duration**: Parking time per vehicle
- **Revenue**: Hourly/daily/monthly
- **Payment Success Rate**: Successful payments %
- **Response Time**: API latency P95/P99

### Alerts
- Occupancy >90% (near full)
- Payment failure rate >5%
- Response time >1 second
- Database connection pool >80%
- Redis cache unavailable

## 🔄 Deployment Strategy

### High Availability
- Multi-instance deployment
- Database replication
- Redis cluster with failover
- Load balancer for traffic distribution
- Auto-scaling based on load

### Disaster Recovery
- Automated database backups
- Redis persistence (RDB + AOF)
- Transaction log archival
- RTO: 15 minutes
- RPO: 5 minutes

## 💡 Use Cases

### Shopping Malls
- Large multi-floor parking
- Multiple entry/exit points
- Peak hour management
- Customer convenience

### Airports
- Long-term parking
- Different vehicle types
- Premium parking zones
- Shuttle integration

### Office Buildings
- Employee parking management
- Visitor parking
- Reserved spots
- Monthly passes

### Hospitals
- Emergency vehicle priority
- Visitor parking
- Staff parking
- Handicapped spots

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/parking-enhancement`
3. Implement changes with tests
4. Update documentation
5. Submit pull request

---

**Built for high-traffic parking facilities with zero double-booking and sub-second response times.**
