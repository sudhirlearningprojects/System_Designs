# wait(), notify(), and notifyAll() - Complete Guide

## Quick Summary

| Method | What It Does | Who Gets Notified |
|--------|-------------|-------------------|
| `wait()` | Current thread **releases lock** and **waits** | N/A |
| `notify()` | Wakes up **ONE** waiting thread (random) | 1 thread |
| `notifyAll()` | Wakes up **ALL** waiting threads | All threads |

---

## Key Concepts

### 1. Must Be Called Inside synchronized Block

```java
// ❌ WRONG - IllegalMonitorStateException
public void wrongWay() {
    wait(); // ERROR: Not in synchronized block
}

// ✅ CORRECT
public void correctWay() {
    synchronized(this) {
        wait(); // OK: Inside synchronized block
    }
}
```

### 2. wait() Releases the Lock

```java
synchronized(lock) {
    System.out.println("Before wait - I have the lock");
    lock.wait(); // Releases lock, other threads can acquire it
    System.out.println("After wait - I have the lock again");
}
```

### 3. notify() vs notifyAll()

- **notify()**: Wakes up ONE random waiting thread
- **notifyAll()**: Wakes up ALL waiting threads (they compete for lock)

---

## Example 1: Producer-Consumer with notify()

```java
import java.util.*;

class ProducerConsumerNotify {
    private Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    private final Object lock = new Object();
    
    // Producer
    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (lock) {
                // Wait if queue is full
                while (queue.size() == CAPACITY) {
                    System.out.println("Queue full, producer waiting...");
                    lock.wait(); // Release lock and wait
                }
                
                // Produce item
                System.out.println("Produced: " + value);
                queue.add(value++);
                
                // Notify ONE waiting consumer
                lock.notify();
                
                Thread.sleep(1000);
            }
        }
    }
    
    // Consumer
    public void consume() throws InterruptedException {
        while (true) {
            synchronized (lock) {
                // Wait if queue is empty
                while (queue.isEmpty()) {
                    System.out.println("Queue empty, consumer waiting...");
                    lock.wait(); // Release lock and wait
                }
                
                // Consume item
                int value = queue.poll();
                System.out.println("Consumed: " + value);
                
                // Notify ONE waiting producer
                lock.notify();
                
                Thread.sleep(1000);
            }
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumerNotify pc = new ProducerConsumerNotify();
        
        Thread producer = new Thread(() -> {
            try { pc.produce(); } catch (InterruptedException e) {}
        });
        
        Thread consumer = new Thread(() -> {
            try { pc.consume(); } catch (InterruptedException e) {}
        });
        
        producer.start();
        consumer.start();
    }
}
```

**Output**:
```
Produced: 0
Consumed: 0
Produced: 1
Consumed: 1
Produced: 2
Queue full, producer waiting...
Consumed: 2
Produced: 3
```

---

## Example 2: Multiple Consumers with notifyAll()

```java
class ProducerMultipleConsumers {
    private Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    private final Object lock = new Object();
    
    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (lock) {
                while (queue.size() == CAPACITY) {
                    lock.wait();
                }
                
                System.out.println("Produced: " + value);
                queue.add(value++);
                
                // Wake up ALL waiting consumers
                lock.notifyAll();
                
                Thread.sleep(1000);
            }
        }
    }
    
    public void consume(String consumerName) throws InterruptedException {
        while (true) {
            synchronized (lock) {
                while (queue.isEmpty()) {
                    System.out.println(consumerName + " waiting...");
                    lock.wait();
                }
                
                int value = queue.poll();
                System.out.println(consumerName + " consumed: " + value);
                
                lock.notifyAll();
                
                Thread.sleep(1500);
            }
        }
    }
    
    public static void main(String[] args) {
        ProducerMultipleConsumers pc = new ProducerMultipleConsumers();
        
        new Thread(() -> {
            try { pc.produce(); } catch (InterruptedException e) {}
        }).start();
        
        new Thread(() -> {
            try { pc.consume("Consumer-1"); } catch (InterruptedException e) {}
        }).start();
        
        new Thread(() -> {
            try { pc.consume("Consumer-2"); } catch (InterruptedException e) {}
        }).start();
        
        new Thread(() -> {
            try { pc.consume("Consumer-3"); } catch (InterruptedException e) {}
        }).start();
    }
}
```

**Output**:
```
Produced: 0
Consumer-1 consumed: 0
Consumer-2 waiting...
Consumer-3 waiting...
Produced: 1
Consumer-2 consumed: 1
Consumer-3 waiting...
Produced: 2
Consumer-3 consumed: 2
```

---

## Example 3: notify() Problem - Lost Wakeup

```java
class NotifyProblem {
    private final Object lock = new Object();
    
    public static void main(String[] args) throws InterruptedException {
        NotifyProblem np = new NotifyProblem();
        
        // Start 3 waiting threads
        Thread t1 = new Thread(() -> np.waitForSignal("Thread-1"));
        Thread t2 = new Thread(() -> np.waitForSignal("Thread-2"));
        Thread t3 = new Thread(() -> np.waitForSignal("Thread-3"));
        
        t1.start();
        t2.start();
        t3.start();
        
        Thread.sleep(1000);
        
        // notify() wakes up only ONE thread
        synchronized (np.lock) {
            System.out.println("Calling notify() - only 1 thread will wake up");
            np.lock.notify();
        }
        
        Thread.sleep(2000);
        System.out.println("Other 2 threads still waiting!");
    }
    
    public void waitForSignal(String threadName) {
        synchronized (lock) {
            try {
                System.out.println(threadName + " is waiting...");
                lock.wait();
                System.out.println(threadName + " woke up!");
            } catch (InterruptedException e) {}
        }
    }
}
```

**Output**:
```
Thread-1 is waiting...
Thread-2 is waiting...
Thread-3 is waiting...
Calling notify() - only 1 thread will wake up
Thread-2 woke up!
Other 2 threads still waiting!
```

**Problem**: Thread-1 and Thread-3 never wake up!

---

## Example 4: notifyAll() Solution

```java
class NotifyAllSolution {
    private final Object lock = new Object();
    
    public static void main(String[] args) throws InterruptedException {
        NotifyAllSolution nas = new NotifyAllSolution();
        
        // Start 3 waiting threads
        Thread t1 = new Thread(() -> nas.waitForSignal("Thread-1"));
        Thread t2 = new Thread(() -> nas.waitForSignal("Thread-2"));
        Thread t3 = new Thread(() -> nas.waitForSignal("Thread-3"));
        
        t1.start();
        t2.start();
        t3.start();
        
        Thread.sleep(1000);
        
        // notifyAll() wakes up ALL threads
        synchronized (nas.lock) {
            System.out.println("Calling notifyAll() - all threads will wake up");
            nas.lock.notifyAll();
        }
        
        Thread.sleep(1000);
        System.out.println("All threads woke up!");
    }
    
    public void waitForSignal(String threadName) {
        synchronized (lock) {
            try {
                System.out.println(threadName + " is waiting...");
                lock.wait();
                System.out.println(threadName + " woke up!");
            } catch (InterruptedException e) {}
        }
    }
}
```

**Output**:
```
Thread-1 is waiting...
Thread-2 is waiting...
Thread-3 is waiting...
Calling notifyAll() - all threads will wake up
Thread-1 woke up!
Thread-2 woke up!
Thread-3 woke up!
All threads woke up!
```

**Solution**: All threads wake up!

---

## Example 5: Real-World - Task Queue

```java
import java.util.*;

class TaskQueue {
    private Queue<String> tasks = new LinkedList<>();
    private final Object lock = new Object();
    private boolean shutdown = false;
    
    // Add task
    public void addTask(String task) {
        synchronized (lock) {
            tasks.add(task);
            System.out.println("Added task: " + task);
            
            // Wake up ONE waiting worker
            lock.notify();
        }
    }
    
    // Worker processes tasks
    public void processTask(String workerName) {
        while (true) {
            synchronized (lock) {
                try {
                    // Wait if no tasks
                    while (tasks.isEmpty() && !shutdown) {
                        System.out.println(workerName + " waiting for tasks...");
                        lock.wait();
                    }
                    
                    if (shutdown && tasks.isEmpty()) {
                        System.out.println(workerName + " shutting down");
                        break;
                    }
                    
                    // Process task
                    String task = tasks.poll();
                    System.out.println(workerName + " processing: " + task);
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // Simulate work outside synchronized block
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        }
    }
    
    // Shutdown
    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            // Wake up ALL workers to check shutdown flag
            lock.notifyAll();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        TaskQueue tq = new TaskQueue();
        
        // Start 3 workers
        Thread w1 = new Thread(() -> tq.processTask("Worker-1"));
        Thread w2 = new Thread(() -> tq.processTask("Worker-2"));
        Thread w3 = new Thread(() -> tq.processTask("Worker-3"));
        
        w1.start();
        w2.start();
        w3.start();
        
        Thread.sleep(500);
        
        // Add tasks
        tq.addTask("Task-1");
        tq.addTask("Task-2");
        tq.addTask("Task-3");
        tq.addTask("Task-4");
        tq.addTask("Task-5");
        
        Thread.sleep(6000);
        
        // Shutdown
        tq.shutdown();
    }
}
```

**Output**:
```
Worker-1 waiting for tasks...
Worker-2 waiting for tasks...
Worker-3 waiting for tasks...
Added task: Task-1
Worker-1 processing: Task-1
Added task: Task-2
Worker-2 processing: Task-2
Added task: Task-3
Worker-3 processing: Task-3
Added task: Task-4
Worker-1 processing: Task-4
Added task: Task-5
Worker-2 processing: Task-5
Worker-1 shutting down
Worker-2 shutting down
Worker-3 shutting down
```

---

## Detailed Comparison

### wait()

**Purpose**: Current thread releases lock and waits

**Behavior**:
1. Must be called inside synchronized block
2. Releases the lock
3. Thread goes to WAITING state
4. Wakes up when notify()/notifyAll() is called
5. Re-acquires lock before continuing

**Syntax**:
```java
synchronized(lock) {
    lock.wait();           // Wait indefinitely
    lock.wait(1000);       // Wait max 1 second
    lock.wait(1000, 500);  // Wait max 1.5 seconds
}
```

---

### notify()

**Purpose**: Wakes up ONE waiting thread (random selection)

**Behavior**:
1. Must be called inside synchronized block
2. Wakes up ONE thread waiting on the same lock
3. Which thread wakes up is **NOT guaranteed** (JVM decides)
4. If no threads waiting, notify() does nothing
5. Woken thread must re-acquire lock before continuing

**When to Use**:
- Only ONE thread needs to wake up
- Example: Producer-Consumer with 1 producer, 1 consumer

**Syntax**:
```java
synchronized(lock) {
    lock.notify();
}
```

---

### notifyAll()

**Purpose**: Wakes up ALL waiting threads

**Behavior**:
1. Must be called inside synchronized block
2. Wakes up ALL threads waiting on the same lock
3. All threads compete to re-acquire the lock
4. Only one gets the lock, others wait again
5. If no threads waiting, notifyAll() does nothing

**When to Use**:
- Multiple threads waiting
- Don't know which thread should wake up
- Safer than notify() (prevents lost wakeups)

**Syntax**:
```java
synchronized(lock) {
    lock.notifyAll();
}
```

---

## Common Patterns

### Pattern 1: Always Use while Loop (Not if)

```java
// ❌ WRONG - Can cause spurious wakeups
synchronized(lock) {
    if (condition) {
        lock.wait();
    }
}

// ✅ CORRECT - Handles spurious wakeups
synchronized(lock) {
    while (condition) {
        lock.wait();
    }
}
```

**Reason**: Thread can wake up without notify() (spurious wakeup)

---

### Pattern 2: Producer-Consumer Template

```java
// Producer
synchronized(lock) {
    while (queueFull) {
        lock.wait();
    }
    addToQueue();
    lock.notifyAll(); // or notify()
}

// Consumer
synchronized(lock) {
    while (queueEmpty) {
        lock.wait();
    }
    removeFromQueue();
    lock.notifyAll(); // or notify()
}
```

---

### Pattern 3: Signaling Between Threads

```java
class Signal {
    private boolean ready = false;
    private final Object lock = new Object();
    
    // Thread 1: Wait for signal
    public void waitForSignal() throws InterruptedException {
        synchronized(lock) {
            while (!ready) {
                lock.wait();
            }
            System.out.println("Signal received!");
        }
    }
    
    // Thread 2: Send signal
    public void sendSignal() {
        synchronized(lock) {
            ready = true;
            lock.notifyAll();
        }
    }
}
```

---

## notify() vs notifyAll() - When to Use

### Use notify() when:
✅ Only ONE thread should wake up  
✅ All waiting threads are equivalent (any can handle the event)  
✅ Example: Single producer, single consumer

### Use notifyAll() when:
✅ Multiple threads waiting  
✅ Different threads waiting for different conditions  
✅ Not sure which thread should wake up  
✅ **Default choice** (safer, prevents bugs)

---

## Common Mistakes

### Mistake 1: Not Using synchronized

```java
// ❌ WRONG - IllegalMonitorStateException
public void wrong() {
    lock.wait(); // ERROR!
}

// ✅ CORRECT
public void correct() {
    synchronized(lock) {
        lock.wait();
    }
}
```

---

### Mistake 2: Using if Instead of while

```java
// ❌ WRONG - Spurious wakeup problem
synchronized(lock) {
    if (queue.isEmpty()) {
        lock.wait();
    }
    process(queue.poll()); // May be null!
}

// ✅ CORRECT
synchronized(lock) {
    while (queue.isEmpty()) {
        lock.wait();
    }
    process(queue.poll()); // Safe
}
```

---

### Mistake 3: Forgetting to Check Condition After wait()

```java
// ❌ WRONG
synchronized(lock) {
    lock.wait();
    // Assumes condition is true, but may not be!
    doWork();
}

// ✅ CORRECT
synchronized(lock) {
    while (!conditionMet) {
        lock.wait();
    }
    // Now condition is guaranteed
    doWork();
}
```

---

## Modern Alternatives

### Use java.util.concurrent Instead

```java
// Old way: wait/notify
synchronized(lock) {
    while (!ready) {
        lock.wait();
    }
}

// Modern way: CountDownLatch
CountDownLatch latch = new CountDownLatch(1);
latch.await(); // Wait
latch.countDown(); // Signal

// Modern way: BlockingQueue
BlockingQueue<String> queue = new LinkedBlockingQueue<>();
queue.put("item"); // Producer
String item = queue.take(); // Consumer (blocks if empty)

// Modern way: Condition
Lock lock = new ReentrantLock();
Condition condition = lock.newCondition();
lock.lock();
try {
    condition.await(); // Like wait()
    condition.signal(); // Like notify()
    condition.signalAll(); // Like notifyAll()
} finally {
    lock.unlock();
}
```

---

## Performance Comparison

| Aspect | notify() | notifyAll() |
|--------|----------|-------------|
| Threads woken | 1 | All |
| CPU overhead | Lower | Higher |
| Context switches | Fewer | More |
| Safety | Risky | Safer |
| Use case | Simple scenarios | Complex scenarios |

---

## Key Takeaways

1. **wait()** - Releases lock and waits for notification
2. **notify()** - Wakes up ONE random waiting thread
3. **notifyAll()** - Wakes up ALL waiting threads
4. **Must use synchronized** - All three methods require lock
5. **Use while loop** - Not if, to handle spurious wakeups
6. **notifyAll() is safer** - Use by default unless sure
7. **Modern alternatives** - Consider BlockingQueue, CountDownLatch, etc.

---

## Quick Reference

```java
// Wait pattern
synchronized(lock) {
    while (condition) {
        lock.wait();
    }
}

// Notify one
synchronized(lock) {
    lock.notify();
}

// Notify all
synchronized(lock) {
    lock.notifyAll();
}
```

**Rule of Thumb**: When in doubt, use **notifyAll()** - it's safer!
