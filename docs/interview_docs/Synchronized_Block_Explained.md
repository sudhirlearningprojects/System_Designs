# Synchronized Block - Complete Guide

## What is synchronized Block?

A **synchronized block** is a mechanism in Java that ensures only **ONE thread at a time** can execute a block of code, preventing race conditions and data corruption in multi-threaded environments.

**Simple Analogy**: Like a bathroom lock - only one person can use it at a time. Others must wait outside.

---

## Basic Syntax

```java
synchronized (lockObject) {
    // Critical section - only one thread at a time
    // Access shared resources here
}
```

---

## Example 1: Without synchronized (Problem)

```java
public class WithoutSynchronized {
    private int counter = 0;
    
    public void increment() {
        counter++; // NOT thread-safe!
    }
    
    public static void main(String[] args) throws InterruptedException {
        WithoutSynchronized obj = new WithoutSynchronized();
        
        // Create 1000 threads, each increments counter
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    obj.increment();
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }
        
        System.out.println("Expected: 1000000");
        System.out.println("Actual: " + obj.counter);
    }
}
```

**Output**:
```
Expected: 1000000
Actual: 987234  ❌ WRONG! (varies each run)
```

**Problem**: Multiple threads modify `counter` simultaneously, causing lost updates.

---

## Example 2: With synchronized (Solution)

```java
public class WithSynchronized {
    private int counter = 0;
    private final Object lock = new Object();
    
    public void increment() {
        synchronized (lock) {
            counter++; // Thread-safe!
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        WithSynchronized obj = new WithSynchronized();
        
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    obj.increment();
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        System.out.println("Expected: 1000000");
        System.out.println("Actual: " + obj.counter);
    }
}
```

**Output**:
```
Expected: 1000000
Actual: 1000000  ✅ CORRECT!
```

**Solution**: synchronized ensures only one thread increments at a time.

---

## How synchronized Works Internally

### Step-by-Step Execution

```java
Object lock = new Object();

// Thread 1
synchronized (lock) {
    // Step 1: Thread 1 acquires lock
    // Step 2: Executes code
    // Step 3: Releases lock
}

// Thread 2 (waiting)
synchronized (lock) {
    // Step 4: Thread 2 acquires lock (after Thread 1 releases)
    // Step 5: Executes code
    // Step 6: Releases lock
}
```

### Visual Representation

```
Time →

Thread 1: [Acquire Lock] → [Execute Code] → [Release Lock]
Thread 2:                   [Wait...] → [Acquire Lock] → [Execute Code] → [Release Lock]
Thread 3:                   [Wait...] → [Wait...] → [Acquire Lock] → [Execute Code]
```

---

## Example 3: Demonstrating Lock Behavior

```java
public class LockBehaviorDemo {
    private final Object lock = new Object();
    
    public void task(String threadName) {
        System.out.println(threadName + " trying to acquire lock...");
        
        synchronized (lock) {
            System.out.println(threadName + " acquired lock!");
            
            // Simulate work
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
            
            System.out.println(threadName + " releasing lock");
        }
        
        System.out.println(threadName + " released lock");
    }
    
    public static void main(String[] args) {
        LockBehaviorDemo demo = new LockBehaviorDemo();
        
        Thread t1 = new Thread(() -> demo.task("Thread-1"));
        Thread t2 = new Thread(() -> demo.task("Thread-2"));
        Thread t3 = new Thread(() -> demo.task("Thread-3"));
        
        t1.start();
        t2.start();
        t3.start();
    }
}
```

**Output**:
```
Thread-1 trying to acquire lock...
Thread-1 acquired lock!
Thread-2 trying to acquire lock...
Thread-3 trying to acquire lock...
Thread-1 releasing lock
Thread-1 released lock
Thread-2 acquired lock!
Thread-2 releasing lock
Thread-2 released lock
Thread-3 acquired lock!
Thread-3 releasing lock
Thread-3 released lock
```

**Observation**: Only one thread executes at a time. Others wait.

---

## Types of synchronized

### 1. synchronized Block (Explicit Lock Object)

```java
private final Object lock = new Object();

public void method() {
    synchronized (lock) {
        // Critical section
    }
}
```

**Advantage**: Fine-grained control, can have multiple locks

---

### 2. synchronized Method (Implicit Lock on 'this')

```java
public synchronized void method() {
    // Entire method is synchronized
    // Lock object is 'this'
}

// Equivalent to:
public void method() {
    synchronized (this) {
        // Method body
    }
}
```

---

### 3. Static synchronized Method (Lock on Class Object)

```java
public static synchronized void method() {
    // Lock object is MyClass.class
}

// Equivalent to:
public static void method() {
    synchronized (MyClass.class) {
        // Method body
    }
}
```

---

## Example 4: Different Lock Objects

```java
public class MultipleLocks {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private int counter1 = 0;
    private int counter2 = 0;
    
    public void incrementCounter1() {
        synchronized (lock1) {
            counter1++;
            sleep(100);
        }
    }
    
    public void incrementCounter2() {
        synchronized (lock2) {
            counter2++;
            sleep(100);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        MultipleLocks obj = new MultipleLocks();
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                obj.incrementCounter1();
                System.out.println("T1: counter1 = " + obj.counter1);
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                obj.incrementCounter2();
                System.out.println("T2: counter2 = " + obj.counter2);
            }
        });
        
        long start = System.currentTimeMillis();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long end = System.currentTimeMillis();
        
        System.out.println("\nTime taken: " + (end - start) + "ms");
        System.out.println("Both threads ran in PARALLEL (different locks)");
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Output**:
```
T1: counter1 = 1
T2: counter2 = 1
T1: counter1 = 2
T2: counter2 = 2
...
Time taken: ~1000ms
Both threads ran in PARALLEL (different locks)
```

**Key Point**: Different locks allow parallel execution!

---

## Example 5: Same Lock Object (Sequential)

```java
public class SameLock {
    private final Object lock = new Object();
    private int counter1 = 0;
    private int counter2 = 0;
    
    public void incrementCounter1() {
        synchronized (lock) { // Same lock
            counter1++;
            sleep(100);
        }
    }
    
    public void incrementCounter2() {
        synchronized (lock) { // Same lock
            counter2++;
            sleep(100);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        SameLock obj = new SameLock();
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                obj.incrementCounter1();
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                obj.incrementCounter2();
            }
        });
        
        long start = System.currentTimeMillis();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long end = System.currentTimeMillis();
        
        System.out.println("Time taken: " + (end - start) + "ms");
        System.out.println("Both threads ran SEQUENTIALLY (same lock)");
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Output**:
```
Time taken: ~2000ms
Both threads ran SEQUENTIALLY (same lock)
```

**Key Point**: Same lock forces sequential execution!

---

## Real-World Example: Bank Account

```java
public class BankAccount {
    private double balance = 1000.0;
    private final Object lock = new Object();
    
    public void deposit(double amount) {
        synchronized (lock) {
            System.out.println(Thread.currentThread().getName() + 
                " depositing: $" + amount);
            balance += amount;
            System.out.println("New balance: $" + balance);
        }
    }
    
    public void withdraw(double amount) {
        synchronized (lock) {
            System.out.println(Thread.currentThread().getName() + 
                " withdrawing: $" + amount);
            if (balance >= amount) {
                balance -= amount;
                System.out.println("New balance: $" + balance);
            } else {
                System.out.println("Insufficient funds!");
            }
        }
    }
    
    public double getBalance() {
        synchronized (lock) {
            return balance;
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        BankAccount account = new BankAccount();
        
        Thread t1 = new Thread(() -> account.deposit(500), "Thread-1");
        Thread t2 = new Thread(() -> account.withdraw(300), "Thread-2");
        Thread t3 = new Thread(() -> account.deposit(200), "Thread-3");
        Thread t4 = new Thread(() -> account.withdraw(800), "Thread-4");
        
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        
        System.out.println("\nFinal balance: $" + account.getBalance());
    }
}
```

**Output**:
```
Thread-1 depositing: $500.0
New balance: $1500.0
Thread-2 withdrawing: $300.0
New balance: $1200.0
Thread-3 depositing: $200.0
New balance: $1400.0
Thread-4 withdrawing: $800.0
New balance: $600.0

Final balance: $600.0
```

**Result**: All transactions are thread-safe!

---

## What Happens Without synchronized?

### Race Condition Example

```java
public class RaceCondition {
    private int counter = 0;
    
    public void increment() {
        // This is actually 3 operations:
        // 1. Read counter value
        // 2. Add 1
        // 3. Write back to counter
        counter++;
    }
}
```

**Problem Scenario**:
```
Time →

Thread 1: Read (0) → Add 1 → Write (1)
Thread 2:      Read (0) → Add 1 → Write (1)

Expected: 2
Actual: 1  ❌ Lost update!
```

**With synchronized**:
```
Thread 1: [Lock] → Read (0) → Add 1 → Write (1) → [Unlock]
Thread 2:                                          [Lock] → Read (1) → Add 1 → Write (2) → [Unlock]

Result: 2  ✅ Correct!
```

---

## synchronized vs Lock Interface

### synchronized (Built-in)

```java
synchronized (lock) {
    // Critical section
}
```

**Pros**:
- Simple syntax
- Automatic lock release (even on exception)
- JVM optimized

**Cons**:
- Cannot interrupt waiting thread
- Cannot try lock with timeout
- Less flexible

---

### ReentrantLock (java.util.concurrent)

```java
Lock lock = new ReentrantLock();

lock.lock();
try {
    // Critical section
} finally {
    lock.unlock(); // Must manually unlock
}
```

**Pros**:
- Can try lock with timeout: `tryLock(1, TimeUnit.SECONDS)`
- Can interrupt waiting thread
- More flexible (fairness, conditions)

**Cons**:
- More verbose
- Must manually unlock in finally block

---

## Common Patterns

### Pattern 1: Synchronized Block

```java
private final Object lock = new Object();

public void method() {
    // Non-critical code (can run in parallel)
    doSomeWork();
    
    // Critical section (synchronized)
    synchronized (lock) {
        modifySharedState();
    }
    
    // Non-critical code
    doMoreWork();
}
```

---

### Pattern 2: Double-Checked Locking (Singleton)

```java
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) { // First check (no lock)
            synchronized (Singleton.class) {
                if (instance == null) { // Second check (with lock)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

---

### Pattern 3: Read-Write Lock Pattern

```java
public class ReadWriteExample {
    private final Object lock = new Object();
    private int value = 0;
    
    // Multiple readers can read simultaneously
    public int read() {
        synchronized (lock) {
            return value;
        }
    }
    
    // Only one writer at a time
    public void write(int newValue) {
        synchronized (lock) {
            value = newValue;
        }
    }
}
```

---

## Performance Considerations

### 1. Minimize synchronized Block Size

```java
// ❌ Bad - Entire method synchronized
public synchronized void process() {
    doExpensiveComputation(); // Doesn't need sync
    updateSharedState();      // Needs sync
    doMoreComputation();      // Doesn't need sync
}

// ✅ Good - Only critical section synchronized
public void process() {
    doExpensiveComputation();
    
    synchronized (lock) {
        updateSharedState(); // Only this needs sync
    }
    
    doMoreComputation();
}
```

---

### 2. Avoid Nested Locks (Deadlock Risk)

```java
// ❌ Dangerous - Can cause deadlock
synchronized (lock1) {
    synchronized (lock2) {
        // Critical section
    }
}

// ✅ Better - Single lock or lock ordering
synchronized (lock1) {
    // Critical section
}
```

---

## Key Concepts

### 1. Monitor (Intrinsic Lock)

Every Java object has an **intrinsic lock** (monitor). When you use `synchronized`, you acquire this lock.

```java
Object obj = new Object();
// obj has an intrinsic lock

synchronized (obj) {
    // Current thread owns obj's lock
}
```

---

### 2. Reentrant

A thread can acquire the same lock multiple times (reentrant).

```java
public synchronized void method1() {
    method2(); // OK - same thread, same lock
}

public synchronized void method2() {
    // Can acquire lock again
}
```

---

### 3. Visibility

synchronized ensures **memory visibility** - changes made by one thread are visible to other threads.

```java
private int value = 0;

// Thread 1
synchronized (lock) {
    value = 42; // Write
}

// Thread 2
synchronized (lock) {
    System.out.println(value); // Reads 42 (guaranteed)
}
```

---

## Common Mistakes

### Mistake 1: Synchronizing on null

```java
Object lock = null;
synchronized (lock) { // NullPointerException!
    // Code
}
```

---

### Mistake 2: Synchronizing on String literals

```java
// ❌ Bad - String literals are interned
synchronized ("lock") {
    // Other code might use same string!
}

// ✅ Good - Use dedicated object
private final Object lock = new Object();
synchronized (lock) {
    // Safe
}
```

---

### Mistake 3: Forgetting to synchronize all access

```java
private int counter = 0;
private final Object lock = new Object();

public void increment() {
    synchronized (lock) {
        counter++; // Synchronized
    }
}

public int getCounter() {
    return counter; // ❌ Not synchronized! Can read stale value
}

// ✅ Correct
public int getCounter() {
    synchronized (lock) {
        return counter;
    }
}
```

---

## Summary Table

| Aspect | Description |
|--------|-------------|
| **Purpose** | Mutual exclusion - one thread at a time |
| **Lock Object** | Any Java object |
| **Automatic** | Lock released automatically (even on exception) |
| **Reentrant** | Same thread can acquire lock multiple times |
| **Visibility** | Ensures memory visibility across threads |
| **Performance** | Some overhead, but JVM optimized |

---

## Key Takeaways

1. **synchronized ensures only one thread executes code at a time**
2. **Prevents race conditions** and data corruption
3. **Lock object** can be any Java object
4. **Automatic lock release** - even on exceptions
5. **Reentrant** - same thread can acquire lock multiple times
6. **Memory visibility** - changes visible across threads
7. **Use smallest critical section** for better performance
8. **Every object has intrinsic lock** (monitor)

---

## Quick Reference

```java
// Synchronized block
synchronized (lockObject) {
    // Critical section
}

// Synchronized method
public synchronized void method() {
    // Entire method synchronized
}

// Static synchronized method
public static synchronized void method() {
    // Lock on Class object
}
```

**Rule**: Use synchronized when multiple threads access shared mutable state!
