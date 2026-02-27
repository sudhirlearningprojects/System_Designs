# Netflix System Design

## Table of Contents
1. [Overview](#overview)
2. [Requirements](#requirements)
3. [High-Level Architecture](#high-level-architecture)
4. [Core Components](#core-components)
5. [Data Storage](#data-storage)
6. [Content Delivery](#content-delivery)
7. [Microservices Architecture](#microservices-architecture)
8. [Scalability & Performance](#scalability--performance)
9. [Reliability & Fault Tolerance](#reliability--fault-tolerance)
10. [Security](#security)
11. [Monitoring & Observability](#monitoring--observability)

---

## Overview

Netflix is a global streaming service serving 200M+ subscribers across 190+ countries, streaming billions of hours of content monthly. The system handles:
- 15,000+ requests per second at peak
- Petabytes of video content
- Personalized recommendations for each user
- 99.99% availability SLA

**Key Design Principles (from Netflix Engineering):**
- Microservices architecture
- Cloud-native (AWS)
- Chaos Engineering
- Freedom & Responsibility culture
- Eventual consistency over strong consistency

---

## Requirements

### Functional Requirements
1. User registration and authentication
2. Browse content catalog (movies, TV shows)
3. Search functionality
4. Video streaming with adaptive bitrate
5. Personalized recommendations
6. User profiles and watch history
7. Resume playback across devices
8. Subtitle and audio track selection
9. Download for offline viewing
10. Content rating and reviews

### Non-Functional Requirements
1. **Availability**: 99.99% uptime
2. **Scalability**: Handle millions of concurrent users
3. **Performance**: 
   - Video start time < 1 second
   - API response time < 100ms
4. **Consistency**: Eventual consistency acceptable
5. **Reliability**: No single point of failure
6. **Global**: Low latency worldwide
7. **Security**: DRM, encryption, secure authentication

### Scale Estimates
- 200M+ active subscribers
- 15K requests/second (peak: 50K+)
- 3 hours average watch time per day per user
- 10 PB+ of video content
- 1000+ microservices
- 100K+ EC2 instances

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT DEVICES                           │
│  (Web, Mobile, Smart TV, Gaming Consoles, Set-top Boxes)        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AWS CLOUD FRONT CDN                         │
│                    (Edge Locations - 200+)                       │
└────────────┬───────────────────────────────────┬────────────────┘
             │                                   │
             │ API Requests                      │ Video Content
             ▼                                   ▼
┌────────────────────────────┐    ┌─────────────────────────────┐
│   AWS ELASTIC LOAD         │    │    OPEN CONNECT CDN         │
│   BALANCER (ELB)           │    │  (Netflix's Own CDN)        │
└────────────┬───────────────┘    └─────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (ZUUL)                            │
│              (Routing, Authentication, Rate Limiting)            │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   MICROSERVICES LAYER                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │  User    │ │ Content  │ │ Search   │ │Recommend │          │
│  │ Service  │ │ Service  │ │ Service  │ │ Service  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ Playback │ │ Billing  │ │Analytics │ │  Auth    │          │
│  │ Service  │ │ Service  │ │ Service  │ │ Service  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │Cassandra │ │   MySQL  │ │   S3     │ │  Redis   │          │
│  │  (NoSQL) │ │   (RDS)  │ │(Storage) │ │ (Cache)  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                        │
│  │   EVCache│ │Elasticsearch│ │  Kafka  │                       │
│  │  (Cache) │ │  (Search) │ │(Streaming)│                      │
│  └──────────┘ └──────────┘ └──────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. API Gateway (Zuul)
**Purpose**: Single entry point for all client requests

**Features**:
- Dynamic routing
- Request filtering
- Authentication/Authorization
- Rate limiting
- Circuit breaking
- Request/Response transformation

**Technology**: Netflix Zuul (custom-built)

```
Client Request → Zuul Gateway → Pre-filters → Routing → Post-filters → Response
```

### 2. Service Discovery (Eureka)
**Purpose**: Dynamic service registration and discovery

**How it works**:
- Services register themselves on startup
- Clients query Eureka to find service instances
- Health checks and automatic deregistration
- Load balancing via Ribbon

### 3. Load Balancing (Ribbon)
**Purpose**: Client-side load balancing

**Features**:
- Multiple load balancing algorithms
- Retry logic
- Zone-aware routing
- Integration with Eureka

### 4. Circuit Breaker (Hystrix)
**Purpose**: Fault tolerance and latency tolerance

**Features**:
- Fail fast mechanism
- Fallback methods
- Request caching
- Real-time monitoring
- Bulkhead pattern

### 5. Configuration Management (Archaius)
**Purpose**: Dynamic configuration management

**Features**:
- Runtime configuration changes
- No service restart required
- Cascading configuration
- Type-safe properties

---

## Core Microservices

### 1. User Service
**Responsibilities**:
- User registration and profile management
- Authentication and authorization
- User preferences
- Watch history
- Multiple profiles per account

**Database**: Cassandra (user data), Redis (sessions)

### 2. Content Service
**Responsibilities**:
- Content metadata management
- Content catalog
- Content categorization
- Content lifecycle management

**Database**: Cassandra (metadata), Elasticsearch (search index)

### 3. Recommendation Service
**Responsibilities**:
- Personalized content recommendations
- Collaborative filtering
- Content-based filtering
- A/B testing for algorithms

**Technology**: 
- Apache Spark for batch processing
- TensorFlow for ML models
- Cassandra for storing recommendations

**Algorithm Approach**:
```
1. Collaborative Filtering (user-user, item-item)
2. Content-Based Filtering (genre, actors, directors)
3. Deep Learning (Neural Networks)
4. Contextual Bandits (real-time learning)
5. Ranking algorithms
```

### 4. Search Service
**Responsibilities**:
- Full-text search
- Autocomplete
- Fuzzy matching
- Personalized search results

**Technology**: Elasticsearch

### 5. Playback Service
**Responsibilities**:
- Video streaming orchestration
- Adaptive bitrate selection
- DRM license management
- Playback state management

**Database**: Cassandra (playback state), Redis (active sessions)

### 6. Encoding Service
**Responsibilities**:
- Video transcoding
- Multiple bitrate generation
- Audio track encoding
- Subtitle processing

**Technology**: AWS EC2 (encoding workers), S3 (storage)

### 7. Billing Service
**Responsibilities**:
- Subscription management
- Payment processing
- Invoice generation
- Retry logic for failed payments

**Database**: MySQL (transactional data)

### 8. Analytics Service
**Responsibilities**:
- User behavior tracking
- A/B testing
- Performance metrics
- Business intelligence

**Technology**: Apache Kafka, Apache Spark, S3

---

## Data Storage

### 1. Cassandra (Primary Database)
**Use Cases**:
- User profiles and preferences
- Watch history
- Content metadata
- Viewing activity
- Recommendations

**Why Cassandra**:
- High write throughput
- Linear scalability
- Multi-region replication
- Tunable consistency
- No single point of failure

**Data Model Example**:
```
Table: user_viewing_history
- user_id (partition key)
- timestamp (clustering key)
- content_id
- watch_duration
- device_type
```

### 2. MySQL (Relational Database)
**Use Cases**:
- Billing and payments
- Subscription data
- Transactional data requiring ACID

**Deployment**: Amazon RDS with Multi-AZ

### 3. EVCache (Distributed Cache)
**Use Cases**:
- Session data
- Frequently accessed metadata
- API response caching

**Technology**: Memcached-based, built by Netflix

**Architecture**:
- Multi-zone replication
- Automatic failover
- Warm-up on deployment

### 4. Amazon S3
**Use Cases**:
- Video files (encoded versions)
- Images and thumbnails
- Logs and analytics data
- Backup and archival

**Storage Classes**:
- S3 Standard (frequently accessed)
- S3 Infrequent Access (older content)
- Glacier (archives)

### 5. Elasticsearch
**Use Cases**:
- Content search
- Log aggregation
- Real-time analytics

### 6. Apache Kafka
**Use Cases**:
- Event streaming
- Real-time data pipeline
- Microservice communication
- Analytics data collection

**Topics**:
- user-activity-events
- playback-events
- recommendation-events
- billing-events

---

## Content Delivery

### Open Connect CDN

Netflix built its own CDN called **Open Connect** to deliver video content efficiently.

**Architecture**:

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 1. Request video
       ▼
┌─────────────────┐
│  Netflix API    │
│   (AWS Cloud)   │
└──────┬──────────┘
       │ 2. Return manifest + CDN URLs
       ▼
┌─────────────────┐
│ Open Connect    │
│  Edge Server    │ ← 3. Stream video
│  (ISP Location) │
└─────────────────┘
```

**Key Features**:
1. **Edge Servers**: Placed inside ISP networks
2. **Proactive Caching**: Popular content pre-cached during off-peak hours
3. **Adaptive Bitrate Streaming**: Multiple quality versions
4. **Efficient Routing**: Directs users to nearest/best server

**Video Encoding**:
- Multiple resolutions: 240p, 360p, 480p, 720p, 1080p, 4K
- Multiple bitrates per resolution
- Multiple codecs: H.264, H.265 (HEVC), VP9, AV1
- Audio: AAC, Dolby Digital, Dolby Atmos

**Adaptive Bitrate Streaming (ABR)**:
```
1. Client measures network bandwidth
2. Requests appropriate quality segment
3. Dynamically switches quality based on conditions
4. Smooth playback without buffering
```

**Protocols**:
- MPEG-DASH (Dynamic Adaptive Streaming over HTTP)
- HLS (HTTP Live Streaming) for Apple devices

---

## Microservices Architecture

### Communication Patterns

#### 1. Synchronous Communication (REST/gRPC)
```
User Service → (HTTP/REST) → Content Service
```

**Use Cases**: Real-time requests requiring immediate response

#### 2. Asynchronous Communication (Kafka)
```
Playback Service → (Kafka Event) → Analytics Service
```

**Use Cases**: Event-driven, fire-and-forget operations

### Service Mesh
Netflix uses **inter-service communication** patterns:
- Service-to-service authentication
- Automatic retries
- Timeouts
- Circuit breaking

### API Versioning
- URL versioning: `/api/v1/content`
- Header versioning: `Accept: application/vnd.netflix.v2+json`

### Data Consistency

**Eventual Consistency Model**:
- Most operations use eventual consistency
- Acceptable for user preferences, recommendations
- Conflict resolution strategies

**Strong Consistency**:
- Billing and payments
- Subscription state changes

---

## Scalability & Performance

### Horizontal Scaling
- Auto-scaling groups in AWS
- Scale based on CPU, memory, request rate
- Predictive scaling for known patterns

### Caching Strategy

**Multi-Level Caching**:
```
Client Cache → CDN Cache → API Gateway Cache → Service Cache → Database
```

**Cache Invalidation**:
- TTL-based expiration
- Event-driven invalidation
- Cache warming strategies

### Database Sharding
**Cassandra Sharding**:
- Partition by user_id
- Consistent hashing
- Automatic rebalancing

### Asynchronous Processing
- Background jobs for encoding
- Batch processing for recommendations
- Queue-based processing (SQS)

### Performance Optimizations
1. **GraphQL/Falcor**: Efficient data fetching
2. **Image Optimization**: WebP, responsive images
3. **Code Splitting**: Lazy loading
4. **Prefetching**: Predictive content loading

---

## Reliability & Fault Tolerance

### Chaos Engineering

Netflix pioneered **Chaos Engineering** with tools like:

#### 1. Chaos Monkey
- Randomly terminates EC2 instances
- Tests service resilience
- Runs in production

#### 2. Chaos Kong
- Simulates entire AWS region failure
- Tests multi-region failover

#### 3. Latency Monkey
- Introduces artificial delays
- Tests timeout handling

### Multi-Region Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  US-EAST-1   │     │  US-WEST-2   │     │  EU-WEST-1   │
│   (Primary)  │◄───►│  (Secondary) │◄───►│  (Secondary) │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Features**:
- Active-active deployment
- Cross-region replication (Cassandra)
- Regional failover
- Data sovereignty compliance

### Circuit Breaker Pattern

```
[Closed] → Request succeeds → [Closed]
[Closed] → Failures exceed threshold → [Open]
[Open] → Fail fast (no requests sent) → [Open]
[Open] → After timeout → [Half-Open]
[Half-Open] → Test request succeeds → [Closed]
[Half-Open] → Test request fails → [Open]
```

### Bulkhead Pattern
- Isolate thread pools per dependency
- Prevent cascading failures
- Resource isolation

### Retry Logic
- Exponential backoff
- Jitter to prevent thundering herd
- Maximum retry attempts

### Graceful Degradation
- Fallback responses
- Cached data when service unavailable
- Reduced functionality over no functionality

---

## Security

### Authentication & Authorization

**OAuth 2.0 Flow**:
```
1. User enters credentials
2. Auth Service validates
3. Issues JWT token
4. Client includes token in requests
5. Services validate token
```

**Token Management**:
- Short-lived access tokens (1 hour)
- Long-lived refresh tokens (30 days)
- Token rotation

### DRM (Digital Rights Management)

**Technologies**:
- Widevine (Google)
- FairPlay (Apple)
- PlayReady (Microsoft)

**Process**:
```
1. Client requests video
2. Receives encrypted content + license URL
3. Requests license from DRM service
4. DRM validates user rights
5. Issues decryption keys
6. Client decrypts and plays
```

### Data Encryption

**In Transit**:
- TLS 1.3 for all communications
- Certificate pinning on mobile apps

**At Rest**:
- S3 server-side encryption (SSE-KMS)
- Database encryption (Cassandra, MySQL)

### Network Security

**VPC Architecture**:
```
┌─────────────────────────────────────────┐
│              VPC                         │
│  ┌────────────────┐  ┌────────────────┐│
│  │ Public Subnet  │  │ Private Subnet ││
│  │  (ELB, NAT)    │  │ (Microservices)││
│  └────────────────┘  └────────────────┘│
│  ┌────────────────────────────────────┐ │
│  │      Data Subnet (Databases)       │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Security Groups**:
- Least privilege access
- Service-to-service authentication
- IP whitelisting

### API Security
- Rate limiting (per user, per IP)
- Request validation
- SQL injection prevention
- XSS protection

---

## Monitoring & Observability

### Metrics Collection

**Atlas** (Netflix's metrics platform):
- Time-series metrics
- Dimensional data model
- Real-time aggregation
- Custom dashboards

**Key Metrics**:
- Request rate (RPS)
- Error rate
- Latency (p50, p95, p99)
- CPU/Memory utilization
- Cache hit ratio
- Video start time
- Buffering ratio

### Distributed Tracing

**Technology**: Zipkin (Netflix contributed)

**Trace Flow**:
```
Client Request → Zuul → User Service → Content Service → Database
     [Trace ID: abc123 propagated through all services]
```

### Logging

**Centralized Logging**:
- All services log to Kafka
- Elasticsearch for indexing
- Kibana for visualization

**Log Levels**:
- ERROR: Critical issues
- WARN: Potential problems
- INFO: Important events
- DEBUG: Detailed information

### Alerting

**Alert Types**:
1. **Threshold Alerts**: Error rate > 5%
2. **Anomaly Detection**: ML-based unusual patterns
3. **Composite Alerts**: Multiple conditions

**Notification Channels**:
- PagerDuty for critical alerts
- Slack for warnings
- Email for informational

### A/B Testing

**Framework**: Netflix's internal A/B testing platform

**Process**:
```
1. Define hypothesis
2. Create test variants (A, B)
3. Randomly assign users
4. Collect metrics
5. Statistical analysis
6. Roll out winner
```

**Metrics Tracked**:
- Engagement (watch time)
- Retention rate
- Conversion rate
- User satisfaction

---

## System Design Decisions & Trade-offs

### 1. Microservices vs Monolith
**Decision**: Microservices
**Rationale**: 
- Independent scaling
- Team autonomy
- Technology flexibility
**Trade-off**: Increased complexity, distributed system challenges

### 2. Eventual Consistency
**Decision**: Eventual consistency for most operations
**Rationale**: 
- Better availability
- Lower latency
- Horizontal scalability
**Trade-off**: Temporary inconsistencies, complex conflict resolution

### 3. Own CDN (Open Connect)
**Decision**: Build custom CDN
**Rationale**: 
- Cost savings at scale
- Better control
- Optimized for video
**Trade-off**: High initial investment, maintenance overhead

### 4. Cassandra over MySQL
**Decision**: Cassandra for most data
**Rationale**: 
- Write-heavy workload
- Linear scalability
- Multi-region replication
**Trade-off**: No joins, eventual consistency, complex data modeling

### 5. Client-Side Load Balancing
**Decision**: Ribbon (client-side)
**Rationale**: 
- Eliminates load balancer bottleneck
- Zone-aware routing
- Better failure handling
**Trade-off**: Client complexity, version management

---

## Key Takeaways from Netflix Engineering

1. **Embrace Failure**: Design for failure, test in production
2. **Automate Everything**: Deployment, scaling, recovery
3. **Measure Everything**: Data-driven decisions
4. **Optimize for Speed**: Fast iteration, continuous deployment
5. **Decentralize**: Empower teams, avoid bottlenecks
6. **Cloud-Native**: Leverage cloud capabilities fully
7. **User Experience First**: Performance and reliability matter most

---

## References

1. Netflix Tech Blog: https://netflixtechblog.com/
2. Netflix Open Source: https://netflix.github.io/
3. "Chaos Engineering" by Netflix
4. "Microservices at Netflix" talks
5. AWS re:Invent Netflix presentations
6. Netflix Engineering Medium publications

---

## Conclusion

Netflix's architecture is a masterclass in building scalable, reliable, distributed systems. Key innovations include:

- **Microservices at scale** (1000+ services)
- **Chaos Engineering** (testing in production)
- **Custom CDN** (Open Connect)
- **Personalization** (ML-driven recommendations)
- **Cloud-native** (fully on AWS)

The system handles massive scale while maintaining high availability and performance, serving as a blueprint for modern streaming platforms.
