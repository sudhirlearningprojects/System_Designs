# Java Deadlock Examples

## Classic Deadlock (Minimal)

```java
public class ClassicDeadlock {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread 1: Holding lock1...");
                sleep(100);
                synchronized (lock2) {
                    System.out.println("Thread 1: Holding lock1 & lock2");
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread 2: Holding lock2...");
                sleep(100);
                synchronized (lock1) {
                    System.out.println("Thread 2: Holding lock2 & lock1");
                }
            }
        });
        
        t1.start();
        t2.start();
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Output:**
```
Thread 1: Holding lock1...
Thread 2: Holding lock2...
[DEADLOCK - Program hangs forever]
```

---

## Using ReentrantLock (Java 5+)

```java
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDeadlock {
    private static final ReentrantLock lock1 = new ReentrantLock();
    private static final ReentrantLock lock2 = new ReentrantLock();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            lock1.lock();
            System.out.println("Thread 1: Acquired lock1");
            sleep(100);
            lock2.lock();
            System.out.println("Thread 1: Acquired lock2");
            lock2.unlock();
            lock1.unlock();
        });
        
        Thread t2 = new Thread(() -> {
            lock2.lock();
            System.out.println("Thread 2: Acquired lock2");
            sleep(100);
            lock1.lock();
            System.out.println("Thread 2: Acquired lock1");
            lock1.unlock();
            lock2.unlock();
        });
        
        t1.start();
        t2.start();
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

---

## Using Virtual Threads (Java 21+)

```java
public class VirtualThreadDeadlock {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) throws InterruptedException {
        Thread vt1 = Thread.ofVirtual().start(() -> {
            synchronized (lock1) {
                System.out.println("Virtual Thread 1: Holding lock1");
                sleep(100);
                synchronized (lock2) {
                    System.out.println("Virtual Thread 1: Holding both locks");
                }
            }
        });
        
        Thread vt2 = Thread.ofVirtual().start(() -> {
            synchronized (lock2) {
                System.out.println("Virtual Thread 2: Holding lock2");
                sleep(100);
                synchronized (lock1) {
                    System.out.println("Virtual Thread 2: Holding both locks");
                }
            }
        });
        
        vt1.join();
        vt2.join();
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

---

## Using Structured Concurrency (Java 21+)

```java
import java.util.concurrent.StructuredTaskScope;

public class StructuredConcurrencyDeadlock {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) throws InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            scope.fork(() -> {
                synchronized (lock1) {
                    System.out.println("Task 1: Holding lock1");
                    Thread.sleep(100);
                    synchronized (lock2) {
                        System.out.println("Task 1: Holding both locks");
                    }
                }
                return null;
            });
            
            scope.fork(() -> {
                synchronized (lock2) {
                    System.out.println("Task 2: Holding lock2");
                    Thread.sleep(100);
                    synchronized (lock1) {
                        System.out.println("Task 2: Holding both locks");
                    }
                }
                return null;
            });
            
            scope.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Bank Transfer Deadlock (Realistic Example)

```java
class BankAccount {
    private final String id;
    private int balance;
    
    public BankAccount(String id, int balance) {
        this.id = id;
        this.balance = balance;
    }
    
    public synchronized void withdraw(int amount) {
        balance -= amount;
    }
    
    public synchronized void deposit(int amount) {
        balance += amount;
    }
    
    public void transfer(BankAccount to, int amount) {
        synchronized (this) {
            System.out.println("Locked " + this.id);
            sleep(100);
            synchronized (to) {
                System.out.println("Locked " + to.id);
                this.withdraw(amount);
                to.deposit(amount);
            }
        }
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}

public class BankDeadlock {
    public static void main(String[] args) {
        BankAccount acc1 = new BankAccount("ACC1", 1000);
        BankAccount acc2 = new BankAccount("ACC2", 1000);
        
        Thread t1 = new Thread(() -> acc1.transfer(acc2, 100));
        Thread t2 = new Thread(() -> acc2.transfer(acc1, 200));
        
        t1.start();
        t2.start();
    }
}
```

---

## Deadlock Detection

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DeadlockDetector {
    public static void main(String[] args) throws InterruptedException {
        Object lock1 = new Object();
        Object lock2 = new Object();
        
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                sleep(100);
                synchronized (lock2) {}
            }
        });
        
        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                sleep(100);
                synchronized (lock1) {}
            }
        });
        
        t1.start();
        t2.start();
        
        // Detect deadlock
        Thread.sleep(500);
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = bean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            System.out.println("DEADLOCK DETECTED!");
            ThreadInfo[] infos = bean.getThreadInfo(deadlockedThreads);
            for (ThreadInfo info : infos) {
                System.out.println(info.getThreadName() + " is deadlocked");
            }
        }
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

---

## Deadlock Prevention (Ordered Locks)

```java
public class DeadlockPrevention {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            // Always acquire locks in same order
            synchronized (lock1) {
                System.out.println("Thread 1: Holding lock1");
                sleep(100);
                synchronized (lock2) {
                    System.out.println("Thread 1: Holding both locks");
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            // Same order prevents deadlock
            synchronized (lock1) {
                System.out.println("Thread 2: Holding lock1");
                sleep(100);
                synchronized (lock2) {
                    System.out.println("Thread 2: Holding both locks");
                }
            }
        });
        
        t1.start();
        t2.start();
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

---

## Using tryLock() to Avoid Deadlock

```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class TryLockSolution {
    private static final ReentrantLock lock1 = new ReentrantLock();
    private static final ReentrantLock lock2 = new ReentrantLock();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            try {
                if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("Thread 1: Acquired lock1");
                        if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("Thread 1: Acquired both locks");
                            } finally {
                                lock2.unlock();
                            }
                        } else {
                            System.out.println("Thread 1: Could not acquire lock2");
                        }
                    } finally {
                        lock1.unlock();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        Thread t2 = new Thread(() -> {
            try {
                if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("Thread 2: Acquired lock2");
                        if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("Thread 2: Acquired both locks");
                            } finally {
                                lock1.unlock();
                            }
                        } else {
                            System.out.println("Thread 2: Could not acquire lock1");
                        }
                    } finally {
                        lock2.unlock();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        t1.start();
        t2.start();
    }
}
```

---

## Key Conditions for Deadlock

1. **Mutual Exclusion**: Resources cannot be shared
2. **Hold and Wait**: Thread holds resource while waiting for another
3. **No Preemption**: Resources cannot be forcibly taken
4. **Circular Wait**: Circular chain of threads waiting for resources

**Prevention**: Break any one of these conditions!
