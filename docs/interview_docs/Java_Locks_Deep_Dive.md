# Java Locks & Locking Mechanisms

## 1. Intrinsic Locks (synchronized)

Every Java object has a built-in monitor lock. `synchronized` acquires it automatically.

```java
// Method-level lock (locks on 'this')
public synchronized void increment() {
    count++;
}

// Block-level lock (finer granularity)
public void increment() {
    synchronized(this) {
        count++;
    }
}

// Static lock (locks on Class object)
public static synchronized void staticMethod() { }
```

**Limitations**: no timeout, no tryLock, not interruptible, always reentrant.

---

## 2. ReentrantLock

Explicit lock with more control than `synchronized`.

```java
ReentrantLock lock = new ReentrantLock();

// Basic usage
lock.lock();
try {
    // critical section
} finally {
    lock.unlock(); // always in finally
}

// tryLock — non-blocking attempt
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try { /* work */ } finally { lock.unlock(); }
} else {
    // handle lock not acquired
}

// Interruptible lock
lock.lockInterruptibly(); // throws InterruptedException if thread interrupted
```

**Fairness**: `new ReentrantLock(true)` — longest-waiting thread gets lock first (lower throughput, prevents starvation).

---

## 3. ReadWriteLock

Allows **multiple concurrent readers** but **exclusive writer**.

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock  = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// Multiple threads can hold readLock simultaneously
readLock.lock();
try { return cache.get(key); } finally { readLock.unlock(); }

// writeLock is exclusive — blocks all readers and writers
writeLock.lock();
try { cache.put(key, value); } finally { writeLock.unlock(); }
```

Best for **read-heavy** workloads (e.g., caches, config stores).

---

## 4. StampedLock (Java 8+)

Faster than ReadWriteLock. Adds **optimistic reads** — no lock acquired at all.

```java
StampedLock lock = new StampedLock();

// Optimistic read — no blocking
long stamp = lock.tryOptimisticRead();
int x = this.x;
if (!lock.validate(stamp)) {  // check if write happened
    stamp = lock.readLock();  // fall back to real read lock
    try { x = this.x; } finally { lock.unlockRead(stamp); }
}

// Write lock
long stamp = lock.writeLock();
try { this.x = newValue; } finally { lock.unlockWrite(stamp); }
```

**Caveat**: Not reentrant. Stamps must be managed carefully.

---

## 5. Condition Variables

Used with `ReentrantLock` as a replacement for `wait()/notify()`.

```java
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();
Condition notEmpty = lock.newCondition();

// Producer
lock.lock();
try {
    while (queue.isFull()) notFull.await();
    queue.add(item);
    notEmpty.signal();
} finally { lock.unlock(); }

// Consumer
lock.lock();
try {
    while (queue.isEmpty()) notEmpty.await();
    return queue.poll();
    notFull.signal();
} finally { lock.unlock(); }
```

Advantage over `wait/notify`: multiple conditions per lock, targeted signaling.

---

## 6. Lock Types Summary

| Lock | Reentrant | Fairness | Try/Timeout | Interruptible | Optimistic Read |
|------|-----------|----------|-------------|---------------|-----------------|
| `synchronized` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `ReentrantLock` | ✅ | ✅ opt-in | ✅ | ✅ | ❌ |
| `ReadWriteLock` | ✅ | ✅ opt-in | ✅ | ✅ | ❌ |
| `StampedLock` | ❌ | ❌ | ✅ | ✅ | ✅ |

---

## 7. Lock-Free Alternatives (Atomic classes)

For simple counters/references, avoid locks entirely using CAS (Compare-And-Swap).

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
counter.compareAndSet(expected, newValue); // CAS operation

AtomicReference<Node> head = new AtomicReference<>();
head.compareAndSet(oldHead, newHead);      // lock-free linked list
```

---

## 8. Deadlock, Livelock, Starvation

| Problem | Cause | Prevention |
|---------|-------|------------|
| **Deadlock** | Circular lock dependency | Always acquire locks in same order; use `tryLock` with timeout |
| **Livelock** | Threads keep retrying and backing off together | Add randomized backoff |
| **Starvation** | Low-priority thread never gets lock | Use fair locks `new ReentrantLock(true)` |

```java
// Deadlock prevention — consistent lock ordering
void transfer(Account from, Account to) {
    Account first  = from.id < to.id ? from : to;  // always lock lower id first
    Account second = from.id < to.id ? to : from;
    synchronized(first) {
        synchronized(second) { /* transfer */ }
    }
}
```

---

## 9. When to Use What

```
Simple counter/flag          → AtomicInteger / AtomicBoolean
Simple critical section      → synchronized
Need tryLock / timeout       → ReentrantLock
Read-heavy (cache/config)    → ReadWriteLock
Ultra-high perf reads        → StampedLock
Producer-consumer            → ReentrantLock + Condition
Concurrent collections       → ConcurrentHashMap, CopyOnWriteArrayList (built-in)
```

---

## 10. volatile — Not a Lock, But Related

`volatile` ensures **visibility** (changes visible to all threads) but **no atomicity**.

```java
volatile boolean running = true;  // safe for flags

// NOT safe — read-modify-write is not atomic
volatile int count = 0;
count++;  // still a race condition!
```

Use `volatile` for simple flags. Use `AtomicInteger` for counters.
