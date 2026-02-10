# Spring Dependency Injection Deep Dive

## Table of Contents
1. [What is Dependency Injection?](#what-is-dependency-injection)
2. [Inversion of Control (IoC)](#inversion-of-control-ioc)
3. [Types of Dependency Injection](#types-of-dependency-injection)
4. [Spring Bean Lifecycle](#spring-bean-lifecycle)
5. [Bean Scopes](#bean-scopes)
6. [@Autowired Deep Dive](#autowired-deep-dive)
7. [Qualifier and Primary](#qualifier-and-primary)
8. [Circular Dependencies](#circular-dependencies)
9. [Lazy Initialization](#lazy-initialization)
10. [Best Practices](#best-practices)

---

## What is Dependency Injection?

**Dependency Injection (DI)** is a design pattern where objects receive their dependencies from external sources rather than creating them internally.

### Without DI (Tight Coupling)

```java
public class OrderService {
    private PaymentService paymentService = new PaymentService(); // Tight coupling
    
    public void processOrder(Order order) {
        paymentService.processPayment(order);
    }
}
```

**Problems**:
- Hard to test (can't mock PaymentService)
- Hard to change implementation
- Violates Single Responsibility Principle

### With DI (Loose Coupling)

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public OrderService(PaymentService paymentService) { // DI via constructor
        this.paymentService = paymentService;
    }
    
    public void processOrder(Order order) {
        paymentService.processPayment(order);
    }
}
```

**Benefits**:
- Easy to test (inject mock)
- Easy to swap implementations
- Follows SOLID principles

---

## Inversion of Control (IoC)

**IoC** means the framework controls object creation and lifecycle, not the developer.

### Traditional Flow (You Control)

```java
public class Application {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();
        UserService userService = new UserService(userRepository);
        UserController userController = new UserController(userService);
        
        // You manage object creation and dependencies
    }
}
```

### Spring IoC (Framework Controls)

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        // Spring creates and manages all beans
    }
}

@RestController
public class UserController {
    private final UserService userService; // Spring injects this
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
}
```

**Spring IoC Container**:
- **BeanFactory**: Basic container
- **ApplicationContext**: Advanced container (most commonly used)

---

## Types of Dependency Injection

### 1. Constructor Injection (Recommended ✓)

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    // Spring automatically injects dependencies
    public OrderService(PaymentService paymentService, InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```

**Advantages**:
- Immutable (final fields)
- Mandatory dependencies (compile-time safety)
- Easy to test
- No reflection needed (since Spring 4.3)

**When to Use**: Always prefer this (Spring's recommendation)

---

### 2. Setter Injection

```java
@Service
public class OrderService {
    private PaymentService paymentService;
    
    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

**Advantages**:
- Optional dependencies
- Can change dependency at runtime

**Disadvantages**:
- Mutable (not thread-safe)
- Can't use final
- Dependencies might be null

**When to Use**: Optional dependencies only

---

### 3. Field Injection (Not Recommended ✗)

```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService; // Field injection
}
```

**Disadvantages**:
- Hard to test (need Spring context)
- Can't use final
- Hidden dependencies
- Violates encapsulation

**When to Use**: Never in production code (only for quick prototypes)

---

### 4. Method Injection

```java
@Service
public class OrderService {
    private PaymentService paymentService;
    private InventoryService inventoryService;
    
    @Autowired
    public void init(PaymentService paymentService, InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```

**When to Use**: Rarely needed, prefer constructor injection

---

## Spring Bean Lifecycle

### Complete Lifecycle

```
1. Instantiation (Constructor called)
2. Populate Properties (Dependency Injection)
3. setBeanName() - BeanNameAware
4. setBeanFactory() - BeanFactoryAware
5. setApplicationContext() - ApplicationContextAware
6. @PostConstruct or InitializingBean.afterPropertiesSet()
7. Custom init-method
8. Bean Ready for Use
9. @PreDestroy or DisposableBean.destroy()
10. Custom destroy-method
```

### Lifecycle Example

```java
@Component
public class UserService implements BeanNameAware, ApplicationContextAware, 
                                     InitializingBean, DisposableBean {
    
    private final UserRepository userRepository;
    private String beanName;
    private ApplicationContext applicationContext;
    
    // 1. Constructor (Instantiation)
    public UserService(UserRepository userRepository) {
        System.out.println("1. Constructor called");
        this.userRepository = userRepository;
    }
    
    // 3. BeanNameAware
    @Override
    public void setBeanName(String name) {
        System.out.println("3. setBeanName: " + name);
        this.beanName = name;
    }
    
    // 4. ApplicationContextAware
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        System.out.println("4. setApplicationContext");
        this.applicationContext = applicationContext;
    }
    
    // 5. @PostConstruct
    @PostConstruct
    public void postConstruct() {
        System.out.println("5. @PostConstruct");
    }
    
    // 6. InitializingBean
    @Override
    public void afterPropertiesSet() {
        System.out.println("6. afterPropertiesSet");
    }
    
    // 7. Custom init method
    public void customInit() {
        System.out.println("7. Custom init method");
    }
    
    // 9. @PreDestroy
    @PreDestroy
    public void preDestroy() {
        System.out.println("9. @PreDestroy");
    }
    
    // 10. DisposableBean
    @Override
    public void destroy() {
        System.out.println("10. destroy");
    }
}
```

**Configuration for Custom Methods**:

```java
@Configuration
public class AppConfig {
    
    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }
}
```

---

## Bean Scopes

### 1. Singleton (Default)

```java
@Service
@Scope("singleton") // Default, can be omitted
public class UserService {
    // One instance per Spring container
}
```

**Characteristics**:
- One instance per ApplicationContext
- Created at startup (eager initialization)
- Thread-safe if stateless

**Use Case**: Stateless services (99% of beans)

---

### 2. Prototype

```java
@Service
@Scope("prototype")
public class ReportGenerator {
    // New instance every time it's requested
}
```

**Characteristics**:
- New instance per request
- Not managed after creation (no destroy callbacks)

**Use Case**: Stateful objects, heavy objects

**Example**:

```java
@Service
public class ReportService {
    private final ApplicationContext context;
    
    public ReportService(ApplicationContext context) {
        this.context = context;
    }
    
    public void generateReport() {
        // Get new instance each time
        ReportGenerator generator = context.getBean(ReportGenerator.class);
        generator.generate();
    }
}
```

---

### 3. Request (Web Applications)

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContext {
    // One instance per HTTP request
    private String userId;
    private String sessionId;
}
```

**Use Case**: Store request-specific data

---

### 4. Session (Web Applications)

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {
    // One instance per HTTP session
    private List<Item> items = new ArrayList<>();
}
```

**Use Case**: Shopping cart, user preferences

---

### 5. Application

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AppConfig {
    // One instance per ServletContext
}
```

**Use Case**: Application-wide configuration

---

### 6. WebSocket

```java
@Component
@Scope(value = "websocket", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WebSocketSession {
    // One instance per WebSocket session
}
```

---

## @Autowired Deep Dive

### How @Autowired Works

```java
@Service
public class OrderService {
    
    @Autowired // Optional since Spring 4.3 for single constructor
    public OrderService(PaymentService paymentService) {
        // Spring searches for PaymentService bean and injects it
    }
}
```

**Resolution Process**:
1. **By Type**: Find bean of matching type
2. **By Qualifier**: If multiple beans, use @Qualifier
3. **By Name**: If no qualifier, match by parameter name
4. **Throw Exception**: If no match or ambiguous

---

### Required vs Optional Dependencies

```java
@Service
public class NotificationService {
    
    // Required dependency (default)
    @Autowired
    private EmailService emailService;
    
    // Optional dependency
    @Autowired(required = false)
    private SmsService smsService; // Can be null
    
    public void sendNotification(String message) {
        emailService.send(message);
        
        if (smsService != null) {
            smsService.send(message);
        }
    }
}
```

**Using Optional**:

```java
@Service
public class NotificationService {
    private final EmailService emailService;
    private final Optional<SmsService> smsService;
    
    public NotificationService(EmailService emailService, Optional<SmsService> smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
    
    public void sendNotification(String message) {
        emailService.send(message);
        smsService.ifPresent(sms -> sms.send(message));
    }
}
```

---

### Injecting Collections

```java
@Service
public class NotificationService {
    private final List<MessageSender> senders;
    
    // Injects ALL beans implementing MessageSender
    public NotificationService(List<MessageSender> senders) {
        this.senders = senders;
    }
    
    public void sendToAll(String message) {
        senders.forEach(sender -> sender.send(message));
    }
}

@Component
public class EmailSender implements MessageSender { }

@Component
public class SmsSender implements MessageSender { }

@Component
public class PushSender implements MessageSender { }
// All three will be injected into the list
```

---

### Injecting Map

```java
@Service
public class NotificationService {
    private final Map<String, MessageSender> senderMap;
    
    // Key = bean name, Value = bean instance
    public NotificationService(Map<String, MessageSender> senderMap) {
        this.senderMap = senderMap;
    }
    
    public void send(String type, String message) {
        MessageSender sender = senderMap.get(type + "Sender");
        if (sender != null) {
            sender.send(message);
        }
    }
}
```

---

## Qualifier and Primary

### Problem: Multiple Implementations

```java
public interface PaymentService {
    void processPayment(Order order);
}

@Service
public class StripePaymentService implements PaymentService { }

@Service
public class PayPalPaymentService implements PaymentService { }

@Service
public class OrderService {
    private final PaymentService paymentService;
    
    // ERROR: Which PaymentService to inject?
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

### Solution 1: @Qualifier

```java
@Service
@Qualifier("stripe")
public class StripePaymentService implements PaymentService { }

@Service
@Qualifier("paypal")
public class PayPalPaymentService implements PaymentService { }

@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public OrderService(@Qualifier("stripe") PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

### Solution 2: @Primary

```java
@Service
@Primary // Default implementation
public class StripePaymentService implements PaymentService { }

@Service
public class PayPalPaymentService implements PaymentService { }

@Service
public class OrderService {
    private final PaymentService paymentService;
    
    // Injects StripePaymentService (marked as @Primary)
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

### Solution 3: Custom Qualifier Annotation

```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface PaymentProvider {
    PaymentType value();
}

public enum PaymentType {
    STRIPE, PAYPAL, SQUARE
}

@Service
@PaymentProvider(PaymentType.STRIPE)
public class StripePaymentService implements PaymentService { }

@Service
@PaymentProvider(PaymentType.PAYPAL)
public class PayPalPaymentService implements PaymentService { }

@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public OrderService(@PaymentProvider(PaymentType.STRIPE) PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

## Circular Dependencies

### What is a Circular Dependency?

A **circular dependency** occurs when two or more beans depend on each other, creating a cycle in the dependency graph.

**Example**: A → B → A (ServiceA needs ServiceB, ServiceB needs ServiceA)

---

### Why is it a Problem?

**Spring Bean Creation Process**:
```
1. Create bean instance (call constructor)
2. Inject dependencies
3. Initialize bean (@PostConstruct)
4. Bean ready to use
```

**With Circular Dependency**:
```
1. Spring starts creating ServiceA
2. ServiceA constructor needs ServiceB
3. Spring starts creating ServiceB
4. ServiceB constructor needs ServiceA
5. ServiceA is not ready yet (still being created)
6. ERROR: BeanCurrentlyInCreationException
```

**The Chicken-Egg Problem**: Spring can't create A without B, and can't create B without A.

---

### Problem Example

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) { // Needs ServiceB
        this.serviceB = serviceB;
    }
    
    public void doA() {
        System.out.println("ServiceA doing work");
        serviceB.doB(); // Calls ServiceB
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) { // Needs ServiceA
        this.serviceA = serviceA;
    }
    
    public void doB() {
        System.out.println("ServiceB doing work");
        serviceA.doA(); // Calls ServiceA
    }
}

// ERROR: BeanCurrentlyInCreationException
// Error creating bean 'serviceA': Requested bean is currently in creation
```

**What Happens**:
```
Spring Container:
  1. Start creating ServiceA
  2. ServiceA constructor needs ServiceB
  3. Start creating ServiceB
  4. ServiceB constructor needs ServiceA
  5. ServiceA is marked as "currently in creation"
  6. DEADLOCK! Throw BeanCurrentlyInCreationException
```

---

### Solution 1: Setter Injection (Break Constructor Cycle)

**Theory**: Use setter injection for one of the dependencies to break the constructor cycle.

**How it Works**:
```
1. Create ServiceA instance (empty constructor)
2. Create ServiceB instance (inject ServiceA)
3. Call setServiceB() on ServiceA (inject ServiceB)
4. Both beans are now fully initialized
```

**Implementation**:

```java
@Service
public class ServiceA {
    private ServiceB serviceB; // Not final (mutable)
    
    // No constructor dependency on ServiceB
    public ServiceA() {
        System.out.println("ServiceA created");
    }
    
    @Autowired // Setter injection
    public void setServiceB(ServiceB serviceB) {
        System.out.println("ServiceB injected into ServiceA");
        this.serviceB = serviceB;
    }
    
    public void doA() {
        serviceB.doB();
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA; // Can still use constructor injection
    
    public ServiceB(ServiceA serviceA) {
        System.out.println("ServiceB created with ServiceA");
        this.serviceA = serviceA;
    }
    
    public void doB() {
        serviceA.doA();
    }
}
```

**Execution Flow**:
```
1. Spring creates ServiceA (no dependencies in constructor)
   Output: "ServiceA created"

2. Spring creates ServiceB (injects ServiceA)
   Output: "ServiceB created with ServiceA"

3. Spring calls setServiceB() on ServiceA
   Output: "ServiceB injected into ServiceA"

4. Both beans are ready
```

**Pros**:
- Simple to implement
- Works with constructor injection on one side

**Cons**:
- Can't use `final` for setter-injected dependency
- Mutable state (not thread-safe during initialization)
- Dependency might be null temporarily
- Violates immutability principle

**When to Use**: Quick fix for legacy code, but not recommended for new code.

---

### Solution 2: @Lazy (Proxy Pattern)

**Theory**: Inject a lazy proxy instead of the actual bean. The proxy delays bean creation until first method call.

**How it Works**:
```
1. Create ServiceA
2. Inject a PROXY of ServiceB (not the real ServiceB)
3. Create ServiceB
4. Inject real ServiceA
5. When ServiceA calls serviceB.doB(), proxy creates real ServiceB
```

**What is a Proxy?**
- A proxy is a placeholder object that looks like the real object
- It intercepts method calls and delegates to the real object
- Spring uses CGLIB or JDK Dynamic Proxy to create proxies

**Implementation**:

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    // @Lazy creates a proxy of ServiceB
    public ServiceA(@Lazy ServiceB serviceB) {
        System.out.println("ServiceA created with ServiceB proxy");
        this.serviceB = serviceB; // This is a PROXY, not real ServiceB
    }
    
    public void doA() {
        System.out.println("ServiceA.doA() called");
        serviceB.doB(); // Proxy intercepts and calls real ServiceB
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {
        System.out.println("ServiceB created with real ServiceA");
        this.serviceA = serviceA; // Real ServiceA
    }
    
    public void doB() {
        System.out.println("ServiceB.doB() called");
    }
}
```

**Execution Flow**:
```
1. Spring starts creating ServiceA
2. ServiceA needs ServiceB, but @Lazy is present
3. Spring creates a PROXY of ServiceB (lightweight placeholder)
4. ServiceA is created with ServiceB proxy
   Output: "ServiceA created with ServiceB proxy"

5. Spring creates real ServiceB
6. ServiceB needs ServiceA (no @Lazy)
7. ServiceA is already created, inject it
   Output: "ServiceB created with real ServiceA"

8. When serviceA.doA() is called:
   Output: "ServiceA.doA() called"
   
9. When serviceB.doB() is called via proxy:
   - Proxy intercepts the call
   - Proxy creates/fetches real ServiceB (if not already created)
   - Proxy delegates to real ServiceB.doB()
   Output: "ServiceB.doB() called"
```

**Visual Representation**:
```
Without @Lazy:
  ServiceA ──needs──> ServiceB
       ↑                 │
       └─────needs───────┘
  DEADLOCK!

With @Lazy:
  ServiceA ──needs──> ServiceB_Proxy ──lazy──> ServiceB (real)
       ↑                                            │
       └──────────────────needs─────────────────────┘
  NO DEADLOCK! Proxy breaks the cycle.
```

**Pros**:
- Can use `final` fields (immutable)
- Constructor injection on both sides
- Thread-safe
- Minimal code changes

**Cons**:
- Slight performance overhead (proxy method calls)
- Real bean created on first method call (lazy initialization)
- Debugging can be confusing (proxy vs real object)

**When to Use**: Recommended solution when you can't refactor the design.

---

### Solution 3: @Lazy on Class Level

**Theory**: Make the entire bean lazy, not just the dependency.

```java
@Service
@Lazy // Entire bean is lazy
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

**How it Works**:
```
1. Spring creates ServiceB first
2. ServiceB needs ServiceA, but ServiceA is @Lazy
3. Spring injects a proxy of ServiceA
4. ServiceB is created
5. ServiceA is created only when first used
```

**When to Use**: When you want to defer bean creation entirely.

---

### Solution 4: ObjectProvider (Lazy Lookup)

**Theory**: Instead of injecting the bean directly, inject a provider that can fetch the bean on demand.

```java
@Service
public class ServiceA {
    private final ObjectProvider<ServiceB> serviceBProvider;
    
    public ServiceA(ObjectProvider<ServiceB> serviceBProvider) {
        System.out.println("ServiceA created with ServiceB provider");
        this.serviceBProvider = serviceBProvider;
    }
    
    public void doA() {
        // Get ServiceB only when needed
        ServiceB serviceB = serviceBProvider.getObject();
        serviceB.doB();
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {
        System.out.println("ServiceB created with ServiceA");
        this.serviceA = serviceA;
    }
    
    public void doB() {
        System.out.println("ServiceB.doB() called");
    }
}
```

**How it Works**:
```
1. Spring creates ServiceA
2. Injects ObjectProvider<ServiceB> (not ServiceB itself)
3. ServiceA is created
4. Spring creates ServiceB
5. Injects real ServiceA
6. When doA() is called, provider fetches ServiceB
```

**Pros**:
- Can use `final` fields
- Explicit lazy lookup
- Can handle optional dependencies

**Cons**:
- More verbose
- Manual bean lookup

**When to Use**: When you need fine-grained control over bean lookup.

---

### Solution 5: Refactor Design (Best Practice)

**Theory**: Circular dependencies often indicate poor design. Refactor to eliminate the cycle.

**Pattern 1: Extract Common Dependency**

```java
// Before: A → B → A (circular)

// After: A → C, B → C (no cycle)

@Service
public class CommonService {
    public void commonLogic() {
        System.out.println("Common logic");
    }
}

@Service
public class ServiceA {
    private final CommonService commonService;
    
    public ServiceA(CommonService commonService) {
        this.commonService = commonService;
    }
    
    public void doA() {
        commonService.commonLogic();
    }
}

@Service
public class ServiceB {
    private final CommonService commonService;
    
    public ServiceB(CommonService commonService) {
        this.commonService = commonService;
    }
    
    public void doB() {
        commonService.commonLogic();
    }
}
```

**Pattern 2: Use Events (Decoupling)**

```java
// Before: A calls B directly, B calls A directly

// After: A publishes event, B listens to event

@Service
public class ServiceA {
    private final ApplicationEventPublisher eventPublisher;
    
    public ServiceA(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public void doA() {
        System.out.println("ServiceA doing work");
        // Publish event instead of calling ServiceB directly
        eventPublisher.publishEvent(new ServiceAEvent("data"));
    }
}

@Service
public class ServiceB {
    // No dependency on ServiceA
    
    @EventListener
    public void handleServiceAEvent(ServiceAEvent event) {
        System.out.println("ServiceB handling event: " + event.getData());
    }
    
    public void doB() {
        System.out.println("ServiceB doing work");
    }
}

public class ServiceAEvent {
    private final String data;
    
    public ServiceAEvent(String data) {
        this.data = data;
    }
    
    public String getData() {
        return data;
    }
}
```

**Pattern 3: Introduce Interface (Dependency Inversion)**

```java
// Before: ServiceA → ServiceB, ServiceB → ServiceA

// After: ServiceA → IServiceB, ServiceB → IServiceA

public interface IServiceA {
    void doA();
}

public interface IServiceB {
    void doB();
}

@Service
public class ServiceA implements IServiceA {
    private final IServiceB serviceB;
    
    public ServiceA(IServiceB serviceB) {
        this.serviceB = serviceB;
    }
    
    @Override
    public void doA() {
        serviceB.doB();
    }
}

@Service
public class ServiceB implements IServiceB {
    private final IServiceA serviceA;
    
    public ServiceB(@Lazy IServiceA serviceA) { // Still need @Lazy
        this.serviceA = serviceA;
    }
    
    @Override
    public void doB() {
        serviceA.doA();
    }
}
```

**Pattern 4: Use Mediator Pattern**

```java
// Mediator coordinates between ServiceA and ServiceB

@Service
public class ServiceMediator {
    private ServiceA serviceA;
    private ServiceB serviceB;
    
    @Autowired
    public void setServiceA(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
    
    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
    
    public void coordinateWork() {
        serviceA.doA();
        serviceB.doB();
    }
}

@Service
public class ServiceA {
    // No dependency on ServiceB
    public void doA() {
        System.out.println("ServiceA doing work");
    }
}

@Service
public class ServiceB {
    // No dependency on ServiceA
    public void doB() {
        System.out.println("ServiceB doing work");
    }
}
```

**Pros**:
- Clean design
- No circular dependencies
- Easy to test
- Follows SOLID principles

**Cons**:
- Requires refactoring
- May need architectural changes

**When to Use**: Always prefer this approach for new code.

---

### Comparison of Solutions

| Solution | Immutability | Complexity | Performance | Recommended |
|----------|--------------|------------|-------------|-------------|
| Setter Injection | ✗ | Low | Good | ✗ |
| @Lazy | ✓ | Low | Slight overhead | ✓ |
| ObjectProvider | ✓ | Medium | Good | ✓ |
| Refactor | ✓ | High | Best | ✓✓✓ |

---

### Real-World Example: Order and Payment Services

**Problem**:
```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    public void createOrder(Order order) {
        // Create order
        paymentService.processPayment(order); // Needs PaymentService
    }
}

@Service
public class PaymentService {
    private final OrderService orderService;
    
    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }
    
    public void processPayment(Order order) {
        // Process payment
        orderService.updateOrderStatus(order); // Needs OrderService
    }
}
// Circular dependency!
```

**Solution: Refactor with Events**:
```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;
    
    public OrderService(PaymentService paymentService, 
                       ApplicationEventPublisher eventPublisher) {
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
    }
    
    public void createOrder(Order order) {
        // Create order
        paymentService.processPayment(order);
    }
    
    @EventListener
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        // Update order status when payment is completed
        updateOrderStatus(event.getOrder());
    }
    
    private void updateOrderStatus(Order order) {
        order.setStatus("PAID");
    }
}

@Service
public class PaymentService {
    private final ApplicationEventPublisher eventPublisher;
    
    public PaymentService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public void processPayment(Order order) {
        // Process payment
        // ...
        
        // Publish event instead of calling OrderService directly
        eventPublisher.publishEvent(new PaymentCompletedEvent(order));
    }
}

public class PaymentCompletedEvent {
    private final Order order;
    
    public PaymentCompletedEvent(Order order) {
        this.order = order;
    }
    
    public Order getOrder() {
        return order;
    }
}
```

**Benefits**:
- No circular dependency
- Loose coupling
- Easy to add more listeners
- Follows event-driven architecture

---

### Key Takeaways

1. **Circular dependencies are a design smell** - they indicate tight coupling
2. **@Lazy is a quick fix** - use it when you can't refactor immediately
3. **Refactoring is the best solution** - extract common logic, use events, or introduce mediator
4. **Avoid setter injection** - it breaks immutability
5. **Use ObjectProvider** - when you need fine-grained control
6. **Think about your design** - circular dependencies often mean poor separation of concerns

---

## Lazy Initialization

### Eager (Default)

```java
@Service
public class HeavyService {
    public HeavyService() {
        // Created at application startup
        System.out.println("HeavyService created");
    }
}
```

---

### Lazy Initialization

```java
@Service
@Lazy // Created only when first used
public class HeavyService {
    public HeavyService() {
        System.out.println("HeavyService created");
    }
}

@Service
public class OrderService {
    private final HeavyService heavyService;
    
    public OrderService(@Lazy HeavyService heavyService) {
        this.heavyService = heavyService; // Proxy injected
    }
    
    public void process() {
        heavyService.doWork(); // Now HeavyService is created
    }
}
```

---

### Global Lazy Initialization

```yaml
# application.yml
spring:
  main:
    lazy-initialization: true # All beans are lazy
```

**Use Case**:
- Faster startup time
- Reduce memory usage
- Development/testing environments

**Trade-off**:
- Errors discovered at runtime, not startup
- First request slower

---

## Best Practices

### 1. Prefer Constructor Injection

```java
// ✓ Good
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}

// ✗ Bad
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
}
```

---

### 2. Use Final Fields

```java
@Service
public class OrderService {
    private final PaymentService paymentService; // Immutable
    
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

---

### 3. Avoid Field Injection

```java
// ✗ Bad - Hard to test
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
}

// ✓ Good - Easy to test
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}

// Test
@Test
void testOrderService() {
    PaymentService mockPayment = mock(PaymentService.class);
    OrderService orderService = new OrderService(mockPayment);
    // Easy to test!
}
```

---

### 4. Use @Primary for Default Implementation

```java
@Service
@Primary
public class DefaultPaymentService implements PaymentService { }

@Service
public class PremiumPaymentService implements PaymentService { }
```

---

### 5. Avoid Circular Dependencies

```java
// ✗ Bad - Circular dependency
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}

// ✓ Good - Refactor to remove circular dependency
@Service
public class CommonService { }

@Service
public class ServiceA {
    private final CommonService commonService;
    
    public ServiceA(CommonService commonService) {
        this.commonService = commonService;
    }
}

@Service
public class ServiceB {
    private final CommonService commonService;
    
    public ServiceB(CommonService commonService) {
        this.commonService = commonService;
    }
}
```

---

### 6. Keep Beans Stateless

```java
// ✓ Good - Stateless (thread-safe)
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    public void processOrder(Order order) {
        // No instance variables modified
    }
}

// ✗ Bad - Stateful (not thread-safe)
@Service
public class OrderService {
    private Order currentOrder; // Shared state!
    
    public void processOrder(Order order) {
        this.currentOrder = order; // Race condition!
    }
}
```

---

### 7. Use @Qualifier for Multiple Implementations

```java
@Service
public class PaymentProcessor {
    private final PaymentService stripeService;
    private final PaymentService paypalService;
    
    public PaymentProcessor(
        @Qualifier("stripe") PaymentService stripeService,
        @Qualifier("paypal") PaymentService paypalService
    ) {
        this.stripeService = stripeService;
        this.paypalService = paypalService;
    }
}
```

---

### 8. Use @Lazy for Heavy Beans

```java
@Service
@Lazy
public class ReportGenerator {
    public ReportGenerator() {
        // Heavy initialization
    }
}
```

---

## Advanced Topics

### 1. ObjectProvider (Lazy Lookup)

```java
@Service
public class OrderService {
    private final ObjectProvider<PaymentService> paymentServiceProvider;
    
    public OrderService(ObjectProvider<PaymentService> paymentServiceProvider) {
        this.paymentServiceProvider = paymentServiceProvider;
    }
    
    public void processOrder(Order order) {
        // Get bean only when needed
        PaymentService paymentService = paymentServiceProvider.getIfAvailable();
        if (paymentService != null) {
            paymentService.processPayment(order);
        }
    }
}
```

---

### 2. ApplicationContext Injection

```java
@Service
public class BeanFactory {
    private final ApplicationContext context;
    
    public BeanFactory(ApplicationContext context) {
        this.context = context;
    }
    
    public <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
    
    public Object getBean(String beanName) {
        return context.getBean(beanName);
    }
}
```

---

### 3. Conditional Bean Creation

```java
@Configuration
public class PaymentConfig {
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
    public PaymentService stripePaymentService() {
        return new StripePaymentService();
    }
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "paypal")
    public PaymentService paypalPaymentService() {
        return new PayPalPaymentService();
    }
}
```

---

### 4. Profile-Specific Beans

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new H2DataSource();
    }
    
    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        return new PostgresDataSource();
    }
}
```

---

## Summary Table

| Feature | Constructor | Setter | Field |
|---------|------------|--------|-------|
| Immutability | ✓ | ✗ | ✗ |
| Mandatory Dependencies | ✓ | ✗ | ✗ |
| Easy Testing | ✓ | ✓ | ✗ |
| Circular Dependencies | ✗ | ✓ | ✓ |
| Recommended | ✓ | Rarely | ✗ |

---

## Key Takeaways

1. **Always use Constructor Injection** with final fields
2. **Avoid Field Injection** (hard to test)
3. **Use @Primary** for default implementations
4. **Use @Qualifier** for multiple implementations
5. **Avoid Circular Dependencies** (refactor if needed)
6. **Keep Beans Stateless** (thread-safe)
7. **Use @Lazy** for heavy beans
8. **Prefer Singleton Scope** (default)
9. **Use @PostConstruct** for initialization logic
10. **Use @PreDestroy** for cleanup logic

---

## Interview Questions

### Q1: Why is Constructor Injection preferred over Field Injection?

**Answer**:
1. **Immutability**: Can use final fields
2. **Testability**: Easy to create instances in tests
3. **Mandatory Dependencies**: Compile-time safety
4. **No Reflection**: Better performance
5. **Clear Dependencies**: All dependencies visible in constructor

---

### Q2: What happens if there are multiple beans of the same type?

**Answer**:
Spring throws `NoUniqueBeanDefinitionException`. Solutions:
1. Use `@Primary` on one bean
2. Use `@Qualifier` to specify which bean
3. Inject `List<BeanType>` to get all beans
4. Use bean name matching

---

### Q3: How to resolve circular dependencies?

**Answer**:
1. **@Lazy**: Inject lazy proxy
2. **Setter Injection**: Break constructor cycle
3. **Refactor**: Extract common logic to third service (best practice)

---

### Q4: What is the difference between @Component, @Service, @Repository, @Controller?

**Answer**:
- **@Component**: Generic stereotype
- **@Service**: Business logic layer
- **@Repository**: Data access layer (adds exception translation)
- **@Controller**: Web layer (Spring MVC)

All are functionally equivalent, but provide semantic meaning.

---

### Q5: When to use Prototype scope?

**Answer**:
- Stateful beans
- Heavy objects that shouldn't be shared
- Beans with mutable state
- Beans that need different configuration per use

**Note**: Spring doesn't manage destroy lifecycle for prototype beans.

---

### Q6: What is the difference between @Autowired and @Inject?

**Answer**:
- **@Autowired**: Spring-specific, has `required` attribute
- **@Inject**: JSR-330 standard, no `required` attribute

Both work the same way in Spring. Use @Autowired for Spring projects.

---

### Q7: How does Spring resolve @Autowired dependencies?

**Answer**:
1. **By Type**: Find bean of matching type
2. **By Qualifier**: If multiple beans, use @Qualifier
3. **By Name**: If no qualifier, match by parameter name
4. **Throw Exception**: If no match or ambiguous

---

### Q8: What is the purpose of @PostConstruct and @PreDestroy?

**Answer**:
- **@PostConstruct**: Called after dependency injection, for initialization logic
- **@PreDestroy**: Called before bean destruction, for cleanup logic

**Example**:
```java
@Component
public class DatabaseConnection {
    
    @PostConstruct
    public void init() {
        // Open database connection
    }
    
    @PreDestroy
    public void cleanup() {
        // Close database connection
    }
}
```

---

### Q9: Can you inject a Prototype bean into a Singleton bean?

**Answer**:
Yes, but the Prototype bean will behave like a Singleton (same instance reused).

**Solution**: Use `@Lookup` or `ObjectProvider`:

```java
@Service
public class SingletonService {
    private final ObjectProvider<PrototypeBean> prototypeProvider;
    
    public SingletonService(ObjectProvider<PrototypeBean> prototypeProvider) {
        this.prototypeProvider = prototypeProvider;
    }
    
    public void doWork() {
        PrototypeBean bean = prototypeProvider.getObject(); // New instance
    }
}
```

---

### Q10: What is the difference between BeanFactory and ApplicationContext?

**Answer**:

| Feature | BeanFactory | ApplicationContext |
|---------|-------------|-------------------|
| Lazy Loading | Yes | No (eager by default) |
| Event Publishing | No | Yes |
| Internationalization | No | Yes |
| AOP Support | Manual | Automatic |
| Use Case | Lightweight | Full-featured (recommended) |

**ApplicationContext** extends **BeanFactory** with additional features.

---
