# WhatsApp Messenger - Implementation Guide

## 🎯 Overview

This guide provides step-by-step instructions for implementing, deploying, and scaling the WhatsApp Messenger system following enterprise software development practices.

## 🏗️ Architecture Implementation

### 1. Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│              Presentation Layer          │
│  Controllers, WebSocket, Exception       │
│           Handlers, DTOs                 │
├─────────────────────────────────────────┤
│             Application Layer            │
│   Services, Validation, Caching,        │
│        Message Queue, Presence          │
├─────────────────────────────────────────┤
│              Domain Layer                │
│    Entities, Business Logic,            │
│         Domain Services                  │
├─────────────────────────────────────────┤
│           Infrastructure Layer           │
│  Repositories, External APIs,           │
│      Database, Redis, Kafka             │
└─────────────────────────────────────────┘
```

### 2. SOLID Principles Implementation

#### Single Responsibility Principle (SRP)
```java
// ✅ Good: Each service has single responsibility
@Service
public class MessageService {
    // Only handles message operations
}

@Service  
public class PresenceService {
    // Only handles user presence
}

@Service
public class ConnectionManagerService {
    // Only handles WebSocket connections
}
```

#### Dependency Inversion Principle (DIP)
```java
// ✅ Good: Depend on abstractions
@Service
public class MessageService {
    private final MessageRepository messageRepository; // Interface
    private final MessageQueueService messageQueueService; // Interface
    
    // Constructor injection
    public MessageService(MessageRepository messageRepository,
                         MessageQueueService messageQueueService) {
        this.messageRepository = messageRepository;
        this.messageQueueService = messageQueueService;
    }
}
```

## 🚀 Development Setup

### 1. Prerequisites Installation

```bash
# Java 17
sdk install java 17.0.7-tem
sdk use java 17.0.7-tem

# Maven 3.8+
brew install maven

# Docker & Docker Compose
brew install docker docker-compose

# Redis
brew install redis

# Kafka
brew install kafka
```

### 2. Environment Configuration

```bash
# Create environment file
cat > .env << EOF
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=whatsapp_db
DB_USERNAME=postgres
DB_PASSWORD=password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka Configuration
KAFKA_BROKERS=localhost:9092
KAFKA_GROUP_ID=whatsapp-group

# Application Configuration
SERVER_PORT=8093
JWT_SECRET=your-secret-key
CORS_ORIGINS=http://localhost:3000,http://localhost:8080

# Monitoring
METRICS_ENABLED=true
HEALTH_CHECK_ENABLED=true
EOF
```

### 3. Database Setup

```sql
-- Create database
CREATE DATABASE whatsapp_db;

-- Create user
CREATE USER whatsapp_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE whatsapp_db TO whatsapp_user;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### 4. Infrastructure Services

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: whatsapp_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

volumes:
  postgres_data:
  redis_data:
```

## 🔧 Implementation Steps

### Step 1: Core Domain Models

```java
// 1. Create base entity
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

// 2. Implement domain entities
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String phoneNumber;
    
    private String name;
    private String profilePicture;
    private String about;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    private LocalDateTime lastSeen;
    
    // Constructors, getters, setters
}
```

### Step 2: Repository Layer

```java
// 1. Create base repository interface
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // Common methods
}

// 2. Implement specific repositories
@Repository
public interface UserRepository extends BaseRepository<User, String> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT u FROM User u WHERE u.phoneNumber LIKE %:query% OR u.name LIKE %:query%")
    List<User> searchUsers(@Param("query") String query);
}
```

### Step 3: Service Layer with Validation

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserDTO registerUser(String phoneNumber, String name) {
        // 1. Validate input
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            throw new WhatsAppException.InvalidOperationException("Invalid phone number format");
        }
        
        // 2. Check for duplicates
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new WhatsAppException.InvalidOperationException("User already exists");
        }
        
        // 3. Create and save user
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .name(name)
                .status(User.UserStatus.OFFLINE)
                .about(WhatsAppConstants.DEFAULT_ABOUT)
                .build();
        
        user = userRepository.save(user);
        log.info("User registered: {}", phoneNumber);
        
        return convertToDTO(user);
    }
    
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .profilePicture(user.getProfilePicture())
                .about(user.getAbout())
                .status(user.getStatus())
                .lastSeen(user.getLastSeen())
                .build();
    }
}
```

### Step 4: Controller Layer with Error Handling

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        UserDTO user = userService.registerUser(
            request.getPhoneNumber(), 
            request.getName()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam @NotBlank String query) {
        
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
}
```

### Step 5: WebSocket Implementation

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    
    private final PresenceService presenceService;
    
    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(@DestinationVariable String chatId, 
                           @Payload TypingEvent event) {
        
        presenceService.updateUserPresence(event.getUserId(), User.UserStatus.TYPING);
        messagingTemplate.convertAndSend(
            WhatsAppConstants.CHAT_TOPIC + chatId + "/typing", 
            event
        );
    }
}
```

## 🧪 Testing Strategy

### 1. Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void registerUser_ValidInput_Success() {
        // Given
        String phoneNumber = "+1234567890";
        String name = "John Doe";
        
        when(userRepository.findByPhoneNumber(phoneNumber))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
            .thenReturn(createUser(phoneNumber, name));
        
        // When
        UserDTO result = userService.registerUser(phoneNumber, name);
        
        // Then
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getName()).isEqualTo(name);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void registerUser_InvalidPhoneNumber_ThrowsException() {
        // Given
        String invalidPhone = "invalid";
        String name = "John Doe";
        
        // When & Then
        assertThatThrownBy(() -> userService.registerUser(invalidPhone, name))
            .isInstanceOf(WhatsAppException.InvalidOperationException.class)
            .hasMessageContaining("Invalid phone number format");
    }
}
```

### 2. Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.profiles.active=test")
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void registerUser_ValidRequest_ReturnsCreated() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setPhoneNumber("+1234567890");
        request.setName("John Doe");
        
        // When
        ResponseEntity<UserDTO> response = restTemplate.postForEntity(
            "/api/v1/users/register", 
            request, 
            UserDTO.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getPhoneNumber()).isEqualTo("+1234567890");
        
        // Verify database
        Optional<User> savedUser = userRepository.findByPhoneNumber("+1234567890");
        assertThat(savedUser).isPresent();
    }
}
```

### 3. WebSocket Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    
    @BeforeEach
    void setUp() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJSClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        String url = "ws://localhost:" + port + "/ws";
        stompSession = stompClient.connect(url, new StompSessionHandlerAdapter()).get();
    }
    
    @Test
    void sendMessage_ReceivesWebSocketNotification() throws Exception {
        // Given
        CompletableFuture<MessageDTO> messageReceived = new CompletableFuture<>();
        
        stompSession.subscribe("/topic/chat/chat123", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageDTO.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageReceived.complete((MessageDTO) payload);
            }
        });
        
        // When
        SendMessageRequest request = SendMessageRequest.builder()
            .chatId("chat123")
            .content("Test message")
            .type(Message.MessageType.TEXT)
            .build();
            
        restTemplate.postForEntity(
            "/api/v1/messages/send?senderId=user123", 
            request, 
            MessageDTO.class
        );
        
        // Then
        MessageDTO receivedMessage = messageReceived.get(5, TimeUnit.SECONDS);
        assertThat(receivedMessage.getContent()).isEqualTo("Test message");
    }
}
```

## 🚀 Deployment Guide

### 1. Production Configuration

```yaml
# application-prod.yml
server:
  port: 8093
  
spring:
  datasource:
    url: jdbc:postgresql://postgres-cluster:5432/whatsapp_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  redis:
    cluster:
      nodes: ${REDIS_CLUSTER_NODES}
    password: ${REDIS_PASSWORD}
    
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        
logging:
  level:
    org.sudhir512kj.whatsapp: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 2. Kubernetes Deployment

```yaml
# whatsapp-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: whatsapp-messenger
spec:
  replicas: 3
  selector:
    matchLabels:
      app: whatsapp-messenger
  template:
    metadata:
      labels:
        app: whatsapp-messenger
    spec:
      containers:
      - name: whatsapp-messenger
        image: whatsapp/messenger:latest
        ports:
        - containerPort: 8093
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: whatsapp-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: whatsapp-secrets
              key: db-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8093
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8093
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: whatsapp-service
spec:
  selector:
    app: whatsapp-messenger
  ports:
  - port: 80
    targetPort: 8093
  type: LoadBalancer
```

### 3. Monitoring Setup

```yaml
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
    - job_name: 'whatsapp-messenger'
      static_configs:
      - targets: ['whatsapp-service:80']
      metrics_path: /actuator/prometheus
      scrape_interval: 5s
```

### 4. CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy WhatsApp Messenger

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run tests
      run: mvn clean test
      
    - name: Run integration tests
      run: mvn verify -P integration-tests

  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Build Docker image
      run: |
        docker build -t whatsapp/messenger:${{ github.sha }} .
        docker tag whatsapp/messenger:${{ github.sha }} whatsapp/messenger:latest
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/whatsapp-messenger \
          whatsapp-messenger=whatsapp/messenger:${{ github.sha }}
        kubectl rollout status deployment/whatsapp-messenger
```

## 📊 Performance Optimization

### 1. Database Optimization

```sql
-- Create indexes for performance
CREATE INDEX CONCURRENTLY idx_messages_chat_created 
ON messages(chat_id, created_at DESC);

CREATE INDEX CONCURRENTLY idx_users_phone 
ON users(phone_number);

CREATE INDEX CONCURRENTLY idx_message_deliveries_user_status 
ON message_deliveries(user_id, status);

-- Partition messages table by date
CREATE TABLE messages_2024_01 PARTITION OF messages
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 2. Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

### 3. Connection Pool Tuning

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
```

## 🔍 Monitoring and Alerting

### 1. Custom Metrics

```java
@Component
public class WhatsAppMetrics {
    
    private final Counter messagesCounter;
    private final Timer messageProcessingTimer;
    private final Gauge activeConnections;
    
    public WhatsAppMetrics(MeterRegistry meterRegistry) {
        this.messagesCounter = Counter.builder("whatsapp.messages.sent")
            .description("Total messages sent")
            .register(meterRegistry);
            
        this.messageProcessingTimer = Timer.builder("whatsapp.message.processing.time")
            .description("Message processing time")
            .register(meterRegistry);
            
        this.activeConnections = Gauge.builder("whatsapp.websocket.connections")
            .description("Active WebSocket connections")
            .register(meterRegistry, this, WhatsAppMetrics::getActiveConnections);
    }
    
    public void incrementMessagesSent() {
        messagesCounter.increment();
    }
    
    public Timer.Sample startMessageProcessing() {
        return Timer.start(messageProcessingTimer);
    }
    
    private double getActiveConnections() {
        // Return actual connection count
        return connectionManager.getActiveConnectionCount();
    }
}
```

### 2. Health Checks

```java
@Component
public class WhatsAppHealthIndicator implements HealthIndicator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check Redis connectivity
        try {
            redisTemplate.opsForValue().get("health-check");
            builder.withDetail("redis", "UP");
        } catch (Exception e) {
            builder.down().withDetail("redis", "DOWN: " + e.getMessage());
        }
        
        // Check Kafka connectivity
        try {
            kafkaTemplate.send("health-check", "ping").get(1, TimeUnit.SECONDS);
            builder.withDetail("kafka", "UP");
        } catch (Exception e) {
            builder.down().withDetail("kafka", "DOWN: " + e.getMessage());
        }
        
        return builder.build();
    }
}
```

## 🎯 Best Practices Summary

### 1. Code Quality
- ✅ Follow SOLID principles
- ✅ Use proper exception handling
- ✅ Implement comprehensive validation
- ✅ Write meaningful tests
- ✅ Use consistent naming conventions

### 2. Performance
- ✅ Implement multi-layer caching
- ✅ Use connection pooling
- ✅ Optimize database queries
- ✅ Implement async processing
- ✅ Monitor performance metrics

### 3. Security
- ✅ Validate all inputs
- ✅ Implement proper authorization
- ✅ Use rate limiting
- ✅ Sanitize error messages
- ✅ Enable security headers

### 4. Scalability
- ✅ Design stateless services
- ✅ Use horizontal scaling
- ✅ Implement circuit breakers
- ✅ Use message queues
- ✅ Plan for multi-region deployment

This implementation guide provides a complete roadmap for building a production-ready WhatsApp Messenger system following enterprise software development practices.