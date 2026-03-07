# Java Streams API — Deep Dive

## What is the Streams API?

Introduced in **Java 8**, the Streams API provides a functional-style approach to processing sequences of elements. A stream is **not a data structure** — it is a pipeline of operations over a source (collection, array, I/O channel).

Key properties:
- **Lazy**: Intermediate operations are not executed until a terminal operation is invoked
- **Non-reusable**: A stream can be consumed only once
- **Possibly parallel**: Can switch to parallel processing with `.parallel()`
- **Non-mutating**: Does not modify the source

---

## Stream Pipeline Structure

```
Source → Intermediate Operations (lazy) → Terminal Operation (triggers execution)
```

```java
List<String> result = list.stream()           // source
    .filter(s -> s.startsWith("A"))           // intermediate
    .map(String::toUpperCase)                 // intermediate
    .sorted()                                 // intermediate
    .collect(Collectors.toList());            // terminal
```

---

## Core Interfaces and Classes

| Interface/Class | Description |
|---|---|
| `Stream<T>` | Stream of object references |
| `IntStream` | Primitive int stream (avoids boxing) |
| `LongStream` | Primitive long stream |
| `DoubleStream` | Primitive double stream |
| `BaseStream<T,S>` | Root interface for all streams |
| `Collectors` | Utility class with built-in collectors |
| `Collector<T,A,R>` | Interface for custom collectors |
| `Optional<T>` | Container for possibly-absent terminal results |
| `StreamSupport` | Low-level utility to create streams from Spliterators |
| `Spliterator<T>` | Enables parallel decomposition of a source |

---

## Stream Sources

```java
// From Collection
list.stream()
list.parallelStream()

// From Array
Arrays.stream(arr)
Stream.of("a", "b", "c")

// From range (primitive)
IntStream.range(0, 10)       // 0..9
IntStream.rangeClosed(1, 10) // 1..10

// Infinite streams
Stream.iterate(0, n -> n + 1)
Stream.generate(Math::random)

// From file lines
Files.lines(Path.of("file.txt"))

// From String chars
"hello".chars()  // returns IntStream
```

---

## Intermediate Operations (Lazy)

These return a new Stream and are not executed until a terminal operation is called.

### filter
```java
stream.filter(x -> x > 10)
```
Keeps elements matching the predicate.

### map
```java
stream.map(String::toUpperCase)
```
Transforms each element using a function.

### flatMap
```java
stream.flatMap(list -> list.stream())
```
Flattens nested streams into one stream. Use when each element maps to multiple elements.

### flatMapToInt / flatMapToLong / flatMapToDouble
Primitive specializations of flatMap.

### mapToInt / mapToLong / mapToDouble
```java
stream.mapToInt(String::length)  // returns IntStream
```
Converts to primitive stream — avoids boxing overhead.

### mapToObj
```java
intStream.mapToObj(i -> "item-" + i)  // IntStream → Stream<String>
```

### distinct
```java
stream.distinct()
```
Removes duplicates using `equals()`.

### sorted
```java
stream.sorted()                          // natural order
stream.sorted(Comparator.reverseOrder()) // custom order
```

### peek
```java
stream.peek(System.out::println)
```
Performs an action on each element without consuming the stream. Useful for debugging.

### limit
```java
stream.limit(5)  // first 5 elements
```

### skip
```java
stream.skip(3)   // skip first 3 elements
```

### takeWhile *(Java 9)*
```java
stream.takeWhile(x -> x < 10)
```
Takes elements while predicate is true, stops at first false.

### dropWhile *(Java 9)*
```java
stream.dropWhile(x -> x < 10)
```
Drops elements while predicate is true, returns rest.

### mapMulti *(Java 16)*
```java
stream.mapMulti((element, consumer) -> {
    consumer.accept(element);
    consumer.accept(element.toUpperCase());
})
```
Imperative alternative to flatMap — more efficient for small expansions.

---

## Terminal Operations (Eager — trigger execution)

### collect
```java
stream.collect(Collectors.toList())
stream.collect(Collectors.toSet())
stream.collect(Collectors.toMap(k -> k, String::length))
stream.collect(Collectors.joining(", ", "[", "]"))
stream.collect(Collectors.groupingBy(String::length))
stream.collect(Collectors.partitioningBy(s -> s.length() > 3))
stream.collect(Collectors.counting())
stream.collect(Collectors.summarizingInt(String::length))
stream.collect(Collectors.toUnmodifiableList())  // Java 10
```

### forEach / forEachOrdered
```java
stream.forEach(System.out::println)
stream.forEachOrdered(System.out::println)  // preserves encounter order in parallel
```

### reduce
```java
stream.reduce(0, Integer::sum)                    // with identity
stream.reduce(Integer::sum)                       // returns Optional
stream.reduce(0, Integer::sum, Integer::sum)      // parallel combiner
```

### count
```java
stream.count()  // returns long
```

### findFirst / findAny
```java
stream.findFirst()  // first element, Optional
stream.findAny()    // any element (faster in parallel), Optional
```

### anyMatch / allMatch / noneMatch
```java
stream.anyMatch(x -> x > 5)
stream.allMatch(x -> x > 0)
stream.noneMatch(x -> x < 0)
```

### min / max
```java
stream.min(Comparator.naturalOrder())  // Optional
stream.max(Comparator.comparingInt(String::length))
```

### toArray
```java
stream.toArray()           // Object[]
stream.toArray(String[]::new)  // String[]
```

### iterator / spliterator
Terminal operations that return an iterator/spliterator for external traversal.

### toList *(Java 16)*
```java
stream.toList()  // returns unmodifiable List, shorter than collect(Collectors.toList())
```

---

## Collectors Deep Dive

### groupingBy
```java
// Group by length
Map<Integer, List<String>> byLength = words.stream()
    .collect(Collectors.groupingBy(String::length));

// Group + count
Map<Integer, Long> countByLength = words.stream()
    .collect(Collectors.groupingBy(String::length, Collectors.counting()));

// Group + downstream collector
Map<String, Double> avgSalaryByDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept,
             Collectors.averagingDouble(Employee::getSalary)));
```

### partitioningBy
```java
Map<Boolean, List<Integer>> evenOdd = numbers.stream()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
```

### teeing *(Java 12)*
```java
// Apply two collectors and merge results
stream.collect(Collectors.teeing(
    Collectors.summingInt(Integer::intValue),
    Collectors.counting(),
    (sum, count) -> sum / count  // average
));
```

### Custom Collector
```java
Collector<String, StringBuilder, String> joining =
    Collector.of(StringBuilder::new,
                 StringBuilder::append,
                 StringBuilder::append,
                 StringBuilder::toString);
```

---

## Primitive Streams — IntStream, LongStream, DoubleStream

Avoid boxing/unboxing overhead for numeric operations.

```java
IntStream.range(1, 6).sum()          // 15
IntStream.range(1, 6).average()      // OptionalDouble
IntStream.range(1, 6).summaryStatistics()  // count, sum, min, max, avg

// Convert to object stream
IntStream.range(1, 4).boxed()        // Stream<Integer>
IntStream.range(1, 4).mapToObj(i -> "item-" + i)
```

---

## Optional — Used with Stream Terminals

```java
Optional<String> first = stream.findFirst();
first.isPresent()
first.isEmpty()           // Java 11
first.get()
first.orElse("default")
first.orElseGet(() -> computeDefault())
first.orElseThrow()       // Java 10
first.ifPresent(System.out::println)
first.ifPresentOrElse(   // Java 9
    System.out::println,
    () -> System.out.println("empty")
);
first.map(String::toUpperCase)
first.filter(s -> s.length() > 3)
first.flatMap(s -> Optional.of(s.trim()))
first.stream()            // Java 9 — Optional to Stream (0 or 1 element)
first.or(() -> Optional.of("fallback"))  // Java 9
```

---

## Parallel Streams

```java
list.parallelStream()
list.stream().parallel()
```

**When to use:**
- Large datasets (100K+ elements)
- CPU-bound operations
- No shared mutable state
- Order doesn't matter (or use `forEachOrdered`)

**When NOT to use:**
- Small collections (thread overhead > gain)
- I/O-bound operations
- Operations with side effects
- When order matters and you can't afford `forEachOrdered`

**Thread pool:** Uses `ForkJoinPool.commonPool()` by default.

```java
// Custom thread pool for parallel stream
ForkJoinPool pool = new ForkJoinPool(4);
pool.submit(() -> list.parallelStream().forEach(this::process)).get();
```

---

## Version-wise Upgrades

### Java 8 (Initial Release)
- `Stream<T>`, `IntStream`, `LongStream`, `DoubleStream`
- All core intermediate and terminal operations
- `Collectors` utility class
- `Optional<T>`
- `Stream.iterate()`, `Stream.generate()`

### Java 9
- `Stream.takeWhile(predicate)` — short-circuit take
- `Stream.dropWhile(predicate)` — short-circuit drop
- `Stream.iterate(seed, hasNext, next)` — finite iterate with stop condition
- `Stream.ofNullable(value)` — empty stream if null, else single-element stream
- `Optional.ifPresentOrElse()`, `Optional.or()`, `Optional.stream()`

```java
// Java 9: finite iterate
Stream.iterate(1, n -> n <= 100, n -> n * 2).forEach(System.out::println);

// Java 9: ofNullable
Stream.ofNullable(null).count();  // 0, no NPE
```

### Java 10
- `Collectors.toUnmodifiableList()`, `toUnmodifiableSet()`, `toUnmodifiableMap()`
- `Optional.orElseThrow()` — no-arg version (cleaner than `get()`)

### Java 11
- `String` stream helpers: `String.lines()`, `String.strip()` work well with streams
- `Optional.isEmpty()`

### Java 12
- `Collectors.teeing(collector1, collector2, merger)` — apply two collectors simultaneously

```java
record MinMax(int min, int max) {}
MinMax result = IntStream.of(3,1,4,1,5,9).boxed()
    .collect(Collectors.teeing(
        Collectors.minBy(Integer::compareTo),
        Collectors.maxBy(Integer::compareTo),
        (min, max) -> new MinMax(min.get(), max.get())
    ));
```

### Java 16
- `Stream.toList()` — unmodifiable list, replaces `collect(Collectors.toList())`
- `Stream.mapMulti(BiConsumer)` — imperative flatMap alternative
- `Stream.mapMultiToInt()`, `mapMultiToLong()`, `mapMultiToDouble()`

```java
// Java 16: toList()
List<String> names = stream.filter(...).map(...).toList();

// Java 16: mapMulti
Stream.of(1, 2, 3)
    .mapMulti((n, consumer) -> { consumer.accept(n); consumer.accept(n * 10); })
    .toList();  // [1, 10, 2, 20, 3, 30]
```

### Java 21 (LTS)
- `Stream.gather(Gatherer)` — preview in Java 22, finalized in Java 24
- Sequenced Collections (`SequencedCollection`) integrate with streams
- Virtual threads improve parallel stream I/O performance

### Java 22 (Preview) / Java 24 (Finalized)
- **`Stream.gather(Gatherer)`** — most significant addition since Java 8
  - Enables custom intermediate operations
  - Supports stateful, short-circuiting, and parallel-aware operations

```java
// Gatherer example (Java 24)
stream.gather(Gatherers.windowFixed(3))   // sliding windows of size 3
stream.gather(Gatherers.windowSliding(3))
stream.gather(Gatherers.scan(identity, accumulator))
stream.gather(Gatherers.fold(identity, accumulator))
```

---

## Common Use Cases with Code

### Group and count
```java
Map<String, Long> freq = words.stream()
    .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
```

### Top N elements
```java
list.stream().sorted(Comparator.reverseOrder()).limit(5).toList();
```

### Flat list of nested lists
```java
List<Integer> flat = nested.stream().flatMap(Collection::stream).toList();
```

### Distinct by property
```java
list.stream()
    .filter(distinctByKey(Person::getName))
    .toList();

static <T> Predicate<T> distinctByKey(Function<T, ?> key) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(key.apply(t));
}
```

### Partition into batches
```java
// Java 22+ with Gatherers
stream.gather(Gatherers.windowFixed(batchSize))

// Pre-Java 22
AtomicInteger counter = new AtomicInteger();
Map<Integer, List<T>> batches = list.stream()
    .collect(Collectors.groupingBy(e -> counter.getAndIncrement() / batchSize));
```

### Sum/average of field
```java
double avg = employees.stream().mapToDouble(Employee::getSalary).average().orElse(0);
int total = orders.stream().mapToInt(Order::getAmount).sum();
```

### Convert list to map
```java
Map<Long, User> userById = users.stream()
    .collect(Collectors.toMap(User::getId, u -> u));

// Handle duplicate keys
Map<Long, User> safe = users.stream()
    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
```

### Chained Optional with stream
```java
Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

### String joining
```java
String csv = list.stream().collect(Collectors.joining(", "));
String wrapped = list.stream().collect(Collectors.joining(", ", "[", "]"));
```

---

## Interview Questions

### Beginner

**Q1. What is a Stream in Java? Is it a data structure?**
No. A Stream is a pipeline for processing data from a source. It does not store data. The source (List, array, etc.) stores data.

**Q2. What is the difference between intermediate and terminal operations?**
Intermediate operations (filter, map, sorted) are lazy — they return a Stream and don't execute until a terminal operation (collect, forEach, count) is called.

**Q3. Can a stream be reused?**
No. Once a terminal operation is called, the stream is consumed. Reusing it throws `IllegalStateException`.

**Q4. What is the difference between `map` and `flatMap`?**
- `map` transforms each element 1-to-1
- `flatMap` transforms each element to a stream and flattens all streams into one (1-to-many)

**Q5. What does `filter` do?**
Keeps only elements that match the given predicate. Elements for which the predicate returns false are discarded.

**Q6. What is `collect` and what does `Collectors.toList()` do?**
`collect` is a terminal operation that accumulates stream elements into a container. `Collectors.toList()` collects into a mutable `ArrayList`.

**Q7. What is the difference between `findFirst()` and `findAny()`?**
`findFirst()` returns the first element in encounter order. `findAny()` returns any element — in parallel streams it's faster since it doesn't enforce order.

**Q8. What is `Optional`? Why is it used with streams?**
`Optional<T>` is a container that may or may not hold a value. Terminal operations like `findFirst()`, `min()`, `max()`, `reduce()` return `Optional` because the stream may be empty.

---

### Intermediate

**Q9. What is the difference between `forEach` and `forEachOrdered`?**
In parallel streams, `forEach` doesn't guarantee order. `forEachOrdered` preserves encounter order but reduces parallelism benefit.

**Q10. How does lazy evaluation work in streams?**
Intermediate operations build a pipeline description. No computation happens until a terminal operation is called. This allows optimizations like short-circuiting — `findFirst()` after `filter()` stops as soon as one match is found.

**Q11. What is the difference between `Stream.of()` and `Arrays.stream()`?**
For arrays, `Arrays.stream(arr)` is preferred — it supports primitive arrays (`int[]`, `double[]`) returning `IntStream`/`DoubleStream`. `Stream.of(arr)` on a primitive array returns `Stream<int[]>` (wraps the whole array as one element).

**Q12. What is `reduce`? Give an example.**
`reduce` combines stream elements into a single result using an associative accumulator.
```java
int sum = IntStream.rangeClosed(1, 10).reduce(0, Integer::sum); // 55
```

**Q13. What is `Collectors.groupingBy` vs `partitioningBy`?**
- `groupingBy` groups by any key — result is `Map<K, List<T>>`
- `partitioningBy` groups by boolean predicate — result is always `Map<Boolean, List<T>>`

**Q14. What is the difference between `peek` and `forEach`?**
`peek` is intermediate — it performs an action and passes the element downstream. `forEach` is terminal — it consumes the stream. `peek` is mainly for debugging.

**Q15. How do you handle null values in streams?**
Use `Stream.ofNullable()` (Java 9) for single nullable values, or `filter(Objects::nonNull)` to remove nulls from a stream.

**Q16. What is `Collectors.teeing`? (Java 12)**
Applies two collectors to the same stream simultaneously and merges their results with a merger function. Useful when you need two aggregations in one pass.

**Q17. What is the difference between `toList()` (Java 16) and `collect(Collectors.toList())`?**
- `toList()` returns an **unmodifiable** list
- `collect(Collectors.toList())` returns a mutable `ArrayList`
- `toList()` is shorter and slightly more efficient

**Q18. What is `mapMulti` (Java 16) and when is it better than `flatMap`?**
`mapMulti` uses an imperative push model (consumer) instead of creating intermediate streams. It's more efficient when each element expands to a small number of elements, avoiding stream creation overhead.

---

### Advanced

**Q19. How does parallel stream work internally?**
Parallel streams use the **Fork/Join framework** with `ForkJoinPool.commonPool()`. The source is split using `Spliterator`, tasks are recursively divided (fork), processed independently, and results merged (join).

**Q20. What are the pitfalls of parallel streams?**
- Shared mutable state causes race conditions
- Small datasets have more overhead than gain
- Stateful operations (sorted, distinct) require synchronization
- I/O-bound tasks block threads — use virtual threads or async instead
- `forEach` in parallel doesn't guarantee order

**Q21. How do you use a custom thread pool with parallel streams?**
```java
ForkJoinPool pool = new ForkJoinPool(8);
pool.submit(() -> list.parallelStream().map(this::process).toList()).get();
pool.shutdown();
```

**Q22. What is a `Spliterator`?**
`Spliterator` (Splittable Iterator) is the mechanism that enables parallel decomposition. It can split a source into two parts for parallel processing. Characteristics like `SIZED`, `ORDERED`, `DISTINCT`, `SORTED` help the stream engine optimize execution.

**Q23. How do you implement a custom `Collector`?**
Implement `Collector<T, A, R>` with:
- `supplier()` — creates the mutable container
- `accumulator()` — adds element to container
- `combiner()` — merges two containers (for parallel)
- `finisher()` — transforms container to result
- `characteristics()` — hints like `CONCURRENT`, `UNORDERED`, `IDENTITY_FINISH`

**Q24. What is the difference between `Stream.iterate` in Java 8 vs Java 9?**
```java
// Java 8 — infinite, must use limit()
Stream.iterate(1, n -> n * 2).limit(10)

// Java 9 — finite with stop predicate
Stream.iterate(1, n -> n <= 1024, n -> n * 2)
```

**Q25. What is `Stream.gather` (Java 22 preview / Java 24)?**
`gather` is a new intermediate operation accepting a `Gatherer` — a generalization of `Collector` for intermediate operations. It enables custom stateful, short-circuiting, and parallel-aware intermediate operations that were previously impossible without custom Spliterators.

Built-in `Gatherers`:
- `Gatherers.windowFixed(n)` — non-overlapping windows
- `Gatherers.windowSliding(n)` — overlapping sliding windows
- `Gatherers.scan(identity, fn)` — running accumulation
- `Gatherers.fold(identity, fn)` — like reduce but as intermediate op

**Q26. How does short-circuiting work in streams?**
Operations like `findFirst()`, `findAny()`, `anyMatch()`, `allMatch()`, `noneMatch()`, `limit()` can stop processing before consuming all elements. The pipeline is evaluated lazily, so once the condition is met, remaining elements are not processed.

**Q27. What is the difference between `reduce` and `collect`?**
- `reduce` is for immutable reduction — combines elements into a single immutable value
- `collect` is for mutable reduction — accumulates into a mutable container (List, Map, StringBuilder)
- `collect` is more efficient for building collections because it avoids creating intermediate objects

**Q28. Can you explain the `CONCURRENT` characteristic of a Collector?**
A `CONCURRENT` collector can accumulate into the same container from multiple threads simultaneously (e.g., `ConcurrentHashMap`). Combined with `UNORDERED`, it allows the most efficient parallel collection. Without `CONCURRENT`, parallel streams split, collect into separate containers, then merge.

**Q29. How would you implement `distinct` by a property (not full object equality)?**
```java
static <T> Predicate<T> distinctByKey(Function<T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
}
list.stream().filter(distinctByKey(Person::getEmail)).toList();
```

**Q30. What happens when you call a terminal operation on an already-consumed stream?**
`IllegalStateException: stream has already been operated upon or closed` is thrown. Streams are single-use. To process the same data again, create a new stream from the source.

---

## Quick Reference — Method Cheat Sheet

| Method | Type | Returns | Notes |
|---|---|---|---|
| `filter` | Intermediate | `Stream<T>` | Predicate-based |
| `map` | Intermediate | `Stream<R>` | 1-to-1 transform |
| `flatMap` | Intermediate | `Stream<R>` | 1-to-many, flattens |
| `mapMulti` | Intermediate | `Stream<R>` | Java 16, imperative flatMap |
| `distinct` | Intermediate | `Stream<T>` | Uses equals() |
| `sorted` | Intermediate | `Stream<T>` | Stateful |
| `peek` | Intermediate | `Stream<T>` | Debug only |
| `limit` | Intermediate | `Stream<T>` | Short-circuits |
| `skip` | Intermediate | `Stream<T>` | Stateful |
| `takeWhile` | Intermediate | `Stream<T>` | Java 9 |
| `dropWhile` | Intermediate | `Stream<T>` | Java 9 |
| `mapToInt` | Intermediate | `IntStream` | Avoids boxing |
| `collect` | Terminal | `R` | Most versatile |
| `toList` | Terminal | `List<T>` | Java 16, unmodifiable |
| `forEach` | Terminal | `void` | |
| `reduce` | Terminal | `Optional<T>` / `T` | |
| `count` | Terminal | `long` | |
| `findFirst` | Terminal | `Optional<T>` | Short-circuits |
| `findAny` | Terminal | `Optional<T>` | Faster in parallel |
| `anyMatch` | Terminal | `boolean` | Short-circuits |
| `allMatch` | Terminal | `boolean` | Short-circuits |
| `noneMatch` | Terminal | `boolean` | Short-circuits |
| `min` / `max` | Terminal | `Optional<T>` | |
| `toArray` | Terminal | `Object[]` / `T[]` | |
