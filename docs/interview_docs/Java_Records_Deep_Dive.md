# Java Records - Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Record Internals](#record-internals)
3. [Features & Capabilities](#features--capabilities)
4. [Limitations & Restrictions](#limitations--restrictions)
5. [Advanced Use Cases](#advanced-use-cases)
   - [Sealed Records - Complete Guide](#3-sealed-records-java-17---complete-guide)
6. [Records vs Classes](#records-vs-classes)
7. [Real-World Examples](#real-world-examples)
8. [Best Practices](#best-practices)

---

## Introduction

### What is a Record?

Introduced in **Java 14 (Preview)** and finalized in **Java 16**, a record is a special kind of class designed to be a transparent carrier for immutable data.

```java
// Traditional class (verbose)
public final class Person {
    private final String name;
    private final int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String name() { return name; }
    public int age() { return age; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person person = (Person) o;
        return age == person.age && Objects.equals(name, person.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
    
    @Override
    public String toString() {
        return "Person[name=" + name + ", age=" + age + "]";
    }
}

// Record (concise)
public record Person(String name, int age) {}
```

**Lines of code:** 30+ → 1 line!

---

## Record Internals

### What the Compiler Generates

When you declare a record, the Java compiler automatically generates:

```java
public record Person(String name, int age) {}

// Compiler generates:
public final class Person extends java.lang.Record {
    private final String name;
    private final int age;
    
    // Canonical constructor
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Accessor methods (not getters!)
    public String name() { return this.name; }
    public int age() { return this.age; }
    
    // equals()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person person = (Person) o;
        return age == person.age && Objects.equals(name, person.name);
    }
    
    // hashCode()
    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
    
    // toString()
    @Override
    public String toString() {
        return "Person[name=" + name + ", age=" + age + "]";
    }
}
```

### Key Characteristics

1. **Implicitly final**: Cannot be extended
2. **Extends java.lang.Record**: All records extend this abstract class
3. **Final fields**: All components are implicitly `private final`
4. **Immutable**: No setters, fields cannot be modified after construction
5. **Canonical constructor**: Automatically generated
6. **Accessor methods**: Named after the field (not `getName()`, but `name()`)

### Bytecode Analysis

```bash
# Compile record
javac Person.java

# Decompile to see generated code
javap -p Person.class
```

**Output:**
```
public final class Person extends java.lang.Record {
  private final java.lang.String name;
  private final int age;
  public Person(java.lang.String, int);
  public java.lang.String name();
  public int age();
  public final java.lang.String toString();
  public final int hashCode();
  public final boolean equals(java.lang.Object);
}
```

---

## Features & Capabilities

### 1. Basic Record Declaration

```java
public record Point(int x, int y) {}

// Usage
Point p = new Point(10, 20);
System.out.println(p.x());        // 10
System.out.println(p.y());        // 20
System.out.println(p);            // Point[x=10, y=20]
```

### 2. Compact Constructor

Validate or normalize data without repeating field assignments:

```java
public record Person(String name, int age) {
    // Compact constructor (no parameter list)
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        // Normalize name
        name = name.trim().toUpperCase();
        // Fields are automatically assigned after this block
    }
}

// Usage
Person p = new Person("  john  ", 25);
System.out.println(p.name());  // "JOHN"
```

### 3. Canonical Constructor (Explicit)

```java
public record Person(String name, int age) {
    // Explicit canonical constructor
    public Person(String name, int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        this.name = name.trim();
        this.age = age;
    }
}
```

### 4. Custom Constructors

```java
public record Person(String name, int age) {
    // Compact constructor for validation
    public Person {
        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");
    }
    
    // Custom constructor (must delegate to canonical)
    public Person(String name) {
        this(name, 0);  // Must call canonical constructor
    }
}

// Usage
Person p1 = new Person("John", 25);
Person p2 = new Person("Jane");  // age defaults to 0
```

### 5. Static Fields and Methods

```java
public record Person(String name, int age) {
    // Static fields allowed
    private static final int MAX_AGE = 150;
    
    // Static methods allowed
    public static Person of(String name, int age) {
        return new Person(name, age);
    }
    
    // Static factory with validation
    public static Person create(String name, int age) {
        if (age > MAX_AGE) {
            throw new IllegalArgumentException("Age too high");
        }
        return new Person(name, age);
    }
}

// Usage
Person p = Person.of("John", 25);
```

### 6. Instance Methods

```java
public record Person(String name, int age) {
    // Instance methods allowed
    public boolean isAdult() {
        return age >= 18;
    }
    
    public String getDisplayName() {
        return name + " (" + age + " years old)";
    }
}

// Usage
Person p = new Person("John", 25);
System.out.println(p.isAdult());         // true
System.out.println(p.getDisplayName());  // "John (25 years old)"
```

### 7. Implementing Interfaces

```java
public interface Identifiable {
    String getId();
}

public record User(String id, String name, int age) implements Identifiable {
    @Override
    public String getId() {
        return id;
    }
}

// Usage
User user = new User("U123", "John", 25);
System.out.println(user.getId());  // "U123"
```

### 8. Generic Records

```java
public record Pair<T, U>(T first, U second) {}

// Usage
Pair<String, Integer> pair = new Pair<>("Age", 25);
System.out.println(pair.first());   // "Age"
System.out.println(pair.second());  // 25

// With different types
Pair<Integer, String> reversed = new Pair<>(25, "Age");
```

### 9. Nested Records

```java
public record Address(String street, String city, String zipCode) {}

public record Person(String name, int age, Address address) {}

// Usage
Address addr = new Address("123 Main St", "New York", "10001");
Person person = new Person("John", 25, addr);
System.out.println(person.address().city());  // "New York"
```

### 10. Record with Collections

```java
public record Team(String name, List<String> members) {
    // Defensive copy to maintain immutability
    public Team {
        members = List.copyOf(members);  // Unmodifiable copy
    }
}

// Usage
List<String> memberList = new ArrayList<>(List.of("Alice", "Bob"));
Team team = new Team("Dev Team", memberList);

// Original list modification doesn't affect record
memberList.add("Charlie");
System.out.println(team.members());  // [Alice, Bob] (unchanged)
```

### 11. Annotations on Records

```java
import com.fasterxml.jackson.annotation.JsonProperty;

public record User(
    @JsonProperty("user_id") String id,
    @JsonProperty("user_name") String name,
    int age
) {}
```

### 12. Local Records (Java 16+)

```java
public class MathUtils {
    public static void main(String[] args) {
        // Local record inside method
        record Point(int x, int y) {}
        
        Point p1 = new Point(10, 20);
        Point p2 = new Point(30, 40);
        
        System.out.println(p1);  // Point[x=10, y=20]
    }
}
```

---

## Limitations & Restrictions

### What Records CANNOT Do

```java
// ❌ 1. Cannot extend other classes (already extends Record)
public record Person(String name) extends SomeClass {}  // COMPILE ERROR

// ❌ 2. Cannot declare instance fields (only static)
public record Person(String name) {
    private int age;  // COMPILE ERROR
}

// ❌ 3. Cannot be abstract
public abstract record Person(String name) {}  // COMPILE ERROR

// ❌ 4. Cannot have non-final instance fields
public record Person(String name) {
    private String nickname;  // COMPILE ERROR
}

// ❌ 5. Cannot have setters (immutable)
public record Person(String name) {
    public void setName(String name) {  // Allowed but defeats purpose
        // Cannot modify this.name (it's final)
    }
}

// ✅ What IS allowed
public record Person(String name) {
    // Static fields OK
    private static int count = 0;
    
    // Static methods OK
    public static int getCount() { return count; }
    
    // Instance methods OK
    public String getUpperName() { return name.toUpperCase(); }
    
    // Implementing interfaces OK
}
```

---

## Advanced Use Cases

### 1. Builder Pattern with Records

```java
public record Person(String name, int age, String email, String phone) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private int age;
        private String email;
        private String phone;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Person build() {
            return new Person(name, age, email, phone);
        }
    }
}

// Usage
Person person = Person.builder()
    .name("John")
    .age(25)
    .email("john@example.com")
    .phone("123-456-7890")
    .build();
```

### 2. Pattern Matching with Records (Java 16+)

```java
public record Point(int x, int y) {}
public record Circle(Point center, int radius) {}
public record Rectangle(Point topLeft, Point bottomRight) {}

public class ShapeProcessor {
    public static double calculateArea(Object shape) {
        return switch (shape) {
            case Circle(Point center, int radius) -> Math.PI * radius * radius;
            case Rectangle(Point(int x1, int y1), Point(int x2, int y2)) -> 
                Math.abs((x2 - x1) * (y2 - y1));
            default -> 0.0;
        };
    }
}

// Usage
Circle circle = new Circle(new Point(0, 0), 5);
System.out.println(calculateArea(circle));  // 78.54
```

### 3. Sealed Records (Java 17+) - Complete Guide

#### What are Sealed Records?

Sealed records combine two powerful Java features:
- **Sealed types** (Java 17): Restrict which classes can extend/implement a type
- **Records** (Java 16): Immutable data carriers with auto-generated boilerplate

This combination creates **closed type hierarchies** perfect for modeling **algebraic data types (ADTs)** and **domain models**.

#### Core Concepts

**1. Sealed Interfaces with Record Implementations**

```java
// Sealed interface restricts permitted implementations
public sealed interface Shape permits Circle, Rectangle, Triangle {}

public record Circle(Point center, int radius) implements Shape {}
public record Rectangle(Point topLeft, Point bottomRight) implements Shape {}
public record Triangle(Point p1, Point p2, Point p3) implements Shape {}

// Exhaustive pattern matching - compiler knows ALL possible types
public static String describe(Shape shape) {
    return switch (shape) {
        case Circle c -> "Circle with radius " + c.radius();
        case Rectangle r -> "Rectangle";
        case Triangle t -> "Triangle";
        // No default needed - compiler enforces exhaustiveness!
    };
}
```

**Why This Matters:**
- ✅ **Compile-time safety**: Adding a new shape forces you to update all switch statements
- ✅ **No default clause**: Compiler knows all cases are covered
- ✅ **Refactoring confidence**: Can't forget to handle new types

**2. Sealed Abstract Classes with Record Subclasses**

```java
public sealed abstract class Result<T> permits Success, Failure {
    // Common behavior for all results
    public abstract boolean isSuccess();
}

public final record Success<T>(T value) extends Result<T> {
    @Override
    public boolean isSuccess() { return true; }
}

public final record Failure<T>(String error, Throwable cause) extends Result<T> {
    @Override
    public boolean isSuccess() { return false; }
}

// Pattern matching with deconstruction
public <T> void handleResult(Result<T> result) {
    switch (result) {
        case Success<T>(var value) -> System.out.println("Success: " + value);
        case Failure<T>(var error, var cause) -> System.err.println("Error: " + error);
    }
}
```

#### Real-World Use Cases

**1. Payment Methods - E-commerce Domain**

```java
public sealed interface PaymentMethod permits CreditCard, DebitCard, UPI, Wallet, NetBanking {}

public record CreditCard(String number, String cvv, String expiry, String cardholderName) 
    implements PaymentMethod {
    public CreditCard {
        if (number == null || number.length() != 16) {
            throw new IllegalArgumentException("Invalid card number");
        }
    }
}

public record DebitCard(String number, String pin) implements PaymentMethod {}
public record UPI(String vpa) implements PaymentMethod {}
public record Wallet(String walletId, double balance) implements PaymentMethod {}
public record NetBanking(String bankCode, String accountNumber) implements PaymentMethod {}

// Payment processor with exhaustive handling
public class PaymentProcessor {
    public PaymentResult process(PaymentMethod payment, double amount) {
        return switch (payment) {
            case CreditCard cc -> processCreditCard(cc, amount * 1.02);  // 2% fee
            case DebitCard dc -> processDebitCard(dc, amount * 1.01);    // 1% fee
            case UPI upi -> processUPI(upi, amount);                     // No fee
            case Wallet w -> w.balance() >= amount 
                ? processWallet(w, amount) 
                : PaymentResult.failure("Insufficient balance");
            case NetBanking nb -> processNetBanking(nb, amount);
            // Compiler ensures all cases are handled!
        };
    }
}
```

**Benefits:**
- Adding a new payment method (e.g., `Cryptocurrency`) will cause compile errors in all switch statements
- No risk of forgetting to handle a payment type
- Type-safe payment processing

**2. API Response Types - Result Pattern**

```java
public sealed interface ApiResponse<T> permits Success, ClientError, ServerError, NetworkError {}

public record Success<T>(T data, int statusCode) implements ApiResponse<T> {}
public record ClientError<T>(String message, int statusCode) implements ApiResponse<T> {}
public record ServerError<T>(String message, int statusCode, String traceId) implements ApiResponse<T> {}
public record NetworkError<T>(String message, Throwable cause) implements ApiResponse<T> {}

// Exhaustive error handling
public <T> void handleResponse(ApiResponse<T> response) {
    switch (response) {
        case Success<T>(var data, var code) -> 
            System.out.println("Success: " + data);
        case ClientError<T>(var msg, var code) -> 
            System.err.println("Client error (" + code + "): " + msg);
        case ServerError<T>(var msg, var code, var traceId) -> 
            System.err.println("Server error (" + code + ") [" + traceId + "]: " + msg);
        case NetworkError<T>(var msg, var cause) -> 
            System.err.println("Network error: " + msg);
    }
}
```

**3. Event Sourcing - Domain Events**

```java
public sealed interface OrderEvent permits OrderCreated, OrderPaid, OrderShipped, OrderDelivered, OrderCancelled {
    String orderId();
    LocalDateTime timestamp();
}

public record OrderCreated(
    String orderId,
    String customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    LocalDateTime timestamp
) implements OrderEvent {}

public record OrderPaid(
    String orderId,
    String paymentId,
    PaymentMethod paymentMethod,
    LocalDateTime timestamp
) implements OrderEvent {}

public record OrderShipped(
    String orderId,
    String trackingNumber,
    String carrier,
    LocalDateTime timestamp
) implements OrderEvent {}

public record OrderDelivered(
    String orderId,
    String signature,
    LocalDateTime timestamp
) implements OrderEvent {}

public record OrderCancelled(
    String orderId,
    String reason,
    RefundStatus refundStatus,
    LocalDateTime timestamp
) implements OrderEvent {}

// Event handler with exhaustive pattern matching
public class OrderEventHandler {
    public void handle(OrderEvent event) {
        switch (event) {
            case OrderCreated e -> {
                createOrder(e.orderId(), e.customerId(), e.items());
                sendConfirmationEmail(e.customerId());
            }
            case OrderPaid e -> {
                updatePaymentStatus(e.orderId(), e.paymentId());
                initiateShipping(e.orderId());
            }
            case OrderShipped e -> {
                updateTrackingInfo(e.orderId(), e.trackingNumber());
                notifyCustomer(e.orderId(), e.trackingNumber());
            }
            case OrderDelivered e -> {
                markAsDelivered(e.orderId());
                requestReview(e.orderId());
            }
            case OrderCancelled e -> {
                cancelOrder(e.orderId());
                processRefund(e.orderId(), e.refundStatus());
            }
        }
    }
}
```

**4. State Machine - Order Status**

```java
public sealed interface OrderState permits Pending, Confirmed, Processing, Shipped, Delivered, Cancelled {}

public record Pending(LocalDateTime createdAt) implements OrderState {}
public record Confirmed(LocalDateTime confirmedAt, String paymentId) implements OrderState {}
public record Processing(LocalDateTime processingAt, String warehouseId) implements OrderState {}
public record Shipped(LocalDateTime shippedAt, String trackingNumber) implements OrderState {}
public record Delivered(LocalDateTime deliveredAt, String signature) implements OrderState {}
public record Cancelled(LocalDateTime cancelledAt, String reason) implements OrderState {}

// State transition validation
public class OrderStateMachine {
    public OrderState transition(OrderState current, OrderEvent event) {
        return switch (current) {
            case Pending p -> switch (event) {
                case OrderPaid e -> new Confirmed(e.timestamp(), e.paymentId());
                case OrderCancelled e -> new Cancelled(e.timestamp(), e.reason());
                default -> throw new IllegalStateException("Invalid transition from Pending");
            };
            case Confirmed c -> switch (event) {
                case OrderShipped e -> new Shipped(e.timestamp(), e.trackingNumber());
                case OrderCancelled e -> new Cancelled(e.timestamp(), e.reason());
                default -> throw new IllegalStateException("Invalid transition from Confirmed");
            };
            case Shipped s -> switch (event) {
                case OrderDelivered e -> new Delivered(e.timestamp(), e.signature());
                default -> throw new IllegalStateException("Invalid transition from Shipped");
            };
            case Delivered d -> throw new IllegalStateException("Order already delivered");
            case Cancelled c -> throw new IllegalStateException("Order already cancelled");
            case Processing pr -> switch (event) {
                case OrderShipped e -> new Shipped(e.timestamp(), e.trackingNumber());
                default -> throw new IllegalStateException("Invalid transition from Processing");
            };
        };
    }
}
```

**5. Notification Channels**

```java
public sealed interface NotificationChannel permits Email, SMS, Push, InApp, Webhook {}

public record Email(String to, String subject, String body) implements NotificationChannel {}
public record SMS(String phoneNumber, String message) implements NotificationChannel {}
public record Push(String deviceToken, String title, String body, Map<String, String> data) 
    implements NotificationChannel {}
public record InApp(String userId, String message, NotificationType type) 
    implements NotificationChannel {}
public record Webhook(String url, String payload, Map<String, String> headers) 
    implements NotificationChannel {}

// Multi-channel notification sender
public class NotificationService {
    public void send(NotificationChannel channel) {
        switch (channel) {
            case Email(var to, var subject, var body) -> 
                emailService.send(to, subject, body);
            case SMS(var phone, var msg) -> 
                smsService.send(phone, msg);
            case Push(var token, var title, var body, var data) -> 
                pushService.send(token, title, body, data);
            case InApp(var userId, var msg, var type) -> 
                inAppService.send(userId, msg, type);
            case Webhook(var url, var payload, var headers) -> 
                webhookService.send(url, payload, headers);
        }
    }
}
```

#### Advanced Patterns

**1. Nested Sealed Hierarchies**

```java
// Top-level sealed interface
public sealed interface Expression permits Value, BinaryOp, UnaryOp {}

// Leaf records
public record Value(int value) implements Expression {}

// Nested sealed interface
public sealed interface BinaryOp extends Expression permits Add, Subtract, Multiply, Divide {}
public record Add(Expression left, Expression right) implements BinaryOp {}
public record Subtract(Expression left, Expression right) implements BinaryOp {}
public record Multiply(Expression left, Expression right) implements BinaryOp {}
public record Divide(Expression left, Expression right) implements BinaryOp {}

public sealed interface UnaryOp extends Expression permits Negate, Absolute {}
public record Negate(Expression expr) implements UnaryOp {}
public record Absolute(Expression expr) implements UnaryOp {}

// Recursive evaluation with exhaustive matching
public class ExpressionEvaluator {
    public int evaluate(Expression expr) {
        return switch (expr) {
            case Value(var v) -> v;
            case Add(var left, var right) -> evaluate(left) + evaluate(right);
            case Subtract(var left, var right) -> evaluate(left) - evaluate(right);
            case Multiply(var left, var right) -> evaluate(left) * evaluate(right);
            case Divide(var left, var right) -> evaluate(left) / evaluate(right);
            case Negate(var e) -> -evaluate(e);
            case Absolute(var e) -> Math.abs(evaluate(e));
        };
    }
}
```

**2. Generic Sealed Records**

```java
public sealed interface Option<T> permits Some, None {
    static <T> Option<T> of(T value) {
        return value != null ? new Some<>(value) : new None<>();
    }
}

public record Some<T>(T value) implements Option<T> {}
public final class None<T> implements Option<T> {
    private static final None<?> INSTANCE = new None<>();
    
    @SuppressWarnings("unchecked")
    public static <T> None<T> instance() {
        return (None<T>) INSTANCE;
    }
}

// Usage with pattern matching
public <T> T getOrDefault(Option<T> option, T defaultValue) {
    return switch (option) {
        case Some<T>(var value) -> value;
        case None<T> n -> defaultValue;
    };
}
```

#### Sealed Records vs Enums

**When to Use Sealed Records:**
```java
// ✅ Each variant has different data
public sealed interface PaymentMethod permits CreditCard, UPI, Wallet {}
public record CreditCard(String number, String cvv) implements PaymentMethod {}
public record UPI(String vpa) implements PaymentMethod {}
public record Wallet(String id, double balance) implements PaymentMethod {}
```

**When to Use Enums:**
```java
// ✅ Simple constants without data
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
```

#### Compiler Guarantees

**1. Exhaustiveness Checking**
```java
public sealed interface Status permits Active, Inactive, Suspended {}
public record Active() implements Status {}
public record Inactive() implements Status {}
public record Suspended() implements Status {}

// ✅ Compiles - all cases covered
public String describe(Status status) {
    return switch (status) {
        case Active a -> "Active";
        case Inactive i -> "Inactive";
        case Suspended s -> "Suspended";
    };
}

// ❌ Compile error - missing Suspended case
public String describe(Status status) {
    return switch (status) {
        case Active a -> "Active";
        case Inactive i -> "Inactive";
        // ERROR: the switch expression does not cover all possible input values
    };
}
```

**2. Refactoring Safety**
```java
// Add new status
public record Banned() implements Status {}  // Add to permits clause

// Compiler will flag ALL switch statements that don't handle Banned!
// This prevents bugs from forgetting to update code
```

#### Best Practices

**1. Keep Hierarchies Shallow**
```java
// ✅ Good - flat hierarchy
public sealed interface Shape permits Circle, Rectangle, Triangle {}

// ❌ Avoid - deep nesting makes pattern matching complex
public sealed interface Shape permits TwoDShape, ThreeDShape {}
public sealed interface TwoDShape extends Shape permits Circle, Rectangle {}
public sealed interface ThreeDShape extends Shape permits Sphere, Cube {}
```

**2. Use Meaningful Names**
```java
// ✅ Good - clear intent
public sealed interface PaymentResult permits PaymentSuccess, PaymentFailure {}

// ❌ Bad - generic names
public sealed interface Result permits Success, Failure {}
```

**3. Combine with Validation**
```java
public record CreditCard(String number, String cvv, String expiry) implements PaymentMethod {
    public CreditCard {
        if (number == null || number.length() != 16) {
            throw new IllegalArgumentException("Invalid card number");
        }
        if (cvv == null || cvv.length() != 3) {
            throw new IllegalArgumentException("Invalid CVV");
        }
    }
}
```

#### Summary: Why Sealed Records?

| Feature | Benefit |
|---------|--------|
| **Closed Hierarchy** | Compiler knows all possible types |
| **Exhaustive Matching** | No default clause needed |
| **Refactoring Safety** | Adding types breaks compilation |
| **Type Safety** | Impossible to add unauthorized types |
| **Pattern Matching** | Clean, readable switch expressions |
| **Domain Modeling** | Perfect for ADTs and state machines |

**Key Takeaway:** Sealed records are ideal for modeling **fixed sets of alternatives** where you want **compile-time guarantees** that all cases are handled. They're the Java equivalent of **sum types** in functional programming languages! 🚀

### 4. Record Serialization

```java
import java.io.*;

public record Person(String name, int age) implements Serializable {}

// Serialization
Person person = new Person("John", 25);
try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("person.ser"))) {
    oos.writeObject(person);
}

// Deserialization
try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("person.ser"))) {
    Person loaded = (Person) ois.readObject();
    System.out.println(loaded);  // Person[name=John, age=25]
}
```

### 5. Records with Validation

```java
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}

public record User(String name, Email email, int age) {
    public User {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Invalid age: " + age);
        }
    }
}

// Usage
Email email = new Email("john@example.com");
User user = new User("John", email, 25);
```

---

## Records vs Classes

### Comparison Table

| Feature | Record | Class |
|---------|--------|-------|
| **Mutability** | Immutable | Mutable/Immutable |
| **Inheritance** | Cannot extend classes | Can extend classes |
| **Fields** | All final | Can be mutable |
| **Boilerplate** | Auto-generated | Manual |
| **equals/hashCode** | Auto-generated | Manual or @EqualsAndHashCode |
| **toString** | Auto-generated | Manual or @ToString |
| **Use Case** | Data carriers | Complex behavior |
| **Extensibility** | Limited | Full |

### When to Use Records

✅ **Use Records for:**
- DTOs (Data Transfer Objects)
- API responses/requests
- Configuration objects
- Value objects
- Immutable data structures
- Database query results
- Event objects

❌ **Don't Use Records for:**
- Entities with complex behavior
- Objects requiring inheritance
- Mutable state
- Objects with many optional fields
- Legacy code compatibility

---

## Real-World Examples

### Example 1: REST API DTOs

```java
// Request DTO
public record CreateUserRequest(
    String username,
    String email,
    String password
) {
    public CreateUserRequest {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }
}

// Response DTO
public record UserResponse(
    Long id,
    String username,
    String email,
    LocalDateTime createdAt
) {}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.create(request);
        UserResponse response = new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Example 2: Database Query Results

```java
public record UserStats(
    Long userId,
    String username,
    long totalOrders,
    BigDecimal totalSpent,
    LocalDateTime lastOrderDate
) {}

// Repository
@Repository
public class UserStatsRepository {
    
    @Query("""
        SELECT new com.example.UserStats(
            u.id,
            u.username,
            COUNT(o.id),
            SUM(o.amount),
            MAX(o.createdAt)
        )
        FROM User u
        LEFT JOIN Order o ON u.id = o.userId
        GROUP BY u.id, u.username
        """)
    List<UserStats> getUserStats();
}
```

### Example 3: Event Sourcing

```java
public sealed interface DomainEvent permits OrderCreated, OrderShipped, OrderCancelled {
    String orderId();
    LocalDateTime timestamp();
}

public record OrderCreated(
    String orderId,
    String customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    LocalDateTime timestamp
) implements DomainEvent {}

public record OrderShipped(
    String orderId,
    String trackingNumber,
    LocalDateTime timestamp
) implements DomainEvent {}

public record OrderCancelled(
    String orderId,
    String reason,
    LocalDateTime timestamp
) implements DomainEvent {}

// Event Handler
public class OrderEventHandler {
    public void handle(DomainEvent event) {
        switch (event) {
            case OrderCreated e -> handleOrderCreated(e);
            case OrderShipped e -> handleOrderShipped(e);
            case OrderCancelled e -> handleOrderCancelled(e);
        }
    }
}
```

### Example 4: Configuration Objects

```java
public record DatabaseConfig(
    String host,
    int port,
    String database,
    String username,
    String password,
    int maxConnections,
    Duration connectionTimeout
) {
    public DatabaseConfig {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        if (maxConnections < 1) {
            throw new IllegalArgumentException("Max connections must be positive");
        }
    }
    
    // Factory method with defaults
    public static DatabaseConfig defaults(String host, String database) {
        return new DatabaseConfig(
            host,
            5432,
            database,
            "postgres",
            "password",
            10,
            Duration.ofSeconds(30)
        );
    }
    
    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }
}

// Usage
DatabaseConfig config = DatabaseConfig.defaults("localhost", "mydb");
System.out.println(config.getJdbcUrl());
```

### Example 5: Kafka Messages

```java
public record OrderEvent(
    String eventId,
    String eventType,
    String orderId,
    String customerId,
    BigDecimal amount,
    LocalDateTime timestamp
) {
    public static OrderEvent orderCreated(String orderId, String customerId, BigDecimal amount) {
        return new OrderEvent(
            UUID.randomUUID().toString(),
            "ORDER_CREATED",
            orderId,
            customerId,
            amount,
            LocalDateTime.now()
        );
    }
}

// Producer
@Service
public class OrderEventProducer {
    
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void publishOrderCreated(String orderId, String customerId, BigDecimal amount) {
        OrderEvent event = OrderEvent.orderCreated(orderId, customerId, amount);
        kafkaTemplate.send("order-events", orderId, event);
    }
}
```

### Example 6: Pagination Response

```java
public record Page<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public Page {
        content = List.copyOf(content);  // Defensive copy
    }
    
    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }
    
    public boolean hasPrevious() {
        return pageNumber > 0;
    }
    
    public boolean isEmpty() {
        return content.isEmpty();
    }
}

// Usage
@GetMapping("/users")
public ResponseEntity<Page<UserResponse>> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    List<UserResponse> users = userService.findAll(page, size);
    long total = userService.count();
    int totalPages = (int) Math.ceil((double) total / size);
    
    Page<UserResponse> response = new Page<>(users, page, size, total, totalPages);
    return ResponseEntity.ok(response);
}
```

### Example 7: Result/Either Pattern

```java
public sealed interface Result<T> permits Success, Failure {
    
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error, Throwable cause) implements Result<T> {}
    
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }
    
    static <T> Result<T> failure(String error, Throwable cause) {
        return new Failure<>(error, cause);
    }
    
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    default T getOrThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new RuntimeException(f.error(), f.cause());
        };
    }
}

// Usage
public Result<User> findUser(Long id) {
    try {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return Result.success(user);
    } catch (Exception e) {
        return Result.failure("Failed to find user", e);
    }
}

// Consumer
Result<User> result = findUser(123L);
switch (result) {
    case Success<User> s -> System.out.println("Found: " + s.value());
    case Failure<User> f -> System.err.println("Error: " + f.error());
}
```

---

## Best Practices

### 1. Use Compact Constructor for Validation

```java
// ✅ Good
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
}

// ❌ Bad - validation in accessor
public record Email(String value) {
    public String value() {
        if (value == null) throw new IllegalArgumentException();
        return value;
    }
}
```

### 2. Defensive Copying for Mutable Fields

```java
// ✅ Good - defensive copy
public record Team(String name, List<String> members) {
    public Team {
        members = List.copyOf(members);
    }
}

// ❌ Bad - mutable reference
public record Team(String name, List<String> members) {}
```

### 3. Use Static Factory Methods

```java
// ✅ Good
public record Range(int start, int end) {
    public static Range of(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("Start must be <= end");
        }
        return new Range(start, end);
    }
}

Range range = Range.of(1, 10);
```

### 4. Keep Records Simple

```java
// ✅ Good - simple data carrier
public record User(Long id, String name, String email) {}

// ❌ Bad - too much logic
public record User(Long id, String name, String email) {
    public void sendEmail() { /* complex logic */ }
    public void updateProfile() { /* complex logic */ }
    public void deleteAccount() { /* complex logic */ }
}
```

### 5. Use Records for DTOs

```java
// ✅ Good - perfect for DTOs
public record CreateOrderRequest(
    String customerId,
    List<OrderItem> items,
    String shippingAddress
) {}

public record OrderResponse(
    String orderId,
    String status,
    BigDecimal total
) {}
```

### 6. Combine with Sealed Interfaces

```java
// ✅ Good - type-safe hierarchy
public sealed interface PaymentMethod permits CreditCard, PayPal, BankTransfer {}

public record CreditCard(String number, String cvv, String expiry) implements PaymentMethod {}
public record PayPal(String email) implements PaymentMethod {}
public record BankTransfer(String accountNumber, String routingNumber) implements PaymentMethod {}
```

---

## Summary

### Key Takeaways

1. **Records are immutable data carriers** - Perfect for DTOs, value objects, and data structures
2. **Compiler generates boilerplate** - equals(), hashCode(), toString(), constructor, accessors
3. **Cannot extend classes** - Already extends java.lang.Record
4. **Use compact constructor** - For validation and normalization
5. **Defensive copying** - For mutable fields like collections
6. **Pattern matching** - Works beautifully with switch expressions
7. **Sealed interfaces** - Create type-safe hierarchies
8. **Keep it simple** - Don't add complex business logic

### When to Use Records

```
Use Records:                    Use Classes:
├─ DTOs                        ├─ Entities with behavior
├─ API requests/responses      ├─ Mutable state
├─ Configuration objects       ├─ Inheritance needed
├─ Value objects               ├─ Complex business logic
├─ Event objects               ├─ Many optional fields
└─ Query results               └─ Legacy compatibility
```

Records are a powerful addition to Java that reduce boilerplate and make code more readable and maintainable! 🚀
