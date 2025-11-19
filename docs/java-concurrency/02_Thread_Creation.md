# Thread Creation and Management

## Table of Contents
- [Creating Threads](#creating-threads)
- [Thread Class vs Runnable Interface](#thread-class-vs-runnable-interface)
- [Thread Properties](#thread-properties)
- [Thread Control Methods](#thread-control-methods)
- [Thread Groups](#thread-groups)
- [Daemon Threads](#daemon-threads)
- [Exception Handling](#exception-handling)

## Creating Threads

Java provides multiple ways to create and start threads:

### 1. Extending Thread Class

```java
public class MyThread extends Thread {
    private String threadName;
    
    public MyThread(String name) {
        this.threadName = name;
    }
    
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(threadName + " - Count: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println(threadName + " finished");
    }
    
    public static void main(String[] args) {
        MyThread thread1 = new MyThread("Thread-1");
        MyThread thread2 = new MyThread("Thread-2");
        
        thread1.start(); // Don't call run() directly!
        thread2.start();
    }
}
```

### 2. Implementing Runnable Interface

```java
public class MyRunnable implements Runnable {
    private String taskName;
    
    public MyRunnable(String name) {
        this.taskName = name;
    }
    
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(taskName + " - Count: " + i + 
                " [Thread: " + Thread.currentThread().getName() + "]");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    public static void main(String[] args) {
        Thread thread1 = new Thread(new MyRunnable("Task-1"), "Worker-1");
        Thread thread2 = new Thread(new MyRunnable("Task-2"), "Worker-2");
        
        thread1.start();
        thread2.start();
    }
}
```

### 3. Using Lambda Expressions (Java 8+)

```java
public class LambdaThreadExample {
    public static void main(String[] args) {
        // Simple lambda
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Lambda Thread - " + i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        // Lambda with method reference
        Thread thread2 = new Thread(LambdaThreadExample::printNumbers);
        
        thread1.start();
        thread2.start();
    }
    
    private static void printNumbers() {
        for (int i = 10; i < 15; i++) {
            System.out.println("Method Reference Thread - " + i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
```

### 4. Using Callable and Future

```java
import java.util.concurrent.*;

public class CallableExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Callable returns a value
        Callable<Integer> task1 = () -> {
            Thread.sleep(2000);
            return 42;
        };
        
        Callable<String> task2 = () -> {
            Thread.sleep(1000);
            return "Hello from Callable";
        };
        
        // Submit tasks and get Future objects
        Future<Integer> future1 = executor.submit(task1);
        Future<String> future2 = executor.submit(task2);
        
        // Get results (blocking calls)
        System.out.println("Result 1: " + future1.get());
        System.out.println("Result 2: " + future2.get());
        
        executor.shutdown();
    }
}
```

## Thread Class vs Runnable Interface

### When to Use Thread Class
- When you need to override other Thread methods
- Simple scenarios with no inheritance requirements

### When to Use Runnable Interface (Preferred)
- Better design (composition over inheritance)
- Class can extend another class
- Task can be reused with different threads
- More flexible and testable

### Comparison Example

```java
public class ThreadVsRunnable {
    
    // Thread class approach
    static class CounterThread extends Thread {
        private int count = 0;
        
        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                count++;
            }
        }
        
        public int getCount() { return count; }
    }
    
    // Runnable approach
    static class CounterTask implements Runnable {
        private int count = 0;
        
        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                count++;
            }
        }
        
        public int getCount() { return count; }
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Thread approach
        CounterThread thread1 = new CounterThread();
        thread1.start();
        thread1.join();
        System.out.println("Thread count: " + thread1.getCount());
        
        // Runnable approach
        CounterTask task = new CounterTask();
        Thread thread2 = new Thread(task);
        thread2.start();
        thread2.join();
        System.out.println("Runnable count: " + task.getCount());
    }
}
```

## Thread Properties

### Thread Name
```java
public class ThreadProperties {
    public static void main(String[] args) {
        Thread currentThread = Thread.currentThread();
        System.out.println("Current thread: " + currentThread.getName());
        
        Thread worker = new Thread(() -> {
            System.out.println("Worker thread: " + Thread.currentThread().getName());
        }, "MyWorkerThread");
        
        worker.start();
        
        // Change thread name
        worker.setName("RenamedWorker");
        System.out.println("New name: " + worker.getName());
    }
}
```

### Thread Priority
```java
public class ThreadPriorityExample {
    public static void main(String[] args) {
        Thread lowPriority = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Low priority: " + i);
                Thread.yield(); // Hint to scheduler
            }
        });
        
        Thread highPriority = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("High priority: " + i);
                Thread.yield();
            }
        });
        
        lowPriority.setPriority(Thread.MIN_PRIORITY);   // 1
        highPriority.setPriority(Thread.MAX_PRIORITY);  // 10
        
        System.out.println("Low priority: " + lowPriority.getPriority());
        System.out.println("High priority: " + highPriority.getPriority());
        
        lowPriority.start();
        highPriority.start();
    }
}
```

### Thread ID and State
```java
public class ThreadInfo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        System.out.println("Thread ID: " + worker.getId());
        System.out.println("Thread State: " + worker.getState());
        System.out.println("Is Alive: " + worker.isAlive());
        
        worker.start();
        
        System.out.println("After start - State: " + worker.getState());
        System.out.println("Is Alive: " + worker.isAlive());
        
        Thread.sleep(100);
        System.out.println("During sleep - State: " + worker.getState());
        
        worker.join();
        System.out.println("After join - State: " + worker.getState());
        System.out.println("Is Alive: " + worker.isAlive());
    }
}
```

## Thread Control Methods

### join() - Wait for Thread Completion
```java
public class JoinExample {
    public static void main(String[] args) throws InterruptedException {
        Thread worker1 = new Thread(() -> {
            try {
                Thread.sleep(2000);
                System.out.println("Worker 1 completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread worker2 = new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("Worker 2 completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        System.out.println("Starting workers...");
        worker1.start();
        worker2.start();
        
        // Wait for both threads to complete
        worker1.join();
        worker2.join();
        
        System.out.println("All workers completed");
    }
}
```

### join(timeout) - Wait with Timeout
```java
public class JoinTimeoutExample {
    public static void main(String[] args) throws InterruptedException {
        Thread longRunningTask = new Thread(() -> {
            try {
                Thread.sleep(5000); // 5 seconds
                System.out.println("Long task completed");
            } catch (InterruptedException e) {
                System.out.println("Long task interrupted");
                Thread.currentThread().interrupt();
            }
        });
        
        longRunningTask.start();
        
        // Wait for maximum 2 seconds
        longRunningTask.join(2000);
        
        if (longRunningTask.isAlive()) {
            System.out.println("Task is still running, interrupting...");
            longRunningTask.interrupt();
        }
    }
}
```

### sleep() - Pause Thread Execution
```java
public class SleepExample {
    public static void main(String[] args) {
        Thread timer = new Thread(() -> {
            for (int i = 5; i >= 1; i--) {
                System.out.println("Countdown: " + i);
                try {
                    Thread.sleep(1000); // Sleep for 1 second
                } catch (InterruptedException e) {
                    System.out.println("Timer interrupted");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            System.out.println("Time's up!");
        });
        
        timer.start();
        
        // Interrupt after 3 seconds
        try {
            Thread.sleep(3000);
            timer.interrupt();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### yield() - Hint to Scheduler
```java
public class YieldExample {
    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Producing: " + i);
                Thread.yield(); // Give other threads a chance
            }
        });
        
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Consuming: " + i);
                Thread.yield(); // Give other threads a chance
            }
        });
        
        producer.start();
        consumer.start();
    }
}
```

### interrupt() - Interrupt Thread
```java
public class InterruptExample {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("Working...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted during sleep");
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                }
            }
            System.out.println("Worker stopped");
        });
        
        worker.start();
        
        // Let it work for 3 seconds
        Thread.sleep(3000);
        
        // Interrupt the worker
        worker.interrupt();
        
        worker.join();
        System.out.println("Main thread finished");
    }
}
```

## Thread Groups

Thread groups provide a way to organize threads hierarchically:

```java
public class ThreadGroupExample {
    public static void main(String[] args) throws InterruptedException {
        ThreadGroup mainGroup = new ThreadGroup("MainGroup");
        ThreadGroup subGroup = new ThreadGroup(mainGroup, "SubGroup");
        
        Thread thread1 = new Thread(mainGroup, () -> {
            try {
                Thread.sleep(2000);
                System.out.println("Thread 1 in main group");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-1");
        
        Thread thread2 = new Thread(subGroup, () -> {
            try {
                Thread.sleep(2000);
                System.out.println("Thread 2 in sub group");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-2");
        
        thread1.start();
        thread2.start();
        
        // Group information
        System.out.println("Main group active threads: " + mainGroup.activeCount());
        System.out.println("Main group active groups: " + mainGroup.activeGroupCount());
        
        // List all threads in group
        Thread[] threads = new Thread[mainGroup.activeCount()];
        mainGroup.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println("Thread: " + t.getName() + 
                    ", Group: " + t.getThreadGroup().getName());
            }
        }
        
        thread1.join();
        thread2.join();
    }
}
```

## Daemon Threads

Daemon threads are background threads that don't prevent JVM from exiting:

```java
public class DaemonThreadExample {
    public static void main(String[] args) throws InterruptedException {
        Thread userThread = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println("User thread: " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        Thread daemonThread = new Thread(() -> {
            while (true) {
                System.out.println("Daemon thread running...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        // Set as daemon before starting
        daemonThread.setDaemon(true);
        
        System.out.println("User thread daemon: " + userThread.isDaemon());
        System.out.println("Daemon thread daemon: " + daemonThread.isDaemon());
        
        userThread.start();
        daemonThread.start();
        
        userThread.join();
        System.out.println("User thread finished, JVM will exit");
        // Daemon thread will be terminated when JVM exits
    }
}
```

### Common Daemon Thread Use Cases
```java
public class DaemonUseCase {
    public static void main(String[] args) throws InterruptedException {
        // Garbage collection simulation
        Thread gcSimulator = new Thread(() -> {
            while (true) {
                System.out.println("GC: Cleaning up memory...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        gcSimulator.setDaemon(true);
        gcSimulator.start();
        
        // Log cleanup daemon
        Thread logCleaner = new Thread(() -> {
            while (true) {
                System.out.println("LOG: Rotating log files...");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        logCleaner.setDaemon(true);
        logCleaner.start();
        
        // Main application work
        for (int i = 0; i < 5; i++) {
            System.out.println("Main application work: " + i);
            Thread.sleep(2000);
        }
        
        System.out.println("Application finished");
        // Daemon threads will be terminated automatically
    }
}
```

## Exception Handling

### Uncaught Exception Handler
```java
public class UncaughtExceptionExample {
    public static void main(String[] args) {
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Uncaught exception in thread " + 
                thread.getName() + ": " + exception.getMessage());
            exception.printStackTrace();
        });
        
        Thread faultyThread = new Thread(() -> {
            System.out.println("About to throw exception...");
            throw new RuntimeException("Something went wrong!");
        });
        
        // Set specific handler for this thread
        faultyThread.setUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Specific handler - Thread: " + thread.getName() + 
                ", Exception: " + exception.getMessage());
        });
        
        faultyThread.start();
        
        // Another thread without specific handler (uses default)
        Thread anotherFaultyThread = new Thread(() -> {
            throw new IllegalStateException("Another error!");
        });
        
        anotherFaultyThread.start();
    }
}
```

### Proper Exception Handling in Threads
```java
public class ProperExceptionHandling {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                // Risky operation
                riskyOperation();
            } catch (InterruptedException e) {
                // Restore interrupt status
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted");
            } catch (Exception e) {
                // Log and handle other exceptions
                System.err.println("Error in worker thread: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Cleanup resources
                System.out.println("Cleaning up resources...");
            }
        });
        
        worker.start();
        
        // Interrupt after 2 seconds
        Thread.sleep(2000);
        worker.interrupt();
        
        worker.join();
    }
    
    private static void riskyOperation() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Operation interrupted");
            }
            
            System.out.println("Processing: " + i);
            Thread.sleep(500);
            
            if (i == 5) {
                // Simulate random error
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Random error occurred");
                }
            }
        }
    }
}
```

## Best Practices

### 1. Always Handle InterruptedException Properly
```java
// Bad
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // Ignoring exception
}

// Good
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Restore interrupt status
    return; // or throw new RuntimeException(e)
}
```

### 2. Use Meaningful Thread Names
```java
// Bad
Thread thread = new Thread(task);

// Good
Thread thread = new Thread(task, "DatabaseConnectionPool-Worker-" + id);
```

### 3. Don't Call run() Directly
```java
// Bad
MyThread thread = new MyThread();
thread.run(); // Executes in current thread!

// Good
MyThread thread = new MyThread();
thread.start(); // Creates new thread
```

### 4. Prefer Runnable over Thread
```java
// Bad - limits inheritance
class MyTask extends Thread {
    // Can't extend another class
}

// Good - composition over inheritance
class MyTask implements Runnable {
    // Can extend another class if needed
}
```

## Summary

- **Multiple ways** to create threads: Thread class, Runnable interface, lambda expressions, Callable
- **Runnable interface** is preferred over extending Thread class
- **Thread properties**: name, priority, ID, state, daemon status
- **Control methods**: join(), sleep(), yield(), interrupt()
- **Thread groups** organize threads hierarchically
- **Daemon threads** are background threads that don't prevent JVM exit
- **Exception handling** requires proper interrupt status management
- **Best practices**: meaningful names, proper exception handling, prefer Runnable

## Next Steps

Continue to [Synchronization](03_Synchronization.md) to learn how to coordinate access to shared resources.