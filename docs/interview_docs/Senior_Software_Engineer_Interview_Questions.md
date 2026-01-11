# Senior Software Engineer Interview Questions (10-15 Years Experience)

## 📋 Interview Structure (75-90 minutes)

1. **Introduction & Background** (10 min)
2. **Technical Deep Dive** (30 min)
3. **Coding & Problem Solving** (20 min)
4. **System Design** (15 min)
5. **Behavioral & Experience** (10 min)
6. **Q&A** (5 min)

---

## 🎯 Part 1: Java & Spring Boot (20 min)

### Q1: Explain Java 8 features and their practical use cases.
**Expected Answer**:

```java
// 1. Lambda Expressions
List<String> names = users.stream()
    .filter(u -> u.getAge() > 18)
    .map(User::getName)
    .collect(Collectors.toList());

// 2. Streams API
int sum = numbers.stream()
    .filter(n -> n % 2 == 0)
    .mapToInt(Integer::intValue)
    .sum();

// 3. Optional
Optional<User> user = userRepository.findById(id);
return user.map(User::getName).orElse("Unknown");

// 4. Method References
list.forEach(System.out::println);

// 5. Default Methods in Interfaces
interface Vehicle {
    default void start() {
        System.out.println("Starting...");
    }
}

// 6. CompletableFuture
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .exceptionally(ex -> "Error: " + ex.getMessage());
```

**Use Cases**:
- Streams: Data processing, filtering, transformations
- Optional: Null safety, cleaner code
- CompletableFuture: Async operations, parallel processing

---

### Q2: How does Spring Boot auto-configuration work?
**Expected Answer**:

```java
// @SpringBootApplication combines three annotations
@SpringBootApplication
// = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// Auto-configuration example
@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create()
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}

// application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
```

**Key Concepts**:
- Conditional annotations (@ConditionalOnClass, @ConditionalOnMissingBean)
- spring.factories file
- Configuration properties binding

---

### Q3: Explain Spring transaction management.
**Expected Answer**:

```java
@Service
public class OrderService {
    
    // Default: REQUIRED propagation, rollback on RuntimeException
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
        paymentService.processPayment(order);
        // If payment fails, order is rolled back
    }
    
    // REQUIRES_NEW: Independent transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(String action) {
        auditRepository.save(new AuditLog(action));
        // Commits even if outer transaction fails
    }
    
    // Rollback on checked exceptions
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Order order) throws Exception {
        orderRepository.save(order);
        if (order.getAmount() > 10000) {
            throw new Exception("Amount too high");
        }
    }
    
    // Read-only optimization
    @Transactional(readOnly = true)
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }
}
```

**Propagation Levels**:
- REQUIRED: Join existing or create new
- REQUIRES_NEW: Always create new
- NESTED: Nested within existing
- SUPPORTS: Join if exists, non-transactional otherwise

---

### Q4: How do you handle exceptions in Spring Boot?
**Expected Answer**:

```java
// Global exception handler
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// Custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Error response DTO
@Data
public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
}
```

---

## 🚀 Part 2: Microservices & REST APIs (15 min)

### Q5: Design a RESTful API for an e-commerce order system.
**Expected Answer**:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    // Create order
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }
    
    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(toResponse(order));
    }
    
    // List orders with pagination
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.findOrders(status, pageable);
        return ResponseEntity.ok(orders.map(this::toResponse));
    }
    
    // Update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        
        Order order = orderService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(toResponse(order));
    }
    
    // Cancel order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}

// Request/Response DTOs
@Data
public class OrderRequest {
    @NotNull
    private Long userId;
    
    @NotEmpty
    private List<OrderItem> items;
    
    @NotNull
    private Address shippingAddress;
}

@Data
public class OrderResponse {
    private Long id;
    private Long userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
```

**REST Best Practices**:
- Use proper HTTP methods (GET, POST, PUT, PATCH, DELETE)
- Return appropriate status codes (200, 201, 204, 400, 404, 500)
- Use pagination for list endpoints
- Version your APIs (/api/v1/)
- Validate input with @Valid
- Use DTOs for request/response

---

### Q6: How do you implement API rate limiting?
**Expected Answer**:

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    private static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SECONDS = 60;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String userId = getUserId(request);
        String key = "rate_limit:" + userId;
        
        Long count = redis.opsForValue().increment(key);
        
        if (count == 1) {
            redis.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        
        if (count > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return false;
        }
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS - count));
        
        return true;
    }
}

// Register interceptor
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
```

---

### Q7: Explain circuit breaker pattern implementation.
**Expected Answer**:

```java
@Service
public class PaymentService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject(
            "http://payment-api/process",
            request,
            PaymentResponse.class
        );
    }
    
    private PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        log.error("Payment failed, using fallback", ex);
        return new PaymentResponse("PENDING", "Will retry later");
    }
}

// application.yml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  
  retry:
    instances:
      paymentService:
        maxAttempts: 3
        waitDuration: 1s
```

---

## 💻 Part 3: Coding Challenges (20 min)

### Q8: Implement a thread-safe Singleton pattern.
**Expected Solution**:

```java
// Double-checked locking
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {
        if (instance != null) {
            throw new RuntimeException("Use getInstance()");
        }
    }
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Bill Pugh (Best approach)
public class Singleton {
    private Singleton() {}
    
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}

// Enum (Most robust)
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        // Business logic
    }
}
```

---

### Q9: Find top K frequent elements in an array.
**Expected Solution**:

```java
public int[] topKFrequent(int[] nums, int k) {
    // Count frequencies
    Map<Integer, Integer> freqMap = new HashMap<>();
    for (int num : nums) {
        freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
    }
    
    // Min heap of size k
    PriorityQueue<Map.Entry<Integer, Integer>> minHeap = 
        new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
    
    for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
        minHeap.offer(entry);
        if (minHeap.size() > k) {
            minHeap.poll();
        }
    }
    
    // Extract result
    int[] result = new int[k];
    for (int i = 0; i < k; i++) {
        result[i] = minHeap.poll().getKey();
    }
    return result;
}
```

**Time**: O(n log k), **Space**: O(n)

---

### Q10: Design and implement LRU Cache.
**Expected Solution**:

```java
class LRUCache {
    class Node {
        int key, value;
        Node prev, next;
    }
    
    private final int capacity;
    private final Map<Integer, Node> map;
    private final Node head, tail;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        head = new Node();
        tail = new Node();
        head.next = tail;
        tail.prev = head;
    }
    
    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        moveToHead(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        Node node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            node = new Node();
            node.key = key;
            node.value = value;
            map.put(key, node);
            addToHead(node);
            if (map.size() > capacity) {
                Node removed = removeTail();
                map.remove(removed.key);
            }
        }
    }
    
    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }
    
    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private Node removeTail() {
        Node node = tail.prev;
        removeNode(node);
        return node;
    }
}
```

**Time**: O(1) for get/put, **Space**: O(capacity)

---

### Q11: Implement a producer-consumer pattern.
**Expected Solution**:

```java
public class ProducerConsumer {
    private final BlockingQueue<Integer> queue;
    private final int capacity;
    
    public ProducerConsumer(int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }
    
    class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10; i++) {
                    queue.put(i);
                    System.out.println("Produced: " + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Integer item = queue.take();
                    System.out.println("Consumed: " + item);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer(5);
        
        Thread producer = new Thread(pc.new Producer());
        Thread consumer = new Thread(pc.new Consumer());
        
        producer.start();
        consumer.start();
    }
}
```

---

## 🏗️ Part 4: System Design (15 min)

### Q12: Design a URL shortener service.
**Expected Answer**:

**Requirements**:
- Shorten long URLs to short codes
- Redirect short URLs to original
- Track analytics (clicks, location)
- Handle 100M URLs, 1B redirects/day

**Architecture**:
```
Client -> Load Balancer -> App Servers
                              |
                    +---------+---------+
                    |                   |
                Redis Cache        PostgreSQL
                (Hot URLs)         (All URLs)
```

**Database Schema**:
```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE,
    long_url TEXT NOT NULL,
    user_id BIGINT,
    created_at TIMESTAMP,
    click_count BIGINT DEFAULT 0
);

CREATE INDEX idx_short_code ON urls(short_code);
```

**Implementation**:
```java
@Service
public class URLShortenerService {
    
    private static final String BASE62 = 
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    public String shortenURL(String longUrl) {
        long id = idGenerator.nextId();
        String shortCode = encodeBase62(id);
        
        urlRepository.save(new URL(shortCode, longUrl));
        redis.set(shortCode, longUrl, 24, TimeUnit.HOURS);
        
        return "https://short.ly/" + shortCode;
    }
    
    public String getLongURL(String shortCode) {
        String longUrl = redis.get(shortCode);
        if (longUrl != null) return longUrl;
        
        URL url = urlRepository.findByShortCode(shortCode);
        if (url != null) {
            redis.set(shortCode, url.getLongUrl(), 24, TimeUnit.HOURS);
            return url.getLongUrl();
        }
        throw new NotFoundException();
    }
    
    private String encodeBase62(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62.charAt((int)(num % 62)));
            num /= 62;
        }
        return sb.reverse().toString();
    }
}
```

**Scale Calculations**:
- Storage: 100M URLs × 500 bytes = 50GB
- QPS: 1B / 86400 = 11.5K reads/sec
- Cache: 20% hot URLs = 10GB Redis

---

### Q13: Design a notification system.
**Expected Answer**:

**Requirements**:
- Send notifications via Email, SMS, Push
- Handle 1M notifications/min
- Retry failed notifications
- User preferences (opt-in/out)

**Architecture**:
```
API -> Kafka -> Workers (Email/SMS/Push)
                  |
              Dead Letter Queue
```

**Implementation**:
```java
@Service
public class NotificationService {
    
    @Autowired
    private KafkaTemplate<String, Notification> kafka;
    
    public void sendNotification(Notification notification) {
        // Check user preferences
        if (!canSendNotification(notification)) {
            return;
        }
        
        // Publish to Kafka
        kafka.send("notifications", notification);
    }
}

@Service
public class EmailWorker {
    
    @KafkaListener(topics = "notifications")
    public void processNotification(Notification notification) {
        try {
            if (notification.getChannel() == Channel.EMAIL) {
                sendEmail(notification);
            }
        } catch (Exception e) {
            handleFailure(notification, e);
        }
    }
    
    private void handleFailure(Notification notification, Exception e) {
        if (notification.getRetryCount() < 3) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            kafka.send("notifications-retry", notification);
        } else {
            kafka.send("notifications-dlq", notification);
        }
    }
}
```

---

## 📨 Part 5: Kafka & Messaging (10 min)

### Q14: Explain Kafka architecture and use cases.
**Expected Answer**:

**Architecture**:
- **Topics**: Logical channels
- **Partitions**: Parallel processing
- **Brokers**: Kafka servers
- **Producers**: Write messages
- **Consumers**: Read messages
- **Consumer Groups**: Load balancing

**Producer Example**:
```java
@Service
public class OrderProducer {
    
    @Autowired
    private KafkaTemplate<String, Order> kafka;
    
    public void publishOrder(Order order) {
        kafka.send("orders", order.getId(), order);
    }
}
```

**Consumer Example**:
```java
@Service
public class OrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-service")
    public void consumeOrder(Order order) {
        processOrder(order);
    }
}
```

**Configuration**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: my-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

---

### Q15: How do you handle Kafka consumer failures?
**Expected Answer**:

```java
@Service
public class ResilientConsumer {
    
    @KafkaListener(topics = "orders")
    public void consume(ConsumerRecord<String, Order> record) {
        try {
            processOrder(record.value());
        } catch (Exception e) {
            handleFailure(record, e);
        }
    }
    
    private void handleFailure(ConsumerRecord<String, Order> record, Exception e) {
        int retryCount = getRetryCount(record);
        
        if (retryCount < 3) {
            // Send to retry topic
            kafka.send("orders-retry", record.value());
        } else {
            // Send to DLQ
            kafka.send("orders-dlq", record.value());
            alertService.sendAlert("Order processing failed", record.key());
        }
    }
}
```

---

## 🎭 Part 6: Behavioral & Experience (10 min)

### Q16: Tell me about a challenging bug you fixed.
**STAR Format Expected**:
```
Situation: Production issue causing 20% request failures
Task: Identify and fix the root cause
Action:
  1. Analyzed logs and metrics
  2. Found connection pool exhaustion
  3. Identified unclosed database connections
  4. Fixed resource leaks in code
  5. Added connection pool monitoring
Result:
  - Error rate dropped to 0%
  - Added automated tests
  - Documented best practices
```

---

### Q17: Describe a time you improved system performance.
**Example Answer**:
```
Problem: API response time 2-3 seconds

Actions:
1. Profiled application (JProfiler)
2. Found N+1 query problem
3. Added @EntityGraph for eager loading
4. Implemented Redis caching
5. Added database indexes

Results:
- Response time: 2s -> 200ms (10x improvement)
- Database load reduced 60%
- User satisfaction increased
```

---

### Q18: How do you stay updated with technology?
**Expected Answer**:
- Read tech blogs (Martin Fowler, Netflix Tech Blog)
- Follow industry leaders on Twitter
- Attend conferences/meetups
- Online courses (Udemy, Coursera)
- Side projects to experiment
- Contribute to open source

---

## 📊 Evaluation Criteria

### Technical Skills (50%)
- ✅ Strong Java and Spring Boot knowledge
- ✅ Problem-solving ability
- ✅ Code quality and best practices
- ✅ System design thinking
- ✅ Database and caching knowledge

### Experience (30%)
- ✅ Relevant project experience
- ✅ Production system ownership
- ✅ Performance optimization
- ✅ Troubleshooting skills
- ✅ Technology breadth

### Communication (20%)
- ✅ Clear explanation of concepts
- ✅ Asking clarifying questions
- ✅ Collaborative approach
- ✅ Learning mindset
- ✅ Cultural fit

---

## 🎯 Red Flags

❌ **Technical**:
- Cannot explain past projects
- Weak coding fundamentals
- No production experience
- Outdated knowledge

❌ **Behavioral**:
- Blames others
- Not curious
- Poor communication
- Rigid thinking

---

## 💡 Interview Tips

### For Candidates:
1. **Prepare examples** from past projects
2. **Practice coding** on whiteboard/online
3. **Review fundamentals** (data structures, algorithms)
4. **Ask questions** about role and team
5. **Be honest** about what you don't know

### For Interviewers:
1. **Start with easier questions** to build confidence
2. **Provide hints** if candidate is stuck
3. **Focus on thought process** not just solution
4. **Allow time for questions** from candidate
5. **Give feedback** on performance

---

## 📝 Sample Interview Flow (75 min)

**0-10 min**: Introduction, background, current role  
**10-30 min**: Java + Spring Boot (Q1-Q4)  
**30-50 min**: Coding challenges (Q8-Q10)  
**50-65 min**: System design (Q12 or Q13)  
**65-75 min**: Behavioral + Candidate questions

---

## 🎯 Decision Framework

### Strong Hire (Score: 8-10)
- Excellent technical skills
- Strong problem-solving
- Good communication
- Relevant experience
- Can contribute immediately

### Hire (Score: 6-7)
- Good technical skills
- Adequate problem-solving
- Some gaps but trainable
- Positive attitude

### Maybe (Score: 4-5)
- Adequate skills with gaps
- Needs more assessment
- Consider different level

### No Hire (Score: 1-3)
- Significant technical gaps
- Poor problem-solving
- Communication issues
- Not a good fit
