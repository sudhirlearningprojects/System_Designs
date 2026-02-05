# Spring Data JPA - Part 4: Advanced Features

[← Back to Index](README.md) | [← Previous: Part 3](05_Spring_Data_JPA_Part3.md) | [Next: Spring WebFlux →](07_Spring_WebFlux.md)

## Table of Contents
- [Pagination & Sorting](#pagination--sorting)
- [@Transactional](#transactional)
- [Locking](#locking)
- [Auditing](#auditing)

---

## Pagination & Sorting

### Understanding Pagination

**Why Pagination?**
- Reduce memory usage
- Improve response time
- Better user experience

### Page vs Slice vs List

| Feature | List | Slice | Page |
|---------|------|-------|------|
| All results | ✅ | ❌ | ❌ |
| Count query | ❌ | ❌ | ✅ |
| Total pages | ❌ | ❌ | ✅ |
| Has next | ❌ | ✅ | ✅ |
| Performance | Slow | Fast | Medium |

### Basic Pagination

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findAll(Pageable pageable);
    Slice<User> findByAge(int age, Pageable pageable);
}

@RestController
public class UserController {
    
    @GetMapping("/users")
    public Page<User> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }
}
```

**Generated SQL**:
```sql
-- Data query
SELECT * FROM user LIMIT 10 OFFSET 0;

-- Count query (for Page only)
SELECT COUNT(*) FROM user;
```

### Sorting

```java
// Single field
Pageable pageable = PageRequest.of(0, 10, Sort.by("username"));

// Ascending
Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());

// Descending
Pageable pageable = PageRequest.of(0, 10, Sort.by("age").descending());

// Multiple fields
Pageable pageable = PageRequest.of(0, 10, 
    Sort.by("age").descending().and(Sort.by("username")));

// Sort.Order
Sort sort = Sort.by(
    Sort.Order.desc("age"),
    Sort.Order.asc("username")
);
Pageable pageable = PageRequest.of(0, 10, sort);
```

**Generated SQL**:
```sql
SELECT * FROM user 
ORDER BY age DESC, username ASC 
LIMIT 10 OFFSET 0;
```

### Page Methods

```java
Page<User> page = userRepository.findAll(pageable);

// Content
List<User> users = page.getContent();

// Pagination info
int currentPage = page.getNumber();           // Current page (0-indexed)
int totalPages = page.getTotalPages();        // Total pages
long totalElements = page.getTotalElements(); // Total records
int pageSize = page.getSize();                // Page size
int numberOfElements = page.getNumberOfElements(); // Items in current page

// Navigation
boolean hasNext = page.hasNext();
boolean hasPrevious = page.hasPrevious();
boolean isFirst = page.isFirst();
boolean isLast = page.isLast();

// Next/Previous
Pageable nextPageable = page.nextPageable();
Pageable previousPageable = page.previousPageable();
```

### Slice vs Page

**Use Slice when**:
- Don't need total count
- Infinite scroll UI
- Performance critical

```java
Slice<User> slice = userRepository.findByAge(25, pageable);

boolean hasNext = slice.hasNext();  // No count query!
List<User> users = slice.getContent();
```

### Custom Pageable

```java
// Unpaged - get all results
Pageable unpaged = Pageable.unpaged();

// Offset-based
Pageable pageable = PageRequest.ofSize(10).withPage(2); // Page 2, size 10
```

### Pagination with Specifications

```java
Specification<User> spec = UserSpecifications.isActive();
Pageable pageable = PageRequest.of(0, 10, Sort.by("username"));

Page<User> users = userRepository.findAll(spec, pageable);
```

### Best Practices

✅ Use Slice for infinite scroll
✅ Use Page when total count needed
✅ Add default sorting for consistent results
❌ Don't fetch all records without pagination

---

## @Transactional

### Understanding Transactions

**ACID Properties**:
- **Atomicity**: All or nothing
- **Consistency**: Valid state always
- **Isolation**: Concurrent transactions don't interfere
- **Durability**: Committed changes persist

### Basic @Transactional

```java
@Service
public class OrderService {
    
    @Transactional
    public Order createOrder(Order order) {
        orderRepository.save(order);
        // If exception thrown, transaction rolls back
        paymentService.processPayment(order);
        return order;
    }
}
```

### Transaction Propagation

**7 Propagation Types**:

```java
// 1. REQUIRED (Default) - Use existing or create new
@Transactional(propagation = Propagation.REQUIRED)
public void method1() {
    method2(); // Uses same transaction
}

// 2. REQUIRES_NEW - Always create new transaction
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void method2() {
    // Runs in separate transaction
    // Commits independently
}

// 3. SUPPORTS - Use existing if available, non-transactional otherwise
@Transactional(propagation = Propagation.SUPPORTS)
public void method3() { }

// 4. NOT_SUPPORTED - Suspend existing transaction
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void method4() {
    // Always runs without transaction
}

// 5. MANDATORY - Must have existing transaction
@Transactional(propagation = Propagation.MANDATORY)
public void method5() {
    // Throws exception if no transaction
}

// 6. NEVER - Must not have transaction
@Transactional(propagation = Propagation.NEVER)
public void method6() {
    // Throws exception if transaction exists
}

// 7. NESTED - Create nested transaction (savepoint)
@Transactional(propagation = Propagation.NESTED)
public void method7() {
    // Can rollback to savepoint
}
```

### Isolation Levels

**4 Isolation Levels**:

```java
// 1. READ_UNCOMMITTED - Dirty reads possible
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void method1() {
    // Can read uncommitted changes
    // Fastest, least safe
}

// 2. READ_COMMITTED (Default) - No dirty reads
@Transactional(isolation = Isolation.READ_COMMITTED)
public void method2() {
    // Only reads committed data
    // Non-repeatable reads possible
}

// 3. REPEATABLE_READ - No dirty/non-repeatable reads
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void method3() {
    // Same query returns same results
    // Phantom reads possible
}

// 4. SERIALIZABLE - Complete isolation
@Transactional(isolation = Isolation.SERIALIZABLE)
public void method4() {
    // No dirty/non-repeatable/phantom reads
    // Slowest, safest
}
```

### Read-Only Optimization

```java
@Transactional(readOnly = true)
public User getUser(Long id) {
    return userRepository.findById(id).orElseThrow();
}

// Benefits:
// - Hibernate skips dirty checking
// - Database can optimize
// - Prevents accidental writes
```

### Timeout

```java
@Transactional(timeout = 30) // 30 seconds
public void longRunningOperation() {
    // Throws exception if exceeds 30 seconds
}
```

### Rollback Rules

```java
// Rollback on checked exceptions
@Transactional(rollbackFor = Exception.class)
public void method1() throws Exception {
    // Rolls back on any Exception
}

// Don't rollback on specific exception
@Transactional(noRollbackFor = BusinessException.class)
public void method2() {
    // Commits even if BusinessException thrown
}

// Multiple exceptions
@Transactional(
    rollbackFor = {SQLException.class, IOException.class},
    noRollbackFor = {ValidationException.class}
)
public void method3() { }
```

### Class vs Method Level

```java
@Service
@Transactional(readOnly = true) // Default for all methods
public class UserService {
    
    public User getUser(Long id) {
        // Uses readOnly=true
        return userRepository.findById(id).orElseThrow();
    }
    
    @Transactional // Override: readOnly=false
    public User createUser(User user) {
        return userRepository.save(user);
    }
}
```

### Proxy Limitations

**Problem**: Self-invocation doesn't work

```java
@Service
public class UserService {
    
    public void method1() {
        method2(); // @Transactional ignored!
    }
    
    @Transactional
    public void method2() {
        // Transaction not started
    }
}
```

**Solution**: Inject self or use separate service

```java
@Service
public class UserService {
    @Autowired
    private UserService self;
    
    public void method1() {
        self.method2(); // Works!
    }
    
    @Transactional
    public void method2() { }
}
```

### Programmatic Transactions

```java
@Service
public class UserService {
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    public User createUser(User user) {
        return transactionTemplate.execute(status -> {
            try {
                return userRepository.save(user);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}
```

### Best Practices

✅ Use readOnly=true for read operations
✅ Keep transactions short
✅ Use REQUIRED for most cases
✅ Set appropriate timeout
❌ Don't call @Transactional methods from same class
❌ Don't use SERIALIZABLE unless necessary

---

## Locking

### Understanding Locking

**Why Locking?**
- Prevent concurrent modification conflicts
- Ensure data consistency
- Handle race conditions

### Optimistic Locking

**How**: Use @Version column

**When**: Low contention, read-heavy workloads

```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private BigDecimal price;
    
    @Version
    private Long version; // Automatically managed
}
```

**How it works**:
```java
// User 1 reads product
Product product1 = productRepository.findById(1L); // version = 1

// User 2 reads same product
Product product2 = productRepository.findById(1L); // version = 1

// User 1 updates
product1.setPrice(new BigDecimal("100"));
productRepository.save(product1); // version = 2, SUCCESS

// User 2 tries to update
product2.setPrice(new BigDecimal("150"));
productRepository.save(product2); // OptimisticLockException!
```

**Generated SQL**:
```sql
-- Read
SELECT * FROM product WHERE id = 1;

-- Update (User 1)
UPDATE product 
SET price = 100, version = 2 
WHERE id = 1 AND version = 1; -- SUCCESS (1 row updated)

-- Update (User 2)
UPDATE product 
SET price = 150, version = 2 
WHERE id = 1 AND version = 1; -- FAIL (0 rows updated)
```

**Handling OptimisticLockException**:
```java
@Service
public class ProductService {
    
    public void updatePrice(Long id, BigDecimal newPrice) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                Product product = productRepository.findById(id).orElseThrow();
                product.setPrice(newPrice);
                productRepository.save(product);
                return; // Success
            } catch (OptimisticLockException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Failed after " + maxRetries + " attempts");
                }
            }
        }
    }
}
```

### Pessimistic Locking

**How**: Database-level locks

**When**: High contention, write-heavy workloads

### LockModeType Options

```java
// 1. PESSIMISTIC_READ - Shared lock
@Lock(LockModeType.PESSIMISTIC_READ)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdWithReadLock(@Param("id") Long id);
// Others can read, but not write

// 2. PESSIMISTIC_WRITE - Exclusive lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdWithWriteLock(@Param("id") Long id);
// Others cannot read or write

// 3. PESSIMISTIC_FORCE_INCREMENT - Lock + increment version
@Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdWithForceIncrement(@Param("id") Long id);
```

**Generated SQL (PostgreSQL)**:
```sql
-- PESSIMISTIC_READ
SELECT * FROM product WHERE id = 1 FOR SHARE;

-- PESSIMISTIC_WRITE
SELECT * FROM product WHERE id = 1 FOR UPDATE;

-- PESSIMISTIC_FORCE_INCREMENT
SELECT * FROM product WHERE id = 1 FOR UPDATE;
UPDATE product SET version = version + 1 WHERE id = 1;
```

### Lock Timeout

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdWithLockTimeout(@Param("id") Long id);
// Throws exception if lock not acquired in 5 seconds
```

### EntityManager Locking

```java
@Service
public class ProductService {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional
    public void updateWithLock(Long id, BigDecimal newPrice) {
        Product product = entityManager.find(
            Product.class, 
            id, 
            LockModeType.PESSIMISTIC_WRITE
        );
        product.setPrice(newPrice);
    }
}
```

### Optimistic vs Pessimistic

| Aspect | Optimistic | Pessimistic |
|--------|------------|-------------|
| Locking | At commit | At read |
| Performance | Better | Worse |
| Contention | Low | High |
| Deadlocks | No | Possible |
| Retries | Required | Not needed |
| Use case | Read-heavy | Write-heavy |

### Best Practices

✅ Use Optimistic for most cases
✅ Use Pessimistic for critical updates
✅ Set lock timeout
✅ Handle OptimisticLockException
❌ Don't hold locks too long
❌ Don't use Pessimistic unnecessarily

---

## Auditing

### Understanding Auditing

**Track**:
- Who created/modified entity
- When created/modified

### Enable Auditing

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Get current user from SecurityContext
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }
            return Optional.of(auth.getName());
        };
    }
}
```

### Auditable Base Class

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;
    
    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(nullable = false)
    private String lastModifiedBy;
    
    // Getters and setters
}
```

### Using Auditable

```java
@Entity
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    
    // Audit fields inherited from Auditable
}

// Usage
User user = new User();
user.setUsername("john");
userRepository.save(user);
// createdDate, createdBy automatically set

user.setUsername("john_updated");
userRepository.save(user);
// lastModifiedDate, lastModifiedBy automatically updated
```

**Generated SQL**:
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255),
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL
);

-- Insert
INSERT INTO user (username, created_date, last_modified_date, created_by, last_modified_by)
VALUES ('john', '2024-01-01 10:00:00', '2024-01-01 10:00:00', 'admin', 'admin');

-- Update
UPDATE user 
SET username = 'john_updated', 
    last_modified_date = '2024-01-02 15:30:00',
    last_modified_by = 'admin'
WHERE id = 1;
```

### Audit Annotations

```java
// @CreatedDate - Set once on creation
@CreatedDate
private LocalDateTime createdDate;

// @LastModifiedDate - Updated on every save
@LastModifiedDate
private LocalDateTime lastModifiedDate;

// @CreatedBy - Set once on creation
@CreatedBy
private String createdBy;

// @LastModifiedBy - Updated on every save
@LastModifiedBy
private String lastModifiedBy;
```

### Custom AuditorAware

```java
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication == null || 
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("SYSTEM");
        }
        
        return Optional.of(authentication.getName());
    }
}
```

### Entity-Level Auditing

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    private Long id;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @CreatedBy
    private String createdBy;
    
    // No lastModified fields - only track creation
}
```

### Custom Auditing Logic

```java
@Component
public class CustomAuditingListener {
    
    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof Auditable) {
            Auditable auditable = (Auditable) entity;
            LocalDateTime now = LocalDateTime.now();
            String user = getCurrentUser();
            
            auditable.setCreatedDate(now);
            auditable.setLastModifiedDate(now);
            auditable.setCreatedBy(user);
            auditable.setLastModifiedBy(user);
        }
    }
    
    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof Auditable) {
            Auditable auditable = (Auditable) entity;
            auditable.setLastModifiedDate(LocalDateTime.now());
            auditable.setLastModifiedBy(getCurrentUser());
        }
    }
    
    private String getCurrentUser() {
        // Get from SecurityContext
        return "current_user";
    }
}
```

### Querying Audit Data

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find by creator
    List<User> findByCreatedBy(String username);
    
    // Find by creation date range
    List<User> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);
    
    // Find recently modified
    List<User> findByLastModifiedDateAfter(LocalDateTime date);
    
    // Find by modifier
    List<User> findByLastModifiedBy(String username);
}
```

### Best Practices

✅ Use @MappedSuperclass for reusable audit fields
✅ Make audit fields non-updatable (createdDate, createdBy)
✅ Use LocalDateTime for timestamps
✅ Implement AuditorAware for user tracking
❌ Don't manually set audit fields
❌ Don't forget @EnableJpaAuditing

---

[← Previous: Part 3](05_Spring_Data_JPA_Part3.md) | [Next: Spring WebFlux →](07_Spring_WebFlux.md)
