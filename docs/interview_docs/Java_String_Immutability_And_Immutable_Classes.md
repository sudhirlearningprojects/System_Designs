# Java String Immutability & Creating Immutable Classes

## Why Strings are Immutable in Java

### 1. String Pool (Memory Optimization)

**Problem Without Immutability**:
```java
String s1 = "Hello";
String s2 = "Hello"; // Points to same object in String Pool
s1.concat(" World"); // If mutable, would affect s2 too!
```

**With Immutability**:
```java
String s1 = "Hello";
String s2 = "Hello"; // Both point to same object in String Pool
String s3 = s1.concat(" World"); // Creates new object, s1 and s2 unchanged

// Memory saved: Only one "Hello" object in heap
```

**String Pool Visualization**:
```
Heap Memory:
┌─────────────────────────────────────┐
│  String Pool                        │
│  ┌───────────────┐                  │
│  │ "Hello"       │ ◄─── s1, s2      │
│  ├───────────────┤                  │
│  │ "World"       │                  │
│  ├───────────────┤                  │
│  │ "Hello World" │ ◄─── s3          │
│  └───────────────┘                  │
└─────────────────────────────────────┘
```

---

### 2. Security

**Scenario**: Database Connection
```java
// Immutable String - Safe
public void connect(String username, String password) {
    // username and password cannot be changed by malicious code
    Connection conn = DriverManager.getConnection(url, username, password);
}

// If String was mutable - Unsafe
public void connect(MutableString username, MutableString password) {
    // Malicious code could change username/password after validation
    if (isValid(username, password)) {
        // Between validation and usage, values could be changed!
        Connection conn = DriverManager.getConnection(url, username, password);
    }
}
```

**Real-World Example**:
```java
// File path security
public void loadFile(String filePath) {
    if (isSecurePath(filePath)) { // Check: "/safe/path/file.txt"
        // If String was mutable, filePath could be changed to "/etc/passwd"
        File file = new File(filePath);
        // Load file
    }
}
```

---

### 3. Thread Safety

**Without Immutability**:
```java
// Mutable String - Thread Unsafe
class Counter {
    private MutableString value = new MutableString("0");
    
    public void increment() {
        int current = Integer.parseInt(value.toString());
        value.setValue(String.valueOf(current + 1)); // Race condition!
    }
}

// Thread 1: reads "0", increments to "1"
// Thread 2: reads "0", increments to "1"
// Result: "1" instead of "2"
```

**With Immutability**:
```java
// Immutable String - Thread Safe
class Counter {
    private String value = "0";
    
    public synchronized void increment() {
        int current = Integer.parseInt(value);
        value = String.valueOf(current + 1); // Creates new object, no race condition
    }
}

// Multiple threads can safely read the same String object
String shared = "Hello";
// Thread 1: reads "Hello"
// Thread 2: reads "Hello"
// No synchronization needed for reads
```

---

### 4. HashCode Caching

**Performance Benefit**:
```java
public final class String {
    private int hash; // Cached hashcode
    
    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            hash = h = calculateHashCode(); // Calculate once
        }
        return h; // Return cached value
    }
}

// Usage in HashMap
Map<String, User> userMap = new HashMap<>();
String key = "user123";
userMap.put(key, user); // hashCode() calculated once
userMap.get(key); // hashCode() returned from cache (O(1))
userMap.get(key); // hashCode() returned from cache (O(1))
```

**Without Immutability**:
```java
// Mutable String - Cannot cache hashcode
public class MutableString {
    private char[] value;
    
    public int hashCode() {
        return calculateHashCode(); // Must recalculate every time!
    }
}

// HashMap would break
Map<MutableString, User> userMap = new HashMap<>();
MutableString key = new MutableString("user123");
userMap.put(key, user); // hashCode: 12345
key.setValue("user456"); // hashCode changes to 67890
userMap.get(key); // Cannot find entry! HashMap broken!
```

---

### 5. Class Loading

**ClassLoader Security**:
```java
// Class names are Strings
ClassLoader loader = new ClassLoader();
String className = "com.example.SafeClass";

// If String was mutable, malicious code could change it
// className.setValue("com.malicious.HackerClass");

Class<?> clazz = loader.loadClass(className); // Safe because String is immutable
```

---

## Creating Immutable Classes

### Basic Immutable Class

```java
/**
 * Immutable Person class
 * Rules:
 * 1. Class is final (cannot be extended)
 * 2. All fields are private and final
 * 3. No setter methods
 * 4. Initialize all fields in constructor
 * 5. Return copies of mutable objects
 */
public final class Person {
    private final String name;
    private final int age;
    private final String email;
    
    public Person(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
    
    public String getEmail() {
        return email;
    }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + ", email='" + email + "'}";
    }
}

// Usage
Person person = new Person("John", 30, "john@example.com");
System.out.println(person.getName()); // "John"
// person.setName("Jane"); // Compile error - no setter!
```

---

### Immutable Class with Mutable Fields (Deep Copy)

```java
import java.util.*;

/**
 * Immutable Employee class with mutable Date field
 * Key: Return defensive copies of mutable objects
 */
public final class Employee {
    private final String id;
    private final String name;
    private final Date hireDate; // Mutable object!
    private final List<String> skills; // Mutable collection!
    
    public Employee(String id, String name, Date hireDate, List<String> skills) {
        this.id = id;
        this.name = name;
        // Defensive copy: Create new Date object
        this.hireDate = new Date(hireDate.getTime());
        // Defensive copy: Create unmodifiable list
        this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Date getHireDate() {
        // Return defensive copy, not original
        return new Date(hireDate.getTime());
    }
    
    public List<String> getSkills() {
        // Return unmodifiable view
        return skills;
    }
}

// Usage
Date date = new Date();
List<String> skills = new ArrayList<>(Arrays.asList("Java", "Spring"));

Employee emp = new Employee("E001", "Alice", date, skills);

// Try to modify original objects
date.setTime(0); // Does not affect emp.hireDate
skills.add("Kafka"); // Does not affect emp.skills

// Try to modify returned objects
Date empDate = emp.getHireDate();
empDate.setTime(0); // Does not affect emp.hireDate (defensive copy)

List<String> empSkills = emp.getSkills();
// empSkills.add("Redis"); // Throws UnsupportedOperationException
```

---

### Immutable Class with Builder Pattern

```java
/**
 * Immutable User class with Builder pattern
 * Use when class has many optional fields
 */
public final class User {
    private final String id;
    private final String username;
    private final String email;
    private final String phone;
    private final String address;
    private final boolean active;
    
    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
        this.active = builder.active;
    }
    
    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    
    // Builder class
    public static class Builder {
        private final String id; // Required
        private final String username; // Required
        private String email;
        private String phone;
        private String address;
        private boolean active = true;
        
        public Builder(String id, String username) {
            this.id = id;
            this.username = username;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
}

// Usage
User user = new User.Builder("U001", "john_doe")
    .email("john@example.com")
    .phone("+1234567890")
    .address("123 Main St")
    .active(true)
    .build();

System.out.println(user.getUsername()); // "john_doe"
```

---

### Immutable Class with Nested Objects

```java
/**
 * Immutable Address class
 */
public final class Address {
    private final String street;
    private final String city;
    private final String zipCode;
    
    public Address(String street, String city, String zipCode) {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
    }
    
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getZipCode() { return zipCode; }
}

/**
 * Immutable Customer class with nested immutable object
 */
public final class Customer {
    private final String id;
    private final String name;
    private final Address address; // Immutable nested object
    
    public Customer(String id, String name, Address address) {
        this.id = id;
        this.name = name;
        this.address = address; // No defensive copy needed (Address is immutable)
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public Address getAddress() { 
        return address; // Safe to return (Address is immutable)
    }
}

// Usage
Address address = new Address("123 Main St", "New York", "10001");
Customer customer = new Customer("C001", "Bob", address);

// Cannot modify address
// address.setCity("Boston"); // Compile error - no setter!
```

---

### Immutable Class with Collections (Java 9+)

```java
import java.util.*;

/**
 * Immutable Product class using Java 9+ immutable collections
 */
public final class Product {
    private final String id;
    private final String name;
    private final List<String> tags;
    private final Map<String, String> attributes;
    private final Set<String> categories;
    
    public Product(String id, String name, 
                   List<String> tags, 
                   Map<String, String> attributes,
                   Set<String> categories) {
        this.id = id;
        this.name = name;
        // Java 9+ immutable collections
        this.tags = List.copyOf(tags);
        this.attributes = Map.copyOf(attributes);
        this.categories = Set.copyOf(categories);
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getTags() { return tags; }
    public Map<String, String> getAttributes() { return attributes; }
    public Set<String> getCategories() { return categories; }
}

// Usage
List<String> tags = new ArrayList<>(Arrays.asList("electronics", "gadget"));
Map<String, String> attrs = new HashMap<>();
attrs.put("color", "black");
attrs.put("brand", "Apple");
Set<String> categories = new HashSet<>(Arrays.asList("phones", "mobile"));

Product product = new Product("P001", "iPhone", tags, attrs, categories);

// Try to modify
// product.getTags().add("new"); // Throws UnsupportedOperationException
// product.getAttributes().put("size", "large"); // Throws UnsupportedOperationException
```

---

### Immutable Class with Validation

```java
/**
 * Immutable BankAccount with validation
 */
public final class BankAccount {
    private final String accountNumber;
    private final String holderName;
    private final double balance;
    
    public BankAccount(String accountNumber, String holderName, double balance) {
        // Validation
        if (accountNumber == null || accountNumber.isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if (holderName == null || holderName.isEmpty()) {
            throw new IllegalArgumentException("Holder name cannot be null or empty");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
    }
    
    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public double getBalance() { return balance; }
    
    // Return new instance for state changes
    public BankAccount deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        return new BankAccount(accountNumber, holderName, balance + amount);
    }
    
    public BankAccount withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        return new BankAccount(accountNumber, holderName, balance - amount);
    }
}

// Usage
BankAccount account = new BankAccount("ACC001", "John Doe", 1000.0);
System.out.println(account.getBalance()); // 1000.0

BankAccount newAccount = account.deposit(500.0);
System.out.println(account.getBalance()); // 1000.0 (original unchanged)
System.out.println(newAccount.getBalance()); // 1500.0 (new instance)
```

---

### Record Class (Java 14+)

```java
/**
 * Immutable class using Java Record (Java 14+)
 * Automatically generates:
 * - Constructor
 * - Getters
 * - equals()
 * - hashCode()
 * - toString()
 */
public record Point(int x, int y) {
    // Compact constructor for validation
    public Point {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates must be non-negative");
        }
    }
    
    // Custom methods
    public double distanceFromOrigin() {
        return Math.sqrt(x * x + y * y);
    }
}

// Usage
Point p1 = new Point(3, 4);
System.out.println(p1.x()); // 3
System.out.println(p1.y()); // 4
System.out.println(p1.distanceFromOrigin()); // 5.0
System.out.println(p1); // Point[x=3, y=4]

// Immutable - cannot change
// p1.x = 5; // Compile error - no setter!
```

---

## Immutability Checklist

### ✅ Rules for Creating Immutable Classes

1. **Declare class as `final`**
   ```java
   public final class MyClass { }
   ```

2. **Make all fields `private` and `final`**
   ```java
   private final String name;
   private final int age;
   ```

3. **No setter methods**
   ```java
   // ❌ Don't do this
   public void setName(String name) { }
   
   // ✅ Do this
   public String getName() { return name; }
   ```

4. **Initialize all fields in constructor**
   ```java
   public MyClass(String name, int age) {
       this.name = name;
       this.age = age;
   }
   ```

5. **Defensive copying for mutable fields**
   ```java
   // Constructor
   this.date = new Date(date.getTime());
   
   // Getter
   public Date getDate() {
       return new Date(date.getTime());
   }
   ```

6. **Return unmodifiable collections**
   ```java
   this.list = Collections.unmodifiableList(new ArrayList<>(list));
   // Or Java 9+
   this.list = List.copyOf(list);
   ```

---

## Benefits of Immutability

| Benefit | Description | Example |
|---------|-------------|---------|
| **Thread Safety** | No synchronization needed | Multiple threads can safely read |
| **Caching** | Safe to cache and reuse | String pool, HashMap keys |
| **Security** | Cannot be modified after creation | Passwords, file paths |
| **Simplicity** | Easier to understand and debug | No unexpected state changes |
| **Failure Atomicity** | Object remains valid even if operation fails | Transaction rollback |

---

## Performance Considerations

### When to Use Immutable Classes

✅ **Use Immutable Classes When**:
- Object represents a value (e.g., String, Integer, Date)
- Object is used as HashMap/HashSet key
- Object is shared across threads
- Object represents configuration or constants
- Object is part of API contract

❌ **Avoid Immutable Classes When**:
- Frequent modifications needed (use mutable alternative)
- Large objects with many fields (memory overhead)
- Performance-critical code with high object creation rate

### Example: Mutable vs Immutable Performance

```java
// Immutable - Creates many objects
String result = "";
for (int i = 0; i < 10000; i++) {
    result = result + i; // Creates 10000 String objects!
}

// Mutable - Better performance
StringBuilder result = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    result.append(i); // Modifies same object
}
```

---

## Summary

**Why Strings are Immutable**:
1. ✅ **String Pool** - Memory optimization
2. ✅ **Security** - Safe for sensitive data
3. ✅ **Thread Safety** - No synchronization needed
4. ✅ **HashCode Caching** - Performance optimization
5. ✅ **Class Loading** - ClassLoader security

**Creating Immutable Classes**:
1. Make class `final`
2. Make fields `private final`
3. No setters
4. Defensive copying for mutable fields
5. Return unmodifiable collections
6. Use Builder pattern for complex objects
7. Use Records (Java 14+) for simple data classes
