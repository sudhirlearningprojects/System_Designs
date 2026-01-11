# Senior Technical Lead Interview Questions (20+ Years Experience)

## 📋 Interview Structure (90-120 minutes)

1. **Introduction & Background** (10 min)
2. **Technical Deep Dive** (40 min)
3. **System Design & Architecture** (20 min)
4. **Leadership & Management** (15 min)
5. **Behavioral & Situational** (15 min)
6. **Q&A** (10 min)

---

## 🎯 Part 1: Java & Core Concepts (15 min)

### Q1: Explain Java Memory Model and how it affects concurrent programming.
**Expected Answer**:

**Java Memory Model (JMM)**:
- **Heap**: Shared memory for objects, accessible by all threads
- **Stack**: Thread-local memory for method calls and local variables
- **Happens-before**: Guarantees visibility of writes across threads
  - Program order rule
  - Monitor lock rule (synchronized)
  - Volatile variable rule
  - Thread start/join rules

**Concurrency Issues**:
```java
// Memory visibility problem
class VisibilityExample {
    private boolean flag = false;  // Not visible across threads
    
    public void writer() {
        flag = true;  // May not be visible to reader
    }
    
    public void reader() {
        while (!flag) { }  // May loop forever
    }
}

// Solution: Use volatile
class FixedExample {
    private volatile boolean flag = false;  // Guarantees visibility
}
```

**False Sharing**:
- Multiple threads modify variables in same cache line
- Solution: @Contended annotation or padding

**Follow-up Answer**: Debug memory leak in production:
1. Enable heap dump on OOM: `-XX:+HeapDumpOnOutOfMemoryError`
2. Use jmap to capture heap dump: `jmap -dump:live,format=b,file=heap.bin <pid>`
3. Analyze with Eclipse MAT or VisualVM
4. Look for: Large collections, static references, ThreadLocal leaks, unclosed resources
5. Monitor with JMX metrics and APM tools

---

### Q2: What are the key differences between Java 8, 11, 17, and 21?
**Expected Answer**:

**Java 8 (LTS - 2014)**:
```java
// Lambdas and Streams
List<String> names = users.stream()
    .filter(u -> u.getAge() > 18)
    .map(User::getName)
    .collect(Collectors.toList());

// Optional
Optional<User> user = findUser(id);
user.ifPresent(u -> System.out.println(u.getName()));

// CompletableFuture
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .thenAccept(result -> save(result));
```

**Java 11 (LTS - 2018)**:
```java
// HTTP Client
HttpClient client = HttpClient.newHttpClient();
HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

// var keyword
var list = new ArrayList<String>();

// String methods
String.isBlank(), lines(), strip(), repeat()
```

**Java 17 (LTS - 2021)**:
```java
// Sealed classes
sealed interface Shape permits Circle, Rectangle { }
final class Circle implements Shape { }

// Records
record Point(int x, int y) { }

// Pattern matching for instanceof
if (obj instanceof String s) {
    System.out.println(s.length());
}
```

**Java 21 (LTS - 2023)**:
```java
// Virtual threads
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> blockingIO());

// Pattern matching for switch
String result = switch (obj) {
    case Integer i -> "Integer: " + i;
    case String s -> "String: " + s;
    default -> "Unknown";
};

// Sequenced Collections
List<String> list = new ArrayList<>();
list.addFirst("first");
list.addLast("last");
```

**Follow-up Answer**: Choose virtual threads when:
- High number of concurrent blocking I/O operations (10K+ threads)
- Thread-per-request model (web servers, microservices)
- Simplifies async code (no callbacks/reactive)
- NOT for CPU-intensive tasks (use platform threads)

---

### Q3: Explain the internal working of HashMap and ConcurrentHashMap.
**Expected Answer**:

**HashMap Internal Structure**:
```java
// Simplified internal structure
class HashMap<K,V> {
    Node<K,V>[] table;  // Array of buckets
    int size;
    int threshold;  // capacity * loadFactor
    float loadFactor = 0.75f;
    
    static class Node<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;  // Linked list for collisions
    }
}
```

**Hash Function**:
```java
static final int hash(Object key) {
    int h = key.hashCode();
    return h ^ (h >>> 16);  // XOR higher bits with lower bits
}

int index = (n - 1) & hash;  // n is table length (power of 2)
```

**Collision Handling**:
1. **Linked List** (< 8 elements): O(n) lookup
2. **Red-Black Tree** (≥ 8 elements): O(log n) lookup
3. Converts back to list when < 6 elements

**Rehashing**:
- Triggered when size > threshold (capacity * 0.75)
- Doubles capacity
- Redistributes all entries

**ConcurrentHashMap**:
```java
// Java 8+ uses CAS + synchronized
class ConcurrentHashMap<K,V> {
    Node<K,V>[] table;
    
    // No segment locking, uses:
    // 1. CAS for updates
    // 2. synchronized on first node of bucket
    // 3. Volatile reads for visibility
    
    public V put(K key, V value) {
        int hash = spread(key.hashCode());
        for (Node<K,V>[] tab = table;;) {
            if (tab == null)
                tab = initTable();  // CAS initialization
            else if ((f = tabAt(tab, i)) == null) {
                if (casTabAt(tab, i, null, new Node(hash, key, value)))
                    break;  // CAS insert
            }
            else {
                synchronized (f) {  // Lock only this bucket
                    // Insert into linked list or tree
                }
            }
        }
    }
}
```

**Performance**:
- Average: O(1) for get/put
- Worst case: O(log n) with tree-ification
- ConcurrentHashMap: Lock-free reads, fine-grained locking for writes

**Follow-up Answer**: Thread-safe LRU Cache:
```java
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head, tail;
    private final ReentrantLock lock = new ReentrantLock();
    
    static class Node<K, V> {
        K key; V value;
        Node<K, V> prev, next;
    }
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        head = new Node<>();
        tail = new Node<>();
        head.next = tail;
        tail.prev = head;
    }
    
    public V get(K key) {
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) return null;
            moveToHead(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }
    
    public void put(K key, V value) {
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node != null) {
                node.value = value;
                moveToHead(node);
            } else {
                node = new Node<>();
                node.key = key;
                node.value = value;
                map.put(key, node);
                addToHead(node);
                if (map.size() > capacity) {
                    Node<K, V> removed = removeTail();
                    map.remove(removed.key);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }
    
    private void addToHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private Node<K, V> removeTail() {
        Node<K, V> node = tail.prev;
        removeNode(node);
        return node;
    }
}
```

---

### Q4: Coding Challenge - Design a Rate Limiter
**Problem**: Implement a thread-safe rate limiter that allows N requests per time window.

```java
public interface RateLimiter {
    boolean allowRequest(String userId);
}
```
**Expected Solution**: Sliding Window Algorithm

```java
public class SlidingWindowRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowMs;
    private final ConcurrentHashMap<String, Queue<Long>> userRequests;
    
    public SlidingWindowRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.userRequests = new ConcurrentHashMap<>();
    }
    
    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        Queue<Long> requests = userRequests.computeIfAbsent(
            userId, k -> new ConcurrentLinkedQueue<>());
        
        // Remove expired requests
        while (!requests.isEmpty() && 
               now - requests.peek() > windowMs) {
            requests.poll();
        }
        
        // Check if under limit
        if (requests.size() < maxRequests) {
            requests.offer(now);
            return true;
        }
        
        return false;
    }
}

// Alternative: Token Bucket Algorithm
public class TokenBucketRateLimiter implements RateLimiter {
    private final int capacity;
    private final double refillRate;  // tokens per second
    private final ConcurrentHashMap<String, Bucket> buckets;
    
    static class Bucket {
        double tokens;
        long lastRefill;
        
        Bucket(int capacity) {
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }
    }
    
    public TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }
    
    @Override
    public boolean allowRequest(String userId) {
        Bucket bucket = buckets.computeIfAbsent(
            userId, k -> new Bucket(capacity));
        
        synchronized (bucket) {
            long now = System.nanoTime();
            double elapsed = (now - bucket.lastRefill) / 1_000_000_000.0;
            
            // Refill tokens
            bucket.tokens = Math.min(capacity, 
                bucket.tokens + elapsed * refillRate);
            bucket.lastRefill = now;
            
            // Consume token
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            
            return false;
        }
    }
}
```

---

## 🚀 Part 2: Spring Boot & Microservices (15 min)

### Q5: Explain Spring Boot auto-configuration mechanism.
**Expected Answer**:

**How Auto-Configuration Works**:
```java
// 1. @SpringBootApplication enables auto-configuration
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 2. @EnableAutoConfiguration triggers auto-configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
}

// 3. spring.factories lists auto-configuration classes
// META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

**Conditional Annotations**:
```java
@Configuration
@ConditionalOnClass(DataSource.class)  // Only if class exists
@ConditionalOnMissingBean(DataSource.class)  // Only if bean not defined
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url")
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create()
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}
```

**Custom Auto-Configuration**:
```java
// 1. Create auto-configuration class
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyServiceProperties properties) {
        return new MyService(properties);
    }
}

// 2. Create properties class
@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {
    private String apiKey;
    private int timeout = 5000;
    // getters/setters
}

// 3. Register in META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyServiceAutoConfiguration
```

**Debugging Auto-Configuration**:
```bash
# Enable debug logging
java -jar app.jar --debug

# Or in application.properties
debug=true

# Shows:
# - Positive matches (auto-configurations applied)
# - Negative matches (why not applied)
# - Exclusions
# - Unconditional classes
```

**Follow-up Answer**: Handle circular dependencies:
```java
// Problem: A depends on B, B depends on A
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) {  // Circular!
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {  // Circular!
        this.serviceA = serviceA;
    }
}

// Solution 1: Use @Lazy
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

// Solution 2: Use setter injection
@Service
public class ServiceA {
    private ServiceB serviceB;
    
    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

// Solution 3: Redesign (best approach)
// Extract common logic to a third service
@Service
public class CommonService {
    // Shared logic
}

@Service
public class ServiceA {
    private final CommonService commonService;
}

@Service
public class ServiceB {
    private final CommonService commonService;
}
```

---

### Q6: What are the different transaction propagation levels in Spring?
**Expected Answer**:

**Transaction Propagation Levels**:

```java
public enum Propagation {
    REQUIRED,      // Join existing or create new (default)
    REQUIRES_NEW,  // Always create new, suspend existing
    NESTED,        // Nested within existing, rollback to savepoint
    SUPPORTS,      // Join if exists, non-transactional otherwise
    NOT_SUPPORTED, // Execute non-transactionally, suspend existing
    NEVER,         // Execute non-transactionally, throw exception if exists
    MANDATORY      // Must have existing transaction, throw exception otherwise
}
```

**Use Cases with Examples**:

```java
@Service
public class OrderService {
    
    // REQUIRED (default) - Most common
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(Order order) {
        orderRepository.save(order);
        paymentService.processPayment(order);  // Joins same transaction
        // If payment fails, order is also rolled back
    }
    
    // REQUIRES_NEW - Independent transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(String action) {
        auditRepository.save(new AuditLog(action));
        // Always commits, even if outer transaction fails
    }
    
    // NESTED - Savepoint rollback
    @Transactional(propagation = Propagation.NESTED)
    public void sendNotification(Order order) {
        notificationRepository.save(notification);
        // Can rollback to savepoint without affecting outer transaction
    }
    
    // MANDATORY - Must have transaction
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateInventory(Long productId, int quantity) {
        // Throws exception if called without transaction
        inventoryRepository.updateQuantity(productId, quantity);
    }
}
```

**@Transactional Pitfalls**:

```java
// Pitfall 1: Self-invocation doesn't work
@Service
public class UserService {
    
    public void registerUser(User user) {
        saveUser(user);  // Transaction NOT applied (self-invocation)
    }
    
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
    
    // Solution: Inject self or use separate service
    @Autowired
    private UserService self;
    
    public void registerUser(User user) {
        self.saveUser(user);  // Works!
    }
}

// Pitfall 2: Checked exceptions don't rollback by default
@Transactional  // Only rolls back on RuntimeException
public void processOrder(Order order) throws Exception {
    orderRepository.save(order);
    throw new Exception("Error");  // Transaction COMMITS!
}

// Solution: Specify rollbackFor
@Transactional(rollbackFor = Exception.class)
public void processOrder(Order order) throws Exception {
    orderRepository.save(order);
    throw new Exception("Error");  // Transaction ROLLS BACK
}

// Pitfall 3: Transaction on private methods
@Transactional  // Doesn't work on private methods
private void saveUser(User user) {
    userRepository.save(user);
}
```

**Follow-up Answer**: Distributed transactions across microservices:

```java
// Saga Pattern - Choreography
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        // 1. Create order
        orderRepository.save(order);
        
        // 2. Publish event
        eventPublisher.publish(new OrderCreatedEvent(order));
    }
    
    @EventListener
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Compensating transaction
        Order order = orderRepository.findById(event.getOrderId());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}

// Saga Pattern - Orchestration
@Service
public class OrderSagaOrchestrator {
    
    public void executeOrderSaga(Order order) {
        try {
            // Step 1: Create order
            orderService.createOrder(order);
            
            // Step 2: Reserve inventory
            inventoryService.reserveInventory(order);
            
            // Step 3: Process payment
            paymentService.processPayment(order);
            
            // Step 4: Confirm order
            orderService.confirmOrder(order);
            
        } catch (Exception e) {
            // Compensate in reverse order
            paymentService.refundPayment(order);
            inventoryService.releaseInventory(order);
            orderService.cancelOrder(order);
        }
    }
}

// Two-Phase Commit (2PC) - Not recommended for microservices
// Use only when strong consistency is absolutely required
@Service
public class DistributedTransactionService {
    
    @Transactional
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        // Phase 1: Prepare
        boolean prepared1 = accountService1.prepare(fromAccount, amount);
        boolean prepared2 = accountService2.prepare(toAccount, amount);
        
        if (prepared1 && prepared2) {
            // Phase 2: Commit
            accountService1.commit(fromAccount);
            accountService2.commit(toAccount);
        } else {
            // Rollback
            accountService1.rollback(fromAccount);
            accountService2.rollback(toAccount);
        }
    }
}
```

---

### Q7: Explain Spring WebFlux and when to use it over Spring MVC.
**Expected Answer**:

**Spring WebFlux Architecture**:
```java
// Traditional Spring MVC (blocking)
@RestController
public class UserController {
    @Autowired
    private UserService userService;
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // Blocks thread
    }
}

// Spring WebFlux (non-blocking)
@RestController
public class UserController {
    @Autowired
    private UserService userService;
    
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.findById(id);  // Non-blocking
    }
    
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        return userService.findAll();  // Stream of users
    }
}
```

**Reactive Types**:
```java
// Mono - 0 or 1 element
Mono<User> user = userRepository.findById(id)
    .map(u -> u.toUpperCase())
    .filter(u -> u.isActive())
    .defaultIfEmpty(new User());

// Flux - 0 to N elements
Flux<User> users = userRepository.findAll()
    .filter(u -> u.getAge() > 18)
    .map(User::getName)
    .take(10);
```

**When to Use WebFlux**:
- ✅ High concurrency (10K+ concurrent connections)
- ✅ Streaming data (SSE, WebSocket)
- ✅ Microservices with many I/O calls
- ✅ Backpressure handling needed
- ❌ CPU-intensive operations
- ❌ Blocking JDBC (use R2DBC instead)
- ❌ Team unfamiliar with reactive

**Follow-up Answer**: Error handling in reactive streams:
```java
@Service
public class UserService {
    
    public Mono<User> getUser(Long id) {
        return userRepository.findById(id)
            // Handle specific error
            .onErrorResume(DatabaseException.class, e -> 
                Mono.just(new User()))
            // Retry with backoff
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            // Fallback value
            .defaultIfEmpty(new User())
            // Log error
            .doOnError(e -> log.error("Error fetching user", e));
    }
    
    // Global error handler
    @ControllerAdvice
    public class GlobalErrorHandler {
        @ExceptionHandler(UserNotFoundException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleNotFound(UserNotFoundException ex) {
            return Mono.just(ResponseEntity.status(404)
                .body(new ErrorResponse(ex.getMessage())));
        }
    }
}
```

---

### Q8: How do you implement circuit breaker pattern in microservices?
**Expected Answer**:

**Circuit Breaker States**:
```
CLOSED → (failures exceed threshold) → OPEN
OPEN → (timeout expires) → HALF_OPEN
HALF_OPEN → (success) → CLOSED
HALF_OPEN → (failure) → OPEN
```

**Implementation with Resilience4j**:
```java
@Service
public class PaymentService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    // Circuit Breaker configuration
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject(
            "http://payment-api/process", 
            request, 
            PaymentResponse.class
        );
    }
    
    // Fallback method
    public PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        log.error("Payment failed, using fallback", ex);
        return new PaymentResponse("PENDING", "Will retry later");
    }
}
```

**Configuration (application.yml)**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 2s
        
  retry:
    instances:
      paymentService:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          
  bulkhead:
    instances:
      paymentService:
        maxConcurrentCalls: 10
        maxWaitDuration: 1s
```

**Programmatic Configuration**:
```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();
            
        return CircuitBreakerRegistry.of(config);
    }
}
```

**Follow-up Answer**: Monitor circuit breaker state changes:
```java
@Component
public class CircuitBreakerMonitor {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            // Register metrics
            TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry)
                .bindTo(meterRegistry);
            
            // Listen to state transitions
            cb.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit Breaker {} transitioned from {} to {}",
                        cb.getName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState());
                    
                    // Send alert
                    if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
                        alertService.sendAlert(
                            "Circuit Breaker OPEN: " + cb.getName(),
                            AlertLevel.CRITICAL
                        );
                    }
                });
            
            // Listen to errors
            cb.getEventPublisher()
                .onError(event -> {
                    log.error("Circuit Breaker {} error", cb.getName(), event.getThrowable());
                });
        });
    }
}

// Prometheus metrics
circuitbreaker_state{name="paymentService",state="closed"} 1
circuitbreaker_calls_total{name="paymentService",kind="successful"} 100
circuitbreaker_calls_total{name="paymentService",kind="failed"} 5
```

---

## 📨 Part 3: Apache Kafka (10 min)

### Q9: Explain Kafka's architecture and how it achieves high throughput.
**Expected Answer**:

**Kafka Architecture**:
```
┌─────────────────────────────────────────────────────────┐
│                    Kafka Cluster                        │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Broker 1 │  │ Broker 2 │  │ Broker 3 │             │
│  │          │  │          │  │          │             │
│  │ Topic A  │  │ Topic A  │  │ Topic A  │             │
│  │ Part 0   │  │ Part 1   │  │ Part 2   │             │
│  │ (Leader) │  │ (Replica)│  │ (Replica)│             │
│  └──────────┘  └──────────┘  └──────────┘             │
└─────────────────────────────────────────────────────────┘
```

**Key Components**:
- **Topics**: Logical channels for messages
- **Partitions**: Parallel processing units (ordered within partition)
- **Replicas**: Fault tolerance (leader + followers)
- **Brokers**: Kafka servers storing data
- **ZooKeeper/KRaft**: Cluster coordination

**High Throughput Techniques**:

```java
// 1. Producer Batching
Properties props = new Properties();
props.put("batch.size", 16384);  // 16KB batches
props.put("linger.ms", 10);      // Wait 10ms to batch
props.put("compression.type", "snappy");  // Compress batches
props.put("buffer.memory", 33554432);     // 32MB buffer

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// 2. Async send with callback
producer.send(new ProducerRecord<>("topic", key, value), 
    (metadata, exception) -> {
        if (exception != null) {
            log.error("Send failed", exception);
        }
    });

// 3. Acks configuration
props.put("acks", "1");  // Leader ack only (fast)
// acks=0: No ack (fastest, may lose data)
// acks=1: Leader ack (balanced)
// acks=all: All replicas ack (slowest, most reliable)
```

**Zero-Copy Optimization**:
```java
// Traditional copy: 4 context switches, 4 data copies
// File → Kernel buffer → User buffer → Socket buffer → NIC

// Zero-copy: 2 context switches, 2 data copies
// File → Kernel buffer → NIC (using sendfile() system call)
// Kafka uses this for consumer reads
```

**Sequential Disk I/O**:
- Kafka writes sequentially to disk (append-only log)
- Sequential writes: ~600 MB/s
- Random writes: ~100 KB/s
- OS page cache optimization

**Follow-up Answer**: Exactly-once semantics:
```java
// Enable idempotent producer
props.put("enable.idempotence", true);
props.put("acks", "all");
props.put("retries", Integer.MAX_VALUE);
props.put("max.in.flight.requests.per.connection", 5);

// Transactional producer
props.put("transactional.id", "my-transactional-id");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);

producer.initTransactions();
try {
    producer.beginTransaction();
    producer.send(new ProducerRecord<>("topic1", "key", "value1"));
    producer.send(new ProducerRecord<>("topic2", "key", "value2"));
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}

// Consumer with exactly-once
props.put("isolation.level", "read_committed");
props.put("enable.auto.commit", false);

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);  // Idempotent processing
    }
    consumer.commitSync();  // Manual commit after processing
}
```

---

### Q10: What are the different consumer offset commit strategies?
**Expected Answer**:

**Offset Commit Strategies**:

```java
// 1. Auto-commit (at-most-once)
props.put("enable.auto.commit", true);
props.put("auto.commit.interval.ms", 5000);
// Risk: May lose messages if consumer crashes after commit but before processing

// 2. Manual commit after processing (at-least-once)
props.put("enable.auto.commit", false);
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);  // Process first
    }
    consumer.commitSync();  // Then commit
    // Risk: May process duplicates if crash after processing but before commit
}

// 3. Commit per batch (performance optimization)
int count = 0;
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);
        count++;
        if (count % 100 == 0) {
            consumer.commitAsync();  // Async for better performance
        }
    }
}

// 4. Exactly-once with transactions
props.put("isolation.level", "read_committed");
props.put("enable.auto.commit", false);

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    producer.beginTransaction();
    try {
        for (ConsumerRecord<String, String> record : records) {
            // Process and produce to output topic
            String result = processRecord(record);
            producer.send(new ProducerRecord<>("output-topic", result));
        }
        
        // Commit offsets as part of transaction
        producer.sendOffsetsToTransaction(
            getOffsets(records), 
            consumer.groupMetadata()
        );
        producer.commitTransaction();
    } catch (Exception e) {
        producer.abortTransaction();
    }
}
```

**Delivery Semantics**:
- **At-most-once**: Commit before processing (may lose messages)
- **At-least-once**: Commit after processing (may duplicate)
- **Exactly-once**: Idempotent processing + transactions

**Idempotent Consumer Pattern**:
```java
@Service
public class IdempotentConsumer {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    public void processMessage(ConsumerRecord<String, String> record) {
        String messageId = record.key();
        
        // Check if already processed
        if (redis.opsForValue().setIfAbsent(messageId, "processed", 24, TimeUnit.HOURS)) {
            // First time seeing this message
            doActualProcessing(record.value());
        } else {
            log.info("Duplicate message ignored: {}", messageId);
        }
    }
}
```

**Follow-up Answer**: Handle poison pill messages:
```java
@Service
public class KafkaConsumerService {
    
    private static final int MAX_RETRIES = 3;
    
    @KafkaListener(topics = "orders")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            processMessage(record);
        } catch (Exception e) {
            handleFailure(record, e);
        }
    }
    
    private void handleFailure(ConsumerRecord<String, String> record, Exception e) {
        int retryCount = getRetryCount(record);
        
        if (retryCount < MAX_RETRIES) {
            // Send to retry topic with delay
            sendToRetryTopic(record, retryCount + 1);
        } else {
            // Send to dead letter queue
            sendToDLQ(record, e);
            log.error("Message sent to DLQ after {} retries", MAX_RETRIES, e);
            
            // Alert monitoring
            alertService.sendAlert("Poison pill detected", record.key());
        }
    }
    
    private void sendToRetryTopic(ConsumerRecord<String, String> record, int retryCount) {
        ProducerRecord<String, String> retryRecord = new ProducerRecord<>(
            "orders-retry",
            record.key(),
            record.value()
        );
        retryRecord.headers().add("retry-count", String.valueOf(retryCount).getBytes());
        retryRecord.headers().add("original-topic", record.topic().getBytes());
        
        kafkaTemplate.send(retryRecord);
    }
    
    private void sendToDLQ(ConsumerRecord<String, String> record, Exception e) {
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
            "orders-dlq",
            record.key(),
            record.value()
        );
        dlqRecord.headers().add("error-message", e.getMessage().getBytes());
        dlqRecord.headers().add("original-topic", record.topic().getBytes());
        dlqRecord.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        
        kafkaTemplate.send(dlqRecord);
    }
}
```

---

### Q11: How do you design Kafka topics for a high-scale system?
**Expected Answer**:

**Partition Count Considerations**:
```java
// Formula: partitions = max(throughput/producer_throughput, throughput/consumer_throughput)
// Example: 100 MB/s throughput, 10 MB/s per producer, 20 MB/s per consumer
// partitions = max(100/10, 100/20) = max(10, 5) = 10 partitions

// Create topic with partitions
bin/kafka-topics.sh --create \
  --topic orders \
  --partitions 12 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config segment.ms=86400000
```

**Partition Guidelines**:
- Start with: `partitions = (target_throughput_MB/s) / (producer_throughput_MB/s)`
- Max partitions per broker: ~4000
- More partitions = more parallelism but higher overhead
- Consider consumer group size (1 consumer per partition max)

**Replication Factor**:
```properties
# Production: min 3 replicas
replication.factor=3
min.insync.replicas=2  # At least 2 replicas must ack

# Critical data: 5 replicas across regions
replication.factor=5
min.insync.replicas=3
```

**Retention Policies**:
```properties
# Time-based retention (7 days)
retention.ms=604800000

# Size-based retention (100 GB per partition)
retention.bytes=107374182400

# Compacted topics (keep latest value per key)
cleanup.policy=compact
min.cleanable.dirty.ratio=0.5
```

**Key Selection for Partitioning**:
```java
// 1. User-based partitioning (all user events in same partition)
ProducerRecord<String, String> record = new ProducerRecord<>(
    "user-events",
    userId,  // Key determines partition
    event
);

// 2. Custom partitioner
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // VIP users go to partition 0 (dedicated consumer)
        if (isVIPUser(key)) {
            return 0;
        }
        
        // Regular users distributed across remaining partitions
        return (Math.abs(key.hashCode()) % (numPartitions - 1)) + 1;
    }
}

// 3. Null key = round-robin distribution
ProducerRecord<String, String> record = new ProducerRecord<>(
    "logs",
    null,  // Round-robin across partitions
    logMessage
);
```

**Topic Naming Convention**:
```
<environment>.<team>.<domain>.<entity>.<version>
Examples:
prod.payments.orders.created.v1
prod.payments.orders.updated.v1
prod.inventory.stock.changed.v1
```

**Follow-up Answer**: Schema evolution:
```java
// Use Avro with Schema Registry
@Configuration
public class KafkaAvroConfig {
    
    @Bean
    public ProducerFactory<String, GenericRecord> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        return new DefaultKafkaProducerFactory<>(props);
    }
}

// Schema evolution example
// V1 Schema
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "name", "type": "string"}
  ]
}

// V2 Schema (backward compatible - added optional field)
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null}  // Optional
  ]
}

// Compatibility modes in Schema Registry
// BACKWARD: New schema can read old data (add optional fields)
// FORWARD: Old schema can read new data (remove fields)
// FULL: Both backward and forward compatible
// NONE: No compatibility checks
```

---

## ☁️ Part 4: AWS/Azure Cloud (10 min)

### Q12: Design a highly available and scalable architecture on AWS.
**Expected Answer**:

**Architecture Diagram**:
```
Route 53 (DNS)
    |
    v
CloudFront (CDN)
    |
    v
ALB (Multi-AZ)
    |
    +-- EC2 Auto Scaling Group (AZ-1)
    +-- EC2 Auto Scaling Group (AZ-2)
    +-- EC2 Auto Scaling Group (AZ-3)
         |
         v
    ElastiCache (Redis)
         |
         v
    RDS Multi-AZ (Primary + Standby)
         |
         +-- Read Replicas (3)
```

**Implementation**:
```yaml
# CloudFormation/Terraform example
Resources:
  # VPC with 3 AZs
  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: 10.0.0.0/16
      EnableDnsHostnames: true
  
  # Application Load Balancer
  ALB:
    Type: AWS::ElasticLoadBalancingV2::LoadBalancer
    Properties:
      Subnets:
        - !Ref PublicSubnet1
        - !Ref PublicSubnet2
        - !Ref PublicSubnet3
      SecurityGroups:
        - !Ref ALBSecurityGroup
  
  # Auto Scaling Group
  AutoScalingGroup:
    Type: AWS::AutoScaling::AutoScalingGroup
    Properties:
      MinSize: 3
      MaxSize: 10
      DesiredCapacity: 6
      HealthCheckType: ELB
      HealthCheckGracePeriod: 300
      TargetGroupARNs:
        - !Ref TargetGroup
      VPCZoneIdentifier:
        - !Ref PrivateSubnet1
        - !Ref PrivateSubnet2
        - !Ref PrivateSubnet3
  
  # RDS Multi-AZ
  Database:
    Type: AWS::RDS::DBInstance
    Properties:
      Engine: postgres
      MultiAZ: true
      AllocatedStorage: 100
      DBInstanceClass: db.r5.xlarge
      BackupRetentionPeriod: 7
```

**Follow-up Answer**: Disaster recovery across regions:
```yaml
# Active-Passive DR Strategy
Primary Region (us-east-1):
  - Full production stack
  - RDS with automated backups
  - S3 with cross-region replication
  
DR Region (us-west-2):
  - Minimal infrastructure (warm standby)
  - RDS read replica (can be promoted)
  - S3 replica bucket
  - Route 53 health checks

# Route 53 Failover
Route53:
  PrimaryRecord:
    Type: A
    SetIdentifier: Primary
    Failover: PRIMARY
    HealthCheckId: !Ref HealthCheck
    AliasTarget: !Ref PrimaryALB
  
  SecondaryRecord:
    Type: A
    SetIdentifier: Secondary
    Failover: SECONDARY
    AliasTarget: !Ref DRALB

# RTO: 15 minutes, RPO: 5 minutes
```

---

### Q13: Explain different AWS compute options and when to use each.
**Expected Answer**:

| Service | Use Case | Pros | Cons |
|---------|----------|------|------|
| **EC2** | Long-running apps, custom OS | Full control, any workload | Manual scaling, patching |
| **Lambda** | Event-driven, short tasks | Auto-scale, pay-per-use | 15min limit, cold starts |
| **ECS** | Docker containers | AWS-native, simple | Vendor lock-in |
| **EKS** | Kubernetes workloads | Portable, ecosystem | Complex, expensive |
| **Fargate** | Serverless containers | No server management | Less control, higher cost |
| **Batch** | Batch jobs, HPC | Optimized for batch | Not for real-time |

**Decision Tree**:
```
Need containers?
  Yes -> Need Kubernetes?
    Yes -> EKS
    No -> Want to manage servers?
      Yes -> ECS on EC2
      No -> ECS on Fargate
  No -> Event-driven?
    Yes -> Lambda
    No -> Long-running?
      Yes -> EC2
      No -> Batch
```

**Follow-up Answer**: Cost optimization:
```yaml
# 1. Right-sizing with AWS Compute Optimizer
# 2. Reserved Instances (1-3 year commitment)
Savings: 30-70% vs On-Demand

# 3. Spot Instances (up to 90% discount)
AutoScalingGroup:
  MixedInstancesPolicy:
    InstancesDistribution:
      OnDemandBaseCapacity: 2
      OnDemandPercentageAboveBaseCapacity: 20
      SpotAllocationStrategy: capacity-optimized

# 4. Auto Scaling policies
TargetTrackingScaling:
  TargetValue: 70  # CPU utilization
  ScaleInCooldown: 300
  ScaleOutCooldown: 60

# 5. Lambda optimization
- Right-size memory (affects CPU)
- Use provisioned concurrency for critical paths
- Minimize cold starts (keep functions warm)

# 6. S3 Intelligent-Tiering
# 7. CloudFront caching
# 8. RDS Reserved Instances
# 9. Delete unused resources (AWS Trusted Advisor)
```

---

### Q14: How do you implement security in AWS?
**Expected Answer**:

**Security Layers**:
```yaml
# 1. IAM - Least Privilege
UserPolicy:
  Effect: Allow
  Action:
    - s3:GetObject
    - s3:PutObject
  Resource: arn:aws:s3:::my-bucket/*
  Condition:
    IpAddress:
      aws:SourceIp: 10.0.0.0/8

# 2. VPC Security
VPC:
  PublicSubnet: Web tier (ALB)
  PrivateSubnet: App tier (EC2)
  IsolatedSubnet: Data tier (RDS)
  
SecurityGroup:
  Ingress:
    - Port: 443, Source: 0.0.0.0/0
    - Port: 80, Source: 0.0.0.0/0
  Egress:
    - Port: 5432, Destination: DB-SG

NACL:  # Stateless firewall
  Inbound:
    - Rule: 100, Allow, TCP, 443
  Outbound:
    - Rule: 100, Allow, TCP, 1024-65535

# 3. Encryption
RDS:
  StorageEncrypted: true
  KmsKeyId: !Ref KMSKey
  
S3:
  BucketEncryption:
    ServerSideEncryptionConfiguration:
      - ServerSideEncryptionByDefault:
          SSEAlgorithm: aws:kms
          KMSMasterKeyID: !Ref KMSKey

# 4. Secrets Management
Secrets:
  DBPassword:
    Type: AWS::SecretsManager::Secret
    Properties:
      GenerateSecretString:
        PasswordLength: 32
        ExcludeCharacters: '"@/\'

# 5. Monitoring
CloudTrail: All API calls logged
GuardDuty: Threat detection
Config: Compliance monitoring
SecurityHub: Centralized security
```

**Follow-up Answer**: Compliance (PCI-DSS, HIPAA):
```yaml
PCI-DSS Requirements:
  1. Network Segmentation:
     - Separate VPC for cardholder data
     - Private subnets only
     - No direct internet access
  
  2. Encryption:
     - TLS 1.2+ for data in transit
     - AES-256 for data at rest
     - KMS for key management
  
  3. Access Control:
     - MFA for all users
     - Role-based access (RBAC)
     - Audit logs (CloudTrail)
  
  4. Monitoring:
     - Real-time alerts (CloudWatch)
     - Log aggregation (CloudWatch Logs)
     - Intrusion detection (GuardDuty)
  
  5. Vulnerability Management:
     - Regular patching (Systems Manager)
     - Security scanning (Inspector)
     - Penetration testing

HIPAA Requirements:
  - BAA with AWS
  - Encryption at rest and in transit
  - Audit controls (CloudTrail)
  - Access controls (IAM)
  - Backup and disaster recovery
  - Use HIPAA-eligible services only
```

---

## 🏗️ Part 5: System Design & Architecture (20 min)

### Q15: Design a URL shortener like TinyURL (bit.ly)
**Expected Answer**:

**System Design**:
```
Client -> API Gateway -> Load Balancer
                            |
                            v
                    App Servers (Stateless)
                            |
                    +-------+-------+
                    |               |
                    v               v
                Redis Cache    PostgreSQL
                (Hot URLs)     (All URLs)
```

**Database Schema**:
```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    click_count BIGINT DEFAULT 0,
    INDEX idx_short_code (short_code),
    INDEX idx_user_id (user_id)
);

CREATE TABLE analytics (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10),
    clicked_at TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    referrer TEXT,
    country VARCHAR(2)
);
```

**Implementation**:
```java
@Service
public class URLShortenerService {
    
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    // Shorten URL
    public String shortenURL(String longUrl) {
        // Generate unique ID
        long id = idGenerator.nextId();  // Snowflake ID
        
        // Encode to Base62
        String shortCode = encodeBase62(id);
        
        // Save to DB
        urlRepository.save(new URL(shortCode, longUrl));
        
        // Cache hot URLs
        redis.set(shortCode, longUrl, 24, TimeUnit.HOURS);
        
        return "https://short.ly/" + shortCode;
    }
    
    // Redirect
    public String getLongURL(String shortCode) {
        // Check cache first
        String longUrl = redis.get(shortCode);
        if (longUrl != null) {
            return longUrl;
        }
        
        // Fallback to DB
        URL url = urlRepository.findByShortCode(shortCode);
        if (url != null) {
            // Update cache
            redis.set(shortCode, url.getLongUrl(), 24, TimeUnit.HOURS);
            
            // Async analytics
            analyticsService.trackClick(shortCode);
            
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

**Scale**: 100M URLs/day, 10B redirects/day
- **Storage**: 100M * 365 * 5 years * 500 bytes = 9TB
- **QPS**: 10B / 86400 = 115K reads/sec
- **Cache**: 20% hot URLs = 2TB Redis cluster

**Follow-up Answer**: Hot keys and cache stampede:
```java
// 1. Cache stampede prevention
public String getLongURL(String shortCode) {
    String longUrl = redis.get(shortCode);
    if (longUrl != null) return longUrl;
    
    // Distributed lock to prevent stampede
    String lockKey = "lock:" + shortCode;
    if (redis.setNX(lockKey, "1", 5, TimeUnit.SECONDS)) {
        try {
            // Only one thread fetches from DB
            URL url = urlRepository.findByShortCode(shortCode);
            if (url != null) {
                redis.set(shortCode, url.getLongUrl(), 24, TimeUnit.HOURS);
                return url.getLongUrl();
            }
        } finally {
            redis.delete(lockKey);
        }
    } else {
        // Wait and retry
        Thread.sleep(100);
        return getLongURL(shortCode);
    }
}

// 2. Hot key detection and mitigation
@Scheduled(fixedRate = 60000)
public void detectHotKeys() {
    List<String> hotKeys = redis.getHotKeys();  // Redis 4.0+
    for (String key : hotKeys) {
        // Replicate to local cache
        localCache.put(key, redis.get(key));
        
        // Add to CDN
        cdnService.cache(key, redis.get(key));
    }
}
```

---

### Q16: Design a distributed cache system.
**Expected Answer**:

**Architecture**: Consistent hashing, replication, sharding

```java
public class DistributedCache {
    private final ConsistentHash<CacheNode> ring;
    private final int replicationFactor = 3;
    
    public void put(String key, String value) {
        List<CacheNode> nodes = ring.getNodes(key, replicationFactor);
        for (CacheNode node : nodes) {
            node.put(key, value);  // Write to replicas
        }
    }
    
    public String get(String key) {
        List<CacheNode> nodes = ring.getNodes(key, replicationFactor);
        return nodes.get(0).get(key);  // Read from primary
    }
}
```

**Follow-up**: Cache warming:
```java
@Service
public class CacheWarmingService {
    public void warmCache() {
        // 1. Identify hot keys from logs
        List<String> hotKeys = analyticsService.getHotKeys(24);
        
        // 2. Pre-load into new cache
        for (String key : hotKeys) {
            String value = database.get(key);
            newCache.put(key, value);
        }
        
        // 3. Gradual traffic shift
        loadBalancer.shiftTraffic(newCache, 10);  // 10%
        Thread.sleep(60000);
        loadBalancer.shiftTraffic(newCache, 50);  // 50%
        Thread.sleep(60000);
        loadBalancer.shiftTraffic(newCache, 100); // 100%
    }
}
```

---

### Q17: How do you design for observability in microservices?
**Expected Answer**:

**Three Pillars**: Metrics, Logs, Traces

```java
// 1. Distributed Tracing
@RestController
public class OrderController {
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        Span span = tracer.buildSpan("getOrder").start();
        span.setTag("order.id", id);
        try {
            return orderService.findById(id);
        } finally {
            span.finish();
        }
    }
}

// 2. Structured Logging
log.info("Order created", 
    kv("orderId", order.getId()),
    kv("userId", order.getUserId()),
    kv("amount", order.getAmount()),
    kv("correlationId", MDC.get("correlationId"))
);

// 3. Metrics
@Timed(value = "order.creation.time")
@Counted(value = "order.creation.count")
public Order createOrder(OrderRequest request) {
    meterRegistry.counter("orders.created", 
        "status", "success",
        "region", "us-east-1"
    ).increment();
}
```

**Follow-up**: Debug performance issue:
```
1. Check metrics dashboard (Grafana)
   - CPU, memory, request rate
   - Identify spike time

2. Analyze distributed traces (Jaeger)
   - Find slow requests
   - Identify bottleneck service

3. Check logs (ELK)
   - Filter by correlation ID
   - Look for errors/warnings

4. Profile application (JProfiler)
   - CPU profiling
   - Memory profiling

5. Database query analysis
   - Slow query log
   - Explain plan
```

---

## 👔 Part 6: Leadership & Management (15 min)

### Q18: How do you handle technical debt in a fast-paced environment?
**Expected Answer**:

**Framework**:
```
1. Identify & Categorize:
   - Code quality (duplication, complexity)
   - Architecture (tight coupling, monolith)
   - Infrastructure (outdated dependencies)
   - Documentation (missing/outdated)

2. Measure Impact:
   - Velocity: Story points/sprint
   - Quality: Bug rate, incident count
   - Developer satisfaction: Survey
   - Time spent on maintenance

3. Prioritize:
   High Impact + High Effort = Plan carefully
   High Impact + Low Effort = Do immediately
   Low Impact + High Effort = Avoid
   Low Impact + Low Effort = Do when time permits

4. Allocate Time:
   - 20% of sprint capacity for tech debt
   - Dedicated "fix-it" days quarterly
   - Include in definition of done

5. Track Progress:
   - Tech debt backlog
   - Metrics dashboard
   - Regular reviews
```

**Example**:
```
Problem: Monolithic application slowing deployments

Approach:
1. Identified bounded contexts
2. Extracted payment service first (high value)
3. Strangler fig pattern (gradual migration)
4. Allocated 2 engineers for 3 sprints
5. Result: Deployment time 2 hours -> 15 minutes
```

---

### Q19: Describe your approach to code reviews and maintaining code quality.
**Expected Answer**:

**Code Review Checklist**:
```
✅ Functionality:
   - Does it solve the problem?
   - Edge cases handled?
   - Tests included?

✅ Design:
   - SOLID principles followed?
   - Appropriate design patterns?
   - Scalable and maintainable?

✅ Code Quality:
   - Readable and self-documenting?
   - No code duplication?
   - Proper error handling?

✅ Performance:
   - Efficient algorithms?
   - Database queries optimized?
   - No memory leaks?

✅ Security:
   - Input validation?
   - SQL injection prevented?
   - Sensitive data protected?
```

**Process**:
```
1. Automated Checks (CI/CD):
   - SonarQube (code quality)
   - Checkstyle (formatting)
   - SpotBugs (bug detection)
   - JaCoCo (code coverage > 80%)

2. Peer Review:
   - Max 400 lines per review
   - Response within 24 hours
   - At least 2 approvals for critical code

3. Constructive Feedback:
   ❌ "This code is bad"
   ✅ "Consider using Strategy pattern here for better extensibility"

4. Knowledge Sharing:
   - Weekly code review sessions
   - Share interesting PRs in team channel
   - Document patterns in wiki
```

---

### Q20: How do you mentor junior developers and build team capability?
**Expected Answer**:

**Mentoring Framework**:
```
1. Onboarding (First 30 days):
   - Assign buddy
   - Setup development environment
   - Small starter tasks
   - Daily check-ins

2. Skill Development:
   - Pair programming sessions (2x/week)
   - Code review as teaching tool
   - Assign gradually complex tasks
   - Encourage questions

3. Career Growth:
   - Quarterly 1-on-1s
   - Individual development plan
   - Conference/training budget
   - Stretch assignments

4. Knowledge Sharing:
   - Internal tech talks
   - Brown bag sessions
   - Documentation culture
   - Rotate on-call duties
```

**Example**:
```
Junior Dev: Struggling with async programming

Approach:
1. Paired on CompletableFuture example
2. Assigned ticket to add async endpoint
3. Reviewed code with detailed feedback
4. Shared resources (articles, videos)
5. Follow-up session after 2 weeks

Result: Now confidently uses reactive programming
```

---

### Q21: How do you handle disagreements with product/business teams?
**Expected Answer**:

**Approach**:
```
1. Understand Business Context:
   - What problem are we solving?
   - What's the business impact?
   - What are the constraints?

2. Data-Driven Discussion:
   - Present technical trade-offs
   - Show metrics/benchmarks
   - Estimate effort and timeline

3. Propose Alternatives:
   Option A: Full solution (3 months, $$$)
   Option B: MVP (1 month, $$)
   Option C: Workaround (1 week, $)

4. Find Common Ground:
   - Focus on shared goals
   - Compromise where possible
   - Escalate if needed

5. Document Decision:
   - Architecture Decision Record (ADR)
   - Rationale and trade-offs
   - Review in retrospective
```

**Example**:
```
Disagreement: Product wants real-time analytics

Technical concern: Current architecture can't support

Resolution:
1. Explained technical constraints
2. Proposed near-real-time (5-min delay)
3. Showed cost comparison ($10K vs $100K)
4. Agreed on phased approach:
   - Phase 1: 5-min delay (1 month)
   - Phase 2: Real-time for critical metrics (3 months)
5. Both teams satisfied
```

---

## 🎭 Part 7: Behavioral & Situational (15 min)

### Q22: Tell me about a time you had to make a critical architectural decision under pressure.
**STAR Format Expected**:

**Example Answer**:
```
Situation:
- E-commerce platform experiencing 10x traffic during Black Friday
- Database hitting 95% CPU, queries timing out
- Revenue loss: $10K/minute

Task:
- Stabilize system immediately
- Prevent future occurrences
- Minimize downtime

Action:
1. Immediate (5 minutes):
   - Enabled read replicas for product catalog
   - Added aggressive caching (Redis)
   - Rate limited non-critical APIs

2. Short-term (2 hours):
   - Scaled database vertically (doubled capacity)
   - Optimized slow queries (added indexes)
   - Implemented circuit breakers

3. Long-term (2 weeks):
   - Migrated to database sharding
   - Implemented CQRS pattern
   - Added load testing to CI/CD

Result:
- System stabilized in 15 minutes
- Handled 50x traffic next year
- Zero downtime during subsequent sales
- Promoted to Senior Architect

Lessons Learned:
- Always have scaling playbook ready
- Load test before major events
- Monitor leading indicators (not just CPU)
```

---

### Q23: Describe a situation where you had to deal with a production outage.
**Expected Answer**:

**Example**:
```
Incident: Payment service down for 45 minutes

Response Process:
1. Detection (2 min):
   - PagerDuty alert
   - Assembled war room

2. Triage (5 min):
   - Checked metrics dashboard
   - Identified database connection pool exhausted
   - Root cause: Memory leak in new deployment

3. Mitigation (10 min):
   - Rolled back to previous version
   - Restarted affected instances
   - Verified system recovery

4. Communication:
   - Status page updated every 5 minutes
   - Stakeholders notified
   - Customer support briefed

5. Post-Mortem (Next day):
   - Timeline documented
   - Root cause: Unclosed database connections
   - Action items:
     • Add connection pool monitoring
     • Implement canary deployments
     • Add integration tests for resource leaks
     • Improve rollback automation

6. Prevention:
   - Deployed fixes within 1 week
   - No similar incidents in 2 years
```

---

### Q24: How do you stay updated with technology trends?
**Expected Answer**:

**Learning Strategy**:
```
1. Daily (30 min):
   - Hacker News, Reddit r/programming
   - Tech newsletters (TLDR, ByteByteGo)
   - Twitter tech influencers

2. Weekly (2 hours):
   - Read technical blogs (Martin Fowler, Netflix Tech Blog)
   - Watch conference talks (YouTube)
   - Experiment with new tools

3. Monthly:
   - Attend local meetups
   - Contribute to open source
   - Write blog posts

4. Quarterly:
   - Attend conferences (AWS re:Invent, KubeCon)
   - Take online courses (Coursera, Udemy)
   - Read technical books

5. Continuous:
   - Side projects (try new tech)
   - Internal tech talks
   - Mentor others (teaching reinforces learning)
```

**Recent Learning**:
```
- Explored virtual threads in Java 21
- Built side project with Rust
- Completed AWS Solutions Architect certification
- Contributed to Spring Boot project
```

---

### Q25: Tell me about a time you had to deliver bad news to stakeholders.
**Expected Answer**:

**Example**:
```
Situation:
Promised feature delivery in 2 weeks
Discovered critical security vulnerability requiring 1 week fix
Would miss deadline

Approach:
1. Transparency:
   - Scheduled immediate meeting
   - Explained situation honestly
   - No sugarcoating

2. Context:
   - Showed security risk (data breach potential)
   - Explained regulatory implications
   - Presented evidence (penetration test results)

3. Options:
   Option A: Ship on time (high risk)
   Option B: Delay 1 week, fix security (recommended)
   Option C: Ship with workaround (medium risk)

4. Recommendation:
   - Strongly recommended Option B
   - Explained long-term cost of Option A
   - Offered to work weekends to minimize delay

5. Follow-through:
   - Delivered secure feature in 2.5 weeks
   - Provided daily updates
   - Documented lessons learned

Result:
- Stakeholders appreciated honesty
- Trust strengthened
- Avoided potential $1M breach
- Implemented security review in process
```

---

## 💼 Part 8: Previous Projects Deep Dive (15 min)

### Q26: Walk me through the most complex system you've designed.
**Expected Discussion**:

**Example Answer**:
```
System: Real-time fraud detection for payment platform

Business Problem:
- Processing 50K transactions/sec
- Fraud rate: 2% ($10M annual loss)
- Need real-time detection (<100ms)

Architecture:
1. Ingestion Layer:
   - Kafka (transaction events)
   - 50 partitions for parallelism

2. Processing Layer:
   - Flink for stream processing
   - Rule engine (100+ rules)
   - ML model (XGBoost)
   - Feature store (Redis)

3. Decision Layer:
   - Risk score calculation
   - Threshold-based blocking
   - Manual review queue

4. Storage Layer:
   - Cassandra (transaction history)
   - Elasticsearch (search/analytics)
   - S3 (audit logs)

Challenges:
1. Latency: Optimized with caching, reduced to 50ms
2. False positives: A/B tested thresholds, reduced by 40%
3. Model deployment: Implemented canary releases
4. Scale: Handled Black Friday (5x traffic)

Technologies:
- Java, Spring Boot, Kafka, Flink
- Redis, Cassandra, Elasticsearch
- Docker, Kubernetes, AWS

Team: 8 engineers, 6 months

Impact:
- Reduced fraud by 60% ($6M saved)
- False positive rate: 15% -> 5%
- 99.99% uptime
- Won company innovation award
```

---

### Q27: What was your biggest technical failure and what did you learn?
**Expected Answer**:

**Example**:
```
Failure: Database migration caused 4-hour outage

Context:
- Migrating from MySQL to PostgreSQL
- 500GB database
- Planned for 2-hour maintenance window

What Went Wrong:
1. Underestimated data migration time (4 hours vs 2 hours)
2. Didn't test rollback procedure
3. Foreign key constraints caused issues
4. No communication plan for extended outage

Impact:
- 4-hour downtime
- $500K revenue loss
- Customer complaints
- Team morale affected

Recovery:
1. Completed migration (couldn't rollback)
2. Communicated transparently with customers
3. Offered credits to affected users
4. Conducted blameless post-mortem

Lessons Learned:
1. Always test on production-like data
2. Have tested rollback plan
3. Use blue-green deployment for databases
4. Over-communicate during incidents
5. Build in buffer time (2x estimate)

Changes Implemented:
1. Created migration runbook
2. Mandatory dry runs for major changes
3. Improved monitoring and alerting
4. Implemented feature flags for gradual rollout
5. Regular disaster recovery drills

Personal Growth:
- Learned humility
- Better risk assessment
- Improved planning skills
- Stronger communication

No similar incidents in 5 years since
```

---

### Q28: How do you approach migrating a legacy monolith to microservices?
**Expected Answer**:

**Migration Strategy**:
```
1. Assessment Phase (2 weeks):
   - Analyze current architecture
   - Identify pain points
   - Map dependencies
   - Estimate effort

2. Planning Phase (1 month):
   - Define bounded contexts (DDD)
   - Prioritize services to extract
   - Design service boundaries
   - Plan data migration

3. Execution (Strangler Fig Pattern):
   
   Step 1: Add API Gateway
   [Clients] -> [API Gateway] -> [Monolith]
   
   Step 2: Extract first service
   [Clients] -> [API Gateway] -> [Service 1]
                              -> [Monolith]
   
   Step 3: Gradually extract more
   [Clients] -> [API Gateway] -> [Service 1]
                              -> [Service 2]
                              -> [Service 3]
                              -> [Monolith] (shrinking)
   
   Step 4: Complete migration
   [Clients] -> [API Gateway] -> [Service 1]
                              -> [Service 2]
                              -> [Service 3]
                              -> [Service N]

4. Service Extraction Order:
   Priority 1: High value, low coupling (payments)
   Priority 2: Frequently changing (user profile)
   Priority 3: Performance bottlenecks (search)
   Priority 4: Everything else

5. Data Migration:
   - Start with shared database
   - Gradually split databases
   - Use CDC (Change Data Capture) for sync
   - Eventual consistency where possible

6. Risk Mitigation:
   - Feature flags for rollback
   - Canary deployments
   - Comprehensive monitoring
   - Parallel run (dual write)
```

**Example Timeline**:
```
Month 1-2: Planning and setup
Month 3-4: Extract payment service
Month 5-6: Extract user service
Month 7-8: Extract inventory service
Month 9-12: Extract remaining services
Month 13-18: Decommission monolith

Team: 12 engineers
Cost: $2M
Benefit: 10x faster deployments, 50% cost reduction
```

---

## 🔧 Part 9: Coding Exercise (20 min)

### Q29: Implement a thread-safe Singleton with lazy initialization.
```java
public class Singleton {
    // Implement here
}
```

**Expected Solution**:

```java
// Solution 1: Double-checked locking
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {
        // Prevent reflection attack
        if (instance != null) {
            throw new RuntimeException("Use getInstance()");
        }
    }
    
    public static Singleton getInstance() {
        if (instance == null) {  // First check (no locking)
            synchronized (Singleton.class) {
                if (instance == null) {  // Second check (with locking)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Solution 2: Bill Pugh Singleton (Best)
public class Singleton {
    private Singleton() {}
    
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}

// Solution 3: Enum Singleton (Most robust)
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        // Business logic
    }
}
// Usage: Singleton.INSTANCE.doSomething();
```

---

### Q30: Design and implement a simple in-memory database with transactions.
**Requirements**:
- GET, SET, DELETE operations
- BEGIN, COMMIT, ROLLBACK for transactions
- Nested transactions support

```java
public interface InMemoryDB {
    void set(String key, String value);
    String get(String key);
    void delete(String key);
    void begin();
    void commit();
    void rollback();
}
```

**Expected Solution**:

```java
public class InMemoryDB {
    private Map<String, String> data = new HashMap<>();
    private Deque<Map<String, String>> transactions = new ArrayDeque<>();
    
    public void set(String key, String value) {
        if (transactions.isEmpty()) {
            data.put(key, value);
        } else {
            transactions.peek().put(key, value);
        }
    }
    
    public String get(String key) {
        // Check transactions from newest to oldest
        for (Map<String, String> txn : transactions) {
            if (txn.containsKey(key)) {
                return txn.get(key);
            }
        }
        return data.get(key);
    }
    
    public void delete(String key) {
        if (transactions.isEmpty()) {
            data.remove(key);
        } else {
            transactions.peek().put(key, null);  // Tombstone
        }
    }
    
    public void begin() {
        transactions.push(new HashMap<>());
    }
    
    public void commit() {
        if (transactions.isEmpty()) {
            throw new IllegalStateException("No transaction to commit");
        }
        
        Map<String, String> txn = transactions.pop();
        
        if (transactions.isEmpty()) {
            // Commit to main data
            for (Map.Entry<String, String> entry : txn.entrySet()) {
                if (entry.getValue() == null) {
                    data.remove(entry.getKey());
                } else {
                    data.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            // Merge with parent transaction
            transactions.peek().putAll(txn);
        }
    }
    
    public void rollback() {
        if (transactions.isEmpty()) {
            throw new IllegalStateException("No transaction to rollback");
        }
        transactions.pop();  // Discard transaction
    }
}

// Usage example
InMemoryDB db = new InMemoryDB();
db.set("a", "1");
db.begin();
db.set("a", "2");
db.begin();
db.set("a", "3");
System.out.println(db.get("a"));  // 3
db.rollback();
System.out.println(db.get("a"));  // 2
db.commit();
System.out.println(db.get("a"));  // 2
```

---

### Q31: Implement a concurrent task scheduler.
**Requirements**:
- Schedule tasks with delay
- Cancel scheduled tasks
- Thread-safe execution
- Handle task failures

```java
public interface TaskScheduler {
    String schedule(Runnable task, long delayMs);
    boolean cancel(String taskId);
    void shutdown();
}
```

**Expected Solution**:

```java
public class ConcurrentTaskScheduler implements TaskScheduler {
    private final ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks;
    private final AtomicLong taskIdGenerator;
    
    public ConcurrentTaskScheduler(int poolSize) {
        this.executor = Executors.newScheduledThreadPool(poolSize);
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicLong(0);
    }
    
    @Override
    public String schedule(Runnable task, long delayMs) {
        String taskId = "task-" + taskIdGenerator.incrementAndGet();
        
        Runnable wrappedTask = () -> {
            try {
                task.run();
            } catch (Exception e) {
                handleTaskFailure(taskId, e);
            } finally {
                scheduledTasks.remove(taskId);
            }
        };
        
        ScheduledFuture<?> future = executor.schedule(
            wrappedTask, 
            delayMs, 
            TimeUnit.MILLISECONDS
        );
        
        scheduledTasks.put(taskId, future);
        return taskId;
    }
    
    @Override
    public boolean cancel(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            return future.cancel(false);  // Don't interrupt if running
        }
        return false;
    }
    
    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void handleTaskFailure(String taskId, Exception e) {
        System.err.println("Task " + taskId + " failed: " + e.getMessage());
        // Could implement retry logic here
    }
}

// Usage
TaskScheduler scheduler = new ConcurrentTaskScheduler(10);
String taskId = scheduler.schedule(() -> {
    System.out.println("Task executed");
}, 5000);

// Cancel if needed
scheduler.cancel(taskId);

// Shutdown
scheduler.shutdown();
```

---

## 🎯 Part 10: Advanced Scenarios

### Q32: How would you design a real-time analytics system processing 1M events/sec?
**Expected Answer**:

**Architecture**:
```
Event Sources -> Kafka (ingestion)
                   |
                   v
            Kafka Streams / Flink (processing)
                   |
                   +-> InfluxDB (time-series metrics)
                   +-> Cassandra (raw events)
                   +-> Redis (real-time counters)
                   |
                   v
            Grafana / Custom Dashboard
```

**Implementation**:
```java
// Kafka Streams for real-time aggregation
StreamsBuilder builder = new StreamsBuilder();

KStream<String, Event> events = builder.stream("events");

// Tumbling window (1 minute)
events
    .groupByKey()
    .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
    .aggregate(
        () -> new EventStats(),
        (key, event, stats) -> stats.add(event),
        Materialized.with(Serdes.String(), eventStatsSerde)
    )
    .toStream()
    .to("aggregated-stats");

// Late data handling (grace period)
events
    .groupByKey()
    .windowedBy(
        TimeWindows.of(Duration.ofMinutes(1))
            .grace(Duration.ofMinutes(5))  // Accept late data
    )
    .count()
    .toStream()
    .to("event-counts");
```

**Scale Considerations**:
- **Partitioning**: 100 Kafka partitions
- **Processing**: 50 Flink task managers
- **Storage**: Time-series DB with downsampling
- **Query**: Pre-aggregated data for fast queries

---

### Q33: Explain your approach to database sharding.
**Expected Answer**:

**Sharding Strategies**:
```
1. Hash-based:
   shard = hash(user_id) % num_shards
   Pros: Even distribution
   Cons: Hard to add shards

2. Range-based:
   Shard 1: user_id 1-1M
   Shard 2: user_id 1M-2M
   Pros: Easy range queries
   Cons: Uneven distribution

3. Geo-based:
   Shard 1: US users
   Shard 2: EU users
   Pros: Low latency
   Cons: Uneven load

4. Consistent hashing:
   Virtual nodes on hash ring
   Pros: Easy to add/remove shards
   Cons: Complex implementation
```

**Implementation**:
```java
@Service
public class ShardingService {
    private final List<DataSource> shards;
    
    public DataSource getShard(String userId) {
        int shardId = Math.abs(userId.hashCode()) % shards.size();
        return shards.get(shardId);
    }
    
    // Cross-shard query
    public List<Order> getOrders(String userId) {
        DataSource shard = getShard(userId);
        return jdbcTemplate.query(shard, 
            "SELECT * FROM orders WHERE user_id = ?", 
            userId);
    }
    
    // Scatter-gather for global queries
    public List<Order> getAllOrders() {
        return shards.parallelStream()
            .flatMap(shard -> 
                jdbcTemplate.query(shard, "SELECT * FROM orders").stream())
            .collect(Collectors.toList());
    }
}
```

---

### Q34: How do you implement feature flags at scale?
**Expected Answer**:

**Architecture**:
```java
@Service
public class FeatureFlagService {
    private final RedisTemplate<String, String> redis;
    private final LoadingCache<String, Boolean> localCache;
    
    public boolean isEnabled(String feature, String userId) {
        // Check local cache first
        Boolean cached = localCache.getIfPresent(feature + ":" + userId);
        if (cached != null) return cached;
        
        // Check Redis
        FeatureFlag flag = getFeatureFlag(feature);
        boolean enabled = evaluateFlag(flag, userId);
        
        // Cache result
        localCache.put(feature + ":" + userId, enabled);
        return enabled;
    }
    
    private boolean evaluateFlag(FeatureFlag flag, String userId) {
        // 1. Check if user in whitelist
        if (flag.getWhitelist().contains(userId)) {
            return true;
        }
        
        // 2. Check percentage rollout
        if (flag.getPercentage() > 0) {
            int hash = Math.abs(userId.hashCode());
            return (hash % 100) < flag.getPercentage();
        }
        
        // 3. Check custom rules
        return flag.getRules().stream()
            .anyMatch(rule -> rule.matches(userId));
    }
}

// Usage
@RestController
public class OrderController {
    @GetMapping("/orders")
    public List<Order> getOrders(@RequestParam String userId) {
        if (featureFlagService.isEnabled("new-order-api", userId)) {
            return newOrderService.getOrders(userId);
        } else {
            return oldOrderService.getOrders(userId);
        }
    }
}
```

**Best Practices**:
- Real-time updates via Redis pub/sub
- Local caching for performance
- Gradual rollout (0% -> 10% -> 50% -> 100%)
- A/B testing integration
- Automatic cleanup of old flags

---

## 🧮 Part 11: Data Structures & Algorithms (20 min)

### Q35: Find Median from Data Stream
**Problem**: Design a data structure that supports adding integers and finding median in real-time.

**Expected Solution**: Two Heaps (Min Heap + Max Heap)

```java
class MedianFinder {
    private PriorityQueue<Integer> maxHeap; // Smaller half
    private PriorityQueue<Integer> minHeap; // Larger half
    
    public MedianFinder() {
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        minHeap = new PriorityQueue<>();
    }
    
    public void addNum(int num) {
        maxHeap.offer(num);
        minHeap.offer(maxHeap.poll());
        
        if (maxHeap.size() < minHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
    }
    
    public double findMedian() {
        if (maxHeap.size() > minHeap.size()) {
            return maxHeap.peek();
        }
        return (maxHeap.peek() + minHeap.peek()) / 2.0;
    }
}
```

**Time Complexity**: O(log n) for addNum, O(1) for findMedian  
**Space Complexity**: O(n)

**Why This Matters**: Tests understanding of heaps, streaming data, and real-time systems

---

### Q36: Top K Frequent Elements
**Problem**: Given array and integer k, return k most frequent elements.

**Expected Solution**: HashMap + Min Heap

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
            minHeap.poll();  // Remove least frequent
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

**Time Complexity**: O(n log k)  
**Space Complexity**: O(n)

**Why This Matters**: Common in analytics, trending features, recommendation systems

---

### Q37: Median of Two Sorted Arrays
**Problem**: Find median of two sorted arrays in O(log(min(m,n))) time.

**Expected Solution**: Binary Search

```java
public double findMedian(int[] nums1, int[] nums2) {
    // Ensure nums1 is smaller
    if (nums1.length > nums2.length) {
        return findMedian(nums2, nums1);
    }
    
    int m = nums1.length, n = nums2.length;
    int left = 0, right = m;
    
    while (left <= right) {
        int partition1 = (left + right) / 2;
        int partition2 = (m + n + 1) / 2 - partition1;
        
        int maxLeft1 = (partition1 == 0) ? Integer.MIN_VALUE : nums1[partition1 - 1];
        int minRight1 = (partition1 == m) ? Integer.MAX_VALUE : nums1[partition1];
        int maxLeft2 = (partition2 == 0) ? Integer.MIN_VALUE : nums2[partition2 - 1];
        int minRight2 = (partition2 == n) ? Integer.MAX_VALUE : nums2[partition2];
        
        if (maxLeft1 <= minRight2 && maxLeft2 <= minRight1) {
            if ((m + n) % 2 == 0) {
                return (Math.max(maxLeft1, maxLeft2) + 
                        Math.min(minRight1, minRight2)) / 2.0;
            }
            return Math.max(maxLeft1, maxLeft2);
        } else if (maxLeft1 > minRight2) {
            right = partition1 - 1;
        } else {
            left = partition1 + 1;
        }
    }
    throw new IllegalArgumentException();
}
```

**Time Complexity**: O(log(min(m,n)))  
**Space Complexity**: O(1)

**Why This Matters**: Tests binary search mastery, critical for distributed systems

---

### Q38: Design LRU Cache
**Problem**: Implement LRU cache with O(1) get and put operations.

**Expected Solution**: HashMap + Doubly Linked List

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

**Time Complexity**: O(1) for both get and put  
**Space Complexity**: O(capacity)

**Why This Matters**: Fundamental for caching systems, used in Redis, CDNs, databases

---

### Q39: Design Consistent Hashing
**Problem**: Implement consistent hashing for distributed cache.

**Expected Solution**: TreeMap (Red-Black Tree)

```java
public class ConsistentHashing {
    private final TreeMap<Integer, String> ring;
    private final int virtualNodes;
    
    public ConsistentHashing(int virtualNodes) {
        this.ring = new TreeMap<>();
        this.virtualNodes = virtualNodes;
    }
    
    public void addNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hash(node + "#" + i);
            ring.put(hash, node);
        }
    }
    
    public void removeNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hash(node + "#" + i);
            ring.remove(hash);
        }
    }
    
    public String getNode(String key) {
        if (ring.isEmpty()) return null;
        
        int hash = hash(key);
        Map.Entry<Integer, String> entry = ring.ceilingEntry(hash);
        
        if (entry == null) {
            entry = ring.firstEntry();  // Wrap around
        }
        
        return entry.getValue();
    }
    
    private int hash(String key) {
        return key.hashCode();
    }
}
```

**Time Complexity**: O(log n) for get/add/remove  
**Space Complexity**: O(n × virtualNodes)

**Why This Matters**: Critical for distributed systems, load balancing, sharding

---

### Q40: Implement Trie (Prefix Tree)
**Problem**: Implement trie for autocomplete/search suggestions.

**Expected Solution**: Tree with 26 children per node

```java
class Trie {
    class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEndOfWord;
    }
    
    private final TrieNode root;
    
    public Trie() {
        root = new TrieNode();
    }
    
    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
        }
        node.isEndOfWord = true;
    }
    
    public boolean search(String word) {
        TrieNode node = searchPrefix(word);
        return node != null && node.isEndOfWord;
    }
    
    public boolean startsWith(String prefix) {
        return searchPrefix(prefix) != null;
    }
    
    private TrieNode searchPrefix(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return null;
            }
            node = node.children[index];
        }
        return node;
    }
}
```

**Time Complexity**: O(m) where m = word length  
**Space Complexity**: O(n × m) where n = number of words

**Why This Matters**: Used in autocomplete, spell checkers, IP routing, search engines

### Technical Depth (40%)
- ✅ Deep understanding of core concepts
- ✅ Hands-on coding ability
- ✅ System design thinking
- ✅ Problem-solving approach
- ✅ Technology breadth and depth

### Leadership & Communication (30%)
- ✅ Clear articulation of ideas
- ✅ Mentoring and team building
- ✅ Stakeholder management
- ✅ Decision-making process
- ✅ Conflict resolution

### Experience & Impact (20%)
- ✅ Scale of systems built
- ✅ Business impact delivered
- ✅ Technical challenges overcome
- ✅ Team and organizational influence
- ✅ Innovation and thought leadership

### Cultural Fit (10%)
- ✅ Learning mindset
- ✅ Collaboration skills
- ✅ Ownership and accountability
- ✅ Adaptability
- ✅ Values alignment

---

## 🎓 Red Flags to Watch For

❌ **Technical Red Flags**:
- Cannot explain past architectural decisions
- Lacks depth in claimed expertise areas
- Unable to code without IDE/Google
- No awareness of trade-offs
- Outdated knowledge (stuck in old technologies)

❌ **Leadership Red Flags**:
- Blames others for failures
- Cannot provide concrete examples
- Dismissive of junior developers
- Rigid thinking, not open to feedback
- Poor communication skills

❌ **Cultural Red Flags**:
- Not curious or learning-oriented
- Arrogant or condescending
- Cannot work collaboratively
- Focuses only on technical aspects, ignores business
- Negative attitude towards previous employers

---

## 💡 Interview Tips

### For Interviewers:
1. **Start broad, go deep** - Begin with high-level questions, drill down based on answers
2. **Listen actively** - Pay attention to thought process, not just final answer
3. **Allow thinking time** - Complex problems need time to think
4. **Probe for depth** - Ask "why" and "how" follow-ups
5. **Assess collaboration** - See how they handle hints and feedback

### Question Selection Strategy:
- **Must ask**: Q1, Q5, Q15, Q26, Q27 (Core technical + Experience)
- **Choose 2-3 from each section** based on role requirements
- **Adapt based on resume** - Focus on claimed expertise
- **Save time for coding** - At least one hands-on exercise

---

## 📝 Sample Interview Flow (90 min)

**0-10 min**: Introduction, background, current role
**10-30 min**: Java + Spring Boot (Q1, Q2, Q5, Q6)
**30-45 min**: System Design (Q15 or Q16)
**45-60 min**: Coding Exercise (Q29 or Q30)
**60-75 min**: Leadership + Behavioral (Q18, Q22, Q23)
**75-85 min**: Previous Projects (Q26, Q27)
**85-90 min**: Candidate questions

---

## 🎯 Decision Framework

### Strong Hire (Score: 9-10)
- Exceptional technical depth across all areas
- Proven track record of leading large-scale systems
- Strong leadership and mentoring abilities
- Excellent communication and collaboration
- Can drive technical strategy

### Hire (Score: 7-8)
- Strong technical skills with some gaps
- Good system design thinking
- Demonstrated leadership experience
- Clear communicator
- Can contribute immediately

### Maybe (Score: 5-6)
- Adequate technical skills but lacks depth
- Limited system design experience
- Some leadership experience
- Needs more assessment or different role

### No Hire (Score: 1-4)
- Significant technical gaps
- Cannot solve problems independently
- Poor communication or collaboration
- Lacks relevant experience
- Cultural misfit
