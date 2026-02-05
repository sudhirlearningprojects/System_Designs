# Spring Core - IoC & Dependency Injection

[← Back to Index](README.md)

## Table of Contents
- [Theory: Understanding IoC & DI](#theory-understanding-ioc--di)
- [What is IoC?](#what-is-ioc)
- [Dependency Injection Types](#dependency-injection-types)
- [Bean Scopes](#bean-scopes)
- [Bean Lifecycle](#bean-lifecycle)

---

## Theory: Understanding IoC & DI

### What Problem Does IoC Solve?

**Traditional Programming (Tight Coupling)**:
- Objects create their own dependencies
- Hard to test (can't mock dependencies)
- Difficult to change implementations
- Violates Single Responsibility Principle

**IoC Solution (Loose Coupling)**:
- Framework creates and injects dependencies
- Easy to test (inject mocks)
- Easy to swap implementations
- Follows Dependency Inversion Principle

### Core Concepts

**1. Inversion of Control (IoC)**
- Control of object creation is inverted from application to framework
- Spring Container manages object lifecycle
- Objects declare dependencies, Spring provides them

**2. Dependency Injection (DI)**
- Technique to implement IoC
- Dependencies are "injected" rather than created
- Three types: Constructor, Setter, Field

**3. Spring Container (ApplicationContext)**
- Creates and manages beans
- Resolves dependencies
- Manages bean lifecycle
- Two types: BeanFactory (basic) and ApplicationContext (advanced)

### Benefits
- ✅ Loose coupling between components
- ✅ Easy unit testing with mocks
- ✅ Better code organization
- ✅ Centralized configuration
- ✅ Aspect-Oriented Programming support

---

## What is IoC (Inversion of Control)?

**Traditional Approach** (You control object creation):
```java
public class UserService {
    private UserRepository repository = new UserRepository(); // Tight coupling
    
    public User getUser(Long id) {
        return repository.findById(id);
    }
}
```

**IoC Approach** (Spring controls object creation):
```java
@Service
public class UserService {
    private final UserRepository repository;
    
    @Autowired // Spring injects dependency
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
    
    public User getUser(Long id) {
        return repository.findById(id);
    }
}
```

---

## Dependency Injection Types

### 1. Constructor Injection (✅ Recommended)
```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    @Autowired // Optional in Spring 4.3+
    public OrderService(PaymentService paymentService, 
                       InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```

**Benefits**:
- Immutable dependencies (final fields)
- Easy to test (constructor parameters)
- Fails fast if dependencies missing
- Clear dependencies

### 2. Setter Injection
```java
@Service
public class EmailService {
    private NotificationService notificationService;
    
    @Autowired
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

**Use Cases**:
- Optional dependencies
- Reconfiguration at runtime

### 3. Field Injection (❌ Not Recommended)
```java
@Service
public class ProductService {
    @Autowired
    private ProductRepository repository; // Hard to test
}
```

**Issues**:
- Cannot use final fields
- Hard to test (requires reflection)
- Hidden dependencies
- Tight coupling to Spring

---

## Bean Scopes

### Singleton (Default)
```java
@Service
@Scope("singleton")
public class ConfigService { }
```
- One instance per Spring container
- Shared across application
- Thread-safe considerations needed

### Prototype
```java
@Service
@Scope("prototype")
public class ReportGenerator { }
```
- New instance every time requested
- Not managed after creation
- No destruction callbacks

### Request (Web)
```java
@Controller
@Scope("request")
public class RequestScopedBean { }
```
- One instance per HTTP request
- Web applications only

### Session (Web)
```java
@Controller
@Scope("session")
public class ShoppingCart { }
```
- One instance per HTTP session
- Web applications only

### Application (Web)
```java
@Component
@Scope("application")
public class AppConfig { }
```
- One instance per ServletContext

---

## Bean Lifecycle

```java
@Component
public class DatabaseConnection implements InitializingBean, DisposableBean {
    
    @PostConstruct
    public void init() {
        System.out.println("1. @PostConstruct - Initialize connection");
    }
    
    @Override
    public void afterPropertiesSet() {
        System.out.println("2. afterPropertiesSet - Properties set");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("3. @PreDestroy - Close connection");
    }
    
    @Override
    public void destroy() {
        System.out.println("4. destroy - Final cleanup");
    }
}
```

### Lifecycle Phases
1. **Instantiation** - Bean created
2. **Populate Properties** - Dependencies injected
3. **BeanNameAware** - Set bean name
4. **BeanFactoryAware** - Set bean factory
5. **ApplicationContextAware** - Set application context
6. **@PostConstruct** - Custom initialization
7. **InitializingBean.afterPropertiesSet()** - Bean ready
8. **Custom init-method** - Additional initialization
9. **Bean Ready** - Available for use
10. **@PreDestroy** - Before destruction
11. **DisposableBean.destroy()** - Cleanup
12. **Custom destroy-method** - Final cleanup

---

## Best Practices

✅ **DO**:
- Use constructor injection for required dependencies
- Use setter injection for optional dependencies
- Keep beans stateless when possible
- Use appropriate scopes

❌ **DON'T**:
- Use field injection in production code
- Create circular dependencies
- Mix business logic with Spring configuration
- Overuse prototype scope

---

[Next: Spring Boot →](02_Spring_Boot.md)
