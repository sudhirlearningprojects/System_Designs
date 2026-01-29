# StringBuilder vs StringBuffer - Deep Dive

## Overview

Both **StringBuilder** and **StringBuffer** are mutable classes for string manipulation, unlike immutable `String` class. The key difference is **thread-safety**.

**Key Difference**:
- **StringBuilder**: Not thread-safe, faster (Java 5+)
- **StringBuffer**: Thread-safe, slower (Java 1.0)

---

## Quick Comparison Table

| Feature | StringBuilder | StringBuffer | String |
|---------|--------------|--------------|--------|
| **Mutability** | Mutable | Mutable | Immutable |
| **Thread-Safe** | ❌ No | ✅ Yes | ✅ Yes |
| **Performance** | Fast | Slow | Slowest (concat) |
| **Synchronization** | No | Yes (synchronized) | N/A |
| **Use Case** | Single-threaded | Multi-threaded | Immutable data |
| **Since** | Java 5 (1.5) | Java 1.0 | Java 1.0 |
| **Memory** | Efficient | Efficient | Creates new objects |

---

## 1. Thread Safety

### StringBuilder (Not Thread-Safe)

```java
StringBuilder sb = new StringBuilder("Hello");

// Single thread - Works fine
sb.append(" World");
System.out.println(sb); // "Hello World"

// Multiple threads - Race condition!
StringBuilder sb2 = new StringBuilder();
Thread t1 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        sb2.append("A");
    }
});
Thread t2 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        sb2.append("B");
    }
});

t1.start();
t2.start();
t1.join();
t2.join();

System.out.println(sb2.length()); // Not guaranteed to be 2000!
```

**Output** (unpredictable):
```
1987  // Lost updates due to race condition
```

---

### StringBuffer (Thread-Safe)

```java
StringBuffer sb = new StringBuffer();

Thread t1 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        sb.append("A");
    }
});
Thread t2 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        sb.append("B");
    }
});

t1.start();
t2.start();
t1.join();
t2.join();

System.out.println(sb.length()); // Always 2000
```

**Output** (guaranteed):
```
2000  // Thread-safe, no lost updates
```

---

## 2. Performance Comparison

### Benchmark Test

```java
public class PerformanceTest {
    
    public static void main(String[] args) {
        int iterations = 100000;
        
        // Test 1: StringBuilder
        long start1 = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            sb.append("test");
        }
        long end1 = System.nanoTime();
        System.out.println("StringBuilder: " + (end1 - start1) / 1_000_000 + " ms");
        
        // Test 2: StringBuffer
        long start2 = System.nanoTime();
        StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < iterations; i++) {
            sbf.append("test");
        }
        long end2 = System.nanoTime();
        System.out.println("StringBuffer: " + (end2 - start2) / 1_000_000 + " ms");
        
        // Test 3: String concatenation
        long start3 = System.nanoTime();
        String str = "";
        for (int i = 0; i < 10000; i++) { // Reduced iterations
            str += "test";
        }
        long end3 = System.nanoTime();
        System.out.println("String concat: " + (end3 - start3) / 1_000_000 + " ms");
    }
}
```

**Output**:
```
StringBuilder: 15 ms
StringBuffer: 28 ms (1.8x slower)
String concat: 2847 ms (189x slower!)
```

**Why StringBuffer is slower?**
- Every method is `synchronized`
- Synchronization overhead even in single-threaded context

---

## 3. Internal Implementation

### StringBuilder (No Synchronization)

```java
public final class StringBuilder extends AbstractStringBuilder {
    
    @Override
    public StringBuilder append(String str) {
        super.append(str);  // No synchronization
        return this;
    }
    
    @Override
    public StringBuilder insert(int offset, String str) {
        super.insert(offset, str);  // No synchronization
        return this;
    }
}
```

---

### StringBuffer (Synchronized Methods)

```java
public final class StringBuffer extends AbstractStringBuilder {
    
    @Override
    public synchronized StringBuffer append(String str) {
        super.append(str);  // Synchronized
        return this;
    }
    
    @Override
    public synchronized StringBuffer insert(int offset, String str) {
        super.insert(offset, str);  // Synchronized
        return this;
    }
}
```

**Key Point**: Every method in StringBuffer is `synchronized`, adding overhead.

---

## 4. Common Methods (Same API)

### Append Operations

```java
StringBuilder sb = new StringBuilder("Hello");
StringBuffer sbf = new StringBuffer("Hello");

// Both have identical API
sb.append(" World");
sbf.append(" World");

sb.append(123);
sbf.append(123);

sb.append(true);
sbf.append(true);

System.out.println(sb);   // "Hello World123true"
System.out.println(sbf);  // "Hello World123true"
```

---

### Insert Operations

```java
StringBuilder sb = new StringBuilder("Hello World");
sb.insert(5, ",");
System.out.println(sb); // "Hello, World"

StringBuffer sbf = new StringBuffer("Hello World");
sbf.insert(5, ",");
System.out.println(sbf); // "Hello, World"
```

---

### Delete Operations

```java
StringBuilder sb = new StringBuilder("Hello World");
sb.delete(5, 11);
System.out.println(sb); // "Hello"

StringBuffer sbf = new StringBuffer("Hello World");
sbf.delete(5, 11);
System.out.println(sbf); // "Hello"
```

---

### Replace Operations

```java
StringBuilder sb = new StringBuilder("Hello World");
sb.replace(6, 11, "Java");
System.out.println(sb); // "Hello Java"

StringBuffer sbf = new StringBuffer("Hello World");
sbf.replace(6, 11, "Java");
System.out.println(sbf); // "Hello Java"
```

---

### Reverse Operations

```java
StringBuilder sb = new StringBuilder("Hello");
sb.reverse();
System.out.println(sb); // "olleH"

StringBuffer sbf = new StringBuffer("Hello");
sbf.reverse();
System.out.println(sbf); // "olleH"
```

---

## 5. When to Use What?

### Use StringBuilder (Most Common)

```java
// ✅ Single-threaded string building
public String buildQuery(List<String> fields) {
    StringBuilder query = new StringBuilder("SELECT ");
    for (int i = 0; i < fields.size(); i++) {
        query.append(fields.get(i));
        if (i < fields.size() - 1) {
            query.append(", ");
        }
    }
    query.append(" FROM users");
    return query.toString();
}

// ✅ Local variable (thread-confined)
public String formatUser(User user) {
    StringBuilder sb = new StringBuilder();
    sb.append("Name: ").append(user.getName()).append("\n");
    sb.append("Email: ").append(user.getEmail()).append("\n");
    sb.append("Age: ").append(user.getAge());
    return sb.toString();
}

// ✅ Loop concatenation
public String generateReport(List<Order> orders) {
    StringBuilder report = new StringBuilder();
    for (Order order : orders) {
        report.append("Order ID: ").append(order.getId()).append("\n");
    }
    return report.toString();
}
```

---

### Use StringBuffer (Rare)

```java
// ✅ Shared mutable state across threads
public class Logger {
    private StringBuffer logBuffer = new StringBuffer();
    
    public void log(String message) {
        // Multiple threads calling this
        logBuffer.append(LocalDateTime.now())
                 .append(": ")
                 .append(message)
                 .append("\n");
    }
    
    public String getLogs() {
        return logBuffer.toString();
    }
}

// ✅ Legacy code (pre-Java 5)
// StringBuffer was the only option before StringBuilder
```

---

### Use String (Immutable)

```java
// ✅ Few concatenations (compiler optimizes)
String greeting = "Hello" + " " + "World"; // OK

// ✅ Immutable data
public class User {
    private final String name; // Immutable
}

// ✅ HashMap keys
Map<String, User> userMap = new HashMap<>();
userMap.put("user123", user); // String is immutable
```

---

## 6. Real-World Examples

### Example 1: JSON Builder (StringBuilder)

```java
public class JsonBuilder {
    
    public String buildUserJson(User user) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(user.getId()).append(",");
        json.append("\"name\":\"").append(user.getName()).append("\",");
        json.append("\"email\":\"").append(user.getEmail()).append("\"");
        json.append("}");
        return json.toString();
    }
}
```

**Output**:
```json
{"id":123,"name":"John","email":"john@example.com"}
```

---

### Example 2: CSV Generator (StringBuilder)

```java
public class CsvGenerator {
    
    public String generateCsv(List<User> users) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Email\n");
        
        for (User user : users) {
            csv.append(user.getId()).append(",")
               .append(user.getName()).append(",")
               .append(user.getEmail()).append("\n");
        }
        
        return csv.toString();
    }
}
```

**Output**:
```
ID,Name,Email
1,John,john@example.com
2,Jane,jane@example.com
```

---

### Example 3: Thread-Safe Logger (StringBuffer)

```java
public class ThreadSafeLogger {
    
    private final StringBuffer buffer = new StringBuffer();
    
    public void log(String level, String message) {
        // Called by multiple threads
        buffer.append("[")
              .append(LocalDateTime.now())
              .append("] ")
              .append(level)
              .append(": ")
              .append(message)
              .append("\n");
    }
    
    public String getFullLog() {
        return buffer.toString();
    }
    
    public void clear() {
        buffer.setLength(0);
    }
}

// Usage
ThreadSafeLogger logger = new ThreadSafeLogger();

Thread t1 = new Thread(() -> logger.log("INFO", "User logged in"));
Thread t2 = new Thread(() -> logger.log("ERROR", "Connection failed"));

t1.start();
t2.start();
```

---

### Example 4: SQL Query Builder (StringBuilder)

```java
public class QueryBuilder {
    
    public String buildSelectQuery(String table, List<String> columns, 
                                   Map<String, Object> conditions) {
        StringBuilder query = new StringBuilder("SELECT ");
        
        // Columns
        query.append(String.join(", ", columns));
        query.append(" FROM ").append(table);
        
        // WHERE clause
        if (!conditions.isEmpty()) {
            query.append(" WHERE ");
            int i = 0;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                if (i > 0) query.append(" AND ");
                query.append(entry.getKey())
                     .append(" = '")
                     .append(entry.getValue())
                     .append("'");
                i++;
            }
        }
        
        return query.toString();
    }
}

// Usage
QueryBuilder builder = new QueryBuilder();
String query = builder.buildSelectQuery(
    "users",
    Arrays.asList("id", "name", "email"),
    Map.of("status", "active", "role", "admin")
);

System.out.println(query);
// SELECT id, name, email FROM users WHERE status = 'active' AND role = 'admin'
```

---

## 7. String vs StringBuilder vs StringBuffer

### Immutability Comparison

```java
// String - Immutable (creates new objects)
String str = "Hello";
str = str + " World";  // Creates new String object
str = str + "!";       // Creates another new String object

// StringBuilder - Mutable (modifies same object)
StringBuilder sb = new StringBuilder("Hello");
sb.append(" World");   // Modifies same object
sb.append("!");        // Modifies same object

// StringBuffer - Mutable (modifies same object, thread-safe)
StringBuffer sbf = new StringBuffer("Hello");
sbf.append(" World");  // Modifies same object (synchronized)
sbf.append("!");       // Modifies same object (synchronized)
```

---

### Memory Comparison

```java
// String concatenation - Creates multiple objects
String result = "";
for (int i = 0; i < 1000; i++) {
    result += "test";  // Creates 1000 new String objects!
}

// StringBuilder - Single object, grows capacity
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append("test");  // Same object, efficient
}
```

**Memory Usage**:
- **String**: ~1000 objects created (garbage collection overhead)
- **StringBuilder**: 1 object (capacity grows as needed)

---

## 8. Capacity Management

### Initial Capacity

```java
// Default capacity: 16 characters
StringBuilder sb1 = new StringBuilder();
System.out.println(sb1.capacity()); // 16

// Custom capacity
StringBuilder sb2 = new StringBuilder(100);
System.out.println(sb2.capacity()); // 100

// From String (length + 16)
StringBuilder sb3 = new StringBuilder("Hello");
System.out.println(sb3.capacity()); // 21 (5 + 16)
```

---

### Capacity Growth

```java
StringBuilder sb = new StringBuilder(10);
System.out.println("Initial capacity: " + sb.capacity()); // 10

sb.append("Hello World!");
System.out.println("After append: " + sb.capacity()); // 22 (10*2 + 2)

// Manual capacity management
sb.ensureCapacity(50);
System.out.println("After ensure: " + sb.capacity()); // 50

sb.trimToSize();
System.out.println("After trim: " + sb.capacity()); // 12 (actual length)
```

**Growth Formula**: `(oldCapacity * 2) + 2`

---

## 9. Common Mistakes

### Mistake 1: Using String Concatenation in Loops

```java
// ❌ Bad: Creates many objects
String result = "";
for (int i = 0; i < 1000; i++) {
    result += "item" + i + ",";
}

// ✅ Good: Use StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append("item").append(i).append(",");
}
String result = sb.toString();
```

---

### Mistake 2: Using StringBuffer in Single-Threaded Code

```java
// ❌ Bad: Unnecessary synchronization overhead
public String buildMessage(List<String> parts) {
    StringBuffer sb = new StringBuffer();
    for (String part : parts) {
        sb.append(part);
    }
    return sb.toString();
}

// ✅ Good: Use StringBuilder
public String buildMessage(List<String> parts) {
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
        sb.append(part);
    }
    return sb.toString();
}
```

---

### Mistake 3: Not Setting Initial Capacity

```java
// ❌ Bad: Multiple capacity expansions
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sb.append("test");
}

// ✅ Good: Set appropriate initial capacity
StringBuilder sb = new StringBuilder(40000); // 10000 * 4 chars
for (int i = 0; i < 10000; i++) {
    sb.append("test");
}
```

---

### Mistake 4: Sharing StringBuilder Across Threads

```java
// ❌ Bad: Race condition
public class Service {
    private StringBuilder sharedBuilder = new StringBuilder();
    
    public void append(String text) {
        sharedBuilder.append(text); // Not thread-safe!
    }
}

// ✅ Good: Use StringBuffer or synchronize
public class Service {
    private StringBuffer sharedBuffer = new StringBuffer();
    
    public void append(String text) {
        sharedBuffer.append(text); // Thread-safe
    }
}
```

---

## 10. Best Practices

### ✅ Do's

```java
// 1. Use StringBuilder for single-threaded operations
StringBuilder sb = new StringBuilder();

// 2. Set initial capacity if size is known
StringBuilder sb = new StringBuilder(1000);

// 3. Use method chaining
sb.append("Hello").append(" ").append("World");

// 4. Reuse StringBuilder in tight loops
StringBuilder sb = new StringBuilder();
for (Item item : items) {
    sb.setLength(0); // Clear
    sb.append(item.getName());
    process(sb.toString());
}

// 5. Use StringBuffer only when needed
public class ThreadSafeCache {
    private StringBuffer buffer = new StringBuffer();
}
```

---

### ❌ Don'ts

```java
// 1. Don't use String concatenation in loops
for (int i = 0; i < 1000; i++) {
    str += "test"; // Creates 1000 objects
}

// 2. Don't use StringBuffer unnecessarily
StringBuilder sb = new StringBuilder(); // Use this instead

// 3. Don't forget to call toString()
StringBuilder sb = new StringBuilder("Hello");
System.out.println(sb); // Prints object representation

// 4. Don't share StringBuilder across threads
private StringBuilder shared = new StringBuilder(); // Not thread-safe

// 5. Don't create new StringBuilder in every iteration
for (int i = 0; i < 1000; i++) {
    StringBuilder sb = new StringBuilder(); // Wasteful
}
```

---

## Interview Questions

### Q1: What is the main difference between StringBuilder and StringBuffer?

**Answer**: StringBuilder is not thread-safe but faster. StringBuffer is thread-safe (synchronized) but slower.

---

### Q2: When should you use StringBuffer over StringBuilder?

**Answer**: Only when the object is shared across multiple threads and needs thread-safety. In 99% of cases, use StringBuilder.

---

### Q3: Why is String concatenation slow?

**Answer**: String is immutable. Each concatenation creates a new String object, causing memory overhead and garbage collection.

---

### Q4: How does StringBuilder grow its capacity?

**Answer**: When capacity is exceeded, it creates a new array with capacity = `(oldCapacity * 2) + 2` and copies existing data.

---

### Q5: Can you convert StringBuilder to StringBuffer?

**Answer**: 
```java
StringBuilder sb = new StringBuilder("Hello");
StringBuffer sbf = new StringBuffer(sb.toString());
```

---

## Key Takeaways

1. **Use StringBuilder** for 99% of cases (single-threaded)
2. **Use StringBuffer** only for shared mutable state across threads
3. **Never use String concatenation** in loops
4. **Set initial capacity** if size is known
5. **StringBuilder is 1.8x faster** than StringBuffer
6. **Both are mutable**, String is immutable
7. **StringBuffer methods are synchronized**, StringBuilder methods are not
8. **Use method chaining** for cleaner code
9. **Call toString()** to get final String
10. **Reuse StringBuilder** in tight loops for better performance

---

## Practice Problems

1. Build a CSV generator using StringBuilder
2. Create a thread-safe logger using StringBuffer
3. Compare performance of String vs StringBuilder for 10,000 concatenations
4. Implement SQL query builder with StringBuilder
5. Write a method to reverse words in a sentence using StringBuilder
