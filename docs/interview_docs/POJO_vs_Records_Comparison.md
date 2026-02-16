# POJO vs Records - Complete Comparison Guide

## Overview

**POJO (Plain Old Java Object)** and **Records** (Java 14+) are both used to represent data, but Records provide a more concise and immutable approach to data modeling.

---

## Basic Definitions

### POJO (Plain Old Java Object)
- Traditional Java class for data representation
- Mutable by default
- Requires boilerplate code (getters, setters, equals, hashCode, toString)
- Available since Java 1.0

### Record (Java 14+)
- Immutable data carrier introduced in Java 14
- Automatically generates boilerplate code
- Designed for data modeling and transfer
- Final and immutable by design

---

## Code Comparison

### Traditional POJO

```java
public class PersonPOJO {
    private String name;
    private int age;
    private String email;
    
    // Default constructor
    public PersonPOJO() {}
    
    // Parameterized constructor
    public PersonPOJO(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
    
    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setEmail(String email) { this.email = email; }
    
    // equals()
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PersonPOJO person = (PersonPOJO) obj;
        return age == person.age &&
               Objects.equals(name, person.name) &&
               Objects.equals(email, person.email);
    }
    
    // hashCode()
    @Override
    public int hashCode() {
        return Objects.hash(name, age, email);
    }
    
    // toString()
    @Override
    public String toString() {
        return "PersonPOJO{" +
               "name='" + name + '\'' +
               ", age=" + age +
               ", email='" + email + '\'' +
               '}';
    }
}
```

### Record (Java 14+)

```java
public record PersonRecord(String name, int age, String email) {
    // That's it! Everything else is auto-generated
}
```

**Usage:**
```java
// POJO usage
PersonPOJO pojo = new PersonPOJO("John", 25, "john@email.com");
pojo.setAge(26); // Mutable

// Record usage
PersonRecord record = new PersonRecord("John", 25, "john@email.com");
// record.age = 26; // Compilation error - immutable
```

---

## Detailed Feature Comparison

| Feature | POJO | Record |
|---------|------|--------|
| **Mutability** | Mutable (by default) | Immutable (always) |
| **Boilerplate Code** | Manual implementation required | Auto-generated |
| **Constructor** | Manual creation needed | Auto-generated canonical constructor |
| **Getters** | Manual getters (getName()) | Auto-generated accessors (name()) |
| **Setters** | Manual setters | Not available (immutable) |
| **equals()** | Manual implementation | Auto-generated |
| **hashCode()** | Manual implementation | Auto-generated |
| **toString()** | Manual implementation | Auto-generated |
| **Inheritance** | Can extend classes | Cannot extend classes (extends Record) |
| **Interface Implementation** | Yes | Yes |
| **Final Class** | Optional | Always final |
| **Serialization** | Manual implementation | Built-in support |
| **Validation** | In setters or constructors | In compact constructor |

---

## Advanced Record Features

### 1. Compact Constructor

```java
public record PersonRecord(String name, int age, String email) {
    // Compact constructor for validation
    public PersonRecord {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Normalize data
        name = name.trim();
        email = email.toLowerCase();
    }
}
```

### 2. Custom Methods in Records

```java
public record PersonRecord(String name, int age, String email) {
    
    // Custom methods
    public boolean isAdult() {
        return age >= 18;
    }
    
    public String getDisplayName() {
        return name.toUpperCase();
    }
    
    public PersonRecord withAge(int newAge) {
        return new PersonRecord(name, newAge, email);
    }
    
    // Static factory methods
    public static PersonRecord of(String name, int age) {
        return new PersonRecord(name, age, "unknown@email.com");
    }
}
```

### 3. Record with Generic Types

```java
public record Result<T>(T data, boolean success, String message) {
    
    public static <T> Result<T> success(T data) {
        return new Result<>(data, true, "Success");
    }
    
    public static <T> Result<T> failure(String message) {
        return new Result<>(null, false, message);
    }
}

// Usage
Result<String> result = Result.success("Hello World");
Result<Integer> errorResult = Result.failure("Invalid input");
```

---

## When to Use POJO vs Record

### Use POJO When:

```java
// 1. Need mutability
public class ShoppingCart {
    private List<Item> items = new ArrayList<>();
    
    public void addItem(Item item) {
        items.add(item);
    }
    
    public void removeItem(Item item) {
        items.remove(item);
    }
}

// 2. Need inheritance
public class Employee {
    protected String name;
    protected double salary;
}

public class Manager extends Employee {
    private List<Employee> subordinates;
}

// 3. Complex business logic with state changes
public class BankAccount {
    private double balance;
    
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
    
    public boolean withdraw(double amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
}
```

### Use Record When:

```java
// 1. Data Transfer Objects (DTOs)
public record UserDTO(Long id, String username, String email, LocalDateTime createdAt) {}

// 2. API Responses
public record ApiResponse<T>(T data, int status, String message, LocalDateTime timestamp) {}

// 3. Configuration objects
public record DatabaseConfig(String url, String username, String password, int maxConnections) {}

// 4. Value objects
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
}

// 5. Tuple-like structures
public record Pair<T, U>(T first, U second) {}
public record Triple<T, U, V>(T first, U second, V third) {}
```

---

## Performance Comparison

### Memory Usage

```java
// POJO - More memory overhead
public class PersonPOJO {
    private String name;    // 8 bytes reference
    private int age;        // 4 bytes
    private String email;   // 8 bytes reference
    // Object header: 12-16 bytes
    // Total: ~32-36 bytes + string data
}

// Record - Less memory overhead (optimized by JVM)
public record PersonRecord(String name, int age, String email) {
    // Object header: 12-16 bytes
    // Fields: 20 bytes
    // Total: ~32-36 bytes + string data
    // But better JVM optimizations
}
```

### Creation Performance

```java
// Benchmark example
@Benchmark
public PersonPOJO createPOJO() {
    return new PersonPOJO("John", 25, "john@email.com");
}

@Benchmark
public PersonRecord createRecord() {
    return new PersonRecord("John", 25, "john@email.com");
}

// Records are typically 10-20% faster for creation
```

---

## Serialization Comparison

### POJO Serialization

```java
// JSON serialization with Jackson
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonPOJO {
    @JsonProperty("full_name")
    private String name;
    
    private int age;
    
    @JsonIgnore
    private String password;
    
    // Getters and setters required
}
```

### Record Serialization

```java
// Records work seamlessly with Jackson
public record PersonRecord(
    @JsonProperty("full_name") String name,
    int age,
    @JsonIgnore String password
) {}

// No getters/setters needed
```

---

## Common Patterns and Best Practices

### 1. Builder Pattern with Records

```java
public record PersonRecord(String name, int age, String email, String phone) {
    
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
        
        public PersonRecord build() {
            return new PersonRecord(name, age, email, phone);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}

// Usage
PersonRecord person = PersonRecord.builder()
    .name("John")
    .age(25)
    .email("john@email.com")
    .build();
```

### 2. Record with Validation

```java
public record EmailRecord(String value) {
    public EmailRecord {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        value = value.toLowerCase().trim();
    }
    
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }
}
```

### 3. Record as Map Key

```java
public record PersonKey(String firstName, String lastName, LocalDate birthDate) {}

// Usage
Map<PersonKey, PersonRecord> personMap = new HashMap<>();
PersonKey key = new PersonKey("John", "Doe", LocalDate.of(1990, 1, 1));
PersonRecord person = new PersonRecord("John Doe", 33, "john@email.com");
personMap.put(key, person);

// Works perfectly due to auto-generated equals() and hashCode()
```

---

## Migration from POJO to Record

### Before (POJO)

```java
public class OrderPOJO {
    private Long id;
    private String customerName;
    private BigDecimal amount;
    private LocalDateTime orderDate;
    
    // 50+ lines of boilerplate code...
}
```

### After (Record)

```java
public record OrderRecord(
    Long id,
    String customerName,
    BigDecimal amount,
    LocalDateTime orderDate
) {
    // Custom methods if needed
    public boolean isExpensive() {
        return amount.compareTo(BigDecimal.valueOf(1000)) > 0;
    }
}
```

---

## Interview Questions & Answers

### Q1: Can a Record extend another class?

**Answer:** No, Records cannot extend other classes because they implicitly extend `java.lang.Record`. However, they can implement interfaces.

```java
// This won't compile
public record PersonRecord(String name) extends SomeClass {} // ❌

// This works
public interface Identifiable {
    String getId();
}

public record PersonRecord(String name, String id) implements Identifiable {
    @Override
    public String getId() {
        return id;
    }
} // ✅
```

### Q2: How do you modify a Record field?

**Answer:** Records are immutable. You create new instances with modified values.

```java
public record PersonRecord(String name, int age) {
    public PersonRecord withAge(int newAge) {
        return new PersonRecord(name, newAge);
    }
}

// Usage
PersonRecord person = new PersonRecord("John", 25);
PersonRecord olderPerson = person.withAge(26);
```

### Q3: Can Records have static fields and methods?

**Answer:** Yes, Records can have static fields and methods.

```java
public record PersonRecord(String name, int age) {
    public static final int MAX_AGE = 150;
    
    public static PersonRecord createChild(String name) {
        return new PersonRecord(name, 0);
    }
}
```

---

## Key Takeaways

### POJO Advantages:
- **Flexibility**: Mutable, can extend classes
- **Legacy compatibility**: Works with older Java versions
- **Complex state management**: Better for objects with changing state

### Record Advantages:
- **Conciseness**: Minimal boilerplate code
- **Immutability**: Thread-safe by design
- **Performance**: JVM optimizations
- **Data integrity**: Built-in validation support
- **Modern approach**: Designed for data modeling

### When to Choose:
- **Use Records** for: DTOs, value objects, configuration, API responses
- **Use POJOs** for: Entities with behavior, mutable objects, inheritance hierarchies

Records represent a modern approach to data modeling in Java, reducing boilerplate while promoting immutability and better design practices.