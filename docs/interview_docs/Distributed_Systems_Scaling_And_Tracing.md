# Distributed Systems Scaling & Distributed Tracing

## Part 1: Scaling in Distributed Systems

### What is a Distributed System?

A system where components located on networked computers communicate and coordinate to achieve a common goal.

**Example**:
```
User Request
     ↓
API Gateway
     ↓
┌────┴────┬────────┬────────┐
↓         ↓        ↓        ↓
Auth    Order   Payment  Notification
Service Service Service  Service
```

---

## Scaling Strategies in Distributed Systems

### 1. Load Balancing

**Purpose**: Distribute traffic across multiple servers.

**Architecture**:
```
         Users
           ↓
    ┌──────────────┐
    │Load Balancer │
    └──────────────┘
           ↓
    ┌──────┼──────┐
    ↓      ↓      ↓
Server 1 Server 2 Server 3
```

**Algorithms**:

**Round Robin**:
```
Request 1 → Server 1
Request 2 → Server 2
Request 3 → Server 3
Request 4 → Server 1 (repeat)
```

**Least Connections**:
```
Server 1: 10 connections
Server 2: 5 connections  ← Route here
Server 3: 8 connections
```

**IP Hash**:
```
hash(client_ip) % num_servers
Same client always goes to same server
```

**Implementation (Nginx)**:
```nginx
upstream backend {
    least_conn;  # Load balancing algorithm
    server backend1.example.com;
    server backend2.example.com;
    server backend3.example.com;
}

server {
    location / {
        proxy_pass http://backend;
    }
}
```

---

### 2. Database Scaling

#### A. Read Replicas

**Architecture**:
```
Application
     ↓
┌────┴────┐
↓         ↓
Write    Read
  ↓       ↓
Master  Replica 1
  ↓     Replica 2
  └──→  Replica 3
```

**Code Example**:
```java
@Service
public class UserService {
    
    @Autowired
    @Qualifier("masterDataSource")
    private DataSource masterDB;
    
    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDB;
    
    // Write to master
    public void createUser(User user) {
        JdbcTemplate master = new JdbcTemplate(masterDB);
        master.update("INSERT INTO users VALUES (?, ?)", 
            user.getId(), user.getName());
    }
    
    // Read from replica
    public User getUser(Long id) {
        JdbcTemplate replica = new JdbcTemplate(replicaDB);
        return replica.queryForObject(
            "SELECT * FROM users WHERE id = ?", 
            new Object[]{id}, 
            new UserRowMapper()
        );
    }
}
```

---

#### B. Database Sharding

**Horizontal Partitioning**: Split data across multiple databases.

**Architecture**:
```
Application
     ↓
Shard Router
     ↓
┌────┼────┬────┐
↓    ↓    ↓    ↓
DB1  DB2  DB3  DB4
(Users (Users (Users (Users
1-1M) 1M-2M) 2M-3M) 3M-4M)
```

**Sharding Strategies**:

**Range-Based**:
```java
public class RangeSharding {
    public DataSource getShard(Long userId) {
        if (userId <= 1_000_000) return shard1;
        if (userId <= 2_000_000) return shard2;
        if (userId <= 3_000_000) return shard3;
        return shard4;
    }
}
```

**Hash-Based**:
```java
public class HashSharding {
    private List<DataSource> shards;
    
    public DataSource getShard(Long userId) {
        int shardIndex = (int) (userId % shards.size());
        return shards.get(shardIndex);
    }
}
```

**Consistent Hashing**:
```java
public class ConsistentHashing {
    private TreeMap<Integer, DataSource> ring = new TreeMap<>();
    
    public void addShard(DataSource shard) {
        for (int i = 0; i < 150; i++) { // Virtual nodes
            int hash = hash(shard.toString() + i);
            ring.put(hash, shard);
        }
    }
    
    public DataSource getShard(Long userId) {
        int hash = hash(userId.toString());
        Map.Entry<Integer, DataSource> entry = ring.ceilingEntry(hash);
        return entry != null ? entry.getValue() : ring.firstEntry().getValue();
    }
}
```

---

### 3. Caching Layers

**Multi-Level Caching**:
```
Request
  ↓
L1: Application Cache (Local)
  ↓ (miss)
L2: Redis Cache (Distributed)
  ↓ (miss)
L3: Database
```

**Implementation**:
```java
@Service
public class ProductService {
    
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    @Autowired
    private ProductRepository repository;
    
    private LoadingCache<Long, Product> localCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(new CacheLoader<Long, Product>() {
            public Product load(Long id) {
                return getFromRedisOrDB(id);
            }
        });
    
    public Product getProduct(Long id) {
        try {
            // L1: Local cache
            return localCache.get(id);
        } catch (ExecutionException e) {
            return null;
        }
    }
    
    private Product getFromRedisOrDB(Long id) {
        // L2: Redis cache
        String key = "product:" + id;
        Product product = redisTemplate.opsForValue().get(key);
        
        if (product != null) {
            return product;
        }
        
        // L3: Database
        product = repository.findById(id).orElse(null);
        if (product != null) {
            redisTemplate.opsForValue().set(key, product, 1, TimeUnit.HOURS);
        }
        
        return product;
    }
}
```

---

### 4. Message Queues (Async Processing)

**Architecture**:
```
API Server → Kafka → Worker 1
                  → Worker 2
                  → Worker 3
```

**Producer**:
```java
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    public void createOrder(Order order) {
        // Save to DB
        orderRepository.save(order);
        
        // Send to queue for async processing
        kafkaTemplate.send("orders", order.getId().toString(), order);
        
        // Return immediately (don't wait for processing)
        return;
    }
}
```

**Consumer**:
```java
@Service
public class OrderProcessor {
    
    @KafkaListener(topics = "orders", groupId = "order-processors")
    public void processOrder(Order order) {
        // Process order asynchronously
        sendConfirmationEmail(order);
        updateInventory(order);
        notifyWarehouse(order);
    }
}
```

**Benefit**: API responds in 50ms, processing takes 5 seconds in background.

---

### 5. Service Mesh (Microservices Scaling)

**Architecture with Istio**:
```
Service A → Envoy Proxy → Service B
                ↓
         Control Plane (Istio)
         - Load Balancing
         - Circuit Breaking
         - Retry Logic
         - Tracing
```

**Configuration**:
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: order-service
spec:
  hosts:
  - order-service
  http:
  - route:
    - destination:
        host: order-service
        subset: v1
      weight: 90
    - destination:
        host: order-service
        subset: v2
      weight: 10  # Canary deployment
```

---

### 6. Auto-Scaling

**Horizontal Pod Autoscaler (Kubernetes)**:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**Behavior**:
```
CPU Usage > 70%
  ↓
Scale up: 3 pods → 5 pods
  ↓
CPU Usage < 50%
  ↓
Scale down: 5 pods → 3 pods
```

---

## Part 2: Distributed Tracing

### What is Distributed Tracing?

Track a request as it flows through multiple services in a distributed system.

**Problem Without Tracing**:
```
User: "My order is slow!"
Developer: "Which service is slow? 🤷"
```

**With Tracing**:
```
Request took 5 seconds:
- API Gateway: 10ms
- Auth Service: 50ms
- Order Service: 100ms
- Payment Service: 4800ms ← FOUND THE PROBLEM!
- Notification: 40ms
```

---

### Key Concepts

**Trace**: End-to-end journey of a request  
**Span**: Single operation within a trace  
**Trace ID**: Unique identifier for entire request  
**Span ID**: Unique identifier for each operation

**Visual**:
```
Trace ID: abc123
├─ Span 1: API Gateway (10ms)
│  └─ Span 2: Auth Service (50ms)
│     └─ Span 3: Order Service (100ms)
│        ├─ Span 4: Database Query (30ms)
│        └─ Span 5: Payment Service (4800ms)
│           └─ Span 6: External API (4700ms)
└─ Span 7: Notification (40ms)
```

---

## Distributed Tracing Technologies

### 1. Zipkin

**Architecture**:
```
Service A → Zipkin Client → Zipkin Collector
Service B → Zipkin Client → Zipkin Collector
Service C → Zipkin Client → Zipkin Collector
                                  ↓
                            Storage (Cassandra/ES)
                                  ↓
                              Zipkin UI
```

**Setup (Spring Boot)**:

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

**application.properties**:
```properties
spring.zipkin.base-url=http://localhost:9411
spring.sleuth.sampler.probability=1.0
```

**Code (Automatic)**:
```java
@RestController
public class OrderController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @GetMapping("/order/{id}")
    public Order getOrder(@PathVariable Long id) {
        // Trace automatically propagated
        Order order = orderService.getOrder(id);
        
        // Call payment service - trace continues
        Payment payment = restTemplate.getForObject(
            "http://payment-service/payment/" + order.getPaymentId(),
            Payment.class
        );
        
        return order;
    }
}
```

**Trace Headers Propagated**:
```
X-B3-TraceId: abc123
X-B3-SpanId: def456
X-B3-ParentSpanId: ghi789
```

---

### 2. Jaeger

**Architecture**:
```
Service A → Jaeger Agent → Jaeger Collector
Service B → Jaeger Agent → Jaeger Collector
Service C → Jaeger Agent → Jaeger Collector
                                  ↓
                            Storage (Cassandra/ES)
                                  ↓
                              Jaeger UI
```

**Setup (Spring Boot)**:

**pom.xml**:
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-spring-jaeger-web-starter</artifactId>
    <version>3.3.1</version>
</dependency>
```

**application.properties**:
```properties
opentracing.jaeger.service-name=order-service
opentracing.jaeger.udp-sender.host=localhost
opentracing.jaeger.udp-sender.port=6831
opentracing.jaeger.sampler.type=const
opentracing.jaeger.sampler.param=1
```

**Manual Instrumentation**:
```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public Order processOrder(Long orderId) {
        // Create custom span
        Span span = tracer.buildSpan("processOrder").start();
        try {
            span.setTag("orderId", orderId);
            
            // Business logic
            Order order = getOrderFromDB(orderId);
            
            // Create child span
            Span childSpan = tracer.buildSpan("validateOrder")
                .asChildOf(span)
                .start();
            try {
                validateOrder(order);
            } finally {
                childSpan.finish();
            }
            
            return order;
        } catch (Exception e) {
            span.setTag("error", true);
            span.log(Map.of("event", "error", "message", e.getMessage()));
            throw e;
        } finally {
            span.finish();
        }
    }
}
```

---

### 3. AWS X-Ray

**Architecture**:
```
Service A → X-Ray SDK → X-Ray Daemon → X-Ray Service
Service B → X-Ray SDK → X-Ray Daemon → X-Ray Service
Service C → X-Ray SDK → X-Ray Daemon → X-Ray Service
                                            ↓
                                      X-Ray Console
```

**Setup (Spring Boot)**:

**pom.xml**:
```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-xray-recorder-sdk-spring</artifactId>
    <version>2.11.0</version>
</dependency>
```

**Configuration**:
```java
@Configuration
public class XRayConfig {
    
    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("order-service");
    }
}
```

**Code**:
```java
@Service
public class OrderService {
    
    public Order getOrder(Long id) {
        // Create subsegment
        Subsegment subsegment = AWSXRay.beginSubsegment("getOrderFromDB");
        try {
            subsegment.putAnnotation("orderId", id);
            Order order = orderRepository.findById(id).orElse(null);
            subsegment.putMetadata("order", order);
            return order;
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }
}
```

---

### 4. OpenTelemetry (Standard)

**Architecture**:
```
Service A → OTel SDK → OTel Collector → Backend (Jaeger/Zipkin/etc)
Service B → OTel SDK → OTel Collector → Backend
Service C → OTel SDK → OTel Collector → Backend
```

**Setup (Spring Boot)**:

**pom.xml**:
```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.31.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.31.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
    <version>1.31.0</version>
</dependency>
```

**Configuration**:
```java
@Configuration
public class OpenTelemetryConfig {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:14250")
            .build();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
            .setResource(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "order-service"
            )))
            .build();
        
        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();
    }
}
```

**Code**:
```java
@Service
public class OrderService {
    
    private final Tracer tracer;
    
    public OrderService(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("order-service");
    }
    
    public Order processOrder(Long orderId) {
        Span span = tracer.spanBuilder("processOrder").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("orderId", orderId);
            
            // Business logic
            Order order = getOrderFromDB(orderId);
            
            // Create child span
            Span childSpan = tracer.spanBuilder("validateOrder")
                .setParent(Context.current())
                .startSpan();
            try (Scope childScope = childSpan.makeCurrent()) {
                validateOrder(order);
            } finally {
                childSpan.end();
            }
            
            return order;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

### 5. Elastic APM

**Architecture**:
```
Service A → APM Agent → APM Server → Elasticsearch
Service B → APM Agent → APM Server → Elasticsearch
Service C → APM Agent → APM Server → Elasticsearch
                                          ↓
                                      Kibana UI
```

**Setup (Spring Boot)**:

**pom.xml**:
```xml
<dependency>
    <groupId>co.elastic.apm</groupId>
    <artifactId>apm-agent-attach</artifactId>
    <version>1.39.0</version>
</dependency>
```

**Application Startup**:
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        ElasticApmAttacher.attach();
        SpringApplication.run(Application.class, args);
    }
}
```

**elasticapm.properties**:
```properties
service_name=order-service
server_url=http://localhost:8200
application_packages=com.example.order
```

**Code (Automatic + Manual)**:
```java
@Service
public class OrderService {
    
    public Order processOrder(Long orderId) {
        // Automatic tracing
        Order order = orderRepository.findById(orderId).orElse(null);
        
        // Manual span
        Span span = ElasticApm.currentSpan()
            .startSpan("external", "payment", "http");
        try {
            span.setName("Call Payment Service");
            Payment payment = callPaymentService(order);
            return order;
        } finally {
            span.end();
        }
    }
}
```

---

## Complete Example: E-commerce with Tracing

**Architecture**:
```
User → API Gateway → Order Service → Payment Service
                  → Inventory Service
                  → Notification Service
```

**Order Service**:
```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private Tracer tracer;
    
    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request) {
        Span span = tracer.buildSpan("createOrder").start();
        try {
            span.setTag("userId", request.getUserId());
            span.setTag("items", request.getItems().size());
            
            // 1. Create order
            Order order = orderService.create(request);
            
            // 2. Call payment service
            Span paymentSpan = tracer.buildSpan("callPaymentService")
                .asChildOf(span)
                .start();
            try {
                Payment payment = restTemplate.postForObject(
                    "http://payment-service/payments",
                    new PaymentRequest(order.getId(), order.getTotal()),
                    Payment.class
                );
                order.setPaymentId(payment.getId());
            } finally {
                paymentSpan.finish();
            }
            
            // 3. Update inventory
            Span inventorySpan = tracer.buildSpan("updateInventory")
                .asChildOf(span)
                .start();
            try {
                restTemplate.postForObject(
                    "http://inventory-service/inventory/reserve",
                    request.getItems(),
                    Void.class
                );
            } finally {
                inventorySpan.finish();
            }
            
            // 4. Send notification (async)
            Span notificationSpan = tracer.buildSpan("sendNotification")
                .asChildOf(span)
                .start();
            try {
                kafkaTemplate.send("notifications", 
                    new OrderCreatedEvent(order.getId()));
            } finally {
                notificationSpan.finish();
            }
            
            return order;
        } catch (Exception e) {
            span.setTag("error", true);
            span.log(Map.of("event", "error", "message", e.getMessage()));
            throw e;
        } finally {
            span.finish();
        }
    }
}
```

**Trace Output**:
```
Trace ID: abc123 (Total: 850ms)
├─ createOrder (850ms)
   ├─ callPaymentService (500ms)
   │  └─ HTTP POST /payments (480ms)
   ├─ updateInventory (200ms)
   │  └─ HTTP POST /inventory/reserve (180ms)
   └─ sendNotification (50ms)
      └─ Kafka send (30ms)
```

---

## Comparison of Tracing Tools

| Tool | Pros | Cons | Best For |
|------|------|------|----------|
| **Zipkin** | Simple, lightweight | Basic features | Small projects |
| **Jaeger** | Feature-rich, CNCF | Complex setup | Kubernetes |
| **AWS X-Ray** | AWS integration | AWS only | AWS workloads |
| **OpenTelemetry** | Vendor-neutral, standard | New, evolving | Future-proof |
| **Elastic APM** | Full observability stack | Heavy | Existing ELK users |

---

## Best Practices

### Scaling

1. **Start Simple**: Vertical scaling first
2. **Add Load Balancer**: Distribute traffic
3. **Cache Aggressively**: Reduce DB load
4. **Async Processing**: Use message queues
5. **Database Scaling**: Read replicas, then sharding
6. **Auto-Scale**: Use Kubernetes HPA
7. **Monitor**: Track metrics continuously

### Tracing

1. **Sample Wisely**: 100% in dev, 1-10% in prod
2. **Add Context**: Tags, logs, metadata
3. **Trace Critical Paths**: Focus on user-facing flows
4. **Set Alerts**: Slow traces, error rates
5. **Correlate Logs**: Include trace ID in logs
6. **Monitor Overhead**: Tracing adds latency
7. **Use Standards**: OpenTelemetry for future-proofing

---

## Key Takeaways

**Scaling**:
1. Load balancing distributes traffic
2. Caching reduces database load
3. Sharding splits data horizontally
4. Message queues enable async processing
5. Auto-scaling adjusts to demand

**Tracing**:
1. Tracks requests across services
2. Identifies performance bottlenecks
3. Helps debug distributed systems
4. Essential for microservices
5. OpenTelemetry is the future standard

**Bottom Line**: Scale horizontally with load balancers and caching. Use distributed tracing to monitor and debug!
