# WhatsApp Messenger Clone - Production-Ready Implementation

## 🚀 Overview

A highly scalable, real-time messaging platform built with Spring Boot, featuring all core WhatsApp functionalities including instant messaging, group chats, media sharing, status updates, and real-time notifications.

## ✨ Key Features

### 📱 Core Messaging
- **Real-time messaging** with WebSocket (sub-100ms latency)
- **Individual & Group chats** (up to 256 participants)
- **Message types**: Text, Image, Video, Audio, Document, Location, Contact, Sticker
- **Message status tracking**: Sent ✓, Delivered ✓✓, Read ✓✓ (blue ticks)
- **Reply and Forward** functionality
- **Message deletion** (delete for everyone within 1 hour)
- **Message search** within chats

### 👥 Advanced Features
- **WhatsApp Status** (24-hour expiry stories)
- **Typing indicators** and real-time presence
- **Last seen** timestamps with privacy controls
- **Profile management** with photos and about section
- **Group administration** (add/remove participants, multiple admins)
- **Media compression** and thumbnail generation
- **Contact synchronization** and user discovery

### 🏗️ Technical Excellence
- **Microservices architecture** with Spring Boot
- **Real-time communication** via WebSocket/STOMP
- **Horizontal scalability** with Redis and Kafka
- **Multi-database strategy** (PostgreSQL + Cassandra)
- **CDN integration** for global media delivery
- **Comprehensive monitoring** and observability

## 🛠️ Technology Stack

### Backend Framework
- **Java 17** with Spring Boot 3.2
- **Spring WebSocket** for real-time communication
- **Spring Data JPA** for database operations
- **Spring Security** for authentication
- **Maven** for dependency management

### Databases & Storage
- **PostgreSQL** - User metadata, chat information
- **Cassandra** - Message storage (horizontal scaling)
- **Redis** - Caching, session management, real-time data
- **Amazon S3** - Media file storage with CDN

### Message Queue & Real-time
- **Apache Kafka** - Event streaming and message queuing
- **WebSocket/STOMP** - Real-time bidirectional communication
- **Redis Pub/Sub** - Real-time notifications

### Infrastructure
- **Docker** - Containerization
- **Kubernetes** - Orchestration (production)
- **Prometheus + Grafana** - Monitoring
- **ELK Stack** - Logging and analytics

## 📊 Scale & Performance

### Capacity
- **2 billion users** supported
- **1 billion daily active users**
- **100 billion messages per day**
- **1.2 million messages per second** at peak
- **99.99% uptime** with multi-region deployment

### Performance Metrics
- **<100ms message delivery** latency
- **<200ms read receipt** latency
- **Sub-second** typing indicator updates
- **10PB+ message storage** with Cassandra
- **100PB+ media storage** with S3/CDN

## 🚀 Quick Start

### Prerequisites
```bash
# Required software
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+
```

### 1. Clone and Setup
```bash
git clone https://github.com/sudhir512kj/system-designs.git
cd system-designs

# Start infrastructure services
docker-compose up -d postgres redis kafka
```

### 2. Database Setup
```bash
# Create WhatsApp database
createdb whatsapp_db

# Tables will be auto-created by Hibernate
```

### 3. Configuration
```bash
# Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export KAFKA_BROKERS=localhost:9092
```

### 4. Build and Run
```bash
# Build the project
mvn clean install

# Run WhatsApp service
./run-systems.sh whatsapp
# OR
mvn spring-boot:run -Dspring-boot.run.profiles=whatsapp
```

**Service will start on**: `http://localhost:8093`

### 5. Test the APIs
```bash
# Register a user
curl -X POST "http://localhost:8093/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890", "name": "John Doe"}'

# Create a chat and send messages (see API documentation)
```

## 📱 Real-time WebSocket Integration

### JavaScript Client Example
```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8093/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected to WhatsApp WebSocket');
    
    // Subscribe to chat messages
    stompClient.subscribe('/topic/chat/chat123', function(message) {
        const messageData = JSON.parse(message.body);
        displayNewMessage(messageData);
    });
    
    // Subscribe to typing indicators
    stompClient.subscribe('/topic/chat/chat123/typing', function(message) {
        const typingData = JSON.parse(message.body);
        showTypingIndicator(typingData.userName);
    });
    
    // Subscribe to read receipts
    stompClient.subscribe('/topic/chat/chat123/read', function(message) {
        const userId = message.body;
        updateMessageReadStatus(userId);
    });
});

// Send typing indicator
function startTyping() {
    stompClient.send('/app/chat/chat123/typing', {}, JSON.stringify({
        userId: 'user123',
        userName: 'John Doe'
    }));
}

// Send message via REST API
async function sendMessage(content) {
    const response = await fetch('http://localhost:8093/api/v1/messages/send?senderId=user123', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            chatId: 'chat123',
            content: content,
            type: 'TEXT'
        })
    });
    return response.json();
}
```

## 🔧 API Usage Examples

### User Management
```bash
# Register user
curl -X POST "http://localhost:8093/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+1234567890", "name": "John Doe"}'

# Search users
curl -X GET "http://localhost:8093/api/v1/users/search?query=john"

# Update status
curl -X PUT "http://localhost:8093/api/v1/users/user123/status?status=ONLINE"
```

### Chat Management
```bash
# Create individual chat
curl -X POST "http://localhost:8093/api/v1/chats/individual" \
  -d "userId1=user123&userId2=user456"

# Create group chat
curl -X POST "http://localhost:8093/api/v1/chats/group" \
  -H "Content-Type: application/json" \
  -d '{
    "creatorId": "user123",
    "name": "Family Group",
    "participantIds": ["user456", "user789"]
  }'

# Get user chats
curl -X GET "http://localhost:8093/api/v1/chats/user/user123"
```

### Messaging
```bash
# Send text message
curl -X POST "http://localhost:8093/api/v1/messages/send?senderId=user123" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "Hello, how are you?",
    "type": "TEXT"
  }'

# Send image message
curl -X POST "http://localhost:8093/api/v1/messages/send?senderId=user123" \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "chat123",
    "content": "Check this out!",
    "type": "IMAGE",
    "mediaUrl": "https://example.com/image.jpg",
    "mediaType": "image/jpeg"
  }'

# Get chat messages
curl -X GET "http://localhost:8093/api/v1/messages/chat/chat123?page=0&size=20"

# Mark as read
curl -X PUT "http://localhost:8093/api/v1/messages/chat/chat123/read?userId=user123"
```

## 🏗️ Architecture Highlights

### Microservices Design
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Service  │    │   Chat Service  │    │ Message Service │
│                 │    │                 │    │                 │
│ • Registration  │    │ • Chat Creation │    │ • Send/Receive  │
│ • Profile Mgmt  │    │ • Participants  │    │ • Delivery      │
│ • Status Update │    │ • Group Admin   │    │ • Read Receipts │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │ WebSocket Layer │
                    │                 │
                    │ • Real-time     │
                    │ • Typing        │
                    │ • Presence      │
                    └─────────────────┘
```

### Data Flow Architecture
```
Client App → Load Balancer → API Gateway → Microservices
     ↓                                           ↓
WebSocket ←─────────── Message Queue ←──── Database
     ↓                     (Kafka)              ↓
Real-time UI ←──────────────────────────── Cache (Redis)
```

### Database Strategy
- **PostgreSQL**: User profiles, chat metadata, relationships
- **Cassandra**: Messages (partitioned by chat_id for scale)
- **Redis**: Active sessions, recent messages, typing indicators
- **S3**: Media files with global CDN distribution

## 📈 Performance Optimizations

### Caching Strategy
```java
// Multi-layer caching
L1: Application Cache (Recent messages)
L2: Redis Cache (User sessions, chat metadata)
L3: Database (PostgreSQL + Cassandra)
```

### Message Delivery Optimization
- **Batch processing** for group messages
- **Connection pooling** for WebSocket management
- **Async processing** for non-critical operations
- **Circuit breakers** for external service calls

### Scalability Features
- **Horizontal scaling** with stateless services
- **Database sharding** by user_id and chat_id
- **CDN integration** for global media delivery
- **Auto-scaling** based on load metrics

## 🔒 Security & Privacy

### Data Protection
- **Message encryption** at rest and in transit
- **User privacy controls** (last seen, profile visibility)
- **Secure media handling** with signed URLs
- **Rate limiting** to prevent abuse

### Authentication & Authorization
- **Phone number verification** (OTP-based)
- **JWT tokens** for session management
- **Role-based access** for group administration
- **API rate limiting** per user

## 📊 Monitoring & Observability

### Key Metrics Tracked
```yaml
System Metrics:
  - Message delivery latency (P95, P99)
  - WebSocket connection count
  - Database query performance
  - Cache hit ratios
  - Error rates by service

Business Metrics:
  - Daily/Monthly active users
  - Messages sent per user
  - Group chat engagement
  - Media sharing statistics
```

### Health Checks
```bash
# Service health
curl http://localhost:8093/actuator/health

# Metrics endpoint
curl http://localhost:8093/actuator/metrics

# Database connectivity
curl http://localhost:8093/actuator/health/db
```

## 🧪 Testing

### Load Testing
```bash
# Install k6 for load testing
brew install k6

# Run message throughput test
k6 run docs/whatsapp/load-test.js

# WebSocket connection test
k6 run docs/whatsapp/websocket-test.js
```

### Integration Testing
```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -Dtest="*IntegrationTest"

# Run WebSocket tests
mvn test -Dtest="*WebSocketTest"
```

## 🚀 Production Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t whatsapp-messenger .

# Run with Docker Compose
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment
```yaml
# Deploy to Kubernetes
kubectl apply -f k8s/whatsapp-deployment.yaml
kubectl apply -f k8s/whatsapp-service.yaml
kubectl apply -f k8s/whatsapp-ingress.yaml
```

### Environment Configuration
```bash
# Production environment variables
export SPRING_PROFILES_ACTIVE=whatsapp,prod
export DB_HOST=postgres-cluster.internal
export REDIS_CLUSTER=redis-cluster.internal
export KAFKA_BROKERS=kafka-cluster.internal:9092
export S3_BUCKET=whatsapp-media-prod
export CDN_URL=https://cdn.whatsapp.example.com
```

## 📚 Documentation

- **[System Design](System_Design.md)** - Complete architecture and design decisions
- **[API Documentation](API_Documentation.md)** - Comprehensive API reference
- **[Scale Calculations](Scale_Calculations.md)** - Performance analysis and capacity planning

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/whatsapp-enhancement`
3. Commit changes: `git commit -am 'Add new WhatsApp feature'`
4. Push to branch: `git push origin feature/whatsapp-enhancement`
5. Submit pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## 🙏 Acknowledgments

- **WhatsApp Engineering Team** for architectural inspiration
- **Signal Protocol** for end-to-end encryption concepts
- **Spring Boot Community** for excellent framework
- **Apache Kafka** for reliable message streaming

---

**Built with ❤️ by Sudhir Meena**

For questions or support, please open an issue or contact via [portfolio](https://sudhirmeenaswe.netlify.app/).

**🌟 Star this repository if you found it helpful!**