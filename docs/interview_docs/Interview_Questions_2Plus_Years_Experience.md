# Interview Questions - Backend Developer (2+ Years Experience)

## 📋 Candidate Profile
- **Experience**: 2+ years in backend development
- **Core Skills**: Java, Spring Boot, Microservices, Kafka, AWS, Docker/Kubernetes
- **Database**: MySQL, MongoDB, Redis, PostgreSQL
- **Recent Work**: Healthcare & Mobility domains, SFTP pipelines, Event-driven architecture

---

## 🎯 Section 1: Java Fundamentals & Collections

### Q1: Explain ArrayList vs LinkedList
**Expected Answer**:

| Feature | ArrayList | LinkedList |
|---------|-----------|------------|
| Storage | Dynamic array | Doubly linked list |
| Access | O(1) | O(n) |
| Insert/Delete | O(n) | O(1) |
| Memory | Less overhead | More overhead (node pointers) |

```java
// ArrayList - fast access
List<String> arrayList = new ArrayList<>();
arrayList.add("A");
String item = arrayList.get(0); // O(1)

// LinkedList - fast insert/delete
List<String> linkedList = new LinkedList<>();
linkedList.addFirst("A"); // O(1)
linkedList.removeLast(); // O(1)
```

**Use ArrayList when**: Random access needed  
**Use LinkedList when**: Frequent insertions/deletions at beginning/end

---

### Q2: What is the difference between HashMap and ConcurrentHashMap?
**Expected Answer**:

```java
// HashMap - NOT thread-safe
Map<String, Integer> hashMap = new HashMap<>();
hashMap.put("key", 1); // Can cause issues in multi-threading

// ConcurrentHashMap - Thread-safe
Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
concurrentMap.put("key", 1); // Safe for concurrent access
```

**Key Differences**:
- HashMap: Not synchronized, allows null key/value, faster
- ConcurrentHashMap: Thread-safe, no null key/value, uses segment locking

---

### Q3: Explain HashSet vs TreeSet
**Expected Answer**:

```java
// HashSet - unordered, O(1) operations
Set<Integer> hashSet = new HashSet<>();
hashSet.add(5);
hashSet.add(1);
hashSet.add(3);
System.out.println(hashSet); // [1, 3, 5] or any order

// TreeSet - sorted, O(log n) operations
Set<Integer> treeSet = new TreeSet<>();
treeSet.add(5);
treeSet.add(1);
treeSet.add(3);
System.out.println(treeSet); // [1, 3, 5] always sorted
```

---

### Q4: What is a PriorityQueue and when to use it?
**Expected Answer**:

```java
// Min Heap by default
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(2);
minHeap.offer(8);
System.out.println(minHeap.poll()); // 2 (smallest)

// Max Heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
maxHeap.offer(5);
maxHeap.offer(2);
maxHeap.offer(8);
System.out.println(maxHeap.poll()); // 8 (largest)
```

**Use Cases**: Top K problems, task scheduling, Dijkstra's algorithm

---

### Q5: Explain fail-fast vs fail-safe iterators
**Expected Answer**:

```java
// Fail-fast (ArrayList, HashMap)
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
for (String item : list) {
    list.remove(item); // ConcurrentModificationException!
}

// Fail-safe (CopyOnWriteArrayList, ConcurrentHashMap)
List<String> safeList = new CopyOnWriteArrayList<>(Arrays.asList("A", "B", "C"));
for (String item : safeList) {
    safeList.remove(item); // No exception
}
```

---

## 🎯 Section 2: Java Streams API

### Q6: Filter and collect even numbers from a list
**Expected Answer**:

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

List<Integer> evenNumbers = numbers.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());

System.out.println(evenNumbers); // [2, 4, 6, 8, 10]
```

---

### Q7: Find sum of all numbers using Stream
**Expected Answer**:

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// Method 1: reduce
int sum = numbers.stream()
    .reduce(0, Integer::sum);

// Method 2: mapToInt
int sum2 = numbers.stream()
    .mapToInt(Integer::intValue)
    .sum();

System.out.println(sum); // 15
```

---

### Q8: Group employees by department
**Expected Answer**:

```java
class Employee {
    String name;
    String department;
    int salary;
}

List<Employee> employees = Arrays.asList(
    new Employee("John", "IT", 50000),
    new Employee("Jane", "HR", 45000),
    new Employee("Bob", "IT", 55000)
);

Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(e -> e.department));

// Result: {IT=[John, Bob], HR=[Jane]}
```

---

### Q9: Find top 3 highest salaries
**Expected Answer**:

```java
List<Employee> employees = getEmployees();

List<Integer> top3Salaries = employees.stream()
    .map(e -> e.salary)
    .distinct()
    .sorted(Comparator.reverseOrder())
    .limit(3)
    .collect(Collectors.toList());
```

---

### Q10: Convert list of strings to uppercase
**Expected Answer**:

```java
List<String> names = Arrays.asList("john", "jane", "bob");

List<String> upperNames = names.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());

System.out.println(upperNames); // [JOHN, JANE, BOB]
```

---

## 🎯 Section 3: Java Concurrency

### Q11: What is synchronized keyword?
**Expected Answer**:

```java
public class Counter {
    private int count = 0;
    
    // Without synchronized - NOT thread-safe
    public void increment() {
        count++; // Race condition!
    }
    
    // With synchronized - Thread-safe
    public synchronized void incrementSafe() {
        count++; // Only one thread at a time
    }
}
```

**How it works**: Only one thread can execute synchronized method at a time

---

### Q12: Explain wait() and notify()
**Expected Answer**:

```java
class ProducerConsumer {
    private Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    
    public synchronized void produce(int value) throws InterruptedException {
        while (queue.size() == CAPACITY) {
            wait(); // Release lock and wait
        }
        queue.add(value);
        notify(); // Wake up waiting consumer
    }
    
    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // Release lock and wait
        }
        int value = queue.poll();
        notify(); // Wake up waiting producer
        return value;
    }
}
```

**Key Points**:
- wait() releases lock and waits
- notify() wakes up one waiting thread
- Must be called inside synchronized block

---

### Q13: What is the difference between Thread and Runnable?
**Expected Answer**:

```java
// Method 1: Extend Thread
class MyThread extends Thread {
    public void run() {
        System.out.println("Thread running");
    }
}
MyThread t = new MyThread();
t.start();

// Method 2: Implement Runnable (Preferred)
class MyRunnable implements Runnable {
    public void run() {
        System.out.println("Runnable running");
    }
}
Thread t = new Thread(new MyRunnable());
t.start();

// Method 3: Lambda
Thread t = new Thread(() -> System.out.println("Lambda running"));
t.start();
```

**Prefer Runnable**: Can extend other classes, better design

---

### Q14: Explain ExecutorService
**Expected Answer**:

```java
// Create thread pool
ExecutorService executor = Executors.newFixedThreadPool(5);

// Submit tasks
for (int i = 0; i < 10; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " by " + Thread.currentThread().getName());
    });
}

// Shutdown
executor.shutdown();
```

**Benefits**: Thread reuse, better resource management, easier task management

---

### Q15: What is CompletableFuture?
**Expected Answer**:

```java
// Async task
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Runs in background thread
    return "Result from async task";
});

// Chain operations
future.thenApply(result -> result.toUpperCase())
      .thenAccept(result -> System.out.println(result));

// Wait for result
String result = future.get();
```

**Use Case**: Non-blocking async operations, parallel processing

---

## 🎯 Section 4: Spring Boot Basics

### Q16: What are Spring Boot annotations?
**Expected Answer**:

```java
@SpringBootApplication // Main application class
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController // REST API controller
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired // Dependency injection
    private UserService userService;
    
    @GetMapping("/{id}") // GET request
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @PostMapping // POST request
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}

@Service // Service layer
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

@Repository // Data access layer
public interface UserRepository extends JpaRepository<User, Long> {
}
```

---

### Q17: Explain @Transactional annotation
**Expected Answer**:

```java
@Service
public class OrderService {
    
    @Transactional // All or nothing
    public void createOrder(Order order) {
        orderRepository.save(order);
        inventoryService.updateStock(order.getProductId(), -order.getQuantity());
        paymentService.processPayment(order.getAmount());
        // If any fails, all rollback
    }
}
```

**Key Points**:
- Ensures ACID properties
- Automatic rollback on exception
- Can configure isolation level, propagation

---

### Q18: How do you handle exceptions in Spring Boot?
**Expected Answer**:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse("ERROR", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

### Q19: What is dependency injection?
**Expected Answer**:

```java
// Constructor injection (Preferred)
@Service
public class UserService {
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

**Benefits**: Loose coupling, easier testing, better maintainability

---

### Q20: Explain application.properties vs application.yml
**Expected Answer**:

```properties
# application.properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
```

```yaml
# application.yml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
```

**Prefer YAML**: More readable, hierarchical structure

---

## 🎯 Section 5: Kafka Basics

### Q21: What is Kafka and why use it?
**Expected Answer**:

Kafka is a distributed streaming platform for building real-time data pipelines.

**Key Components**:
- **Producer**: Sends messages to topics
- **Consumer**: Reads messages from topics
- **Topic**: Category of messages
- **Partition**: Ordered log of messages
- **Broker**: Kafka server

```java
// Producer
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
producer.send(new ProducerRecord<>("orders", "order-123", "order-data"));

// Consumer
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        System.out.println(record.value());
    }
}
```

---

### Q22: Explain Kafka consumer groups
**Expected Answer**:

```
Topic: orders (3 partitions)

Consumer Group A:
  Consumer 1 → Partition 0
  Consumer 2 → Partition 1
  Consumer 3 → Partition 2

Consumer Group B:
  Consumer 1 → Partition 0, 1, 2
```

**Key Points**:
- Each partition consumed by only ONE consumer in a group
- Multiple groups can consume same topic
- Enables parallel processing

---

### Q23: How do you ensure message ordering in Kafka?
**Expected Answer**:

```java
// Use same key for related messages
producer.send(new ProducerRecord<>("orders", "user-123", "order-1"));
producer.send(new ProducerRecord<>("orders", "user-123", "order-2"));
// Both go to same partition, maintain order
```

**Key**: Messages with same key go to same partition (ordered)

---

### Q24: What is offset in Kafka?
**Expected Answer**:

Offset is the position of a message in a partition.

```
Partition 0:
[msg0] [msg1] [msg2] [msg3] [msg4]
  ↑      ↑      ↑      ↑      ↑
  0      1      2      3      4  (offsets)
```

**Commit strategies**:
- Auto-commit: Automatic offset commit
- Manual commit: Control when to commit

```java
// Manual commit
props.put("enable.auto.commit", "false");
for (ConsumerRecord<String, String> record : records) {
    processRecord(record);
}
consumer.commitSync(); // Commit after processing
```

---

### Q25: How did you use Kafka in your project?
**Expected Answer**:

"In mobility project, we used Kafka for:
- **Real-time location updates**: Drivers send location every 5 seconds
- **Ride requests**: Users request rides, matched with nearby drivers
- **Event streaming**: All events (ride created, started, completed) go through Kafka
- **Analytics**: Separate consumers process events for analytics

**Configuration**:
- 50 partitions for high throughput
- Consumer group per microservice
- Manual offset commit for reliability"

---

## 🎯 Section 6: Database & SQL

### Q26: Write SQL to find second highest salary
**Expected Answer**:

```sql
-- Method 1: LIMIT OFFSET
SELECT salary 
FROM employees 
ORDER BY salary DESC 
LIMIT 1 OFFSET 1;

-- Method 2: Subquery
SELECT MAX(salary) 
FROM employees 
WHERE salary < (SELECT MAX(salary) FROM employees);

-- Method 3: DENSE_RANK
SELECT salary
FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) as rank
    FROM employees
) ranked
WHERE rank = 2;
```

---

### Q27: Explain INNER JOIN vs LEFT JOIN
**Expected Answer**:

```sql
-- INNER JOIN: Only matching rows
SELECT u.name, o.order_id
FROM users u
INNER JOIN orders o ON u.id = o.user_id;
-- Result: Only users who have orders

-- LEFT JOIN: All left table rows + matching right
SELECT u.name, o.order_id
FROM users u
LEFT JOIN orders o ON u.id = o.user_id;
-- Result: All users, NULL for users without orders
```

---

### Q28: What are database indexes?
**Expected Answer**:

```sql
-- Create index
CREATE INDEX idx_email ON users(email);

-- Query uses index (fast)
SELECT * FROM users WHERE email = 'john@example.com';
```

**Benefits**: Faster queries  
**Drawbacks**: Slower writes, more storage

**When to use**: Columns in WHERE, JOIN, ORDER BY

---

### Q29: Explain database transactions (ACID)
**Expected Answer**:

```java
@Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepo.findById(fromId);
    Account to = accountRepo.findById(toId);
    
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));
    
    accountRepo.save(from);
    accountRepo.save(to);
    // Both succeed or both fail
}
```

**ACID**:
- **A**tomicity: All or nothing
- **C**onsistency: Valid state always
- **I**solation: Transactions don't interfere
- **D**urability: Changes are permanent

---

### Q30: How do you use Redis for caching?
**Expected Answer**:

```java
@Service
public class ProductService {
    
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    public Product getProduct(String id) {
        // Check cache first
        String key = "product:" + id;
        Product product = redisTemplate.opsForValue().get(key);
        
        if (product == null) {
            // Cache miss - fetch from DB
            product = productRepository.findById(id);
            // Store in cache
            redisTemplate.opsForValue().set(key, product, 1, TimeUnit.HOURS);
        }
        
        return product;
    }
}
```

**Or use Spring Cache**:
```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(String id) {
    return productRepository.findById(id);
}

@CacheEvict(value = "products", key = "#id")
public void updateProduct(String id, Product product) {
    productRepository.save(product);
}
```

---

## 🎯 Section 7: REST API & Microservices

### Q31: What are HTTP status codes?
**Expected Answer**:

```java
@RestController
public class UserController {
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
        }
        return ResponseEntity.ok(user); // 200
    }
    
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created); // 201
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
```

**Common codes**:
- 200 OK
- 201 Created
- 204 No Content
- 400 Bad Request
- 401 Unauthorized
- 404 Not Found
- 500 Internal Server Error

---

### Q32: Explain REST API best practices
**Expected Answer**:

```java
// 1. Use proper HTTP methods
GET    /api/users        // List users
GET    /api/users/123    // Get user
POST   /api/users        // Create user
PUT    /api/users/123    // Update user
DELETE /api/users/123    // Delete user

// 2. Use plural nouns
/api/users (not /api/user)

// 3. Versioning
/api/v1/users

// 4. Pagination
GET /api/users?page=1&size=20

// 5. Filtering
GET /api/users?status=active&role=admin

// 6. Proper error responses
{
  "error": "USER_NOT_FOUND",
  "message": "User with id 123 not found",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Q33: What is a microservice?
**Expected Answer**:

Microservices = Small, independent services that work together

```
Monolithic:                  Microservices:
┌─────────────┐              ┌─────────────┐
│ All in One  │              │ User Service │
│             │              └─────────────┘
│ - Users     │              ┌─────────────┐
│ - Orders    │              │ Order Service│
│ - Payments  │              └─────────────┘
│ - Inventory │              ┌───────────────┐
└─────────────┘              │ Payment Service│
                           └───────────────┘
```

**Benefits**: Independent deployment, scalability, technology flexibility

---

### Q34: How do microservices communicate?
**Expected Answer**:

```java
// 1. REST API (Synchronous)
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable Long id);
}

// 2. Kafka (Asynchronous)
@Service
public class OrderService {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void createOrder(Order order) {
        orderRepository.save(order);
        kafkaTemplate.send("order-events", new OrderEvent(order));
    }
}
```

---

### Q35: Explain your SFTP pipeline automation
**Expected Answer**:

"We automated healthcare data ingestion:

**Before**: Manual process (4 hours/day)
1. Download files from SFTP
2. Validate format
3. Upload to database
4. Send notifications

**After**: Automated with Spring Batch
```java
@Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
public void processFiles() {
    List<File> files = sftpService.downloadFiles();
    for (File file : files) {
        try {
            validateFile(file);
            loadToDatabase(file);
            archiveFile(file);
        } catch (Exception e) {
            sendAlert(file, e);
        }
    }
}
```

**Result**: 95% reduction in manual work"

---

## 🎯 Section 8: Docker & AWS

### Q36: What is Docker?
**Expected Answer**:

Docker = Package application + dependencies into containers

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/myapp.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t myapp:1.0 .

# Run container
docker run -p 8080:8080 myapp:1.0
```

**Benefits**: Consistency, portability, isolation

---

### Q37: Basic Docker commands
**Expected Answer**:

```bash
# Images
docker images                  # List images
docker pull nginx             # Download image
docker rmi nginx              # Remove image

# Containers
docker ps                     # Running containers
docker ps -a                  # All containers
docker run -d nginx           # Run in background
docker stop <container-id>    # Stop container
docker rm <container-id>      # Remove container
docker logs <container-id>    # View logs
docker exec -it <id> bash     # Enter container
```

---

### Q38: What is Kubernetes?
**Expected Answer**:

Kubernetes = Container orchestration platform

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: myapp:1.0
        ports:
        - containerPort: 8080
```

```bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl scale deployment myapp --replicas=5
```

**Features**: Auto-scaling, self-healing, load balancing

---

### Q39: Explain your AWS deployment
**Expected Answer**:

"We deployed on AWS EKS:

**Architecture**:
1. **Code**: GitHub repository
2. **CI/CD**: CodePipeline + CodeBuild
3. **Container Registry**: ECR
4. **Orchestration**: EKS (Kubernetes)
5. **Load Balancer**: ALB

**Deployment Flow**:
```
GitHub push → CodePipeline triggers → CodeBuild builds Docker image → 
Push to ECR → Update EKS deployment → Rolling update
```

**Result**: 40% faster deployments, zero downtime"

---

### Q40: What AWS services have you used?
**Expected Answer**:

- **EKS**: Kubernetes cluster
- **ECR**: Docker image registry
- **S3**: File storage (SFTP files)
- **RDS**: PostgreSQL database
- **CodePipeline**: CI/CD
- **CodeBuild**: Build Docker images
- **CloudWatch**: Logging and monitoring

---

## 🎯 Section 9: Problem Solving

### Q41: How do you debug a slow API?
**Expected Answer**:

1. **Check logs**: Look for errors
2. **Monitor metrics**: Response time, CPU, memory
3. **Database queries**: Check slow query log
4. **Add logging**: Time each operation
5. **Profile code**: Find bottlenecks

```java
@GetMapping("/users")
public List<User> getUsers() {
    long start = System.currentTimeMillis();
    
    List<User> users = userService.findAll();
    log.info("DB query took: {}ms", System.currentTimeMillis() - start);
    
    return users;
}
```

---

### Q42: How do you handle high traffic?
**Expected Answer**:

1. **Caching**: Redis for frequently accessed data
2. **Load balancing**: Distribute across multiple servers
3. **Database optimization**: Indexes, read replicas
4. **Async processing**: Use queues for heavy tasks
5. **Rate limiting**: Prevent abuse

---

### Q43: Explain a bug you fixed
**Expected Answer**:

"**Problem**: Race condition in inventory update

**Scenario**: Two users buying last item simultaneously

**Root Cause**: No locking mechanism

**Solution**: Added pessimistic locking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdWithLock(@Param("id") Long id);

@Transactional
public void purchaseProduct(Long productId) {
    Product product = productRepo.findByIdWithLock(productId);
    if (product.getStock() > 0) {
        product.setStock(product.getStock() - 1);
        productRepo.save(product);
    }
}
```

**Result**: No more overselling"

---

## 📊 Evaluation Criteria

| Area | Weight | Key Skills |
|------|--------|------------|
| Java Fundamentals | 25% | Collections, Streams, Concurrency |
| Spring Boot | 20% | Annotations, REST APIs, Transactions |
| Kafka | 15% | Producer/Consumer, Partitions |
| Database | 15% | SQL, Caching, Transactions |
| Docker/AWS | 10% | Containers, Deployment |
| Problem Solving | 10% | Debugging, Optimization |
| Communication | 5% | Explain clearly |

---

## 🎯 Expected Proficiency

### Must Know (✅ Critical)
- Java Collections (ArrayList, HashMap, HashSet)
- Stream API basics (filter, map, collect)
- synchronized keyword
- Spring Boot annotations (@RestController, @Service, @Autowired)
- REST API design
- Kafka producer/consumer
- SQL queries (SELECT, JOIN, WHERE)
- Docker basics

### Should Know (✅ Important)
- CompletableFuture
- wait/notify
- @Transactional
- Exception handling
- Redis caching
- Kubernetes basics
- AWS services

### Good to Know (⭐ Bonus)
- Advanced concurrency
- Microservices patterns
- Performance tuning
- CI/CD pipelines

---

## 💡 Interview Tips

1. **Start simple**: Explain basics first, then go deeper
2. **Use examples**: Relate to your projects
3. **Draw diagrams**: Visualize architecture
4. **Mention metrics**: "95% reduction", "100K events/min"
5. **Ask questions**: Clarify requirements
6. **Be honest**: Say "I don't know" if unsure
7. **Show learning**: "I learned X from Y project"

---

## 🔗 Related Documents

- [Kafka Interview Questions](./Kafka_Interview_Questions.md)
- [CompletableFuture Threading](./CompletableFuture_Threading_Explained.md)
- [Synchronized Block](./Synchronized_Block_Explained.md)
- [Wait/Notify Explained](./Wait_Notify_NotifyAll_Explained.md)
- [Priority Queue Deep Dive](./Priority_Queue_Deep_Dive.md)
- [Docker & Kubernetes](./Docker_Kubernetes_Interview_Questions.md)LocatorBuilder builder) {
    return builder.routes()
        .route("pricing", r -> r.path("/api/pricing/**")
            .filters(f -> f.circuitBreaker(c -> c.setName("pricingCB")))
            .uri("lb://pricing-service"))
        .build();
}
```

---

## 🎯 Section 6: Docker & Kubernetes

### Q19: Write a Dockerfile for Spring Boot application
**Expected Answer**:
```dockerfile
FROM openjdk:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Q20: Explain Kubernetes HPA (Horizontal Pod Autoscaler)
**Expected Answer**:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**How it works**: Scales pods based on CPU/memory utilization

---

### Q21: How do you implement ConfigMaps and Secrets in Kubernetes?
**Expected Answer**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  DATABASE_URL: "jdbc:postgresql://db:5432/myapp"
  LOG_LEVEL: "INFO"

---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  DB_PASSWORD: cGFzc3dvcmQ=  # base64 encoded

---
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: app
    envFrom:
    - configMapRef:
        name: app-config
    - secretRef:
        name: app-secrets
```

---

## 🎯 Section 7: Performance & Optimization

### Q22: How did you reduce manual intervention by 95% in SFTP pipeline?
**Expected Answer**:
- **Before**: Manual file download, validation, and upload
- **After**: Automated Spring Batch jobs with schedulers
- **Components**:
  - Cron-based scheduler
  - Automatic file validation
  - Error handling with retry logic
  - Notification on failures

```java
@Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
public void processFiles() {
    List<File> files = sftpService.downloadFiles();
    files.forEach(file -> {
        try {
            processFile(file);
        } catch (Exception e) {
            sendAlert(file, e);
        }
    });
}
```

---

### Q23: How did you improve deployment efficiency by 40%?
**Expected Answer**:
- **Before**: Manual deployments, 30-40 minutes
- **After**: Automated CI/CD pipeline, 15-20 minutes
- **Improvements**:
  - Docker multi-stage builds (smaller images)
  - Parallel test execution
  - Kubernetes rolling updates
  - CodePipeline automation

---

### Q24: Explain your approach to load testing
**Expected Answer**:
- Tools: JMeter, Gatling, K6
- Metrics: Throughput, latency (P50, P95, P99), error rate
- Scenarios: Normal load, peak load, stress test

```java
// Example: JMeter test plan
- Thread Group: 1000 users
- Ramp-up: 60 seconds
- Duration: 10 minutes
- Assertions: Response time < 200ms
```

---

## 🎯 Section 8: Monitoring & Observability

### Q25: How do you monitor microservices in production?
**Expected Answer**:
```java
@RestController
public class MetricsController {
    
    @Autowired
    private MeterRegistry registry;
    
    @GetMapping("/api/orders")
    public List<Order> getOrders() {
        Timer.Sample sample = Timer.start(registry);
        try {
            List<Order> orders = orderService.getOrders();
            registry.counter("orders.retrieved", "status", "success")
                .increment(orders.size());
            return orders;
        } finally {
            sample.stop(registry.timer("orders.latency"));
        }
    }
}
```

**Stack**:
- Metrics: Micrometer + Prometheus + Grafana
- Logging: ELK/EFK stack
- Tracing: Jaeger/Zipkin
- Alerts: PagerDuty/Slack

---

### Q26: What metrics do you track for Kafka consumers?
**Expected Answer**:
- Consumer lag
- Records consumed per second
- Commit rate
- Rebalance frequency
- Processing latency

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group my-group --describe
```

---

## 🎯 Section 9: Problem Solving & Scenarios

### Q27: A service is experiencing high latency. How do you debug?
**Expected Answer**:
1. Check metrics (Grafana dashboards)
2. Analyze logs (ELK stack)
3. Check database slow queries
4. Review thread dumps (`/actuator/threaddump`)
5. Check external API latencies
6. Review cache hit rates
7. Check resource utilization (CPU, memory)

---

### Q28: How do you handle a Kafka consumer that's falling behind?
**Expected Answer**:
1. **Scale consumers**: Add more instances
2. **Optimize processing**: Reduce per-message processing time
3. **Batch processing**: Process multiple messages together
4. **Parallel processing**: Use CompletableFuture
5. **Increase partitions**: More parallelism

```java
// Parallel processing
ExecutorService executor = Executors.newFixedThreadPool(20);
for (ConsumerRecord<String, String> record : records) {
    CompletableFuture.runAsync(() -> process(record), executor);
}
```

---

### Q29: Design a rate limiting solution for APIs
**Expected Answer**:
```java
@RestController
public class ApiController {
    
    @RateLimit(requests = 100, window = 3600, scope = RateLimit.Scope.USER)
    @GetMapping("/api/data")
    public ResponseEntity<Data> getData() {
        return ResponseEntity.ok(dataService.getData());
    }
}

// Redis-based implementation
public class RateLimiter {
    public boolean allowRequest(String key, int limit, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        
        if (count == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        
        return count <= limit;
    }
}
```

---

### Q30: How do you implement retry logic with exponential backoff?
**Expected Answer**:
```java
@Retryable(
    value = {ServiceException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}

// Manual implementation
public <T> T retryWithBackoff(Supplier<T> operation, int maxRetries) {
    int attempt = 0;
    while (attempt < maxRetries) {
        try {
            return operation.get();
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxRetries) throw e;
            
            long delay = (long) Math.pow(2, attempt) * 1000;
            Thread.sleep(delay);
        }
    }
    throw new RuntimeException("Max retries exceeded");
}
```

---

## 🎯 Section 10: Behavioral & Experience-Based

### Q31: Describe your GenAI PoC for exception summarization
**Expected Answer**:
- **Problem**: Manual triage of ingestion exceptions took hours
- **Solution**: GenAI (GPT-4) to auto-summarize exceptions
- **Implementation**:
  - Collect exception logs
  - Send to GPT-4 API with prompt
  - Generate root cause summary
  - Display in dashboard
- **Impact**: 60% reduction in triage time

---

### Q32: How do you ensure code quality in your team?
**Expected Answer**:
- Code reviews (PR reviews)
- Unit tests (JUnit, Mockito) - 80%+ coverage
- Integration tests
- SonarQube for code quality
- CI/CD pipeline with automated tests
- Coding standards and linting

---

### Q33: Explain a challenging bug you fixed
**Expected Answer**:
- **Problem**: Race condition in concurrent Kafka processing
- **Root Cause**: Multiple threads updating same database record
- **Solution**: Pessimistic locking with `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- **Learning**: Always consider concurrency in distributed systems

---

## 📊 Evaluation Criteria

| Area | Weight | Key Skills |
|------|--------|------------|
| Java & Spring Boot | 25% | Core concepts, best practices |
| Kafka & Event-Driven | 20% | Performance, reliability |
| Database & Caching | 15% | Optimization, design |
| AWS & Cloud | 15% | Deployment, CI/CD |
| Microservices | 10% | Architecture, patterns |
| Docker/Kubernetes | 10% | Containerization, orchestration |
| Problem Solving | 5% | Debugging, optimization |

---

## 🎯 Expected Proficiency Levels

### Must Know (Critical)
- ✅ Spring Boot fundamentals
- ✅ Kafka producer/consumer
- ✅ REST API design
- ✅ SQL and NoSQL basics
- ✅ Docker basics
- ✅ Git and CI/CD

### Should Know (Important)
- ✅ CompletableFuture and async programming
- ✅ Resilience patterns (Circuit Breaker, Retry)
- ✅ Redis caching strategies
- ✅ Kubernetes deployments
- ✅ AWS services (EKS, S3, CodePipeline)
- ✅ Microservices patterns

### Good to Know (Bonus)
- ✅ Kafka Streams
- ✅ Advanced Kubernetes (HPA, Ingress)
- ✅ Distributed tracing
- ✅ GenAI integration
- ✅ Performance tuning

---

## 💡 Interview Tips

1. **Relate to your experience**: Always connect answers to your healthcare/mobility projects
2. **Show impact**: Mention metrics (95% reduction, 40% improvement, 100K events/min)
3. **Explain trade-offs**: Discuss why you chose one approach over another
4. **Ask clarifying questions**: Show you think about requirements
5. **Draw diagrams**: Visualize architectures when explaining
6. **Discuss failures**: Share what you learned from mistakes
7. **Stay current**: Mention latest versions (Java 17, Spring Boot 3.x)

---

## 🔗 Related Documents

- [Kafka Interview Questions](./Kafka_Interview_Questions.md)
- [CompletableFuture Threading](./CompletableFuture_Threading_Explained.md)
- [Spring Boot Deadlock Monitoring](./Spring_Boot_Deadlock_Monitoring.md)
- [Database Scaling](./Database_Scaling_Read_Heavy_Ecommerce.md)
- [Docker & Kubernetes](./Docker_Kubernetes_Interview_Questions.md)
