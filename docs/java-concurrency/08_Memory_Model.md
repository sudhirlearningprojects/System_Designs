# Java Memory Model (JMM)

## Table of Contents
- [Introduction to Java Memory Model](#introduction-to-java-memory-model)
- [Happens-Before Relationship](#happens-before-relationship)
- [Memory Visibility](#memory-visibility)
- [Reordering and Optimization](#reordering-and-optimization)
- [Synchronization Actions](#synchronization-actions)
- [Final Fields](#final-fields)
- [Double-Checked Locking](#double-checked-locking)
- [Best Practices](#best-practices)

## Introduction to Java Memory Model

### Theoretical Foundation

The **Java Memory Model (JMM)** defines:
1. **Memory consistency**: Rules for when writes become visible to reads
2. **Ordering guarantees**: Which operations can be reordered
3. **Synchronization semantics**: How synchronization affects memory
4. **Thread interaction**: How threads communicate through memory

### Abstract Memory Model

```
Thread 1                    Thread 2
┌─────────────┐            ┌─────────────┐
│ Local Cache │            │ Local Cache │
│   x = 1     │            │   y = 2     │
└─────────────┘            └─────────────┘
      │                           │
      └─────────┬─────────────────┘
                │
        ┌───────▼────────┐
        │  Main Memory   │
        │   x = ?, y = ? │
        └────────────────┘
```

### Key Concepts

1. **Program Order**: Order of operations within a single thread
2. **Synchronization Order**: Global order of synchronization actions
3. **Happens-Before**: Partial ordering of memory operations
4. **Data Race**: Conflicting accesses not ordered by happens-before

```java
public class MemoryModelBasics {
    
    private int x = 0;
    private int y = 0;
    private volatile boolean flag = false;
    
    // Thread 1
    public void writer() {
        x = 1;          // 1. Write to x
        y = 2;          // 2. Write to y  
        flag = true;    // 3. Volatile write (memory barrier)
    }
    
    // Thread 2
    public void reader() {
        if (flag) {     // 1. Volatile read (memory barrier)
            // Due to happens-before, we're guaranteed to see:
            assert x == 1; // x write happens-before flag write
            assert y == 2; // y write happens-before flag write
        }
    }
    
    public static void demonstrateVisibility() throws InterruptedException {
        MemoryModelBasics example = new MemoryModelBasics();
        
        Thread writer = new Thread(example::writer);
        Thread reader = new Thread(() -> {
            while (!example.flag) {
                Thread.yield();
            }
            example.reader();
            System.out.println("Reader saw x=" + example.x + ", y=" + example.y);
        });
        
        reader.start();
        Thread.sleep(100); // Ensure reader starts first
        writer.start();
        
        writer.join();
        reader.join();
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateVisibility();
    }
}
```

## Happens-Before Relationship

### Happens-Before Rules

The **happens-before** relationship defines memory ordering:

1. **Program Order**: Each action happens-before every subsequent action in program order
2. **Monitor Lock**: Unlock happens-before every subsequent lock on same monitor
3. **Volatile**: Write to volatile field happens-before every subsequent read
4. **Thread Start**: Thread.start() happens-before every action in started thread
5. **Thread Join**: Every action in thread happens-before Thread.join() returns
6. **Transitivity**: If A happens-before B and B happens-before C, then A happens-before C

```java
import java.util.concurrent.CountDownLatch;

public class HappensBeforeExamples {
    
    private int data = 0;
    private volatile boolean ready = false;
    
    // Rule 1: Program Order
    public void programOrder() {
        int a = 1;      // 1
        int b = 2;      // 2 happens-before 3 (program order)
        int c = a + b;  // 3
    }
    
    // Rule 2: Monitor Lock
    private final Object lock = new Object();
    private int sharedData = 0;
    
    public void monitorLockExample() {
        // Thread 1
        new Thread(() -> {
            synchronized(lock) {
                sharedData = 42; // Write inside synchronized block
            } // Unlock happens-before subsequent lock
        }).start();
        
        // Thread 2
        new Thread(() -> {
            synchronized(lock) { // Lock happens-after unlock from Thread 1
                System.out.println(sharedData); // Guaranteed to see 42
            }
        }).start();
    }
    
    // Rule 3: Volatile
    public void volatileExample() {
        // Thread 1 (Writer)
        new Thread(() -> {
            data = 100;     // 1. Non-volatile write
            ready = true;   // 2. Volatile write happens-before volatile read
        }).start();
        
        // Thread 2 (Reader)
        new Thread(() -> {
            if (ready) {    // 1. Volatile read
                System.out.println(data); // 2. Guaranteed to see 100
            }
        }).start();
    }
    
    // Rule 4: Thread Start
    public void threadStartExample() {
        int localData = 50;
        
        Thread worker = new Thread(() -> {
            // This action happens-after Thread.start()
            System.out.println("Worker sees localData: " + localData);
        });
        
        localData = 100; // This write happens-before thread start
        worker.start();  // start() happens-before actions in worker thread
    }
    
    // Rule 5: Thread Join
    public void threadJoinExample() throws InterruptedException {
        final int[] result = new int[1];
        
        Thread worker = new Thread(() -> {
            result[0] = 200; // This write happens-before join() returns
        });
        
        worker.start();
        worker.join(); // join() happens-after all actions in worker thread
        
        System.out.println("Result: " + result[0]); // Guaranteed to see 200
    }
    
    // Rule 6: Transitivity
    public void transitivityExample() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        int[] sharedArray = new int[1];
        
        // Thread 1
        Thread t1 = new Thread(() -> {
            sharedArray[0] = 300; // A
            latch1.countDown();   // B (happens-after A)
        });
        
        // Thread 2
        Thread t2 = new Thread(() -> {
            try {
                latch1.await();   // C (happens-after B)
                sharedArray[0] += 100; // D (happens-after C)
                latch2.countDown(); // E (happens-after D)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Thread 3
        Thread t3 = new Thread(() -> {
            try {
                latch2.await();   // F (happens-after E)
                // By transitivity: A happens-before F
                System.out.println("Final value: " + sharedArray[0]); // Sees 400
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        t1.start();
        t2.start();
        t3.start();
        
        t1.join();
        t2.join();
        t3.join();
    }
    
    public static void main(String[] args) throws InterruptedException {
        HappensBeforeExamples examples = new HappensBeforeExamples();
        
        System.out.println("=== Monitor Lock Example ===");
        examples.monitorLockExample();
        Thread.sleep(100);
        
        System.out.println("=== Volatile Example ===");
        examples.volatileExample();
        Thread.sleep(100);
        
        System.out.println("=== Thread Start Example ===");
        examples.threadStartExample();
        Thread.sleep(100);
        
        System.out.println("=== Thread Join Example ===");
        examples.threadJoinExample();
        
        System.out.println("=== Transitivity Example ===");
        examples.transitivityExample();
    }
}
```

## Memory Visibility

### Visibility Problems

Without proper synchronization, writes by one thread may not be visible to other threads:

```java
public class VisibilityProblems {
    
    // Problem 1: Non-volatile field
    private boolean stopRequested = false;
    
    public void startBackgroundTask() {
        Thread backgroundThread = new Thread(() -> {
            int i = 0;
            while (!stopRequested) { // May never see the change!
                i++;
            }
            System.out.println("Background task stopped after " + i + " iterations");
        });
        backgroundThread.start();
    }
    
    public void stopBackgroundTask() {
        stopRequested = true; // Write may not be visible to background thread
    }
    
    // Solution 1: Volatile field
    private volatile boolean volatileStopRequested = false;
    
    public void startVolatileBackgroundTask() {
        Thread backgroundThread = new Thread(() -> {
            int i = 0;
            while (!volatileStopRequested) { // Will see the change
                i++;
            }
            System.out.println("Volatile background task stopped after " + i + " iterations");
        });
        backgroundThread.start();
    }
    
    public void stopVolatileBackgroundTask() {
        volatileStopRequested = true; // Write is visible to all threads
    }
    
    // Problem 2: Partial visibility
    private int a = 0;
    private int b = 0;
    private volatile boolean flag = false;
    
    public void writer() {
        a = 1;          // 1. May not be visible
        b = 2;          // 2. May not be visible
        flag = true;    // 3. Volatile write - creates memory barrier
    }
    
    public void reader() {
        if (flag) {     // Volatile read - creates memory barrier
            // Due to happens-before, both a and b writes are visible
            System.out.println("a=" + a + ", b=" + b); // Will print a=1, b=2
        }
    }
    
    // Problem 3: Word tearing (theoretical on some platforms)
    private long longValue = 0; // 64-bit value might be written in two 32-bit chunks
    
    public void writeLong() {
        longValue = 0x123456789ABCDEFL; // Might be visible partially
    }
    
    public void readLong() {
        long value = longValue; // Might read inconsistent value
        System.out.println("Long value: " + Long.toHexString(value));
    }
    
    // Solution 3: Volatile long
    private volatile long volatileLongValue = 0;
    
    public void writeVolatileLong() {
        volatileLongValue = 0x123456789ABCDEFL; // Atomic write
    }
    
    public void readVolatileLong() {
        long value = volatileLongValue; // Atomic read
        System.out.println("Volatile long value: " + Long.toHexString(value));
    }
    
    public static void demonstrateVisibilityProblem() throws InterruptedException {
        VisibilityProblems example = new VisibilityProblems();
        
        // This might run forever without volatile
        example.startBackgroundTask();
        Thread.sleep(1000);
        example.stopBackgroundTask();
        Thread.sleep(1000);
        
        // This will terminate properly
        example.startVolatileBackgroundTask();
        Thread.sleep(1000);
        example.stopVolatileBackgroundTask();
        Thread.sleep(1000);
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateVisibilityProblem();
    }
}
```

## Reordering and Optimization

### Compiler and CPU Reordering

Both compiler and CPU can reorder operations for optimization:

```java
public class ReorderingExamples {
    
    private int x = 0;
    private int y = 0;
    private int a = 0;
    private int b = 0;
    
    // Example of possible reordering
    public void thread1() {
        a = 1;  // 1. Write to a
        x = b;  // 2. Read from b (can be reordered before 1)
    }
    
    public void thread2() {
        b = 1;  // 1. Write to b  
        y = a;  // 2. Read from a (can be reordered before 1)
    }
    
    // Possible outcomes due to reordering:
    // x=0, y=0 (both reads happen before writes)
    // x=0, y=1 (thread1 read reordered, thread2 in order)
    // x=1, y=0 (thread1 in order, thread2 read reordered)
    // x=1, y=1 (both in program order)
    
    public static void demonstrateReordering() throws InterruptedException {
        for (int i = 0; i < 100000; i++) {
            ReorderingExamples example = new ReorderingExamples();
            
            Thread t1 = new Thread(example::thread1);
            Thread t2 = new Thread(example::thread2);
            
            t1.start();
            t2.start();
            
            t1.join();
            t2.join();
            
            if (example.x == 0 && example.y == 0) {
                System.out.println("Reordering detected at iteration " + i + 
                    ": x=" + example.x + ", y=" + example.y);
                break;
            }
        }
    }
    
    // Preventing reordering with volatile
    private volatile int volatileX = 0;
    private volatile int volatileY = 0;
    private volatile int volatileA = 0;
    private volatile int volatileB = 0;
    
    public void volatileThread1() {
        volatileA = 1;      // 1. Volatile write
        volatileX = volatileB; // 2. Volatile read (cannot be reordered before 1)
    }
    
    public void volatileThread2() {
        volatileB = 1;      // 1. Volatile write
        volatileY = volatileA; // 2. Volatile read (cannot be reordered before 1)
    }
    
    // Memory barriers prevent reordering
    private int data1 = 0;
    private int data2 = 0;
    private volatile boolean ready = false;
    
    public void producer() {
        data1 = 100;    // 1. Regular write
        data2 = 200;    // 2. Regular write
        ready = true;   // 3. Volatile write (store-store barrier prevents 1,2 from moving after 3)
    }
    
    public void consumer() {
        if (ready) {    // 1. Volatile read (load-load barrier prevents 2,3 from moving before 1)
            int d1 = data1; // 2. Regular read
            int d2 = data2; // 3. Regular read
            System.out.println("Consumer saw data1=" + d1 + ", data2=" + d2);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Demonstrating Reordering ===");
        demonstrateReordering();
        
        System.out.println("\n=== Producer-Consumer with Memory Barriers ===");
        ReorderingExamples example = new ReorderingExamples();
        
        Thread producer = new Thread(example::producer);
        Thread consumer = new Thread(() -> {
            while (!example.ready) {
                Thread.yield();
            }
            example.consumer();
        });
        
        consumer.start();
        Thread.sleep(100);
        producer.start();
        
        producer.join();
        consumer.join();
    }
}
```

## Synchronization Actions

### Types of Synchronization Actions

1. **Volatile read/write**
2. **Lock acquire/release**
3. **Thread start/join**
4. **Object construction completion**

```java
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizationActions {
    
    private volatile int volatileCounter = 0;
    private int regularCounter = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    // Volatile synchronization
    public void volatileSync() {
        regularCounter = 100;        // 1. Regular write
        volatileCounter = 1;         // 2. Volatile write (synchronization action)
        
        // All writes before volatile write are visible after volatile read
    }
    
    public void volatileRead() {
        int vol = volatileCounter;   // 1. Volatile read (synchronization action)
        int reg = regularCounter;    // 2. Regular read (sees write from volatileSync)
        System.out.println("Volatile: " + vol + ", Regular: " + reg);
    }
    
    // Lock synchronization
    public void lockSync() {
        lock.lock();                 // Synchronization action (acquire)
        try {
            regularCounter = 200;    // Protected by lock
        } finally {
            lock.unlock();           // Synchronization action (release)
        }
    }
    
    public void lockRead() {
        lock.lock();                 // Synchronization action (acquire)
        try {
            int value = regularCounter; // Sees writes from lockSync
            System.out.println("Lock protected value: " + value);
        } finally {
            lock.unlock();           // Synchronization action (release)
        }
    }
    
    // Thread synchronization
    public void threadSync() throws InterruptedException {
        final int[] result = new int[1];
        
        Thread worker = new Thread(() -> {
            result[0] = 300;         // Write in worker thread
        });
        
        worker.start();              // Synchronization action
        worker.join();               // Synchronization action
        
        System.out.println("Thread result: " + result[0]); // Sees write from worker
    }
    
    // Object construction synchronization
    static class ImmutableObject {
        private final int value;
        private final String name;
        
        public ImmutableObject(int value, String name) {
            this.value = value;      // Final field write
            this.name = name;        // Final field write
            // Constructor completion is synchronization action
        }
        
        public int getValue() { return value; }
        public String getName() { return name; }
    }
    
    public static void constructorSync() {
        // Object construction completion happens-before first use
        ImmutableObject obj = new ImmutableObject(42, "Test");
        
        // These reads are guaranteed to see the constructor writes
        System.out.println("Object: " + obj.getValue() + ", " + obj.getName());
    }
    
    public static void main(String[] args) throws InterruptedException {
        SynchronizationActions example = new SynchronizationActions();
        
        System.out.println("=== Volatile Synchronization ===");
        Thread t1 = new Thread(example::volatileSync);
        Thread t2 = new Thread(example::volatileRead);
        
        t1.start();
        t1.join();
        t2.start();
        t2.join();
        
        System.out.println("\n=== Lock Synchronization ===");
        Thread t3 = new Thread(example::lockSync);
        Thread t4 = new Thread(example::lockRead);
        
        t3.start();
        t3.join();
        t4.start();
        t4.join();
        
        System.out.println("\n=== Thread Synchronization ===");
        example.threadSync();
        
        System.out.println("\n=== Constructor Synchronization ===");
        constructorSync();
    }
}
```

## Final Fields

### Final Field Semantics

Final fields have special memory model guarantees:

```java
public class FinalFieldSemantics {
    
    // Properly constructed immutable object
    static class ImmutablePoint {
        private final int x;
        private final int y;
        private final String label;
        
        public ImmutablePoint(int x, int y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
            // Final field writes happen-before constructor completion
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public String getLabel() { return label; }
    }
    
    // Improperly constructed object (leaking this)
    static class ImproperlyConstructed {
        private final int value;
        private static ImproperlyConstructed instance;
        
        public ImproperlyConstructed(int value) {
            instance = this;     // BAD: Leaking 'this' before construction complete
            this.value = value;  // Final field write might not be visible to other threads
        }
        
        public int getValue() { return value; }
        public static ImproperlyConstructed getInstance() { return instance; }
    }
    
    // Safe publication through final field
    static class SafePublication {
        private final ImmutablePoint point;
        
        public SafePublication(int x, int y, String label) {
            this.point = new ImmutablePoint(x, y, label);
            // Final field write ensures point is fully constructed
        }
        
        public ImmutablePoint getPoint() { return point; }
    }
    
    // Final array - reference is final, but contents can change
    static class FinalArray {
        private final int[] array;
        
        public FinalArray(int size) {
            this.array = new int[size];
            // Array reference is final, but elements can be modified
        }
        
        public void setElement(int index, int value) {
            array[index] = value; // Modifying array contents
        }
        
        public int getElement(int index) {
            return array[index];
        }
        
        public int[] getArray() { return array; } // Returns same reference always
    }
    
    public static void demonstrateFinalFields() throws InterruptedException {
        // Test proper construction
        SafePublication publisher = new SafePublication(10, 20, "Origin");
        
        Thread reader = new Thread(() -> {
            ImmutablePoint point = publisher.getPoint();
            // Guaranteed to see fully constructed point
            System.out.println("Point: (" + point.getX() + ", " + point.getY() + 
                ") - " + point.getLabel());
        });
        
        reader.start();
        reader.join();
        
        // Test final array
        FinalArray finalArray = new FinalArray(3);
        finalArray.setElement(0, 100);
        finalArray.setElement(1, 200);
        finalArray.setElement(2, 300);
        
        Thread arrayReader = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println("Array[" + i + "] = " + finalArray.getElement(i));
            }
        });
        
        arrayReader.start();
        arrayReader.join();
    }
    
    // Final field with mutable object
    static class FinalMutable {
        private final StringBuilder buffer;
        
        public FinalMutable(String initial) {
            this.buffer = new StringBuilder(initial);
            // Reference is final, but StringBuilder is mutable
        }
        
        public void append(String text) {
            synchronized(buffer) {
                buffer.append(text); // Need synchronization for mutable operations
            }
        }
        
        public String getValue() {
            synchronized(buffer) {
                return buffer.toString();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateFinalFields();
        
        // Test final mutable
        FinalMutable finalMutable = new FinalMutable("Hello");
        
        Thread writer = new Thread(() -> {
            finalMutable.append(" World");
            finalMutable.append("!");
        });
        
        Thread reader = new Thread(() -> {
            try {
                Thread.sleep(100); // Let writer complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Final mutable: " + finalMutable.getValue());
        });
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
    }
}
```

## Double-Checked Locking

### The Problem and Solution

Double-checked locking is a common pattern that requires careful implementation:

```java
public class DoubleCheckedLocking {
    
    // BROKEN: Without volatile
    private static DoubleCheckedLocking brokenInstance;
    
    public static DoubleCheckedLocking getBrokenInstance() {
        if (brokenInstance == null) {           // 1. First check (no synchronization)
            synchronized (DoubleCheckedLocking.class) {
                if (brokenInstance == null) {   // 2. Second check (synchronized)
                    brokenInstance = new DoubleCheckedLocking(); // 3. PROBLEM: Can be reordered
                }
            }
        }
        return brokenInstance; // Might return partially constructed object
    }
    
    // CORRECT: With volatile
    private static volatile DoubleCheckedLocking correctInstance;
    
    public static DoubleCheckedLocking getCorrectInstance() {
        if (correctInstance == null) {          // 1. First check (volatile read)
            synchronized (DoubleCheckedLocking.class) {
                if (correctInstance == null) {  // 2. Second check (synchronized)
                    correctInstance = new DoubleCheckedLocking(); // 3. Volatile write
                }
            }
        }
        return correctInstance; // Always returns fully constructed object
    }
    
    // ALTERNATIVE: Initialization-on-demand holder
    private static class Holder {
        static final DoubleCheckedLocking INSTANCE = new DoubleCheckedLocking();
    }
    
    public static DoubleCheckedLocking getHolderInstance() {
        return Holder.INSTANCE; // Thread-safe lazy initialization
    }
    
    // ALTERNATIVE: Enum singleton (best practice)
    public enum SingletonEnum {
        INSTANCE;
        
        public void doSomething() {
            System.out.println("Enum singleton method");
        }
    }
    
    private final String data;
    private final long timestamp;
    
    private DoubleCheckedLocking() {
        // Simulate expensive initialization
        this.data = "Initialized at " + System.currentTimeMillis();
        this.timestamp = System.nanoTime();
        
        try {
            Thread.sleep(100); // Simulate initialization work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public String getData() { return data; }
    public long getTimestamp() { return timestamp; }
    
    public static void testDoubleCheckedLocking() throws InterruptedException {
        Thread[] threads = new Thread[10];
        
        // Test correct implementation
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                DoubleCheckedLocking instance = getCorrectInstance();
                System.out.println("Thread " + Thread.currentThread().getName() + 
                    " got instance: " + instance.getData());
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Test holder pattern
        System.out.println("\n=== Holder Pattern ===");
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                DoubleCheckedLocking instance = getHolderInstance();
                System.out.println("Holder instance: " + instance.getData());
            });
            threads[i].start();
        }
        
        for (int i = 0; i < 5; i++) {
            threads[i].join();
        }
        
        // Test enum singleton
        System.out.println("\n=== Enum Singleton ===");
        SingletonEnum.INSTANCE.doSomething();
    }
    
    public static void main(String[] args) throws InterruptedException {
        testDoubleCheckedLocking();
    }
}
```

## Best Practices

### 1. Prefer High-Level Constructs

```java
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryModelBestPractices {
    
    // GOOD: Use concurrent collections
    private final java.util.concurrent.ConcurrentHashMap<String, String> cache = 
        new java.util.concurrent.ConcurrentHashMap<>();
    
    // GOOD: Use atomic references
    private final AtomicReference<String> status = new AtomicReference<>("IDLE");
    
    // GOOD: Use appropriate locks
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private String data = "";
    
    public String readData() {
        rwLock.readLock().lock();
        try {
            return data;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void writeData(String newData) {
        rwLock.writeLock().lock();
        try {
            data = newData;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    // GOOD: Immutable objects
    public static final class ImmutableConfig {
        private final String host;
        private final int port;
        private final boolean ssl;
        
        public ImmutableConfig(String host, int port, boolean ssl) {
            this.host = host;
            this.port = port;
            this.ssl = ssl;
        }
        
        public String getHost() { return host; }
        public int getPort() { return port; }
        public boolean isSsl() { return ssl; }
    }
}
```

### 2. Avoid Common Pitfalls

```java
public class MemoryModelPitfalls {
    
    // BAD: Relying on non-volatile fields for coordination
    private boolean stopFlag = false; // Should be volatile
    
    // BAD: Double-checked locking without volatile
    private static MemoryModelPitfalls instance;
    
    public static MemoryModelPitfalls getBadInstance() {
        if (instance == null) {
            synchronized (MemoryModelPitfalls.class) {
                if (instance == null) {
                    instance = new MemoryModelPitfalls(); // Broken without volatile
                }
            }
        }
        return instance;
    }
    
    // BAD: Assuming operations are atomic when they're not
    private long counter = 0; // 64-bit operations might not be atomic
    
    public void badIncrement() {
        counter++; // Not atomic on all platforms
    }
    
    // GOOD: Proper volatile usage
    private volatile boolean volatileStopFlag = false;
    
    // GOOD: Proper double-checked locking
    private static volatile MemoryModelPitfalls volatileInstance;
    
    public static MemoryModelPitfalls getGoodInstance() {
        if (volatileInstance == null) {
            synchronized (MemoryModelPitfalls.class) {
                if (volatileInstance == null) {
                    volatileInstance = new MemoryModelPitfalls();
                }
            }
        }
        return volatileInstance;
    }
    
    // GOOD: Atomic operations
    private final java.util.concurrent.atomic.AtomicLong atomicCounter = 
        new java.util.concurrent.atomic.AtomicLong(0);
    
    public void goodIncrement() {
        atomicCounter.incrementAndGet();
    }
}
```

## Summary

- **Java Memory Model** defines memory consistency and ordering guarantees
- **Happens-before** relationship establishes memory ordering between operations
- **Memory visibility** requires proper synchronization to ensure writes are seen
- **Reordering** by compiler and CPU can affect program behavior
- **Synchronization actions** create happens-before relationships
- **Final fields** have special guarantees for safe publication
- **Double-checked locking** requires volatile for correctness
- **Use high-level constructs** instead of low-level memory model details

## Next Steps

Continue to [Lock-Free Programming](09_Lock_Free_Programming.md) to learn advanced non-blocking algorithms and data structures.