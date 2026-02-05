# Spring Framework Deep Dive - Navigation Guide

Complete guide to Spring Framework with detailed examples and best practices.

## 📚 Documentation Structure

### Core Spring Framework
- **[01_Spring_Core_IoC_DI.md](01_Spring_Core_IoC_DI.md)** - Inversion of Control, Dependency Injection, Bean Scopes, Lifecycle
- **[02_Spring_Boot.md](02_Spring_Boot.md)** - Auto-configuration, Starters, Profiles, Configuration Properties

### Data Access & Persistence
- **[03_Spring_Data_JPA_Part1.md](03_Spring_Data_JPA_Part1.md)** - Core Annotations, Entity Mapping, Column Types
- **[04_Spring_Data_JPA_Part2.md](04_Spring_Data_JPA_Part2.md)** - Relationships, Cascade Types, Fetch Strategies
- **[05_Spring_Data_JPA_Part3.md](05_Spring_Data_JPA_Part3.md)** - Repositories, Query Methods, Custom Queries
- **[06_Spring_Data_JPA_Part4.md](06_Spring_Data_JPA_Part4.md)** - Pagination, Specifications, Projections, Transactions

### Reactive Programming
- **[07_Spring_WebFlux.md](07_Spring_WebFlux.md)** - Reactive Programming, Mono/Flux, WebClient, R2DBC

### Security
- **[08_Spring_Security.md](08_Spring_Security.md)** - Authentication, Authorization, JWT, OAuth2, Method Security

### Microservices & Cloud
- **[09_Spring_Cloud.md](09_Spring_Cloud.md)** - Service Discovery, Load Balancing, Circuit Breaker, API Gateway

### Messaging & Events
- **[10_Spring_Kafka.md](10_Spring_Kafka.md)** - Kafka Producer, Consumer, Event-Driven Architecture

### Cross-Cutting Concerns
- **[11_Spring_AOP.md](11_Spring_AOP.md)** - Aspect-Oriented Programming, Advice Types, Custom Annotations

### Batch Processing
- **[12_Spring_Batch_Part1.md](12_Spring_Batch_Part1.md)** - Core Concepts, Annotations, Job Configuration
- **[13_Spring_Batch_Part2.md](13_Spring_Batch_Part2.md)** - Readers, Processors, Writers, Error Handling

### Integration
- **[14_Spring_Integration.md](14_Spring_Integration.md)** - Enterprise Integration Patterns, Message Channels, File Processing

---

## 🔍 Quick Search Guide

### By Topic

**Dependency Injection**
- Constructor, Setter, Field Injection → [01_Spring_Core_IoC_DI.md](01_Spring_Core_IoC_DI.md#dependency-injection-types)

**Database Operations**
- Entity Mapping → [03_Spring_Data_JPA_Part1.md](03_Spring_Data_JPA_Part1.md)
- Relationships → [04_Spring_Data_JPA_Part2.md](04_Spring_Data_JPA_Part2.md)
- Query Methods → [05_Spring_Data_JPA_Part3.md](05_Spring_Data_JPA_Part3.md)
- Transactions → [06_Spring_Data_JPA_Part4.md](06_Spring_Data_JPA_Part4.md#transactional)

**Reactive Programming**
- Mono/Flux → [07_Spring_WebFlux.md](07_Spring_WebFlux.md#mono-vs-flux)
- WebClient → [07_Spring_WebFlux.md](07_Spring_WebFlux.md#webclient)

**Security**
- JWT Authentication → [08_Spring_Security.md](08_Spring_Security.md#jwt-authentication)
- Method Security → [08_Spring_Security.md](08_Spring_Security.md#method-security)

**Microservices**
- Service Discovery → [09_Spring_Cloud.md](09_Spring_Cloud.md#service-discovery)
- Circuit Breaker → [09_Spring_Cloud.md](09_Spring_Cloud.md#circuit-breaker)

**Batch Processing**
- Job Configuration → [12_Spring_Batch_Part1.md](12_Spring_Batch_Part1.md)
- ItemReader/Writer → [13_Spring_Batch_Part2.md](13_Spring_Batch_Part2.md)

### By Annotation

| Annotation | File | Description |
|------------|------|-------------|
| `@Autowired` | [01_Spring_Core_IoC_DI.md](01_Spring_Core_IoC_DI.md) | Dependency injection |
| `@Entity` | [03_Spring_Data_JPA_Part1.md](03_Spring_Data_JPA_Part1.md) | JPA entity |
| `@Transactional` | [06_Spring_Data_JPA_Part4.md](06_Spring_Data_JPA_Part4.md) | Transaction management |
| `@EnableBatchProcessing` | [12_Spring_Batch_Part1.md](12_Spring_Batch_Part1.md) | Enable Spring Batch |
| `@KafkaListener` | [10_Spring_Kafka.md](10_Spring_Kafka.md) | Kafka consumer |
| `@CircuitBreaker` | [09_Spring_Cloud.md](09_Spring_Cloud.md) | Resilience4j circuit breaker |

### By Use Case

**Building REST APIs** → [02_Spring_Boot.md](02_Spring_Boot.md), [07_Spring_WebFlux.md](07_Spring_WebFlux.md)

**Database CRUD Operations** → [03-06_Spring_Data_JPA](03_Spring_Data_JPA_Part1.md)

**Securing Applications** → [08_Spring_Security.md](08_Spring_Security.md)

**Building Microservices** → [09_Spring_Cloud.md](09_Spring_Cloud.md)

**Processing Large Datasets** → [12-13_Spring_Batch](12_Spring_Batch_Part1.md)

**Event-Driven Architecture** → [10_Spring_Kafka.md](10_Spring_Kafka.md), [14_Spring_Integration.md](14_Spring_Integration.md)

---

## 📖 Reading Order

### For Beginners
1. Spring Core (IoC & DI)
2. Spring Boot
3. Spring Data JPA (Parts 1-4)
4. Spring Security
5. Spring AOP

### For Intermediate
1. Spring WebFlux
2. Spring Cloud
3. Spring Kafka
4. Spring Batch

### For Advanced
1. Spring Integration
2. Advanced Spring Data JPA patterns
3. Custom Spring Boot Starters
4. Performance Optimization

---

## 🎯 Common Patterns

**Repository Pattern** → [05_Spring_Data_JPA_Part3.md](05_Spring_Data_JPA_Part3.md#repository-pattern)

**Service Layer Pattern** → [01_Spring_Core_IoC_DI.md](01_Spring_Core_IoC_DI.md)

**DTO Pattern** → [06_Spring_Data_JPA_Part4.md](06_Spring_Data_JPA_Part4.md#projections)

**Circuit Breaker Pattern** → [09_Spring_Cloud.md](09_Spring_Cloud.md#circuit-breaker)

**Saga Pattern** → [10_Spring_Kafka.md](10_Spring_Kafka.md)

---

## 💡 Best Practices

Each document includes:
- ✅ Recommended approaches
- ❌ Anti-patterns to avoid
- 🔥 Production-ready examples
- 📊 Performance considerations
- 🔒 Security best practices

---

## 🔗 External Resources

- [Spring Official Documentation](https://spring.io/projects)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

---

**Last Updated**: 2024
**Maintained By**: System Design Documentation Team
