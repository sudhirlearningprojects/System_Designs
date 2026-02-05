# Spring Data JPA - Part 3: Repositories & Query Methods

[← Back to Index](README.md) | [← Previous: Part 2](04_Spring_Data_JPA_Part2.md) | [Next: Part 4 - Advanced →](06_Spring_Data_JPA_Part4.md)

## Table of Contents
- [Repository Interfaces](#repository-interfaces)
- [Query Method Keywords](#query-method-keywords)
- [@Query Annotation](#query-annotation)
- [@EntityGraph](#entitygraph)

---

## Repository Interfaces

### Understanding Repository Hierarchy

**Spring Data JPA Repository Hierarchy**:
```
Repository (marker interface)
    ↓
CrudRepository (basic CRUD)
    ↓
PagingAndSortingRepository (+ pagination)
    ↓
JpaRepository (+ JPA specific)
```

### 1. Repository

**Marker interface** - no methods

```java
public interface Repository<T, ID> {
    // Empty - just marks as repository
}
```

### 2. CrudRepository

**Basic CRUD operations**

```java
public interface CrudRepository<T, ID> extends Repository<T, ID> {
    <S extends T> S save(S entity);
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
    
    Optional<T> findById(ID id);
    boolean existsById(ID id);
    Iterable<T> findAll();
    Iterable<T> findAllById(Iterable<ID> ids);
    
    long count();
    
    void deleteById(ID id);
    void delete(T entity);
    void deleteAll(Iterable<? extends T> entities);
    void deleteAll();
}
```

### 3. PagingAndSortingRepository

**Adds pagination and sorting**

```java
public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {
    Iterable<T> findAll(Sort sort);
    Page<T> findAll(Pageable pageable);
}
```

### 4. JpaRepository (Most Used)

**JPA-specific methods**

```java
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID> {
    List<T> findAll();
    List<T> findAll(Sort sort);
    List<T> findAllById(Iterable<ID> ids);
    
    <S extends T> List<S> saveAll(Iterable<S> entities);
    
    void flush();
    <S extends T> S saveAndFlush(S entity);
    
    void deleteInBatch(Iterable<T> entities);
    void deleteAllInBatch();
    
    T getOne(ID id);  // Returns proxy
}
```

### Comparison Table

| Feature | CrudRepository | PagingAndSortingRepository | JpaRepository |
|---------|----------------|----------------------------|---------------|
| CRUD | ✅ | ✅ | ✅ |
| Pagination | ❌ | ✅ | ✅ |
| Sorting | ❌ | ✅ | ✅ |
| Batch delete | ❌ | ❌ | ✅ |
| Flush | ❌ | ❌ | ✅ |
| Return type | Iterable | Iterable | List |

### Basic Usage

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring generates implementation automatically
}

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

### Custom Repository Interface

```java
// Custom interface
public interface CustomUserRepository {
    List<User> findUsersWithComplexLogic();
}

// Implementation
@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<User> findUsersWithComplexLogic() {
        // Custom JPQL or Criteria API
        return entityManager
            .createQuery("SELECT u FROM User u WHERE ...", User.class)
            .getResultList();
    }
}

// Combine both
public interface UserRepository extends JpaRepository<User, Long>, CustomUserRepository {
    // Now has both standard and custom methods
}
```

### @NoRepositoryBean

**Create base repository for all entities**

```java
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    Optional<T> findByIdWithLock(ID id);
}

@NoRepositoryBean
public class BaseRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {
    
    private EntityManager entityManager;
    
    public BaseRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }
    
    @Override
    public Optional<T> findByIdWithLock(ID id) {
        return Optional.ofNullable(
            entityManager.find(getDomainClass(), id, LockModeType.PESSIMISTIC_WRITE)
        );
    }
}

// Enable custom base repository
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl.class)
public class JpaConfig { }

// All repositories now have findByIdWithLock
public interface UserRepository extends BaseRepository<User, Long> { }
```

---

## Query Method Keywords

### Understanding Query Methods

**Spring Data JPA** generates queries from method names

### Basic Find Methods

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find by single property
    User findByUsername(String username);
    // SELECT * FROM user WHERE username = ?
    
    List<User> findByEmail(String email);
    // SELECT * FROM user WHERE email = ?
    
    Optional<User> findByPhone(String phone);
    // SELECT * FROM user WHERE phone = ?
}
```

### Logical Operators

```java
// AND
User findByUsernameAndEmail(String username, String email);
// WHERE username = ? AND email = ?

// OR
List<User> findByUsernameOrEmail(String username, String email);
// WHERE username = ? OR email = ?

// Complex
List<User> findByUsernameAndEmailOrPhone(String username, String email, String phone);
// WHERE (username = ? AND email = ?) OR phone = ?
```

### Comparison Operators

```java
// Greater Than
List<User> findByAgeGreaterThan(int age);
// WHERE age > ?

// Greater Than Equal
List<User> findByAgeGreaterThanEqual(int age);
// WHERE age >= ?

// Less Than
List<User> findByAgeLessThan(int age);
// WHERE age < ?

// Less Than Equal
List<User> findByAgeLessThanEqual(int age);
// WHERE age <= ?

// Between
List<User> findByAgeBetween(int start, int end);
// WHERE age BETWEEN ? AND ?

// Not Equal
List<User> findByAgeNot(int age);
// WHERE age <> ?
```

### String Operations

```java
// Like (contains)
List<User> findByUsernameLike(String pattern);
// WHERE username LIKE ?
// Usage: findByUsernameLike("%john%")

// Containing
List<User> findByUsernameContaining(String infix);
// WHERE username LIKE ?
// Auto adds %: findByUsernameContaining("john") -> %john%

// Starting With
List<User> findByUsernameStartingWith(String prefix);
// WHERE username LIKE ?
// Auto adds %: findByUsernameStartingWith("john") -> john%

// Ending With
List<User> findByUsernameEndingWith(String suffix);
// WHERE username LIKE ?
// Auto adds %: findByUsernameEndingWith("doe") -> %doe

// Ignore Case
User findByUsernameIgnoreCase(String username);
// WHERE LOWER(username) = LOWER(?)

// Combining
List<User> findByUsernameContainingIgnoreCase(String infix);
// WHERE LOWER(username) LIKE LOWER(?)
```

### Null Handling

```java
// Is Null
List<User> findByEmailIsNull();
// WHERE email IS NULL

// Is Not Null
List<User> findByEmailIsNotNull();
// WHERE email IS NOT NULL
```

### Collection Operations

```java
// In
List<User> findByUsernameIn(Collection<String> usernames);
// WHERE username IN (?, ?, ?)

// Not In
List<User> findByUsernameNotIn(Collection<String> usernames);
// WHERE username NOT IN (?, ?, ?)
```

### Boolean Operations

```java
// True
List<User> findByActiveTrue();
// WHERE active = true

// False
List<User> findByActiveFalse();
// WHERE active = false
```

### Ordering

```java
// Single field ascending
List<User> findByAgeOrderByUsernameAsc(int age);
// WHERE age = ? ORDER BY username ASC

// Single field descending
List<User> findByAgeOrderByUsernameDesc(int age);
// WHERE age = ? ORDER BY username DESC

// Multiple fields
List<User> findByAgeOrderByUsernameAscEmailDesc(int age);
// WHERE age = ? ORDER BY username ASC, email DESC
```

### Limiting Results

```java
// First
User findFirstByOrderByUsernameAsc();
// SELECT * FROM user ORDER BY username ASC LIMIT 1

// Top
User findTopByOrderByAgeDesc();
// SELECT * FROM user ORDER BY age DESC LIMIT 1

// Top N
List<User> findTop3ByOrderByAgeDesc();
// SELECT * FROM user ORDER BY age DESC LIMIT 3

// First N
List<User> findFirst10ByOrderByCreatedDateDesc();
// SELECT * FROM user ORDER BY created_date DESC LIMIT 10
```

### Distinct

```java
List<User> findDistinctByAge(int age);
// SELECT DISTINCT * FROM user WHERE age = ?

List<String> findDistinctUsernameByAge(int age);
// SELECT DISTINCT username FROM user WHERE age = ?
```

### Count & Exists

```java
// Count
long countByAge(int age);
// SELECT COUNT(*) FROM user WHERE age = ?

long countByAgeGreaterThan(int age);
// SELECT COUNT(*) FROM user WHERE age > ?

// Exists
boolean existsByUsername(String username);
// SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM user WHERE username = ?

boolean existsByEmail(String email);
// SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM user WHERE email = ?
```

### Delete Methods

```java
// Delete by property
void deleteByUsername(String username);
// DELETE FROM user WHERE username = ?

// Delete with return
Long deleteByAge(int age);
// Returns number of deleted entities

// Delete with list return
List<User> deleteByAgeGreaterThan(int age);
// Returns deleted entities
```

### Nested Properties

```java
@Entity
public class User {
    @OneToOne
    private Address address;
}

@Entity
public class Address {
    private String city;
}

// Query nested property
List<User> findByAddressCity(String city);
// SELECT u.* FROM user u JOIN address a ON u.address_id = a.id WHERE a.city = ?

List<User> findByAddressCityAndAddressZipCode(String city, String zipCode);
// WHERE a.city = ? AND a.zip_code = ?
```

### Complete Example

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Simple find
    Optional<User> findByUsername(String username);
    
    // Multiple conditions
    List<User> findByAgeGreaterThanAndActiveTrueOrderByUsernameAsc(int age);
    
    // String operations
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String username, String email);
    
    // Collection
    List<User> findByUsernameInAndActiveTrue(List<String> usernames);
    
    // Limiting
    List<User> findTop10ByActiveTrueOrderByCreatedDateDesc();
    
    // Count
    long countByAgeBetweenAndActiveTrue(int minAge, int maxAge);
    
    // Exists
    boolean existsByUsernameAndActiveTrue(String username);
    
    // Delete
    @Transactional
    Long deleteByActiveFalseAndCreatedDateBefore(LocalDateTime date);
}
```

---

## @Query Annotation

### Understanding @Query

**Use @Query when**:
- Query method name becomes too long
- Need complex joins or subqueries
- Need native SQL features
- Want better performance control

### JPQL vs Native SQL

| Feature | JPQL | Native SQL |
|---------|------|------------|
| Database independent | ✅ | ❌ |
| Entity-based | ✅ | ❌ |
| Type-safe | ✅ | ❌ |
| Database-specific features | ❌ | ✅ |
| Performance | Good | Better |

### Basic JPQL Query

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.username = ?1")
    User findByUsername(String username);
    
    // Generated SQL:
    // SELECT * FROM user WHERE username = ?
}
```

### Named Parameters

```java
@Query("SELECT u FROM User u WHERE u.username = :username AND u.email = :email")
User findByUsernameAndEmail(@Param("username") String username, 
                            @Param("email") String email);

// Can reuse parameters
@Query("SELECT u FROM User u WHERE u.username = :name OR u.email = :name")
List<User> findByUsernameOrEmail(@Param("name") String name);
```

### Positional Parameters

```java
@Query("SELECT u FROM User u WHERE u.username = ?1 AND u.age > ?2")
List<User> findByUsernameAndAgeGreaterThan(String username, int age);
```

### JOIN Queries

```java
// INNER JOIN
@Query("SELECT u FROM User u INNER JOIN u.orders o WHERE o.total > :amount")
List<User> findUsersWithOrdersAbove(@Param("amount") BigDecimal amount);

// Generated SQL:
// SELECT u.* FROM user u 
// INNER JOIN orders o ON u.id = o.user_id 
// WHERE o.total > ?

// LEFT JOIN
@Query("SELECT u FROM User u LEFT JOIN u.orders o WHERE u.age > :age")
List<User> findUsersWithOptionalOrders(@Param("age") int age);

// RIGHT JOIN
@Query("SELECT u FROM User u RIGHT JOIN u.profile p WHERE p.bio IS NOT NULL")
List<User> findUsersWithProfile();
```

### JOIN FETCH (Solve N+1)

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
User findByIdWithOrders(@Param("id") Long id);

// Multiple joins
@Query("SELECT u FROM User u " +
       "LEFT JOIN FETCH u.orders o " +
       "LEFT JOIN FETCH u.profile p " +
       "WHERE u.id = :id")
User findByIdWithOrdersAndProfile(@Param("id") Long id);

// Generated SQL:
// SELECT u.*, o.*, p.* FROM user u
// LEFT JOIN orders o ON u.id = o.user_id
// LEFT JOIN user_profile p ON u.profile_id = p.id
// WHERE u.id = ?
```

### Subqueries

```java
// IN subquery
@Query("SELECT u FROM User u WHERE u.id IN " +
       "(SELECT o.user.id FROM Order o WHERE o.total > :amount)")
List<User> findUsersWithHighValueOrders(@Param("amount") BigDecimal amount);

// EXISTS subquery
@Query("SELECT u FROM User u WHERE EXISTS " +
       "(SELECT 1 FROM Order o WHERE o.user = u AND o.status = 'PENDING')")
List<User> findUsersWithPendingOrders();

// NOT EXISTS
@Query("SELECT u FROM User u WHERE NOT EXISTS " +
       "(SELECT 1 FROM Order o WHERE o.user = u)")
List<User> findUsersWithoutOrders();
```

### Aggregate Functions

```java
// COUNT
@Query("SELECT COUNT(u) FROM User u WHERE u.age > :age")
long countUsersOlderThan(@Param("age") int age);

// SUM
@Query("SELECT SUM(o.total) FROM Order o WHERE o.user.id = :userId")
BigDecimal getTotalOrderAmount(@Param("userId") Long userId);

// AVG
@Query("SELECT AVG(u.age) FROM User u WHERE u.active = true")
Double getAverageAge();

// MAX
@Query("SELECT MAX(o.total) FROM Order o WHERE o.user.id = :userId")
BigDecimal getMaxOrderAmount(@Param("userId") Long userId);

// MIN
@Query("SELECT MIN(u.createdDate) FROM User u")
LocalDateTime getFirstUserCreatedDate();
```

### GROUP BY & HAVING

```java
// GROUP BY
@Query("SELECT u.age, COUNT(u) FROM User u GROUP BY u.age")
List<Object[]> countUsersByAge();

// GROUP BY with HAVING
@Query("SELECT u.city, COUNT(u) FROM User u " +
       "GROUP BY u.city HAVING COUNT(u) > :minCount")
List<Object[]> findCitiesWithMinUsers(@Param("minCount") long minCount);

// Multiple grouping
@Query("SELECT u.city, u.age, COUNT(u) FROM User u " +
       "GROUP BY u.city, u.age ORDER BY COUNT(u) DESC")
List<Object[]> getUserStatistics();
```

### DTO Projections

```java
// DTO class
public class UserDTO {
    private String username;
    private String email;
    
    public UserDTO(String username, String email) {
        this.username = username;
        this.email = email;
    }
}

// Query with DTO projection
@Query("SELECT new com.example.dto.UserDTO(u.username, u.email) FROM User u")
List<UserDTO> findAllUserDTOs();

// With conditions
@Query("SELECT new com.example.dto.UserDTO(u.username, u.email) " +
       "FROM User u WHERE u.age > :age")
List<UserDTO> findUserDTOsByAge(@Param("age") int age);

// With aggregation
@Query("SELECT new com.example.dto.OrderSummaryDTO(u.username, COUNT(o), SUM(o.total)) " +
       "FROM User u LEFT JOIN u.orders o GROUP BY u.id, u.username")
List<OrderSummaryDTO> getOrderSummaries();
```

### @Modifying Queries

```java
// UPDATE
@Modifying
@Transactional
@Query("UPDATE User u SET u.email = :email WHERE u.id = :id")
int updateEmail(@Param("id") Long id, @Param("email") String email);

// DELETE
@Modifying
@Transactional
@Query("DELETE FROM User u WHERE u.active = false")
int deleteInactiveUsers();

// Bulk update
@Modifying
@Transactional
@Query("UPDATE User u SET u.active = false WHERE u.lastLoginDate < :date")
int deactivateInactiveUsers(@Param("date") LocalDateTime date);
```

### Native SQL Queries

```java
// Basic native query
@Query(value = "SELECT * FROM users WHERE username = :username", nativeQuery = true)
User findByUsernameNative(@Param("username") String username);

// With JOIN
@Query(value = "SELECT u.* FROM users u " +
               "INNER JOIN orders o ON u.id = o.user_id " +
               "WHERE o.total > :amount", 
       nativeQuery = true)
List<User> findUsersWithHighValueOrdersNative(@Param("amount") BigDecimal amount);

// Database-specific features (PostgreSQL)
@Query(value = "SELECT * FROM users WHERE username ILIKE :pattern", 
       nativeQuery = true)
List<User> searchUsersCaseInsensitive(@Param("pattern") String pattern);

// Window functions (PostgreSQL)
@Query(value = "SELECT *, ROW_NUMBER() OVER (PARTITION BY city ORDER BY age DESC) as rank " +
               "FROM users", 
       nativeQuery = true)
List<Object[]> getUsersRankedByAgeInCity();
```

### Named Queries

```java
@Entity
@NamedQuery(
    name = "User.findByAgeRange",
    query = "SELECT u FROM User u WHERE u.age BETWEEN :minAge AND :maxAge"
)
@NamedQuery(
    name = "User.findActiveUsers",
    query = "SELECT u FROM User u WHERE u.active = true ORDER BY u.username"
)
public class User { }

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    List<User> findActiveUsers();
}
```

### Best Practices

✅ **DO**:
- Use named parameters for readability
- Use JOIN FETCH to avoid N+1
- Use DTO projections for read-only data
- Use @Modifying with @Transactional

❌ **DON'T**:
- Use native queries unless necessary
- Forget @Transactional on @Modifying
- Select unnecessary columns
- Use positional parameters for complex queries

---

## @EntityGraph

### Understanding @EntityGraph

**Problem**: N+1 query problem with LAZY loading

**Solution**: @EntityGraph loads related entities in single query

### @NamedEntityGraph

**Define on entity**:

```java
@Entity
@NamedEntityGraph(
    name = "User.orders",
    attributeNodes = @NamedAttributeNode("orders")
)
public class User {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph("User.orders")
    List<User> findAll();
}
```

**Generated SQL**:
```sql
SELECT u.*, o.* FROM user u
LEFT JOIN orders o ON u.id = o.user_id;
```

### Ad-hoc EntityGraph

**Define inline**:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    @EntityGraph(attributePaths = {"orders"})
    List<User> findAll();
    
    @EntityGraph(attributePaths = {"orders", "profile"})
    User findById(Long id);
}
```

### Nested Paths

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user")
    private List<Order> orders;
}

@Entity
public class Order {
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}

// Load nested relationships
@EntityGraph(attributePaths = {"orders", "orders.items"})
User findById(Long id);

// Generated SQL:
// SELECT u.*, o.*, i.* FROM user u
// LEFT JOIN orders o ON u.id = o.user_id
// LEFT JOIN order_items i ON o.id = i.order_id
// WHERE u.id = ?
```

### EntityGraphType

```java
// FETCH: Load specified + EAGER attributes
@EntityGraph(attributePaths = {"orders"}, type = EntityGraphType.FETCH)
List<User> findAll();

// LOAD: Load specified, others as defined in entity
@EntityGraph(attributePaths = {"orders"}, type = EntityGraphType.LOAD)
List<User> findAll();
```

### Subgraphs

```java
@Entity
@NamedEntityGraph(
    name = "User.full",
    attributeNodes = {
        @NamedAttributeNode("profile"),
        @NamedAttributeNode(value = "orders", subgraph = "orders-subgraph")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "orders-subgraph",
            attributeNodes = @NamedAttributeNode("items")
        )
    }
)
public class User { }

@EntityGraph("User.full")
User findById(Long id);
```

### @EntityGraph vs JOIN FETCH

| Feature | @EntityGraph | JOIN FETCH |
|---------|--------------|------------|
| Reusability | ✅ | ❌ |
| Type-safe | ✅ | ❌ |
| Flexibility | ✅ | ✅ |
| Cartesian product | Possible | Possible |
| Pagination | Works | Breaks |

### Best Practices

✅ Use @EntityGraph for reusable fetch strategies
✅ Use ad-hoc for one-off queries
✅ Prefer attributePaths for simplicity
❌ Avoid loading too many relationships

---

## Specifications

### Understanding Specifications

**Problem**: Dynamic query building

**Solution**: Criteria API with type-safe queries

### Enable Specifications

```java
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // Now has specification methods
}
```

### Basic Specification

```java
public class UserSpecifications {
    
    public static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("username"), username);
    }
    
    public static Specification<User> isActive() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isTrue(root.get("active"));
    }
    
    public static Specification<User> ageGreaterThan(int age) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.greaterThan(root.get("age"), age);
    }
}

// Usage
List<User> users = userRepository.findAll(
    UserSpecifications.hasUsername("john")
);
```

### Combining Specifications

```java
// AND
Specification<User> spec = Specification
    .where(UserSpecifications.isActive())
    .and(UserSpecifications.ageGreaterThan(18));

List<User> users = userRepository.findAll(spec);

// OR
Specification<User> spec = Specification
    .where(UserSpecifications.hasUsername("john"))
    .or(UserSpecifications.hasUsername("jane"));

// Complex
Specification<User> spec = Specification
    .where(UserSpecifications.isActive())
    .and(UserSpecifications.ageGreaterThan(18)
        .or(UserSpecifications.hasRole("ADMIN")));
```

### Dynamic Query Building

```java
public List<User> searchUsers(String username, Integer minAge, Boolean active) {
    Specification<User> spec = Specification.where(null);
    
    if (username != null) {
        spec = spec.and(UserSpecifications.hasUsername(username));
    }
    
    if (minAge != null) {
        spec = spec.and(UserSpecifications.ageGreaterThan(minAge));
    }
    
    if (active != null && active) {
        spec = spec.and(UserSpecifications.isActive());
    }
    
    return userRepository.findAll(spec);
}
```

### Specifications with Pagination

```java
Specification<User> spec = UserSpecifications.isActive();
Pageable pageable = PageRequest.of(0, 10, Sort.by("username"));

Page<User> users = userRepository.findAll(spec, pageable);
```

---

## Projections

### Understanding Projections

**Problem**: Loading full entities when only few fields needed

**Solution**: Project only required fields

### Interface Projections (Closed)

```java
public interface UserSummary {
    String getUsername();
    String getEmail();
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findAllBy();
    UserSummary findByUsername(String username);
}

// Generated SQL:
// SELECT u.username, u.email FROM user u
```

### Open Projections

```java
public interface UserInfo {
    String getUsername();
    
    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();
    
    @Value("#{target.orders.size()}")
    int getOrderCount();
}
```

### Class-Based Projections

```java
public class UserDTO {
    private String username;
    private String email;
    
    public UserDTO(String username, String email) {
        this.username = username;
        this.email = email;
    }
}

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT new com.example.dto.UserDTO(u.username, u.email) FROM User u")
    List<UserDTO> findAllDTOs();
}
```

### Dynamic Projections

```java
public interface UserRepository extends JpaRepository<User, Long> {
    <T> List<T> findByAge(int age, Class<T> type);
}

// Usage
List<UserSummary> summaries = userRepository.findByAge(25, UserSummary.class);
List<UserDTO> dtos = userRepository.findByAge(25, UserDTO.class);
```

---

[← Previous: Part 2](04_Spring_Data_JPA_Part2.md) | [Next: Part 4 - Advanced →](06_Spring_Data_JPA_Part4.md)
