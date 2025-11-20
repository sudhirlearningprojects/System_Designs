# Uber Clone - Global Ride-Hailing System Design

## Table of Contents
1. [Problem Statement](#problem-statement)
2. [Functional Requirements](#functional-requirements)
3. [Non-Functional Requirements](#non-functional-requirements)
4. [Back-of-Envelope Calculations](#back-of-envelope-calculations)
5. [High-Level Design](#high-level-design)
6. [API Design](#api-design)
7. [Database Design](#database-design)
8. [Deep Dive: Geo-location & Matching Engine](#deep-dive-geo-location--matching-engine)
9. [Low-Level Design](#low-level-design)
10. [Scalability & Performance](#scalability--performance)
11. [Fault Tolerance & Reliability](#fault-tolerance--reliability)

---

## Problem Statement

Design a global, real-time ride-hailing platform like Uber that connects riders with drivers, handles millions of concurrent users, provides real-time location tracking, efficient matching, and reliable payment processing.

---

## Functional Requirements

### Rider Features
1. **User Management**: Registration, authentication, profile management
2. **Location Services**: GPS-based location detection, pickup/drop-off selection
3. **Vehicle Selection**: Choose ride types (UberX, XL, Black, Pool)
4. **Fare Estimation**: Dynamic pricing with upfront estimates
5. **Ride Request**: Request rides and get matched with drivers
6. **Real-time Tracking**: Track driver location and ETA
7. **Payments**: Multiple payment methods, automated billing
8. **Ratings**: Rate drivers and provide feedback
9. **History**: View past and current rides

### Driver Features
1. **Driver Onboarding**: Registration, document verification, background checks
2. **Availability**: Toggle online/offline status
3. **Ride Acceptance**: Receive and accept/decline ride requests
4. **Navigation**: Turn-by-turn navigation
5. **Earnings**: View trip history and earnings dashboard

### Backend Services
1. **Matching Engine**: Pair nearest available driver with rider
2. **Geo-location Service**: Process millions of GPS coordinates with low latency
3. **Pricing Engine**: Dynamic surge pricing
4. **Payment Processing**: Billing and payment gateway integration
5. **Notification Service**: Push notifications for ride updates

---

## Non-Functional Requirements

| Requirement | Target | Priority |
|-------------|--------|----------|
| **Availability** | 99.99% uptime | Critical |
| **Scalability** | 10M concurrent users | Critical |
| **Latency** | <1s for matching, <100ms for location updates | Critical |
| **Consistency** | Strong for payments, eventual for location | Critical |
| **Throughput** | 100K ride requests/sec | High |
| **Data Retention** | 7 years for compliance | Medium |

---

## Back-of-Envelope Calculations

### Scale Assumptions
- **Total Users**: 100M riders, 5M drivers
- **Daily Active Users (DAU)**: 10M riders, 1M drivers
- **Concurrent Active**: 500K riders, 100K drivers (peak)
- **Rides per Day**: 15M rides
- **Average Ride Duration**: 20 minutes
- **Location Update Frequency**: Every 4 seconds

### Traffic Estimates

**Ride Requests**:
- 15M rides/day = 173 rides/sec average
- Peak: 3x = 520 rides/sec

**Location Updates**:
- 100K active drivers × (1 update / 4 sec) = 25K updates/sec
- 500K active riders × (1 update / 10 sec) = 50K updates/sec
- **Total**: 75K location updates/sec

**Database Queries**:
- Matching queries: 520 queries/sec (peak)
- Location queries: 75K writes/sec + 520 reads/sec
- Payment transactions: 173 TPS

### Storage Estimates

**User Data**:
- 100M riders × 1KB = 100GB
- 5M drivers × 2KB = 10GB

**Ride Data**:
- 15M rides/day × 365 days × 2KB = 10.95TB/year
- 7 years retention = 76.65TB

**Location Data** (Hot data - 24 hours):
- 75K updates/sec × 86400 sec × 200 bytes = 1.3TB/day
- Archive after 24 hours

**Total Storage**: ~100TB (with 7-year retention)

### Bandwidth Estimates

**Location Updates**:
- 75K updates/sec × 200 bytes = 15MB/sec = 120 Mbps

**Ride Matching**:
- 520 requests/sec × 1KB = 520KB/sec = 4.16 Mbps

**Total Bandwidth**: ~150 Mbps (ingress + egress)

---

## High-Level Design

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway (Kong)                       │
│                    Rate Limiting, Auth, Routing                  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
        ┌───────▼──────┐  ┌─────▼─────┐  ┌──────▼──────┐
        │ Rider Service│  │Driver Svc │  │ Ride Service│
        └──────────────┘  └───────────┘  └─────────────┘
                │                │                │
                └────────────────┼────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼──────┐      ┌─────────▼────────┐      ┌───────▼──────┐
│ Geo-Location │      │ Matching Engine  │      │Payment Service│
│   Service    │      │   (Core Logic)   │      │               │
└──────────────┘      └──────────────────┘      └───────────────┘
        │                        │                        │
        │                        │                        │
┌───────▼──────┐      ┌─────────▼────────┐      ┌───────▼──────┐
│Redis Geo     │      │  PostgreSQL      │      │Stripe/PayPal │
│(Hot Locations)│     │  (Ride Data)     │      │              │
└──────────────┘      └──────────────────┘      └──────────────┘
        │
┌───────▼──────┐
│  Cassandra   │
│(Location Log)│
└──────────────┘
```

### Key Components

1. **API Gateway**: Kong/AWS API Gateway for routing, auth, rate limiting
2. **Rider Service**: User management, ride requests, history
3. **Driver Service**: Driver management, availability, earnings
4. **Ride Service**: Ride lifecycle management, status tracking
5. **Geo-Location Service**: Real-time location ingestion and querying
6. **Matching Engine**: Core algorithm to match riders with drivers
7. **Pricing Service**: Dynamic fare calculation with surge pricing
8. **Payment Service**: Payment processing with idempotency
9. **Notification Service**: Push notifications via FCM/APNS
10. **Trip Service**: Navigation, route optimization

---

## API Design

### Rider APIs

```http
POST /api/v1/riders/register
POST /api/v1/riders/login
GET  /api/v1/riders/{riderId}/profile

POST /api/v1/rides/estimate
POST /api/v1/rides/request
GET  /api/v1/rides/{rideId}
PUT  /api/v1/rides/{rideId}/cancel
GET  /api/v1/rides/{rideId}/location
POST /api/v1/rides/{rideId}/rating

GET  /api/v1/riders/{riderId}/history
```

### Driver APIs

```http
POST /api/v1/drivers/register
POST /api/v1/drivers/login
PUT  /api/v1/drivers/{driverId}/status
GET  /api/v1/drivers/{driverId}/earnings

GET  /api/v1/drivers/{driverId}/ride-requests
POST /api/v1/rides/{rideId}/accept
POST /api/v1/rides/{rideId}/decline
PUT  /api/v1/rides/{rideId}/start
PUT  /api/v1/rides/{rideId}/complete

POST /api/v1/drivers/{driverId}/location
```

### Internal APIs

```http
POST /internal/matching/find-drivers
POST /internal/pricing/calculate
POST /internal/geo/nearby-drivers
```

---

## Database Design

### PostgreSQL Schema (Transactional Data)

```sql
-- Users Table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- RIDER, DRIVER
    rating DECIMAL(3,2),
    total_rides INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_type ON users(user_type);

-- Drivers Table
CREATE TABLE drivers (
    driver_id UUID PRIMARY KEY REFERENCES users(user_id),
    license_number VARCHAR(50) UNIQUE NOT NULL,
    vehicle_id UUID REFERENCES vehicles(vehicle_id),
    status VARCHAR(20) DEFAULT 'OFFLINE', -- ONLINE, OFFLINE, ON_TRIP
    current_location GEOGRAPHY(POINT),
    is_verified BOOLEAN DEFAULT FALSE,
    total_earnings DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_drivers_status ON drivers(status);
CREATE INDEX idx_drivers_location ON drivers USING GIST(current_location);

-- Vehicles Table
CREATE TABLE vehicles (
    vehicle_id UUID PRIMARY KEY,
    driver_id UUID REFERENCES drivers(driver_id),
    vehicle_type VARCHAR(20) NOT NULL, -- UBERX, XL, BLACK, POOL
    make VARCHAR(50),
    model VARCHAR(50),
    year INTEGER,
    license_plate VARCHAR(20) UNIQUE,
    color VARCHAR(30),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Rides Table
CREATE TABLE rides (
    ride_id UUID PRIMARY KEY,
    rider_id UUID REFERENCES users(user_id),
    driver_id UUID REFERENCES drivers(driver_id),
    vehicle_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL, -- REQUESTED, ACCEPTED, STARTED, COMPLETED, CANCELLED
    
    pickup_location GEOGRAPHY(POINT) NOT NULL,
    pickup_address TEXT,
    dropoff_location GEOGRAPHY(POINT) NOT NULL,
    dropoff_address TEXT,
    
    estimated_fare DECIMAL(10,2),
    actual_fare DECIMAL(10,2),
    distance_km DECIMAL(10,2),
    duration_minutes INTEGER,
    
    requested_at TIMESTAMP DEFAULT NOW(),
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    payment_id UUID,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    feedback TEXT
);
CREATE INDEX idx_rides_rider ON rides(rider_id);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_requested_at ON rides(requested_at);

-- Payments Table
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    ride_id UUID REFERENCES rides(ride_id),
    user_id UUID REFERENCES users(user_id),
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50), -- CARD, WALLET, CASH
    payment_status VARCHAR(20), -- PENDING, COMPLETED, FAILED, REFUNDED
    transaction_id VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_payments_ride ON payments(ride_id);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);

-- Pricing Rules Table
CREATE TABLE pricing_rules (
    rule_id UUID PRIMARY KEY,
    city_id UUID NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    base_fare DECIMAL(10,2) NOT NULL,
    per_km_rate DECIMAL(10,2) NOT NULL,
    per_minute_rate DECIMAL(10,2) NOT NULL,
    minimum_fare DECIMAL(10,2) NOT NULL,
    surge_multiplier DECIMAL(3,2) DEFAULT 1.0,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP
);
```

### Redis Data Structures (Real-time Data)

```
# Driver Location (Geospatial Index)
GEOADD drivers:online {longitude} {latitude} {driver_id}
GEORADIUS drivers:online {longitude} {latitude} 5 km WITHDIST

# Driver Status
HSET driver:{driver_id} status "ONLINE" location "{lat},{lng}" updated_at {timestamp}

# Active Rides (for quick lookup)
HSET ride:{ride_id} status "STARTED" driver_id {driver_id} rider_id {rider_id}

# Driver Availability (Sorted Set by last update time)
ZADD drivers:available {timestamp} {driver_id}

# Ride Request Queue (per city/region)
LPUSH ride_requests:{city_id} {ride_request_json}

# Location History (Time Series - last 1 hour)
ZADD location:{driver_id} {timestamp} "{lat},{lng}"
```

### Cassandra Schema (Location History - Time Series)

```cql
CREATE TABLE location_history (
    driver_id UUID,
    timestamp TIMESTAMP,
    latitude DOUBLE,
    longitude DOUBLE,
    speed DOUBLE,
    heading DOUBLE,
    PRIMARY KEY (driver_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

CREATE TABLE ride_tracking (
    ride_id UUID,
    timestamp TIMESTAMP,
    driver_location TEXT,
    rider_location TEXT,
    distance_remaining DOUBLE,
    eta_minutes INT,
    PRIMARY KEY (ride_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);
```

---

## Deep Dive: Geo-location & Matching Engine

### Problem Statement
**How to efficiently store 100K active driver locations and find the top 10 nearest available drivers within 5km radius with <100ms latency?**

### Solution: H3 Hexagonal Hierarchical Spatial Index

#### 1. Why H3 Over Traditional Geohash?

Uber uses **H3** (open-sourced by Uber in 2018) instead of traditional Geohash:

**H3 Advantages**:
- **Uniform cell sizes**: Hexagons have consistent neighbor distances (no edge distortion)
- **Better coverage**: 6 neighbors vs 8 in square grids (more efficient)
- **Hierarchical**: 16 resolutions from 4M km² to 0.9 m²
- **Fast neighbor lookup**: O(1) for adjacent cells
- **10x faster**: Proximity queries compared to Geohash

**H3 Resolutions Used by Uber**:
```
Resolution 5: ~252 km² - City-level sharding
Resolution 7: ~5.16 km² - Neighborhood matching  
Resolution 9: ~0.105 km² - Fine-grained driver search (100m radius)
Resolution 11: ~0.0016 km² - Precise location tracking
```

**Benefits**:
- Reduces search space from 100K drivers to ~50-100 drivers per cell
- Enables horizontal sharding by geography
- Supports hierarchical queries (search nearby cells if not enough drivers)

#### 2. H3 + Redis Implementation

Store drivers in Redis Sets indexed by H3 cell:

```redis
# Add driver to H3 cell (resolution 9)
SADD drivers:h3:r9:{h3_cell_id} {driver_id}

# Find nearby drivers (center + 6 neighbors = 7 cells)
SUNION drivers:h3:r9:{cell1} drivers:h3:r9:{cell2} ... drivers:h3:r9:{cell7}
```

**Performance**:
- O(1) for cell lookup
- O(N) for union of 7 cells where N = drivers per cell (~50)
- <5ms for 20 nearest drivers
- Memory: ~8 bytes per H3 cell ID vs 16 bytes for lat/lng

#### 3. DISCO Matching Algorithm (Uber's Production System)

**DISCO** (Dispatch Optimization) is Uber's core matching engine:

```
Algorithm: Find Best Driver (DISCO-inspired)
Input: rider_location, vehicle_type, max_radius_km
Output: matched_driver_id

1. Spatial Filtering (H3 Hierarchical Search):
   a. Get rider's H3 cell (resolution 9 = ~100m radius)
   b. Query center cell + 6 neighbors (7 cells total)
   c. If < 10 drivers, expand to resolution 7 (5km radius)
   d. If still < 10, expand to 10km radius
   
2. Temporal Filtering:
   - status = ONLINE
   - vehicle_type matches
   - not in cooldown period (recently declined)
   - last location update < 30 seconds (freshness check)
   
3. Multi-Factor Scoring (ML-optimized weights):
   score = 0.5 * (1/distance) + 0.2 * acceptance_rate + 
           0.2 * driver_rating + 0.1 * (1/eta)
   
   Factors:
   - Distance: Closer is better (most important)
   - Acceptance rate: Reliability (80-100%)
   - Driver rating: Quality (1-5 stars)
   - ETA: Faster arrival (ML-predicted)
   
4. Sort by score (descending), select top 3 drivers

5. Sequential notification (30 sec timeout per driver):
   - Send to driver #1, wait 30 seconds
   - If declined/timeout, add to cooldown (5 min)
   - Try driver #2, then #3
   
6. If all decline:
   - Expand radius to 10km
   - Retry with new driver pool
   - If still no match, notify rider "No drivers available"

7. Batching (for efficiency during peak):
   - Batch 100 ride requests
   - Run matching every 2 seconds
   - Optimize global assignment (Hungarian algorithm)
```

**Performance Metrics**:
- **Match rate**: 95% of rides matched within 30 seconds
- **Average ETA**: 4 minutes globally
- **Throughput**: 100K matches/second during peak
- **Latency**: p99 < 1 second

#### 4. Location Update Flow (Production Architecture)

```
Driver App → WebSocket → Location Service → H3 + Redis + Kafka → Cassandra

1. Driver sends location every 4 seconds via WebSocket
   - Persistent connection (reduces overhead)
   - Binary protocol (smaller payload)
   - Automatic reconnection on network loss
   
2. Location Service processes update:
   a. Calculate H3 cell IDs (resolution 9, 7, 5)
   b. Update Redis:
      - SADD drivers:h3:r9:{cell} {driver_id}
      - HSET driver:{id} lat {lat} lng {lng} updated_at {ts}
   c. Remove from old H3 cells (if moved)
   
3. Publish to Kafka (async):
   - Topic: location.updates
   - Partition: by driver_id (for ordering)
   - Throughput: 75K updates/sec
   
4. Kafka consumers:
   - Cassandra: Archive for analytics (90-day retention)
   - Flink: Real-time ETA calculation
   - Pinot: Aggregate metrics (driver heatmaps)
   - Active rides: Update rider's app with driver location
```

**Optimizations**:
- **Batching**: Process 100 updates together (reduces Redis round trips)
- **Compression**: Gzip location payloads (50% size reduction)
- **Sampling**: Archive 1 in 10 updates to Cassandra (reduce storage)
- **TTL**: Expire stale locations after 60 seconds

#### 5. Production Optimizations

**Multi-Layer Caching**:
```
L1: Caffeine (Application) - 1 min TTL, 10K entries
    - Driver profiles
    - Pricing rules
    
L2: Redis (Distributed) - 5 min TTL, 1M entries  
    - Active driver locations
    - Ride details
    - User profiles
    
L3: PostgreSQL (Database) - Persistent
    - Historical data
    - Audit logs
```

**Connection Management**:
- **WebSocket**: Persistent connections for drivers (100K concurrent)
- **HTTP/2**: Multiplexing for REST APIs
- **gRPC**: Internal service communication (5-10x faster than REST)
- **Connection pooling**: HikariCP with 50 max connections

**Batch Processing**:
- Location updates: 100 updates/batch
- Notification sending: 50 notifications/batch
- Database inserts: 500 records/batch

**Database Scaling**:
- **PostgreSQL**: Master-slave replication (1 master, 3 read replicas)
- **Redis**: Cluster mode with 6 nodes (3 masters, 3 replicas)
- **Cassandra**: 9-node cluster with RF=3

**Sharding Strategy**:
- **Geographic sharding**: By H3 cell (city-level)
- **User sharding**: By user_id hash (consistent hashing)
- **Time-based partitioning**: Rides table by month

---

## Low-Level Design

### Class Diagram

```java
// Domain Models
class User {
    UUID userId;
    String phoneNumber;
    String email;
    String name;
    UserType userType;
    BigDecimal rating;
}

class Driver extends User {
    String licenseNumber;
    Vehicle vehicle;
    DriverStatus status;
    Location currentLocation;
    boolean isVerified;
}

class Rider extends User {
    List<PaymentMethod> paymentMethods;
}

class Vehicle {
    UUID vehicleId;
    VehicleType type;
    String make, model;
    int year;
    String licensePlate;
}

class Ride {
    UUID rideId;
    UUID riderId;
    UUID driverId;
    RideStatus status;
    Location pickup, dropoff;
    BigDecimal estimatedFare, actualFare;
    LocalDateTime requestedAt, completedAt;
}

class Location {
    double latitude;
    double longitude;
    String address;
}

// Services
interface MatchingService {
    Driver findNearestDriver(Location pickup, VehicleType type);
    void notifyDriver(UUID driverId, RideRequest request);
}

interface GeoLocationService {
    void updateDriverLocation(UUID driverId, Location location);
    List<Driver> findNearbyDrivers(Location location, double radiusKm, int limit);
}

interface PricingService {
    BigDecimal calculateFare(Location pickup, Location dropoff, VehicleType type);
    BigDecimal getSurgeMultiplier(String cityId);
}

interface RideService {
    Ride requestRide(RideRequest request);
    Ride acceptRide(UUID rideId, UUID driverId);
    Ride startRide(UUID rideId);
    Ride completeRide(UUID rideId);
}
```

### Sequence Diagrams

#### Ride Request Flow

```
Rider App → API Gateway → Ride Service → Matching Engine → Driver App

1. Rider requests ride
2. Ride Service creates ride record (status: REQUESTED)
3. Matching Engine finds nearest driver
4. Send push notification to driver
5. Driver accepts (30 sec timeout)
6. Update ride status to ACCEPTED
7. Notify rider with driver details
8. Start real-time location tracking
```

#### Real-time Tracking Flow

```
Driver App → WebSocket Gateway → Location Service → Redis → Rider App

1. Driver sends location every 4 seconds
2. Location Service updates Redis GEOADD
3. If driver on active ride:
   - Calculate ETA
   - Publish to Kafka topic: ride.location.updated
4. Rider App subscribes to WebSocket for updates
5. Receive driver location + ETA every 4 seconds
```

---

## Scalability & Performance

### Horizontal Scaling

**Stateless Services** (Auto-scale based on CPU/Memory):
- Rider Service: 10-50 instances
- Driver Service: 10-50 instances
- Ride Service: 20-100 instances
- API Gateway: 5-20 instances

**Stateful Services**:
- Location Service: 50-200 instances (WebSocket connections)
- Matching Engine: 20-50 instances (CPU intensive)

### Database Scaling

**PostgreSQL**:
- Master-Slave replication (1 master, 3 read replicas)
- Partition rides table by month (time-based partitioning)
- Archive old data to S3 after 1 year

**Redis**:
- Redis Cluster with 6 nodes (3 masters, 3 replicas)
- Shard by geohash prefix
- Memory: 64GB per node = 384GB total

**Cassandra**:
- 9-node cluster (RF=3)
- Partition by driver_id for location history
- TTL: 90 days for location data

### Caching Strategy

**L1 Cache** (Application Level - Caffeine):
- Pricing rules: 5 min TTL
- Driver profiles: 1 min TTL

**L2 Cache** (Redis):
- Active driver locations: 30 sec TTL
- Ride details: 5 min TTL
- User profiles: 1 hour TTL

### Load Balancing

- **API Gateway**: AWS ALB with path-based routing
- **WebSocket**: Sticky sessions with consistent hashing
- **Database**: PgBouncer for connection pooling

---

## Fault Tolerance & Reliability

### High Availability

**Multi-Region Deployment**:
- Active-Active in US-East, US-West, EU-West, AP-South
- Route users to nearest region (latency-based routing)
- Cross-region replication for critical data

**Circuit Breaker Pattern**:
- Protect payment gateway calls
- Fallback to alternative payment processor
- Timeout: 5 seconds, Failure threshold: 50%

**Retry Mechanism**:
- Exponential backoff for transient failures
- Max 3 retries for idempotent operations

### Data Consistency

**Strong Consistency** (PostgreSQL):
- Payments: ACID transactions
- Ride status: Pessimistic locking

**Eventual Consistency** (Redis/Cassandra):
- Driver locations: Last-write-wins
- Location history: Append-only

### Disaster Recovery

**Backup Strategy**:
- PostgreSQL: Daily full backup + WAL archiving
- Redis: RDB snapshots every 5 minutes
- Cassandra: Daily snapshots to S3

**RTO/RPO**:
- RTO: 15 minutes (automated failover)
- RPO: 5 minutes (data loss tolerance)

---

## Security

### Authentication & Authorization
- JWT tokens with 24-hour expiry
- OAuth 2.0 for social login
- Role-based access control (RBAC)

### Data Protection
- TLS 1.3 for all communications
- Encrypt PII at rest (AES-256)
- PCI DSS compliance for payment data

### Rate Limiting
- 100 requests/min per user
- 1000 requests/min per IP
- DDoS protection via AWS Shield

---

## Monitoring & Observability

### Metrics
- Request latency (p50, p95, p99)
- Ride matching success rate
- Driver availability rate
- Payment success rate

### Logging
- Centralized logging with ELK stack
- Distributed tracing with Jaeger
- Correlation IDs for request tracking

### Alerts
- High latency (>2s for matching)
- Low driver availability (<10%)
- Payment failures (>5%)
- Service downtime

---

## Cost Estimation (Monthly)

| Component | Specification | Cost |
|-----------|--------------|------|
| EC2 Instances | 200 × m5.xlarge | $24,000 |
| RDS PostgreSQL | db.r5.4xlarge (Multi-AZ) | $3,500 |
| ElastiCache Redis | 6 × cache.r5.2xlarge | $4,200 |
| Cassandra (EC2) | 9 × i3.2xlarge | $5,400 |
| S3 Storage | 100TB | $2,300 |
| Data Transfer | 500TB/month | $45,000 |
| CloudFront CDN | 1PB | $85,000 |
| **Total** | | **~$170,000/month** |

**Revenue**: 15M rides/day × $2 commission = $30M/day = $900M/month
**Profit Margin**: 99.98%

---

## Communication Protocols (Uber's Production Stack)

### Protocol Selection

Uber uses **three protocols** optimized for different use cases:

| Protocol | Use Case | Latency | Throughput | When to Use |
|----------|----------|---------|------------|-------------|
| **gRPC** | Internal service-to-service | 1-5ms | 100K RPS | Synchronous, low-latency |
| **WebSocket** | Client real-time updates | 50-100ms | 100K connections | Bidirectional streaming |
| **Kafka** | Event streaming | 5-10ms | 1M events/sec | Async, high-throughput |
| **REST** | Public APIs | 10-50ms | 10K RPS | External integrations |

### Why gRPC for Internal Services?

**Advantages over REST**:
- **5-10x lower latency**: Binary Protocol Buffers vs JSON
- **4x smaller payload**: 50 bytes vs 200 bytes
- **Type safety**: Compile-time validation
- **Bidirectional streaming**: Real-time data flow
- **HTTP/2**: Multiplexing, header compression

**Use Cases**:
```
Matching Engine ↔ Geo-Location Service (gRPC)
Ride Service ↔ Payment Service (gRPC)
Pricing Service ↔ Surge Calculator (gRPC)
```

### Why WebSocket for Clients?

**Advantages over HTTP Polling**:
- **Persistent connection**: No handshake overhead
- **Bidirectional**: Server push to client
- **250x less overhead**: 2 bytes vs 500 bytes per message
- **10x lower server load**: Single connection vs multiple requests

**Use Cases**:
```
Driver App → Location updates (every 4 seconds)
Rider App ← Driver location streaming (every 2 seconds)
Ride status notifications
```

### Why Kafka for Events?

**Advantages**:
- **High throughput**: 1M events/sec per broker
- **Durability**: Persistent storage with replication
- **Decoupling**: Async processing
- **Scalability**: Horizontal scaling with partitions

**Topics**:
```
uber.location.updates (100 partitions, 7-day retention)
uber.ride.events (50 partitions, 90-day retention)
uber.payment.transactions (20 partitions, 7-year retention)
```

### Complete Flow Example

**Ride Request (Total: ~110ms p99)**:
```
1. Rider App → REST API → Ride Service (50ms)
2. Ride Service → gRPC → Matching Engine (2ms)
3. Matching Engine → gRPC → Geo-Location Service (3ms)
4. Geo-Location → Redis GEORADIUS (1ms)
5. Matching Engine → gRPC → Notification Service (2ms)
6. Notification → WebSocket → Driver App (50ms)
```

**Location Update (Total: ~55ms p99)**:
```
1. Driver App → WebSocket → Location Service (50ms)
2. Location Service → Redis GEOADD (1ms)
3. Location Service → Kafka → location.updates (5ms, async)
4. Kafka Consumer → Cassandra archival (10ms, async)
```

### Performance Comparison

| Metric | REST | gRPC | Improvement |
|--------|------|------|-------------|
| Latency | 15ms | 2ms | 7.5x faster |
| Payload | 200B | 50B | 4x smaller |
| Throughput | 20K RPS | 100K RPS | 5x higher |

**See**: [Communication_Protocols.md](Communication_Protocols.md) for detailed analysis

---

## Future Enhancements

1. **Ride Pooling**: Match multiple riders going in same direction
2. **ML-based ETA**: Predict accurate arrival times using historical data
3. **Dynamic Pricing**: Real-time surge pricing based on demand/supply
4. **Driver Incentives**: Gamification and bonus programs
5. **Safety Features**: SOS button, ride sharing, driver verification
6. **Multi-modal Transport**: Integration with bikes, scooters, public transit

---

**Design Completed**: Production-grade system with gRPC (internal), WebSocket (client), and Kafka (events) handling 10M concurrent users, 100K location updates/sec, and <1s matching latency with 99.99% availability.
