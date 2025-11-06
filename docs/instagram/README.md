# Instagram Clone - Social Media Platform

A highly scalable, fault-tolerant Instagram clone built with microservices architecture, supporting billions of users with real-time features.

## 🚀 Features

### Core Social Features
- **User Management**: Registration, authentication, profile management
- **Content Sharing**: Photo/video posts with captions, hashtags, location tagging
- **Social Interactions**: Follow/unfollow, likes, comments, shares
- **News Feed**: Personalized timeline with hybrid push/pull algorithm
- **Stories**: 24-hour temporary content with views tracking
- **Direct Messaging**: Private messaging with real-time delivery
- **Search**: Users, posts, hashtags with Elasticsearch
- **Notifications**: Real-time push notifications via WebSocket

### Advanced Features
- **Feed Algorithm**: Hybrid approach for celebrities vs regular users
- **Media Processing**: Async image/video processing with multiple formats
- **Caching Strategy**: Multi-layer caching (L1/L2/L3) for optimal performance
- **Real-time Updates**: WebSocket connections for live interactions
- **Content Moderation**: AI-powered content filtering
- **Analytics**: User engagement and content performance metrics

## 🏗️ Architecture

### Microservices Design
- **User Service**: Authentication, profiles, social graph
- **Post Service**: Content creation, interactions, media management
- **Feed Service**: Timeline generation with hybrid algorithm
- **Notification Service**: Real-time notifications and push messages
- **Search Service**: Elasticsearch-powered search functionality
- **Media Service**: File upload, processing, and CDN delivery

### Technology Stack
- **Backend**: Spring Boot 3.2, Java 17
- **Databases**: PostgreSQL (users), Cassandra (posts), Redis (cache), Neo4j (social graph)
- **Message Queue**: Apache Kafka for async processing
- **Search**: Elasticsearch for full-text search
- **Storage**: Amazon S3 + CloudFront CDN
- **Caching**: Redis Cluster with consistent hashing
- **Security**: JWT authentication, OAuth2, rate limiting

## 📊 Scale Targets

- **Users**: 2B registered, 100M DAU
- **Posts**: 500M posts/day, 50M media uploads/day
- **Throughput**: 500K read QPS, 100K write QPS
- **Latency**: <200ms feed generation, <100ms interactions
- **Availability**: 99.99% uptime
- **Storage**: 734PB media, 185TB search index

## 🚀 Quick Start

### Prerequisites
```bash
# Required services
docker-compose up -d postgres redis kafka elasticsearch

# Environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export JWT_SECRET=mySecretKey
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Running the Application
```bash
# Build the project
mvn clean install

# Run Instagram service
./run-systems.sh instagram
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=instagram

# Service will be available at http://localhost:8087
```

### Database Setup
```sql
-- Create Instagram database
CREATE DATABASE instagram_db;

-- Tables will be auto-created by Hibernate
-- Check application logs for table creation
```

## 📡 API Endpoints

### Authentication
```bash
# Register user
curl -X POST http://localhost:8087/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123",
    "fullName": "John Doe"
  }'

# Login
curl -X POST http://localhost:8087/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

### Posts
```bash
# Create post
curl -X POST http://localhost:8087/api/v1/posts \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Beautiful sunset! 🌅 #sunset #photography",
    "mediaUrls": ["https://example.com/image.jpg"],
    "hashtags": ["sunset", "photography"],
    "location": "Malibu Beach"
  }'

# Get news feed
curl -X GET "http://localhost:8087/api/v1/feed?page=0&size=20" \
  -H "Authorization: Bearer <token>"

# Like post
curl -X POST http://localhost:8087/api/v1/posts/{postId}/like \
  -H "Authorization: Bearer <token>"
```

### Social Features
```bash
# Follow user
curl -X POST http://localhost:8087/api/v1/users/{userId}/follow \
  -H "Authorization: Bearer <token>"

# Search users
curl -X GET "http://localhost:8087/api/v1/users/search?q=john" \
  -H "Authorization: Bearer <token>"
```

## 🔧 Configuration

### Application Properties
Key configurations in `application-instagram.yml`:

```yaml
instagram:
  media:
    max-file-size: 50MB
    allowed-types: jpg,jpeg,png,gif,mp4,mov
  feed:
    cache-ttl: 300 # 5 minutes
    celebrity-threshold: 1000000 # 1M followers
  rate-limit:
    posts-per-hour: 50
    likes-per-hour: 1000
```

### Performance Tuning
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
```

## 📈 Monitoring & Observability

### Health Checks
```bash
# Application health
curl http://localhost:8087/actuator/health

# Metrics
curl http://localhost:8087/actuator/metrics
```

### Key Metrics to Monitor
- **Feed Generation Latency**: Target <200ms P95
- **Post Creation Rate**: Monitor for spam detection
- **Cache Hit Ratio**: Should be >90% for feeds
- **Database Connection Pool**: Monitor for bottlenecks
- **Kafka Lag**: Ensure real-time processing

## 🔒 Security Features

### Authentication & Authorization
- JWT-based stateless authentication
- Password hashing with BCrypt
- Rate limiting per user and endpoint
- Input validation and sanitization

### Data Protection
- SQL injection prevention
- XSS protection
- CORS configuration
- Sensitive data masking in logs

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
# Example load test for feed generation
k6 run --vus 100 --duration 30s load-test-feed.js
```

## 📚 Documentation

- **[System Design](System_Design.md)**: Complete HLD/LLD documentation
- **[Architecture Diagrams](Architecture_Diagrams.md)**: Visual system architecture
- **[API Documentation](API_Documentation.md)**: Comprehensive API reference
- **[Scale Calculations](Scale_Calculations.md)**: Performance and capacity planning

## 🚀 Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t instagram-clone .

# Run with Docker Compose
docker-compose -f docker-compose-instagram.yml up -d
```

### Kubernetes Deployment
```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/instagram/
```

## 🔮 Future Enhancements

### Planned Features
- **Live Streaming**: Real-time video broadcasting
- **AR Filters**: Augmented reality camera filters
- **Shopping**: In-app product tagging and purchases
- **Reels**: Short-form video content
- **IGTV**: Long-form video content
- **Business Analytics**: Creator and business insights

### Technical Improvements
- **GraphQL API**: More efficient data fetching
- **Event Sourcing**: Better audit trails and replay capability
- **Machine Learning**: Personalized feed ranking
- **Edge Computing**: Geo-distributed content delivery
- **Blockchain**: NFT integration for digital collectibles

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/instagram-enhancement`
3. Commit changes: `git commit -am 'Add new Instagram feature'`
4. Push to branch: `git push origin feature/instagram-enhancement`
5. Submit pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## 🙏 Acknowledgments

- Instagram for inspiration and feature reference
- Spring Boot community for excellent framework
- Open source contributors for tools and libraries

---

**Built with ❤️ for learning system design at scale**

For questions or support, please open an issue or contact via the main repository.