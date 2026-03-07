# Design Patterns — When to Use Which

Design patterns are reusable solutions to commonly occurring problems in software design. They are grouped into three categories: **Creational**, **Structural**, and **Behavioral**.

---

## Creational Patterns

These deal with object creation mechanisms.

---

### 1. Singleton
**Use when:** You need exactly one instance of a class shared across the application.

**Real-world triggers:**
- Database connection pool
- Logger
- Configuration manager
- Thread pool

**Java hint:** Use `enum` singleton or double-checked locking with `volatile` for thread safety.

```java
public class Config {
    private static volatile Config instance;
    private Config() {}
    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) instance = new Config();
            }
        }
        return instance;
    }
}
```

**Avoid when:** You need multiple instances or testability (singletons are hard to mock).

---

### 2. Factory Method
**Use when:** The exact type of object to create is determined at runtime, and you want subclasses to decide which class to instantiate.

**Real-world triggers:**
- Payment gateway (CreditCard, UPI, NetBanking)
- Notification service (Email, SMS, Push)
- Shape rendering (Circle, Square, Triangle)

```java
interface Notification { void send(); }
class EmailNotification implements Notification { public void send() { ... } }
class SMSNotification implements Notification { public void send() { ... } }

class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SMSNotification();
            default      -> throw new IllegalArgumentException("Unknown type");
        };
    }
}
```

**Avoid when:** The object creation logic is simple and unlikely to change.

---

### 3. Abstract Factory
**Use when:** You need to create families of related objects without specifying their concrete classes.

**Real-world triggers:**
- UI toolkit (Windows vs Mac buttons, checkboxes)
- Cloud provider abstraction (AWS vs Azure storage, compute)
- Database driver families (MySQL vs PostgreSQL)

**Difference from Factory Method:** Factory Method creates one product; Abstract Factory creates a family of related products.

**Avoid when:** You only have one product type — use Factory Method instead.

---

### 4. Builder
**Use when:** An object has many optional parameters or complex construction steps.

**Real-world triggers:**
- Building HTTP requests
- Constructing SQL queries
- Creating complex DTOs or domain objects
- `StringBuilder`, `Stream.Builder`, Lombok `@Builder`

```java
User user = new User.Builder("john@example.com")
    .name("John")
    .age(30)
    .role("ADMIN")
    .build();
```

**Avoid when:** The object has only 2–3 fields — a constructor is simpler.

---

### 5. Prototype
**Use when:** Creating a new object is expensive (DB call, network call) and you can clone an existing one.

**Real-world triggers:**
- Caching and cloning cached objects before modification
- Game objects (clone a template enemy/character)
- Document templates

```java
class Document implements Cloneable {
    public Document clone() throws CloneNotSupportedException {
        return (Document) super.clone();
    }
}
```

**Avoid when:** Object creation is cheap — cloning adds unnecessary complexity.

---

## Structural Patterns

These deal with object composition and relationships.

---

### 6. Adapter
**Use when:** You want to make two incompatible interfaces work together.

**Real-world triggers:**
- Integrating a third-party library with a different interface
- Legacy code integration
- Converting XML response to JSON expected by your system
- Java's `Arrays.asList()` adapts array to List

```java
interface ModernPayment { void pay(double amount); }
class LegacyPaymentSystem { void makePayment(int cents) { ... } }

class PaymentAdapter implements ModernPayment {
    private LegacyPaymentSystem legacy = new LegacyPaymentSystem();
    public void pay(double amount) { legacy.makePayment((int)(amount * 100)); }
}
```

**Avoid when:** You control both interfaces — just align them directly.

---

### 7. Decorator
**Use when:** You want to add behavior to an object dynamically without modifying its class or using inheritance.

**Real-world triggers:**
- Java I/O streams (`BufferedReader` wraps `FileReader`)
- Adding logging, caching, or auth to a service
- Spring's `@Transactional`, `@Cacheable` (AOP-based decoration)
- HTTP middleware/filter chains

```java
interface Coffee { double cost(); }
class SimpleCoffee implements Coffee { public double cost() { return 1.0; } }
class MilkDecorator implements Coffee {
    private Coffee coffee;
    MilkDecorator(Coffee c) { this.coffee = c; }
    public double cost() { return coffee.cost() + 0.5; }
}
```

**Avoid when:** You need to modify core behavior — subclassing or refactoring is cleaner.

---

### 8. Facade
**Use when:** You want to provide a simplified interface to a complex subsystem.

**Real-world triggers:**
- Service layer hiding JPA repositories, caches, and external APIs
- Spring Boot auto-configuration hiding complex setup
- `OrderService.placeOrder()` internally calling inventory, payment, notification services

```java
class OrderFacade {
    public void placeOrder(Order order) {
        inventoryService.reserve(order);
        paymentService.charge(order);
        notificationService.confirm(order);
    }
}
```

**Avoid when:** The client genuinely needs fine-grained control over subsystem components.

---

### 9. Proxy
**Use when:** You want to control access to an object — for lazy loading, caching, security, or logging.

**Real-world triggers:**
- Spring AOP proxies (`@Transactional`, `@Cacheable`)
- Hibernate lazy loading proxies
- Remote service proxies (RMI, gRPC stubs)
- Security access control

**Types:** Virtual Proxy (lazy init), Protection Proxy (access control), Remote Proxy (network call abstraction).

**Avoid when:** Direct access is fine and the overhead of proxying isn't justified.

---

### 10. Composite
**Use when:** You need to treat individual objects and groups of objects uniformly (tree structures).

**Real-world triggers:**
- File system (File and Folder both implement a common interface)
- UI component trees (Button, Panel, Window)
- Organization hierarchy (Employee, Manager)
- Menu items with sub-menus

```java
interface Component { void render(); }
class Leaf implements Component { public void render() { ... } }
class Composite implements Component {
    List<Component> children = new ArrayList<>();
    public void render() { children.forEach(Component::render); }
}
```

**Avoid when:** The hierarchy is flat — a simple list is sufficient.

---

### 11. Bridge
**Use when:** You want to separate abstraction from implementation so both can vary independently.

**Real-world triggers:**
- Notification system: abstraction = Notification type (Alert, Reminder), implementation = channel (Email, SMS)
- Shape + rendering engine (Shape abstraction, OpenGL/DirectX implementation)

**Difference from Adapter:** Bridge is designed upfront; Adapter is a retrofit for incompatible interfaces.

**Avoid when:** Abstraction and implementation don't need to vary independently.

---

### 12. Flyweight
**Use when:** You have a large number of similar objects consuming too much memory.

**Real-world triggers:**
- Character rendering in a text editor (share font/style objects)
- Game particles (bullets, trees)
- String interning in Java (`String.intern()`)
- Connection pool objects

**Avoid when:** Object count is small or objects have mostly unique state.

---

## Behavioral Patterns

These deal with communication and responsibility between objects.

---

### 13. Strategy
**Use when:** You have multiple algorithms for a task and want to switch between them at runtime.

**Real-world triggers:**
- Sorting strategies (QuickSort, MergeSort)
- Payment methods (Credit Card, UPI, Wallet)
- Discount calculation strategies
- Compression algorithms (ZIP, GZIP)
- `Comparator` in Java is a Strategy pattern

```java
interface SortStrategy { void sort(int[] arr); }
class QuickSort implements SortStrategy { ... }
class MergeSort implements SortStrategy { ... }

class Sorter {
    private SortStrategy strategy;
    Sorter(SortStrategy s) { this.strategy = s; }
    void sort(int[] arr) { strategy.sort(arr); }
}
```

**Avoid when:** There's only one algorithm — the abstraction adds unnecessary complexity.

---

### 14. Observer
**Use when:** One object's state change should automatically notify multiple dependent objects.

**Real-world triggers:**
- Event listeners (UI button clicks)
- Kafka/event-driven systems (publisher-subscriber)
- Spring `ApplicationEvent` / `@EventListener`
- Stock price updates notifying multiple dashboards
- `java.util.Observable` (deprecated), `PropertyChangeListener`

```java
interface Observer { void update(String event); }
class EventBus {
    List<Observer> observers = new ArrayList<>();
    void subscribe(Observer o) { observers.add(o); }
    void publish(String event) { observers.forEach(o -> o.update(event)); }
}
```

**Avoid when:** The coupling between publisher and subscriber needs to be tight and synchronous — use direct calls.

---

### 15. Command
**Use when:** You want to encapsulate a request as an object to support undo/redo, queuing, or logging.

**Real-world triggers:**
- Undo/redo in text editors
- Job queues and task schedulers
- Transaction rollback
- Remote control (each button = a command)
- `Runnable` in Java is a Command pattern

```java
interface Command { void execute(); void undo(); }
class DeleteCommand implements Command {
    public void execute() { /* delete */ }
    public void undo()    { /* restore */ }
}
```

**Avoid when:** You don't need undo/redo or queuing — a simple method call is cleaner.

---

### 16. Template Method
**Use when:** You have an algorithm skeleton where some steps are fixed and others are customizable by subclasses.

**Real-world triggers:**
- Spring's `JdbcTemplate`, `RestTemplate`, `KafkaTemplate`
- Data processing pipelines (read → transform → write)
- Report generation (header fixed, body varies)
- `AbstractList` in Java Collections

```java
abstract class DataProcessor {
    final void process() { readData(); transformData(); writeData(); } // template
    abstract void readData();
    abstract void transformData();
    void writeData() { /* default implementation */ }
}
```

**Avoid when:** The algorithm steps vary too much — Strategy is more flexible.

---

### 17. Chain of Responsibility
**Use when:** You want to pass a request along a chain of handlers until one handles it.

**Real-world triggers:**
- Spring Security filter chain
- Servlet filter chain
- Logging levels (DEBUG → INFO → WARN → ERROR)
- Approval workflows (Manager → Director → VP)
- Exception handling chains

```java
abstract class Handler {
    Handler next;
    Handler setNext(Handler h) { this.next = h; return h; }
    abstract void handle(Request r);
}
```

**Avoid when:** Every handler must process the request — use a simple loop instead.

---

### 18. State
**Use when:** An object's behavior changes based on its internal state, and you want to avoid large if-else/switch blocks.

**Real-world triggers:**
- Order lifecycle (PLACED → CONFIRMED → SHIPPED → DELIVERED → CANCELLED)
- Traffic light (RED → GREEN → YELLOW)
- Vending machine states
- TCP connection states

```java
interface OrderState { void next(OrderContext ctx); }
class PlacedState implements OrderState {
    public void next(OrderContext ctx) { ctx.setState(new ConfirmedState()); }
}
```

**Avoid when:** The object has only 2–3 states — a simple boolean or enum is sufficient.

---

### 19. Iterator
**Use when:** You want to traverse a collection without exposing its internal structure.

**Real-world triggers:**
- Java's `Iterator` and `Iterable` interfaces
- Custom data structures (Tree, Graph traversal)
- Paginated API results
- `for-each` loop in Java uses Iterator internally

**Avoid when:** The collection is a standard Java collection — built-in iterators are sufficient.

---

### 20. Mediator
**Use when:** Multiple objects communicate with each other in complex ways, creating tight coupling.

**Real-world triggers:**
- Chat room (users don't talk directly, they go through the room)
- Air traffic control (planes communicate via tower)
- UI form components (one field change affects others)
- Spring's `ApplicationEventPublisher` acts as a mediator

**Avoid when:** Only 2–3 objects interact — direct communication is simpler.

---

### 21. Memento
**Use when:** You need to capture and restore an object's state without violating encapsulation.

**Real-world triggers:**
- Undo/redo functionality
- Game save/load
- Transaction rollback
- Browser back button

**Avoid when:** State is simple or you can use Command pattern's undo instead.

---

### 22. Visitor
**Use when:** You want to add new operations to a class hierarchy without modifying the classes.

**Real-world triggers:**
- Compiler AST traversal (type checking, code generation)
- Tax calculation on different product types
- Serialization of different node types
- Report generation across different entity types

**Avoid when:** The class hierarchy changes frequently — adding a new class requires updating all visitors.

---

## Quick Decision Guide

| Situation | Pattern |
|---|---|
| Need one global instance | Singleton |
| Object creation varies at runtime | Factory Method |
| Families of related objects | Abstract Factory |
| Many optional constructor params | Builder |
| Clone expensive objects | Prototype |
| Incompatible interfaces | Adapter |
| Add behavior without subclassing | Decorator |
| Simplify complex subsystem | Facade |
| Control access to object | Proxy |
| Tree/hierarchical structures | Composite |
| Abstraction + implementation vary | Bridge |
| Millions of similar objects | Flyweight |
| Swap algorithms at runtime | Strategy |
| Notify multiple dependents | Observer |
| Encapsulate request, support undo | Command |
| Fixed algorithm, variable steps | Template Method |
| Pass request through handlers | Chain of Responsibility |
| Behavior changes with state | State |
| Traverse without exposing internals | Iterator |
| Reduce coupling between many objects | Mediator |
| Save/restore object state | Memento |
| Add operations without modifying classes | Visitor |

---

## Patterns Commonly Used in Spring Boot

| Spring Feature | Pattern Behind It |
|---|---|
| `@Bean`, `ApplicationContext` | Factory, Singleton |
| `@Autowired` / DI | Dependency Injection (Factory + Strategy) |
| `@Transactional`, `@Cacheable` | Proxy (AOP) |
| `JdbcTemplate`, `RestTemplate` | Template Method |
| `ApplicationEvent` / `@EventListener` | Observer |
| Filter chain (Security, Servlet) | Chain of Responsibility |
| `@Component` with multiple impls | Strategy |
| Lombok `@Builder` | Builder |

---

## Key Interview Takeaways

- **Creational** = How objects are created (Singleton, Factory, Builder, Prototype, Abstract Factory)
- **Structural** = How objects are composed (Adapter, Decorator, Facade, Proxy, Composite, Bridge, Flyweight)
- **Behavioral** = How objects communicate (Strategy, Observer, Command, Template Method, Chain of Responsibility, State, Iterator, Mediator, Memento, Visitor)
- Strategy vs Template Method: Strategy uses composition (inject algorithm), Template Method uses inheritance (override steps)
- Decorator vs Proxy: Decorator adds behavior, Proxy controls access
- Adapter vs Facade: Adapter converts interface, Facade simplifies interface
- Factory Method vs Abstract Factory: Factory creates one product, Abstract Factory creates a family
