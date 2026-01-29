# Thread-Safe Singleton Pattern - Deep Dive

## Overview
Singleton pattern ensures a class has only one instance and provides a global point of access to it. In multi-threaded environments, special care must be taken to ensure thread safety.

**Key Characteristics**:
- Only one instance exists throughout application lifecycle
- Global access point via static method
- Lazy or eager initialization
- Thread-safe in concurrent environments
- Prevents multiple instantiation

---

## Problem: Why Thread Safety Matters

### Non-Thread-Safe Singleton (Broken)

```java
public class Singleton {
    private static Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {  // ← Race condition!
            instance = new Singleton();
        }
        return instance;
    }
}
```

**Problem**:
```
Thread 1: Checks instance == null → TRUE
Thread 2: Checks instance == null → TRUE (before Thread 1 creates instance)
Thread 1: Creates new instance
Thread 2: Creates another new instance ← TWO INSTANCES!
```

---

## Implementation 1: Eager Initialization (Thread-Safe)

### Code

```java
public class EagerSingleton {
    // Instance created at class loading time
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    
    private EagerSingleton() {}
    
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

### Pros & Cons

**Pros**:
- ✅ Thread-safe (JVM guarantees thread safety during class loading)
- ✅ Simple implementation
- ✅ No synchronization overhead

**Cons**:
- ❌ Instance created even if never used (memory waste)
- ❌ No lazy initialization
- ❌ Cannot handle exceptions during initialization

**Use Case**: When instance is lightweight and always needed

---

## Implementation 2: Synchronized Method (Thread-Safe but Slow)

### Code

```java
public class SynchronizedSingleton {
    private static SynchronizedSingleton instance;
    
    private SynchronizedSingleton() {}
    
    public static synchronized SynchronizedSingleton getInstance() {
        if (instance == null) {
            instance = new SynchronizedSingleton();
        }
        return instance;
    }
}
```

### Pros & Cons

**Pros**:
- ✅ Thread-safe
- ✅ Lazy initialization
- ✅ Simple to understand

**Cons**:
- ❌ Synchronized on every call (performance bottleneck)
- ❌ Only first call needs synchronization, rest are wasteful

**Performance**:
```
First call:  100ms (synchronized + creation)
Second call: 10ms  (synchronized, no creation) ← Unnecessary overhead
Third call:  10ms  (synchronized, no creation) ← Unnecessary overhead
```

**Use Case**: Low-concurrency applications where performance isn't critical

---

## Implementation 3: Double-Checked Locking (Recommended)

### Code

```java
public class DoubleCheckedSingleton {
    // volatile ensures visibility across threads
    private static volatile DoubleCheckedSingleton instance;
    
    private DoubleCheckedSingleton() {}
    
    public static DoubleCheckedSingleton getInstance() {
        if (instance == null) {  // First check (no locking)
            synchronized (DoubleCheckedSingleton.class) {
                if (instance == null) {  // Second check (with locking)
                    instance = new DoubleCheckedSingleton();
                }
            }
        }
        return instance;
    }
}
```

### Why Double-Checked?

```
Thread 1: instance == null? YES → Enter synchronized block
Thread 2: instance == null? YES → Wait for lock
Thread 1: instance == null? YES (inside lock) → Create instance
Thread 1: Exit synchronized block
Thread 2: Acquire lock
Thread 2: instance == null? NO (second check) → Return existing instance
Thread 3: instance == null? NO (first check) → Return immediately (no lock!)
```

### Why volatile?

Without `volatile`, instruction reordering can cause issues:

```java
// instance = new Singleton() is actually 3 steps:
1. Allocate memory
2. Initialize object
3. Assign reference to instance

// Without volatile, JVM can reorder:
1. Allocate memory
3. Assign reference to instance ← instance is not null but not initialized!
2. Initialize object

// Another thread sees instance != null but object is not fully initialized!
```

### Pros & Cons

**Pros**:
- ✅ Thread-safe
- ✅ Lazy initialization
- ✅ High performance (synchronization only on first call)
- ✅ Industry standard

**Cons**:
- ❌ Verbose
- ❌ Requires volatile (Java 5+)

**Use Case**: Production applications requiring lazy initialization and high performance

---

## Implementation 4: Bill Pugh Singleton (Best Practice)

### Code

```java
public class BillPughSingleton {
    
    private BillPughSingleton() {}
    
    // Static inner class - loaded only when getInstance() is called
    private static class SingletonHelper {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }
    
    public static BillPughSingleton getInstance() {
        return SingletonHelper.INSTANCE;
    }
}
```

### How It Works

```
1. BillPughSingleton class loaded → SingletonHelper NOT loaded
2. getInstance() called → SingletonHelper loaded
3. JVM guarantees thread-safe initialization of static fields
4. INSTANCE created (lazy + thread-safe)
```

### Pros & Cons

**Pros**:
- ✅ Thread-safe (JVM guarantees)
- ✅ Lazy initialization
- ✅ No synchronization overhead
- ✅ Clean and simple
- ✅ Best practice recommended by Joshua Bloch

**Cons**:
- ❌ None (this is the preferred approach)

**Use Case**: Default choice for Singleton pattern in Java

---

## Implementation 5: Enum Singleton (Serialization-Safe)

### Code

```java
public enum EnumSingleton {
    INSTANCE;
    
    private String data;
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getData() {
        return data;
    }
    
    public void doSomething() {
        System.out.println("Enum Singleton: " + data);
    }
}
```

### Usage

```java
public class Test {
    public static void main(String[] args) {
        EnumSingleton singleton = EnumSingleton.INSTANCE;
        singleton.setData("Hello World");
        singleton.doSomething();
        
        EnumSingleton another = EnumSingleton.INSTANCE;
        System.out.println(singleton == another); // true
    }
}
```

### Pros & Cons

**Pros**:
- ✅ Thread-safe (JVM guarantees)
- ✅ Serialization-safe (JVM handles)
- ✅ Reflection-proof
- ✅ Simplest implementation
- ✅ Recommended by Joshua Bloch (Effective Java)

**Cons**:
- ❌ Cannot extend other classes (enums can't extend)
- ❌ No lazy initialization
- ❌ Less flexible

**Use Case**: When serialization safety is critical (e.g., distributed systems)

---

## Breaking Singleton Pattern

### 1. Reflection Attack

```java
public class ReflectionAttack {
    public static void main(String[] args) throws Exception {
        BillPughSingleton instance1 = BillPughSingleton.getInstance();
        
        // Use reflection to create another instance
        Constructor<BillPughSingleton> constructor = 
            BillPughSingleton.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        BillPughSingleton instance2 = constructor.newInstance();
        
        System.out.println(instance1 == instance2); // false - BROKEN!
    }
}
```

**Solution**: Throw exception in constructor

```java
public class ReflectionProofSingleton {
    private static volatile ReflectionProofSingleton instance;
    
    private ReflectionProofSingleton() {
        if (instance != null) {
            throw new IllegalStateException("Instance already exists!");
        }
    }
    
    public static ReflectionProofSingleton getInstance() {
        if (instance == null) {
            synchronized (ReflectionProofSingleton.class) {
                if (instance == null) {
                    instance = new ReflectionProofSingleton();
                }
            }
        }
        return instance;
    }
}
```

### 2. Serialization Attack

```java
public class SerializationAttack {
    public static void main(String[] args) throws Exception {
        BillPughSingleton instance1 = BillPughSingleton.getInstance();
        
        // Serialize
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("singleton.ser"));
        out.writeObject(instance1);
        out.close();
        
        // Deserialize
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("singleton.ser"));
        BillPughSingleton instance2 = (BillPughSingleton) in.readObject();
        in.close();
        
        System.out.println(instance1 == instance2); // false - BROKEN!
    }
}
```

**Solution**: Implement readResolve()

```java
public class SerializationSafeSingleton implements Serializable {
    private static volatile SerializationSafeSingleton instance;
    
    private SerializationSafeSingleton() {}
    
    public static SerializationSafeSingleton getInstance() {
        if (instance == null) {
            synchronized (SerializationSafeSingleton.class) {
                if (instance == null) {
                    instance = new SerializationSafeSingleton();
                }
            }
        }
        return instance;
    }
    
    // Prevent creating new instance during deserialization
    protected Object readResolve() {
        return getInstance();
    }
}
```

**Best Solution**: Use Enum (automatically serialization-safe)

---

## Comparison Table

| Implementation | Thread-Safe | Lazy Init | Performance | Serialization-Safe | Reflection-Proof |
|---------------|-------------|-----------|-------------|-------------------|------------------|
| **Eager** | ✅ | ❌ | ⭐⭐⭐ | ❌ | ❌ |
| **Synchronized** | ✅ | ✅ | ⭐ | ❌ | ❌ |
| **Double-Checked** | ✅ | ✅ | ⭐⭐⭐ | ❌ | ❌ |
| **Bill Pugh** | ✅ | ✅ | ⭐⭐⭐ | ❌ | ❌ |
| **Enum** | ✅ | ❌ | ⭐⭐⭐ | ✅ | ✅ |

---

## Real-World Examples

### 1. Database Connection Pool

```java
public class DatabaseConnectionPool {
    private static volatile DatabaseConnectionPool instance;
    private HikariDataSource dataSource;
    
    private DatabaseConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        config.setUsername("user");
        config.setPassword("password");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }
    
    public static DatabaseConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
```

### 2. Logger

```java
public class Logger {
    private static volatile Logger instance;
    private PrintWriter writer;
    
    private Logger() {
        try {
            writer = new PrintWriter(new FileWriter("app.log", true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize logger", e);
        }
    }
    
    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }
    
    public synchronized void log(String message) {
        writer.println(LocalDateTime.now() + ": " + message);
        writer.flush();
    }
}
```

### 3. Configuration Manager

```java
public class ConfigurationManager {
    
    private ConfigurationManager() {}
    
    private static class SingletonHelper {
        private static final ConfigurationManager INSTANCE = new ConfigurationManager();
    }
    
    public static ConfigurationManager getInstance() {
        return SingletonHelper.INSTANCE;
    }
    
    private Properties properties = new Properties();
    
    public void loadConfig(String filePath) throws IOException {
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        }
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
```

### 4. Cache Manager

```java
public class CacheManager {
    private static volatile CacheManager instance;
    private Map<String, Object> cache;
    
    private CacheManager() {
        cache = new ConcurrentHashMap<>();
    }
    
    public static CacheManager getInstance() {
        if (instance == null) {
            synchronized (CacheManager.class) {
                if (instance == null) {
                    instance = new CacheManager();
                }
            }
        }
        return instance;
    }
    
    public void put(String key, Object value) {
        cache.put(key, value);
    }
    
    public Object get(String key) {
        return cache.get(key);
    }
    
    public void clear() {
        cache.clear();
    }
}
```

---

## Interview Questions & Answers

### Q1: Why is the constructor private?

**Answer**: To prevent external instantiation using `new` keyword. Only the class itself can create an instance.

### Q2: What happens if two threads call getInstance() simultaneously?

**Answer**: 
- **Eager/Enum**: No issue, instance already created
- **Synchronized**: Second thread waits for lock
- **Double-Checked**: First check fails for both, but synchronized block ensures only one creates instance
- **Bill Pugh**: JVM guarantees thread-safe class loading

### Q3: Why use volatile in double-checked locking?

**Answer**: Prevents instruction reordering. Without volatile, another thread might see partially constructed object.

### Q4: Can we clone a Singleton?

**Answer**: No, override clone() to throw exception:

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException("Singleton cannot be cloned");
}
```

### Q5: Which Singleton implementation is best?

**Answer**:
- **General use**: Bill Pugh (lazy + thread-safe + simple)
- **Serialization needed**: Enum
- **Always needed**: Eager initialization

### Q6: Is Singleton an anti-pattern?

**Answer**: Debatable. Issues:
- Global state (hard to test)
- Tight coupling
- Violates Single Responsibility Principle

Alternatives: Dependency Injection (Spring @Bean with singleton scope)

---

## Testing Singleton

### Unit Test

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SingletonTest {
    
    @Test
    void testSingleInstance() {
        BillPughSingleton instance1 = BillPughSingleton.getInstance();
        BillPughSingleton instance2 = BillPughSingleton.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    @Test
    void testThreadSafety() throws InterruptedException {
        Set<BillPughSingleton> instances = ConcurrentHashMap.newKeySet();
        
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> instances.add(BillPughSingleton.getInstance()));
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        assertEquals(1, instances.size());
    }
}
```

---

## Spring Framework Alternative

Instead of manual Singleton, use Spring:

```java
@Component
public class MyService {
    // Spring manages as singleton by default
}

// Or explicit:
@Configuration
public class AppConfig {
    @Bean
    @Scope("singleton")
    public MyService myService() {
        return new MyService();
    }
}
```

**Benefits**:
- Dependency injection
- Easier testing (can mock)
- Lifecycle management
- No global state

---

## Key Takeaways

1. **Bill Pugh Singleton** is the best general-purpose implementation
2. **Enum Singleton** for serialization safety
3. **Always use volatile** with double-checked locking
4. **Prevent reflection/serialization attacks** if security is critical
5. **Consider Dependency Injection** instead of Singleton pattern
6. **Thread safety** is critical in multi-threaded environments
7. **Lazy initialization** saves memory but adds complexity

---

## Practice Problems

1. Implement thread-safe Singleton with lazy initialization
2. Break Singleton using reflection and fix it
3. Make Singleton serialization-safe
4. Compare performance: Synchronized vs Double-Checked Locking
5. Implement Singleton with initialization parameters
6. Create Singleton that can be reset (for testing)
7. Design thread-safe Singleton for database connection pool
8. Implement Singleton with dependency injection
9. Write unit tests to verify thread safety
10. Convert Singleton to Spring Bean
