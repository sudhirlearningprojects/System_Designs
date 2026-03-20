# Java Versions Deep Dive: Major Changes and Features

## Java 8 (March 2014) - LTS

### Lambda Expressions
```java
// Before Java 8
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
Collections.sort(names, new Comparator<String>() {
    public int compare(String a, String b) {
        return a.compareTo(b);
    }
});

// Java 8
names.sort((a, b) -> a.compareTo(b));
```

### Stream API
```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
int sum = numbers.stream()
    .filter(n -> n % 2 == 0)
    .mapToInt(Integer::intValue)
    .sum();
```

### Functional Interfaces
- `@FunctionalInterface` annotation
- Predicate, Function, Consumer, Supplier

### Default Methods in Interfaces
```java
interface Vehicle {
    default void print() {
        System.out.println("I am a vehicle");
    }
}
```

### Optional Class
```java
Optional<String> optional = Optional.ofNullable(getValue());
String result = optional.orElse("default");
```

### Date/Time API (java.time)
```java
LocalDate date = LocalDate.now();
LocalDateTime dateTime = LocalDateTime.now();
ZonedDateTime zonedDateTime = ZonedDateTime.now();
```

### Method References
```java
names.forEach(System.out::println);
```

---

## Java 9 (September 2017)

### Module System (Project Jigsaw)
```java
module com.example.myapp {
    requires java.sql;
    exports com.example.myapp.api;
}
```

### JShell (REPL)
Interactive Java shell for quick testing

### Factory Methods for Collections
```java
List<String> list = List.of("a", "b", "c");
Set<String> set = Set.of("a", "b", "c");
Map<String, Integer> map = Map.of("key1", 1, "key2", 2);
```

### Private Methods in Interfaces
```java
interface MyInterface {
    private void helper() {
        // implementation
    }
}
```

### Stream API Enhancements
```java
Stream.ofNullable(value);
stream.takeWhile(predicate);
stream.dropWhile(predicate);
```

### Process API Improvements
```java
ProcessHandle current = ProcessHandle.current();
long pid = current.pid();
```

---

## Java 10 (March 2018)

### Local Variable Type Inference (var)
```java
var list = new ArrayList<String>();
var map = new HashMap<String, Integer>();
var stream = list.stream();
```

### Unmodifiable Collections
```java
List<String> copy = List.copyOf(originalList);
```

### Optional.orElseThrow()
```java
String value = optional.orElseThrow();
```

---

## Java 11 (September 2018) - LTS

### String Methods
```java
" ".isBlank();
"Java".repeat(3);
"A\nB\nC".lines().forEach(System.out::println);
"  text  ".strip();
```

### File Methods
```java
Path path = Files.writeString(Path.of("file.txt"), "content");
String content = Files.readString(path);
```

### HTTP Client (Standard)
```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com"))
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

### Lambda Parameter Type Inference
```java
(var x, var y) -> x + y
```

### Running Java Files Directly
```bash
java HelloWorld.java
```

---

## Java 12 (March 2019)

### Switch Expressions (Preview)
```java
String result = switch (day) {
    case MONDAY, FRIDAY -> "Work";
    case SATURDAY, SUNDAY -> "Weekend";
    default -> "Midweek";
};
```

### String Methods
```java
"text".indent(4);
"text".transform(String::toUpperCase);
```

### Teeing Collector
```java
double mean = Stream.of(1, 2, 3, 4, 5)
    .collect(Collectors.teeing(
        Collectors.summingDouble(i -> i),
        Collectors.counting(),
        (sum, count) -> sum / count
    ));
```

---

## Java 13 (September 2019)

### Text Blocks (Preview)
```java
String json = """
    {
        "name": "John",
        "age": 30
    }
    """;
```

### Switch Expressions (Second Preview)
```java
int numLetters = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> 6;
    case TUESDAY -> 7;
    default -> 8;
};
```

---

## Java 14 (March 2020)

### Records (Preview)
```java
record Point(int x, int y) {}

Point p = new Point(1, 2);
System.out.println(p.x());
```

### Pattern Matching for instanceof (Preview)
```java
if (obj instanceof String s) {
    System.out.println(s.toUpperCase());
}
```

### Switch Expressions (Standard)
```java
String result = switch (value) {
    case 1 -> "one";
    case 2 -> "two";
    default -> "other";
};
```

### Helpful NullPointerExceptions
```java
// Shows which variable was null
a.b.c.d = 5; // NPE: Cannot read field "c" because "a.b" is null
```

---

## Java 15 (September 2020)

### Text Blocks (Standard)
```java
String html = """
    <html>
        <body>
            <p>Hello</p>
        </body>
    </html>
    """;
```

### Sealed Classes (Preview)
```java
sealed class Shape permits Circle, Rectangle {}
final class Circle extends Shape {}
final class Rectangle extends Shape {}
```

### Hidden Classes
For framework use, not directly accessible

---

## Java 16 (March 2021)

### Records (Standard)
```java
record Person(String name, int age) {
    public Person {
        if (age < 0) throw new IllegalArgumentException();
    }
}
```

### Pattern Matching for instanceof (Standard)
```java
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s.toUpperCase());
}
```

### Stream.toList()
```java
List<String> list = stream.toList();
```

---

## Java 17 (September 2021) - LTS

### Sealed Classes (Standard)
```java
sealed interface Service permits BasicService, PremiumService {}
final class BasicService implements Service {}
final class PremiumService implements Service {}
```

### Pattern Matching for switch (Preview)
```java
String formatted = switch (obj) {
    case Integer i -> String.format("int %d", i);
    case Long l -> String.format("long %d", l);
    case String s -> String.format("String %s", s);
    default -> obj.toString();
};
```

### Enhanced Pseudo-Random Number Generators
```java
RandomGenerator generator = RandomGenerator.of("L64X128MixRandom");
```

---

## Java 18 (March 2022)

### UTF-8 by Default
Default charset is UTF-8

### Simple Web Server
```bash
jwebserver
```

### Code Snippets in Javadoc
```java
/**
 * {@snippet :
 * List<String> list = new ArrayList<>();
 * }
 */
```

### Pattern Matching for switch (Second Preview)
```java
static String formatterPatternSwitch(Object o) {
    return switch (o) {
        case Integer i -> String.format("int %d", i);
        case Long l -> String.format("long %d", l);
        case Double d -> String.format("double %f", d);
        case String s -> String.format("String %s", s);
        default -> o.toString();
    };
}
```

---

## Java 19 (September 2022)

### Virtual Threads (Preview)
```java
Thread.startVirtualThread(() -> {
    System.out.println("Virtual thread");
});

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> System.out.println("Task"));
}
```

### Pattern Matching for switch (Third Preview)
```java
static void testTriangle(Shape s) {
    switch (s) {
        case Triangle t when t.calculateArea() > 100 -> 
            System.out.println("Large triangle");
        case Triangle t -> 
            System.out.println("Small triangle");
        default -> 
            System.out.println("Not a triangle");
    }
}
```

### Record Patterns (Preview)
```java
record Point(int x, int y) {}

if (obj instanceof Point(int x, int y)) {
    System.out.println(x + y);
}
```

---

## Java 20 (March 2023)

### Scoped Values (Incubator)
```java
final static ScopedValue<String> USER = ScopedValue.newInstance();

ScopedValue.where(USER, "admin").run(() -> {
    System.out.println(USER.get());
});
```

### Record Patterns (Second Preview)
```java
record Point(int x, int y) {}
record Rectangle(Point p1, Point p2) {}

if (shape instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
    // Use x1, y1, x2, y2
}
```

### Pattern Matching for switch (Fourth Preview)
Enhanced with when clauses and null handling

---

## Java 21 (September 2023) - LTS

### Virtual Threads (Standard)
```java
Thread thread = Thread.ofVirtual().start(() -> {
    System.out.println("Virtual thread");
});

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));
            return i;
        });
    });
}
```

### Sequenced Collections
```java
interface SequencedCollection<E> extends Collection<E> {
    SequencedCollection<E> reversed();
    void addFirst(E);
    void addLast(E);
    E getFirst();
    E getLast();
    E removeFirst();
    E removeLast();
}

List<String> list = new ArrayList<>();
list.addFirst("first");
list.addLast("last");
```

### Pattern Matching for switch (Standard)
```java
String result = switch (obj) {
    case Integer i -> "Integer: " + i;
    case String s when s.length() > 5 -> "Long string: " + s;
    case String s -> "Short string: " + s;
    case null -> "null value";
    default -> "Unknown";
};
```

### Record Patterns (Standard)
```java
record Point(int x, int y) {}

static void printSum(Object obj) {
    if (obj instanceof Point(int x, int y)) {
        System.out.println(x + y);
    }
}
```

### String Templates (Preview)
```java
String name = "John";
int age = 30;
String message = STR."Hello \{name}, you are \{age} years old";
```

---

## Java 22 (March 2024)

### Unnamed Variables and Patterns
```java
// Unused variables
if (obj instanceof Point(int x, _)) {
    System.out.println(x);
}

// Unused catch parameters
try {
    // code
} catch (Exception _) {
    // handle
}
```

### String Templates (Second Preview)
```java
String json = STR."""
    {
        "name": "\{name}",
        "age": \{age}
    }
    """;
```

### Statements before super() (Preview)
```java
class Child extends Parent {
    Child(int value) {
        if (value < 0) throw new IllegalArgumentException();
        super(value);
    }
}
```

### Foreign Function & Memory API (Preview)
Direct access to native memory and functions

---

## Java 23 (September 2024)

### Primitive Types in Patterns (Preview)
```java
int result = switch (obj) {
    case int i -> i * 2;
    case long l -> (int) l;
    default -> 0;
};
```

### Module Import Declarations (Preview)
```java
import module java.base;
```

### Markdown in Javadoc (Preview)
```java
/// # Heading
/// This is **bold** text
```

### Structured Concurrency (Preview)
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user = scope.fork(() -> findUser());
    Future<Integer> order = scope.fork(() -> fetchOrder());
    
    scope.join();
    scope.throwIfFailed();
    
    return new Response(user.resultNow(), order.resultNow());
}
```

---

## Key LTS Versions Summary

| Version | Release Date | Support Until | Key Features |
|---------|--------------|---------------|--------------|
| Java 8  | March 2014   | 2030          | Lambdas, Streams, Optional, Date/Time API |
| Java 11 | September 2018 | 2026        | HTTP Client, String methods, var in lambdas |
| Java 17 | September 2021 | 2029        | Sealed classes, Pattern matching, Records |
| Java 21 | September 2023 | 2031        | Virtual threads, Sequenced collections, Pattern matching for switch |

---

## Migration Considerations

### Java 8 → 11
- Remove deprecated APIs (e.g., sun.misc.Unsafe usage)
- Update dependencies for module system compatibility
- Review SecurityManager usage (deprecated)

### Java 11 → 17
- Adopt sealed classes for better type hierarchies
- Use pattern matching to simplify instanceof checks
- Leverage records for immutable data carriers

### Java 17 → 21
- Adopt virtual threads for high-concurrency applications
- Use sequenced collections for predictable ordering
- Implement pattern matching in switch for cleaner code

---

## Performance Improvements Across Versions

- **Java 9**: Compact Strings (reduced memory footprint)
- **Java 10**: Application Class-Data Sharing
- **Java 11**: Epsilon GC, ZGC (experimental)
- **Java 12**: Shenandoah GC
- **Java 15**: ZGC and Shenandoah improvements
- **Java 17**: Sealed classes optimization
- **Java 21**: Virtual threads (massive scalability improvement)

---

## Interview Tips

1. **Focus on LTS versions**: Java 8, 11, 17, 21
2. **Understand virtual threads**: Game-changer for concurrent applications
3. **Pattern matching evolution**: Know the progression from instanceof to switch
4. **Records vs Classes**: When to use each
5. **Stream API mastery**: Still heavily used since Java 8
6. **Module system basics**: Understanding JPMS concepts
7. **Deprecations**: Know what's deprecated and alternatives
