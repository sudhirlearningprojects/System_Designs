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

### 5. Digital Payment Platform - PhonePe/GPay Clone
**Location**: `org.sudhir512kj.digitalpayment` package

A highly scalable digital payment platform enabling instant money transfers:
- P2P and P2M transactions
- Multiple payment methods (UPI, Cards, Net Banking, Wallet)
- Real-time fraud detection
- Strong consistency for financial data
- High availability (99.99% uptime)

**Documentation**: [docs/digitalpayment/](docs/digitalpayment/)

**Key Features**:
- Atomic wallet operations with pessimistic locking
- Idempotency for duplicate prevention
- Strategy pattern for PSP integration
- Real-time fraud detection and rate limiting
- Comprehensive transaction lifecycle management
- Event-driven architecture with Kafka

**Scale**: 100M users, 50K TPS, sub-2 second response times

---

### 6. Ticket Booking Platform - Ticketmaster/BookMyShow Clone
**Location**: `org.sudhir512kj.ticketbooking` package

A highly available and scalable ticket booking platform that prevents overselling:
- Real-time inventory management with Redis
- Atomic ticket hold and release mechanism
- High-concurrency booking during flash sales
- Secure payment processing integration
- Zero overselling guarantee

**Documentation**: [docs/ticketbooking/](docs/ticketbooking/)

**Key Features**:
- Redis-based inventory management for zero overselling
- 10-minute ticket hold with automatic expiry
- Pessimistic locking for critical operations
- Multi-layer caching for optimal performance
- Circuit breaker pattern for payment resilience
- Event search and discovery with filters

**Scale**: 50M users, 100K concurrent requests, 500K bookings/day

---

### 7. Instagram Clone - Social Media Platform
**Location**: `org.sudhir512kj.instagram` package

A highly scalable social media platform with real-time features and billions of users:
- User profiles and social graph management
- Photo/video posts with media processing
- News feed with hybrid push/pull algorithm
- Real-time notifications and messaging
- Stories with 24-hour expiry
- Advanced search with Elasticsearch
- Content moderation and security

**Documentation**: [docs/instagram/](docs/instagram/)

**Key Features**:
- Hybrid feed generation (celebrity vs regular users)
- Multi-layer caching strategy (L1/L2/L3)
- Async media processing pipeline
- Real-time WebSocket connections
- Microservices architecture
- AI-powered content moderation
- Global CDN for media delivery

**Scale**: 2B users, 100M DAU, 500M posts/day, 734PB storage

---

### 8. API Rate Limiter - Distributed Rate Limiting System
**Location**: `org.sudhir512kj.ratelimiter` package

A highly available and scalable distributed API rate limiter with multi-layered protection:
- Multi-algorithm support (Sliding Window, Token Bucket, Fixed Window, Leaky Bucket)
- Multi-scope protection (User, IP, API Key, Tenant, Global)
- Redis-based distributed state management
- Dynamic rule configuration with priority matching
- Real-time analytics and monitoring
- DDoS protection and abuse prevention

**Documentation**: [docs/ratelimiter/](docs/ratelimiter/)

**Key Features**:
- **Annotation-Based**: Simple `@RateLimit` annotations on methods
- **Multi-Algorithm Support**: Sliding Window, Token Bucket, Fixed Window, Leaky Bucket
- **Multi-Scope Protection**: User, IP, API Key, Tenant, Global, Custom (SpEL)
- **Multi-Layer Defense**: Combine multiple rate limits for comprehensive protection
- **Real-time Management**: Dynamic rule updates without restarts
- **High Availability**: Redis clustering with automatic failover
- **Comprehensive Analytics**: Request tracking and violation monitoring
- **Developer-Friendly**: Clean annotations, no configuration files needed

**Scale**: 1M requests/second, sub-millisecond latency, 99.99% availability

**Quick Example**:
```java
@GetMapping("/api/data")
@RateLimit(requests = 100, window = 3600, scope = RateLimit.Scope.USER)
public String getData() {
    return "data";
}
```

---

### 9. Distributed Notification System
**Location**: `org.sudhir512kj.notification` package

A highly scalable, fault-tolerant distributed notification system for multi-channel message delivery:
- Multi-channel support (Email, SMS, Push, In-App, WebSocket)
- Retry mechanism with exponential backoff
- Dead Letter Queue (DLQ) for failed messages
- User preference management with quiet hours
- Priority-based delivery (Critical <100ms, High <1s)
- Scalable fan-out for broadcast notifications

**Documentation**: [docs/notification/](docs/notification/)

**Key Features**:
- **Multi-Channel Delivery**: Email, SMS, Push, In-App, WebSocket
- **Reliability**: Exponential backoff retry + DLQ + Circuit breaker
- **User Preferences**: Channel opt-in/opt-out, quiet hours, timezone support
- **Priority Queues**: CRITICAL, HIGH, MEDIUM, LOW with SLA guarantees
- **Scalable Fan-Out**: Efficient broadcast to millions of users
- **Idempotency**: Duplicate request prevention
- **Provider Integration**: SendGrid, Twilio, FCM, APNS
- **Observability**: Comprehensive metrics and distributed tracing

**Scale**: 10M notifications/min, 500M users, 99.99% availability

**Quick Example**:
```java
{
  "userId": "user123",
  "type": "TRANSACTIONAL",
  "priority": "HIGH",
  "channels": ["EMAIL", "PUSH"],
  "templateId": "order-confirmation",
  "idempotencyKey": "order-123-notif"
}
```

---

### 10. Uber Clone - Global Ride-Hailing Platform
**Location**: `org.sudhir512kj.uber` package

A production-ready, scalable ride-hailing platform like Uber supporting millions of concurrent users:
- Real-time driver-rider matching with <1s latency
- Geo-spatial indexing for efficient location queries
- Dynamic surge pricing based on demand/supply
- Real-time location tracking with WebSocket
- Multi-region deployment for global scale
- High availability (99.99% uptime)

**Documentation**: [docs/uber/](docs/uber/)

**Key Features**:
- **gRPC Internal Communication**: 5-10x faster than REST (2ms vs 15ms latency)
- **WebSocket Real-time Streaming**: Persistent connections for location updates
- **Kafka Event Streaming**: 1M events/sec for analytics and audit logs
- **Intelligent Matching**: Multi-factor scoring (distance, rating, experience)
- **Geo-Sharding**: Geohash-based partitioning reduces search space 1000x
- **Redis Geospatial**: GEOADD/GEORADIUS for O(log N) location queries
- **Dynamic Pricing**: Surge multiplier based on demand/supply ratio
- **Payment Integration**: Stripe, PayPal with idempotency
- **Fault Tolerance**: Circuit breaker, retry, multi-region failover

**Scale**: 10M concurrent users, 75K location updates/sec, 100TB storage

**Quick Example**:
```java
// Request a ride
POST /api/v1/rides/request
{
  "riderId": "uuid",
  "pickupLocation": {"latitude": 37.7749, "longitude": -122.4194},
  "dropoffLocation": {"latitude": 37.8044, "longitude": -122.2712},
  "vehicleType": "UBERX"
}
```

---

### 11. Google Docs Clone - Collaborative Document Editing Platform
**Location**: `org.sudhir512kj.googledocs` package

A production-ready collaborative document editing platform with real-time synchronization:
- Real-time multi-user editing with Operational Transformation
- Version history with restore capability
- Suggesting mode for tracked changes
- Comments with threaded replies and emoji reactions
- Offline editing with sync on reconnect
- Auto-save every 3 seconds

**Documentation**: [docs/googledocs/](docs/googledocs/)

**Key Features**:
- **Operational Transformation (OT)**: Conflict-free concurrent editing
- **WebSocket Real-time Sync**: Sub-500ms latency for operations
- **Live Cursor Tracking**: See where other users are editing
- **Version Control**: Complete audit trail with restore
- **Suggesting Mode**: Track changes with accept/reject workflow
- **Comments & Reactions**: Threaded discussions with emoji support
- **Granular Permissions**: Owner, Editor, Commenter, Viewer
- **Watermarks**: Custom text/image watermarks
- **Export Formats**: Word, PDF, Markdown, HTML

**Scale**: 1B users, 100M DAU, 5B documents, 5M ops/sec, 11PB storage

**Quick Example**:
```javascript
// Real-time collaboration via WebSocket
stompClient.subscribe('/topic/document/' + docId, function(message) {
    const operation = JSON.parse(message.body);
    applyOperation(operation);
});
```

---

### 12. TinyURL Clone - URL Shortener System
**Location**: `org.sudhir512kj.urlshortener` package

A highly scalable URL shortener service similar to TinyURL with sub-100ms redirect latency:
- Convert long URLs to short, memorable links
- Custom aliases and expiration dates
- Real-time analytics and click tracking
- High availability (99.99% uptime)
- Handles billions of URLs and redirects

**Documentation**: [docs/urlshortener/](docs/urlshortener/)

**Key Features**:
- **Base62 Encoding**: 3.5 trillion unique short URLs
- **Multi-Layer Caching**: Application cache + Redis + Database
- **Rate Limiting**: Prevent abuse with sliding window algorithm
- **URL Validation**: Block malicious domains and validate format
- **Analytics Tracking**: Geographic data, referrers, user agents
- **Custom Aliases**: User-defined short URLs
- **Expiration Support**: Time-based URL expiry
- **Circuit Breaker**: Database fault tolerance

**Scale**: 100M URLs/day, 10B redirects/day, <100ms latency

**Quick Example**:
```bash
# Shorten URL
curl -X POST http://localhost:8092/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com"}

# Access short URL
curl -I http://localhost:8092/abc123
```

---

### 13. WhatsApp Messenger - Real-time Messaging Platform
**Location**: `org.sudhir512kj.whatsapp` package

A highly scalable, real-time messaging platform similar to WhatsApp supporting billions of users:
- Real-time messaging with sub-100ms latency
- Individual and group chats (up to 256 participants)
- Message types: Text, Image, Video, Audio, Document, Location, Contact, Sticker
- Message status tracking: Sent ✓, Delivered ✓✓, Read ✓✓ (blue ticks)
- WhatsApp Status (24-hour expiry stories)
- Typing indicators and online presence
- High availability (99.99% uptime)

**Documentation**: [docs/whatsapp/](docs/whatsapp/)

**Key Features**:
- **Real-time WebSocket Communication**: Sub-100ms message delivery
- **Multi-Message Types**: Text, media, location, contact sharing
- **Group Management**: Create groups, add/remove participants, admin controls
- **Status Updates**: 24-hour expiry stories with view tracking
- **Typing Indicators**: Real-time typing status and online presence
- **Message Features**: Reply, forward, delete for everyone (within 1 hour)
- **Read Receipts**: Delivery confirmation and read status tracking
- **Media Handling**: Image/video compression, thumbnail generation
- **Search Functionality**: Message search within chats
- **Privacy Controls**: Last seen, profile visibility settings

**Scale**: 2B users, 1B DAU, 100B messages/day, 1.2M messages/sec peak

**Quick Example**:
```javascript
// Real-time messaging via WebSocket
stompClient.subscribe('/topic/chat/chat123', function(message) {
    const messageData = JSON.parse(message.body);
    displayNewMessage(messageData);
});

// Send message via REST API
fetch('/api/v1/messages/send?senderId=user123', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
        chatId: 'chat123',
        content: 'Hello, how are you?',
        type: 'TEXT'
    })
});
```

---

### 14. Cloudflare Clone - CDN & Web Security Platform
**Location**: `org.sudhir512kj.cloudflare` package

A comprehensive CDN and web security platform that sits between websites and users:
- Global CDN with 200+ edge locations worldwide
- Multi-layered DDoS protection (L3/L4/L7)
- Web Application Firewall (WAF) with OWASP Top 10 protection
- DNS management with 1.1.1.1 resolver
- SSL/TLS certificate management and encryption
- Load balancing and traffic distribution
- Real-time analytics and security insights

**Documentation**: [docs/cloudflare/](docs/cloudflare/)

**Key Features**:
- **Global CDN**: 200+ edge locations with <50ms latency
- **DDoS Mitigation**: Block 182B+ threats/day with 100Tbps capacity
- **WAF Engine**: Pattern-based security rules with custom actions
- **DNS Service**: Authoritative DNS with fast resolution
- **SSL/TLS**: Automatic certificate provisioning and renewal
- **Rate Limiting**: Token bucket algorithm for API protection
- **Cache Management**: Multi-layer caching with Redis
- **Analytics**: Real-time traffic and security monitoring
- **Edge Computing**: Serverless functions at the edge

**Scale**: 45M requests/sec, 200+ edge locations, 99.99% availability

**Quick Example**:
```bash
# Add domain to Cloudflare
curl -X POST http://localhost:8094/api/v1/zones \
  -H "Content-Type: application/json" \
  -d '{"domain": "example.com", "planType": "FREE"}'

# Access through CDN
curl -H "Host: example.com" http://localhost:8094/
```

---

### 15. TikTok Clone - Short Video & Live Streaming Platform
**Location**: `org.sudhir512kj.tiktok` package

A production-ready short-form video sharing platform with live streaming capabilities:
- Short video upload and sharing (15-60 seconds)
- AI-powered For You feed with personalized recommendations
- Live streaming with RTMP ingestion and HLS delivery
- Real-time interactions (likes, comments, gifts)
- Video effects and filters
- Social features (follow, duet, stitch)
- High availability (99.99% uptime)

**Documentation**: [docs/tiktok/](docs/tiktok/)

**Key Features**:
- **Short Video Platform**: Upload, transcode, and share 15-60 second videos
- **For You Feed**: Hybrid recommendation algorithm (collaborative + content-based + trending)
- **Live Streaming**: RTMP ingestion, multi-bitrate transcoding, HLS delivery
- **Real-time Chat**: WebSocket-based live comments, likes, and gifts
- **Video Processing**: FFmpeg transcoding to multiple resolutions (360p-1080p)
- **CDN Delivery**: CloudFront for global video distribution
- **Recommendation Engine**: ML-powered personalized feed generation
- **Social Graph**: Follow, like, comment, share functionality
- **Analytics**: View count, engagement metrics, trending videos

**Scale**: 1.5B MAU, 500M DAU, 1B videos/day, 500PB storage, 529 Gbps bandwidth

**Quick Example**:
```bash
# Upload video
curl -X POST http://localhost:8095/api/v1/videos/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@video.mp4" \
  -F "caption=Amazing dance! #fyp"

# Get For You feed
curl http://localhost:8095/api/v1/videos/feed/foryou?userId=123&page=0&size=20

# Create live stream
curl -X POST http://localhost:8095/api/v1/live/create?userId=123 \
  -H "Content-Type: application/json" \
  -d '{"title": "Live Q&A", "description": "Ask me anything!"}'
```

---

### 16. Cloud Infrastructure Platform - AWS/Azure Clone
**Location**: `org.sudhir512kj.cloudinfra` package

A comprehensive cloud infrastructure management platform similar to AWS Console/Azure Portal:
- Virtual machine provisioning and lifecycle management
- Object storage with S3-like buckets
- Virtual Private Cloud (VPC) networking
- Managed database services
- Load balancers for traffic distribution
- Multi-region deployment support
- High availability (99.99% uptime)

**Documentation**: [docs/cloudinfra/](docs/cloudinfra/)

**Key Features**:
- **Compute Service**: VM creation with multiple instance types (t2.micro to t2.large)
- **Storage Service**: S3-like object storage with versioning and encryption
- **Network Service**: VPC, subnets, security groups for isolation
- **Database Service**: Managed PostgreSQL, MySQL, MongoDB, Redis, Cassandra
- **Load Balancers**: Application, Network, and Gateway load balancers
- **Async Provisioning**: Non-blocking resource creation with state tracking
- **Multi-Region**: Deploy resources across multiple geographic regions
- **Pay-per-Use**: Cost-effective pricing model
- **Resource Tagging**: Organize and track cloud resources

**Scale**: 1M+ VMs, 100PB storage, 100K requests/sec, multi-region

**Quick Example**:
```bash
# Create VM
curl -X POST http://localhost:8096/api/v1/compute/vms \
  -H "X-Account-Id: acc-123" \
  -H "Content-Type: application/json" \
  -d '{"name":"web-server","region":"us-east-1","instanceType":"t2.medium","diskGb":50}'

# Create storage bucket
curl -X POST http://localhost:8096/api/v1/storage/buckets \
  -H "X-Account-Id: acc-123" \
  -d '{"bucketName":"my-data","region":"us-east-1","storageClass":"STANDARD"}'

# Create VPC
curl -X POST http://localhost:8096/api/v1/network/vpcs \
  -H "X-Account-Id: acc-123" \
  -d '{"name":"main-vpc","region":"us-east-1","cidrBlock":"10.0.0.0/16"}'
```

---

### 17. Distributed Database System - PostgreSQL-Based Sharded Database
**Location**: `org.sudhir512kj.distributeddb` package

A highly scalable, available, and fault-tolerant distributed database system built on PostgreSQL:
- Multi-criteria sharding (Hash, Range, Geo, Tenant-based)
- Automatic failover with leader election
- Read replicas for horizontal scaling
- Cross-shard distributed transactions with 2PC
- Intelligent query routing and aggregation
- Strong and eventual consistency models
- High availability (99.99% uptime)

**Documentation**: [docs/distributeddb/](docs/distributeddb/)

**Key Features**:
- **Multi-Criteria Sharding**: Hash, Range, Geo-location, Tenant-based strategies
- **Automatic Failover**: Health monitoring with replica promotion
- **Read Scalability**: Load-balanced read replicas per shard
- **Distributed Transactions**: Two-Phase Commit for ACID guarantees
- **Query Routing**: Intelligent routing based on sharding criteria
- **Consistency Models**: Strong, Eventual, and Causal consistency
- **Replication**: Async replication with configurable lag tolerance
- **Health Monitoring**: Continuous health checks every 5 seconds

**Scale**: 100+ shards, 1M read QPS, 500K write QPS, 100TB+ storage, <10ms latency

**Quick Example**:
```bash
# Register shard
curl -X POST http://localhost:8097/api/v1/db/shards \
  -H "Content-Type: application/json" \
  -d '{"shardId":"shard-1","type":"HASH","strategy":"CONSISTENT_HASH","primaryNode":"node-1","replicaNodes":["node-2","node-3"],"region":"US-EAST"}'

# Execute query
curl -X POST http://localhost:8097/api/v1/db/query \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM users WHERE user_id = ?","parameters":{"user_id":"user123"},"type":"SELECT","shardingKey":"user123","consistencyLevel":"STRONG"}'
```

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

4. **Build and run using the convenience script**
```bash
mvn clean install

# Use the run-systems.sh script for easy startup
./run-systems.sh parkinglot      # Port 8080
./run-systems.sh dropbox         # Port 8081
./run-systems.sh payment         # Port 8082
./run-systems.sh jobscheduler    # Port 8083
./run-systems.sh digitalpayment  # Port 8084
./run-systems.sh ticketbooking   # Port 8086
./run-systems.sh instagram       # Port 8087
./run-systems.sh ratelimiter     # Port 8088
./run-systems.sh notification    # Port 8089
./run-systems.sh uber            # Port 8090
./run-systems.sh googledocs      # Port 8091
./run-systems.sh urlshortener    # Port 8092
./run-systems.sh whatsapp        # Port 8093
./run-systems.sh cloudflare      # Port 8094
./run-systems.sh tiktok          # Port 8095
./run-systems.sh cloudinfra      # Port 8096
./run-systems.sh distributeddb   # Port 8097
./run-systems.sh spotify         # Port 8098
./run-systems.sh probability     # Port 8099
```

**Alternative: Run directly with Maven profiles**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=parkinglot
mvn spring-boot:run -Dspring-boot.run.profiles=dropbox
mvn spring-boot:run -Dspring-boot.run.profiles=payment
mvn spring-boot:run -Dspring-boot.run.profiles=jobscheduler
mvn spring-boot:run -Dspring-boot.run.profiles=digitalpayment
mvn spring-boot:run -Dspring-boot.run.profiles=ticketbooking
mvn spring-boot:run -Dspring-boot.run.profiles=instagram
mvn spring-boot:run -Dspring-boot.run.profiles=ratelimiter
mvn spring-boot:run -Dspring-boot.run.profiles=notification
mvn spring-boot:run -Dspring-boot.run.profiles=uber
mvn spring-boot:run -Dspring-boot.run.profiles=googledocs
mvn spring-boot:run -Dspring-boot.run.profiles=urlshortener
mvn spring-boot:run -Dspring-boot.run.profiles=whatsapp
mvn spring-boot:run -Dspring-boot.run.profiles=cloudflare
mvn spring-boot:run -Dspring-boot.run.profiles=tiktok
mvn spring-boot:run -Dspring-boot.run.profiles=cloudinfra
mvn spring-boot:run -Dspring-boot.run.profiles=distributeddb
mvn spring-boot:run -Dspring-boot.run.profiles=spotify
mvn spring-boot:run -Dspring-boot.run.profiles=probability
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
├── ticketbooking/              # Ticket booking system implementation
│   ├── model/                  # Booking entities (Event, Booking, TicketType)
│   ├── service/                # Booking business logic
│   ├── repository/             # Booking data access
│   ├── controller/             # Booking APIs
│   ├── dto/                    # Booking DTOs
│   └── config/                 # Booking configuration
│
├── instagram/                  # Instagram clone implementation
│   ├── model/                  # Social entities (User, Post, Story, Comment)
│   ├── service/                # Social business logic
│   ├── repository/             # Social data access
│   ├── controller/             # Social APIs
│   ├── dto/                    # Social DTOs
│   └── config/                 # Social configuration
│
├── ratelimiter/                # API Rate Limiter implementation
│   ├── model/                  # Rate limit entities (Rule, Attempt)
│   ├── service/                # Rate limiting business logic
│   ├── repository/             # Rate limit data access
│   ├── controller/             # Rate limit APIs
│   ├── dto/                    # Rate limit DTOs
│   ├── algorithm/              # Rate limiting algorithms
│   ├── interceptor/            # Request interceptors
│   └── config/                 # Rate limiter configuration
│
├── notification/               # Distributed Notification System
│   ├── model/                  # Notification entities (Notification, DLQEntry)
│   ├── service/                # Notification business logic
│   ├── repository/             # Notification data access
│   ├── controller/             # Notification APIs
│   ├── dto/                    # Notification DTOs
│   ├── worker/                 # Channel workers (Email, SMS, Push)
│   └── config/                 # Notification configuration
│
├── uber/                       # Uber Clone - Ride-Hailing Platform
│   ├── model/                  # Ride entities (User, Driver, Ride, Vehicle)
│   ├── service/                # Ride-hailing business logic
│   ├── repository/             # Ride data access
│   ├── controller/             # Ride APIs
│   ├── dto/                    # Ride DTOs
│   ├── websocket/              # WebSocket for real-time tracking
│   └── config/                 # Uber configuration
│
├── googledocs/                 # Google Docs Clone - Collaborative Editing
│   ├── model/                  # Document entities (Document, Version, Comment)
│   ├── service/                # Document business logic
│   ├── repository/             # Document data access
│   ├── controller/             # Document APIs
│   ├── dto/                    # Document DTOs
│   ├── websocket/              # WebSocket for real-time collaboration
│   ├── ot/                     # Operational Transformation
│   └── config/                 # Google Docs configuration
│
├── urlshortener/               # URL Shortener implementation
│   ├── model/                  # URL entities (URL, Analytics)
│   ├── service/                # URL shortening business logic
│   ├── repository/             # URL data access
│   ├── controller/             # URL APIs
│   ├── dto/                    # URL DTOs
│   └── config/                 # URL shortener configuration
│
├── whatsapp/                   # WhatsApp Messenger implementation
│   ├── model/                  # Messaging entities (User, Chat, Message, Status)
│   ├── service/                # Messaging business logic
│   ├── repository/             # Messaging data access
│   ├── controller/             # Messaging APIs
│   ├── dto/                    # Messaging DTOs
│   ├── websocket/              # WebSocket for real-time messaging
│   └── config/                 # WhatsApp configuration
│
├── cloudflare/                 # Cloudflare Clone - CDN & Security Platform
│   ├── model/                  # CDN entities (Zone, DNSRecord, SecurityRule)
│   ├── service/                # CDN business logic
│   ├── repository/             # CDN data access
│   ├── controller/             # CDN APIs
│   ├── dto/                    # CDN DTOs
│   ├── filter/                 # Security filters (DDoS, WAF)
│   └── config/                 # Cloudflare configuration
│
├── tiktok/                     # TikTok Clone - Short Video & Live Streaming
│   ├── model/                  # Video entities (User, Video, LiveStream, Comment)
│   ├── service/                # Video business logic
│   ├── repository/             # Video data access
│   ├── controller/             # Video APIs
│   ├── dto/                    # Video DTOs
│   ├── websocket/              # WebSocket for live streaming
│   ├── processor/              # Video processing workers
│   └── config/                 # TikTok configuration
│
├── cloudinfra/                 # Cloud Infrastructure Platform
│   ├── model/                  # Cloud entities (Resource, VM, StorageBucket, VPC)
│   ├── service/                # Cloud business logic
│   ├── repository/             # Cloud data access
│   ├── controller/             # Cloud APIs
│   ├── dto/                    # Cloud DTOs
│   └── config/                 # Cloud configuration
│
├── distributeddb/              # Distributed Database System
│   ├── model/                  # DB entities (ShardConfig, DatabaseNode, QueryRequest)
│   ├── service/                # DB business logic
│   ├── repository/             # DB data access
│   ├── controller/             # DB APIs
│   ├── dto/                    # DB DTOs
│   ├── router/                 # Shard routing and registry
│   ├── replication/            # Replication service
│   └── config/                 # DB configuration
│
├── [future-system]/            # Next system design
│   └── ...
│
docs/
├── webflux/                    # Spring WebFlux documentation
│   ├── README.md               # WebFlux overview
│   ├── Getting_Started.md      # Setup and first application
│   ├── Annotated_Controllers.md # @RestController approach
│   ├── Functional_Endpoints.md # RouterFunction approach
│   ├── WebClient.md            # Reactive HTTP client
│   ├── Reactive_Data_Access.md # R2DBC and reactive repositories
│   ├── Error_Handling.md       # Exception management
│   ├── Testing.md              # WebTestClient and testing strategies
│   ├── Performance.md          # Optimization and backpressure
│   └── Examples.md             # Real-world production patterns
│
├── webclient/                  # Spring WebClient documentation
│   ├── README.md               # WebClient overview and comparison
│   ├── Getting_Started.md      # Basic usage and setup
│   ├── Configuration.md        # Connection pool, SSL, timeouts
│   ├── Request_Building.md     # Building HTTP requests
│   ├── Response_Handling.md    # Processing responses
│   ├── Error_Handling.md       # Exception management
│   ├── Resilience_Patterns.md  # Retry, timeout, circuit breaker
│   ├── Authentication.md       # OAuth2, JWT, Basic Auth
│   ├── Testing.md              # MockWebServer and testing
│   ├── Performance.md          # Optimization strategies
│   └── Examples.md             # Real-world integrations
│
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
├── ticketbooking/              # Ticket booking documentation
│   ├── System_Design.md        # Ticket booking system HLD/LLD
│   ├── Architecture_Diagrams.md # Ticket booking architecture
│   ├── API_Documentation.md    # Ticket booking API reference
│   ├── Scale_Calculations.md   # Ticket booking performance analysis
│   └── README.md               # Ticket booking overview
│
├── instagram/                  # Instagram clone documentation
│   ├── System_Design.md        # Instagram system HLD/LLD
│   ├── Architecture_Diagrams.md # Instagram architecture
│   ├── API_Documentation.md    # Instagram API reference
│   ├── Scale_Calculations.md   # Instagram performance analysis
│   └── README.md               # Instagram overview
│
├── ratelimiter/                # API Rate Limiter documentation
│   ├── System_Design.md        # Complete system design with theory
│   ├── Theory_and_Concepts.md  # Rate limiting fundamentals and algorithms
│   ├── Annotation_Usage_Guide.md # Complete annotation usage guide
│   ├── Beginner_Tutorial.md    # Step-by-step tutorial for beginners
│   ├── Flow_Diagram.md         # Complete flow diagrams and visualizations
│   ├── API_Documentation.md    # API reference and examples
│   └── Scale_Calculations.md   # Performance analysis and cost calculations
│
├── notification/               # Distributed Notification System documentation
│   ├── System_Design.md        # Notification system HLD/LLD
│   ├── API_Documentation.md    # Notification API reference
│   ├── Scale_Calculations.md   # Notification performance analysis
│   └── README.md               # Notification overview
│
├── uber/                       # Uber Clone documentation
│   ├── System_Design.md        # Uber system HLD/LLD with geo-location deep dive
│   ├── API_Documentation.md    # Uber API reference
│   ├── Scale_Calculations.md   # Uber performance analysis
│   └── README.md               # Uber overview
│
├── googledocs/                 # Google Docs Clone documentation
│   ├── System_Design.md        # Google Docs system HLD/LLD with OT deep dive
│   ├── API_Documentation.md    # Google Docs API reference
│   ├── Scale_Calculations.md   # Google Docs performance analysis
│   └── README.md               # Google Docs overview
│
├── urlshortener/               # URL Shortener documentation
│   ├── System_Design.md        # URL shortener system HLD/LLD
│   ├── API_Documentation.md    # URL shortener API reference
│   └── README.md               # URL shortener overview
│
├── whatsapp/                   # WhatsApp Messenger documentation
│   ├── System_Design.md        # WhatsApp system HLD/LLD with real-time messaging
│   ├── API_Documentation.md    # WhatsApp API reference
│   ├── Scale_Calculations.md   # WhatsApp performance analysis
│   └── README.md               # WhatsApp overview
│
├── cloudflare/                 # Cloudflare Clone documentation
│   ├── System_Design.md        # Cloudflare system HLD/LLD with CDN and security
│   ├── API_Documentation.md    # Cloudflare API reference
│   ├── Scale_Calculations.md   # Cloudflare performance analysis
│   └── README.md               # Cloudflare overview
│
├── tiktok/                     # TikTok Clone documentation
│   ├── System_Design.md        # TikTok system HLD/LLD with video streaming
│   ├── API_Documentation.md    # TikTok API reference
│   ├── Scale_Calculations.md   # TikTok performance analysis
│   └── README.md               # TikTok overview
│
├── cloudinfra/                 # Cloud Infrastructure documentation
│   ├── System_Design.md        # Cloud platform HLD/LLD
│   ├── API_Documentation.md    # Cloud API reference
│   └── README.md               # Cloud platform overview
│
├── distributeddb/              # Distributed Database documentation
│   ├── System_Design.md        # Distributed DB HLD/LLD
│   ├── API_Documentation.md    # Distributed DB API reference
│   └── README.md               # Distributed DB overview
│
├── spotify/                    # Spotify Clone documentation
│   ├── System_Design.md        # Spotify system HLD/LLD with streaming
│   ├── API_Documentation.md    # Spotify API reference
│   ├── Scale_Calculations.md   # Spotify performance analysis
│   └── README.md               # Spotify overview
│
├── probability/                # Probability Management System documentation
│   ├── System_Design.md        # Prediction market HLD/LLD with LMSR
│   ├── API_Documentation.md    # Prediction market API reference
│   └── README.md               # Prediction market overview
│
└── [future-system]/            # Future system docs
```

## 📚 Enhanced Documentation Standards

Each system design includes comprehensive documentation following enterprise standards:

### Core Documentation
- **System_Design.md**: Complete HLD/LLD with ByteByteGo principles and WhatsApp's actual tech stack references
- **API_Documentation.md**: Production-ready API reference with SDKs, error handling, and integration examples
- **Scale_Calculations.md**: Detailed performance analysis with real-world capacity planning
- **README.md**: Quick start guide with architecture highlights and deployment instructions

### Enhanced Features
- **Software Design Principles**: SOLID principles, clean architecture, and design patterns
- **Error Handling**: Comprehensive exception hierarchy with proper HTTP status codes
- **Security Documentation**: Input validation, authorization, rate limiting, and security best practices
- **Performance Optimization**: Multi-layer caching, horizontal scaling, and optimization strategies
- **Integration Examples**: Real-world code examples for React Native, Flutter, and web applications
- **Monitoring & Observability**: Health checks, metrics, logging, and alerting guidelines
- **Development Workflow**: Testing strategies, CI/CD pipelines, and deployment procedures

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
- **Reactive**: Spring WebFlux with Project Reactor
- **Database**: PostgreSQL (metadata), Redis (caching), R2DBC (reactive)
- **Messaging**: Apache Kafka
- **Storage**: Amazon S3 / Local filesystem
- **Real-time**: WebSocket (STOMP)

### Spring WebFlux Features
- **Non-blocking I/O**: Event-driven architecture with Netty
- **Reactive Streams**: Mono and Flux for async data processing
- **WebClient**: Reactive HTTP client replacing RestTemplate
- **R2DBC**: Reactive database connectivity
- **Backpressure**: Built-in flow control
- **High Concurrency**: Handle 10K+ concurrent connections
- **Documentation**: [Complete WebFlux Guide](docs/webflux/)

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

## 🎆 Enhanced System Quality

### Software Engineering Excellence
- **Clean Architecture**: Layered design with proper separation of concerns
- **SOLID Principles**: Single responsibility, open/closed, dependency inversion
- **Design Patterns**: Repository, Factory, Observer, Strategy patterns
- **Exception Handling**: Typed exceptions with proper error propagation
- **Input Validation**: Comprehensive validation with meaningful error messages
- **Security**: Authorization, rate limiting, input sanitization

### Production Readiness
- **Scalability**: Horizontal scaling with load balancers and connection management
- **Reliability**: Circuit breakers, retry mechanisms, graceful degradation
- **Performance**: Multi-layer caching, async processing, connection pooling
- **Monitoring**: Health checks, metrics, distributed tracing
- **Documentation**: Comprehensive API docs, integration guides, deployment instructions
- **Testing**: Unit tests, integration tests, load testing scenarios

### Technology Stack Highlights
- **Backend**: Java 17 + Spring Boot 3.2 with reactive programming
- **Real-time**: WebSocket with STOMP + SockJS fallback
- **Messaging**: Apache Kafka with exactly-once semantics
- **Caching**: Redis Cluster with consistent hashing
- **Databases**: PostgreSQL + Cassandra + Redis (multi-database strategy)
- **Infrastructure**: Docker + Kubernetes with auto-scaling

---

**Built with ❤️ by Sudhir Meena**

🌟 **Enterprise-Grade System Designs** | 🚀 **Production-Ready Architecture** | 📚 **Comprehensive Documentation**

For questions or support, please open an issue or visit my [portfolio website](https://sudhirmeenaswe.netlify.app/) and contact via [contact form](https://sudhirmeenaswe.netlify.app/#contact).
### 18. Spotify Clone - Music Streaming Platform
**Location**: `org.sudhir512kj.spotify` package

A production-ready music streaming platform similar to Spotify supporting millions of users:
- Music streaming in multiple qualities (96kbps to lossless FLAC)
- Full-text search with Elasticsearch
- Playlist creation and management
- Offline downloads for premium users
- Artist dashboard for music uploads
- User library for favorites
- Listening history analytics
- High availability (99.99% uptime)

**Documentation**: [docs/spotify/](docs/spotify/)

**Key Features**:
- **Multi-Quality Streaming**: 96/160/320 kbps, FLAC for different tiers
- **Search Engine**: Elasticsearch for full-text search with fuzzy matching
- **Playlist Management**: Create, edit, share playlists
- **Offline Mode**: Download tracks for offline listening (Premium)
- **Artist Dashboard**: Upload tracks, manage catalog, view analytics
- **User Library**: Save favorite tracks, albums, artists
- **Listening History**: Cassandra for billions of listening events
- **Multi-Database**: PostgreSQL (metadata), Cassandra (history), Redis (cache)

**Scale**: 500M users, 100M DAU, 100M+ tracks, 13.4 PB storage, 3 Tbps bandwidth

**Quick Example**:
```bash
# Upload track
curl -X POST http://localhost:8098/api/v1/tracks/upload \
  -F "metadata={\"title\":\"Song\",\"artistId\":\"artist-123\"}" \
  -F "audioFile=@song.mp3"

# Stream track
curl -X POST http://localhost:8098/api/v1/stream \
  -d '{"trackId":"track-123","userId":"user-456","audioQuality":"HIGH"}'
```

---

### 19. Probability Management System - Prediction Market Platform
**Location**: `org.sudhir512kj.probability` package

A production-ready prediction market platform similar to Polymarket, Kalshi, and PredictIt:
- Binary (YES/NO) and categorical markets
- Order book trading with price-time priority matching
- Automated Market Maker (AMM) using LMSR algorithm
- Real-time position tracking and P&L calculation
- Oracle-based settlement with automatic payouts
- High availability (99.99% uptime)

**Documentation**: [docs/probability/](docs/probability/)

**Key Features**:
- **LMSR Algorithm**: Logarithmic Market Scoring Rule for dynamic pricing
- **Order Book Trading**: Limit and market orders with instant matching
- **In-Memory Matching**: Redis-based order book for <10ms execution
- **Position Management**: Real-time unrealized/realized P&L tracking
- **Oracle Integration**: Chainlink, UMA, manual resolution
- **Strong Consistency**: Pessimistic locking for financial operations
- **Event-Driven**: Kafka for async processing and audit logs
- **WebSocket Updates**: Real-time price and order status updates

**Scale**: 10M users, 1M DAU, 1B orders/day, 11.5K orders/sec, <100ms latency

**Quick Example**:
```bash
# Create market
curl -X POST http://localhost:8099/api/v1/markets \
  -H "X-User-Id: user-123" \
  -d '{"question":"Will Bitcoin reach $100K by 2025?","marketType":"BINARY","outcomes":["YES","NO"],"endDate":"2025-12-31T23:59:59","liquidityParameter":100.0}'

# Place order
curl -X POST http://localhost:8099/api/v1/orders \
  -H "X-User-Id: user-123" \
  -d '{"marketId":"market-uuid","outcomeId":"outcome-uuid","orderType":"LIMIT","side":"BUY","price":0.65,"quantity":100}'
```

---

