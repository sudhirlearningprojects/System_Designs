# Java 21 Features - Complete Guide

## Table of Contents
1. [Virtual Threads](#virtual-threads)
2. [Pattern Matching for switch](#pattern-matching-for-switch)
3. [Record Patterns](#record-patterns)
4. [Sequenced Collections](#sequenced-collections)
5. [String Templates (Preview)](#string-templates-preview)
6. [Unnamed Patterns and Variables](#unnamed-patterns-and-variables)
7. [Unnamed Classes and Instance Main Methods (Preview)](#unnamed-classes-and-instance-main-methods-preview)
8. [Scoped Values (Preview)](#scoped-values-preview)
9. [Structured Concurrency (Preview)](#structured-concurrency-preview)
10. [Other Features](#other-features)

---

## Virtual Threads

### What are Virtual Threads?

Lightweight threads managed by the JVM, not the OS. Enables **millions of concurrent threads**.

**Traditional Thread:**
- OS-managed (heavyweight)
- Limited to ~thousands
- 1 MB stack memory each

**Virtual Thread:**
- JVM-managed (lightweight)
- Millions possible
- Few KB memory each

### Creating Virtual Threads

```java
// Method 1: Thread.ofVirtual()
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Running in virtual thread");
});

// Method 2: Thread.startVirtualThread()
Thread.startVirtualThread(() -> {
    System.out.println("Virtual thread");
});

// Method 3: Executors
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> {
    System.out.println("Task in virtual thread");
});
```

### Example: Million Threads

```java
// Traditional threads - OutOfMemoryError!
for (int i = 0; i < 1_000_000; i++) {
    new Thread(() -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }).start();
}

// Virtual threads - Works fine!
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1_000_000; i++) {
        executor.submit(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException e) {}
        });
    }
}
```

### Real-World Example: Web Server

```java
// Before: Platform threads (limited concurrency)
ExecutorService executor = Executors.newFixedThreadPool(200);

// After: Virtual threads (unlimited concurrency)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Handle requests
while (true) {
    Socket client = serverSocket.accept();
    executor.submit(() -> handleRequest(client));
}
```

### When to Use

✅ **Use Virtual Threads for:**
- I/O-bound tasks (network, database, file)
- High concurrency requirements
- Blocking operations

❌ **Don't use for:**
- CPU-intensive tasks
- Synchronized blocks (pins carrier thread)
- Native code

---

## Pattern Matching for switch

### Enhanced switch with Pattern Matching

```java
// Before Java 21
Object obj = "Hello";
String result;
if (obj instanceof String) {
    String s = (String) obj;
    result = s.toUpperCase();
} else if (obj instanceof Integer) {
    Integer i = (Integer) obj;
    result = String.valueOf(i * 2);
} else {
    result = "Unknown";
}

// After Java 21
Object obj = "Hello";
String result = switch (obj) {
    case String s -> s.toUpperCase();
    case Integer i -> String.valueOf(i * 2);
    case null -> "Null value";
    default -> "Unknown";
};
```

### Type Patterns

```java
static String format(Object obj) {
    return switch (obj) {
        case Integer i -> String.format("int %d", i);
        case Long l -> String.format("long %d", l);
        case Double d -> String.format("double %f", d);
        case String s -> String.format("String %s", s);
        default -> obj.toString();
    };
}
```

### Guarded Patterns

```java
static String classify(int number) {
    return switch (number) {
        case int i when i < 0 -> "Negative";
        case int i when i == 0 -> "Zero";
        case int i when i > 0 && i < 10 -> "Small positive";
        case int i when i >= 10 -> "Large positive";
        default -> throw new IllegalStateException();
    };
}
```

### Null Handling

```java
static String describe(String s) {
    return switch (s) {
        case null -> "Null";
        case "" -> "Empty";
        case String str when str.length() > 10 -> "Long string";
        default -> "Normal string";
    };
}
```

### Real-World Example

```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height) implements Shape {}

static double area(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Triangle t -> 0.5 * t.base() * t.height();
    };
}
```

---

## Record Patterns

### Deconstructing Records in Patterns

```java
record Point(int x, int y) {}

// Before
static void printPoint(Object obj) {
    if (obj instanceof Point p) {
        int x = p.x();
        int y = p.y();
        System.out.println("x: " + x + ", y: " + y);
    }
}

// After: Record Pattern
static void printPoint(Object obj) {
    if (obj instanceof Point(int x, int y)) {
        System.out.println("x: " + x + ", y: " + y);
    }
}
```

### Nested Record Patterns

```java
record Point(int x, int y) {}
record Rectangle(Point topLeft, Point bottomRight) {}

static void printRectangle(Object obj) {
    if (obj instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
        System.out.println("Rectangle from (" + x1 + "," + y1 + 
                          ") to (" + x2 + "," + y2 + ")");
    }
}
```

### With switch

```java
record Point(int x, int y) {}

static String quadrant(Object obj) {
    return switch (obj) {
        case Point(int x, int y) when x > 0 && y > 0 -> "Q1";
        case Point(int x, int y) when x < 0 && y > 0 -> "Q2";
        case Point(int x, int y) when x < 0 && y < 0 -> "Q3";
        case Point(int x, int y) when x > 0 && y < 0 -> "Q4";
        case Point(int x, int y) -> "Origin or axis";
        default -> "Not a point";
    };
}
```

### Real-World Example: JSON Processing

```java
sealed interface JsonValue {}
record JsonObject(Map<String, JsonValue> fields) implements JsonValue {}
record JsonArray(List<JsonValue> elements) implements JsonValue {}
record JsonString(String value) implements JsonValue {}
record JsonNumber(double value) implements JsonValue {}

static void process(JsonValue json) {
    switch (json) {
        case JsonObject(var fields) -> 
            fields.forEach((k, v) -> System.out.println(k + ": " + v));
        case JsonArray(var elements) -> 
            elements.forEach(System.out::println);
        case JsonString(var value) -> 
            System.out.println("String: " + value);
        case JsonNumber(var value) -> 
            System.out.println("Number: " + value);
    }
}
```

---

## Sequenced Collections

### New Interfaces

```java
interface SequencedCollection<E> extends Collection<E> {
    SequencedCollection<E> reversed();
    void addFirst(E e);
    void addLast(E e);
    E getFirst();
    E getLast();
    E removeFirst();
    E removeLast();
}

interface SequencedSet<E> extends Set<E>, SequencedCollection<E> {
    SequencedSet<E> reversed();
}

interface SequencedMap<K,V> extends Map<K,V> {
    SequencedMap<K,V> reversed();
    SequencedSet<K> sequencedKeySet();
    SequencedCollection<V> sequencedValues();
    SequencedSet<Entry<K,V>> sequencedEntrySet();
    V putFirst(K k, V v);
    V putLast(K k, V v);
    Entry<K,V> firstEntry();
    Entry<K,V> lastEntry();
    Entry<K,V> pollFirstEntry();
    Entry<K,V> pollLastEntry();
}
```

### List Operations

```java
List<String> list = new ArrayList<>();
list.addFirst("A");  // [A]
list.addLast("C");   // [A, C]
list.addFirst("B");  // [B, A, C]

String first = list.getFirst();  // B
String last = list.getLast();    // C

list.removeFirst();  // [A, C]
list.removeLast();   // [A]

// Reversed view
List<String> reversed = list.reversed();
```

### Set Operations

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.addFirst("A");
set.addLast("C");
set.addFirst("B");  // [B, A, C]

String first = set.getFirst();  // B
String last = set.getLast();    // C

// Reversed view
SequencedSet<String> reversed = set.reversed();
```

### Map Operations

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.putFirst("A", 1);
map.putLast("C", 3);
map.putFirst("B", 2);  // {B=2, A=1, C=3}

Map.Entry<String, Integer> first = map.firstEntry();  // B=2
Map.Entry<String, Integer> last = map.lastEntry();    // C=3

map.pollFirstEntry();  // Removes B=2
map.pollLastEntry();   // Removes C=3

// Reversed view
SequencedMap<String, Integer> reversed = map.reversed();
```

### Real-World Example: LRU Cache

```java
class LRUCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> cache;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>();
    }
    
    public V get(K key) {
        V value = cache.remove(key);
        if (value != null) {
            cache.putLast(key, value);  // Move to end (most recent)
        }
        return value;
    }
    
    public void put(K key, V value) {
        cache.remove(key);
        cache.putLast(key, value);
        
        if (cache.size() > capacity) {
            cache.pollFirstEntry();  // Remove oldest
        }
    }
}
```

---

## String Templates (Preview)

### Embedded Expressions in Strings

```java
// Before
String name = "John";
int age = 25;
String message = "Hello, " + name + "! You are " + age + " years old.";

// After: String Template
String message = STR."Hello, \{name}! You are \{age} years old.";
```

### Multi-line Templates

```java
String name = "John";
String html = STR."""
    <html>
        <body>
            <h1>Hello, \{name}!</h1>
        </body>
    </html>
    """;
```

### Expressions in Templates

```java
int x = 10;
int y = 20;
String result = STR."Sum: \{x + y}, Product: \{x * y}";
// "Sum: 30, Product: 200"
```

### Custom Template Processors

```java
// JSON template processor
String json = JSON."""
    {
        "name": "\{name}",
        "age": \{age}
    }
    """;
```

**Note:** This is a preview feature, use `--enable-preview` flag.

---

## Unnamed Patterns and Variables

### Unnamed Patterns (_)

```java
// Before: Unused variable
if (obj instanceof Point p) {
    // Don't use 'p'
}

// After: Unnamed pattern
if (obj instanceof Point _) {
    // Just checking type
}
```

### Unnamed Variables in switch

```java
static String describe(Object obj) {
    return switch (obj) {
        case Integer _ -> "An integer";
        case String _ -> "A string";
        case Point _ -> "A point";
        default -> "Something else";
    };
}
```

### Multiple Unnamed Variables

```java
record Point(int x, int y) {}

// Ignore y coordinate
if (obj instanceof Point(int x, int _)) {
    System.out.println("x: " + x);
}
```

### In Loops

```java
// Before
for (int i = 0; i < 10; i++) {
    // Don't use 'i'
}

// After
for (int _ = 0; _ < 10; _++) {
    // Clearly unused
}
```

---

## Unnamed Classes and Instance Main Methods (Preview)

### Simplified Hello World

```java
// Before
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}

// After: Unnamed class
void main() {
    System.out.println("Hello, World!");
}
```

### Instance Main Method

```java
// No need for static
void main() {
    System.out.println("Hello!");
}

// With arguments
void main(String[] args) {
    System.out.println("Args: " + Arrays.toString(args));
}
```

### Benefits

- Easier for beginners
- Less boilerplate
- Faster prototyping

**Note:** Preview feature, use `--enable-preview` flag.

---

## Scoped Values (Preview)

### Alternative to ThreadLocal

```java
// ThreadLocal (old way)
private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

// Scoped Value (new way)
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
```

### Usage

```java
class UserContext {
    private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
    
    public static void runWithUser(User user, Runnable task) {
        ScopedValue.where(CURRENT_USER, user).run(task);
    }
    
    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }
}

// Usage
User user = new User("John");
UserContext.runWithUser(user, () -> {
    User current = UserContext.getCurrentUser();
    System.out.println(current.name());
});
```

### Benefits over ThreadLocal

- **Immutable** - Cannot be modified after binding
- **Automatic cleanup** - No memory leaks
- **Better performance** - Optimized for virtual threads
- **Inheritance** - Automatically inherited by child threads

---

## Structured Concurrency (Preview)

### Coordinated Concurrent Tasks

```java
// Before: Unstructured
ExecutorService executor = Executors.newCachedThreadPool();
Future<String> future1 = executor.submit(() -> fetchUser());
Future<String> future2 = executor.submit(() -> fetchOrders());

try {
    String user = future1.get();
    String orders = future2.get();
} finally {
    executor.shutdown();
}

// After: Structured Concurrency
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user = scope.fork(() -> fetchUser());
    Future<String> orders = scope.fork(() -> fetchOrders());
    
    scope.join();           // Wait for all
    scope.throwIfFailed();  // Propagate errors
    
    String userData = user.resultNow();
    String orderData = orders.resultNow();
}
```

### ShutdownOnSuccess

```java
// First successful result wins
try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
    scope.fork(() -> fetchFromServer1());
    scope.fork(() -> fetchFromServer2());
    scope.fork(() -> fetchFromServer3());
    
    scope.join();
    String result = scope.result();  // First successful result
}
```

### Real-World Example: Parallel API Calls

```java
record UserData(String name, List<String> orders, String profile) {}

UserData fetchUserData(String userId) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Future<String> name = scope.fork(() -> fetchUserName(userId));
        Future<List<String>> orders = scope.fork(() -> fetchUserOrders(userId));
        Future<String> profile = scope.fork(() -> fetchUserProfile(userId));
        
        scope.join();
        scope.throwIfFailed();
        
        return new UserData(
            name.resultNow(),
            orders.resultNow(),
            profile.resultNow()
        );
    }
}
```

---

## Other Features

### 1. Generational ZGC

Improved garbage collection with generations.

```bash
java -XX:+UseZGC -XX:+ZGenerational MyApp
```

### 2. Key Encapsulation Mechanism API

```java
// Generate key pair
KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
KeyPair kp = kpg.generateKeyPair();

// KEM
KEM kem = KEM.getInstance("DHKEM");
KEM.Encapsulator encapsulator = kem.newEncapsulator(kp.getPublic());
KEM.Encapsulated encapsulated = encapsulator.encapsulate();

byte[] sharedSecret = encapsulated.key().getEncoded();
```

### 3. Deprecations and Removals

- Deprecated: Finalization (for removal)
- Removed: Applet API
- Removed: Security Manager (for removal)

---

## Migration Guide

### From Java 17 to Java 21

```java
// 1. Replace ThreadLocal with ScopedValue
// Before
private static final ThreadLocal<User> USER = new ThreadLocal<>();

// After
private static final ScopedValue<User> USER = ScopedValue.newInstance();

// 2. Use Virtual Threads
// Before
ExecutorService executor = Executors.newFixedThreadPool(100);

// After
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// 3. Use Pattern Matching for switch
// Before
if (obj instanceof String) {
    String s = (String) obj;
    // use s
}

// After
switch (obj) {
    case String s -> // use s
    default -> // handle other cases
}

// 4. Use Sequenced Collections
// Before
LinkedList<String> list = new LinkedList<>();
list.addFirst("A");

// After (more explicit)
List<String> list = new ArrayList<>();
list.addFirst("A");  // Now available on List interface
```

---

## Performance Improvements

### Virtual Threads Benchmark

```java
// Traditional threads: ~10,000 concurrent connections
// Virtual threads: ~1,000,000+ concurrent connections

// Memory usage
// Traditional: 10,000 threads × 1 MB = 10 GB
// Virtual: 1,000,000 threads × 10 KB = 10 GB (same memory, 100x more threads)
```

### Generational ZGC

- Reduced pause times
- Better throughput
- Lower memory overhead

---

## Summary

### Major Features

1. **Virtual Threads** - Millions of lightweight threads
2. **Pattern Matching for switch** - Enhanced switch expressions
3. **Record Patterns** - Deconstruct records in patterns
4. **Sequenced Collections** - First/last element access
5. **String Templates** - Embedded expressions (preview)
6. **Structured Concurrency** - Coordinated tasks (preview)

### Preview Features

- String Templates
- Unnamed Classes and Instance Main
- Scoped Values
- Structured Concurrency

### Impact

- **Massive scalability** with virtual threads
- **Cleaner code** with pattern matching
- **Better APIs** with sequenced collections
- **Easier concurrency** with structured concurrency

---

**Release Date**: September 19, 2023  
**LTS Version**: Yes  
**Support Until**: September 2031  
**Preview Features**: Use `--enable-preview` flag

---

## Getting Started

```bash
# Download Java 21
# https://jdk.java.net/21/

# Verify installation
java -version

# Run with preview features
java --enable-preview MyApp.java

# Compile with preview features
javac --enable-preview --release 21 MyApp.java
```

---

## References

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [JEP 440: Record Patterns](https://openjdk.org/jeps/440)
- [JEP 431: Sequenced Collections](https://openjdk.org/jeps/431)
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
