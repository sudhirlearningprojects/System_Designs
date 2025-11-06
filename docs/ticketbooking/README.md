# Ticket Booking Platform - System Overview

A highly scalable, fault-tolerant ticket booking platform similar to Ticketmaster/BookMyShow that handles high-concurrency ticket sales while preventing overselling through sophisticated inventory management.

## 🎯 Key Features

### Core Functionality
- **Event Management**: Create, update, and manage events with multiple ticket types
- **Real-time Inventory**: Atomic ticket hold and release mechanism using Redis
- **Secure Booking Flow**: 10-minute hold period with payment confirmation
- **Search & Discovery**: Fast event search by city, genre, date, and name
- **User Management**: Registration, authentication, and booking history
- **Payment Integration**: Secure payment processing with third-party gateways

### Technical Highlights
- **Zero Overselling**: Strong consistency guarantees for ticket inventory
- **High Concurrency**: Handles 100K+ concurrent booking requests
- **Sub-second Response**: Optimized for flash sale scenarios
- **Fault Tolerance**: Circuit breaker pattern and graceful degradation
- **Horizontal Scaling**: Microservices architecture with auto-scaling

## 🏗️ Architecture Overview

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Mobile App  │    │ Web Client  │    │ Admin Panel │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       └──────────────────┼──────────────────┘
                          │
              ┌───────────▼───────────┐
              │     API Gateway       │
              │ (Auth, Rate Limiting) │
              └───────────┬───────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
   ┌────▼────┐    ┌───────▼───────┐    ┌────▼────┐
   │ Event   │    │   Booking     │    │  User   │
   │ Service │    │   Service     │    │ Service │
   └────┬────┘    └───────┬───────┘    └────┬────┘
        │                 │                 │
        │        ┌────────▼────────┐        │
        │        │ Payment Service │        │
        │        └─────────────────┘        │
        │                                   │
   ┌────▼────┐              ┌──────────────▼──────────────┐
   │PostgreSQL│              │           Redis            │
   │(Events, │              │    (Inventory Cache,       │
   │Bookings)│              │     Hold Management)       │
   └─────────┘              └─────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 6+

### Running the System

1. **Start Infrastructure Services**
```bash
docker-compose up -d postgres redis
```

2. **Configure Environment Variables**
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=ticketbooking
export DB_USERNAME=postgres
export DB_PASSWORD=password

export REDIS_HOST=localhost
export REDIS_PORT=6379

export JWT_SECRET=mySecretKey
export PAYMENT_GATEWAY_URL=https://api.stripe.com
```

3. **Build and Run**
```bash
mvn clean install
./run-systems.sh ticketbooking
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=ticketbooking
```

4. **Verify Installation**
```bash
curl http://localhost:8086/api/events/search
```

## 📊 Performance Specifications

### Scale Targets
- **Users**: 50 million registered users
- **Concurrent Users**: 500K during flash sales
- **Throughput**: 100K requests/second peak
- **Events**: 1,000 events/day globally
- **Bookings**: 500K bookings/day

### Performance Metrics
- **Event Search**: <200ms (95th percentile)
- **Ticket Hold**: <500ms (95th percentile)
- **Booking Confirmation**: <1s (95th percentile)
- **Availability**: 99.99% uptime
- **Consistency**: Zero overselling guarantee

## 🔧 Core Components

### 1. Inventory Management
**Redis-based atomic operations** ensure no ticket overselling:
```java
// Atomic ticket hold with TTL
Long newCount = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
redisTemplate.opsForValue().set(holdKey, quantity, 10, TimeUnit.MINUTES);
```

### 2. Booking Workflow
**Three-phase booking process**:
1. **Hold Phase**: Reserve tickets for 10 minutes
2. **Payment Phase**: Process payment via gateway
3. **Confirmation Phase**: Confirm booking and update inventory

### 3. Concurrency Control
**Pessimistic locking** for critical database operations:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<TicketType> findByIdWithLock(@Param("id") Long id);
```

### 4. Caching Strategy
**Multi-layer caching** for optimal performance:
- **L1**: Application cache for metadata
- **L2**: Redis for inventory and sessions
- **L3**: Database for persistent storage

## 🛡️ Reliability Features

### High Availability
- **Database Replication**: Master-slave PostgreSQL setup
- **Redis Clustering**: High-availability cache with Sentinel
- **Service Redundancy**: Multiple instances with load balancing
- **Circuit Breaker**: Prevents cascade failures

### Data Consistency
- **Strong Consistency**: For ticket inventory (Redis atomic ops)
- **Eventual Consistency**: For non-critical data (event metadata)
- **ACID Transactions**: For booking confirmations

### Fault Tolerance
- **Graceful Degradation**: System remains functional during partial failures
- **Retry Mechanisms**: Exponential backoff for transient failures
- **Health Checks**: Automated failure detection and recovery

## 📈 Monitoring & Observability

### Key Metrics
- **Business**: Booking success rate, revenue, conversion
- **Technical**: Response time, error rate, throughput
- **Infrastructure**: CPU, memory, disk, network usage

### Alerting Thresholds
- **Critical**: Error rate >1%, booking success <80%
- **Warning**: Response time >1s, CPU usage >70%

### Logging Strategy
- **Structured Logging**: JSON format with correlation IDs
- **Centralized Aggregation**: ELK stack for log analysis
- **Distributed Tracing**: Request tracing across services

## 🔐 Security

### Authentication & Authorization
- **JWT Tokens**: Stateless authentication
- **Role-based Access**: User, Admin roles
- **Rate Limiting**: Prevent API abuse

### Data Protection
- **Encryption**: TLS 1.3 in transit, AES-256 at rest
- **PCI Compliance**: Secure payment data handling
- **Input Validation**: Prevent injection attacks

## 💰 Cost Analysis

### Monthly AWS Costs (Estimated)
- **Compute**: $10,972 (auto-scaling instances)
- **Database**: $4,447 (RDS + ElastiCache)
- **Storage**: $410 (EBS + RDS storage)
- **Network**: $11,664 (data transfer)
- **Total**: ~$28K/month (~$336K/year)

### Cost Optimization
- **Reserved Instances**: 40% savings on predictable workloads
- **Auto-scaling**: Aggressive scaling policies
- **Caching**: 80% reduction in database queries

## 🔄 API Examples

### Search Events
```bash
curl "http://localhost:8086/api/events/search?city=Mumbai&genre=Concert"
```

### Hold Tickets
```bash
curl -X POST http://localhost:8086/api/bookings/hold \
  -H "Content-Type: application/json" \
  -d '{"userId":123,"eventId":1,"ticketTypeId":2,"quantity":2}'
```

### Confirm Booking
```bash
curl -X POST "http://localhost:8086/api/bookings/456/confirm?paymentId=pay_xyz123"
```

## 📚 Documentation

- **[System Design](System_Design.md)**: Complete HLD/LLD documentation
- **[Architecture Diagrams](Architecture_Diagrams.md)**: Visual system architecture
- **[API Documentation](API_Documentation.md)**: Comprehensive API reference
- **[Scale Calculations](Scale_Calculations.md)**: Performance analysis and capacity planning

## 🧪 Testing

### Load Testing
```bash
# Install k6
brew install k6

# Run load test
k6 run --vus 1000 --duration 60s load-test.js
```

### Integration Tests
```bash
mvn test -Dtest=BookingIntegrationTest
```

## 🚀 Deployment

### Docker Deployment
```bash
docker build -t ticketbooking:latest .
docker run -p 8086:8086 ticketbooking:latest
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/
kubectl get pods -n ticketbooking
```

## 🔮 Future Enhancements

### Advanced Features
1. **Dynamic Pricing**: AI-based pricing optimization
2. **Seat Selection**: Interactive venue maps
3. **Recommendation Engine**: Personalized event suggestions
4. **Waitlist Management**: Automatic booking when available
5. **Mobile Push Notifications**: Real-time updates

### Technical Improvements
1. **Event Sourcing**: Complete audit trail
2. **CQRS**: Separate read/write models
3. **GraphQL**: Flexible mobile APIs
4. **Machine Learning**: Fraud detection
5. **Blockchain**: Ticket authenticity verification

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow coding standards
4. Add comprehensive tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

---

**Built with ❤️ for high-scale, fault-tolerant ticket booking**

For questions or support, please open an issue or contact the development team.