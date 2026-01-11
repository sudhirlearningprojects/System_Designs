# Apache Cassandra Complete Guide - Part 4: Spring Boot Integration

## 📋 Table of Contents
1. [Project Setup](#project-setup)
2. [Entity Modeling](#entity-modeling)
3. [Repository Layer](#repository-layer)
4. [Service Layer](#service-layer)
5. [REST Controllers](#rest-controllers)

---

## Project Setup

### Maven Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-cassandra</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

### Configuration
```yaml
spring:
  data:
    cassandra:
      keyspace-name: ecommerce
      contact-points: localhost
      port: 9042
      local-datacenter: datacenter1
      schema-action: create_if_not_exists
      request:
        timeout: 10s
        consistency: LOCAL_QUORUM
```

### Cassandra Configuration Class
```java
@Configuration
@EnableCassandraRepositories(basePackages = "com.example.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {
    
    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspace;
    
    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;
    
    @Value("${spring.data.cassandra.port}")
    private int port;
    
    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }
    
    @Override
    protected String getContactPoints() {
        return contactPoints;
    }
    
    @Override
    protected int getPort() {
        return port;
    }
}
```

---

## Entity Modeling

### User Entity
```java
@Table("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @PrimaryKey
    private UUID userId;
    
    private String email;
    private String name;
    private Integer age;
    
    @Column("created_at")
    private Instant createdAt;
}
```

### Product Entity
```java
@Table("products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @PrimaryKey
    private UUID productId;
    
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String category;
    
    @CassandraType(type = CassandraType.Name.SET, typeArguments = CassandraType.Name.TEXT)
    private Set<String> tags;
    
    @Column("created_at")
    private Instant createdAt;
}
```

### Order Entity (Composite Primary Key)
```java
@Table("orders_by_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID userId;
    
    @PrimaryKeyColumn(name = "order_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID orderId;
    
    @Column("total_amount")
    private BigDecimal totalAmount;
    
    private String status;
    
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UDT, userTypeName = "order_item")
    private List<OrderItem> items;
    
    @Column("created_at")
    private Instant createdAt;
}

@UserDefinedType("order_item")
@Data
public class OrderItem {
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID productId;
    
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
```

### Time-Series Entity
```java
@Table("sensor_readings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {
    @PrimaryKeyColumn(name = "device_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID deviceId;
    
    @PrimaryKeyColumn(name = "reading_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED, 
                      ordering = Ordering.DESCENDING)
    private Instant readingTime;
    
    private Float temperature;
    private Float humidity;
    
    @Column("battery_level")
    private Integer batteryLevel;
}
```

---

## Repository Layer

### Basic Repository
```java
@Repository
public interface UserRepository extends CassandraRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

@Repository
public interface ProductRepository extends CassandraRepository<Product, UUID> {
    List<Product> findByCategory(String category);
}
```

### Custom Queries
```java
@Repository
public interface OrderRepository extends CassandraRepository<Order, OrderPrimaryKey> {
    
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0")
    List<Order> findByUserId(UUID userId);
    
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 AND order_id > ?1 LIMIT ?2")
    List<Order> findByUserIdAfterOrderId(UUID userId, UUID lastOrderId, int limit);
    
    @Query("SELECT * FROM orders_by_user WHERE user_id = ?0 ALLOW FILTERING")
    List<Order> findByUserIdWithFiltering(UUID userId);
}

@Repository
public interface SensorReadingRepository extends CassandraRepository<SensorReading, SensorReadingKey> {
    
    @Query("SELECT * FROM sensor_readings WHERE device_id = ?0 AND reading_time >= ?1 AND reading_time < ?2")
    List<SensorReading> findByDeviceIdAndTimeRange(UUID deviceId, Instant start, Instant end);
}
```

### Custom Repository Implementation
```java
public interface CustomProductRepository {
    List<Product> findByCategoryWithPagination(String category, int pageSize, String pagingState);
}

@Repository
public class CustomProductRepositoryImpl implements CustomProductRepository {
    
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    @Override
    public List<Product> findByCategoryWithPagination(String category, int pageSize, String pagingState) {
        Select select = QueryBuilder.selectFrom("products")
            .all()
            .whereColumn("category").isEqualTo(QueryBuilder.literal(category))
            .limit(pageSize);
        
        SimpleStatement statement = select.build();
        
        if (pagingState != null) {
            statement = statement.setPagingState(ByteBuffer.wrap(pagingState.getBytes()));
        }
        
        return cassandraTemplate.select(statement, Product.class);
    }
}
```

---

## Service Layer

### User Service
```java
@Service
@Slf4j
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(UserRequest request) {
        User user = User.builder()
            .userId(UUID.randomUUID())
            .email(request.getEmail())
            .name(request.getName())
            .age(request.getAge())
            .createdAt(Instant.now())
            .build();
        
        return userRepository.save(user);
    }
    
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

### Product Service
```java
@Service
@Slf4j
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
            .productId(UUID.randomUUID())
            .sku(request.getSku())
            .name(request.getName())
            .price(request.getPrice())
            .stock(request.getStock())
            .category(request.getCategory())
            .tags(request.getTags())
            .createdAt(Instant.now())
            .build();
        
        return productRepository.save(product);
    }
    
    public Product updateStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock");
        }
        
        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
```

### Order Service
```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    public Order createOrder(OrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.updateStock(
                itemRequest.getProductId(),
                itemRequest.getQuantity()
            );
            
            OrderItem item = new OrderItem();
            item.setProductId(product.getProductId());
            item.setProductName(product.getName());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(product.getPrice());
            
            items.add(item);
            totalAmount = totalAmount.add(
                product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }
        
        Order order = Order.builder()
            .userId(request.getUserId())
            .orderId(UUIDs.timeBased())
            .items(items)
            .totalAmount(totalAmount)
            .status("PENDING")
            .createdAt(Instant.now())
            .build();
        
        // Write to multiple tables (denormalization)
        orderRepository.save(order);
        saveOrderByStatus(order);
        
        return order;
    }
    
    private void saveOrderByStatus(Order order) {
        String cql = "INSERT INTO orders_by_status (status, order_id, user_id, total_amount, created_at) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        cassandraTemplate.getCqlOperations().execute(
            cql,
            order.getStatus(),
            order.getOrderId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }
    
    public List<Order> getUserOrders(UUID userId) {
        return orderRepository.findByUserId(userId);
    }
}
```

### Sensor Service (Time-Series)
```java
@Service
@Slf4j
public class SensorService {
    
    @Autowired
    private SensorReadingRepository sensorReadingRepository;
    
    public SensorReading recordReading(SensorReadingRequest request) {
        SensorReading reading = SensorReading.builder()
            .deviceId(request.getDeviceId())
            .readingTime(Instant.now())
            .temperature(request.getTemperature())
            .humidity(request.getHumidity())
            .batteryLevel(request.getBatteryLevel())
            .build();
        
        return sensorReadingRepository.save(reading);
    }
    
    public List<SensorReading> getReadings(UUID deviceId, Instant start, Instant end) {
        return sensorReadingRepository.findByDeviceIdAndTimeRange(deviceId, start, end);
    }
    
    public List<SensorReading> getLastHourReadings(UUID deviceId) {
        Instant end = Instant.now();
        Instant start = end.minus(1, ChronoUnit.HOURS);
        return getReadings(deviceId, start, end);
    }
}
```

---

## REST Controllers

### User Controller
```java
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
```

### Product Controller
```java
@RestController
@RequestMapping("/api/v1/products")
@Slf4j
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID productId) {
        Product product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping
    public ResponseEntity<List<Product>> getProductsByCategory(@RequestParam String category) {
        List<Product> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
}
```

### Order Controller
```java
@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable UUID userId) {
        List<Order> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }
}
```

---

## Advanced Features

### Batch Operations
```java
@Service
public class BatchService {
    
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    public void batchInsertOrders(List<Order> orders) {
        BatchStatementBuilder batch = BatchStatement.builder(BatchType.UNLOGGED);
        
        for (Order order : orders) {
            SimpleStatement statement = QueryBuilder.insertInto("orders_by_user")
                .value("user_id", QueryBuilder.literal(order.getUserId()))
                .value("order_id", QueryBuilder.literal(order.getOrderId()))
                .value("total_amount", QueryBuilder.literal(order.getTotalAmount()))
                .value("status", QueryBuilder.literal(order.getStatus()))
                .build();
            
            batch.addStatement(statement);
        }
        
        cassandraTemplate.getCqlOperations().execute(batch.build());
    }
}
```

### Async Operations
```java
@Service
public class AsyncService {
    
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    @Async
    public CompletableFuture<User> getUserAsync(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            Select select = QueryBuilder.selectFrom("users")
                .all()
                .whereColumn("user_id").isEqualTo(QueryBuilder.literal(userId));
            
            return cassandraTemplate.selectOne(select.build(), User.class);
        });
    }
}
```
