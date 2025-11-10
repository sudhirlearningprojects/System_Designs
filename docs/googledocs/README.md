# Google Docs Clone - System Design

A production-ready, scalable collaborative document editing platform with real-time synchronization, version control, and advanced collaboration features.

## 🎯 Overview

This Google Docs clone implements a complete cloud-based document editing system that supports:
- **Real-time Collaboration**: Multiple users editing simultaneously with Operational Transformation
- **Version History**: Complete audit trail with restore capability
- **Suggesting Mode**: Track changes with accept/reject workflow
- **Comments & Reactions**: Threaded discussions with emoji reactions
- **Offline Editing**: Local changes sync when reconnected
- **Auto-save**: Continuous background saving every 3 seconds

## ✨ Key Features

### Core Document Features
- ✅ Create, read, update, delete documents
- ✅ Rich text editing with formatting
- ✅ Auto-save every 3 seconds
- ✅ Watermarks (text/image)
- ✅ Export to Word, PDF, Markdown
- ✅ Templates for quick creation
- ✅ Document tags and organization

### Real-Time Collaboration
- ✅ **Operational Transformation (OT)**: Conflict-free concurrent editing
- ✅ **Live Cursor Tracking**: See where other users are editing
- ✅ **Active User List**: Real-time presence indicators
- ✅ **WebSocket Sync**: Sub-500ms latency for operations
- ✅ **Conflict Resolution**: Automatic merge of concurrent edits

### Version Control
- ✅ **Auto-versioning**: Snapshots every major change
- ✅ **Manual Saves**: Create named versions
- ✅ **Version History**: Browse all previous versions
- ✅ **Restore**: Rollback to any previous version
- ✅ **Diff View**: Compare versions side-by-side

### Collaboration Tools
- ✅ **Suggesting Mode**: Track changes like Word's "Track Changes"
- ✅ **Comments**: Anchor comments to specific text ranges
- ✅ **Threaded Replies**: Nested comment discussions
- ✅ **Emoji Reactions**: React to comments (👍 ❤️ 😂 😮 😢 🎉)
- ✅ **Comment Resolution**: Mark comments as resolved

### Access Control
- ✅ **Granular Permissions**: Owner, Editor, Commenter, Viewer
- ✅ **Share Links**: Generate shareable links with permissions
- ✅ **User Management**: Add/remove collaborators
- ✅ **Permission Inheritance**: Folder-level permissions

### Advanced Features
- ✅ **Offline Editing**: Edit without internet, sync on reconnect
- ✅ **Search**: Full-text search within documents
- ✅ **Voice Typing**: Speech-to-text (client-side)
- ✅ **Add-ons**: Extensible plugin architecture
- ✅ **E-signatures**: Request and manage signatures

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────┐
│   Clients   │  Web, Mobile, Desktop Apps
└──────┬──────┘
       │
┌──────▼──────┐
│ API Gateway │  Load Balancer, Rate Limiting
└──────┬──────┘
       │
┌──────▼──────────────────────────────────┐
│        Application Layer                 │
│  ┌──────────┐  ┌──────────┐            │
│  │Document  │  │Collab    │            │
│  │Service   │  │Service   │            │
│  └──────────┘  └──────────┘            │
│  ┌──────────┐  ┌──────────┐            │
│  │Comment   │  │Version   │            │
│  │Service   │  │Service   │            │
│  └──────────┘  └──────────┘            │
└──────┬──────────────────────────────────┘
       │
┌──────▼──────────────────────────────────┐
│         Real-Time Layer                  │
│  WebSocket Server (STOMP)                │
│  - Operation Broadcasting                │
│  - Cursor Tracking                       │
└──────┬──────────────────────────────────┘
       │
┌──────▼──────────────────────────────────┐
│          Data Layer                      │
│  ┌──────────┐  ┌──────────┐            │
│  │PostgreSQL│  │  Redis   │            │
│  │(Metadata)│  │ (Cache)  │            │
│  └──────────┘  └──────────┘            │
│  ┌──────────┐  ┌──────────┐            │
│  │Cassandra │  │    S3    │            │
│  │(Versions)│  │(Storage) │            │
│  └──────────┘  └──────────┘            │
└─────────────────────────────────────────┘
```

### Technology Stack

- **Backend**: Spring Boot 3.2, Java 17
- **Database**: PostgreSQL (metadata), Cassandra (versions)
- **Cache**: Redis Cluster
- **Storage**: Amazon S3
- **Real-time**: WebSocket (STOMP over SockJS)
- **Messaging**: Apache Kafka
- **Search**: Elasticsearch

## 📊 Scale & Performance

### Capacity
- **Users**: 1 Billion total, 100M daily active
- **Documents**: 5 Billion documents
- **Storage**: 11 PB (documents + versions)
- **Concurrent Editing**: 10M simultaneous users
- **Operations**: 5M edit operations/second

### Performance
- **Read Latency**: <100ms (p99)
- **Write Latency**: <200ms (p99)
- **Real-time Sync**: <500ms
- **Availability**: 99.99% uptime

### Cost
- **Monthly Cost**: $33M
- **Cost per MAU**: $0.11
- **Cost per Document**: $0.022

## 🚀 Getting Started

### Prerequisites
```bash
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+
```

### Installation

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
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

4. **Build and run**
```bash
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=googledocs
```

The application will start on `http://localhost:8091`

### Quick Start Script
```bash
./run-systems.sh googledocs
```

## 📚 API Examples

### Create Document
```bash
curl -X POST http://localhost:8091/api/v1/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My Document",
    "userId": "user-123"
  }'
```

### Get Document
```bash
curl -X GET http://localhost:8091/api/v1/documents/{documentId}
```

### Add Comment
```bash
curl -X POST http://localhost:8091/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "doc-456",
    "userId": "user-123",
    "content": "Great work!",
    "startPosition": 100,
    "endPosition": 150
  }'
```

### WebSocket Connection
```javascript
const socket = new SockJS('http://localhost:8091/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to document updates
    stompClient.subscribe('/topic/document/' + documentId, function(message) {
        const operation = JSON.parse(message.body);
        applyOperation(operation);
    });
    
    // Join document session
    stompClient.send('/app/document/' + documentId + '/join', {}, JSON.stringify({
        userId: 'user-123',
        userName: 'Alice'
    }));
});
```

## 🔧 Configuration

### Application Properties
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/googledocs
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

server:
  port: 8091
```

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
# Install k6
brew install k6

# Run load test
k6 run docs/googledocs/load-test.js
```

## 📖 Documentation

- **[System Design](System_Design.md)**: Complete HLD/LLD with architecture diagrams
- **[API Documentation](API_Documentation.md)**: Comprehensive API reference
- **[Scale Calculations](Scale_Calculations.md)**: Performance analysis and cost breakdown

## 🔑 Key Design Decisions

### 1. Operational Transformation (OT)
- **Why**: Proven algorithm for conflict-free concurrent editing
- **Alternative**: CRDT (more complex, higher overhead)
- **Trade-off**: OT requires central server, but simpler to implement

### 2. WebSocket for Real-time Sync
- **Why**: Low-latency bidirectional communication
- **Alternative**: Long polling (higher latency)
- **Trade-off**: More complex server infrastructure

### 3. Redis for Session State
- **Why**: Fast in-memory access for active sessions
- **Alternative**: Database (too slow for real-time)
- **Trade-off**: Additional infrastructure component

### 4. Cassandra for Version History
- **Why**: Efficient time-series storage, horizontal scaling
- **Alternative**: PostgreSQL (limited scalability)
- **Trade-off**: Eventually consistent reads

### 5. Multi-layer Caching
- **Why**: Reduce database load by 95%
- **Layers**: Application (Caffeine) → Redis → Database
- **Trade-off**: Cache invalidation complexity

## 🔐 Security

### Authentication
- JWT-based authentication
- Token expiry: 24 hours
- Refresh token support

### Authorization
- Role-based access control (RBAC)
- Permission types: Owner, Editor, Commenter, Viewer
- Document-level permissions

### Data Protection
- TLS 1.3 for all communication
- AES-256 encryption at rest
- End-to-end encryption for sensitive documents

## 📈 Monitoring

### Metrics
- Request rate and latency (p50, p95, p99)
- Active WebSocket connections
- Cache hit ratio
- Database query performance
- Error rates

### Logging
- Structured logging with JSON format
- Log levels: DEBUG, INFO, WARN, ERROR
- Distributed tracing with correlation IDs

### Alerting
- High latency (p99 > 500ms)
- Error rate > 1%
- Database connection pool > 80%
- Redis memory > 90%

## 🚧 Roadmap

### Phase 1 (Current)
- ✅ Real-time collaboration with OT
- ✅ Version history
- ✅ Comments and suggestions
- ✅ Permissions management

### Phase 2 (Next)
- 🔲 AI-powered features (smart compose, grammar)
- 🔲 Advanced formatting (tables, charts)
- 🔲 Voice typing integration
- 🔲 Mobile app support

### Phase 3 (Future)
- 🔲 Video/audio calls within document
- 🔲 Whiteboard integration
- 🔲 Advanced DLP (Data Loss Prevention)
- 🔲 Compliance certifications (SOC 2, HIPAA)

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## 🙏 Acknowledgments

- Google Docs for inspiration
- Operational Transformation algorithm by Ellis and Gibbs
- Spring Boot team for the excellent framework
- Open source community

## 📞 Support

For questions or support:
- **Email**: sudhir512kj@gmail.com
- **Portfolio**: [sudhirmeenaswe.netlify.app](https://sudhirmeenaswe.netlify.app/)
- **GitHub Issues**: [Create an issue](https://github.com/sudhir512kj/system-designs/issues)

---

**Built with ❤️ by Sudhir Meena**
