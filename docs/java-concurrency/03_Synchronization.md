# Synchronization in Java

## Table of Contents
- [What is Synchronization?](#what-is-synchronization)
- [Race Conditions](#race-conditions)
- [synchronized Keyword](#synchronized-keyword)
- [Intrinsic Locks (Monitors)](#intrinsic-locks-monitors)
- [Explicit Locks](#explicit-locks)
- [ReadWriteLock](#readwritelock)
- [Volatile Keyword](#volatile-keyword)
- [Deadlocks](#deadlocks)
- [Best Practices](#best-practices)

## What is Synchronization?

**Synchronization** is the coordination of multiple threads to ensure thread-safe access to shared resources. It prevents race conditions and maintains data consistency.

### Why Synchronization is Needed

**The Core Problem**: Multiple threads accessing shared data without coordination leads to **data races** and **inconsistent state**.

**What Happens Without Synchronization**:
1. **Lost Updates**: Thread A's changes overwritten by Thread B
2. **Inconsistent Reads**: Thread sees partially updated data
3. **Torn Reads/Writes**: 64-bit values read/written in chunks
4. **Reordering Effects**: Compiler/CPU reorder operations

```java
public class UnsynchronizedCounter {
    private int count = 0;
    
    public void increment() {
        count++; // DANGEROUS: Not atomic! Actually 3 operations:
                 // 1. LOAD count from memory
                 // 2. ADD 1 to loaded value  
                 // 3. STORE result back to memory
                 // Any thread switch between these = data race!
    }
    
    public int getCount() {
        return count; // Might read stale/inconsistent value
    }
    
    public static void main(String[] args) throws InterruptedException {
        UnsynchronizedCounter counter = new UnsynchronizedCounter();
        
        // Create 1000 threads, each incrementing 1000 times
        // Expected result: 1,000,000 increments
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment(); // Race condition here!
                    // Multiple threads can:
                    // 1. Read same value (e.g., 100)
                    // 2. Both increment to 101
                    // 3. Both store 101
                    // Result: Lost one increment!
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Expected: 1000000");
        System.out.println("Actual: " + counter.getCount()); // Usually 800,000-950,000
        // Why less? Lost updates due to race conditions!
        // Each lost update = 2+ threads reading same value
    }
}
```

**Detailed Race Condition Analysis**:
```
Scenario: count = 100, Thread A and Thread B both call increment()

Time | Thread A           | Thread B           | Memory (count)
-----|--------------------|--------------------|---------------
1    | LOAD count (100)   |                    | 100
2    |                    | LOAD count (100)   | 100  ← Both read same value!
3    | ADD 1 (result=101) |                    | 100
4    |                    | ADD 1 (result=101) | 100
5    | STORE 101          |                    | 101
6    |                    | STORE 101          | 101  ← Lost Thread A's work!

Expected: 102 (100 + 1 + 1)
Actual: 101 (one increment lost)
```

## Race Conditions

A **race condition** occurs when multiple threads access shared data concurrently, and the outcome depends on the timing of their execution.

### Anatomy of a Race Condition

```java
public class RaceConditionDemo {
    private int balance = 100;
    
    // Unsafe withdrawal method
    public void withdraw(int amount) {
        if (balance >= amount) {          // Check
            try {
                Thread.sleep(1); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            balance -= amount;            // Update
            System.out.println("Withdrew " + amount + ", balance: " + balance);
        } else {
            System.out.println("Insufficient funds for " + amount);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        RaceConditionDemo account = new RaceConditionDemo();
        
        // Two threads trying to withdraw simultaneously
        Thread thread1 = new Thread(() -> account.withdraw(80));
        Thread thread2 = new Thread(() -> account.withdraw(80));
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        // Both withdrawals might succeed, leading to negative balance!
    }
}
```

### Critical Section

The **critical section** is the part of code that accesses shared resources and must be executed by only one thread at a time.

```java
public class CriticalSectionExample {
    private int sharedResource = 0;
    
    public void accessResource() {
        // Non-critical section
        System.out.println("Thread " + Thread.currentThread().getName() + " entering");
        
        // CRITICAL SECTION - needs synchronization
        int temp = sharedResource;
        temp = temp + 1;
        try {
            Thread.sleep(1); // Simulate processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sharedResource = temp;
        // END CRITICAL SECTION
        
        System.out.println("Thread " + Thread.currentThread().getName() + 
            " updated resource to: " + sharedResource);
    }
}
```

## synchronized Keyword

**Theory**: The `synchronized` keyword implements **mutual exclusion** using **intrinsic locks** (also called monitors). It ensures that only one thread can execute a synchronized block/method at a time.

**How synchronized Works**:
1. **Acquire Lock**: Thread must acquire object's intrinsic lock
2. **Execute Code**: Only lock holder can execute synchronized code
3. **Release Lock**: Lock automatically released when exiting synchronized block
4. **Memory Barrier**: Creates happens-before relationship for memory visibility

**Intrinsic Lock Properties**:
- **Every Java object** has an intrinsic lock
- **Reentrant**: Same thread can acquire same lock multiple times
- **Blocking**: Threads wait if lock is held by another thread
- **Fair ordering**: No guarantee of fairness (can cause starvation)

**Memory Effects of synchronized**:
```
Before synchronized block:  [Thread's local cache may be stale]
    ↓
Enter synchronized:         [Memory barrier - refresh from main memory]
    ↓
Execute synchronized code:  [All reads see latest values]
    ↓
Exit synchronized:          [Memory barrier - flush to main memory]
    ↓
After synchronized block:   [Changes visible to other threads]
```

### Synchronized Methods

```java
public class SynchronizedCounter {
    private int count = 0;
    
    // Synchronized instance method
    public synchronized void increment() {
        count++; // Only one thread can execute this at a time
    }
    
    public synchronized void decrement() {
        count--;
    }
    
    public synchronized int getCount() {
        return count;
    }
    
    // Synchronized static method (class-level lock)
    public static synchronized void staticMethod() {
        System.out.println("Static synchronized method");
    }
    
    public static void main(String[] args) throws InterruptedException {
        SynchronizedCounter counter = new SynchronizedCounter();
        
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Final count: " + counter.getCount()); // Always 1000000
    }
}
```

### Synchronized Blocks

```java
public class SynchronizedBlocks {
    private int count1 = 0;
    private int count2 = 0;
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void incrementCount1() {
        synchronized(lock1) {
            count1++;
        }
    }
    
    public void incrementCount2() {
        synchronized(lock2) {
            count2++;
        }
    }
    
    // Fine-grained locking for better concurrency
    public void incrementBoth() {
        synchronized(lock1) {
            count1++;
        }
        synchronized(lock2) {
            count2++;
        }
    }
    
    // Synchronized on this object
    public void method1() {
        synchronized(this) {
            // Critical section
            System.out.println("Method1 executing");
        }
    }
    
    // Equivalent to above
    public synchronized void method2() {
        System.out.println("Method2 executing");
    }
    
    // Class-level synchronization
    public void staticLikeMethod() {
        synchronized(SynchronizedBlocks.class) {
            System.out.println("Class-level synchronization");
        }
    }
}
```

### Synchronized Collections

```java
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SynchronizedCollections {
    public static void main(String[] args) throws InterruptedException {
        // Synchronized wrapper
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // Thread-safe operations
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                syncList.add(i);
            }
        });
        
        Thread reader = new Thread(() -> {
            // Need external synchronization for iteration
            synchronized(syncList) {
                for (Integer value : syncList) {
                    System.out.println("Read: " + value);
                }
            }
        });
        
        writer.start();
        Thread.sleep(100); // Let writer add some elements
        reader.start();
        
        writer.join();
        reader.join();
        
        // Better alternative: ConcurrentHashMap, CopyOnWriteArrayList
        List<Integer> cowList = new CopyOnWriteArrayList<>();
        cowList.add(1);
        cowList.add(2);
        
        // Safe iteration without external synchronization
        for (Integer value : cowList) {
            System.out.println("COW List: " + value);
        }
    }
}
```

## Intrinsic Locks (Monitors)

Every Java object has an intrinsic lock (monitor). The `synchronized` keyword uses these locks.

### Monitor Characteristics

```java
public class MonitorExample {
    private int value = 0;
    
    public synchronized void method1() {
        System.out.println("Method1 - Thread: " + Thread.currentThread().getName());
        method2(); // Reentrant - same thread can acquire lock again
    }
    
    public synchronized void method2() {
        System.out.println("Method2 - Thread: " + Thread.currentThread().getName());
        value++;
    }
    
    public synchronized void method3() {
        System.out.println("Method3 - Thread: " + Thread.currentThread().getName());
    }
    
    public static void main(String[] args) throws InterruptedException {
        MonitorExample monitor = new MonitorExample();
        
        Thread thread1 = new Thread(() -> monitor.method1(), "Thread-1");
        Thread thread2 = new Thread(() -> monitor.method3(), "Thread-2");
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
    }
}
```

### Reentrant Nature of Intrinsic Locks

```java
public class ReentrantExample {
    private int count = 0;
    
    public synchronized void outerMethod() {
        System.out.println("Outer method - count: " + count);
        innerMethod(); // Same thread can acquire lock again
    }
    
    public synchronized void innerMethod() {
        count++;
        System.out.println("Inner method - count: " + count);
        if (count < 3) {
            innerMethod(); // Recursive call - still reentrant
        }
    }
    
    public static void main(String[] args) {
        ReentrantExample example = new ReentrantExample();
        example.outerMethod();
    }
}
```

## Explicit Locks

The `java.util.concurrent.locks` package provides more flexible locking mechanisms.

### ReentrantLock

```java
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    
    public void increment() {
        lock.lock();
        try {
            count++;
            System.out.println("Count: " + count + " by " + Thread.currentThread().getName());
        } finally {
            lock.unlock(); // Always unlock in finally block
        }
    }
    
    public void decrement() {
        if (lock.tryLock()) { // Non-blocking lock attempt
            try {
                count--;
                System.out.println("Decremented to: " + count);
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Could not acquire lock for decrement");
        }
    }
    
    public void timedOperation() {
        try {
            if (lock.tryLock(2, java.util.concurrent.TimeUnit.SECONDS)) {
                try {
                    Thread.sleep(1000);
                    System.out.println("Timed operation completed");
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("Could not acquire lock within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ReentrantLockExample example = new ReentrantLockExample();
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                example.increment();
                example.decrement();
                example.timedOperation();
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
```

### Lock with Condition Variables

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerWithLock {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    
    private final int[] buffer = new int[10];
    private int count = 0;
    private int putIndex = 0;
    private int takeIndex = 0;
    
    public void put(int item) throws InterruptedException {
        lock.lock();
        try {
            while (count == buffer.length) {
                notFull.await(); // Wait until buffer is not full
            }
            
            buffer[putIndex] = item;
            putIndex = (putIndex + 1) % buffer.length;
            count++;
            
            System.out.println("Produced: " + item + ", Count: " + count);
            notEmpty.signal(); // Signal that buffer is not empty
        } finally {
            lock.unlock();
        }
    }
    
    public int take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // Wait until buffer is not empty
            }
            
            int item = buffer[takeIndex];
            takeIndex = (takeIndex + 1) % buffer.length;
            count--;
            
            System.out.println("Consumed: " + item + ", Count: " + count);
            notFull.signal(); // Signal that buffer is not full
            
            return item;
        } finally {
            lock.unlock();
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumerWithLock pc = new ProducerConsumerWithLock();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    pc.put(i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    pc.take();
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
    }
}
```

## ReadWriteLock

ReadWriteLock allows multiple readers or one writer, improving performance for read-heavy scenarios.

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockExample {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private String data = "Initial Data";
    
    public String readData() {
        lock.readLock().lock();
        try {
            System.out.println("Reading data: " + data + " by " + 
                Thread.currentThread().getName());
            Thread.sleep(1000); // Simulate read operation
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void writeData(String newData) {
        lock.writeLock().lock();
        try {
            System.out.println("Writing data: " + newData + " by " + 
                Thread.currentThread().getName());
            Thread.sleep(2000); // Simulate write operation
            this.data = newData;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ReadWriteLockExample example = new ReadWriteLockExample();
        
        // Multiple readers
        for (int i = 0; i < 3; i++) {
            Thread reader = new Thread(() -> example.readData(), "Reader-" + i);
            reader.start();
        }
        
        Thread.sleep(500);
        
        // One writer
        Thread writer = new Thread(() -> example.writeData("Updated Data"), "Writer");
        writer.start();
        
        Thread.sleep(500);
        
        // More readers after writer
        for (int i = 3; i < 5; i++) {
            Thread reader = new Thread(() -> example.readData(), "Reader-" + i);
            reader.start();
        }
    }
}
```

## Volatile Keyword

The `volatile` keyword ensures visibility of changes across threads and prevents certain compiler optimizations.

### Visibility Problem

```java
public class VisibilityProblem {
    private boolean flag = false; // Without volatile
    private int counter = 0;
    
    public void writer() {
        counter = 42;
        flag = true; // Other thread might not see this change
    }
    
    public void reader() {
        if (flag) {
            System.out.println("Counter: " + counter); // Might print 0!
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        VisibilityProblem example = new VisibilityProblem();
        
        Thread writerThread = new Thread(example::writer);
        Thread readerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                example.reader();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        readerThread.start();
        Thread.sleep(100);
        writerThread.start();
        
        Thread.sleep(1000);
        readerThread.interrupt();
    }
}
```

### Volatile Solution

```java
public class VolatileSolution {
    private volatile boolean flag = false; // Ensures visibility
    private int counter = 0;
    
    public void writer() {
        counter = 42;
        flag = true; // Guaranteed to be visible to other threads
    }
    
    public void reader() {
        if (flag) {
            System.out.println("Counter: " + counter); // Will print 42
        }
    }
    
    // Volatile for thread-safe singleton (double-checked locking)
    private static volatile VolatileSolution instance;
    
    public static VolatileSolution getInstance() {
        if (instance == null) {
            synchronized (VolatileSolution.class) {
                if (instance == null) {
                    instance = new VolatileSolution();
                }
            }
        }
        return instance;
    }
}
```

### When to Use Volatile

```java
public class VolatileUseCases {
    // 1. Status flags
    private volatile boolean running = true;
    
    public void stop() {
        running = false;
    }
    
    public void doWork() {
        while (running) {
            // Do work
        }
    }
    
    // 2. One writer, multiple readers
    private volatile long counter = 0;
    
    public void increment() { // Only one thread calls this
        counter++;
    }
    
    public long getCounter() { // Multiple threads can call this
        return counter;
    }
    
    // 3. Independent variables
    private volatile int x;
    private volatile int y;
    
    public void setValues(int newX, int newY) {
        x = newX; // Each assignment is atomic and visible
        y = newY;
    }
}
```

## Deadlocks

A **deadlock** occurs when two or more threads are blocked forever, waiting for each other.

### Classic Deadlock Example

```java
public class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void method1() {
        synchronized (lock1) {
            System.out.println("Thread " + Thread.currentThread().getName() + 
                " acquired lock1");
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            synchronized (lock2) {
                System.out.println("Thread " + Thread.currentThread().getName() + 
                    " acquired lock2");
            }
        }
    }
    
    public void method2() {
        synchronized (lock2) { // Different order!
            System.out.println("Thread " + Thread.currentThread().getName() + 
                " acquired lock2");
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            synchronized (lock1) {
                System.out.println("Thread " + Thread.currentThread().getName() + 
                    " acquired lock1");
            }
        }
    }
    
    public static void main(String[] args) {
        DeadlockExample example = new DeadlockExample();
        
        Thread thread1 = new Thread(example::method1, "Thread-1");
        Thread thread2 = new Thread(example::method2, "Thread-2");
        
        thread1.start();
        thread2.start();
        
        // Threads will deadlock!
    }
}
```

### Deadlock Prevention

```java
public class DeadlockPrevention {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    // Solution 1: Consistent lock ordering
    public void method1() {
        synchronized (lock1) { // Always acquire lock1 first
            synchronized (lock2) {
                System.out.println("Method1 executed by " + 
                    Thread.currentThread().getName());
            }
        }
    }
    
    public void method2() {
        synchronized (lock1) { // Same order as method1
            synchronized (lock2) {
                System.out.println("Method2 executed by " + 
                    Thread.currentThread().getName());
            }
        }
    }
    
    // Solution 2: Timeout-based locking
    private final ReentrantLock lockA = new ReentrantLock();
    private final ReentrantLock lockB = new ReentrantLock();
    
    public void timedMethod1() {
        try {
            if (lockA.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (lockB.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            System.out.println("TimedMethod1 executed");
                        } finally {
                            lockB.unlock();
                        }
                    } else {
                        System.out.println("Could not acquire lockB");
                    }
                } finally {
                    lockA.unlock();
                }
            } else {
                System.out.println("Could not acquire lockA");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Deadlock Detection

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class DeadlockDetection {
    public static void detectDeadlock() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            System.out.println("Deadlock detected!");
            for (long threadId : deadlockedThreads) {
                System.out.println("Deadlocked thread ID: " + threadId);
            }
        } else {
            System.out.println("No deadlock detected");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Start deadlock detection in background
        Thread detector = new Thread(() -> {
            while (true) {
                detectDeadlock();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        detector.setDaemon(true);
        detector.start();
        
        // Create potential deadlock scenario
        DeadlockExample deadlockExample = new DeadlockExample();
        Thread thread1 = new Thread(deadlockExample::method1);
        Thread thread2 = new Thread(deadlockExample::method2);
        
        thread1.start();
        thread2.start();
        
        Thread.sleep(10000); // Let detector run
    }
}
```

## Best Practices

### 1. Minimize Lock Scope

```java
// Bad - lock held too long
public synchronized void badMethod() {
    // Long computation
    for (int i = 0; i < 1000000; i++) {
        Math.sqrt(i);
    }
    
    // Critical section
    sharedResource++;
}

// Good - minimal lock scope
public void goodMethod() {
    // Long computation outside lock
    double result = 0;
    for (int i = 0; i < 1000000; i++) {
        result += Math.sqrt(i);
    }
    
    // Only critical section is synchronized
    synchronized(this) {
        sharedResource++;
    }
}
```

### 2. Use Concurrent Collections

```java
// Bad
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());

// Good
Map<String, String> map = new ConcurrentHashMap<>();
```

### 3. Prefer Immutable Objects

```java
// Thread-safe because immutable
public final class ImmutablePoint {
    private final int x;
    private final int y;
    
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    
    public ImmutablePoint move(int dx, int dy) {
        return new ImmutablePoint(x + dx, y + dy);
    }
}
```

### 4. Use ThreadLocal for Thread-Specific Data

```java
public class ThreadLocalExample {
    private static final ThreadLocal<SimpleDateFormat> dateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    
    public String formatDate(Date date) {
        return dateFormat.get().format(date); // Thread-safe
    }
}
```

## Summary

**Key Theoretical Insights**:

1. **The Synchronization Problem**:
   - **Root cause**: Shared mutable state + concurrent access
   - **Manifestation**: Race conditions, lost updates, inconsistent state
   - **Solution**: Mutual exclusion + memory barriers

2. **synchronized Keyword**:
   - **Mechanism**: Intrinsic locks (monitors) on every Java object
   - **Properties**: Mutual exclusion, reentrancy, memory barriers
   - **Trade-off**: Simple but inflexible

3. **Explicit Locks**:
   - **Advantage**: Timeout, interrupt, try-lock, fairness, conditions
   - **Responsibility**: Manual lock management (lock/unlock)
   - **Use case**: When synchronized is insufficient

4. **ReadWriteLock**:
   - **Optimization**: Multiple readers OR single writer
   - **Benefit**: Better performance for read-heavy workloads
   - **Complexity**: More complex than simple mutual exclusion

5. **volatile Keyword**:
   - **Purpose**: Memory visibility without mutual exclusion
   - **Limitation**: Ensures visibility but not atomicity
   - **Use case**: Status flags, single-writer scenarios

6. **Deadlock Prevention**:
   - **Cause**: Circular wait for resources
   - **Prevention**: Consistent lock ordering, timeout, deadlock detection
   - **Design**: Minimize lock scope and nesting

**Performance Hierarchy** (fastest to slowest):
```
1. No synchronization (unsafe)
2. volatile (visibility only)
3. Atomic operations (lock-free)
4. ReentrantLock (explicit)
5. synchronized (intrinsic)
6. Multiple nested locks
```

**Best Practices Summary**:
- **Minimize critical sections**: Lock only what's necessary
- **Use concurrent collections**: ConcurrentHashMap, CopyOnWriteArrayList
- **Prefer immutable objects**: Thread-safe by design
- **Consistent lock ordering**: Prevent deadlocks
- **Always use try-finally**: With explicit locks
- **Consider lock-free alternatives**: Atomic classes, concurrent collections

## Next Steps

**Mastery Checklist**:
- ✓ Can you explain why `counter++` needs synchronization?
- ✓ Do you understand the difference between synchronized methods and blocks?
- ✓ Can you identify potential deadlock scenarios?
- ✓ Do you know when to use ReentrantLock vs synchronized?
- ✓ Can you explain the memory effects of synchronization?

**Common Interview Questions**:
1. "What happens if you don't synchronize access to shared variables?"
2. "Explain the difference between synchronized and volatile."
3. "How would you prevent deadlocks in a multi-threaded application?"
4. "When would you choose ReentrantLock over synchronized?"
5. "What is the happens-before relationship in synchronization?"

Continue to [Thread Communication](04_Thread_Communication.md) to learn about wait/notify and producer-consumer patterns.