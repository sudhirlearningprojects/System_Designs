# Distributed Notification System

A highly scalable, fault-tolerant distributed notification system capable of delivering messages across multiple channels (email, SMS, push, in-app) to millions of users with guaranteed delivery and respect for user preferences.

## 🎯 Key Features

### Multi-Channel Support
- **Email**: SendGrid, AWS SES integration
- **SMS**: Twilio, AWS SNS integration
- **Push**: FCM (Android), APNS (iOS)
- **In-App**: Database-backed notifications
- **WebSocket**: Real-time delivery

### Reliability & Fault Tolerance
- **Retry Mechanism**: Exponential backoff with jitter
- **Dead Letter Queue (DLQ)**: Unprocessable message handling
- **Circuit Breaker**: Provider failure protection
- **At-Least-Once Delivery**: Guaranteed message delivery
- **Idempotency**: Duplicate request prevention

### Scalability
- **Horizontal Scaling**: Auto-scaling worker pools
- **Kafka Partitioning**: 550 partitions for parallel processing
- **Database Sharding**: 64 shards by userId
- **Redis Caching**: 95% cache hit rate for preferences
- **Fan-Out**: Efficient broadcast to millions

### User Preferences
- **Channel Control**: Opt-in/opt-out per channel
- **Notification Types**: Granular control by type
- **Quiet Hours**: Time-based delivery restrictions
- **Timezone Support**: Respect user timezones

### Priority-Based Delivery
- **CRITICAL**: <100ms (OTP, security alerts)
- **HIGH**: <1s (payment confirmations)
- **MEDIUM**: <5s (order updates)
- **LOW**: Best effort (marketing)

## 📊 Scale

- **Throughput**: 10M notifications/minute (50K/sec peak)
- **Users**: 500M users
- **Daily Volume**: 1B notifications/day
- **Latency**: <100ms for critical notifications
- **Availability**: 99.99% uptime
- **Retention**: 90 days (logs), 1 year (analytics)

## 🏗️ Architecture

```
API Gateway → Notification Service → Kafka → Channel Workers → Providers
                     ↓
              User Preferences (Redis Cache)
                     ↓
              Database (PostgreSQL)
```

### Core Components

1. **Notification Service**: Request validation, preference checking, routing
2. **Kafka Message Bus**: Priority-based topics for async processing
3. **Channel Workers**: Email, SMS, Push, In-App workers with circuit breakers
4. **Preference Service**: User preference management with caching
5. **Retry Service**: Exponential backoff retry mechanism
6. **DLQ Processor**: Failed message handling and alerting
7. **Metrics Service**: Observability and monitoring

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+

### 1. Start Infrastructure
```bash
docker-compose up -d postgres redis kafka
```

### 2. Configure Environment
```bash
export DB_URL=jdbc:postgresql://localhost:5432/notifications
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Provider API Keys
export SENDGRID_API_KEY=your_key
export TWILIO_ACCOUNT_SID=your_sid
export TWILIO_AUTH_TOKEN=your_token
export FCM_SERVER_KEY=your_key
```

### 3. Build and Run
```bash
mvn clean install
./run-systems.sh notification  # Port 8089
```

### 4. Test the API
```bash
# Send a notification
curl -X POST http://localhost:8089/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "type": "TRANSACTIONAL",
    "priority": "HIGH",
    "channels": ["EMAIL", "PUSH"],
    "templateId": "order-confirmation",
    "templateData": {
      "orderId": "ORD-123",
      "amount": 99.99
    },
    "idempotencyKey": "order-123-notif"
  }'

# Get user notifications
curl http://localhost:8089/api/v1/notifications/user/user123

# Update preferences
curl -X PUT http://localhost:8089/api/v1/notifications/user/user123/preferences \
  -H "Content-Type: application/json" \
  -d '{
    "enabledChannels": {
      "TRANSACTIONAL": ["EMAIL", "SMS"],
      "PROMOTIONAL": []
    },
    "quietHours": {
      "start": "22:00:00",
      "end": "08:00:00"
    },
    "timezone": "America/New_York"
  }'
```

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD/LLD
- [API Documentation](API_Documentation.md) - API reference
- [Scale Calculations](Scale_Calculations.md) - Performance analysis

## 🔧 Configuration

### Kafka Topics
```yaml
notifications.critical:
  partitions: 50
  replication: 3
  retention: 7 days

notifications.high:
  partitions: 100
  replication: 3
  retention: 3 days

notifications.medium:
  partitions: 200
  replication: 2
  retention: 1 day

notifications.low:
  partitions: 200
  replication: 2
  retention: 12 hours
```

### Circuit Breaker
```yaml
email-provider:
  failureRateThreshold: 50%
  waitDurationInOpenState: 30s
  slidingWindowSize: 100
  minimumNumberOfCalls: 10
```

### Retry Policy
```yaml
maxRetries: 5
initialDelay: 1s
backoffMultiplier: 2.0
maxDelay: 60s
```

## 📈 Monitoring

### Key Metrics
- `notifications.sent` - Total notifications sent by channel/priority
- `notifications.delivery.latency` - Delivery latency by channel
- `notifications.failed` - Failed notifications by channel/reason
- `notifications.dlq` - DLQ entries by reason
- `kafka.consumer.lag` - Consumer lag by topic

### Alerts
- High failure rate (>5% for 5 minutes)
- DLQ backlog (>1000 entries)
- High latency (p99 >10s for 5 minutes)
- Consumer lag (>100K messages)

### Dashboards
- Grafana: Real-time metrics and alerts
- Kibana: Log analysis and debugging
- Jaeger: Distributed tracing

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -P integration-tests
```

### Load Testing
```bash
k6 run docs/notification/load-test.js
```

## 🔒 Security

- **Encryption at Rest**: AES-256 for sensitive data
- **Encryption in Transit**: TLS 1.3
- **PII Masking**: Sensitive data masked in logs
- **RBAC**: Role-based access control
- **Rate Limiting**: Per-user, per-endpoint limits
- **API Authentication**: JWT-based authentication

## 💰 Cost

### Monthly Cost (1B notifications)
- Infrastructure: $131,359
- Email (SendGrid): $15,000
- SMS (Twilio): $1,125,000
- **Total**: $1,256,359/month
- **Per Notification**: $1.26

### Optimization Strategies
- Batch processing
- Cheaper SMS providers
- Smart channel routing
- Compression
- Auto-scaling

## 🚦 API Rate Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| POST /notifications | 1000 | 60s |
| GET /notifications/user/{id} | 100 | 60s |
| PUT /preferences | 10 | 60s |

## 🔄 Failure Handling

### Retry Strategy
1. Attempt 1: Immediate
2. Attempt 2: 1s delay
3. Attempt 3: 2s delay
4. Attempt 4: 4s delay
5. Attempt 5: 8s delay
6. Move to DLQ

### DLQ Processing
- Automatic reprocessing every 5 minutes
- Manual intervention alerts
- 30-day retention

## 🌍 Multi-Region Deployment

### Active-Active Setup
- Route 53 for global DNS
- Regional Kafka clusters
- Cross-region database replication
- Regional Redis caches
- CloudFront for API caching

## 📦 Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot2</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

## 🎓 Design Patterns Used

- **Circuit Breaker**: Provider failure protection
- **Retry Pattern**: Exponential backoff with jitter
- **Dead Letter Queue**: Failed message handling
- **Fan-Out**: Broadcast notifications
- **Priority Queue**: Priority-based processing
- **Idempotency**: Duplicate prevention
- **CQRS**: Separate read/write paths
- **Event Sourcing**: Audit trail

## 🔮 Future Enhancements

1. **AI-Powered Optimization**
   - Best time to send predictions
   - Channel preference learning
   - Content personalization

2. **Advanced Analytics**
   - A/B testing framework
   - Conversion tracking
   - User engagement scoring

3. **Rich Media Support**
   - Image/video attachments
   - Interactive notifications
   - AMP for email

4. **Multi-Tenancy**
   - Tenant isolation
   - Custom rate limits
   - White-label support

---

**Built with ❤️ for high-scale distributed systems**

For questions or support, open an issue or contact via [portfolio](https://sudhirmeenaswe.netlify.app/).
