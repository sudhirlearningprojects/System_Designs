# Can notifyAll() Be Called Outside synchronized Block?

## Quick Answer

**NO** - You CANNOT call `notifyAll()` (or `notify()` or `wait()`) outside a synchronized block.

**Result**: `IllegalMonitorStateException`

---

## Example: What Happens

```java
public class NotifyOutsideSynchronized {
    private final Object lock = new Object();
    
    public static void main(String[] args) {
        NotifyOutsideSynchronized example = new NotifyOutsideSynchronized();
        
        // ❌ WRONG - Throws IllegalMonitorStateException
        example.notifyOutsideSync();
        
        // ✅ CORRECT
        example.notifyInsideSync();
    }
    
    // ❌ WRONG - Will throw exception
    public void notifyOutsideSync() {
        try {
            lock.notifyAll(); // ERROR!
        } catch (IllegalMonitorStateException e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("Cannot call notifyAll() outside synchronized block!");
        }
    }
    
    // ✅ CORRECT
    public void notifyInsideSync() {
        synchronized (lock) {
            lock.notifyAll(); // OK
            System.out.println("SUCCESS: notifyAll() called inside synchronized block");
        }
    }
}
```

**Output**:
```
ERROR: current thread is not owner
Cannot call notifyAll() outside synchronized block!
SUCCESS: notifyAll() called inside synchronized block
```

---

## Why This Restriction Exists

### Reason: Thread Must Own the Monitor

To call `wait()`, `notify()`, or `notifyAll()`, the thread must **own the monitor** (lock) of the object.

**How to own the monitor?**
- Enter a `synchronized` block/method on that object

```java
synchronized(lock) {
    // Now current thread owns the monitor of 'lock'
    lock.notifyAll(); // OK
}
// Thread no longer owns the monitor
lock.notifyAll(); // ERROR!
```

---

## All Three Methods Require synchronized

```java
public class AllThreeRequireSync {
    private final Object lock = new Object();
    
    public void demonstrateAll() {
        // ❌ ALL THREE FAIL outside synchronized
        try {
            lock.wait();       // IllegalMonitorStateException
        } catch (Exception e) {
            System.out.println("wait() failed: " + e.getClass().getSimpleName());
        }
        
        try {
            lock.notify();     // IllegalMonitorStateException
        } catch (Exception e) {
            System.out.println("notify() failed: " + e.getClass().getSimpleName());
        }
        
        try {
            lock.notifyAll();  // IllegalMonitorStateException
        } catch (Exception e) {
            System.out.println("notifyAll() failed: " + e.getClass().getSimpleName());
        }
        
        // ✅ ALL THREE WORK inside synchronized
        synchronized (lock) {
            try {
                System.out.println("\nInside synchronized block:");
                // These would work (commented to avoid blocking)
                // lock.wait();
                lock.notify();
                lock.notifyAll();
                System.out.println("All methods work!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        new AllThreeRequireSync().demonstrateAll();
    }
}
```

**Output**:
```
wait() failed: IllegalMonitorStateException
notify() failed: IllegalMonitorStateException
notifyAll() failed: IllegalMonitorStateException

Inside synchronized block:
All methods work!
```

---

## Common Mistake: Synchronizing on Different Objects

```java
public class WrongObjectSync {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void wrongWay() {
        synchronized (lock1) {
            // ❌ WRONG - synchronized on lock1, but calling notifyAll on lock2
            try {
                lock2.notifyAll(); // IllegalMonitorStateException
            } catch (IllegalMonitorStateException e) {
                System.out.println("ERROR: Synchronized on lock1, but notifying lock2");
            }
        }
    }
    
    public void correctWay() {
        synchronized (lock1) {
            // ✅ CORRECT - synchronized on lock1, calling notifyAll on lock1
            lock1.notifyAll();
            System.out.println("SUCCESS: Same object for sync and notify");
        }
    }
    
    public static void main(String[] args) {
        WrongObjectSync example = new WrongObjectSync();
        example.wrongWay();
        example.correctWay();
    }
}
```

**Output**:
```
ERROR: Synchronized on lock1, but notifying lock2
SUCCESS: Same object for sync and notify
```

**Rule**: The object in `synchronized(obj)` must be the SAME object calling `obj.notifyAll()`

---

## Correct Patterns

### Pattern 1: synchronized Block

```java
synchronized (lock) {
    // Modify shared state
    ready = true;
    // Notify waiting threads
    lock.notifyAll();
}
```

### Pattern 2: synchronized Method

```java
public synchronized void notifyWaiters() {
    // 'this' is the lock
    this.notifyAll(); // OK
    // or simply
    notifyAll(); // OK (implicit 'this')
}
```

### Pattern 3: Static synchronized Method

```java
public static synchronized void notifyWaiters() {
    // Class object is the lock
    MyClass.class.notifyAll(); // OK
}
```

---

## Real Example: Producer-Consumer

```java
class ProducerConsumer {
    private final Object lock = new Object();
    private boolean dataReady = false;
    
    // Producer
    public void produce() {
        // Do work outside synchronized (good practice)
        String data = prepareData();
        
        // Only synchronize when modifying shared state
        synchronized (lock) {
            dataReady = true;
            lock.notifyAll(); // ✅ Inside synchronized
        }
    }
    
    // Consumer
    public void consume() throws InterruptedException {
        synchronized (lock) {
            while (!dataReady) {
                lock.wait(); // ✅ Inside synchronized
            }
            processData();
        }
    }
    
    private String prepareData() {
        return "data";
    }
    
    private void processData() {
        System.out.println("Processing data");
    }
}
```

---

## What If You Try?

```java
public class TryItYourself {
    public static void main(String[] args) {
        Object lock = new Object();
        
        System.out.println("Attempting notifyAll() outside synchronized...");
        
        try {
            lock.notifyAll();
        } catch (IllegalMonitorStateException e) {
            System.out.println("\n❌ Exception thrown!");
            System.out.println("Exception: " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("\nConclusion: notifyAll() MUST be inside synchronized block");
        }
    }
}
```

**Output**:
```
Attempting notifyAll() outside synchronized...

❌ Exception thrown!
Exception: IllegalMonitorStateException
Message: current thread is not owner
```

---

## Summary Table

| Method | Requires synchronized? | Exception if not |
|--------|----------------------|------------------|
| `wait()` | ✅ YES | IllegalMonitorStateException |
| `notify()` | ✅ YES | IllegalMonitorStateException |
| `notifyAll()` | ✅ YES | IllegalMonitorStateException |

---

## Key Takeaways

1. **notifyAll() MUST be inside synchronized block** - No exceptions
2. **Same object** - synchronized(lock) and lock.notifyAll() must use same object
3. **IllegalMonitorStateException** - Thrown if called outside synchronized
4. **All three methods** - wait(), notify(), notifyAll() have same requirement
5. **Thread must own monitor** - synchronized gives thread ownership

---

## Quick Reference

```java
// ❌ WRONG
lock.notifyAll();

// ✅ CORRECT
synchronized (lock) {
    lock.notifyAll();
}

// ✅ ALSO CORRECT (synchronized method)
public synchronized void notifyAll() {
    this.notifyAll(); // or just notifyAll()
}
```

**Bottom Line**: You **CANNOT** call `notifyAll()` outside synchronized block. It will always throw `IllegalMonitorStateException`.
