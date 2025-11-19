# Spring Data JPA - Complete Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Architecture](#architecture)
3. [Entity Mapping](#entity-mapping)
4. [Repository Patterns](#repository-patterns)
5. [Query Methods](#query-methods)
6. [Transactions](#transactions)
7. [Performance Optimization](#performance-optimization)
8. [Advanced Features](#advanced-features)
9. [Interview Questions](#interview-questions)
10. [Best Practices](#best-practices)

## Introduction

Spring Data JPA simplifies data access layer implementation by providing repository abstractions and reducing boilerplate code while leveraging JPA (Java Persistence API) and Hibernate.

### Key Features
- **Repository Abstraction**: Automatic CRUD operations
- **Query Methods**: Derive queries from method names
- **Custom Queries**: @Query annotation support
- **Pagination & Sorting**: Built-in support
- **Auditing**: Automatic audit fields
- **Specifications**: Dynamic query building

## Architecture

### JPA Stack

```
┌─────────────────────────────────────────────────┐
│              Spring Data JPA                   │
├─────────────────────────────────────────────────┤
│                    JPA API                     │
├─────────────────────────────────────────────────┤
│              Hibernate (Provider)              │
├─────────────────────────────────────────────────┤
│                   JDBC                         │
├─────────────────────────────────────────────────┤
│                  Database                      │
└─────────────────────────────────────────────────┘
```

### Core Components

```java
// Entity Manager Factory
@Configuration
@EnableJpaRepositories(basePackages = "com.example.repository")
public class JpaConfig {
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.example.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties());
        return em;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }
}
```

## Entity Mapping

### Basic Entity

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
}
```

### Relationships

```java
// One-to-Many
@Entity
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();
    
    // Helper methods
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);
    }
    
    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }
}

@Entity
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @ManyToMany
    @JoinTable(
        name = "employee_project",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    private Set<Project> projects = new HashSet<>();
}

// Many-to-Many with Join Entity
@Entity
public class EmployeeProject {
    @EmbeddedId
    private EmployeeProjectId id;
    
    @ManyToOne
    @MapsId("employeeId")
    @JoinColumn(name = "employee_id")
    private Employee employee;
    
    @ManyToOne
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;
    
    private LocalDate assignedDate;
    private String role;
}

@Embeddable
public class EmployeeProjectId implements Serializable {
    private Long employeeId;
    private Long projectId;
}
```

### Inheritance Mapping

```java
// Table Per Class Hierarchy
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String accountNumber;
    private BigDecimal balance;
}

@Entity
@DiscriminatorValue("SAVINGS")
public class SavingsAccount extends Account {
    private BigDecimal interestRate;
}

@Entity
@DiscriminatorValue("CHECKING")
public class CheckingAccount extends Account {
    private BigDecimal overdraftLimit;
}

// Table Per Subclass
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String manufacturer;
    private String model;
}

@Entity
@Table(name = "cars")
@PrimaryKeyJoinColumn(name = "vehicle_id")
public class Car extends Vehicle {
    private int numberOfDoors;
}

@Entity
@Table(name = "motorcycles")
@PrimaryKeyJoinColumn(name = "vehicle_id")
public class Motorcycle extends Vehicle {
    private boolean hasSidecar;
}
```

## Repository Patterns

### Repository Hierarchy

```java
// Base Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Inherits basic CRUD operations
}

// Custom Repository Interface
public interface CustomUserRepository {
    List<User> findUsersWithComplexCriteria(UserSearchCriteria criteria);
    Page<User> findActiveUsersWithPagination(Pageable pageable);
}

// Custom Repository Implementation
@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<User> findUsersWithComplexCriteria(UserSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getName() != null) {
            predicates.add(cb.like(root.get("name"), "%" + criteria.getName() + "%"));
        }
        
        if (criteria.getEmail() != null) {
            predicates.add(cb.equal(root.get("email"), criteria.getEmail()));
        }
        
        if (criteria.getCreatedAfter() != null) {
            predicates.add(cb.greaterThan(root.get("createdAt"), criteria.getCreatedAfter()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("createdAt")));
        
        return entityManager.createQuery(query).getResultList();
    }
}

// Extended Repository
public interface UserRepository extends JpaRepository<User, Long>, CustomUserRepository {
    // Query methods + custom methods
}
```

### Repository Implementation

```java
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        
        return userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        
        user.setEmail(request.getEmail());
        // Other updates...
        
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}
```

## Query Methods

### Derived Queries

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Simple property expressions
    List<User> findByUsername(String username);
    List<User> findByEmail(String email);
    Optional<User> findByUsernameAndEmail(String username, String email);
    
    // Comparison operators
    List<User> findByAgeGreaterThan(Integer age);
    List<User> findByAgeBetween(Integer startAge, Integer endAge);
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    // String operations
    List<User> findByUsernameContaining(String username);
    List<User> findByUsernameStartingWith(String prefix);
    List<User> findByEmailEndingWith(String suffix);
    List<User> findByUsernameIgnoreCase(String username);
    
    // Collection operations
    List<User> findByStatusIn(Collection<UserStatus> statuses);
    List<User> findByUsernameNotIn(Collection<String> usernames);
    
    // Null handling
    List<User> findByLastLoginIsNull();
    List<User> findByLastLoginIsNotNull();
    
    // Boolean operations
    List<User> findByActiveTrue();
    List<User> findByActiveFalse();
    
    // Ordering
    List<User> findByStatusOrderByCreatedAtDesc(UserStatus status);
    List<User> findByActiveOrderByUsernameAscCreatedAtDesc(Boolean active);
    
    // Limiting results
    User findFirstByOrderByCreatedAtDesc();
    List<User> findTop10ByStatusOrderByCreatedAtDesc(UserStatus status);
    
    // Distinct
    List<String> findDistinctUsernameByStatus(UserStatus status);
    
    // Counting
    long countByStatus(UserStatus status);
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Existence
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // Deletion
    void deleteByStatus(UserStatus status);
    long deleteByCreatedAtBefore(LocalDateTime date);
}
```

### Custom Queries

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // JPQL Queries
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmailJPQL(String email);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:username% AND u.status = :status")
    List<User> findByUsernameContainingAndStatus(@Param("username") String username, 
                                               @Param("status") UserStatus status);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    // Native SQL Queries
    @Query(value = "SELECT * FROM users u WHERE u.created_at > :date", nativeQuery = true)
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);
    
    @Query(value = "SELECT u.*, COUNT(o.id) as order_count " +
                   "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
                   "GROUP BY u.id HAVING COUNT(o.id) > :minOrders", 
           nativeQuery = true)
    List<Object[]> findUsersWithMinimumOrders(@Param("minOrders") int minOrders);
    
    // Modifying Queries
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.lastLogin < :date")
    int updateInactiveUsers(@Param("status") UserStatus status, @Param("date") LocalDateTime date);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.status = :status")
    int deleteByStatus(@Param("status") UserStatus status);
    
    // Projection Queries
    @Query("SELECT u.username, u.email FROM User u WHERE u.status = :status")
    List<Object[]> findUsernameAndEmailByStatus(@Param("status") UserStatus status);
    
    // DTO Projection
    @Query("SELECT new com.example.dto.UserSummaryDTO(u.id, u.username, u.email) " +
           "FROM User u WHERE u.status = :status")
    List<UserSummaryDTO> findUserSummariesByStatus(@Param("status") UserStatus status);
}
```

### Specifications

```java
public class UserSpecifications {
    
    public static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> 
            username == null ? null : criteriaBuilder.equal(root.get("username"), username);
    }
    
    public static Specification<User> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> 
            email == null ? null : criteriaBuilder.equal(root.get("email"), email);
    }
    
    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, criteriaBuilder) -> 
            status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }
    
    public static Specification<User> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> 
            date == null ? null : criteriaBuilder.greaterThan(root.get("createdAt"), date);
    }
    
    public static Specification<User> hasRole(String roleName) {
        return (root, query, criteriaBuilder) -> {
            Join<User, Role> roleJoin = root.join("roles");
            return criteriaBuilder.equal(roleJoin.get("name"), roleName);
        };
    }
}

// Usage
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
}

@Service
public class UserService {
    
    public Page<User> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        
        if (criteria.getUsername() != null) {
            spec = spec.and(UserSpecifications.hasUsername(criteria.getUsername()));
        }
        
        if (criteria.getEmail() != null) {
            spec = spec.and(UserSpecifications.hasEmail(criteria.getEmail()));
        }
        
        if (criteria.getStatus() != null) {
            spec = spec.and(UserSpecifications.hasStatus(criteria.getStatus()));
        }
        
        if (criteria.getCreatedAfter() != null) {
            spec = spec.and(UserSpecifications.createdAfter(criteria.getCreatedAfter()));
        }
        
        return userRepository.findAll(spec, pageable);
    }
}
```

## Transactions

### Transaction Management

```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }
}

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PaymentService paymentService;
    
    // Default transaction settings
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setItems(request.getItems());
        
        // This will participate in the same transaction
        inventoryService.reserveItems(request.getItems());
        
        return orderRepository.save(order);
    }
    
    // Read-only transaction
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    // Custom transaction settings
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        propagation = Propagation.REQUIRES_NEW,
        timeout = 30,
        rollbackFor = {BusinessException.class},
        noRollbackFor = {ValidationException.class}
    )
    public void processPayment(Long orderId, PaymentRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        try {
            paymentService.processPayment(request);
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        } catch (PaymentException e) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            throw new BusinessException("Payment processing failed", e);
        }
    }
    
    // Programmatic transaction management
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    public Order createOrderProgrammatic(CreateOrderRequest request) {
        return transactionTemplate.execute(status -> {
            try {
                Order order = new Order();
                order.setCustomerId(request.getCustomerId());
                
                inventoryService.reserveItems(request.getItems());
                
                return orderRepository.save(order);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("Order creation failed", e);
            }
        });
    }
}
```

### Transaction Propagation

```java
@Service
public class TransactionPropagationExample {
    
    @Transactional
    public void outerMethod() {
        // Transaction T1 starts
        
        innerMethodRequired();     // Participates in T1
        innerMethodRequiresNew();  // Creates new transaction T2
        innerMethodNested();       // Creates nested transaction
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void innerMethodRequired() {
        // Participates in existing transaction or creates new one
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void innerMethodRequiresNew() {
        // Always creates new transaction, suspends current one
    }
    
    @Transactional(propagation = Propagation.NESTED)
    public void innerMethodNested() {
        // Creates nested transaction (savepoint)
    }
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public void innerMethodSupports() {
        // Participates if transaction exists, non-transactional otherwise
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void innerMethodNotSupported() {
        // Always executes non-transactionally, suspends current transaction
    }
    
    @Transactional(propagation = Propagation.NEVER)
    public void innerMethodNever() {
        // Throws exception if transaction exists
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void innerMethodMandatory() {
        // Throws exception if no transaction exists
    }
}
```

## Performance Optimization

### Lazy Loading and N+1 Problem

```java
// N+1 Problem Example
@Entity
public class Author {
    @Id
    private Long id;
    private String name;
    
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<Book> books;
}

@Entity
public class Book {
    @Id
    private Long id;
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Author author;
}

// Problematic code - causes N+1 queries
public void printAuthorsAndBooks() {
    List<Author> authors = authorRepository.findAll(); // 1 query
    
    for (Author author : authors) {
        System.out.println(author.getName());
        for (Book book : author.getBooks()) { // N queries (one per author)
            System.out.println(book.getTitle());
        }
    }
}

// Solution 1: Join Fetch
public interface AuthorRepository extends JpaRepository<Author, Long> {
    
    @Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books")
    List<Author> findAllWithBooks();
    
    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id")
    Optional<Author> findByIdWithBooks(@Param("id") Long id);
}

// Solution 2: Entity Graph
@Entity
@NamedEntityGraph(
    name = "Author.books",
    attributeNodes = @NamedAttributeNode("books")
)
public class Author {
    // ... fields
}

public interface AuthorRepository extends JpaRepository<Author, Long> {
    
    @EntityGraph(value = "Author.books")
    List<Author> findAll();
    
    @EntityGraph(attributePaths = {"books"})
    Optional<Author> findById(Long id);
}

// Solution 3: Batch Fetching
@Entity
public class Author {
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Book> books;
}
```

### Caching

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES));
        return cacheManager;
    }
}

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    @Id
    private Long id;
    
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(mappedBy = "product")
    private List<Review> reviews;
}

@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElse(null);
    }
    
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
    
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() {
        // Clear all product cache entries
    }
}
```

### Batch Operations

```java
@Repository
public class BatchUserRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:20}")
    private int batchSize;
    
    @Transactional
    public void batchInsertUsers(List<User> users) {
        for (int i = 0; i < users.size(); i++) {
            entityManager.persist(users.get(i));
            
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }
    
    @Transactional
    public void batchUpdateUsers(List<User> users) {
        for (int i = 0; i < users.size(); i++) {
            entityManager.merge(users.get(i));
            
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }
    
    @Modifying
    @Query("UPDATE User u SET u.status = :newStatus WHERE u.status = :oldStatus")
    int bulkUpdateUserStatus(@Param("oldStatus") UserStatus oldStatus, 
                           @Param("newStatus") UserStatus newStatus);
}
```

## Advanced Features

### Auditing

```java
@Configuration
@EnableJpaAuditing
public class AuditConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Version
    private Long version;
}

@Entity
public class Document extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
}
```

### Custom Repository Implementation

```java
public interface CustomDocumentRepository {
    List<Document> findDocumentsWithComplexSearch(DocumentSearchCriteria criteria);
    Page<Document> findDocumentsWithFullTextSearch(String searchTerm, Pageable pageable);
}

@Repository
public class CustomDocumentRepositoryImpl implements CustomDocumentRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Document> findDocumentsWithComplexSearch(DocumentSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Document> query = cb.createQuery(Document.class);
        Root<Document> root = query.from(Document.class);
        
        List<Predicate> predicates = buildPredicates(cb, root, criteria);
        
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        // Add sorting
        if (criteria.getSortBy() != null) {
            if (criteria.getSortDirection() == SortDirection.DESC) {
                query.orderBy(cb.desc(root.get(criteria.getSortBy())));
            } else {
                query.orderBy(cb.asc(root.get(criteria.getSortBy())));
            }
        }
        
        TypedQuery<Document> typedQuery = entityManager.createQuery(query);
        
        // Add pagination
        if (criteria.getOffset() != null) {
            typedQuery.setFirstResult(criteria.getOffset());
        }
        if (criteria.getLimit() != null) {
            typedQuery.setMaxResults(criteria.getLimit());
        }
        
        return typedQuery.getResultList();
    }
    
    @Override
    public Page<Document> findDocumentsWithFullTextSearch(String searchTerm, Pageable pageable) {
        // Native query for full-text search (PostgreSQL example)
        String searchQuery = """
            SELECT d.*, ts_rank(to_tsvector('english', d.title || ' ' || d.content), 
                               plainto_tsquery('english', :searchTerm)) as rank
            FROM documents d
            WHERE to_tsvector('english', d.title || ' ' || d.content) @@ 
                  plainto_tsquery('english', :searchTerm)
            ORDER BY rank DESC
            """;
        
        Query query = entityManager.createNativeQuery(searchQuery, Document.class);
        query.setParameter("searchTerm", searchTerm);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        List<Document> documents = query.getResultList();
        
        // Count query
        String countQuery = """
            SELECT COUNT(*)
            FROM documents d
            WHERE to_tsvector('english', d.title || ' ' || d.content) @@ 
                  plainto_tsquery('english', :searchTerm)
            """;
        
        Query countQ = entityManager.createNativeQuery(countQuery);
        countQ.setParameter("searchTerm", searchTerm);
        long total = ((Number) countQ.getSingleResult()).longValue();
        
        return new PageImpl<>(documents, pageable, total);
    }
}
```

## Interview Questions

### Basic Level

**Q1: What is Spring Data JPA and how does it differ from plain JPA?**

**Answer:** Spring Data JPA is a framework that simplifies JPA-based data access layers by providing:
- **Repository Abstractions**: Automatic CRUD operations without implementation
- **Query Methods**: Derive queries from method names
- **Reduced Boilerplate**: No need to write basic CRUD code
- **Integration**: Seamless Spring integration with transactions, caching

Plain JPA requires manual EntityManager usage and more boilerplate code.

**Q2: Explain the repository hierarchy in Spring Data JPA.**

**Answer:**
```java
Repository<T, ID>                    // Marker interface
├── CrudRepository<T, ID>           // Basic CRUD operations
│   └── PagingAndSortingRepository<T, ID>  // Pagination and sorting
│       └── JpaRepository<T, ID>    // JPA-specific operations (batch operations)
└── JpaSpecificationExecutor<T>     // Dynamic queries with Specifications
```

### Intermediate Level

**Q3: How do you solve the N+1 query problem in JPA?**

**Answer:**
```java
// Problem: N+1 queries
List<Author> authors = authorRepository.findAll(); // 1 query
for (Author author : authors) {
    author.getBooks().size(); // N queries
}

// Solution 1: JOIN FETCH
@Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books")
List<Author> findAllWithBooks();

// Solution 2: @EntityGraph
@EntityGraph(attributePaths = {"books"})
List<Author> findAll();

// Solution 3: @BatchSize
@Entity
public class Author {
    @OneToMany(mappedBy = "author")
    @BatchSize(size = 10)
    private List<Book> books;
}
```

**Q4: Explain transaction propagation in Spring Data JPA.**

**Answer:**
- **REQUIRED** (default): Join existing transaction or create new
- **REQUIRES_NEW**: Always create new transaction, suspend current
- **NESTED**: Create nested transaction (savepoint)
- **SUPPORTS**: Join if exists, non-transactional otherwise
- **NOT_SUPPORTED**: Always non-transactional, suspend current
- **NEVER**: Throw exception if transaction exists
- **MANDATORY**: Throw exception if no transaction exists

### Advanced Level

**Q5: Design a multi-tenant JPA application with separate schemas.**

**Answer:**
```java
@Component
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setCurrentTenant(String tenant) {
        currentTenant.set(tenant);
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
}

@Component
public class MultiTenantConnectionProvider implements MultiTenantConnectionProvider {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setSchema(tenantIdentifier);
        return connection;
    }
    
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

@Component
public class CurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {
    
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        return tenant != null ? tenant : "default";
    }
    
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}

@Configuration
public class MultiTenantJpaConfig {
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.example.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider());
        properties.put("hibernate.tenant_identifier_resolver", currentTenantIdentifierResolver());
        
        em.setJpaPropertyMap(properties);
        return em;
    }
}
```

**Q6: Implement optimistic locking with conflict resolution.**

**Answer:**
```java
@Entity
public class Product {
    @Id
    private Long id;
    
    private String name;
    private BigDecimal price;
    private Integer quantity;
    
    @Version
    private Long version;
}

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public Product updateProductWithRetry(Long id, UpdateProductRequest request) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
                
                // Apply updates
                product.setName(request.getName());
                product.setPrice(request.getPrice());
                
                return productRepository.save(product);
                
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new ConcurrentUpdateException("Failed to update after " + maxRetries + " attempts");
                }
                
                // Wait before retry
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Update interrupted", ie);
                }
            }
        }
        
        throw new ConcurrentUpdateException("Update failed");
    }
    
    // Custom conflict resolution
    public Product updateProductWithMerge(Long id, UpdateProductRequest request, Long expectedVersion) {
        Product current = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        if (!current.getVersion().equals(expectedVersion)) {
            // Handle conflict - could merge changes or ask user to resolve
            return handleVersionConflict(current, request, expectedVersion);
        }
        
        current.setName(request.getName());
        current.setPrice(request.getPrice());
        
        return productRepository.save(current);
    }
    
    private Product handleVersionConflict(Product current, UpdateProductRequest request, Long expectedVersion) {
        // Custom merge logic
        ConflictResolution resolution = new ConflictResolution();
        resolution.setCurrentVersion(current);
        resolution.setRequestedChanges(request);
        resolution.setExpectedVersion(expectedVersion);
        
        // Could implement various strategies:
        // 1. Last write wins
        // 2. Field-level merging
        // 3. User intervention required
        
        throw new OptimisticLockException("Version conflict detected", resolution);
    }
}
```

## Best Practices

### Entity Design

```java
// Good entity design
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_status_created", columnList = "status, created_at")
})
public class User extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    // Use appropriate fetch types
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
    
    // Helper methods for bidirectional relationships
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
    
    // Override equals and hashCode properly
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
```

### Repository Best Practices

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    // Use Optional for single results
    Optional<User> findByUsername(String username);
    
    // Use specific return types
    boolean existsByEmail(String email);
    long countByStatus(UserStatus status);
    
    // Use @Query for complex queries
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);
    
    // Use projections for read-only data
    @Query("SELECT new com.example.dto.UserSummary(u.id, u.username, u.email) FROM User u")
    List<UserSummary> findAllUserSummaries();
    
    // Use modifying queries for bulk operations
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.id = :id")
    int updateLastLogin(@Param("id") Long id, @Param("loginTime") LocalDateTime loginTime);
}
```

### Performance Configuration

```java
# application.properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true

# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000

# Query optimization
spring.jpa.properties.hibernate.query.plan_cache_max_size=2048
spring.jpa.properties.hibernate.query.plan_parameter_metadata_max_size=128

# Second-level cache
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

This comprehensive Spring Data JPA guide covers entity mapping, repository patterns, query methods, transactions, performance optimization, and advanced features with practical examples and best practices.