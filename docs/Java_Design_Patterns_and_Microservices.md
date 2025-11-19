# Java Design Patterns & Microservices Design Patterns in Spring Framework

## Table of Contents
1. [Design Patterns Fundamentals](#design-patterns-fundamentals)
2. [Core Java Design Patterns](#core-java-design-patterns)
3. [Spring Framework Design Patterns](#spring-framework-design-patterns)
4. [Microservices Design Patterns](#microservices-design-patterns)
5. [Spring Boot Microservices Patterns](#spring-boot-microservices-patterns)
6. [Best Practices](#best-practices)

---

## Design Patterns Fundamentals

### What are Design Patterns?
Design patterns are **reusable solutions to commonly occurring problems** in software design. They represent best practices evolved over time by experienced developers and provide a common vocabulary for discussing design solutions.

### Why Use Design Patterns?
- **Reusability**: Proven solutions that can be applied across different contexts
- **Communication**: Common vocabulary for developers to discuss design decisions
- **Best Practices**: Encapsulate years of collective experience
- **Maintainability**: Lead to more organized and maintainable code
- **Problem-Solving**: Provide structured approaches to common challenges

### Categories of Design Patterns

#### 1. Creational Patterns
**Purpose**: Deal with object creation mechanisms
- **Problem**: Direct object instantiation can lead to design problems
- **Solution**: Create objects in a manner suitable to the situation
- **Examples**: Singleton, Factory, Builder, Prototype

#### 2. Structural Patterns
**Purpose**: Deal with object composition and relationships
- **Problem**: How to compose objects to form larger structures
- **Solution**: Identify simple ways to realize relationships between entities
- **Examples**: Adapter, Decorator, Facade, Proxy

#### 3. Behavioral Patterns
**Purpose**: Focus on communication between objects and assignment of responsibilities
- **Problem**: Complex control flow and communication between objects
- **Solution**: Define how objects interact and distribute responsibilities
- **Examples**: Observer, Strategy, Command, Template Method

### Design Principles Behind Patterns

#### SOLID Principles
- **S**ingle Responsibility: A class should have only one reason to change
- **O**pen/Closed: Open for extension, closed for modification
- **L**iskov Substitution: Objects should be replaceable with instances of their subtypes
- **I**nterface Segregation: Many client-specific interfaces are better than one general-purpose interface
- **D**ependency Inversion: Depend on abstractions, not concretions

#### Other Key Principles
- **Favor Composition over Inheritance**: More flexible and maintainable
- **Program to Interfaces**: Reduces coupling between components
- **Encapsulate What Varies**: Identify aspects that vary and separate them

---

## Core Java Design Patterns

### 1. Singleton Pattern
**Purpose**: Ensures only one instance of a class exists globally.

**Theory**: 
- **Problem**: Sometimes you need exactly one instance of a class (database connections, thread pools, caches)
- **Solution**: Make the class responsible for keeping track of its sole instance
- **Key Components**: Private constructor, static instance variable, static access method
- **Thread Safety**: Must handle concurrent access in multi-threaded environments
- **Spring Context**: Spring manages singletons by default, eliminating manual implementation

**Pure Java Implementation**:
```java
// Thread-safe Singleton with double-checked locking
public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    
    private DatabaseConnection() {
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method");
        }
    }
    
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }
}

// Enum Singleton (Best Practice)
public enum ConfigManager {
    INSTANCE;
    
    private Properties config = new Properties();
    
    public String getProperty(String key) {
        return config.getProperty(key);
    }
    
    public void setProperty(String key, String value) {
        config.setProperty(key, value);
    }
}

// Usage
ConfigManager.INSTANCE.setProperty("db.url", "localhost:5432");
String dbUrl = ConfigManager.INSTANCE.getProperty("db.url");
```

**Spring Implementation**:
```java
@Service
@Scope("singleton") // Default scope
public class UserService {
    // Spring manages singleton lifecycle
}
```

### 2. Factory Pattern
**Purpose**: Creates objects without specifying exact classes.

**Theory**:
- **Problem**: Direct object instantiation couples code to specific classes
- **Solution**: Delegate object creation to a factory method or class
- **Benefits**: Loose coupling, easier testing, centralized creation logic
- **Types**: Simple Factory, Factory Method, Abstract Factory
- **Spring Integration**: `@Configuration` classes act as factories, `@Bean` methods are factory methods

**Pure Java Implementation**:
```java
// Simple Factory
public interface PaymentProcessor {
    void processPayment(double amount);
}

public class StripeProcessor implements PaymentProcessor {
    public void processPayment(double amount) {
        System.out.println("Processing $" + amount + " via Stripe");
    }
}

public class PayPalProcessor implements PaymentProcessor {
    public void processPayment(double amount) {
        System.out.println("Processing $" + amount + " via PayPal");
    }
}

public class PaymentProcessorFactory {
    public static PaymentProcessor createProcessor(String type) {
        return switch (type.toLowerCase()) {
            case "stripe" -> new StripeProcessor();
            case "paypal" -> new PayPalProcessor();
            default -> throw new IllegalArgumentException("Unknown processor: " + type);
        };
    }
}

// Factory Method Pattern
public abstract class NotificationFactory {
    public abstract Notification createNotification();
    
    public void sendNotification(String message) {
        Notification notification = createNotification();
        notification.send(message);
    }
}

public class EmailNotificationFactory extends NotificationFactory {
    public Notification createNotification() {
        return new EmailNotification();
    }
}

public class SMSNotificationFactory extends NotificationFactory {
    public Notification createNotification() {
        return new SMSNotification();
    }
}

// Usage
PaymentProcessor processor = PaymentProcessorFactory.createProcessor("stripe");
processor.processPayment(100.0);

NotificationFactory factory = new EmailNotificationFactory();
factory.sendNotification("Hello World");
```

**Spring Implementation**:
```java
@Component
public class PaymentProcessorFactory {
    
    @Autowired
    private StripeProcessor stripeProcessor;
    
    @Autowired
    private PayPalProcessor paypalProcessor;
    
    public PaymentProcessor getProcessor(String type) {
        return switch (type.toLowerCase()) {
            case "stripe" -> stripeProcessor;
            case "paypal" -> paypalProcessor;
            default -> throw new IllegalArgumentException("Unknown processor: " + type);
        };
    }
}
```

### 3. Builder Pattern
**Purpose**: Constructs complex objects step by step.

**Theory**:
- **Problem**: Complex objects with many optional parameters lead to telescoping constructors
- **Solution**: Separate construction process from representation
- **Benefits**: Readable code, immutable objects, flexible construction
- **When to Use**: Objects with 4+ parameters, many optional parameters, need for immutability
- **Spring Usage**: Configuration builders, test data builders, DTO construction

**Pure Java Implementation**:
```java
// Immutable User class with Builder
public final class User {
    private final String name;
    private final String email;
    private final int age;
    private final List<String> roles;
    private final boolean active;
    
    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.age = builder.age;
        this.roles = Collections.unmodifiableList(new ArrayList<>(builder.roles));
        this.active = builder.active;
    }
    
    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
    public List<String> getRoles() { return roles; }
    public boolean isActive() { return active; }
    
    public static class Builder {
        private String name;
        private String email;
        private int age;
        private List<String> roles = new ArrayList<>();
        private boolean active = true;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public Builder addRole(String role) {
            this.roles.add(role);
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public User build() {
            if (name == null || email == null) {
                throw new IllegalStateException("Name and email are required");
            }
            return new User(this);
        }
    }
}

// Database Connection Builder
public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maxConnections;
    private final int timeout;
    
    private DatabaseConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.username = builder.username;
        this.password = builder.password;
        this.maxConnections = builder.maxConnections;
        this.timeout = builder.timeout;
    }
    
    public static class Builder {
        private String host = "localhost";
        private int port = 5432;
        private String database;
        private String username;
        private String password;
        private int maxConnections = 10;
        private int timeout = 30;
        
        public Builder host(String host) {
            this.host = host;
            return this;
        }
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder database(String database) {
            this.database = database;
            return this;
        }
        
        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }
        
        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }
        
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}

// Usage
User user = new User.Builder()
    .name("John Doe")
    .email("john@example.com")
    .age(30)
    .addRole("USER")
    .addRole("ADMIN")
    .active(true)
    .build();

DatabaseConfig dbConfig = new DatabaseConfig.Builder()
    .host("prod-db.company.com")
    .port(5432)
    .database("myapp")
    .credentials("admin", "secret")
    .maxConnections(50)
    .timeout(60)
    .build();
```

**Spring Implementation**:
```java
@Entity
public class User {
    private String name;
    private String email;
    private List<String> roles;
    
    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.roles = builder.roles;
    }
    
    public static class Builder {
        private String name;
        private String email;
        private List<String> roles = new ArrayList<>();
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder addRole(String role) {
            this.roles.add(role);
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}
```

### 4. Observer Pattern
**Purpose**: Notifies multiple objects about state changes.

**Theory**:
- **Problem**: One-to-many dependency between objects where changes in one affect many
- **Solution**: Define subscription mechanism to notify multiple objects
- **Components**: Subject (Observable), Observer, ConcreteSubject, ConcreteObserver
- **Benefits**: Loose coupling, dynamic relationships, broadcast communication
- **Spring Implementation**: ApplicationEventPublisher and @EventListener provide built-in observer pattern

**Pure Java Implementation**:
```java
// Observer interface
public interface Observer {
    void update(String message);
}

// Subject interface
public interface Subject {
    void addObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers(String message);
}

// Concrete Subject
public class NewsAgency implements Subject {
    private List<Observer> observers = new ArrayList<>();
    
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
    
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }
    
    public void setNews(String news) {
        notifyObservers(news);
    }
}

// Concrete Observers
public class NewsChannel implements Observer {
    private String name;
    
    public NewsChannel(String name) {
        this.name = name;
    }
    
    public void update(String news) {
        System.out.println(name + " received: " + news);
    }
}

// Usage
NewsAgency agency = new NewsAgency();
agency.addObserver(new NewsChannel("CNN"));
agency.addObserver(new NewsChannel("BBC"));
agency.setNews("Breaking News!");
```

**Spring Implementation**:
```java
@Component
public class OrderEventPublisher {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void createOrder(Order order) {
        // Business logic
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }
}

@EventListener
@Component
public class EmailNotificationService {
    
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Send email notification
        sendOrderConfirmationEmail(event.getOrder());
    }
}

@EventListener
@Component
public class InventoryService {
    
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Update inventory
        updateInventory(event.getOrder());
    }
}
```

### 5. Strategy Pattern
**Purpose**: Defines family of algorithms and makes them interchangeable.

**Theory**:
- **Problem**: Multiple ways to perform a task, need to switch algorithms at runtime
- **Solution**: Encapsulate algorithms in separate classes with common interface
- **Benefits**: Open/Closed principle, eliminates conditional statements, runtime algorithm selection
- **Components**: Strategy interface, ConcreteStrategy classes, Context class
- **Spring Usage**: Multiple implementations of same interface, auto-wired as Map or List

**Pure Java Implementation**:
```java
// Strategy interface
public interface PaymentStrategy {
    void pay(double amount);
}

// Concrete strategies
public class CreditCardPayment implements PaymentStrategy {
    private String cardNumber;
    
    public CreditCardPayment(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " using Credit Card");
    }
}

public class PayPalPayment implements PaymentStrategy {
    private String email;
    
    public PayPalPayment(String email) {
        this.email = email;
    }
    
    public void pay(double amount) {
        System.out.println("Paid $" + amount + " using PayPal");
    }
}

// Context class
public class ShoppingCart {
    private PaymentStrategy paymentStrategy;
    
    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.paymentStrategy = strategy;
    }
    
    public void checkout(double amount) {
        paymentStrategy.pay(amount);
    }
}

// Usage
ShoppingCart cart = new ShoppingCart();
cart.setPaymentStrategy(new CreditCardPayment("1234-5678"));
cart.checkout(100.0);

cart.setPaymentStrategy(new PayPalPayment("user@example.com"));
cart.checkout(50.0);
```

**Spring Implementation**:
```java
public interface DiscountStrategy {
    double calculateDiscount(double amount);
}

@Component("regularDiscount")
public class RegularCustomerDiscount implements DiscountStrategy {
    public double calculateDiscount(double amount) {
        return amount * 0.05; // 5% discount
    }
}

@Component("premiumDiscount")
public class PremiumCustomerDiscount implements DiscountStrategy {
    public double calculateDiscount(double amount) {
        return amount * 0.15; // 15% discount
    }
}

@Service
public class PricingService {
    
    @Autowired
    private Map<String, DiscountStrategy> discountStrategies;
    
    public double calculateFinalPrice(double amount, String customerType) {
        DiscountStrategy strategy = discountStrategies.get(customerType + "Discount");
        double discount = strategy.calculateDiscount(amount);
        return amount - discount;
    }
}
```

### 6. Template Method Pattern
**Purpose**: Defines skeleton of algorithm, subclasses override specific steps.

**Theory**:
- **Problem**: Multiple classes share similar algorithm structure but differ in specific steps
- **Solution**: Define algorithm skeleton in base class, let subclasses override specific steps
- **Benefits**: Code reuse, consistent algorithm structure, controlled extension points
- **Components**: AbstractClass with template method, ConcreteClass implementations
- **Spring Usage**: JdbcTemplate, RestTemplate, TransactionTemplate follow this pattern

**Pure Java Implementation**:
```java
// Abstract class with template method
public abstract class DataProcessor {
    
    // Template method - defines algorithm skeleton
    public final void processData() {
        loadData();
        validateData();
        transformData();
        saveData();
    }
    
    // Abstract methods - must be implemented
    protected abstract void loadData();
    protected abstract void transformData();
    protected abstract void saveData();
    
    // Hook method - can be overridden
    protected void validateData() {
        System.out.println("Default validation");
    }
}

// Concrete implementations
public class CsvDataProcessor extends DataProcessor {
    
    @Override
    protected void loadData() {
        System.out.println("Loading CSV data");
    }
    
    @Override
    protected void transformData() {
        System.out.println("Transforming CSV to objects");
    }
    
    @Override
    protected void saveData() {
        System.out.println("Saving to database");
    }
}

public class JsonDataProcessor extends DataProcessor {
    
    @Override
    protected void loadData() {
        System.out.println("Loading JSON data");
    }
    
    @Override
    protected void transformData() {
        System.out.println("Parsing JSON");
    }
    
    @Override
    protected void saveData() {
        System.out.println("Saving to NoSQL");
    }
}

// Usage
DataProcessor csvProcessor = new CsvDataProcessor();
csvProcessor.processData();

DataProcessor jsonProcessor = new JsonDataProcessor();
jsonProcessor.processData();
```

**Spring Implementation**:
```java
public abstract class DataProcessor {
    
    // Template method
    public final void processData() {
        loadData();
        validateData();
        transformData();
        saveData();
    }
    
    protected abstract void loadData();
    protected abstract void transformData();
    
    protected void validateData() {
        // Default validation
    }
    
    protected void saveData() {
        // Default save logic
    }
}

@Component
public class CsvDataProcessor extends DataProcessor {
    
    @Override
    protected void loadData() {
        // Load CSV data
    }
    
    @Override
    protected void transformData() {
        // Transform CSV data
    }
}
```

---

## Spring Framework Design Patterns

**Spring's Philosophy**: Spring framework is built around several core design patterns that promote loose coupling, testability, and maintainability. Understanding these patterns helps in writing better Spring applications.

### 1. Dependency Injection (IoC)
**Purpose**: Inverts control of object creation and dependency management.

**Theory**:
- **Problem**: Tight coupling when objects create their dependencies directly
- **Solution**: External entity provides dependencies to objects
- **Inversion of Control**: Framework controls object lifecycle instead of application code
- **Types**: Constructor injection (recommended), Setter injection, Field injection
- **Benefits**: Loose coupling, easier testing, better separation of concerns
- **Spring Magic**: Container manages entire object graph and dependency resolution

```java
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // Constructor injection (recommended)
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    public User createUser(UserDto userDto) {
        User user = new User(userDto.getName(), userDto.getEmail());
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser);
        return savedUser;
    }
}
```

### 2. Proxy Pattern (AOP)
**Purpose**: Provides placeholder/surrogate for another object to control access.

**Theory**:
- **Problem**: Need to add behavior (logging, security, caching) without modifying original classes
- **Solution**: Create proxy object that wraps original object and adds behavior
- **AOP Concepts**: Cross-cutting concerns, aspects, join points, pointcuts, advice
- **Types**: JDK Dynamic Proxy (interfaces), CGLIB Proxy (classes)
- **Spring AOP**: Runtime proxy creation, method interception, declarative programming

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("@annotation(Loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        Object result = joinPoint.proceed();
        
        long executionTime = System.currentTimeMillis() - start;
        System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
        
        return result;
    }
}

@Service
public class UserService {
    
    @Loggable
    public User findUserById(Long id) {
        return userRepository.findById(id);
    }
}
```

### 3. Front Controller Pattern
**Purpose**: Centralizes request handling logic.

**Theory**:
- **Problem**: Multiple entry points lead to duplicated request handling logic
- **Solution**: Single controller handles all requests and delegates to appropriate handlers
- **Benefits**: Centralized security, logging, routing; consistent request processing
- **Components**: Front Controller, Dispatcher, View, Helper
- **Spring MVC**: DispatcherServlet acts as front controller, delegates to @Controller classes

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserDto.from(user));
    }
    
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
    }
}
```

### 4. Repository Pattern
**Purpose**: Encapsulates data access logic.

**Theory**:
- **Problem**: Data access code scattered throughout application, tight coupling to data source
- **Solution**: Centralize data access logic in repository classes
- **Benefits**: Separation of concerns, easier testing, data source independence
- **Domain-Driven Design**: Repository represents collection of domain objects
- **Spring Data**: Automatic implementation generation, query derivation, custom queries

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);
}

@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }
}
```

---

## Microservices Design Patterns

**Microservices Challenges**: Distributed systems introduce complexity around communication, data consistency, fault tolerance, and service coordination. These patterns address common microservices challenges.

**Pattern Categories**:
- **Decomposition**: How to break monolith into services
- **Communication**: How services interact
- **Data Management**: How to handle distributed data
- **Reliability**: How to handle failures
- **Observability**: How to monitor distributed systems

### 1. API Gateway Pattern
**Purpose**: Single entry point for all client requests.

**Theory**:
- **Problem**: Clients need to call multiple microservices, each with different protocols/formats
- **Solution**: Single gateway that routes requests to appropriate services
- **Benefits**: Simplified client code, centralized cross-cutting concerns, protocol translation
- **Responsibilities**: Routing, authentication, rate limiting, request/response transformation
- **Challenges**: Single point of failure, performance bottleneck, complexity concentration

```java
@RestController
@RequestMapping("/api/gateway")
public class ApiGatewayController {
    
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;
    
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return userServiceClient.getUser(id);
    }
    
    @GetMapping("/user/{id}/orders")
    public ResponseEntity<List<OrderDto>> getUserOrders(@PathVariable Long id) {
        return orderServiceClient.getOrdersByUserId(id);
    }
}

// Feign Client
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {
    
    @GetMapping("/users/{id}")
    ResponseEntity<UserDto> getUser(@PathVariable Long id);
}
```

### 2. Circuit Breaker Pattern
**Purpose**: Prevents cascading failures in distributed systems.

**Theory**:
- **Problem**: Service failures can cascade through system, causing total system failure
- **Solution**: Monitor service calls and "trip" circuit when failure threshold reached
- **States**: Closed (normal), Open (failing), Half-Open (testing recovery)
- **Benefits**: Prevents cascade failures, faster failure detection, automatic recovery
- **Implementation**: Failure counting, timeout handling, fallback mechanisms
- **Spring Integration**: Resilience4j provides circuit breaker implementation

```java
@Service
public class OrderService {
    
    private final PaymentServiceClient paymentServiceClient;
    
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    @TimeLimiter(name = "payment-service")
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> paymentServiceClient.processPayment(request));
    }
    
    public CompletableFuture<PaymentResponse> fallbackPayment(PaymentRequest request, Exception ex) {
        return CompletableFuture.completedFuture(
            PaymentResponse.builder()
                .status("PENDING")
                .message("Payment will be processed later")
                .build()
        );
    }
}

// Configuration
resilience4j:
  circuitbreaker:
    instances:
      payment-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
```

### 3. Saga Pattern
**Purpose**: Manages distributed transactions across microservices.

**Theory**:
- **Problem**: ACID transactions don't work across multiple services/databases
- **Solution**: Sequence of local transactions with compensating actions
- **Types**: Choreography (event-driven), Orchestration (centralized coordinator)
- **Benefits**: Maintains data consistency, handles long-running transactions
- **Challenges**: Complex error handling, eventual consistency, compensating logic
- **When to Use**: Business processes spanning multiple services

```java
@Service
public class OrderSagaOrchestrator {
    
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    
    @SagaOrchestrationStart
    public void processOrder(OrderCreatedEvent event) {
        Order order = event.getOrder();
        
        try {
            // Step 1: Reserve inventory
            inventoryService.reserveItems(order.getItems());
            
            // Step 2: Process payment
            PaymentResult paymentResult = paymentService.processPayment(order.getPaymentInfo());
            
            if (paymentResult.isSuccessful()) {
                // Step 3: Arrange shipping
                shippingService.arrangeShipping(order);
                
                // Complete saga
                completeOrder(order);
            } else {
                // Compensate: Release inventory
                inventoryService.releaseItems(order.getItems());
                cancelOrder(order, "Payment failed");
            }
            
        } catch (Exception e) {
            // Compensate all completed steps
            compensateOrder(order);
        }
    }
    
    private void compensateOrder(Order order) {
        // Rollback in reverse order
        shippingService.cancelShipping(order.getId());
        paymentService.refundPayment(order.getPaymentInfo());
        inventoryService.releaseItems(order.getItems());
    }
}
```

### 4. Event Sourcing Pattern
**Purpose**: Stores all changes as sequence of events.

**Theory**:
- **Problem**: Traditional CRUD loses historical information and business intent
- **Solution**: Store events that led to current state instead of current state
- **Benefits**: Complete audit trail, temporal queries, event replay, debugging
- **Challenges**: Event schema evolution, snapshot optimization, query complexity
- **Components**: Event Store, Event Stream, Aggregate, Projection
- **Use Cases**: Financial systems, audit requirements, complex business domains

```java
@Entity
public class EventStore {
    @Id
    private String eventId;
    private String aggregateId;
    private String eventType;
    private String eventData;
    private LocalDateTime timestamp;
    private Long version;
}

@Service
public class AccountEventSourcingService {
    
    private final EventStoreRepository eventStoreRepository;
    
    public void processCommand(CreateAccountCommand command) {
        AccountCreatedEvent event = new AccountCreatedEvent(
            command.getAccountId(),
            command.getOwnerName(),
            command.getInitialBalance()
        );
        
        saveEvent(event);
    }
    
    public void processCommand(DebitAccountCommand command) {
        // Load current state from events
        Account account = loadAccountFromEvents(command.getAccountId());
        
        if (account.getBalance() >= command.getAmount()) {
            AccountDebitedEvent event = new AccountDebitedEvent(
                command.getAccountId(),
                command.getAmount(),
                account.getBalance() - command.getAmount()
            );
            
            saveEvent(event);
        } else {
            throw new InsufficientFundsException();
        }
    }
    
    private Account loadAccountFromEvents(String accountId) {
        List<EventStore> events = eventStoreRepository.findByAggregateIdOrderByVersion(accountId);
        
        Account account = new Account();
        for (EventStore eventStore : events) {
            account.apply(deserializeEvent(eventStore));
        }
        
        return account;
    }
}
```

### 5. CQRS (Command Query Responsibility Segregation)
**Purpose**: Separates read and write operations.

**Theory**:
- **Problem**: Same model used for reads and writes leads to complex, suboptimal design
- **Solution**: Separate models for commands (writes) and queries (reads)
- **Benefits**: Optimized read/write models, independent scaling, simplified queries
- **Challenges**: Eventual consistency, increased complexity, data synchronization
- **Often Combined**: With Event Sourcing for complete event-driven architecture
- **Use Cases**: High-read/write ratio differences, complex business logic

```java
// Command Side
@Service
public class UserCommandService {
    
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public void createUser(CreateUserCommand command) {
        User user = new User(command.getName(), command.getEmail());
        User savedUser = userRepository.save(user);
        
        eventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), savedUser.getName(), savedUser.getEmail()));
    }
    
    public void updateUser(UpdateUserCommand command) {
        User user = userRepository.findById(command.getId())
            .orElseThrow(() -> new UserNotFoundException());
        
        user.updateName(command.getName());
        userRepository.save(user);
        
        eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getName()));
    }
}

// Query Side
@Service
public class UserQueryService {
    
    private final UserReadModelRepository readModelRepository;
    
    public UserReadModel findById(Long id) {
        return readModelRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException());
    }
    
    public List<UserReadModel> findByNameContaining(String name) {
        return readModelRepository.findByNameContainingIgnoreCase(name);
    }
    
    @EventListener
    public void handle(UserCreatedEvent event) {
        UserReadModel readModel = new UserReadModel(
            event.getId(),
            event.getName(),
            event.getEmail()
        );
        readModelRepository.save(readModel);
    }
}
```

### 6. Database per Service Pattern
**Purpose**: Each microservice has its own database.

**Theory**:
- **Problem**: Shared database creates coupling between services
- **Solution**: Each service owns its data and database schema
- **Benefits**: Service independence, technology diversity, better scalability
- **Challenges**: Data consistency, complex queries across services, data duplication
- **Implementation**: Separate schemas, different database technologies, data synchronization
- **Trade-offs**: Consistency vs. availability (CAP theorem)

```java
// User Service Database Configuration
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.userservice.repository",
    entityManagerFactoryRef = "userEntityManagerFactory",
    transactionManagerRef = "userTransactionManager"
)
public class UserServiceDatabaseConfig {
    
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.user")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(userDataSource());
        em.setPackagesToScan("com.example.userservice.entity");
        return em;
    }
}

// Order Service Database Configuration
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.orderservice.repository",
    entityManagerFactoryRef = "orderEntityManagerFactory",
    transactionManagerRef = "orderTransactionManager"
)
public class OrderServiceDatabaseConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.order")
    public DataSource orderDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public LocalContainerEntityManagerFactoryBean orderEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(orderDataSource());
        em.setPackagesToScan("com.example.orderservice.entity");
        return em;
    }
}
```

---

## Spring Boot Microservices Patterns

**Spring Boot for Microservices**: Spring Boot simplifies microservices development by providing auto-configuration, embedded servers, and production-ready features. These patterns leverage Spring Boot's capabilities for building robust microservices.

### 1. Service Discovery Pattern
**Purpose**: Services automatically discover and communicate with each other.

**Theory**:
- **Problem**: Hard-coded service locations don't work in dynamic, cloud-native environments
- **Solution**: Services register themselves and discover others through registry
- **Types**: Client-side discovery, Server-side discovery, Service mesh
- **Benefits**: Dynamic scaling, load balancing, fault tolerance
- **Components**: Service Registry (Eureka), Service Registration, Service Discovery
- **Spring Cloud**: Netflix Eureka, Consul, Zookeeper integration

```java
// Eureka Server
@SpringBootApplication
@EnableEurekaServer
public class ServiceDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
    }
}

// Service Registration
@SpringBootApplication
@EnableEurekaClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

// Service Communication
@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable Long id);
}

@Service
public class OrderService {
    
    private final UserServiceClient userServiceClient;
    
    public Order createOrder(CreateOrderRequest request) {
        // Discover and call user service
        UserDto user = userServiceClient.getUser(request.getUserId());
        
        return Order.builder()
            .userId(user.getId())
            .userName(user.getName())
            .items(request.getItems())
            .build();
    }
}
```

### 2. Configuration Management Pattern
**Purpose**: Centralized configuration management.

**Theory**:
- **Problem**: Configuration scattered across services, hard to manage and update
- **Solution**: Centralized configuration server with environment-specific configs
- **Benefits**: Consistent configuration, environment promotion, runtime updates
- **12-Factor App**: Configuration stored in environment, separated from code
- **Spring Cloud Config**: Git-based configuration, encryption, refresh capabilities
- **Security**: Sensitive data encryption, access control, audit trails

```java
// Config Server
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

// Client Service
@RestController
@RefreshScope
public class UserController {
    
    @Value("${user.service.max-users:1000}")
    private int maxUsers;
    
    @Value("${user.service.welcome-message:Welcome}")
    private String welcomeMessage;
    
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
            "maxUsers", maxUsers,
            "welcomeMessage", welcomeMessage
        );
    }
}

// Dynamic Configuration Update
@Component
public class ConfigurationUpdateListener {
    
    @EventListener
    public void handleRefreshEvent(RefreshRemoteApplicationEvent event) {
        System.out.println("Configuration refreshed for: " + event.getDestinationService());
    }
}
```

### 3. Distributed Tracing Pattern
**Purpose**: Tracks requests across multiple services.

**Theory**:
- **Problem**: Debugging issues across multiple services is complex
- **Solution**: Trace requests through entire service call chain
- **Concepts**: Trace (end-to-end request), Span (single operation), Context propagation
- **Benefits**: Performance monitoring, bottleneck identification, error correlation
- **Implementation**: Correlation IDs, span creation, context propagation
- **Tools**: Zipkin, Jaeger, Spring Cloud Sleuth integration

```java
@RestController
public class UserController {
    
    private final UserService userService;
    private final Tracer tracer;
    
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        Span span = tracer.nextSpan().name("get-user").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("user.id", String.valueOf(id));
            
            UserDto user = userService.findById(id);
            
            span.tag("user.found", "true");
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}

// Async Processing with Tracing
@Service
public class NotificationService {
    
    @Async
    @NewSpan("send-notification")
    public CompletableFuture<Void> sendNotification(@SpanTag("user.id") Long userId, String message) {
        // Send notification logic
        return CompletableFuture.completedFuture(null);
    }
}
```

### 4. Health Check Pattern
**Purpose**: Monitors service health and availability.

**Theory**:
- **Problem**: Need to know if services are healthy and ready to handle requests
- **Solution**: Expose health endpoints that report service status
- **Types**: Liveness (is service running), Readiness (can service handle requests)
- **Components**: Health indicators, dependency checks, custom health logic
- **Benefits**: Automated recovery, load balancer integration, monitoring alerts
- **Spring Boot Actuator**: Built-in health endpoints, custom health indicators

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("connection-pool", getConnectionPoolInfo())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withException(e)
                .build();
        }
        
        return Health.down().withDetail("database", "Connection invalid").build();
    }
}

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    private final PaymentServiceClient paymentServiceClient;
    
    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = paymentServiceClient.healthCheck();
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("payment-service", "Available")
                    .withDetail("response-time", measureResponseTime())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("payment-service", "Unavailable")
                .withException(e)
                .build();
        }
        
        return Health.down().build();
    }
}
```

### 5. Bulkhead Pattern
**Purpose**: Isolates resources to prevent cascading failures.

**Theory**:
- **Problem**: Resource exhaustion in one area can affect entire system
- **Solution**: Isolate resources into separate pools (like ship bulkheads)
- **Types**: Thread pool isolation, connection pool isolation, service isolation
- **Benefits**: Fault isolation, resource protection, better resilience
- **Implementation**: Separate thread pools, circuit breakers, resource quotas
- **Hystrix Pattern**: Netflix's implementation of bulkhead and circuit breaker patterns

```java
@Configuration
public class ThreadPoolConfig {
    
    @Bean("userServiceExecutor")
    public Executor userServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("UserService-");
        executor.initialize();
        return executor;
    }
    
    @Bean("orderServiceExecutor")
    public Executor orderServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("OrderService-");
        executor.initialize();
        return executor;
    }
}

@Service
public class UserService {
    
    @Async("userServiceExecutor")
    public CompletableFuture<User> processUserAsync(Long userId) {
        // User processing logic
        return CompletableFuture.completedFuture(new User());
    }
}

@Service
public class OrderService {
    
    @Async("orderServiceExecutor")
    public CompletableFuture<Order> processOrderAsync(Long orderId) {
        // Order processing logic
        return CompletableFuture.completedFuture(new Order());
    }
}
```

---

## Best Practices

### 1. Dependency Injection Best Practices

```java
// ✅ Good: Constructor injection
@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
}

// ❌ Avoid: Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
}
```

### 2. Exception Handling Patterns

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .code("USER_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Invalid input")
            .details(ex.getErrors())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### 3. Caching Patterns

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "userProfiles", key = "#id")
    })
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

### 4. Security Patterns

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
}

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
@GetMapping("/users/{userId}")
public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
    // Method implementation
}
```

### 5. Testing Patterns

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private EmailService emailService;
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserRequest request = new CreateUserRequest("John Doe", "john@example.com");
        
        // When
        User user = userService.createUser(request);
        
        // Then
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        verify(emailService).sendWelcomeEmail(user);
    }
}

@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUserWhenFound() throws Exception {
        // Given
        User user = new User("John Doe", "john@example.com");
        when(userService.findById(1L)).thenReturn(user);
        
        // When & Then
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
}
```

---

## Design Pattern Decision Tree

### Questions to Ask When Choosing Patterns

1. **What problem am I solving?**
   - Object creation → Creational patterns
   - Object behavior → Behavioral patterns
   - Object structure → Structural patterns
   - Service communication → Microservices patterns

2. **What are my constraints?**
   - Performance requirements
   - Scalability needs
   - Team expertise
   - Technology stack

3. **What are the trade-offs?**
   - Complexity vs. flexibility
   - Performance vs. maintainability
   - Consistency vs. availability
   - Coupling vs. cohesion

### Pattern Maturity Model

#### Level 1: Basic Patterns
- Dependency Injection
- Repository Pattern
- Factory Pattern
- Observer Pattern

#### Level 2: Intermediate Patterns
- Strategy Pattern
- Template Method
- Proxy Pattern (AOP)
- Circuit Breaker

#### Level 3: Advanced Patterns
- CQRS
- Event Sourcing
- Saga Pattern
- Distributed Tracing

---

## Pattern Selection Guidelines

### When to Use Which Pattern

#### Creational Patterns
- **Singleton**: Shared resources (caches, connection pools, configuration)
- **Factory**: Multiple implementations, runtime type determination
- **Builder**: Complex objects with many optional parameters

#### Behavioral Patterns
- **Observer**: Event-driven architectures, loose coupling
- **Strategy**: Multiple algorithms, runtime algorithm selection
- **Template Method**: Common algorithm structure with variations

#### Microservices Patterns
- **API Gateway**: Multiple client types, cross-cutting concerns
- **Circuit Breaker**: External service dependencies, fault tolerance
- **Saga**: Multi-service transactions, eventual consistency
- **Event Sourcing**: Audit requirements, complex business domains
- **CQRS**: Different read/write patterns, performance optimization

### Anti-Patterns to Avoid

#### Common Mistakes
- **God Object**: Single class doing too much
- **Spaghetti Code**: Unstructured, hard-to-follow code
- **Golden Hammer**: Using same pattern for every problem
- **Premature Optimization**: Applying complex patterns too early

#### Microservices Anti-Patterns
- **Distributed Monolith**: Services too tightly coupled
- **Chatty Services**: Too many fine-grained service calls
- **Shared Database**: Multiple services accessing same database
- **Synchronous Communication**: Over-reliance on synchronous calls

## Pattern Relationships and Combinations

### Commonly Combined Patterns
- **CQRS + Event Sourcing**: Complete event-driven architecture
- **Circuit Breaker + Bulkhead**: Comprehensive fault tolerance
- **API Gateway + Service Discovery**: Dynamic service routing
- **Saga + Event Sourcing**: Distributed transaction with audit trail

### Pattern Evolution
- **Monolith → Microservices**: Decomposition patterns
- **Synchronous → Asynchronous**: Event-driven patterns
- **Shared Database → Database per Service**: Data isolation patterns
- **Manual Configuration → Automated**: Configuration management patterns

## Summary

This document covers the essential design patterns used in Java Spring frameworks:

**Fundamentals**: SOLID principles, pattern categories, design principles
**Core Java Patterns**: Singleton, Factory, Builder, Observer, Strategy, Template Method
**Spring Framework Patterns**: Dependency Injection, Proxy (AOP), Front Controller, Repository
**Microservices Patterns**: API Gateway, Circuit Breaker, Saga, Event Sourcing, CQRS, Database per Service
**Spring Boot Patterns**: Service Discovery, Configuration Management, Distributed Tracing, Health Check, Bulkhead

### Key Takeaways
1. **Understand the Problem**: Each pattern solves specific problems
2. **Know the Trade-offs**: Every pattern has benefits and drawbacks
3. **Context Matters**: Choose patterns based on your specific requirements
4. **Start Simple**: Don't over-engineer; add complexity when needed
5. **Combine Wisely**: Patterns work better when combined thoughtfully

These patterns provide proven solutions for building scalable, maintainable, and robust applications using Spring framework ecosystem. Understanding both the theory and practical implementation helps in making informed architectural decisions.

---

## Deep Interview Questions by Pattern

### Core Java Design Patterns

#### Singleton Pattern
**Basic Questions:**
- How do you implement thread-safe Singleton in Java?
  *Answer: Use synchronized method, double-checked locking with volatile, or enum implementation.*
- What are the problems with double-checked locking?
  *Answer: Without volatile keyword, it can create partially initialized objects due to instruction reordering.*

**Advanced Questions:**
- How does Spring manage Singleton scope differently from GoF Singleton?
  *Answer: Spring creates one instance per container/context, GoF ensures one instance per JVM/classloader.*
- What happens to Singleton pattern during serialization/deserialization?
  *Answer: Creates new instance during deserialization; override readResolve() method to return existing instance.*
- How would you test a class that uses Singleton dependency?
  *Answer: Use dependency injection to inject mock/stub instead of direct Singleton access.*
- Explain the difference between eager and lazy initialization in Singleton.
  *Answer: Eager creates instance at class loading; lazy creates on first access - trade-off between startup time and memory.*
- How does reflection break Singleton pattern and how to prevent it?
  *Answer: Reflection can access private constructor; prevent by throwing exception in constructor if instance exists.*

**Expert Questions:**
- How does Singleton pattern behave in multi-classloader environment?
  *Answer: Each classloader creates its own instance; use context classloader or shared parent classloader.*
- What are the memory implications of Singleton in long-running applications?
  *Answer: Can cause memory leaks if holding references to large objects; cannot be garbage collected.*
- How would you implement Singleton pattern using enum? Why is it considered best?
  *Answer: `enum Singleton { INSTANCE; }` - JVM guarantees thread safety and prevents reflection/serialization issues.*
- Explain how Spring's @Scope("singleton") handles circular dependencies.
  *Answer: Uses early exposure of bean references and proxy objects to resolve circular dependencies.*

#### Factory Pattern
**Basic Questions:**
- Difference between Factory Method and Abstract Factory?
  *Answer: Factory Method creates one product type; Abstract Factory creates families of related products.*
- When would you choose Factory over direct instantiation?
  *Answer: When object creation is complex, need runtime type determination, or want to decouple creation logic.*

**Advanced Questions:**
- How does Spring's @Configuration class implement Factory pattern?
  *Answer: @Bean methods act as factory methods, @Configuration class is the factory creating and managing beans.*
- Design a factory that can create objects based on runtime configuration.
  *Answer: Use strategy pattern with configuration-driven factory that maps config keys to implementation classes.*
- How would you handle factory method that might fail?
  *Answer: Return Optional<T>, throw specific exceptions, or use Result/Either pattern for error handling.*
- Explain how Spring Boot's auto-configuration uses Factory pattern.
  *Answer: @EnableAutoConfiguration uses factories to conditionally create beans based on classpath and properties.*

**Expert Questions:**
- How would you implement a factory that supports plugin architecture?
  *Answer: Use ServiceLoader mechanism or scan classpath for implementations, register in factory registry.*
- Design a factory pattern that can handle versioning of created objects.
  *Answer: Include version parameter in factory method, maintain version-to-implementation mapping.*
- How does Spring's FactoryBean interface work internally?
  *Answer: Spring calls getObject() method to get actual bean, getObjectType() for type information.*
- Implement a factory that uses reflection to create objects dynamically.
  *Answer: Use Class.forName() and Constructor.newInstance() with parameter mapping from configuration.*

#### Builder Pattern
**Basic Questions:**
- When should you use Builder over constructor?
  *Answer: When object has 4+ parameters, many optional parameters, or need immutable objects with validation.*
- How does Builder pattern ensure immutability?
  *Answer: Builder creates final object with all fields set; original object fields are final/private.*

**Advanced Questions:**
- How would you implement a Builder that validates object state?
  *Answer: Add validation in build() method or use step builder pattern with required fields first.*
- Design a Builder pattern that supports inheritance.
  *Answer: Use generic self-type: `abstract class Builder<T extends Builder<T>>` with self() method.*
- How does Spring Boot's configuration properties use Builder pattern?
  *Answer: @ConfigurationProperties classes often use builder pattern for complex nested configurations.*
- What are the performance implications of Builder pattern?
  *Answer: Slight overhead due to temporary builder object; negligible for most use cases.*

**Expert Questions:**
- How would you implement a Builder that can build different object types?
  *Answer: Use generic builder with type parameter and factory method for each target type.*
- Design a Builder pattern that supports fluent validation.
  *Answer: Chain validation methods that return builder, collect errors, validate on build().*
- How would you make Builder pattern work with generic types?
  *Answer: Use bounded wildcards and type tokens: `Builder<T extends SomeType>`.*
- Implement a Builder that can serialize/deserialize its state.
  *Answer: Make builder serializable, store intermediate state, restore builder from serialized data.*

#### Observer Pattern
**Basic Questions:**
- How does Observer pattern promote loose coupling?
  *Answer: Subject only knows observer interface, not concrete implementations; observers can be added/removed dynamically.*
- What's the difference between push and pull models?
  *Answer: Push sends data with notification; pull sends notification only, observer fetches data when needed.*

**Advanced Questions:**
- How does Spring's ApplicationEventPublisher implement Observer pattern?
  *Answer: ApplicationEventPublisher is subject, @EventListener methods are observers, Spring manages registration/notification.*
- How would you handle exceptions in observer notifications?
  *Answer: Catch exceptions per observer, use error handler, or publish error events to separate error observers.*
- Design an Observer pattern that supports priority-based notifications.
  *Answer: Use PriorityQueue or sorted list of observers, add priority field to observer interface.*
- How does @EventListener annotation work internally?
  *Answer: Spring scans for @EventListener methods, creates ApplicationListener proxies, registers with event multicaster.*

**Expert Questions:**
- How would you implement Observer pattern in distributed system?
  *Answer: Use message queues (Kafka/RabbitMQ) as event bus, services subscribe to topics/queues.*
- Design an Observer that can handle millions of observers efficiently.
  *Answer: Use concurrent data structures, batch notifications, async processing, or event streaming platforms.*
- How would you implement transactional event publishing in Spring?
  *Answer: Use @TransactionalEventListener with AFTER_COMMIT phase to publish events after transaction success.*
- What are the memory leak risks with Observer pattern and how to prevent them?
  *Answer: Observers hold references preventing GC; use weak references or explicit unsubscribe mechanisms.*

#### Strategy Pattern
**Basic Questions:**
- How does Strategy pattern eliminate conditional statements?
  *Answer: Replace if-else/switch with polymorphism; context delegates to strategy interface implementation.*
- When would you choose Strategy over inheritance?
  *Answer: When algorithms vary independently of clients, need runtime algorithm switching, or composition over inheritance.*

**Advanced Questions:**
- How does Spring's dependency injection support Strategy pattern?
  *Answer: Inject Map<String, StrategyInterface> or List<StrategyInterface>, select strategy by name or criteria.*
- Design a Strategy pattern that can be configured at runtime.
  *Answer: Use factory with configuration mapping, strategy registry, or external configuration to select strategy.*
- How would you implement Strategy pattern with caching?
  *Answer: Wrap strategies with caching decorator or use cache-aware strategy implementations.*
- Explain how Spring Security uses Strategy pattern for authentication.
  *Answer: AuthenticationProvider interface with multiple implementations (LDAP, DB, OAuth) selected by AuthenticationManager.*

**Expert Questions:**
- How would you implement Strategy pattern that supports composition of strategies?
  *Answer: Use composite strategy that chains multiple strategies or decorator pattern for layered strategies.*
- Design a Strategy pattern that can handle async operations.
  *Answer: Return CompletableFuture<T> from strategy methods, use async execution context.*
- How would you implement Strategy pattern with metrics and monitoring?
  *Answer: Use decorator pattern to wrap strategies with metrics collection, or AOP for cross-cutting concerns.*
- Create a Strategy pattern that supports A/B testing scenarios.
  *Answer: Use weighted strategy selector, feature flags, or traffic splitting to route to different strategy implementations.*

#### Template Method Pattern
**Basic Questions:**
- How does Template Method pattern promote code reuse?
  *Answer: Common algorithm structure in base class, subclasses override specific steps; eliminates duplicate code.*
- What's the difference between Template Method and Strategy?
  *Answer: Template Method uses inheritance to vary algorithm steps; Strategy uses composition to vary entire algorithm.*

**Advanced Questions:**
- How does Spring's JdbcTemplate implement Template Method pattern?
  *Answer: JdbcTemplate defines query execution steps, callbacks (RowMapper, PreparedStatementSetter) customize specific steps.*
- Design a Template Method that supports hooks and callbacks.
  *Answer: Add optional hook methods with empty default implementation, use callback interfaces for customization.*
- How would you handle exceptions in template methods?
  *Answer: Define exception handling strategy in template, allow subclasses to override exception handling steps.*
- Explain how Spring's TransactionTemplate works.
  *Answer: Template manages transaction lifecycle, TransactionCallback defines business logic executed within transaction.*

**Expert Questions:**
- How would you implement Template Method pattern with async operations?
  *Answer: Return CompletableFuture from template method, chain async operations, handle exceptions in async context.*
- Design a Template Method that supports pipeline processing.
  *Answer: Chain multiple template methods, use functional interfaces for pipeline stages, support parallel processing.*
- How would you implement Template Method with retry and circuit breaker?
  *Answer: Wrap template method execution with resilience patterns, make retry/circuit breaker configurable.*
- Create a Template Method that can be extended through configuration.
  *Answer: Use strategy pattern within template method, load strategies from configuration, support plugin architecture.*

### Spring Framework Design Patterns

#### Dependency Injection (IoC)
**Basic Questions:**
- Explain the three types of dependency injection.
  *Answer: Constructor (recommended), Setter (optional dependencies), Field (reflection-based, avoid in production).*
- Why is constructor injection preferred over field injection?
  *Answer: Ensures immutability, makes dependencies explicit, enables testing without Spring container.*

**Advanced Questions:**
- How does Spring resolve circular dependencies?
  *Answer: Uses early exposure of bean references through ObjectFactory, creates proxy for circular references.*
- Explain the difference between @Autowired and @Resource.
  *Answer: @Autowired is by-type injection (Spring), @Resource is by-name injection (JSR-250 standard).*
- How does Spring handle dependency injection in multi-threaded environment?
  *Answer: Bean creation is synchronized, singleton beans are thread-safe for injection, prototype beans created per request.*
- What happens when Spring cannot find a bean to inject?
  *Answer: Throws NoSuchBeanDefinitionException unless @Autowired(required=false) or Optional<T> is used.*

**Expert Questions:**
- How would you implement custom dependency injection container?
  *Answer: Use reflection to scan annotations, maintain bean registry, resolve dependencies recursively, handle lifecycle.*
- Explain Spring's bean lifecycle and all the interfaces involved.
  *Answer: Instantiation → populate properties → BeanNameAware → BeanFactoryAware → ApplicationContextAware → @PostConstruct → InitializingBean → custom init → ready.*
- How does Spring handle dependency injection with generics?
  *Answer: Uses ResolvableType to match generic types, supports injection of List<T>, Map<String,T> with type safety.*
- Design a system that can switch DI containers at runtime.
  *Answer: Use abstraction layer over DI containers, factory pattern to create containers, configuration-driven selection.*
- How does Spring's @Conditional annotation work with dependency injection?
  *Answer: Evaluates conditions during bean registration phase, skips bean creation if condition fails.*

#### Proxy Pattern (AOP)
**Basic Questions:**
- Difference between JDK Dynamic Proxy and CGLIB?
  *Answer: JDK proxy requires interface, creates proxy implementing interface; CGLIB extends target class, works without interface.*
- When does Spring use each type of proxy?
  *Answer: JDK proxy when target implements interface; CGLIB when target is class without interface or proxyTargetClass=true.*

**Advanced Questions:**
- How does Spring AOP handle method interception?
  *Answer: Creates proxy with MethodInterceptor chain, invokes advice before/after/around target method execution.*
- Explain the difference between compile-time and runtime weaving.
  *Answer: Compile-time weaving modifies bytecode during compilation; runtime weaving creates proxies at runtime.*
- How would you debug issues with AOP proxies?
  *Answer: Enable AOP debugging, check proxy type, verify pointcut expressions, use @EnableAspectJAutoProxy(exposeProxy=true).*
- What are the limitations of Spring AOP?
  *Answer: Only method-level interception, no self-invocation, runtime overhead, proxy-based limitations.*

**Expert Questions:**
- How would you implement custom aspect that modifies method parameters?
  *Answer: Use @Around advice with ProceedingJoinPoint, modify args array, call proceed(modifiedArgs).*
- Design an AOP solution for distributed tracing.
  *Answer: Create aspect that generates trace IDs, propagates context via ThreadLocal/MDC, integrates with tracing systems.*
- How does Spring handle AOP with inheritance and interfaces?
  *Answer: Proxy implements all interfaces, advice applies to interface methods, inheritance affects proxy type selection.*
- Implement a custom proxy factory that supports multiple advice types.
  *Answer: Use ProxyFactory, add multiple advisors, configure advice ordering, handle different advice types.*
- How would you handle AOP in reactive programming model?
  *Answer: Use reactive-specific aspects, handle Mono/Flux return types, propagate context through reactive streams.*

#### Repository Pattern
**Basic Questions:**
- How does Repository pattern abstract data access?
  *Answer: Encapsulates data access logic, provides domain-oriented interface, hides persistence implementation details.*
- What's the difference between DAO and Repository?
  *Answer: DAO is data-centric (CRUD operations), Repository is domain-centric (collection-like interface for aggregates).*

**Advanced Questions:**
- How does Spring Data generate repository implementations?
  *Answer: Uses proxy creation with MethodInterceptor, parses method names for queries, generates implementations at runtime.*
- Design a Repository pattern that supports multiple data sources.
  *Answer: Use @Qualifier for different repositories, routing data source, or abstract repository with data source selection.*
- How would you implement caching in Repository pattern?
  *Answer: Use @Cacheable annotations, cache abstraction, or decorator pattern with cache-aware repository wrapper.*
- Explain how Spring Data handles query derivation.
  *Answer: Parses method names using naming conventions (findBy, countBy), creates queries from method signatures.*

**Expert Questions:**
- How would you implement Repository pattern for event sourcing?
  *Answer: Store/retrieve event streams, implement aggregate reconstruction from events, support snapshots for performance.*
- Design a Repository that supports both SQL and NoSQL databases.
  *Answer: Use common interface with different implementations, polyglot persistence, or unified query abstraction.*
- How would you implement Repository pattern with distributed caching?
  *Answer: Use Redis/Hazelcast for distributed cache, implement cache-aside pattern, handle cache invalidation.*
- Create a Repository pattern that supports multi-tenancy.
  *Answer: Include tenant context in queries, use tenant-specific data sources, or schema-based tenant isolation.*

### Microservices Design Patterns

#### API Gateway Pattern
**Basic Questions:**
- What problems does API Gateway solve?
  *Answer: Single entry point, protocol translation, cross-cutting concerns (auth, logging), client simplification.*
- How does API Gateway handle routing?
  *Answer: URL path matching, header-based routing, service discovery integration, load balancing to backend services.*

**Advanced Questions:**
- How would you implement rate limiting in API Gateway?
  *Answer: Use token bucket/sliding window algorithms, Redis for distributed state, per-client/API key limits.*
- Design an API Gateway that supports versioning.
  *Answer: URL path versioning (/v1/, /v2/), header-based versioning, or query parameter versioning with routing rules.*
- How does API Gateway handle authentication and authorization?
  *Answer: JWT token validation, OAuth2 integration, RBAC policies, upstream service identity propagation.*
- What are the scalability challenges with API Gateway?
  *Answer: Single point of failure, bottleneck for all traffic, state management, connection pooling.*

**Expert Questions:**
- How would you implement API Gateway with zero-downtime deployments?
  *Answer: Blue-green deployment, rolling updates, health checks, graceful shutdown, connection draining.*
- Design an API Gateway that supports GraphQL federation.
  *Answer: Schema stitching, query planning across services, resolver delegation, type merging.*
- How would you handle API Gateway in multi-region deployment?
  *Answer: Regional gateways, global load balancer, cross-region failover, data locality considerations.*
- Implement an API Gateway that supports request/response transformation.
  *Answer: Middleware pipeline, transformation rules, JSON/XML conversion, header manipulation.*
- How would you implement circuit breaker pattern in API Gateway?
  *Answer: Per-service circuit breakers, failure threshold monitoring, fallback responses, health check integration.*

#### Circuit Breaker Pattern
**Basic Questions:**
- Explain the three states of Circuit Breaker.
  *Answer: Closed (normal), Open (failing, calls rejected), Half-Open (testing recovery with limited calls).*
- How does Circuit Breaker prevent cascading failures?
  *Answer: Fails fast when service is down, prevents resource exhaustion, provides fallback responses.*

**Advanced Questions:**
- How would you configure Circuit Breaker thresholds?
  *Answer: Failure rate (50%), minimum calls (10), timeout duration (30s), based on SLA and traffic patterns.*
- Design a Circuit Breaker that supports different failure types.
  *Answer: Separate counters for timeouts, exceptions, HTTP errors; weighted failure scoring.*
- How does Circuit Breaker pattern work with bulkhead pattern?
  *Answer: Circuit breaker per thread pool/resource, isolated failure domains, independent recovery.*
- What metrics should you monitor for Circuit Breaker?
  *Answer: Failure rate, response time, circuit state changes, fallback execution rate.*

**Expert Questions:**
- How would you implement Circuit Breaker for async operations?
  *Answer: CompletableFuture with timeout, async failure counting, non-blocking state transitions.*
- Design a Circuit Breaker that adapts thresholds based on traffic patterns.
  *Answer: Machine learning for threshold adjustment, traffic-based scaling, time-of-day patterns.*
- How would you implement Circuit Breaker in reactive streams?
  *Answer: Reactor/RxJava operators, backpressure handling, stream error recovery.*
- Create a Circuit Breaker that supports A/B testing for fallback strategies.
  *Answer: Multiple fallback strategies, traffic splitting, success rate comparison.*
- How would you implement distributed Circuit Breaker across multiple instances?
  *Answer: Shared state in Redis/Hazelcast, gossip protocol, eventual consistency for state sync.*

#### Saga Pattern
**Basic Questions:**
- Difference between Choreography and Orchestration Saga?
  *Answer: Choreography uses events (decentralized), Orchestration uses coordinator (centralized control).*
- How does Saga pattern handle distributed transactions?
  *Answer: Sequence of local transactions with compensating actions for rollback, eventual consistency.*

**Advanced Questions:**
- How would you implement compensation logic in Saga?
  *Answer: Reverse order execution, idempotent compensating actions, compensation state tracking.*
- Design a Saga pattern that handles partial failures.
  *Answer: Checkpoint mechanism, retry logic, dead letter queue for failed compensations.*
- How does Saga pattern ensure data consistency?
  *Answer: Eventual consistency through compensation, business-level consistency rules.*
- What are the challenges with long-running Sagas?
  *Answer: State management, timeout handling, partial failure recovery, resource locking.*

**Expert Questions:**
- How would you implement Saga pattern with event sourcing?
  *Answer: Store saga events, replay for state reconstruction, event-driven compensation triggers.*
- Design a Saga that supports nested transactions.
  *Answer: Hierarchical saga structure, parent-child compensation relationships, recursive rollback.*
- How would you handle Saga timeout and recovery?
  *Answer: Timeout per step, saga state persistence, recovery coordinator, manual intervention.*
- Implement a Saga pattern that supports parallel execution.
  *Answer: Fork-join pattern, parallel step execution, barrier synchronization, partial rollback.*
- How would you implement Saga pattern with different consistency levels?
  *Answer: Configurable consistency policies, eventual vs immediate consistency, business rule validation.*

#### Event Sourcing Pattern
**Basic Questions:**
- How does Event Sourcing differ from traditional CRUD?
  *Answer: Stores immutable events instead of current state, complete audit trail, state derived from events.*
- What are the benefits of storing events instead of state?
  *Answer: Complete history, audit trail, temporal queries, debugging, event replay capability.*

**Advanced Questions:**
- How would you handle event schema evolution?
  *Answer: Event versioning, upcasting old events, backward compatibility, schema registry.*
- Design an Event Sourcing system that supports snapshots.
  *Answer: Periodic state snapshots, snapshot + events since snapshot, configurable snapshot frequency.*
- How does Event Sourcing handle concurrent updates?
  *Answer: Optimistic concurrency control, event version numbers, conflict detection and resolution.*
- What are the query challenges with Event Sourcing?
  *Answer: Event replay for queries, CQRS for read models, eventual consistency, query performance.*

**Expert Questions:**
- How would you implement Event Sourcing with CQRS?
  *Answer: Events update write model, project to read models, separate command/query handlers.*
- Design an Event Sourcing system that supports time travel queries.
  *Answer: Query state at specific timestamp, event filtering by time, temporal indexing.*
- How would you handle Event Sourcing in distributed system?
  *Answer: Event partitioning, distributed event store, cross-aggregate consistency, saga pattern.*
- Implement Event Sourcing that supports event encryption.
  *Answer: Encrypt event payload, key management, field-level encryption, compliance requirements.*
- How would you implement Event Sourcing with different storage backends?
  *Answer: Abstract event store interface, SQL/NoSQL implementations, append-only optimization.*

#### CQRS Pattern
**Basic Questions:**
- Why separate read and write models?
  *Answer: Different optimization needs, complex queries vs simple commands, independent scaling.*
- How does CQRS improve performance?
  *Answer: Optimized read models, denormalized data, separate scaling, reduced contention.*

**Advanced Questions:**
- How would you handle eventual consistency in CQRS?
  *Answer: Event-driven updates, consistency boundaries, user feedback about processing state.*
- Design a CQRS system that supports multiple read models.
  *Answer: Event projections to different models, materialized views, specialized query databases.*
- How does CQRS pattern work with caching?
  *Answer: Cache read models, invalidation on events, distributed caching for read replicas.*
- What are the complexity trade-offs with CQRS?
  *Answer: Increased complexity vs performance gains, eventual consistency challenges, more infrastructure.*

**Expert Questions:**
- How would you implement CQRS with different consistency guarantees?
  *Answer: Strong consistency for commands, eventual for queries, configurable consistency levels.*
- Design a CQRS system that supports real-time analytics.
  *Answer: Stream processing for real-time projections, OLAP cubes, time-series databases.*
- How would you handle CQRS in multi-tenant environment?
  *Answer: Tenant-specific read models, shared event store with tenant isolation, data partitioning.*
- Implement CQRS that supports both batch and stream processing.
  *Answer: Lambda architecture, batch for historical data, stream for real-time updates.*
- How would you implement CQRS with polyglot persistence?
  *Answer: Different databases for different read models, event store as single source of truth.*

### Spring Boot Microservices Patterns

#### Service Discovery Pattern
**Basic Questions:**
- How does service discovery work in microservices?
  *Answer: Services register with registry, clients query registry for service locations, dynamic service location resolution.*
- Difference between client-side and server-side discovery?
  *Answer: Client-side: client queries registry directly; Server-side: load balancer queries registry, client calls load balancer.*

**Advanced Questions:**
- How would you implement health checks with service discovery?
  *Answer: Periodic health check endpoints, registry removes unhealthy instances, TTL-based registration renewal.*
- Design a service discovery that supports blue-green deployments.
  *Answer: Version-aware registry, traffic routing by version tags, gradual traffic shifting.*
- How does service discovery handle network partitions?
  *Answer: CAP theorem trade-offs, eventual consistency, cached service locations, fallback mechanisms.*
- What are the scalability limits of service discovery?
  *Answer: Registry becomes bottleneck, gossip protocols for distribution, hierarchical registries.*

**Expert Questions:**
- How would you implement service discovery across multiple data centers?
  *Answer: Federated registries, cross-DC replication, locality-aware routing, WAN-optimized protocols.*
- Design a service discovery that supports service mesh integration.
  *Answer: Sidecar proxy registration, control plane integration, xDS protocol support.*
- How would you implement service discovery with zero-downtime updates?
  *Answer: Rolling updates, health check delays, connection draining, graceful shutdown.*
- Create a service discovery that supports canary deployments.
  *Answer: Weighted routing, traffic splitting, gradual rollout, automated rollback on failures.*
- How would you implement service discovery with custom load balancing algorithms?
  *Answer: Pluggable load balancer interface, custom algorithms (latency-based, resource-aware), metrics integration.*

#### Distributed Tracing Pattern
**Basic Questions:**
- How does distributed tracing work across services?
  *Answer: Trace ID propagated across service calls, spans created for operations, parent-child relationships maintained.*
- What information is captured in a trace span?
  *Answer: Operation name, start/end time, tags/annotations, parent span ID, service information.*

**Advanced Questions:**
- How would you implement sampling in distributed tracing?
  *Answer: Probabilistic sampling, rate limiting, adaptive sampling based on traffic, head-based vs tail-based.*
- Design a tracing system that handles high-volume traffic.
  *Answer: Async span reporting, batching, compression, distributed storage, sampling strategies.*
- How does distributed tracing affect application performance?
  *Answer: CPU overhead for span creation, network overhead for reporting, memory for span storage.*
- What are the storage challenges with distributed tracing?
  *Answer: High volume data, time-series storage, retention policies, query performance optimization.*

**Expert Questions:**
- How would you implement distributed tracing for async operations?
  *Answer: Context propagation in async calls, CompletableFuture integration, reactive streams support.*
- Design a tracing system that supports custom metrics.
  *Answer: Custom tags/annotations, metric extraction from spans, integration with monitoring systems.*
- How would you implement distributed tracing with security considerations?
  *Answer: PII scrubbing, secure transport, access controls, audit logging.*
- Create a tracing system that supports real-time anomaly detection.
  *Answer: Stream processing of spans, ML-based anomaly detection, alerting integration.*
- How would you implement distributed tracing across different technology stacks?
  *Answer: Standard protocols (OpenTracing/OpenTelemetry), language-specific libraries, protocol translation.*

### Scenario-Based Questions

#### System Design Scenarios
1. **E-commerce Platform**: "Design a microservices architecture for an e-commerce platform. Which patterns would you use and why?"
   *Answer: API Gateway for client access, CQRS for product catalog, Saga for order processing, Event Sourcing for inventory, Circuit Breaker for external services.*

2. **Payment Processing**: "How would you design a payment processing system that guarantees exactly-once processing using design patterns?"
   *Answer: Idempotency pattern with unique request IDs, Saga pattern for distributed transactions, Event Sourcing for audit trail, Circuit Breaker for external payment providers.*

3. **Real-time Analytics**: "Design a system that processes millions of events per second. Which patterns would ensure scalability and fault tolerance?"
   *Answer: Event Sourcing for data ingestion, CQRS for read/write separation, Bulkhead for resource isolation, Circuit Breaker for downstream services.*

4. **Multi-tenant SaaS**: "How would you implement a multi-tenant SaaS application using microservices patterns?"
   *Answer: Database per Service with tenant isolation, API Gateway with tenant routing, Strategy pattern for tenant-specific features, Repository pattern with tenant context.*

5. **Legacy Migration**: "You need to migrate a monolithic application to microservices. What patterns would you use for gradual migration?"
   *Answer: Strangler Fig pattern for gradual replacement, API Gateway for routing, Database per Service for data migration, Event-driven architecture for decoupling.*

#### Troubleshooting Scenarios
1. **Memory Leaks**: "Your application has memory leaks related to Observer pattern. How would you identify and fix them?"
   *Answer: Use WeakReference for observers, implement explicit unsubscribe mechanism, check for circular references, use memory profilers to identify retained objects.*

2. **Circular Dependencies**: "Spring is throwing circular dependency errors. How would you resolve them using design patterns?"
   *Answer: Use @Lazy annotation, refactor to use events (Observer pattern), introduce interface/abstraction, use setter injection instead of constructor.*

3. **Performance Issues**: "Your microservices are experiencing high latency. Which patterns would you investigate and how?"
   *Answer: Check Circuit Breaker timeouts, implement Bulkhead pattern for resource isolation, add caching with Repository pattern, use async processing.*

4. **Data Consistency**: "You're seeing data inconsistency in your distributed system. How would you use Saga pattern to fix it?"
   *Answer: Implement compensating transactions, use event-driven saga orchestration, add idempotency checks, implement eventual consistency with proper ordering.*

5. **Cascading Failures**: "Your system is experiencing cascading failures. Which patterns would prevent this?"
   *Answer: Circuit Breaker to fail fast, Bulkhead for resource isolation, Timeout pattern, graceful degradation with fallback responses.*

### Code Review Questions

#### Pattern Implementation Review
```java
// Question: What's wrong with this Singleton implementation?
public class DatabaseConnection {
    private static DatabaseConnection instance;
    
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
}
```
*Answer: Not thread-safe, can create multiple instances in concurrent environment. Fix: add synchronized, use double-checked locking with volatile, or use enum.*

```java
// Question: How would you improve this Factory implementation?
@Component
public class PaymentFactory {
    public PaymentProcessor create(String type) {
        if ("stripe".equals(type)) {
            return new StripeProcessor();
        } else if ("paypal".equals(type)) {
            return new PayPalProcessor();
        }
        throw new IllegalArgumentException("Unknown type");
    }
}
```
*Answer: Violates Open/Closed principle, hard-coded dependencies. Fix: use Map<String, PaymentProcessor> with Spring injection, or Strategy pattern with registry.*

```java
// Question: What are the issues with this Observer implementation?
@Component
public class OrderService {
    private List<OrderObserver> observers = new ArrayList<>();
    
    public void addObserver(OrderObserver observer) {
        observers.add(observer);
    }
    
    public void createOrder(Order order) {
        // Create order logic
        for (OrderObserver observer : observers) {
            observer.onOrderCreated(order);
        }
    }
}
```
*Answer: Not thread-safe, no exception handling, memory leak risk, no unsubscribe. Fix: use ConcurrentHashMap, try-catch per observer, WeakReference, add removeObserver method.*