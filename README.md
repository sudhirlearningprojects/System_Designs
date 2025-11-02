# System Designs Collection

A comprehensive collection of system design implementations for various distributed systems and applications.

## 📋 Available System Designs

### 1. Dropbox Clone - Cloud Storage System
**Location**: `org.sudhir512kj.dropbox` package

A complete implementation of a cloud storage system similar to Dropbox with:
- Multi-device file synchronization
- Real-time collaboration and conflict resolution
- Data deduplication (30% storage savings)
- Scalable architecture supporting 500M users
- High availability (99.99% uptime)

**Documentation**: [docs/dropbox/](docs/dropbox/)

**Key Features**:
- File upload/download with chunking
- WebSocket-based real-time sync
- Permission-based file sharing
- Version control and history
- Hash-based deduplication
- Microservices architecture

**Scale**: 500M users, 210PB storage, 27.77 GB/s peak bandwidth

---

### 2. Payment Service - Fault-Tolerant Payment System
**Location**: `org.sudhir512kj.payment` package

A highly available and fault-tolerant payment service that guarantees exactly-once processing:
- Exactly-once payment processing
- Idempotency and duplicate prevention
- External payment processor fault tolerance
- Distributed transaction management
- High availability (99.99% uptime)

**Documentation**: [docs/payment/](docs/payment/)

**Key Features**:
- Circuit breaker pattern for resilience
- Exponential backoff retry mechanism
- Saga pattern for distributed transactions
- Multi-processor support (Stripe, PayPal, Square)
- Comprehensive audit logging
- PCI DSS compliant architecture

**Scale**: 100K TPS, $0.002 per transaction, 950B annual transactions

---

### 3. Job Scheduler - Distributed Job Scheduling System
**Location**: `org.sudhir512kj.jobscheduler` package

A reliable, scalable, and fault-tolerant distributed job scheduling system:
- One-off and recurring job scheduling
- Cron-based and interval-based scheduling
- Job pause/resume/cancel functionality
- Fault-tolerant execution with retries
- Horizontal scalability with lease-based coordination

**Documentation**: [docs/jobscheduler/](docs/jobscheduler/)

**Key Features**:
- Timing wheel for efficient scheduling
- Distributed coordination with lease management
- Exactly-once job execution guarantee
- Dead letter queue for failed jobs
- Real-time job status tracking
- Thundering herd prevention

**Scale**: Millions of scheduled jobs, 100K+ executions/sec, sub-second latency

---

### 4. Parking Lot Management System - High-Availability Parking System
**Location**: `org.sudhir512kj.parkinglot` package

A fault-tolerant parking lot management system for multi-story facilities:
- Multi-floor, multi-gate support
- Real-time spot availability tracking
- Thread-safe spot allocation
- Multiple payment methods
- High availability (99.99% uptime)

**Documentation**: [docs/parkinglot/](docs/parkinglot/)

**Key Features**:
- Atomic spot assignment (no double-booking)
- Redis caching for O(1) availability lookups
- Circuit breaker pattern for payment resilience
- Real-time display board updates
- Support for multiple vehicle types
- Microservices architecture

**Scale**: 1000+ spots, sub-second response times, concurrent vehicle processing

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+

### Running a System Design

1. **Clone the repository**
```bash
git clone https://github.com/sudhir512kj/system-designs.git
cd system-designs
```

2. **Start infrastructure services**
```bash
docker-compose up -d postgres redis kafka
```

3. **Configure environment variables**
```bash
# For Dropbox system
export DROPBOX_STORAGE_PATH=/tmp/dropbox
export DROPBOX_S3_BUCKET=dropbox-storage-bucket

# For Payment system
export STRIPE_API_KEY=sk_test_...
export PAYPAL_CLIENT_ID=...
export SQUARE_ACCESS_TOKEN=...

# For Job Scheduler system
export SCHEDULER_LEASE_DURATION=30
export SCHEDULER_HEARTBEAT_INTERVAL=10

# For Parking Lot system
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Common
export JWT_SECRET=mySecretKey
```

4. **Build and run**
```bash
mvn clean install

# Run Dropbox system
mvn spring-boot:run -Dspring-boot.run.main-class=org.sudhir512kj.dropbox.DropboxApplication

# Run Payment system
mvn spring-boot:run -Dspring-boot.run.main-class=org.sudhir512kj.payment.PaymentApplication

# Run Job Scheduler system
mvn spring-boot:run -Dspring-boot.run.main-class=org.sudhir512kj.jobscheduler.JobSchedulerApplication

# Run Parking Lot system
mvn spring-boot:run -Dspring-boot.run.main-class=org.sudhir512kj.parkinglot.ParkingLotApplication
```

## 🏗️ Project Structure

```
src/main/java/org/sudhir512kj/
├── dropbox/                    # Dropbox clone implementation
│   ├── model/                  # Domain entities
│   ├── service/                # Business logic
│   ├── repository/             # Data access
│   ├── controller/             # REST APIs
│   ├── dto/                    # Data transfer objects
│   └── config/                 # Configuration
│
├── payment/                    # Payment service implementation
│   ├── model/                  # Payment entities
│   ├── service/                # Payment business logic
│   ├── repository/             # Payment data access
│   ├── controller/             # Payment APIs
│   ├── dto/                    # Payment DTOs
│   └── config/                 # Payment configuration
│
├── parkinglot/                 # Parking lot system implementation
│   ├── model/                  # Parking entities (Vehicle, Spot, Ticket)
│   ├── service/                # Parking business logic
│   ├── repository/             # Parking data access
│   ├── controller/             # Parking APIs
│   ├── dto/                    # Parking DTOs
│   └── config/                 # Parking configuration
│
├── [future-system]/            # Next system design
│   └── ...
│
docs/
├── dropbox/                    # Dropbox documentation
│   ├── System_Design.md        # Complete HLD/LLD
│   ├── Architecture_Diagrams.md # Visual diagrams
│   ├── API_Documentation.md    # API reference
│   └── Scale_Calculations.md   # Performance analysis
│
├── payment/                    # Payment service documentation
│   ├── System_Design.md        # Payment system HLD/LLD
│   ├── Architecture_Diagrams.md # Payment architecture
│   ├── API_Documentation.md    # Payment API reference
│   └── Scale_Calculations.md   # Payment performance analysis
│
├── parkinglot/                 # Parking lot documentation
│   ├── System_Design.md        # Parking system HLD/LLD
│   ├── Architecture_Diagrams.md # Parking architecture
│   ├── API_Documentation.md    # Parking API reference
│   └── Scale_Calculations.md   # Parking performance analysis
│
└── [future-system]/            # Future system docs
```

## 📚 Documentation Standards

Each system design includes:
- **System_Design.md**: Complete High-Level and Low-Level Design
- **Architecture_Diagrams.md**: Visual system architecture using Mermaid
- **API_Documentation.md**: Comprehensive API reference
- **Scale_Calculations.md**: Back-of-envelope calculations and performance analysis
- **README.md**: System-specific overview and quick start

## 🎯 Design Principles

All system designs follow these principles:
- **Scalability**: Horizontal scaling with load balancers
- **Reliability**: High availability with failover mechanisms
- **Performance**: Sub-second response times for critical operations
- **Security**: Authentication, authorization, and data encryption
- **Maintainability**: Clean code with proper separation of concerns
- **Observability**: Comprehensive logging and monitoring

## 🔧 Technology Stack

### Backend
- **Framework**: Spring Boot 3.2, Java 17
- **Database**: PostgreSQL (metadata), Redis (caching)
- **Messaging**: Apache Kafka
- **Storage**: Amazon S3 / Local filesystem
- **Real-time**: WebSocket (STOMP)

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Orchestration**: Kubernetes (production)
- **Monitoring**: Micrometer, Prometheus, Grafana
- **CI/CD**: GitHub Actions

## 🧪 Testing

### Running Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn verify -P integration-tests

# Load testing (example for Parking Lot)
k6 run docs/parkinglot/load-test.js
```

## 📈 Performance Benchmarks

Each system design includes detailed performance analysis:
- Throughput and latency measurements
- Scalability limits and bottlenecks
- Cost analysis for cloud deployment
- Optimization recommendations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch for your system design
3. Follow the documentation standards
4. Include comprehensive tests
5. Submit a pull request

### Adding a New System Design

1. Create package: `org.sudhir512kj.[system-name]`
2. Implement following structure:
   - `model/` - Domain entities
   - `service/` - Business logic
   - `repository/` - Data access
   - `controller/` - API endpoints
   - `config/` - Configuration
3. Add documentation in `docs/[system-name]/`
4. Update this README with system overview

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- AWS for reliable cloud infrastructure
- Open source community for tools and libraries

---

**Built with ❤️ by Sudhir Kumar**

For questions or support, please open an issue or visit my [portfolio website](https://sudhirmeenaswe.netlify.app/) and contact via [contact form](https://sudhirmeenaswe.netlify.app/#contact).