# Java Optional Class - Deep Dive

## Overview

`Optional<T>` is a container object introduced in Java 8 to represent the presence or absence of a value, eliminating the need for null checks and preventing NullPointerException.

**Key Benefits**:
- Eliminates NullPointerException
- Makes null handling explicit
- Improves code readability
- Functional programming style
- Better API design

---

## The Problem: NullPointerException

### Before Optional (Java 7 and earlier)

```java
public class UserService {
    
    public String getUserEmail(Long userId) {
        User user = userRepository.findById(userId);
        if (user != null) {
            Address address = user.getAddress();
            if (address != null) {
                String email = address.getEmail();
                if (email != null) {
                    return email.toLowerCase();
                }
            }
        }
        return "default@email.com";
    }
}
```

**Problems**:
- Nested null checks (pyramid of doom)
- Easy to forget null checks
- NullPointerException at runtime
- Verbose and hard to read

---

## Creating Optional

### 1. Optional.of() - Non-null value

```java
Optional<String> optional = Optional.of("Hello");
System.out.println(optional.get()); // "Hello"

Optional<String> nullOptional = Optional.of(null); // NullPointerException!
```

**Use when**: Value is guaranteed to be non-null

---

### 2. Optional.ofNullable() - May be null

```java
String value = null;
Optional<String> optional = Optional.ofNullable(value);
System.out.println(optional.isPresent()); // false

String value2 = "Hello";
Optional<String> optional2 = Optional.ofNullable(value2);
System.out.println(optional2.isPresent()); // true
```

**Use when**: Value might be null (most common)

---

### 3. Optional.empty() - Empty Optional

```java
Optional<String> empty = Optional.empty();
System.out.println(empty.isPresent()); // false
```

**Use when**: Explicitly returning empty Optional

---

## Checking for Value

### isPresent() and isEmpty()

```java
Optional<String> optional = Optional.of("Hello");

// Check if value present
if (optional.isPresent()) {
    System.out.println("Value: " + optional.get());
}

// Check if empty (Java 11+)
if (optional.isEmpty()) {
    System.out.println("No value");
}
```

---

## Retrieving Value

### 1. get() - Direct access (Dangerous!)

```java
Optional<String> optional = Optional.of("Hello");
String value = optional.get(); // "Hello"

Optional<String> empty = Optional.empty();
String value2 = empty.get(); // NoSuchElementException!
```

**Warning**: Only use if you're sure value exists

---

### 2. orElse() - Default value

```java
Optional<String> optional = Optional.empty();
String value = optional.orElse("Default");
System.out.println(value); // "Default"

Optional<String> optional2 = Optional.of("Hello");
String value2 = optional2.orElse("Default");
System.out.println(value2); // "Hello"
```

**Note**: Default value is always evaluated

---

### 3. orElseGet() - Lazy default value

```java
Optional<String> optional = Optional.empty();
String value = optional.orElseGet(() -> {
    System.out.println("Computing default...");
    return "Default";
});
// Output: "Computing default..."
// value = "Default"

Optional<String> optional2 = Optional.of("Hello");
String value2 = optional2.orElseGet(() -> {
    System.out.println("Computing default...");
    return "Default";
});
// No output (supplier not called)
// value2 = "Hello"
```

**Use when**: Default value is expensive to compute

---

### 4. orElseThrow() - Throw exception

```java
Optional<String> optional = Optional.empty();

// Throw default exception
String value = optional.orElseThrow(); // NoSuchElementException

// Throw custom exception
String value2 = optional.orElseThrow(() -> 
    new IllegalArgumentException("Value not found"));
```

**Use when**: Absence of value is exceptional

---

## Transforming Optional

### 1. map() - Transform value

```java
Optional<String> optional = Optional.of("hello");

Optional<String> upper = optional.map(String::toUpperCase);
System.out.println(upper.get()); // "HELLO"

Optional<Integer> length = optional.map(String::length);
System.out.println(length.get()); // 5

// Empty optional
Optional<String> empty = Optional.empty();
Optional<String> result = empty.map(String::toUpperCase);
System.out.println(result.isPresent()); // false
```

**Chain transformations**:
```java
Optional<String> result = Optional.of("  hello  ")
    .map(String::trim)
    .map(String::toUpperCase)
    .map(s -> s + "!");

System.out.println(result.get()); // "HELLO!"
```

---

### 2. flatMap() - Transform to Optional

```java
public class User {
    private Optional<Address> address;
    
    public Optional<Address> getAddress() {
        return address;
    }
}

public class Address {
    private String city;
    
    public String getCity() {
        return city;
    }
}

// Without flatMap (nested Optional)
Optional<User> user = Optional.of(new User());
Optional<Optional<Address>> nestedAddress = user.map(User::getAddress);

// With flatMap (flattened)
Optional<Address> address = user.flatMap(User::getAddress);
Optional<String> city = user.flatMap(User::getAddress)
                            .map(Address::getCity);
```

**Use when**: Transformation returns Optional

---

### 3. filter() - Conditional filtering

```java
Optional<Integer> number = Optional.of(42);

Optional<Integer> even = number.filter(n -> n % 2 == 0);
System.out.println(even.isPresent()); // true

Optional<Integer> greaterThan50 = number.filter(n -> n > 50);
System.out.println(greaterThan50.isPresent()); // false
```

**Example: Validate email**
```java
Optional<String> email = Optional.of("user@example.com");

Optional<String> validEmail = email.filter(e -> e.contains("@"))
                                   .filter(e -> e.length() > 5);

System.out.println(validEmail.isPresent()); // true
```

---

## Consuming Optional

### 1. ifPresent() - Execute if present

```java
Optional<String> optional = Optional.of("Hello");

optional.ifPresent(value -> {
    System.out.println("Value: " + value);
});
// Output: "Value: Hello"

Optional<String> empty = Optional.empty();
empty.ifPresent(value -> {
    System.out.println("Value: " + value);
});
// No output
```

---

### 2. ifPresentOrElse() - Execute if present or else (Java 9+)

```java
Optional<String> optional = Optional.of("Hello");

optional.ifPresentOrElse(
    value -> System.out.println("Value: " + value),
    () -> System.out.println("No value")
);
// Output: "Value: Hello"

Optional<String> empty = Optional.empty();
empty.ifPresentOrElse(
    value -> System.out.println("Value: " + value),
    () -> System.out.println("No value")
);
// Output: "No value"
```

---

## Real-World Examples

### Example 1: Repository Pattern

```java
public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public String getUserEmail(Long userId) {
        return userRepository.findById(userId)
            .map(User::getEmail)
            .orElse("unknown@email.com");
    }
    
    public User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}
```

---

### Example 2: Nested Object Navigation

```java
public class Order {
    private Optional<Customer> customer;
    
    public Optional<Customer> getCustomer() {
        return customer;
    }
}

public class Customer {
    private Optional<Address> address;
    
    public Optional<Address> getAddress() {
        return address;
    }
}

public class Address {
    private String city;
    
    public String getCity() {
        return city;
    }
}

// Get city from order
public String getOrderCity(Order order) {
    return Optional.ofNullable(order)
        .flatMap(Order::getCustomer)
        .flatMap(Customer::getAddress)
        .map(Address::getCity)
        .orElse("Unknown");
}
```

**Before Optional**:
```java
public String getOrderCity(Order order) {
    if (order != null) {
        Customer customer = order.getCustomer();
        if (customer != null) {
            Address address = customer.getAddress();
            if (address != null) {
                return address.getCity();
            }
        }
    }
    return "Unknown";
}
```

---

### Example 3: Configuration Properties

```java
@Service
public class ConfigService {
    
    private Map<String, String> config = new HashMap<>();
    
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(config.get(key));
    }
    
    public int getIntProperty(String key, int defaultValue) {
        return getProperty(key)
            .map(Integer::parseInt)
            .orElse(defaultValue);
    }
    
    public String getRequiredProperty(String key) {
        return getProperty(key)
            .orElseThrow(() -> new IllegalStateException(
                "Required property not found: " + key));
    }
}
```

---

### Example 4: Stream API Integration

```java
List<User> users = Arrays.asList(
    new User(1L, "Alice", "alice@example.com"),
    new User(2L, "Bob", null),
    new User(3L, "Charlie", "charlie@example.com")
);

// Find first user with email
Optional<User> userWithEmail = users.stream()
    .filter(u -> u.getEmail() != null)
    .findFirst();

userWithEmail.ifPresent(u -> 
    System.out.println("Found: " + u.getName()));

// Get all emails (skip nulls)
List<String> emails = users.stream()
    .map(User::getEmail)
    .flatMap(Optional::ofNullable)
    .collect(Collectors.toList());
```

---

### Example 5: REST API Response

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/email")
    public ResponseEntity<String> getUserEmail(@PathVariable Long id) {
        Optional<String> email = userService.findById(id)
            .map(User::getEmail)
            .filter(e -> !e.isEmpty());
        
        return email.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.noContent().build());
    }
}
```

---

## Common Patterns

### Pattern 1: Default Value

```java
// Bad
String name = user.getName();
if (name == null) {
    name = "Guest";
}

// Good
String name = Optional.ofNullable(user.getName())
    .orElse("Guest");
```

---

### Pattern 2: Throw Exception

```java
// Bad
User user = userRepository.findById(id);
if (user == null) {
    throw new UserNotFoundException("User not found");
}

// Good
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserNotFoundException("User not found"));
```

---

### Pattern 3: Conditional Execution

```java
// Bad
User user = userRepository.findById(id);
if (user != null) {
    sendEmail(user.getEmail());
}

// Good
userRepository.findById(id)
    .ifPresent(user -> sendEmail(user.getEmail()));
```

---

### Pattern 4: Chain Transformations

```java
// Bad
User user = userRepository.findById(id);
String email = null;
if (user != null) {
    email = user.getEmail();
    if (email != null) {
        email = email.toLowerCase();
    }
}

// Good
String email = userRepository.findById(id)
    .map(User::getEmail)
    .map(String::toLowerCase)
    .orElse(null);
```

---

## Anti-Patterns (What NOT to Do)

### 1. Don't use get() without checking

```java
// Bad
Optional<String> optional = Optional.empty();
String value = optional.get(); // NoSuchElementException!

// Good
String value = optional.orElse("default");
```

---

### 2. Don't use isPresent() + get()

```java
// Bad
Optional<String> optional = Optional.of("Hello");
if (optional.isPresent()) {
    String value = optional.get();
    System.out.println(value);
}

// Good
optional.ifPresent(System.out::println);
```

---

### 3. Don't use Optional for fields

```java
// Bad
public class User {
    private Optional<String> email; // Don't do this!
}

// Good
public class User {
    private String email; // Can be null
    
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }
}
```

---

### 4. Don't use Optional for collections

```java
// Bad
public Optional<List<User>> getUsers() {
    return Optional.ofNullable(users);
}

// Good
public List<User> getUsers() {
    return users != null ? users : Collections.emptyList();
}
```

---

### 5. Don't use Optional as method parameter

```java
// Bad
public void processUser(Optional<User> user) {
    user.ifPresent(u -> process(u));
}

// Good
public void processUser(User user) {
    if (user != null) {
        process(user);
    }
}
```

---

## Performance Considerations

### Optional vs Null Check

```java
// Null check (faster)
if (value != null) {
    return value.toUpperCase();
}

// Optional (slightly slower due to object creation)
return Optional.ofNullable(value)
    .map(String::toUpperCase)
    .orElse(null);
```

**When to use Optional**:
- API design (return types)
- Stream operations
- Functional programming style

**When to use null checks**:
- Performance-critical code
- Internal implementation
- Simple null checks

---

## Optional with Streams

### flatMap with Optional

```java
List<User> users = Arrays.asList(
    new User(1L, "Alice"),
    new User(2L, "Bob"),
    new User(3L, "Charlie")
);

// Get all addresses (skip users without address)
List<Address> addresses = users.stream()
    .map(User::getAddress) // Returns Optional<Address>
    .flatMap(Optional::stream) // Java 9+
    .collect(Collectors.toList());

// Java 8 alternative
List<Address> addresses = users.stream()
    .map(User::getAddress)
    .filter(Optional::isPresent)
    .map(Optional::get)
    .collect(Collectors.toList());
```

---

## Java 9+ Enhancements

### 1. or() - Alternative Optional

```java
Optional<String> optional = Optional.empty();

Optional<String> result = optional.or(() -> Optional.of("Alternative"));
System.out.println(result.get()); // "Alternative"
```

---

### 2. stream() - Convert to Stream

```java
Optional<String> optional = Optional.of("Hello");

optional.stream()
    .map(String::toUpperCase)
    .forEach(System.out::println);
// Output: "HELLO"
```

---

### 3. ifPresentOrElse()

```java
Optional<String> optional = Optional.of("Hello");

optional.ifPresentOrElse(
    value -> System.out.println("Present: " + value),
    () -> System.out.println("Empty")
);
```

---

## Comparison Table

| Method | Returns | Use Case |
|--------|---------|----------|
| `of()` | Optional | Non-null value |
| `ofNullable()` | Optional | Possibly null value |
| `empty()` | Optional | Empty Optional |
| `isPresent()` | boolean | Check if value exists |
| `isEmpty()` | boolean | Check if empty (Java 11+) |
| `get()` | T | Get value (unsafe) |
| `orElse()` | T | Default value |
| `orElseGet()` | T | Lazy default value |
| `orElseThrow()` | T | Throw exception |
| `map()` | Optional | Transform value |
| `flatMap()` | Optional | Transform to Optional |
| `filter()` | Optional | Conditional filtering |
| `ifPresent()` | void | Execute if present |

---

## Interview Questions & Answers

### Q1: What is Optional and why was it introduced?

**Answer**: Optional is a container object to represent presence or absence of a value, introduced in Java 8 to eliminate NullPointerException and make null handling explicit.

### Q2: Difference between orElse() and orElseGet()?

**Answer**:
- `orElse()`: Default value always evaluated
- `orElseGet()`: Supplier called only if Optional is empty (lazy)

### Q3: When should you NOT use Optional?

**Answer**:
- As class fields
- As method parameters
- For collections (use empty collection instead)
- In performance-critical code

### Q4: What's wrong with optional.isPresent() + optional.get()?

**Answer**: It's verbose and defeats the purpose of Optional. Use `ifPresent()`, `map()`, or `orElse()` instead.

### Q5: Can Optional contain null?

**Answer**: No. `Optional.of(null)` throws NullPointerException. Use `Optional.ofNullable()` for potentially null values.

---

## Key Takeaways

1. **Use Optional for return types**, not fields or parameters
2. **Prefer orElse/orElseGet** over get()
3. **Use map/flatMap** for transformations
4. **Avoid isPresent() + get()** pattern
5. **Don't use Optional for collections**
6. **Use ifPresent()** for conditional execution
7. **orElseThrow()** for exceptional cases
8. **filter()** for conditional logic
9. **flatMap()** for nested Optionals
10. **Consider performance** in critical code

---

## Practice Problems

1. Convert null-checking code to Optional
2. Implement nested object navigation with Optional
3. Use Optional with Stream API
4. Handle multiple Optional values
5. Implement repository pattern with Optional
6. Create REST API with Optional responses
7. Transform Optional chain with map/flatMap
8. Implement configuration service with Optional
9. Handle Optional in concurrent code
10. Optimize Optional usage for performance
