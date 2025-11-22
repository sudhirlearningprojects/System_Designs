# Reactive Data Access with R2DBC

## Overview

R2DBC (Reactive Relational Database Connectivity) provides reactive, non-blocking database access for relational databases.

## Supported Databases

- PostgreSQL
- MySQL
- Microsoft SQL Server
- H2
- Oracle

## Setup

### Dependencies

```xml
<dependencies>
    <!-- R2DBC -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>
    
    <!-- PostgreSQL R2DBC Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>
    
    <!-- Connection Pool -->
    <dependency>
        <groupId>io.r2dbc</groupId>
        <artifactId>r2dbc-pool</artifactId>
    </dependency>
</dependencies>
```

### Configuration

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/mydb
    username: postgres
    password: password
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      max-acquire-time: 3s
      max-create-connection-time: 5s
```

## Entity Mapping

```java
@Data
@Table("users")
public class User {
    
    @Id
    private Long id;
    
    @Column("username")
    private String username;
    
    @Column("email")
    private String email;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    private List<Order> orders; // Not persisted
}
```

## Repository Interface

### Basic CRUD

```java
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    
    // Derived query methods
    Mono<User> findByUsername(String username);
    
    Flux<User> findByEmailContaining(String email);
    
    Mono<Boolean> existsByUsername(String username);
    
    Mono<Long> countByCreatedAtAfter(LocalDateTime date);
}
```

### Custom Queries

```java
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    
    @Query("SELECT * FROM users WHERE username = :username")
    Mono<User> findByUsernameCustom(@Param("username") String username);
    
    @Query("SELECT * FROM users WHERE created_at > :date ORDER BY created_at DESC")
    Flux<User> findRecentUsers(@Param("date") LocalDateTime date);
    
    @Modifying
    @Query("UPDATE users SET email = :email WHERE id = :id")
    Mono<Integer> updateEmail(@Param("id") Long id, @Param("email") String email);
    
    @Modifying
    @Query("DELETE FROM users WHERE created_at < :date")
    Mono<Integer> deleteOldUsers(@Param("date") LocalDateTime date);
}
```

## Service Layer

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public Mono<User> createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    public Mono<User> getUserById(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }
    
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Mono<User> updateUser(Long id, User user) {
        return userRepository.findById(id)
            .flatMap(existing -> {
                existing.setUsername(user.getUsername());
                existing.setEmail(user.getEmail());
                existing.setUpdatedAt(LocalDateTime.now());
                return userRepository.save(existing);
            })
            .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }
    
    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }
}
```

## Advanced Queries

### Pagination

```java
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    
    @Query("SELECT * FROM users ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<User> findAllPaginated(@Param("limit") int limit, @Param("offset") int offset);
}

// Service
public Mono<Page<User>> getUsers(int page, int size) {
    int offset = page * size;
    
    return userRepository.findAllPaginated(size, offset)
        .collectList()
        .zipWith(userRepository.count())
        .map(tuple -> new PageImpl<>(tuple.getT1(), PageRequest.of(page, size), tuple.getT2()));
}
```

### Sorting

```java
public Flux<User> getUsersSorted(String sortBy, String direction) {
    Sort.Direction dir = direction.equalsIgnoreCase("asc") 
        ? Sort.Direction.ASC 
        : Sort.Direction.DESC;
    
    return userRepository.findAll(Sort.by(dir, sortBy));
}
```

### Complex Filters

```java
@Query("""
    SELECT * FROM users 
    WHERE (:username IS NULL OR username LIKE :username)
    AND (:email IS NULL OR email LIKE :email)
    AND (:fromDate IS NULL OR created_at >= :fromDate)
    AND (:toDate IS NULL OR created_at <= :toDate)
    """)
Flux<User> findByFilters(
    @Param("username") String username,
    @Param("email") String email,
    @Param("fromDate") LocalDateTime fromDate,
    @Param("toDate") LocalDateTime toDate
);
```

## Transactions

### Declarative Transactions

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    
    @Transactional
    public Mono<Order> createOrder(Order order) {
        return orderRepository.save(order)
            .flatMap(savedOrder -> 
                inventoryRepository.decrementStock(order.getProductId(), order.getQuantity())
                    .thenReturn(savedOrder)
            );
    }
}
```

### Programmatic Transactions

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ReactiveTransactionManager transactionManager;
    
    public Mono<Order> createOrder(Order order) {
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);
        
        return orderRepository.save(order)
            .flatMap(savedOrder -> 
                inventoryRepository.decrementStock(order.getProductId(), order.getQuantity())
                    .thenReturn(savedOrder)
            )
            .as(operator::transactional);
    }
}
```

## DatabaseClient for Custom Queries

```java
@Service
@RequiredArgsConstructor
public class CustomUserService {
    
    private final DatabaseClient databaseClient;
    
    public Flux<User> findUsersByCustomCriteria(String criteria) {
        return databaseClient.sql(
            "SELECT * FROM users WHERE username LIKE :criteria OR email LIKE :criteria"
        )
        .bind("criteria", "%" + criteria + "%")
        .map((row, metadata) -> {
            User user = new User();
            user.setId(row.get("id", Long.class));
            user.setUsername(row.get("username", String.class));
            user.setEmail(row.get("email", String.class));
            return user;
        })
        .all();
    }
    
    public Mono<Integer> updateUserStatus(Long userId, String status) {
        return databaseClient.sql(
            "UPDATE users SET status = :status WHERE id = :id"
        )
        .bind("status", status)
        .bind("id", userId)
        .fetch()
        .rowsUpdated();
    }
}
```

## Relationships

### One-to-Many

```java
@Data
@Table("orders")
public class Order {
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    private BigDecimal amount;
    
    @Transient
    private User user;
}

// Service to load relationships
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    
    public Mono<Order> getOrderWithUser(Long orderId) {
        return orderRepository.findById(orderId)
            .flatMap(order -> 
                userRepository.findById(order.getUserId())
                    .map(user -> {
                        order.setUser(user);
                        return order;
                    })
            );
    }
    
    public Flux<Order> getOrdersWithUsers() {
        return orderRepository.findAll()
            .flatMap(order -> 
                userRepository.findById(order.getUserId())
                    .map(user -> {
                        order.setUser(user);
                        return order;
                    })
            );
    }
}
```

### Many-to-Many

```java
@Data
@Table("users")
public class User {
    @Id
    private Long id;
    private String username;
    
    @Transient
    private List<Role> roles;
}

@Data
@Table("roles")
public class Role {
    @Id
    private Long id;
    private String name;
}

// Service
@Service
@RequiredArgsConstructor
public class UserRoleService {
    
    private final DatabaseClient databaseClient;
    
    public Mono<User> getUserWithRoles(Long userId) {
        Mono<User> user = databaseClient.sql("SELECT * FROM users WHERE id = :id")
            .bind("id", userId)
            .map((row, metadata) -> mapToUser(row))
            .one();
        
        Flux<Role> roles = databaseClient.sql("""
            SELECT r.* FROM roles r
            JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            """)
            .bind("userId", userId)
            .map((row, metadata) -> mapToRole(row))
            .all();
        
        return user.zipWith(roles.collectList())
            .map(tuple -> {
                User u = tuple.getT1();
                u.setRoles(tuple.getT2());
                return u;
            });
    }
}
```

## Reactive MongoDB

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
</dependency>
```

### Configuration

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
      auto-index-creation: true
```

### Entity

```java
@Document(collection = "products")
@Data
public class Product {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String sku;
    
    private String name;
    private String description;
    private BigDecimal price;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    private List<String> tags;
}
```

### Repository

```java
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
    
    Flux<Product> findByNameContaining(String name);
    
    Flux<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    Flux<Product> findByTagsContaining(String tag);
    
    @Query("{ 'price': { $gte: ?0, $lte: ?1 } }")
    Flux<Product> findByPriceRange(BigDecimal min, BigDecimal max);
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public Mono<Product> createProduct(Product product) {
        product.setCreatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }
    
    public Flux<Product> searchProducts(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }
    
    public Flux<Product> getProductsByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max);
    }
}
```

## Caching with Redis

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

### Configuration

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### Service with Caching

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final ReactiveRedisTemplate<String, User> redisTemplate;
    
    public Mono<User> getUserById(Long id) {
        String key = "user:" + id;
        
        return redisTemplate.opsForValue().get(key)
            .switchIfEmpty(
                userRepository.findById(id)
                    .flatMap(user -> 
                        redisTemplate.opsForValue()
                            .set(key, user, Duration.ofMinutes(10))
                            .thenReturn(user)
                    )
            );
    }
    
    public Mono<User> updateUser(Long id, User user) {
        return userRepository.save(user)
            .flatMap(updated -> {
                String key = "user:" + id;
                return redisTemplate.delete(key)
                    .thenReturn(updated);
            });
    }
}
```

## Performance Optimization

### Batch Operations

```java
public Flux<User> createUsers(List<User> users) {
    return userRepository.saveAll(users);
}

public Mono<Void> deleteUsers(List<Long> ids) {
    return userRepository.deleteAllById(ids);
}
```

### Parallel Processing

```java
public Flux<Order> processOrders(List<Long> orderIds) {
    return Flux.fromIterable(orderIds)
        .parallel()
        .runOn(Schedulers.parallel())
        .flatMap(orderRepository::findById)
        .flatMap(this::processOrder)
        .sequential();
}
```

### Connection Pooling

```java
@Configuration
public class R2dbcConfig {
    
    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(DRIVER, "postgresql")
            .option(HOST, "localhost")
            .option(PORT, 5432)
            .option(USER, "postgres")
            .option(PASSWORD, "password")
            .option(DATABASE, "mydb")
            .build();
        
        ConnectionFactory connectionFactory = ConnectionFactories.get(options);
        
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
            .maxIdleTime(Duration.ofMinutes(30))
            .initialSize(10)
            .maxSize(50)
            .maxAcquireTime(Duration.ofSeconds(3))
            .maxCreateConnectionTime(Duration.ofSeconds(5))
            .build();
        
        return new ConnectionPool(poolConfig);
    }
}
```

## Database Initialization

```java
@Configuration
public class DatabaseInitializer {
    
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        
        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
        populator.addPopulators(new ResourceDatabasePopulator(
            new ClassPathResource("schema.sql")
        ));
        populator.addPopulators(new ResourceDatabasePopulator(
            new ClassPathResource("data.sql")
        ));
        
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
```

## Best Practices

1. **Use connection pooling**: Configure appropriate pool sizes
2. **Avoid blocking calls**: Never use blocking JDBC drivers
3. **Handle empty results**: Use switchIfEmpty()
4. **Use transactions wisely**: Only when necessary
5. **Optimize queries**: Use indexes and proper WHERE clauses
6. **Cache frequently accessed data**: Use Redis for hot data
7. **Batch operations**: Use saveAll() for multiple inserts
8. **Monitor performance**: Track query execution times

## Next Steps

- [Error Handling](Error_Handling.md) - Database error handling
- [Testing](Testing.md) - Testing reactive repositories
- [Performance](Performance.md) - Optimization strategies
