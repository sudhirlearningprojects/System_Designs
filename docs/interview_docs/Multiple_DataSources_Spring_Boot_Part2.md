# Multiple DataSources in Spring Boot - Part 2

## Table of Contents
- [Advanced Transaction Management](#advanced-transaction-management)
- [Cross-Database Transactions](#cross-database-transactions)
- [Dynamic DataSource Routing](#dynamic-datasource-routing)
- [Testing Strategies](#testing-strategies)
- [Common Pitfalls and Solutions](#common-pitfalls-and-solutions)
- [Performance Optimization](#performance-optimization)
- [Production Best Practices](#production-best-practices)
- [Interview Questions](#interview-questions)

---

## Advanced Transaction Management

### Specifying Transaction Manager

When using multiple datasources, you must explicitly specify which transaction manager to use:

```java
@Service
public class MultiDatabaseService {
    
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    // Uses primary transaction manager (default due to @Primary)
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
    
    // Explicitly uses secondary transaction manager
    @Transactional("secondaryTransactionManager")
    public void saveProduct(Product product) {
        productRepository.save(product);
    }
    
    // Uses primary transaction manager explicitly
    @Transactional("primaryTransactionManager")
    public void saveUserExplicit(User user) {
        userRepository.save(user);
    }
}
```

### Transaction Propagation with Multiple DataSources

```java
@Service
public class ComplexTransactionService {
    
    private final UserService userService;
    private final ProductService productService;
    
    /**
     * This will NOT work as expected!
     * Each method uses different transaction managers
     * No atomic guarantee across both databases
     */
    @Transactional("primaryTransactionManager")
    public void saveUserAndProduct(User user, Product product) {
        userService.createUser(user);  // Uses primaryTransactionManager
        productService.createProduct(product);  // Uses secondaryTransactionManager
        // If product save fails, user save will NOT rollback!
    }
}
```

**Key Point:** Transactions are isolated per datasource. You cannot have a single transaction spanning multiple databases without distributed transactions (JTA).

### Read-Only Transactions

```java
@Service
public class ReportingService {
    
    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional(value = "secondaryTransactionManager", readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
```

**Benefits:**
- Performance optimization (no flush, no dirty checking)
- Database can route to read replicas
- Clear intent in code

---

## Cross-Database Transactions

### Problem: No Atomic Guarantee

```java
@Service
public class OrderService {
    
    private final UserRepository userRepository;  // Primary DB
    private final ProductRepository productRepository;  // Secondary DB
    
    @Transactional("primaryTransactionManager")
    public void createOrder(Long userId, Long productId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // This is in a different database!
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(product.getStock() - 1);
        productRepository.save(product);  // NOT in same transaction!
        
        // If this fails, product stock is already decremented!
        user.setOrderCount(user.getOrderCount() + 1);
        userRepository.save(user);
    }
}
```

### Solution 1: Manual Transaction Management

```java
@Service
public class OrderService {
    
    @Autowired
    @Qualifier("primaryTransactionManager")
    private PlatformTransactionManager primaryTxManager;
    
    @Autowired
    @Qualifier("secondaryTransactionManager")
    private PlatformTransactionManager secondaryTxManager;
    
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    public void createOrder(Long userId, Long productId) {
        TransactionStatus primaryTx = primaryTxManager.getTransaction(
            new DefaultTransactionDefinition()
        );
        TransactionStatus secondaryTx = secondaryTxManager.getTransaction(
            new DefaultTransactionDefinition()
        );
        
        try {
            // Update user in primary DB
            User user = userRepository.findById(userId).orElseThrow();
            user.setOrderCount(user.getOrderCount() + 1);
            userRepository.save(user);
            
            // Update product in secondary DB
            Product product = productRepository.findById(productId).orElseThrow();
            product.setStock(product.getStock() - 1);
            productRepository.save(product);
            
            // Commit both transactions
            primaryTxManager.commit(primaryTx);
            secondaryTxManager.commit(secondaryTx);
            
        } catch (Exception e) {
            // Rollback both transactions
            primaryTxManager.rollback(primaryTx);
            secondaryTxManager.rollback(secondaryTx);
            throw e;
        }
    }
}
```

**Issues with Manual Approach:**
- Not truly atomic (network partition can cause inconsistency)
- Complex error handling
- No guarantee both commits succeed

### Solution 2: JTA (Java Transaction API) - Distributed Transactions

**Add Dependencies:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jta-atomikos</artifactId>
</dependency>
```

**Configuration:**

```java
@Configuration
@EnableTransactionManagement
public class JtaConfig {
    
    @Bean
    public DataSource primaryDataSource() {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setUniqueResourceName("primaryDS");
        ds.setXaDataSourceClassName("com.mysql.cj.jdbc.MysqlXADataSource");
        
        Properties props = new Properties();
        props.setProperty("url", "jdbc:mysql://localhost:3306/primary_db");
        props.setProperty("user", "primary_user");
        props.setProperty("password", "primary_pass");
        ds.setXaProperties(props);
        
        return ds;
    }
    
    @Bean
    public DataSource secondaryDataSource() {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setUniqueResourceName("secondaryDS");
        ds.setXaDataSourceClassName("org.postgresql.xa.PGXADataSource");
        
        Properties props = new Properties();
        props.setProperty("url", "jdbc:postgresql://localhost:5432/secondary_db");
        props.setProperty("user", "secondary_user");
        props.setProperty("password", "secondary_pass");
        ds.setXaProperties(props);
        
        return ds;
    }
    
    @Bean
    public JtaTransactionManager transactionManager() {
        UserTransactionManager utm = new UserTransactionManager();
        UserTransactionImp uti = new UserTransactionImp();
        return new JtaTransactionManager(uti, utm);
    }
}
```

**Usage:**

```java
@Service
public class OrderService {
    
    @Transactional  // Now uses JTA transaction manager
    public void createOrder(Long userId, Long productId) {
        // Both operations in single distributed transaction
        User user = userRepository.findById(userId).orElseThrow();
        user.setOrderCount(user.getOrderCount() + 1);
        userRepository.save(user);
        
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(product.getStock() - 1);
        productRepository.save(product);
        
        // Both commit or both rollback atomically
    }
}
```

**JTA Considerations:**
- ✅ True ACID guarantees across databases
- ❌ Performance overhead (2-phase commit)
- ❌ Complexity in configuration
- ❌ Not all databases support XA transactions
- ❌ Can cause deadlocks and timeouts

### Solution 3: Saga Pattern (Recommended for Microservices)

```java
@Service
public class OrderSagaService {
    
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public void createOrder(Long userId, Long productId) {
        try {
            // Step 1: Reserve product
            Product product = productRepository.findById(productId).orElseThrow();
            product.setStock(product.getStock() - 1);
            productRepository.save(product);
            
            try {
                // Step 2: Update user
                User user = userRepository.findById(userId).orElseThrow();
                user.setOrderCount(user.getOrderCount() + 1);
                userRepository.save(user);
                
                // Success - publish event
                kafkaTemplate.send("order-created", "Order created successfully");
                
            } catch (Exception e) {
                // Compensating transaction: Restore product stock
                product.setStock(product.getStock() + 1);
                productRepository.save(product);
                throw e;
            }
            
        } catch (Exception e) {
            // Handle failure
            kafkaTemplate.send("order-failed", "Order creation failed");
            throw e;
        }
    }
}
```

---

## Dynamic DataSource Routing

Use `AbstractRoutingDataSource` to switch datasources at runtime.

### Configuration

```java
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}

public class DataSourceContextHolder {
    
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();
    
    public static void setDataSourceType(String dataSourceType) {
        contextHolder.set(dataSourceType);
    }
    
    public static String getDataSourceType() {
        return contextHolder.get();
    }
    
    public static void clearDataSourceType() {
        contextHolder.remove();
    }
}

@Configuration
public class DynamicDataSourceConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @Primary
    public DataSource routingDataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("PRIMARY", primaryDataSource());
        targetDataSources.put("SECONDARY", secondaryDataSource());
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource());
        
        return routingDataSource;
    }
}
```

### Custom Annotation

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {
    String value() default "PRIMARY";
}

@Aspect
@Component
public class DataSourceAspect {
    
    @Around("@annotation(dataSource)")
    public Object switchDataSource(ProceedingJoinPoint joinPoint, DataSource dataSource) throws Throwable {
        String dsType = dataSource.value();
        DataSourceContextHolder.setDataSourceType(dsType);
        
        try {
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

### Usage

```java
@Service
public class DynamicService {
    
    @DataSource("PRIMARY")
    public void saveToPrimary(User user) {
        userRepository.save(user);
    }
    
    @DataSource("SECONDARY")
    public void saveToSecondary(Product product) {
        productRepository.save(product);
    }
}
```

---

## Testing Strategies

### Test Configuration

```java
@TestConfiguration
public class TestDataSourceConfig {
    
    @Bean
    @Primary
    public DataSource primaryTestDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("primary_test_db")
                .build();
    }
    
    @Bean
    public DataSource secondaryTestDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("secondary_test_db")
                .build();
    }
}
```

### Integration Test

```java
@SpringBootTest
@Import(TestDataSourceConfig.class)
class MultiDataSourceIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    @Transactional("primaryTransactionManager")
    void testSaveUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        User saved = userRepository.save(user);
        assertNotNull(saved.getId());
    }
    
    @Test
    @Transactional("secondaryTransactionManager")
    void testSaveProduct() {
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("99.99"));
        
        Product saved = productRepository.save(product);
        assertNotNull(saved.getId());
    }
}
```

### Testcontainers Approach

```java
@SpringBootTest
@Testcontainers
class MultiDataSourceTestcontainersTest {
    
    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("primary_db")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("secondary_db")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.primary.jdbc-url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.primary.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.primary.password", mysqlContainer::getPassword);
        
        registry.add("spring.datasource.secondary.jdbc-url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.secondary.username", postgresContainer::getUsername);
        registry.add("spring.datasource.secondary.password", postgresContainer::getPassword);
    }
    
    @Test
    void testMultiDatabaseOperations() {
        // Test with real databases
    }
}
```

---

## Common Pitfalls and Solutions

### Pitfall 1: Forgetting @Primary

**Problem:**
```java
// No @Primary annotation
@Bean
public DataSource primaryDataSource() { ... }

@Bean
public DataSource secondaryDataSource() { ... }
```

**Error:**
```
NoUniqueBeanDefinitionException: No qualifying bean of type 'javax.sql.DataSource' available: 
expected single matching bean but found 2
```

**Solution:** Add `@Primary` to one datasource.

### Pitfall 2: Using `url` Instead of `jdbc-url`

**Problem:**
```yaml
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/db1  # Wrong!
```

**Solution:**
```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:mysql://localhost:3306/db1  # Correct!
```

### Pitfall 3: Wrong Package Scanning

**Problem:**
```java
@EnableJpaRepositories(
    basePackages = "com.example.repository",  // Too broad!
    entityManagerFactoryRef = "primaryEntityManagerFactory"
)
```

**Solution:** Use specific packages:
```java
@EnableJpaRepositories(
    basePackages = "com.example.primary.repository",  // Specific!
    entityManagerFactoryRef = "primaryEntityManagerFactory"
)
```

### Pitfall 4: Transaction Manager Not Specified

**Problem:**
```java
@Transactional  // Which transaction manager?
public void saveProduct(Product product) {
    productRepository.save(product);  // May use wrong datasource!
}
```

**Solution:**
```java
@Transactional("secondaryTransactionManager")  // Explicit!
public void saveProduct(Product product) {
    productRepository.save(product);
}
```

### Pitfall 5: Assuming Cross-Database Transactions

**Problem:**
```java
@Transactional("primaryTransactionManager")
public void saveUserAndProduct(User user, Product product) {
    userRepository.save(user);  // Primary DB
    productRepository.save(product);  // Secondary DB - NOT in same transaction!
}
```

**Solution:** Use JTA or implement Saga pattern.

---

## Performance Optimization

### 1. Connection Pool Tuning

```yaml
spring:
  datasource:
    primary:
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        leak-detection-threshold: 60000
```

**Guidelines:**
- `maximum-pool-size`: CPU cores * 2 + disk spindles
- `minimum-idle`: Same as maximum for stable performance
- `connection-timeout`: 30 seconds (default)
- `idle-timeout`: 10 minutes
- `max-lifetime`: 30 minutes (less than DB timeout)

### 2. Read Replicas

```java
@Configuration
public class ReadReplicaConfig {
    
    @Bean
    public DataSource primaryDataSource() {
        // Master database
    }
    
    @Bean
    public DataSource readReplicaDataSource() {
        // Read replica
    }
    
    @Bean
    @Primary
    public DataSource routingDataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("WRITE", primaryDataSource());
        targetDataSources.put("READ", readReplicaDataSource());
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource());
        
        return routingDataSource;
    }
}

@Service
public class UserService {
    
    @Transactional(readOnly = true)
    @DataSource("READ")
    public List<User> getAllUsers() {
        return userRepository.findAll();  // Routes to read replica
    }
    
    @Transactional
    @DataSource("WRITE")
    public User createUser(User user) {
        return userRepository.save(user);  // Routes to master
    }
}
```

### 3. Lazy Initialization

```yaml
spring:
  jpa:
    properties:
      hibernate:
        enable_lazy_load_no_trans: false  # Avoid N+1 queries
```

### 4. Batch Operations

```java
@Service
public class BatchService {
    
    @Transactional("primaryTransactionManager")
    public void batchInsertUsers(List<User> users) {
        int batchSize = 50;
        for (int i = 0; i < users.size(); i++) {
            userRepository.save(users.get(i));
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
```

---

## Production Best Practices

### 1. Health Checks

```java
@Component
public class DataSourceHealthIndicator implements HealthIndicator {
    
    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;
    
    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;
    
    @Override
    public Health health() {
        try {
            checkDataSource(primaryDataSource, "primary");
            checkDataSource(secondaryDataSource, "secondary");
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
    
    private void checkDataSource(DataSource ds, String name) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException(name + " datasource is not valid");
            }
        }
    }
}
```

### 2. Monitoring

```java
@Configuration
public class DataSourceMetricsConfig {
    
    @Bean
    public MeterBinder hikariMetrics(@Qualifier("primaryDataSource") DataSource primaryDs,
                                      @Qualifier("secondaryDataSource") DataSource secondaryDs) {
        return (registry) -> {
            if (primaryDs instanceof HikariDataSource) {
                HikariMetrics.monitor(registry, (HikariDataSource) primaryDs, "primary");
            }
            if (secondaryDs instanceof HikariDataSource) {
                HikariMetrics.monitor(registry, (HikariDataSource) secondaryDs, "secondary");
            }
        };
    }
}
```

### 3. Secrets Management

```yaml
# Don't hardcode credentials!
spring:
  datasource:
    primary:
      jdbc-url: ${PRIMARY_DB_URL}
      username: ${PRIMARY_DB_USERNAME}
      password: ${PRIMARY_DB_PASSWORD}
    secondary:
      jdbc-url: ${SECONDARY_DB_URL}
      username: ${SECONDARY_DB_USERNAME}
      password: ${SECONDARY_DB_PASSWORD}
```

### 4. Circuit Breaker

```java
@Service
public class ResilientService {
    
    @CircuitBreaker(name = "primaryDB", fallbackMethod = "fallbackGetUser")
    @Transactional("primaryTransactionManager")
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
    
    public User fallbackGetUser(Long id, Exception e) {
        // Return cached data or default
        return getCachedUser(id);
    }
}
```

---

## Interview Questions

### Q1: Why use multiple datasources?

**Answer:**
- Legacy system integration during migration
- CQRS pattern (separate read/write databases)
- Multi-tenancy with database-per-tenant
- Reporting database separate from transactional database
- Microservices integration (anti-pattern but sometimes necessary)

### Q2: Can you have a single transaction across multiple databases?

**Answer:**
No, not with standard Spring transactions. Each datasource has its own transaction manager. Options:
1. **JTA/XA**: Distributed transactions with 2-phase commit (performance overhead)
2. **Saga Pattern**: Compensating transactions for eventual consistency
3. **Manual Management**: Commit/rollback both transactions manually (not truly atomic)

### Q3: What is @Primary annotation used for?

**Answer:**
When multiple beans of the same type exist, `@Primary` marks one as the default. Required for one datasource to avoid `NoUniqueBeanDefinitionException`. The primary datasource is used when no explicit qualifier is specified.

### Q4: Difference between `url` and `jdbc-url` in configuration?

**Answer:**
- `url`: Used for single datasource configuration
- `jdbc-url`: Required when using `@ConfigurationProperties` with multiple datasources
- Using `url` with multiple datasources causes binding issues

### Q5: How to test multiple datasources?

**Answer:**
1. **Embedded Databases**: H2 for unit tests
2. **Testcontainers**: Real databases in Docker containers
3. **Test Profiles**: Separate configuration for tests
4. **@DataJpaTest**: For repository layer testing

### Q6: Performance impact of multiple datasources?

**Answer:**
- Connection pool overhead (more connections)
- No connection reuse across datasources
- Increased memory usage
- Potential for connection exhaustion
- Mitigation: Proper pool sizing, monitoring, circuit breakers

### Q7: How to handle datasource failures?

**Answer:**
1. **Circuit Breaker**: Fail fast when datasource is down
2. **Retry Logic**: Exponential backoff for transient failures
3. **Fallback**: Return cached data or default values
4. **Health Checks**: Monitor datasource availability
5. **Graceful Degradation**: Continue with available datasources

### Q8: Can repositories from different datasources be used in same service?

**Answer:**
Yes, but be careful:
- Each repository uses its own datasource
- No atomic transactions across datasources
- Explicitly specify transaction manager
- Consider eventual consistency patterns

### Q9: How to implement read-write splitting?

**Answer:**
Use `AbstractRoutingDataSource`:
1. Create routing datasource with master and replica
2. Use ThreadLocal to store routing key
3. Create custom annotation (@ReadOnly, @WriteOnly)
4. Use AOP to set routing key before method execution
5. Route read-only transactions to replicas

### Q10: What are alternatives to multiple datasources?

**Answer:**
1. **Single Database with Schemas**: Logical separation
2. **Microservices**: Each service has its own database
3. **Database Views**: Virtual tables for read operations
4. **Materialized Views**: Pre-computed aggregations
5. **Event Sourcing**: Separate write and read models

---

## Summary

### Key Takeaways

1. **Configuration**: Use separate config classes with `@EnableJpaRepositories`
2. **@Primary**: Required on one datasource to avoid ambiguity
3. **Transactions**: Explicitly specify transaction manager
4. **No Cross-DB Transactions**: Use JTA or Saga pattern
5. **Package Structure**: Separate packages for each datasource
6. **Testing**: Use Testcontainers or embedded databases
7. **Monitoring**: Track connection pools and health
8. **Performance**: Tune connection pools appropriately

### When to Use

✅ **Use Multiple DataSources When:**
- Migrating from legacy systems
- Implementing CQRS
- Multi-tenant architecture
- Separate reporting database

❌ **Avoid When:**
- Single database with multiple schemas suffices
- Can use microservices instead
- Need frequent cross-database joins
- Team lacks expertise in distributed systems

### Production Checklist

- [ ] Connection pool properly sized
- [ ] Health checks implemented
- [ ] Metrics and monitoring configured
- [ ] Secrets externalized
- [ ] Circuit breakers in place
- [ ] Retry logic implemented
- [ ] Transaction managers explicitly specified
- [ ] Integration tests with real databases
- [ ] Fallback strategies defined
- [ ] Documentation updated

---

**Related Topics:**
- [Spring_Transactional_Deep_Dive.md](Spring_Transactional_Deep_Dive.md)
- [Database_Partitioning_And_Sharding.md](Database_Partitioning_And_Sharding.md)
- [Spring_Data_JPA_Part1.md](spring/03_Spring_Data_JPA_Part1.md)
