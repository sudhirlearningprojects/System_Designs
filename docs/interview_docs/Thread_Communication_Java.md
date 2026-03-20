# Thread Communication in Java: Complete Guide

## Overview

Thread communication in Java refers to mechanisms that allow threads to coordinate their actions and share data safely. This is essential for building concurrent applications where multiple threads need to work together.

---

## 1. wait(), notify(), and notifyAll() - Classic Approach

### Basic Mechanism
These methods are defined in the `Object` class and must be called within a synchronized block.

```java
class SharedResource {
    private int data;
    private boolean available = false;
    
    public synchronized void produce(int value) throws InterruptedException {
        while (available) {
            wait(); // Release lock and wait
        }
        data = value;
        available = true;
        System.out.println("Produced: " + value);
        notify(); // Wake up one waiting thread
    }
    
    public synchronized int consume() throws InterruptedException {
        while (!available) {
            wait(); // Release lock and wait
        }
        available = false;
        System.out.println("Consumed: " + data);
        notifyAll(); // Wake up all waiting threads
        return data;
    }
}
```

### Producer-Consumer Example
```java
class ProducerConsumerExample {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();
        
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    resource.produce(i);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    resource.consume();
                    Thread.sleep(1500);
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

### Key Points
- **wait()**: Releases lock and waits until notify/notifyAll
- **notify()**: Wakes up one waiting thread
- **notifyAll()**: Wakes up all waiting threads
- Must be called within synchronized block
- Always use while loop, not if, to check condition (spurious wakeups)

---

## 2. BlockingQueue - Modern Approach

### Using ArrayBlockingQueue
```java
import java.util.concurrent.*;

class BlockingQueueExample {
    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        
        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put(i); // Blocks if queue is full
                    System.out.println("Produced: " + i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    Integer value = queue.take(); // Blocks if queue is empty
                    System.out.println("Consumed: " + value);
                    Thread.sleep(1000);
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

### BlockingQueue Implementations
```java
// Bounded queue
BlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(100);

// Unbounded queue
BlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>();

// Priority queue
BlockingQueue<Task> priorityQueue = new PriorityBlockingQueue<>();

// Synchronous handoff (zero capacity)
BlockingQueue<String> syncQueue = new SynchronousQueue<>();

// Delayed elements
BlockingQueue<Delayed> delayQueue = new DelayQueue<>();
```

---

## 3. CountDownLatch - Wait for Multiple Threads

### Basic Usage
```java
import java.util.concurrent.CountDownLatch;

class CountDownLatchExample {
    public static void main(String[] args) throws InterruptedException {
        int workerCount = 3;
        CountDownLatch latch = new CountDownLatch(workerCount);
        
        for (int i = 1; i <= workerCount; i++) {
            int workerId = i;
            new Thread(() -> {
                System.out.println("Worker " + workerId + " starting");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Worker " + workerId + " finished");
                latch.countDown(); // Decrement count
            }).start();
        }
        
        System.out.println("Main thread waiting for workers...");
        latch.await(); // Wait until count reaches zero
        System.out.println("All workers completed!");
    }
}
```

### Real-World Example: Service Initialization
```java
class ServiceInitializer {
    private final CountDownLatch latch;
    
    public ServiceInitializer(int serviceCount) {
        this.latch = new CountDownLatch(serviceCount);
    }
    
    public void startService(String serviceName) {
        new Thread(() -> {
            try {
                System.out.println("Initializing " + serviceName);
                Thread.sleep(2000);
                System.out.println(serviceName + " ready");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }).start();
    }
    
    public void waitForAllServices() throws InterruptedException {
        latch.await();
        System.out.println("All services initialized!");
    }
    
    public static void main(String[] args) throws InterruptedException {
        ServiceInitializer initializer = new ServiceInitializer(3);
        initializer.startService("Database");
        initializer.startService("Cache");
        initializer.startService("MessageQueue");
        initializer.waitForAllServices();
    }
}
```

---

## 4. CyclicBarrier - Synchronization Point

### Basic Usage
```java
import java.util.concurrent.CyclicBarrier;

class CyclicBarrierExample {
    public static void main(String[] args) {
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            System.out.println("All threads reached barrier, continuing...");
        });
        
        for (int i = 1; i <= parties; i++) {
            int threadId = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + threadId + " doing work");
                    Thread.sleep(threadId * 1000);
                    System.out.println("Thread " + threadId + " waiting at barrier");
                    barrier.await(); // Wait for all threads
                    System.out.println("Thread " + threadId + " continuing");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

### Multi-Phase Computation
```java
class ParallelComputation {
    public static void main(String[] args) {
        int workers = 4;
        CyclicBarrier barrier = new CyclicBarrier(workers);
        
        for (int i = 0; i < workers; i++) {
            int workerId = i;
            new Thread(() -> {
                try {
                    // Phase 1
                    System.out.println("Worker " + workerId + " - Phase 1");
                    Thread.sleep(1000);
                    barrier.await();
                    
                    // Phase 2
                    System.out.println("Worker " + workerId + " - Phase 2");
                    Thread.sleep(1000);
                    barrier.await();
                    
                    // Phase 3
                    System.out.println("Worker " + workerId + " - Phase 3");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

---

## 5. Semaphore - Resource Access Control

### Basic Usage
```java
import java.util.concurrent.Semaphore;

class SemaphoreExample {
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(3); // 3 permits
        
        for (int i = 1; i <= 10; i++) {
            int taskId = i;
            new Thread(() -> {
                try {
                    System.out.println("Task " + taskId + " waiting for permit");
                    semaphore.acquire(); // Acquire permit
                    System.out.println("Task " + taskId + " got permit");
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " releasing permit");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release(); // Release permit
                }
            }).start();
        }
    }
}
```

### Connection Pool Example
```java
class ConnectionPool {
    private final Semaphore semaphore;
    private final int maxConnections;
    
    public ConnectionPool(int maxConnections) {
        this.maxConnections = maxConnections;
        this.semaphore = new Semaphore(maxConnections);
    }
    
    public Connection getConnection() throws InterruptedException {
        semaphore.acquire();
        return new Connection();
    }
    
    public void releaseConnection(Connection conn) {
        conn.close();
        semaphore.release();
    }
    
    static class Connection {
        public void close() {
            System.out.println("Connection closed");
        }
    }
}
```

---

## 6. Exchanger - Thread Pair Communication

### Basic Usage
```java
import java.util.concurrent.Exchanger;

class ExchangerExample {
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger<>();
        
        // Thread 1
        new Thread(() -> {
            try {
                String data = "Data from Thread 1";
                System.out.println("Thread 1 sending: " + data);
                String received = exchanger.exchange(data);
                System.out.println("Thread 1 received: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Thread 2
        new Thread(() -> {
            try {
                String data = "Data from Thread 2";
                System.out.println("Thread 2 sending: " + data);
                String received = exchanger.exchange(data);
                System.out.println("Thread 2 received: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

### Buffer Exchange Pattern
```java
class BufferExchanger {
    public static void main(String[] args) {
        Exchanger<List<Integer>> exchanger = new Exchanger<>();
        
        // Producer
        new Thread(() -> {
            List<Integer> buffer = new ArrayList<>();
            try {
                for (int i = 0; i < 100; i++) {
                    buffer.add(i);
                    if (buffer.size() == 10) {
                        System.out.println("Producer exchanging buffer");
                        buffer = exchanger.exchange(buffer);
                        buffer.clear();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Consumer
        new Thread(() -> {
            List<Integer> buffer = new ArrayList<>();
            try {
                for (int i = 0; i < 10; i++) {
                    buffer = exchanger.exchange(buffer);
                    System.out.println("Consumer received: " + buffer);
                    buffer.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

---

## 7. Phaser - Flexible Synchronization

### Basic Usage
```java
import java.util.concurrent.Phaser;

class PhaserExample {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(1); // Register main thread
        
        for (int i = 1; i <= 3; i++) {
            int taskId = i;
            phaser.register(); // Register each thread
            new Thread(() -> {
                System.out.println("Task " + taskId + " - Phase 1");
                phaser.arriveAndAwaitAdvance(); // Wait for all
                
                System.out.println("Task " + taskId + " - Phase 2");
                phaser.arriveAndAwaitAdvance();
                
                System.out.println("Task " + taskId + " - Phase 3");
                phaser.arriveAndDeregister(); // Deregister
            }).start();
        }
        
        phaser.arriveAndDeregister(); // Main thread deregisters
    }
}
```

### Dynamic Party Registration
```java
class DynamicPhaser {
    public static void main(String[] args) {
        Phaser phaser = new Phaser() {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.println("Phase " + phase + " completed");
                return phase >= 2 || registeredParties == 0;
            }
        };
        
        phaser.register(); // Register main
        
        for (int i = 0; i < 3; i++) {
            phaser.register();
            new Thread(new Task(phaser, i)).start();
        }
        
        phaser.arriveAndDeregister();
    }
    
    static class Task implements Runnable {
        private final Phaser phaser;
        private final int id;
        
        Task(Phaser phaser, int id) {
            this.phaser = phaser;
            this.id = id;
        }
        
        @Override
        public void run() {
            while (!phaser.isTerminated()) {
                System.out.println("Task " + id + " phase " + phaser.getPhase());
                phaser.arriveAndAwaitAdvance();
            }
        }
    }
}
```

---

## 8. CompletableFuture - Asynchronous Communication

### Basic Chaining
```java
import java.util.concurrent.CompletableFuture;

class CompletableFutureExample {
    public static void main(String[] args) {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread 1: Fetching data");
            return "Data";
        })
        .thenApply(data -> {
            System.out.println("Thread 2: Processing " + data);
            return data.toUpperCase();
        })
        .thenAccept(result -> {
            System.out.println("Thread 3: Result = " + result);
        })
        .join();
    }
}
```

### Combining Multiple Futures
```java
class CombiningFutures {
    public static void main(String[] args) {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Result1";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            return "Result2";
        });
        
        // Wait for both
        CompletableFuture<Void> combined = CompletableFuture.allOf(future1, future2);
        combined.thenRun(() -> {
            System.out.println("Both completed");
            System.out.println(future1.join() + ", " + future2.join());
        }).join();
        
        // Wait for any
        CompletableFuture<Object> any = CompletableFuture.anyOf(future1, future2);
        System.out.println("First result: " + any.join());
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

---

## 9. Volatile Variables - Visibility Guarantee

### Basic Usage
```java
class VolatileExample {
    private volatile boolean running = true;
    
    public void start() {
        new Thread(() -> {
            while (running) {
                // Do work
            }
            System.out.println("Thread stopped");
        }).start();
    }
    
    public void stop() {
        running = false; // Visible to other threads immediately
    }
}
```

### Double-Checked Locking
```java
class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

---

## 10. Atomic Variables - Lock-Free Communication

### AtomicInteger Example
```java
import java.util.concurrent.atomic.AtomicInteger;

class AtomicExample {
    private AtomicInteger counter = new AtomicInteger(0);
    
    public void increment() {
        counter.incrementAndGet();
    }
    
    public int get() {
        return counter.get();
    }
    
    public static void main(String[] args) throws InterruptedException {
        AtomicExample example = new AtomicExample();
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    example.increment();
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        System.out.println("Counter: " + example.get()); // 10000
    }
}
```

### Compare-And-Swap
```java
class CASExample {
    private AtomicInteger value = new AtomicInteger(0);
    
    public void update(int newValue) {
        int current;
        do {
            current = value.get();
        } while (!value.compareAndSet(current, newValue));
    }
}
```

---

## 11. Thread.join() - Wait for Completion

### Basic Usage
```java
class JoinExample {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("Worker starting");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Worker finished");
        });
        
        worker.start();
        System.out.println("Main waiting for worker");
        worker.join(); // Wait for worker to complete
        System.out.println("Main continuing");
    }
}
```

### Multiple Thread Join
```java
class MultipleJoinExample {
    public static void main(String[] args) throws InterruptedException {
        Thread[] workers = new Thread[5];
        
        for (int i = 0; i < 5; i++) {
            int id = i;
            workers[i] = new Thread(() -> {
                System.out.println("Worker " + id + " working");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Worker " + id + " done");
            });
            workers[i].start();
        }
        
        // Wait for all workers
        for (Thread worker : workers) {
            worker.join();
        }
        
        System.out.println("All workers completed");
    }
}
```

---

## 12. Piped Streams - Direct Thread Communication

### PipedInputStream/PipedOutputStream
```java
import java.io.*;

class PipedStreamExample {
    public static void main(String[] args) throws IOException {
        PipedOutputStream output = new PipedOutputStream();
        PipedInputStream input = new PipedInputStream(output);
        
        // Writer thread
        new Thread(() -> {
            try (PrintWriter writer = new PrintWriter(output)) {
                for (int i = 1; i <= 5; i++) {
                    writer.println("Message " + i);
                    writer.flush();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Reader thread
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Received: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
```

---

## Comparison Table

| Mechanism | Use Case | Reusable | Capacity | Blocking |
|-----------|----------|----------|----------|----------|
| wait/notify | Simple coordination | Yes | N/A | Yes |
| BlockingQueue | Producer-Consumer | Yes | Limited/Unlimited | Yes |
| CountDownLatch | Wait for N threads | No | N/A | Yes |
| CyclicBarrier | Synchronization point | Yes | N/A | Yes |
| Semaphore | Resource limiting | Yes | N permits | Yes |
| Exchanger | Pair exchange | Yes | 1 pair | Yes |
| Phaser | Multi-phase sync | Yes | Dynamic | Yes |
| CompletableFuture | Async pipelines | Yes | N/A | Optional |
| volatile | Visibility | Yes | N/A | No |
| Atomic | Lock-free updates | Yes | N/A | No |
| join() | Wait for thread | Yes | N/A | Yes |
| Piped Streams | Stream data | Yes | Buffer | Yes |

---

## Best Practices

1. **Prefer high-level constructs**: Use BlockingQueue over wait/notify
2. **Use immutable objects**: Reduces synchronization needs
3. **Minimize lock scope**: Hold locks for shortest time possible
4. **Avoid nested locks**: Prevents deadlocks
5. **Use timeout versions**: `await(timeout)` instead of `await()`
6. **Handle InterruptedException**: Always restore interrupt status
7. **Use try-finally**: Ensure locks/resources are released
8. **Consider lock-free alternatives**: Atomic variables when possible

---

## Interview Questions

**Q: Difference between wait() and sleep()?**
- wait() releases lock, sleep() doesn't
- wait() must be in synchronized block
- wait() can be woken by notify(), sleep() can't

**Q: When to use CountDownLatch vs CyclicBarrier?**
- CountDownLatch: One-time event, threads wait for count to reach zero
- CyclicBarrier: Reusable, threads wait for each other at barrier

**Q: Why use BlockingQueue over wait/notify?**
- Simpler API, less error-prone
- Built-in thread safety
- Multiple implementations for different needs

**Q: What is spurious wakeup?**
- Thread wakes from wait() without notify/notifyAll
- Always use while loop to check condition

**Q: Volatile vs Synchronized?**
- volatile: Visibility only, no atomicity
- synchronized: Both visibility and atomicity, mutual exclusion
