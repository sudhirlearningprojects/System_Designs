# ThreadLocal in Java — Deep Dive

## What is ThreadLocal?

`ThreadLocal<T>` provides **thread-local variables** — each thread that accesses a `ThreadLocal` variable gets its own independently initialized copy. No thread can see or modify another thread's copy.

It solves the problem of sharing state across method calls **within the same thread** without passing parameters explicitly, while keeping that state completely isolated from other threads.

```
Thread-1 ──→ ThreadLocal.get() ──→ value-A   (Thread-1's copy)
Thread-2 ──→ ThreadLocal.get() ──→ value-B   (Thread-2's copy)
Thread-3 ──→ ThreadLocal.get() ──→ value-C   (Thread-3's copy)
```

---

## How it Works Internally

Each `Thread` object has a field:
```java
ThreadLocal.ThreadLocalMap threadLocals;  // inside java.lang.Thread
```

`ThreadLocalMap` is a custom hash map where:
- **Key** = the `ThreadLocal` instance (stored as a `WeakReference`)
- **Value** = the thread-local value for that thread

When you call `threadLocal.get()`:
1. Gets the current thread via `Thread.currentThread()`
2. Gets the thread's `threadLocals` map
3. Looks up the `ThreadLocal` instance as the key
4. Returns the associated value

This means the value lives **on the thread**, not on the `ThreadLocal` object itself.

```
Thread object
└── threadLocals (ThreadLocalMap)
    ├── [ThreadLocal-A (WeakRef)] → value-1
    ├── [ThreadLocal-B (WeakRef)] → value-2
    └── [ThreadLocal-C (WeakRef)] → value-3
```

---

## Class Hierarchy

```
java.lang.Object
└── java.lang.ThreadLocal<T>
    └── java.lang.InheritableThreadLocal<T>
```

---

## Methods

### set(T value)
```java
threadLocal.set("user-123");
```
Stores the value in the current thread's `ThreadLocalMap`. If no map exists yet, creates one.

### get()
```java
String value = threadLocal.get();
```
Returns the current thread's copy. If `set()` was never called on this thread, calls `initialValue()` and stores + returns that.

### remove()
```java
threadLocal.remove();
```
Removes the current thread's value from its `ThreadLocalMap`. **Critical in thread pools** — without this, the value persists on the reused thread.

### initialValue()
```java
ThreadLocal<List<String>> tl = new ThreadLocal<>() {
    @Override
    protected List<String> initialValue() {
        return new ArrayList<>();
    }
};
```
Called lazily on first `get()` if no value has been set. Override to provide a default. Returns `null` by default.

### withInitial(Supplier) — Java 8+
```java
ThreadLocal<SimpleDateFormat> tl =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
```
Static factory method — cleaner alternative to overriding `initialValue()`. The supplier is called once per thread on first access.

---

## InheritableThreadLocal

Child threads inherit the parent thread's values at the time of child thread creation.

```java
InheritableThreadLocal<String> itl = new InheritableThreadLocal<>();
itl.set("parent-value");

Thread child = new Thread(() -> {
    System.out.println(itl.get()); // prints "parent-value"
});
child.start();
```

### childValue(T parentValue)
Override to transform the inherited value for the child thread:
```java
InheritableThreadLocal<Integer> depth = new InheritableThreadLocal<>() {
    @Override
    protected Integer childValue(Integer parentValue) {
        return parentValue + 1;  // child gets parent's depth + 1
    }
};
```

**Limitation:** Inheritance happens at thread creation time only. Changes to the parent's value after child creation are not reflected in the child.

**Does NOT work with thread pools** — pooled threads are created once, not per task. Use `TransmittableThreadLocal` (TTL library) for thread pool context propagation.

---

## Use Cases

### 1. Per-Thread Expensive Object (Classic Use Case)
`SimpleDateFormat` is not thread-safe. Instead of synchronizing or creating per-call, store one per thread:

```java
private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

public String format(Date date) {
    return DATE_FORMAT.get().format(date);  // safe, no sync needed
}
```

### 2. Request Context in Web Applications
Store the current user/request context at the filter level, access it anywhere in the call stack without passing it as a parameter:

```java
public class UserContext {
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    public static void set(String userId) { CURRENT_USER.set(userId); }
    public static String get() { return CURRENT_USER.get(); }
    public static void clear() { CURRENT_USER.remove(); }
}

// In servlet filter
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
    try {
        UserContext.set(extractUserId(req));
        chain.doFilter(req, res);
    } finally {
        UserContext.clear();  // always clear — thread goes back to pool
    }
}

// Deep in service layer — no parameter passing needed
public void auditLog(String action) {
    String user = UserContext.get();  // just works
    log.info("User {} performed {}", user, action);
}
```

### 3. Database Transaction / Connection Per Thread
```java
private static final ThreadLocal<Connection> TX_CONNECTION = new ThreadLocal<>();

public static Connection getConnection() {
    Connection conn = TX_CONNECTION.get();
    if (conn == null) {
        conn = dataSource.getConnection();
        TX_CONNECTION.set(conn);
    }
    return conn;
}

public static void closeConnection() {
    Connection conn = TX_CONNECTION.get();
    if (conn != null) {
        conn.close();
        TX_CONNECTION.remove();
    }
}
```
This is exactly how Spring's `TransactionSynchronizationManager` works internally.

### 4. Distributed Tracing — Trace/Span ID Propagation
```java
public class TraceContext {
    private static final ThreadLocal<String> TRACE_ID =
        ThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    public static String getTraceId() { return TRACE_ID.get(); }
    public static void setTraceId(String id) { TRACE_ID.set(id); }
    public static void clear() { TRACE_ID.remove(); }
}
```
Spring Sleuth / Micrometer Tracing use this pattern internally.

### 5. Avoiding Parameter Drilling in Recursive Algorithms
```java
private static final ThreadLocal<Set<Integer>> VISITED =
    ThreadLocal.withInitial(HashSet::new);

public void dfs(int node) {
    Set<Integer> visited = VISITED.get();
    if (visited.contains(node)) return;
    visited.add(node);
    // recurse
}
```

---

## Memory Leak — The Critical Pitfall

### Why it happens

`ThreadLocalMap` keys are `WeakReference`s to `ThreadLocal` objects. When the `ThreadLocal` instance is GC'd, the key becomes `null` — but the **value is a strong reference and stays in the map**.

In a thread pool, threads live forever. If `remove()` is never called:
- The value stays in the thread's map indefinitely
- Multiplied by all pooled threads = memory leak

```
Thread (lives forever in pool)
└── threadLocals map
    └── [null key (ThreadLocal was GC'd)] → LARGE_VALUE_OBJECT  ← leak
```

### Fix — always remove in finally

```java
try {
    threadLocal.set(value);
    doWork();
} finally {
    threadLocal.remove();  // non-negotiable in thread pools
}
```

### Stale data bug (worse than leak)

Even if the value is small, forgetting `remove()` means the next task on the same thread sees the previous task's data:

```java
// Thread-1 handles Request-A: sets userId = "alice"
// Thread-1 returns to pool
// Thread-1 handles Request-B: never sets userId
// Thread-1's get() returns "alice" — wrong user!
```

---

## ThreadLocal vs Alternatives

| Approach | Thread Safety | Scope | Use When |
|---|---|---|---|
| `ThreadLocal` | Isolated per thread | Within a thread's call stack | Per-thread state, no sharing needed |
| Method parameters | N/A | Explicit | Simple, few layers |
| `synchronized` / locks | Shared, serialized | Across threads | Shared mutable state |
| `AtomicReference` | Shared, lock-free | Across threads | High-concurrency shared state |
| Scoped Values (Java 21) | Immutable, per-scope | Structured concurrency | Replacing ThreadLocal in virtual threads |

---

## ThreadLocal with Virtual Threads (Java 21+)

Virtual threads (Project Loom) can have millions of instances. Each carrying a `ThreadLocal` value means millions of copies in memory — potential issue.

Java 21 introduces **Scoped Values** (`ScopedValue`) as the preferred alternative for virtual threads:

```java
// ScopedValue — immutable, no remove() needed, works with virtual threads
static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

ScopedValue.where(USER_ID, "alice").run(() -> {
    System.out.println(USER_ID.get()); // "alice"
    // value is automatically gone when the scope exits
});
```

Key differences from `ThreadLocal`:
- Immutable within a scope — cannot be changed after binding
- Automatically cleaned up when scope exits — no `remove()` needed
- Inherited by child scopes (structured concurrency)
- Much lower memory footprint with millions of virtual threads

---

## Quick Reference

| Method | Description |
|---|---|
| `set(T value)` | Store value for current thread |
| `get()` | Get current thread's value (calls `initialValue()` if absent) |
| `remove()` | Delete current thread's value — **always call in thread pools** |
| `initialValue()` | Override to provide default value (called lazily) |
| `withInitial(Supplier)` | Static factory with lambda initializer (Java 8+) |
| `InheritableThreadLocal` | Child threads inherit parent's value at creation time |
| `childValue(parentVal)` | Override to transform inherited value for child thread |

---

## Interview Questions

**Q1. What is ThreadLocal and when would you use it?**
`ThreadLocal` gives each thread its own copy of a variable. Use it when you need per-thread state that must be accessible across the call stack without parameter passing — e.g., request context, transaction connections, non-thread-safe objects like `SimpleDateFormat`.

**Q2. How does ThreadLocal work internally?**
Each `Thread` has a `ThreadLocalMap` field. The `ThreadLocal` instance is the key (as a `WeakReference`), and the thread-local value is the value. `get()`/`set()` operate on the current thread's map.

**Q3. Why must you call `remove()` in thread pools?**
Thread pool threads are reused. Without `remove()`, the value from a previous task persists on the thread. The next task either sees stale data (correctness bug) or accumulates objects (memory leak).

**Q4. What is the memory leak risk with ThreadLocal?**
Keys in `ThreadLocalMap` are `WeakReference`s. When the `ThreadLocal` object is GC'd, the key becomes `null` but the value (strong reference) remains in the map. In long-lived pooled threads, these orphaned values accumulate. Fix: always call `remove()`.

**Q5. What is the difference between `ThreadLocal` and `InheritableThreadLocal`?**
`InheritableThreadLocal` copies the parent thread's values to child threads at creation time. `ThreadLocal` does not — each thread starts with `null` (or `initialValue()`). `InheritableThreadLocal` doesn't work with thread pools since threads are pre-created.

**Q6. How does Spring use ThreadLocal internally?**
Spring's `TransactionSynchronizationManager` uses `ThreadLocal` to bind the current `Connection` and transaction state to the executing thread. `SecurityContextHolder` in Spring Security uses `ThreadLocal` to store the `Authentication` object per request thread.

**Q7. What is the difference between ThreadLocal and ScopedValue (Java 21)?**
`ThreadLocal` is mutable, requires manual `remove()`, and has high memory cost with virtual threads. `ScopedValue` is immutable within a scope, auto-cleaned on scope exit, and designed for structured concurrency with virtual threads. `ScopedValue` is the modern replacement.

**Q8. Can two threads share a ThreadLocal value?**
No. That's the entire point — each thread has its own copy. If you need shared state between threads, use `AtomicReference`, `volatile`, or synchronized data structures instead.
