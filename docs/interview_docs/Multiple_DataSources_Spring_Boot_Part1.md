# Multiple DataSources in Spring Boot - Part 1

## Table of Contents
- [Introduction](#introduction)
- [When to Use Multiple DataSources](#when-to-use-multiple-datasources)
- [Basic Configuration](#basic-configuration)
- [Configuration Approaches](#configuration-approaches)
- [Detailed Implementation](#detailed-implementation)

---

## Introduction

Configuring multiple datasources in Spring Boot allows your application to connect to different databases simultaneously. This is common in enterprise applications that need to:
- Read from legacy systems while writing to new systems
- Separate read and write operations (CQRS pattern)
- Integrate with multiple microservices databases
- Implement multi-tenancy with database-per-tenant

---

## When to Use Multiple DataSources

### ✅ Valid Use Cases
1. **Legacy System Integration**: Reading from old database while migrating to new one
2. **CQRS Pattern**: Separate read and write databases
3. **Multi-Tenancy**: Different database per tenant
4. **Reporting Database**: Separate analytics database from transactional database
5. **Microservices Integration**: Accessing multiple service databases (anti-pattern but sometimes necessary)
6. **Sharding**: Horizontal partitioning across multiple databases

### ❌ When NOT to Use
1. **Single Database with Multiple Schemas**: Use single datasource with schema configuration
2. **Temporary Data**: Use in-memory databases or caching instead
3. **Cross-Database Joins**: Indicates poor database design; refactor instead

---

## Basic Configuration

### Step 1: Add Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- HikariCP (included in spring-boot-starter-data-jpa) -->
</dependencies>
```

### Step 2: Application Properties

```yaml
# application.yml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:mysql://localhost:3306/primary_db
      username: primary_user
      password: primary_pass
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
    
    secondary:
      jdbc-url: jdbc:postgresql://localhost:5432/secondary_db
      username: secondary_user
      password: secondary_pass
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
  
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

**Important Notes:**
- Use `jdbc-url` instead of `url` when configuring multiple datasources
- Each datasource can have its own connection pool configuration
- Don't use `spring.datasource.url` at root level when using multiple datasources

---

## Configuration Approaches

### Approach 1: Separate Configuration Classes (Recommended)

**Pros:**
- Clear separation of concerns
- Easy to maintain and test
- Type-safe configuration
- Better IDE support

**Cons:**
- More boilerplate code
- Requires understanding of Spring configuration

### Approach 2: Single Configuration Class

**Pros:**
- Less code
- All datasource config in one place

**Cons:**
- Can become cluttered
- Harder to maintain as complexity grows

### Approach 3: AbstractRoutingDataSource (Dynamic Routing)

**Pros:**
- Runtime datasource switching
- Useful for multi-tenancy
- Single repository layer

**Cons:**
- More complex
- Requires careful thread-local management
- Not suitable for all scenarios

---

## Detailed Implementation

### Project Structure

```
src/main/java/com/example/
├── config/
│   ├── PrimaryDataSourceConfig.java
│   └── SecondaryDataSourceConfig.java
├── primary/
│   ├── entity/
│   │   └── User.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       └── UserService.java
└── secondary/
    ├── entity/
    │   └── Product.java
    ├── repository/
    │   └── ProductRepository.java
    └── service/
        └── ProductService.java
```

### Primary DataSource Configuration

```java
package com.example.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.primary.repository",
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryDataSourceConfig {
    
    /**
     * Primary DataSource Bean
     * @Primary annotation makes this the default datasource
     */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * Primary EntityManagerFactory
     * Manages JPA entities for primary database
     */
    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") DataSource dataSource) {
        
        return builder
                .dataSource(dataSource)
                .packages("com.example.primary.entity")
                .persistenceUnit("primary")
                .build();
    }
    
    /**
     * Primary TransactionManager
     * Manages transactions for primary database
     */
    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

### Secondary DataSource Configuration

```java
package com.example.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.secondary.repository",
    entityManagerFactoryRef = "secondaryEntityManagerFactory",
    transactionManagerRef = "secondaryTransactionManager"
)
public class SecondaryDataSourceConfig {
    
    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("secondaryDataSource") DataSource dataSource) {
        
        return builder
                .dataSource(dataSource)
                .packages("com.example.secondary.entity")
                .persistenceUnit("secondary")
                .build();
    }
    
    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

### Entity Classes

**Primary Database Entity:**

```java
package com.example.primary.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String email;
    
    private String firstName;
    private String lastName;
}
```

**Secondary Database Entity:**

```java
package com.example.secondary.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    private Integer stock;
}
```

### Repository Interfaces

**Primary Repository:**

```java
package com.example.primary.repository;

import com.example.primary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);
}
```

**Secondary Repository:**

```java
package com.example.secondary.repository;

import com.example.secondary.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByPriceLessThan(BigDecimal price);
    List<Product> findByStockGreaterThan(Integer stock);
}
```

### Service Layer

**Primary Service:**

```java
package com.example.primary.service;

import com.example.primary.entity.User;
import com.example.primary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional("primaryTransactionManager")
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

**Secondary Service:**

```java
package com.example.secondary.service;

import com.example.secondary.entity.Product;
import com.example.secondary.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional("secondaryTransactionManager")
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional(value = "secondaryTransactionManager", readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
```

---

## Key Configuration Elements Explained

### 1. @EnableJpaRepositories

```java
@EnableJpaRepositories(
    basePackages = "com.example.primary.repository",  // Where to scan for repositories
    entityManagerFactoryRef = "primaryEntityManagerFactory",  // Which EMF to use
    transactionManagerRef = "primaryTransactionManager"  // Which TM to use
)
```

**Purpose:** Tells Spring Data JPA where to find repositories and which EntityManagerFactory and TransactionManager to use.

### 2. @Primary Annotation

```java
@Primary
@Bean(name = "primaryDataSource")
public DataSource primaryDataSource() { ... }
```

**Purpose:** Marks this bean as the default when multiple beans of the same type exist. Required for one datasource to avoid ambiguity.

### 3. @ConfigurationProperties

```java
@ConfigurationProperties(prefix = "spring.datasource.primary")
public DataSource primaryDataSource() { ... }
```

**Purpose:** Binds properties from application.yml to the DataSource configuration automatically.

### 4. EntityManagerFactoryBuilder

```java
public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        @Qualifier("primaryDataSource") DataSource dataSource) { ... }
```

**Purpose:** Provides a fluent API to build EntityManagerFactory with proper configuration.

### 5. @Qualifier

```java
@Qualifier("primaryDataSource") DataSource dataSource
```

**Purpose:** Specifies which bean to inject when multiple beans of the same type exist.

---

## Continue to Part 2

Part 2 covers:
- Advanced transaction management
- Cross-database transactions
- Dynamic datasource routing
- Testing strategies
- Common pitfalls and solutions
- Performance optimization
- Production best practices
