# Netflix System Design

Comprehensive Netflix backend implementation based on Netflix Technology Blog.

## Key Features

### 1. Adaptive Bitrate Streaming (ABR)
- Dynamic quality selection based on bandwidth
- Buffer health monitoring
- Smooth quality transitions

### 2. Content Encoding Pipeline
- Multi-bitrate encoding (360p to 4K)
- H.265 codec support
- HLS manifest generation

### 3. Open Connect CDN
- Regional CDN server selection
- Optimal routing based on user location
- Health checking

### 4. Recommendation Engine
- Collaborative filtering
- Cosine similarity for content matching
- Redis caching with 1-hour TTL
- Personalized recommendations

### 5. EVCache (Distributed Caching)
- Redis-based caching layer
- Cache-aside pattern
- TTL management

### 6. Circuit Breaker (Hystrix-inspired)
- Automatic failure detection
- Fallback mechanisms
- Self-healing with timeout

### 7. Chaos Engineering (Simian Army)
- Chaos Monkey: Random failures
- Latency Monkey: Network delays
- Conformity Monkey: Version checking

### 8. A/B Testing Framework
- Variant assignment based on user ID
- Metric tracking per variant
- Distribution control

### 9. Analytics Pipeline
- Kafka-based event streaming
- Playback event tracking
- Quality switch monitoring

### 10. Personalized Thumbnails
- User-specific artwork selection
- Multiple thumbnail variants
- A/B testing for artwork

## Architecture

```
Client → API Gateway → Microservices
                      ├── Streaming Service
                      ├── Recommendation Engine
                      ├── Encoding Service
                      ├── CDN Service
                      └── Analytics Service
                      
Data Stores:
- Cassandra: User profiles, viewing activity, content metadata
- Redis: Session cache, recommendations, EVCache
- Kafka: Event streaming, analytics pipeline
```

## Technologies Referenced from Netflix Blog

1. **EVCache**: Distributed caching
2. **Hystrix**: Circuit breaker pattern
3. **Zuul**: API Gateway (implicit in routing)
4. **Simian Army**: Chaos engineering
5. **Open Connect**: CDN infrastructure
6. **Adaptive Bitrate**: Dynamic quality selection
7. **Personalization**: Recommendation algorithms
8. **A/B Testing**: Experimentation framework

## API Endpoints

### Streaming
- POST `/api/v1/streaming/start` - Initiate streaming session
- POST `/api/v1/streaming/heartbeat` - Update playback progress

### Content
- POST `/api/v1/content/stream/start` - Start content stream
- POST `/api/v1/content/stream/progress` - Update progress
- GET `/api/v1/content/recommendations/{userId}` - Get recommendations

## Configuration

See `application-netflix.yml` for:
- Cassandra settings
- Redis configuration
- Kafka brokers
- Quality profiles
- Cache TTL settings
