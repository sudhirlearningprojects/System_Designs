# Atomic Operations and Lock-Free Programming

## Table of Contents
- [Introduction to Atomic Operations](#introduction-to-atomic-operations)
- [Compare-and-Swap (CAS)](#compare-and-swap-cas)
- [Atomic Classes](#atomic-classes)
- [AtomicReference and ABA Problem](#atomicreference-and-aba-problem)
- [Lock-Free Data Structures](#lock-free-data-structures)
- [Memory Ordering and Barriers](#memory-ordering-and-barriers)
- [Performance Comparison](#performance-comparison)
- [Best Practices](#best-practices)

## Introduction to Atomic Operations

### Theoretical Foundation

**Atomic operations** are indivisible operations that complete entirely or not at all. They form the foundation of lock-free programming:

1. **Atomicity**: Operation appears instantaneous to other threads
2. **Consistency**: System remains in valid state
3. **Isolation**: Concurrent operations don't interfere
4. **Linearizability**: Operations appear to occur at single point in time

### Hardware Support

Modern processors provide atomic instructions:
- **x86**: LOCK prefix, CMPXCHG (compare-and-exchange)
- **ARM**: LDREX/STREX (load/store exclusive)
- **SPARC**: CAS (compare-and-swap)
- **PowerPC**: LWARX/STWCX (load/store conditional)

### Lock-Free vs Wait-Free

1. **Lock-Free**: At least one thread makes progress
2. **Wait-Free**: Every thread makes progress in finite steps
3. **Obstruction-Free**: Thread makes progress when running alone

```java
import java.util.concurrent.atomic.*;

public class AtomicBasics {
    
    // Non-atomic operation (race condition)
    private int counter = 0;
    
    public void unsafeIncrement() {
        counter++; // Read-Modify-Write: NOT atomic
    }
    
    // Atomic operation (thread-safe)
    private AtomicInteger atomicCounter = new AtomicInteger(0);
    
    public void safeIncrement() {
        atomicCounter.incrementAndGet(); // Atomic operation
    }
    
    public static void demonstrateRaceCondition() throws InterruptedException {
        AtomicBasics example = new AtomicBasics();
        
        Thread[] threads = new Thread[10];
        
        // Test unsafe increment
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    example.unsafeIncrement();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Unsafe counter (expected 10000): " + example.counter);
        
        // Test safe increment
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    example.safeIncrement();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Safe counter (expected 10000): " + example.atomicCounter.get());
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateRaceCondition();
    }
}
```

## Compare-and-Swap (CAS)

### CAS Theory

**Compare-and-Swap** is the fundamental atomic operation:
```
boolean CAS(memory_location, expected_value, new_value) {
    if (*memory_location == expected_value) {
        *memory_location = new_value;
        return true;
    }
    return false;
}
```

### CAS Properties

1. **Atomic**: Entire operation is indivisible
2. **Non-blocking**: Doesn't use locks
3. **Optimistic**: Assumes no contention
4. **Retry-based**: Failed operations can be retried

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CASExample {
    
    public static void basicCAS() {
        AtomicInteger atomicInt = new AtomicInteger(10);
        
        // Successful CAS
        boolean success1 = atomicInt.compareAndSet(10, 20);
        System.out.println("CAS(10->20): " + success1 + ", value: " + atomicInt.get());
        
        // Failed CAS
        boolean success2 = atomicInt.compareAndSet(10, 30);
        System.out.println("CAS(10->30): " + success2 + ", value: " + atomicInt.get());
        
        // Successful CAS with current value
        boolean success3 = atomicInt.compareAndSet(20, 30);
        System.out.println("CAS(20->30): " + success3 + ", value: " + atomicInt.get());
    }
    
    public static void casLoop() {
        AtomicInteger counter = new AtomicInteger(0);
        
        // Implement increment using CAS loop
        int increment = 5;
        int current;
        do {
            current = counter.get();
        } while (!counter.compareAndSet(current, current + increment));
        
        System.out.println("Incremented by " + increment + ", new value: " + counter.get());
    }
    
    public static void casWithRetry() {
        AtomicInteger sharedCounter = new AtomicInteger(0);
        
        // Multiple threads trying to increment
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    // CAS-based increment with retry
                    int current, next;
                    do {
                        current = sharedCounter.get();
                        next = current + 1;
                    } while (!sharedCounter.compareAndSet(current, next));
                }
                System.out.println("Thread " + threadId + " completed");
            });
            threads[i].start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Final counter value: " + sharedCounter.get());
    }
    
    public static void weakCAS() {
        AtomicInteger atomicInt = new AtomicInteger(10);
        
        // weakCompareAndSet may fail spuriously
        int attempts = 0;
        while (!atomicInt.weakCompareAndSet(10, 20)) {
            attempts++;
            if (attempts > 100) {
                System.out.println("Too many spurious failures");
                break;
            }
        }
        
        System.out.println("Weak CAS attempts: " + attempts + ", final value: " + atomicInt.get());
    }
    
    public static void main(String[] args) {
        System.out.println("=== Basic CAS ===");
        basicCAS();
        
        System.out.println("\n=== CAS Loop ===");
        casLoop();
        
        System.out.println("\n=== CAS with Retry ===");
        casWithRetry();
        
        System.out.println("\n=== Weak CAS ===");
        weakCAS();
    }
}
```

## Atomic Classes

### Atomic Primitives

Java provides atomic wrappers for primitive types:

```java
import java.util.concurrent.atomic.*;

public class AtomicPrimitives {
    
    public static void atomicInteger() {
        AtomicInteger atomicInt = new AtomicInteger(0);
        
        // Basic operations
        System.out.println("Initial: " + atomicInt.get());
        System.out.println("Set to 10: " + atomicInt.getAndSet(10));
        System.out.println("Current: " + atomicInt.get());
        
        // Increment/Decrement
        System.out.println("Pre-increment: " + atomicInt.incrementAndGet());
        System.out.println("Post-increment: " + atomicInt.getAndIncrement());
        System.out.println("Current: " + atomicInt.get());
        
        // Add/Subtract
        System.out.println("Add 5: " + atomicInt.addAndGet(5));
        System.out.println("Get and add 3: " + atomicInt.getAndAdd(3));
        System.out.println("Current: " + atomicInt.get());
        
        // Update operations (Java 8+)
        System.out.println("Update (multiply by 2): " + 
            atomicInt.updateAndGet(x -> x * 2));
        System.out.println("Accumulate (add 10): " + 
            atomicInt.accumulateAndGet(10, Integer::sum));
    }
    
    public static void atomicLong() {
        AtomicLong atomicLong = new AtomicLong(1000000000L);
        
        // Large number operations
        System.out.println("Initial: " + atomicLong.get());
        System.out.println("Multiply by 2: " + 
            atomicLong.updateAndGet(x -> x * 2));
        System.out.println("Divide by 3: " + 
            atomicLong.updateAndGet(x -> x / 3));
    }
    
    public static void atomicBoolean() {
        AtomicBoolean flag = new AtomicBoolean(false);
        
        System.out.println("Initial: " + flag.get());
        System.out.println("Set to true: " + flag.getAndSet(true));
        System.out.println("Compare and set (true->false): " + 
            flag.compareAndSet(true, false));
        System.out.println("Current: " + flag.get());
    }
    
    public static void concurrentCounter() throws InterruptedException {
        AtomicLong counter = new AtomicLong(0);
        int numThreads = 10;
        int incrementsPerThread = 100000;
        
        Thread[] threads = new Thread[numThreads];
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.incrementAndGet();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;
        
        System.out.println("Counter value: " + counter.get());
        System.out.println("Expected: " + (numThreads * incrementsPerThread));
        System.out.println("Time taken: " + duration + "ms");
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== AtomicInteger ===");
        atomicInteger();
        
        System.out.println("\n=== AtomicLong ===");
        atomicLong();
        
        System.out.println("\n=== AtomicBoolean ===");
        atomicBoolean();
        
        System.out.println("\n=== Concurrent Counter ===");
        concurrentCounter();
    }
}
```

### Atomic Arrays

```java
import java.util.concurrent.atomic.*;

public class AtomicArrays {
    
    public static void atomicIntegerArray() {
        AtomicIntegerArray array = new AtomicIntegerArray(5);
        
        // Initialize array
        for (int i = 0; i < array.length(); i++) {
            array.set(i, i * 10);
        }
        
        System.out.println("Initial array:");
        for (int i = 0; i < array.length(); i++) {
            System.out.print(array.get(i) + " ");
        }
        System.out.println();
        
        // Atomic operations on array elements
        System.out.println("Increment element 2: " + array.incrementAndGet(2));
        System.out.println("Add 5 to element 1: " + array.addAndGet(1, 5));
        System.out.println("CAS element 0 (0->100): " + array.compareAndSet(0, 0, 100));
        
        System.out.println("Final array:");
        for (int i = 0; i < array.length(); i++) {
            System.out.print(array.get(i) + " ");
        }
        System.out.println();
    }
    
    public static void concurrentArrayAccess() throws InterruptedException {
        AtomicIntegerArray sharedArray = new AtomicIntegerArray(10);
        
        // Multiple threads updating different array elements
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    sharedArray.incrementAndGet(index);
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Array after concurrent updates:");
        for (int i = 0; i < sharedArray.length(); i++) {
            System.out.println("Element " + i + ": " + sharedArray.get(i));
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== AtomicIntegerArray ===");
        atomicIntegerArray();
        
        System.out.println("\n=== Concurrent Array Access ===");
        concurrentArrayAccess();
    }
}
```

## AtomicReference and ABA Problem

### AtomicReference Theory

**AtomicReference** provides atomic operations on object references:
1. **Reference atomicity**: Entire reference update is atomic
2. **Object immutability**: Often used with immutable objects
3. **CAS on references**: Compare and swap object references

### ABA Problem

The **ABA Problem** occurs when:
1. Thread 1 reads value A
2. Thread 2 changes A to B, then back to A
3. Thread 1's CAS succeeds, but state may have changed

```java
import java.util.concurrent.atomic.*;

public class AtomicReferenceExample {
    
    static class Node {
        final String data;
        volatile Node next;
        
        Node(String data) {
            this.data = data;
        }
        
        @Override
        public String toString() {
            return data;
        }
    }
    
    public static void basicAtomicReference() {
        AtomicReference<String> atomicRef = new AtomicReference<>("Initial");
        
        System.out.println("Initial: " + atomicRef.get());
        
        // Atomic update
        String oldValue = atomicRef.getAndSet("Updated");
        System.out.println("Old value: " + oldValue + ", New value: " + atomicRef.get());
        
        // CAS operation
        boolean success = atomicRef.compareAndSet("Updated", "Final");
        System.out.println("CAS success: " + success + ", Value: " + atomicRef.get());
    }
    
    public static void lockFreeStack() {
        AtomicReference<Node> head = new AtomicReference<>();
        
        // Push operation
        Node newNode = new Node("First");
        Node currentHead;
        do {
            currentHead = head.get();
            newNode.next = currentHead;
        } while (!head.compareAndSet(currentHead, newNode));
        
        // Push another node
        Node secondNode = new Node("Second");
        do {
            currentHead = head.get();
            secondNode.next = currentHead;
        } while (!head.compareAndSet(currentHead, secondNode));
        
        System.out.println("Stack top: " + head.get());
        System.out.println("Stack second: " + head.get().next);
        
        // Pop operation
        Node poppedNode;
        do {
            poppedNode = head.get();
            if (poppedNode == null) break;
        } while (!head.compareAndSet(poppedNode, poppedNode.next));
        
        System.out.println("Popped: " + poppedNode);
        System.out.println("New top: " + head.get());
    }
    
    public static void abaProbleDemonstration() {
        AtomicReference<Integer> atomicRef = new AtomicReference<>(1);
        
        // Thread 1: Read value, then try to CAS after delay
        Thread thread1 = new Thread(() -> {
            Integer initialValue = atomicRef.get();
            System.out.println("Thread 1 read: " + initialValue);
            
            try {
                Thread.sleep(1000); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Try to update based on old value
            boolean success = atomicRef.compareAndSet(initialValue, 10);
            System.out.println("Thread 1 CAS success: " + success + 
                ", Final value: " + atomicRef.get());
        });
        
        // Thread 2: Change value and change it back
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.println("Thread 2 changing 1 to 2");
            atomicRef.set(2);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.println("Thread 2 changing 2 back to 1");
            atomicRef.set(1);
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Solution to ABA problem using AtomicStampedReference
    public static void abaProbleSolution() {
        AtomicStampedReference<Integer> stampedRef = 
            new AtomicStampedReference<>(1, 0);
        
        Thread thread1 = new Thread(() -> {
            int[] stamp = new int[1];
            Integer initialValue = stampedRef.get(stamp);
            int initialStamp = stamp[0];
            
            System.out.println("Thread 1 read: " + initialValue + 
                ", stamp: " + initialStamp);
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // CAS with stamp check
            boolean success = stampedRef.compareAndSet(
                initialValue, 10, initialStamp, initialStamp + 1);
            System.out.println("Thread 1 CAS success: " + success);
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Change value with new stamp
            int[] stamp = new int[1];
            Integer currentValue = stampedRef.get(stamp);
            stampedRef.compareAndSet(currentValue, 2, stamp[0], stamp[0] + 1);
            System.out.println("Thread 2 changed to 2");
            
            // Change back with new stamp
            currentValue = stampedRef.get(stamp);
            stampedRef.compareAndSet(currentValue, 1, stamp[0], stamp[0] + 1);
            System.out.println("Thread 2 changed back to 1");
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Basic AtomicReference ===");
        basicAtomicReference();
        
        System.out.println("\n=== Lock-Free Stack ===");
        lockFreeStack();
        
        System.out.println("\n=== ABA Problem Demonstration ===");
        abaProbleDemonstration();
        
        System.out.println("\n=== ABA Problem Solution ===");
        abaProbleSolution();
    }
}
```

## Lock-Free Data Structures

### Lock-Free Queue Implementation

```java
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeQueue<T> {
    
    private static class Node<T> {
        volatile T data;
        volatile AtomicReference<Node<T>> next;
        
        Node(T data) {
            this.data = data;
            this.next = new AtomicReference<>(null);
        }
    }
    
    private final AtomicReference<Node<T>> head;
    private final AtomicReference<Node<T>> tail;
    
    public LockFreeQueue() {
        Node<T> dummy = new Node<>(null);
        head = new AtomicReference<>(dummy);
        tail = new AtomicReference<>(dummy);
    }
    
    public void enqueue(T item) {
        Node<T> newNode = new Node<>(item);
        
        while (true) {
            Node<T> currentTail = tail.get();
            Node<T> tailNext = currentTail.next.get();
            
            if (currentTail == tail.get()) { // Tail hasn't changed
                if (tailNext == null) {
                    // Try to link new node at end of list
                    if (currentTail.next.compareAndSet(null, newNode)) {
                        break; // Successfully linked
                    }
                } else {
                    // Try to advance tail to next node
                    tail.compareAndSet(currentTail, tailNext);
                }
            }
        }
        
        // Try to advance tail to new node
        tail.compareAndSet(tail.get(), newNode);
    }
    
    public T dequeue() {
        while (true) {
            Node<T> currentHead = head.get();
            Node<T> currentTail = tail.get();
            Node<T> headNext = currentHead.next.get();
            
            if (currentHead == head.get()) { // Head hasn't changed
                if (currentHead == currentTail) {
                    if (headNext == null) {
                        return null; // Queue is empty
                    }
                    // Try to advance tail
                    tail.compareAndSet(currentTail, headNext);
                } else {
                    if (headNext == null) {
                        continue; // Inconsistent state, retry
                    }
                    
                    T data = headNext.data;
                    
                    // Try to advance head
                    if (head.compareAndSet(currentHead, headNext)) {
                        return data;
                    }
                }
            }
        }
    }
    
    public boolean isEmpty() {
        Node<T> currentHead = head.get();
        Node<T> currentTail = tail.get();
        return currentHead == currentTail && currentHead.next.get() == null;
    }
    
    public static void testLockFreeQueue() throws InterruptedException {
        LockFreeQueue<Integer> queue = new LockFreeQueue<>();
        
        // Producer threads
        Thread[] producers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int producerId = i;
            producers[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    queue.enqueue(producerId * 100 + j);
                }
            });
            producers[i].start();
        }
        
        // Consumer threads
        Thread[] consumers = new Thread[2];
        for (int i = 0; i < 2; i++) {
            final int consumerId = i;
            consumers[i] = new Thread(() -> {
                int consumed = 0;
                while (consumed < 150) {
                    Integer item = queue.dequeue();
                    if (item != null) {
                        consumed++;
                        if (consumed % 50 == 0) {
                            System.out.println("Consumer " + consumerId + 
                                " consumed " + consumed + " items");
                        }
                    } else {
                        Thread.yield();
                    }
                }
            });
            consumers[i].start();
        }
        
        for (Thread producer : producers) {
            producer.join();
        }
        for (Thread consumer : consumers) {
            consumer.join();
        }
        
        System.out.println("Queue empty: " + queue.isEmpty());
    }
    
    public static void main(String[] args) throws InterruptedException {
        testLockFreeQueue();
    }
}
```

## Memory Ordering and Barriers

### Memory Ordering Theory

**Memory ordering** defines the order in which memory operations appear to execute:

1. **Sequential Consistency**: All operations appear in some sequential order
2. **Relaxed Ordering**: No ordering constraints
3. **Acquire-Release**: Synchronizes with other acquire-release operations
4. **Volatile**: Provides acquire-release semantics in Java

```java
import java.util.concurrent.atomic.*;

public class MemoryOrdering {
    
    // Volatile provides memory barriers
    private volatile boolean flag = false;
    private int data = 0;
    
    public void writer() {
        data = 42;        // 1. Write data
        flag = true;      // 2. Set flag (volatile write - release barrier)
    }
    
    public void reader() {
        if (flag) {       // 1. Read flag (volatile read - acquire barrier)
            System.out.println("Data: " + data); // 2. Read data (guaranteed to see 42)
        }
    }
    
    // Atomic operations provide memory ordering
    private AtomicInteger atomicCounter = new AtomicInteger(0);
    private int regularCounter = 0;
    
    public void atomicWriter() {
        regularCounter = 100;                    // 1. Regular write
        atomicCounter.incrementAndGet();         // 2. Atomic write (memory barrier)
    }
    
    public void atomicReader() {
        int atomic = atomicCounter.get();        // 1. Atomic read (memory barrier)
        int regular = regularCounter;            // 2. Regular read (sees write from step 1)
        System.out.println("Atomic: " + atomic + ", Regular: " + regular);
    }
    
    // Demonstrating memory ordering with multiple threads
    public static void memoryOrderingDemo() throws InterruptedException {
        MemoryOrdering example = new MemoryOrdering();
        
        Thread writer = new Thread(example::writer);
        Thread reader = new Thread(() -> {
            while (!example.flag) {
                Thread.yield();
            }
            example.reader();
        });
        
        reader.start();
        Thread.sleep(100); // Ensure reader starts first
        writer.start();
        
        writer.join();
        reader.join();
    }
    
    // Fence operations for explicit memory barriers
    public static void explicitBarriers() {
        AtomicInteger sharedVar = new AtomicInteger(0);
        
        // Store-Store barrier
        sharedVar.set(1);
        // VarHandle.storeStoreFence(); // Explicit barrier (Java 9+)
        sharedVar.set(2);
        
        // Load-Load barrier
        int val1 = sharedVar.get();
        // VarHandle.loadLoadFence(); // Explicit barrier (Java 9+)
        int val2 = sharedVar.get();
        
        System.out.println("Values: " + val1 + ", " + val2);
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Memory Ordering Demo ===");
        memoryOrderingDemo();
        
        System.out.println("\n=== Explicit Barriers ===");
        explicitBarriers();
    }
}
```

## Performance Comparison

```java
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

public class AtomicPerformanceTest {
    
    private static final int NUM_THREADS = 4;
    private static final int OPERATIONS_PER_THREAD = 1_000_000;
    
    // Different synchronization mechanisms
    private int unsafeCounter = 0;
    private volatile int volatileCounter = 0;
    private AtomicInteger atomicCounter = new AtomicInteger(0);
    private int synchronizedCounter = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private int lockCounter = 0;
    
    public void unsafeIncrement() {
        unsafeCounter++;
    }
    
    public void volatileIncrement() {
        volatileCounter++; // Still not atomic!
    }
    
    public void atomicIncrement() {
        atomicCounter.incrementAndGet();
    }
    
    public synchronized void synchronizedIncrement() {
        synchronizedCounter++;
    }
    
    public void lockIncrement() {
        lock.lock();
        try {
            lockCounter++;
        } finally {
            lock.unlock();
        }
    }
    
    public static void performanceTest() throws InterruptedException {
        AtomicPerformanceTest test = new AtomicPerformanceTest();
        
        // Test atomic operations
        long startTime = System.nanoTime();
        Thread[] threads = new Thread[NUM_THREADS];
        
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    test.atomicIncrement();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long atomicTime = System.nanoTime() - startTime;
        System.out.println("Atomic: " + atomicTime / 1_000_000 + "ms, " +
            "Result: " + test.atomicCounter.get());
        
        // Test synchronized operations
        startTime = System.nanoTime();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    test.synchronizedIncrement();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long syncTime = System.nanoTime() - startTime;
        System.out.println("Synchronized: " + syncTime / 1_000_000 + "ms, " +
            "Result: " + test.synchronizedCounter);
        
        // Test lock operations
        startTime = System.nanoTime();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    test.lockIncrement();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long lockTime = System.nanoTime() - startTime;
        System.out.println("ReentrantLock: " + lockTime / 1_000_000 + "ms, " +
            "Result: " + test.lockCounter);
        
        System.out.println("\nPerformance comparison:");
        System.out.println("Atomic vs Synchronized: " + 
            (double)syncTime / atomicTime + "x");
        System.out.println("Atomic vs Lock: " + 
            (double)lockTime / atomicTime + "x");
    }
    
    public static void contentionTest() throws InterruptedException {
        System.out.println("\n=== Contention Test ===");
        
        for (int numThreads = 1; numThreads <= 8; numThreads *= 2) {
            AtomicInteger counter = new AtomicInteger(0);
            
            long startTime = System.nanoTime();
            Thread[] threads = new Thread[numThreads];
            
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 1_000_000; j++) {
                        counter.incrementAndGet();
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            System.out.println("Threads: " + numThreads + 
                ", Time: " + duration + "ms, " +
                "Throughput: " + (numThreads * 1_000_000 / duration) + " ops/ms");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        performanceTest();
        contentionTest();
    }
}
```

## Best Practices

### 1. When to Use Atomic Operations

```java
public class AtomicUsageGuidelines {
    
    // GOOD: Simple counters and flags
    private AtomicInteger requestCount = new AtomicInteger(0);
    private AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    
    public void incrementRequests() {
        requestCount.incrementAndGet();
    }
    
    public boolean isShutdown() {
        return shutdownFlag.get();
    }
    
    // GOOD: Single variable updates
    private AtomicReference<String> currentStatus = new AtomicReference<>("IDLE");
    
    public void updateStatus(String newStatus) {
        currentStatus.set(newStatus);
    }
    
    // BAD: Multiple related variables (use locks instead)
    private AtomicInteger x = new AtomicInteger(0);
    private AtomicInteger y = new AtomicInteger(0);
    
    public void badMove(int dx, int dy) {
        x.addAndGet(dx); // Not atomic with respect to y update
        y.addAndGet(dy); // Race condition possible
    }
    
    // GOOD: Use locks for multiple variables
    private final Object lock = new Object();
    private int coordX = 0;
    private int coordY = 0;
    
    public void goodMove(int dx, int dy) {
        synchronized(lock) {
            coordX += dx;
            coordY += dy;
        }
    }
}
```

### 2. Avoiding Common Pitfalls

```java
public class AtomicPitfalls {
    
    private AtomicInteger counter = new AtomicInteger(0);
    
    // WRONG: Check-then-act race condition
    public void wrongIncrement() {
        if (counter.get() < 100) {
            counter.incrementAndGet(); // Race condition!
        }
    }
    
    // CORRECT: Atomic check-then-act
    public void correctIncrement() {
        int current, next;
        do {
            current = counter.get();
            if (current >= 100) return;
            next = current + 1;
        } while (!counter.compareAndSet(current, next));
    }
    
    // WRONG: Multiple atomic operations
    public void wrongDoubleIncrement() {
        counter.incrementAndGet(); // First increment
        counter.incrementAndGet(); // Second increment - not atomic together
    }
    
    // CORRECT: Single atomic operation
    public void correctDoubleIncrement() {
        counter.addAndGet(2);
    }
    
    // WRONG: Volatile for compound operations
    private volatile int volatileCounter = 0;
    
    public void wrongVolatileIncrement() {
        volatileCounter++; // Not atomic!
    }
    
    // CORRECT: Use atomic for compound operations
    private AtomicInteger atomicCounter = new AtomicInteger(0);
    
    public void correctAtomicIncrement() {
        atomicCounter.incrementAndGet();
    }
}
```

### 3. Performance Optimization

```java
public class AtomicOptimization {
    
    // Use appropriate atomic type
    private AtomicLong longCounter = new AtomicLong(0); // For large numbers
    private AtomicInteger intCounter = new AtomicInteger(0); // For small numbers
    
    // Batch operations when possible
    public void batchUpdate(int increment) {
        intCounter.addAndGet(increment); // Better than multiple incrementAndGet()
    }
    
    // Use lazySet for better performance when ordering not critical
    private AtomicReference<String> status = new AtomicReference<>();
    
    public void updateStatusLazy(String newStatus) {
        status.lazySet(newStatus); // Weaker memory ordering, better performance
    }
    
    // Use weakCompareAndSet in loops
    public void optimizedCAS(int newValue) {
        int current;
        do {
            current = intCounter.get();
        } while (!intCounter.weakCompareAndSet(current, newValue));
    }
    
    // Reduce contention with thread-local aggregation
    private final ThreadLocal<Integer> localCounter = ThreadLocal.withInitial(() -> 0);
    private final AtomicInteger globalCounter = new AtomicInteger(0);
    
    public void localIncrement() {
        localCounter.set(localCounter.get() + 1);
        
        // Periodically flush to global counter
        if (localCounter.get() % 100 == 0) {
            globalCounter.addAndGet(localCounter.get());
            localCounter.set(0);
        }
    }
}
```

## Summary

- **Atomic operations** provide thread-safe operations without locks
- **Compare-and-Swap (CAS)** is the fundamental atomic operation
- **Atomic classes** offer lock-free alternatives to synchronized blocks
- **AtomicReference** enables lock-free data structures
- **ABA problem** can be solved with stamped references
- **Memory ordering** ensures proper visibility of operations
- **Performance** is generally better than locks for simple operations
- **Use atomic operations** for single-variable updates, locks for multiple variables

## Next Steps

Continue to [Memory Model](08_Memory_Model.md) to learn about Java Memory Model and happens-before relationships.