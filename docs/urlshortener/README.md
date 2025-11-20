# TinyURL Clone - URL Shortener System

A highly scalable URL shortener service similar to TinyURL with sub-100ms redirect latency, supporting billions of URLs and redirects.

## 📋 Overview

This URL shortener implements a production-ready system with:
- **Short URL Generation** with Base62 encoding
- **Fast Redirects** with multi-layer caching (<100ms)
- **Analytics Tracking** for clicks, geography, and referrers
- **Custom Aliases** for branded short URLs
- **Expiration Support** for time-limited URLs
- **Rate Limiting** to prevent abuse
- **High Availability** with 99.99% uptime

## 🎯 Key Features

### ✅ **URL Shortening**
- Base62 encoding for compact URLs (3.5 trillion unique URLs)
- Collision detection and resolution
- Custom alias support for branded links
- Bulk URL shortening API
- URL validation and sanitization
- Malicious domain blocking

### ✅ **Fast Redirects**
- Multi-layer caching (Application + Redis + Database)
- Sub-100ms redirect latency
- Geographic routing for global users
- Automatic cache warming
- Cache invalidation on updates
- CDN integration for static assets

### ✅ **Analytics & Tracking**
- Click count tracking
- Geographic location data (country, city)
- Referrer tracking (source websites)
- User agent analysis (browser, device, OS)
- Time-series analytics
- Real-time dashboard

### ✅ **Security & Abuse Prevention**
- Rate limiting with sliding window algorithm
- Malicious URL detection
- Spam prevention
- CAPTCHA for suspicious activity
- IP-based blocking
- URL expiration support

### ✅ **Management Features**
- URL editing and updates
- Soft deletion with recovery
- Expiration date setting
- Access control and permissions
- Bulk operations
- API key management

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Requests                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Load Balancer + CDN                                │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────────┐
│              Application Servers                                │
├─────────────────────┼───────────────────────────────────────────┤
│ URL Service │ Analytics │ Rate Limiter │ Cache Manager          │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│ PostgreSQL   │ │  Redis  │ │  Analytics  │
│   Cluster    │ │  Cache  │ │   Database  │
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
export DB_URL=jdbc:postgresql://localhost:5432/urlshortener_db
export DB_USERNAME=url_user
export DB_PASSWORD=url_pass

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Application
export BASE_URL=http://localhost:8092
export SHORT_CODE_LENGTH=7
```

### Run the Service
```bash
mvn clean install
./run-systems.sh urlshortener
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=urlshortener

# Service available at http://localhost:8092
```

### Shorten a URL
```bash
# Create short URL
curl -X POST http://localhost:8092/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/very/long/url/path",
    "customAlias": "mylink",
    "expirationDate": "2025-12-31T23:59:59"
  }'

# Response
{
  "shortUrl": "http://localhost:8092/mylink",
  "longUrl": "https://www.example.com/very/long/url/path",
  "shortCode": "mylink",
  "createdAt": "2024-01-15T10:30:00"
}

# Access short URL (redirects to long URL)
curl -I http://localhost:8092/mylink
```

## 📊 Performance & Scale

### Scale Targets
- **URLs Created**: 100M new URLs per day
- **Redirects**: 10B redirects per day
- **Latency**: <100ms for redirects (P95)
- **Availability**: 99.99% uptime
- **Storage**: Billions of URLs
- **Throughput**: 115K redirects/second

### Key Metrics
- **Cache Hit Ratio**: >95% for popular URLs
- **Database QPS**: 10K queries/second
- **Redis QPS**: 100K operations/second
- **URL Generation**: <10ms per URL
- **Analytics Processing**: Real-time with <1s delay

## 🔧 Core Components

### 1. **URL Shortening Service**
Generates short codes using Base62 encoding with collision detection.

### 2. **Redirect Service**
Fast URL lookup with multi-layer caching strategy.

### 3. **Analytics Service**
Tracks clicks, geography, referrers, and user agents.

### 4. **Rate Limiter**
Prevents abuse with sliding window algorithm.

### 5. **Cache Manager**
Manages multi-layer cache with intelligent invalidation.

### 6. **URL Validator**
Validates URLs and blocks malicious domains.

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD/LLD with encoding algorithms
- [API Documentation](API_Documentation.md) - Complete REST API reference

## 🎯 Critical Design Decisions

### 1. **Base62 Encoding**
```
Counter: 1234567890
Base62: aZl38jU

Benefits:
- URL-safe characters (a-z, A-Z, 0-9)
- 62^7 = 3.5 trillion unique URLs
- Compact representation
```

### 2. **Multi-Layer Caching**
```
Request → L1 (App Cache) → L2 (Redis) → L3 (Database)
         ↓ 50% hit        ↓ 45% hit    ↓ 5% hit
```
- 95% cache hit ratio
- Sub-100ms response time
- Reduced database load

### 3. **Rate Limiting Strategy**
```
Sliding Window Algorithm:
- 100 requests per hour per IP
- 1000 requests per hour per API key
- Exponential backoff for violations
```

### 4. **Analytics Architecture**
```
Click Event → Kafka → Stream Processing → Time-Series DB
```
- Real-time analytics
- Scalable event processing
- Historical data retention

## 🔒 Security Features

### URL Validation
- Format validation (RFC 3986)
- Malicious domain blocking
- Phishing URL detection
- Spam prevention
- HTTPS enforcement option

### Rate Limiting
- Per-IP rate limiting
- Per-user rate limiting
- API key-based limits
- CAPTCHA for suspicious activity
- Automatic IP blocking

### Access Control
- API key authentication
- User-based permissions
- URL ownership verification
- Soft deletion with recovery
- Audit logging

## 🧪 Testing Strategy

### Unit Tests
- Base62 encoding/decoding
- URL validation logic
- Cache management
- Rate limiting algorithms

### Integration Tests
- End-to-end URL shortening
- Redirect performance
- Analytics tracking
- Cache consistency

### Load Tests
- 100K redirects/second
- Cache performance under load
- Database connection pooling
- Rate limiter effectiveness

## 📈 Monitoring & Alerting

### Key Metrics
- **URL Creation Rate**: New URLs per second
- **Redirect Rate**: Redirects per second
- **Cache Hit Ratio**: Percentage of cache hits
- **Response Time**: P50, P95, P99 latencies
- **Error Rate**: Failed requests percentage

### Alerts
- Response time >100ms for 5 minutes
- Cache hit ratio <90%
- Error rate >1%
- Database connection pool >80%
- Rate limit violations spike

## 🔄 Deployment Strategy

### High Availability
- Multi-AZ deployment
- Auto-scaling based on traffic
- Database read replicas
- Redis cluster with failover
- CDN for global distribution

### Disaster Recovery
- Automated database backups
- Cross-region replication
- Point-in-time recovery
- RTO: 15 minutes
- RPO: 5 minutes

## 💡 Use Cases

### Personal Use
- Share long URLs on social media
- Track link performance
- Create memorable links

### Business Use
- Marketing campaigns with branded links
- QR code generation
- Email marketing tracking
- Social media analytics

### Enterprise Use
- Internal link management
- Access control and permissions
- Detailed analytics and reporting
- API integration with existing systems

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/url-enhancement`
3. Implement changes with tests
4. Update documentation
5. Submit pull request

---

**Built for billions of URLs with sub-100ms redirects and comprehensive analytics.**
