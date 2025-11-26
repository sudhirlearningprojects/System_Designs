# Java 8 Features - Complete Guide

## Table of Contents
1. [Lambda Expressions](#lambda-expressions)
2. [Functional Interfaces](#functional-interfaces)
3. [Stream API](#stream-api)
4. [Optional Class](#optional-class)
5. [Default Methods](#default-methods)
6. [Method References](#method-references)
7. [Date and Time API](#date-and-time-api)
8. [Nashorn JavaScript Engine](#nashorn-javascript-engine)
9. [CompletableFuture](#completablefuture)
10. [Other Features](#other-features)

---

## Lambda Expressions

### What are Lambda Expressions?

Anonymous functions that can be passed as arguments or stored in variables.

**Syntax:**
```java
(parameters) -> expression
(parameters) -> { statements; }
```

### Before Java 8

```java
// Anonymous inner class
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello World");
    }
};
```

### After Java 8

```java
// Lambda expression
Runnable r = () -> System.out.println("Hello World");
```

### Examples

```java
// No parameters
() -> System.out.println("Hello")

// One parameter (parentheses optional)
x -> x * x
(x) -> x * x

// Multiple parameters
(x, y) -> x + y

// Multiple statements
(x, y) -> {
    int sum = x + y;
    return sum;
}

// With type declaration
(int x, int y) -> x + y
```

### Real-World Examples

```java
// Sorting with lambda
List<String> names = Arrays.asList("John", "Jane", "Bob");
Collections.sort(names, (a, b) -> a.compareTo(b));

// Thread creation
new Thread(() -> System.out.println("Running in thread")).start();

// Event handling
button.addActionListener(e -> System.out.println("Button clicked"));

// Filtering
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
numbers.stream()
       .filter(n -> n % 2 == 0)
       .forEach(System.out::println);
```

---

## Functional Interfaces

### What is a Functional Interface?

Interface with **exactly one abstract method** (SAM - Single Abstract Method).

**Annotation:**
```java
@FunctionalInterface
public interface MyFunction {
    void execute();
}
```

### Built-in Functional Interfaces

#### 1. Predicate<T>

Tests a condition, returns boolean.

```java
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
}

// Usage
Predicate<Integer> isEven = n -> n % 2 == 0;
System.out.println(isEven.test(4));  // true
System.out.println(isEven.test(5));  // false

// Real example
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
numbers.stream()
       .filter(n -> n > 2)
       .forEach(System.out::println);  // 3, 4, 5
```

#### 2. Function<T, R>

Takes input T, returns output R.

```java
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}

// Usage
Function<String, Integer> length = s -> s.length();
System.out.println(length.apply("Hello"));  // 5

// Real example
List<String> names = Arrays.asList("John", "Jane", "Bob");
names.stream()
     .map(name -> name.toUpperCase())
     .forEach(System.out::println);  // JOHN, JANE, BOB
```

#### 3. Consumer<T>

Takes input T, returns nothing.

```java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}

// Usage
Consumer<String> printer = s -> System.out.println(s);
printer.accept("Hello");  // Hello

// Real example
List<String> names = Arrays.asList("John", "Jane", "Bob");
names.forEach(name -> System.out.println("Hello " + name));
```

#### 4. Supplier<T>

Takes no input, returns T.

```java
@FunctionalInterface
public interface Supplier<T> {
    T get();
}

// Usage
Supplier<Double> randomValue = () -> Math.random();
System.out.println(randomValue.get());  // 0.123456

// Real example
Supplier<String> uuidSupplier = () -> UUID.randomUUID().toString();
System.out.println(uuidSupplier.get());
```

#### 5. BiFunction<T, U, R>

Takes two inputs, returns output.

```java
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}

// Usage
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
System.out.println(add.apply(5, 3));  // 8

// Real example
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.merge("A", 2, (oldVal, newVal) -> oldVal + newVal);
System.out.println(map.get("A"));  // 3
```

---

## Stream API

### What is Stream API?

Process collections in a functional style with operations like filter, map, reduce.

**Not a data structure** - it's a pipeline of operations.

### Creating Streams

```java
// From collection
List<String> list = Arrays.asList("A", "B", "C");
Stream<String> stream1 = list.stream();

// From array
String[] array = {"A", "B", "C"};
Stream<String> stream2 = Arrays.stream(array);

// Using Stream.of()
Stream<String> stream3 = Stream.of("A", "B", "C");

// Infinite stream
Stream<Integer> stream4 = Stream.iterate(0, n -> n + 1);

// Generate
Stream<Double> stream5 = Stream.generate(Math::random);
```

### Intermediate Operations

**Return a new stream** - can be chained.

#### filter()

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
numbers.stream()
       .filter(n -> n % 2 == 0)
       .forEach(System.out::println);  // 2, 4, 6
```

#### map()

```java
List<String> names = Arrays.asList("john", "jane", "bob");
names.stream()
     .map(String::toUpperCase)
     .forEach(System.out::println);  // JOHN, JANE, BOB
```

#### flatMap()

```java
List<List<Integer>> nested = Arrays.asList(
    Arrays.asList(1, 2),
    Arrays.asList(3, 4),
    Arrays.asList(5, 6)
);

nested.stream()
      .flatMap(List::stream)
      .forEach(System.out::println);  // 1, 2, 3, 4, 5, 6
```

#### distinct()

```java
List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 3, 4);
numbers.stream()
       .distinct()
       .forEach(System.out::println);  // 1, 2, 3, 4
```

#### sorted()

```java
List<String> names = Arrays.asList("John", "Alice", "Bob");
names.stream()
     .sorted()
     .forEach(System.out::println);  // Alice, Bob, John

// Custom comparator
names.stream()
     .sorted((a, b) -> b.compareTo(a))
     .forEach(System.out::println);  // John, Bob, Alice
```

#### limit() and skip()

```java
Stream.iterate(1, n -> n + 1)
      .limit(5)
      .forEach(System.out::println);  // 1, 2, 3, 4, 5

List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
numbers.stream()
       .skip(2)
       .forEach(System.out::println);  // 3, 4, 5
```

### Terminal Operations

**Produce a result** - ends the stream.

#### forEach()

```java
List<String> names = Arrays.asList("John", "Jane", "Bob");
names.stream().forEach(System.out::println);
```

#### collect()

```java
List<String> names = Arrays.asList("John", "Jane", "Bob");

// To List
List<String> upperNames = names.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());

// To Set
Set<String> nameSet = names.stream()
    .collect(Collectors.toSet());

// To Map
Map<String, Integer> nameLength = names.stream()
    .collect(Collectors.toMap(
        name -> name,
        name -> name.length()
    ));

// Joining
String joined = names.stream()
    .collect(Collectors.joining(", "));  // "John, Jane, Bob"
```

#### reduce()

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// Sum
int sum = numbers.stream()
    .reduce(0, (a, b) -> a + b);  // 15

// Product
int product = numbers.stream()
    .reduce(1, (a, b) -> a * b);  // 120

// Max
Optional<Integer> max = numbers.stream()
    .reduce(Integer::max);  // 5
```

#### count()

```java
long count = Stream.of("A", "B", "C").count();  // 3
```

#### anyMatch(), allMatch(), noneMatch()

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

boolean hasEven = numbers.stream().anyMatch(n -> n % 2 == 0);  // true
boolean allEven = numbers.stream().allMatch(n -> n % 2 == 0);  // false
boolean noneNegative = numbers.stream().noneMatch(n -> n < 0); // true
```

#### findFirst(), findAny()

```java
Optional<Integer> first = Stream.of(1, 2, 3).findFirst();  // 1
Optional<Integer> any = Stream.of(1, 2, 3).findAny();      // 1 (or any)
```

### Real-World Examples

#### Example 1: Filter and Transform

```java
class Employee {
    String name;
    int salary;
    String department;
    
    // Constructor, getters
}

List<Employee> employees = Arrays.asList(
    new Employee("John", 50000, "IT"),
    new Employee("Jane", 60000, "HR"),
    new Employee("Bob", 55000, "IT")
);

// Get names of IT employees with salary > 50000
List<String> itEmployees = employees.stream()
    .filter(e -> e.getDepartment().equals("IT"))
    .filter(e -> e.getSalary() > 50000)
    .map(Employee::getName)
    .collect(Collectors.toList());
```

#### Example 2: Grouping

```java
// Group employees by department
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));

// Count by department
Map<String, Long> countByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

#### Example 3: Statistics

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

IntSummaryStatistics stats = numbers.stream()
    .mapToInt(Integer::intValue)
    .summaryStatistics();

System.out.println("Count: " + stats.getCount());    // 5
System.out.println("Sum: " + stats.getSum());        // 15
System.out.println("Min: " + stats.getMin());        // 1
System.out.println("Max: " + stats.getMax());        // 5
System.out.println("Average: " + stats.getAverage()); // 3.0
```

---

## Optional Class

### What is Optional?

Container object that may or may not contain a value. Helps avoid NullPointerException.

### Creating Optional

```java
// Empty optional
Optional<String> empty = Optional.empty();

// Optional with value
Optional<String> opt = Optional.of("Hello");

// Optional with nullable value
Optional<String> nullable = Optional.ofNullable(null);
```

### Methods

```java
Optional<String> opt = Optional.of("Hello");

// isPresent()
if (opt.isPresent()) {
    System.out.println(opt.get());
}

// ifPresent()
opt.ifPresent(value -> System.out.println(value));

// orElse()
String value = opt.orElse("Default");

// orElseGet()
String value2 = opt.orElseGet(() -> "Default");

// orElseThrow()
String value3 = opt.orElseThrow(() -> new RuntimeException("Not found"));

// map()
Optional<Integer> length = opt.map(String::length);

// filter()
Optional<String> filtered = opt.filter(s -> s.length() > 3);
```

### Real-World Examples

#### Before Java 8

```java
public String getUserCity(User user) {
    if (user != null) {
        Address address = user.getAddress();
        if (address != null) {
            City city = address.getCity();
            if (city != null) {
                return city.getName();
            }
        }
    }
    return "Unknown";
}
```

#### After Java 8

```java
public String getUserCity(User user) {
    return Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .map(City::getName)
        .orElse("Unknown");
}
```

#### Repository Pattern

```java
public interface UserRepository {
    Optional<User> findById(Long id);
}

// Usage
Optional<User> user = userRepository.findById(1L);
user.ifPresent(u -> System.out.println(u.getName()));

// Or with orElseThrow
User user = userRepository.findById(1L)
    .orElseThrow(() -> new UserNotFoundException("User not found"));
```

---

## Default Methods

### What are Default Methods?

Methods with implementation in interfaces.

### Syntax

```java
public interface Vehicle {
    // Abstract method
    void start();
    
    // Default method
    default void stop() {
        System.out.println("Vehicle stopped");
    }
}
```

### Example

```java
interface Vehicle {
    void start();
    
    default void honk() {
        System.out.println("Beep beep!");
    }
}

class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }
    
    // Can override default method
    @Override
    public void honk() {
        System.out.println("Car horn!");
    }
}

// Usage
Car car = new Car();
car.start();  // Car started
car.honk();   // Car horn!
```

### Multiple Inheritance

```java
interface A {
    default void hello() {
        System.out.println("Hello from A");
    }
}

interface B {
    default void hello() {
        System.out.println("Hello from B");
    }
}

class C implements A, B {
    // Must override to resolve conflict
    @Override
    public void hello() {
        A.super.hello();  // Call A's implementation
        // Or B.super.hello();
        // Or custom implementation
    }
}
```

---

## Method References

### What are Method References?

Shorthand for lambda expressions that call a single method.

**Syntax:** `ClassName::methodName`

### Types

#### 1. Static Method Reference

```java
// Lambda
Function<String, Integer> parser = s -> Integer.parseInt(s);

// Method reference
Function<String, Integer> parser = Integer::parseInt;

// Usage
List<String> numbers = Arrays.asList("1", "2", "3");
numbers.stream()
       .map(Integer::parseInt)
       .forEach(System.out::println);
```

#### 2. Instance Method Reference

```java
// Lambda
Consumer<String> printer = s -> System.out.println(s);

// Method reference
Consumer<String> printer = System.out::println;

// Usage
List<String> names = Arrays.asList("John", "Jane", "Bob");
names.forEach(System.out::println);
```

#### 3. Instance Method of Arbitrary Object

```java
// Lambda
Function<String, String> upper = s -> s.toUpperCase();

// Method reference
Function<String, String> upper = String::toUpperCase;

// Usage
List<String> names = Arrays.asList("john", "jane", "bob");
names.stream()
     .map(String::toUpperCase)
     .forEach(System.out::println);
```

#### 4. Constructor Reference

```java
// Lambda
Supplier<List<String>> listSupplier = () -> new ArrayList<>();

// Method reference
Supplier<List<String>> listSupplier = ArrayList::new;

// Usage
List<String> names = Arrays.asList("John", "Jane", "Bob");
List<String> upperNames = names.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toCollection(ArrayList::new));
```

---

## Date and Time API

### Problems with Old API

```java
// Old API (java.util.Date, java.util.Calendar)
Date date = new Date();  // Mutable, not thread-safe
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  // Not thread-safe
```

### New API (java.time)

#### LocalDate

```java
// Current date
LocalDate today = LocalDate.now();  // 2024-01-15

// Specific date
LocalDate date = LocalDate.of(2024, 1, 15);
LocalDate date2 = LocalDate.parse("2024-01-15");

// Operations
LocalDate tomorrow = today.plusDays(1);
LocalDate nextWeek = today.plusWeeks(1);
LocalDate nextMonth = today.plusMonths(1);
LocalDate nextYear = today.plusYears(1);

// Get components
int year = today.getYear();
int month = today.getMonthValue();
int day = today.getDayOfMonth();
DayOfWeek dayOfWeek = today.getDayOfWeek();
```

#### LocalTime

```java
// Current time
LocalTime now = LocalTime.now();  // 14:30:45

// Specific time
LocalTime time = LocalTime.of(14, 30, 45);
LocalTime time2 = LocalTime.parse("14:30:45");

// Operations
LocalTime later = now.plusHours(2);
LocalTime earlier = now.minusMinutes(30);

// Get components
int hour = now.getHour();
int minute = now.getMinute();
int second = now.getSecond();
```

#### LocalDateTime

```java
// Current date-time
LocalDateTime now = LocalDateTime.now();

// Specific date-time
LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
LocalDateTime dt2 = LocalDateTime.parse("2024-01-15T14:30:45");

// Operations
LocalDateTime future = now.plusDays(1).plusHours(2);
```

#### ZonedDateTime

```java
// Current date-time with timezone
ZonedDateTime now = ZonedDateTime.now();

// Specific timezone
ZonedDateTime nyTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
ZonedDateTime tokyoTime = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));

// Convert between timezones
ZonedDateTime converted = nyTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
```

#### Period and Duration

```java
// Period (date-based)
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end = LocalDate.of(2024, 12, 31);
Period period = Period.between(start, end);
System.out.println(period.getMonths());  // 11

// Duration (time-based)
LocalTime startTime = LocalTime.of(9, 0);
LocalTime endTime = LocalTime.of(17, 30);
Duration duration = Duration.between(startTime, endTime);
System.out.println(duration.toHours());  // 8
```

#### Formatting

```java
LocalDateTime now = LocalDateTime.now();

// Format
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = now.format(formatter);  // "2024-01-15 14:30:45"

// Parse
LocalDateTime parsed = LocalDateTime.parse("2024-01-15 14:30:45", formatter);
```

---

## Nashorn JavaScript Engine

### Execute JavaScript from Java

```java
ScriptEngineManager manager = new ScriptEngineManager();
ScriptEngine engine = manager.getEngineByName("nashorn");

// Execute JavaScript
engine.eval("print('Hello from JavaScript')");

// Get result
Object result = engine.eval("10 + 20");
System.out.println(result);  // 30

// Call JavaScript function
engine.eval("function add(a, b) { return a + b; }");
Invocable invocable = (Invocable) engine;
Object sum = invocable.invokeFunction("add", 5, 3);
System.out.println(sum);  // 8
```

**Note:** Nashorn is deprecated in Java 11 and removed in Java 15.

---

## CompletableFuture

### Asynchronous Programming

```java
// Simple async task
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Long running task
    return "Result";
});

// Get result (blocking)
String result = future.get();

// Non-blocking callback
future.thenAccept(result -> System.out.println(result));
```

### Chaining

```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(String::toUpperCase)
    .thenAccept(System.out::println);  // HELLO WORLD
```

### Combining

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

// Combine both
CompletableFuture<String> combined = future1.thenCombine(future2, (s1, s2) -> s1 + " " + s2);
System.out.println(combined.get());  // Hello World
```

### Exception Handling

```java
CompletableFuture.supplyAsync(() -> {
    if (true) throw new RuntimeException("Error");
    return "Success";
})
.exceptionally(ex -> "Recovered from: " + ex.getMessage())
.thenAccept(System.out::println);  // Recovered from: Error
```

---

## Other Features

### 1. forEach() in Iterable

```java
List<String> names = Arrays.asList("John", "Jane", "Bob");
names.forEach(name -> System.out.println(name));
```

### 2. Base64 Encoding/Decoding

```java
// Encode
String original = "Hello World";
String encoded = Base64.getEncoder().encodeToString(original.getBytes());

// Decode
byte[] decoded = Base64.getDecoder().decode(encoded);
String decodedStr = new String(decoded);
```

### 3. Parallel Array Sorting

```java
int[] numbers = {5, 3, 8, 1, 9, 2};
Arrays.parallelSort(numbers);
System.out.println(Arrays.toString(numbers));  // [1, 2, 3, 5, 8, 9]
```

### 4. StringJoiner

```java
StringJoiner joiner = new StringJoiner(", ", "[", "]");
joiner.add("John").add("Jane").add("Bob");
System.out.println(joiner.toString());  // [John, Jane, Bob]
```

### 5. Collectors

```java
List<String> names = Arrays.asList("John", "Jane", "Bob");

// Joining
String joined = names.stream().collect(Collectors.joining(", "));

// Grouping
Map<Integer, List<String>> byLength = names.stream()
    .collect(Collectors.groupingBy(String::length));

// Partitioning
Map<Boolean, List<String>> partitioned = names.stream()
    .collect(Collectors.partitioningBy(s -> s.length() > 3));
```

---

## Summary

### Key Features

1. **Lambda Expressions** - Concise anonymous functions
2. **Stream API** - Functional-style operations on collections
3. **Optional** - Avoid NullPointerException
4. **Default Methods** - Interface methods with implementation
5. **Method References** - Shorthand for lambdas
6. **Date/Time API** - Modern, immutable date-time handling
7. **CompletableFuture** - Asynchronous programming
8. **Functional Interfaces** - Predicate, Function, Consumer, Supplier

### Impact

- **More concise code** with lambdas
- **Better performance** with parallel streams
- **Safer code** with Optional
- **Easier async programming** with CompletableFuture
- **Better date handling** with java.time

---

**Release Date**: March 18, 2014  
**LTS Version**: Yes  
**End of Support**: December 2030
