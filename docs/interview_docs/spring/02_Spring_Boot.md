# Spring Boot - Rapid Application Development

[← Back to Index](README.md) | [← Previous: Spring Core](01_Spring_Core_IoC_DI.md) | [Next: Spring Data JPA Part 1 →](03_Spring_Data_JPA_Part1.md)

## Table of Contents
- [Theory: Understanding Spring Boot](#theory-understanding-spring-boot)
- [Auto-Configuration](#auto-configuration)
- [Starter Dependencies](#starter-dependencies)
- [Configuration Properties](#configuration-properties)
- [Profiles](#profiles)

---

## Theory: Understanding Spring Boot

### What is Spring Boot?

Spring Boot is an **opinionated framework** built on top of Spring Framework that simplifies application development by:
- Eliminating boilerplate configuration
- Providing sensible defaults
- Embedding web servers (Tomcat, Jetty, Undertow)
- Offering production-ready features out-of-the-box

### Core Principles

**1. Convention Over Configuration**
- Sensible defaults for common scenarios
- Minimal configuration required
- Override only when needed

**2. Standalone Applications**
- Embedded web server (no WAR deployment)
- Self-contained JAR with all dependencies
- Run with `java -jar app.jar`

**3. Production-Ready**
- Health checks
- Metrics
- Externalized configuration
- Logging

### How Auto-Configuration Works

```
@SpringBootApplication
       ↓
@EnableAutoConfiguration
       ↓
Scans classpath for libraries
       ↓
Applies conditional configuration
       ↓
Creates beans automatically
```

**Example**: If `spring-boot-starter-data-jpa` is on classpath:
- Auto-configures DataSource
- Creates EntityManagerFactory
- Sets up TransactionManager
- Configures JPA repositories

### Spring vs Spring Boot

| Aspect | Spring | Spring Boot |
|--------|--------|-------------|
| Configuration | XML/Java Config | Auto-configuration |
| Deployment | WAR to server | Embedded server |
| Dependencies | Manual management | Starter POMs |
| Setup Time | Hours | Minutes |
| Boilerplate | High | Minimal |

### When to Use Spring Boot?

✅ **Use Spring Boot**:
- Microservices
- REST APIs
- Rapid prototyping
- Cloud-native applications
- Standalone applications

❌ **Consider Plain Spring**:
- Legacy enterprise applications
- Need fine-grained control
- Existing complex configuration
- Non-standard requirements

---

## Auto-Configuration

### Without Spring Boot
```java
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.example")
public class WebConfig implements WebMvcConfigurer {
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://localhost:5432/mydb");
        ds.setUsername("user");
        ds.setPassword("pass");
        return ds;
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### With Spring Boot
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
```

```java
@SpringBootApplication // Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## Starter Dependencies

```xml
<!-- Web Applications -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Data Access -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Reactive Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Batch Processing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

---

## Configuration Properties

```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppConfig {
    private String name;
    private int maxConnections;
    private List<String> allowedOrigins;
    private Database database;
    
    // Getters and setters
    
    public static class Database {
        private int poolSize;
        private int timeout;
        // Getters and setters
    }
}
```

```properties
# application.properties
app.name=MyApp
app.max-connections=100
app.allowed-origins=http://localhost:3000,http://example.com
app.database.pool-size=20
app.database.timeout=30
```

---

## Profiles

### Configuration Classes
```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        return new H2DataSource(); // In-memory DB for dev
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() {
        return new PostgreSQLDataSource(); // Production DB
    }
}
```

### Profile-Specific Properties
```properties
# application-dev.properties
spring.datasource.url=jdbc:h2:mem:testdb
logging.level.root=DEBUG

# application-prod.properties
spring.datasource.url=jdbc:postgresql://prod-server:5432/mydb
logging.level.root=WARN
```

### Activating Profiles
```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable
export SPRING_PROFILES_ACTIVE=prod

# application.properties
spring.profiles.active=dev
```

---

[← Previous: Spring Core](01_Spring_Core_IoC_DI.md) | [Next: Spring Data JPA Part 1 →](03_Spring_Data_JPA_Part1.md)
