# MongoDB Complete Guide - Part 5: Spring Boot Integration

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
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
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
    mongodb:
      uri: mongodb://localhost:27017/ecommerce
      connection-pool:
        max-size: 100
        min-size: 10
```

---

## Entity Modeling

### User Entity
```java
@Document(collection = "users")
@Data
@Builder
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String name;
    private Integer age;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
```

### Product Entity
```java
@Document(collection = "products")
@Data
@Builder
public class Product {
    @Id
    private String id;
    
    @Indexed
    private String sku;
    
    @TextIndexed
    private String name;
    
    private BigDecimal price;
    private Integer stock;
    
    @Indexed
    private String category;
    
    private List<String> tags;
}
```

---

## Repository Layer

```java
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByAgeGreaterThan(int age);
    boolean existsByEmail(String email);
}

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategoryAndStockGreaterThan(String category, int stock);
    
    @Query("{ 'category': ?0, 'stock': { $gt: 0 } }")
    List<Product> findAvailableByCategory(String category);
    
    Page<Product> findByCategory(String category, Pageable pageable);
}
```

---

## Service Layer

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email exists");
        }
        
        User user = User.builder()
            .email(request.getEmail())
            .name(request.getName())
            .age(request.getAge())
            .build();
        
        return userRepository.save(user);
    }
    
    public User getUserById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    public Product updateStock(String id, int quantity) {
        Query query = new Query(Criteria.where("_id").is(id).and("stock").gte(quantity));
        Update update = new Update().inc("stock", -quantity);
        
        Product product = mongoTemplate.findAndModify(
            query, update, 
            new FindAndModifyOptions().returnNew(true),
            Product.class
        );
        
        if (product == null) {
            throw new InsufficientStockException("Insufficient stock");
        }
        
        return product;
    }
}
```

---

## REST Controllers

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    
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
