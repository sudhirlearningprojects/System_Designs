# PostgreSQL Complete Guide - Part 5: Spring Boot Integration

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
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
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
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce
    username: app_user
    password: password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

---

## Entity Modeling

### User Entity
```java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    private Integer age;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### Product Entity
```java
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String sku;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    private Integer stock;
    
    private String category;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### Order Entity
```java
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    private Integer quantity;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
}

enum OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
```

---

## Repository Layer

### Basic Repository
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByAgeGreaterThan(Integer age);
    boolean existsByEmail(String email);
}

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    List<Product> findByCategoryAndStockGreaterThan(String category, Integer stock);
    Page<Product> findByCategory(String category, Pageable pageable);
}
```

### Custom Queries
```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query(value = "SELECT SUM(total_amount) FROM orders WHERE user_id = :userId", 
           nativeQuery = true)
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);
    
    @Query("SELECT new com.example.dto.OrderStats(o.user.id, COUNT(o), SUM(o.totalAmount)) " +
           "FROM Order o WHERE o.status = 'DELIVERED' " +
           "GROUP BY o.user.id ORDER BY SUM(o.totalAmount) DESC")
    List<OrderStats> getTopCustomers(Pageable pageable);
}
```

### Specifications (Dynamic Queries)
```java
public class ProductSpecifications {
    
    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> 
            category == null ? null : cb.equal(root.get("category"), category);
    }
    
    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("price"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.between(root.get("price"), min, max);
        };
    }
    
    public static Specification<Product> stockGreaterThan(Integer stock) {
        return (root, query, cb) -> 
            stock == null ? null : cb.greaterThan(root.get("stock"), stock);
    }
}

// Usage
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, 
                                           JpaSpecificationExecutor<Product> {
}

// Service
List<Product> products = productRepository.findAll(
    Specification.where(ProductSpecifications.hasCategory("electronics"))
        .and(ProductSpecifications.priceBetween(100.0, 1000.0))
        .and(ProductSpecifications.stockGreaterThan(0))
);
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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }
        
        User user = User.builder()
            .email(request.getEmail())
            .name(request.getName())
            .age(request.getAge())
            .build();
        
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public Page<User> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
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
    private EntityManager entityManager;
    
    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
            .sku(request.getSku())
            .name(request.getName())
            .price(request.getPrice())
            .stock(request.getStock())
            .category(request.getCategory())
            .build();
        
        return productRepository.save(product);
    }
    
    @Transactional
    public Product updateStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock");
        }
        
        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }
    
    @Transactional
    public Product updateStockWithLock(Long id, int quantity) {
        // Pessimistic locking
        Product product = entityManager.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE);
        
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }
        
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock");
        }
        
        product.setStock(product.getStock() - quantity);
        return product;
    }
}
```

### Order Service with Transactions
```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order createOrder(OrderRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        Order order = Order.builder()
            .user(user)
            .status(OrderStatus.PENDING)
            .items(new ArrayList<>())
            .build();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.updateStockWithLock(
                itemRequest.getProductId(),
                itemRequest.getQuantity()
            );
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(product.getPrice());
            
            order.getItems().add(item);
            totalAmount = totalAmount.add(
                product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }
        
        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }
    
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findByIdWithItems(id)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
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
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userService.getUsers(page, size);
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
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getProducts(category, pageable);
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
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
}
```
