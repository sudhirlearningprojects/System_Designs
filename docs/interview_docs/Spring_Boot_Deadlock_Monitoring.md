# Spring Boot Deadlock Monitoring - Complete Guide

## How Spring Boot Monitors Deadlocks

Spring Boot uses **ThreadMXBean** from Java Management Extensions (JMX) to detect and monitor deadlocks. It provides multiple mechanisms:

1. **Spring Boot Actuator** - `/actuator/threaddump` endpoint
2. **Scheduled Health Checks** - Periodic deadlock detection
3. **Custom Monitoring** - Programmatic deadlock detection
4. **JMX MBeans** - Real-time monitoring via JConsole/VisualVM

---

## 1. ThreadMXBean - Core Deadlock Detection

### How It Works

```java
import java.lang.management.*;

public class DeadlockDetector {
    
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    // Detect monitor deadlocks (synchronized blocks)
    public static long[] findDeadlockedThreads() {
        return threadMXBean.findDeadlockedThreads();
    }
    
    // Detect both monitor and ownable synchronizer deadlocks (ReentrantLock, etc.)
    public static long[] findMonitorDeadlockedThreads() {
        return threadMXBean.findMonitorDeadlockedThreads();
    }
    
    // Get detailed thread info
    public static ThreadInfo[] getThreadInfo(long[] threadIds) {
        return threadMXBean.getThreadInfo(threadIds, true, true);
    }
}
```

**Key Methods**:
- `findDeadlockedThreads()`: Detects all deadlocks (synchronized + Lock)
- `findMonitorDeadlockedThreads()`: Detects only synchronized deadlocks
- `getThreadInfo()`: Gets detailed info about deadlocked threads

---

## 2. Spring Boot Actuator Deadlock Detection

### Setup

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.properties**:
```properties
management.endpoints.web.exposure.include=threaddump,health
management.endpoint.threaddump.enabled=true
```

### Actuator Thread Dump Endpoint

**Access**: `GET http://localhost:8080/actuator/threaddump`

**Response** (JSON format):
```json
{
  "threads": [
    {
      "threadName": "Thread-1",
      "threadId": 23,
      "blockedTime": -1,
      "blockedCount": 5,
      "waitedTime": -1,
      "waitedCount": 0,
      "lockName": "java.lang.Object@5e2de80c",
      "lockOwnerId": 24,
      "lockOwnerName": "Thread-2",
      "inNative": false,
      "suspended": false,
      "threadState": "BLOCKED",
      "stackTrace": [...]
    }
  ],
  "deadlockedThreads": [23, 24]
}
```

**Deadlock Detection**: Actuator automatically calls `ThreadMXBean.findDeadlockedThreads()` and includes results.

---

## 3. Custom Deadlock Monitor (Scheduled)

### Implementation

```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.lang.management.*;

@Component
public class DeadlockMonitor {
    
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    public void detectDeadlocks() {
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads, true, true);
            
            System.err.println("DEADLOCK DETECTED!");
            System.err.println("Number of deadlocked threads: " + deadlockedThreads.length);
            
            for (ThreadInfo threadInfo : threadInfos) {
                printDeadlockInfo(threadInfo);
            }
            
            // Send alert (email, Slack, PagerDuty)
            sendAlert(threadInfos);
        }
    }
    
    private void printDeadlockInfo(ThreadInfo threadInfo) {
        System.err.println("\nThread: " + threadInfo.getThreadName());
        System.err.println("State: " + threadInfo.getThreadState());
        System.err.println("Blocked on: " + threadInfo.getLockName());
        System.err.println("Owned by: " + threadInfo.getLockOwnerName());
        
        System.err.println("Stack trace:");
        for (StackTraceElement element : threadInfo.getStackTrace()) {
            System.err.println("  " + element);
        }
        
        MonitorInfo[] monitors = threadInfo.getLockedMonitors();
        if (monitors.length > 0) {
            System.err.println("Locked monitors:");
            for (MonitorInfo monitor : monitors) {
                System.err.println("  " + monitor);
            }
        }
    }
    
    private void sendAlert(ThreadInfo[] threadInfos) {
        // Implementation: Send email, Slack notification, etc.
    }
}
```

**Enable Scheduling**:
```java
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 4. Custom Health Indicator

### Implementation

```java
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;
import java.lang.management.*;
import java.util.*;

@Component
public class DeadlockHealthIndicator implements HealthIndicator {
    
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    @Override
    public Health health() {
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        
        if (deadlockedThreads == null || deadlockedThreads.length == 0) {
            return Health.up()
                .withDetail("deadlocks", 0)
                .build();
        }
        
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
        
        Map<String, Object> details = new HashMap<>();
        details.put("deadlockCount", deadlockedThreads.length);
        
        List<String> threadNames = new ArrayList<>();
        for (ThreadInfo info : threadInfos) {
            threadNames.add(info.getThreadName());
        }
        details.put("deadlockedThreads", threadNames);
        
        return Health.down()
            .withDetails(details)
            .build();
    }
}
```

**Access**: `GET http://localhost:8080/actuator/health`

**Response**:
```json
{
  "status": "DOWN",
  "components": {
    "deadlock": {
      "status": "DOWN",
      "details": {
        "deadlockCount": 2,
        "deadlockedThreads": ["Thread-1", "Thread-2"]
      }
    }
  }
}
```

---

## 5. Deadlock Example & Detection

### Creating a Deadlock

```java
public class DeadlockExample {
    
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void method1() {
        synchronized (lock1) {
            System.out.println(Thread.currentThread().getName() + " acquired lock1");
            
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized (lock2) {
                System.out.println(Thread.currentThread().getName() + " acquired lock2");
            }
        }
    }
    
    public void method2() {
        synchronized (lock2) {
            System.out.println(Thread.currentThread().getName() + " acquired lock2");
            
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized (lock1) {
                System.out.println(Thread.currentThread().getName() + " acquired lock1");
            }
        }
    }
    
    public static void main(String[] args) {
        DeadlockExample example = new DeadlockExample();
        
        Thread t1 = new Thread(() -> example.method1(), "Thread-1");
        Thread t2 = new Thread(() -> example.method2(), "Thread-2");
        
        t1.start();
        t2.start();
    }
}
```

### Detection Output

```
DEADLOCK DETECTED!
Number of deadlocked threads: 2

Thread: Thread-1
State: BLOCKED
Blocked on: java.lang.Object@5e2de80c
Owned by: Thread-2
Stack trace:
  com.example.DeadlockExample.method1(DeadlockExample.java:15)
Locked monitors:
  java.lang.Object@6d6f6e28

Thread: Thread-2
State: BLOCKED
Blocked on: java.lang.Object@6d6f6e28
Owned by: Thread-1
Stack trace:
  com.example.DeadlockExample.method2(DeadlockExample.java:27)
Locked monitors:
  java.lang.Object@5e2de80c
```

---

## 6. Advanced Monitoring with Micrometer

### Setup

```java
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.lang.management.*;

@Component
public class ThreadMetrics {
    
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final MeterRegistry registry;
    
    public ThreadMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Register gauges
        Gauge.builder("jvm.threads.deadlocked", this, ThreadMetrics::getDeadlockedThreadCount)
            .description("Number of deadlocked threads")
            .register(registry);
        
        Gauge.builder("jvm.threads.blocked", this, ThreadMetrics::getBlockedThreadCount)
            .description("Number of blocked threads")
            .register(registry);
    }
    
    private double getDeadlockedThreadCount() {
        long[] deadlocked = threadMXBean.findDeadlockedThreads();
        return deadlocked == null ? 0 : deadlocked.length;
    }
    
    private double getBlockedThreadCount() {
        ThreadInfo[] threads = threadMXBean.dumpAllThreads(false, false);
        return Arrays.stream(threads)
            .filter(t -> t.getThreadState() == Thread.State.BLOCKED)
            .count();
    }
}
```

**Prometheus Metrics**:
```
# HELP jvm_threads_deadlocked Number of deadlocked threads
# TYPE jvm_threads_deadlocked gauge
jvm_threads_deadlocked 2.0

# HELP jvm_threads_blocked Number of blocked threads
# TYPE jvm_threads_blocked gauge
jvm_threads_blocked 5.0
```

---

## 7. Alerting on Deadlocks

### Email Alert

```java
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class DeadlockAlertService {
    
    private final JavaMailSender mailSender;
    
    public DeadlockAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendDeadlockAlert(ThreadInfo[] deadlockedThreads) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("ops-team@company.com");
        message.setSubject("CRITICAL: Deadlock Detected in Production");
        message.setText(buildAlertMessage(deadlockedThreads));
        
        mailSender.send(message);
    }
    
    private String buildAlertMessage(ThreadInfo[] threads) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deadlock detected at: ").append(new Date()).append("\n\n");
        sb.append("Number of deadlocked threads: ").append(threads.length).append("\n\n");
        
        for (ThreadInfo thread : threads) {
            sb.append("Thread: ").append(thread.getThreadName()).append("\n");
            sb.append("State: ").append(thread.getThreadState()).append("\n");
            sb.append("Blocked on: ").append(thread.getLockName()).append("\n");
            sb.append("Owned by: ").append(thread.getLockOwnerName()).append("\n\n");
        }
        
        return sb.toString();
    }
}
```

---

## 8. JMX Monitoring

### Enable JMX

**application.properties**:
```properties
spring.jmx.enabled=true
management.endpoints.jmx.exposure.include=*
```

**Connect with JConsole**:
```bash
jconsole
```

**Steps**:
1. Connect to your Spring Boot application
2. Go to **MBeans** tab
3. Navigate to `java.lang` → `Threading`
4. Click **Operations** → `findDeadlockedThreads`
5. Click **Invoke** button

**Result**: Returns array of deadlocked thread IDs or `null`.

---

## 9. Prevention Strategies

### 1. Lock Ordering

```java
// BAD: Different lock order causes deadlock
public void method1() {
    synchronized (lock1) {
        synchronized (lock2) { }
    }
}

public void method2() {
    synchronized (lock2) {
        synchronized (lock1) { }
    }
}

// GOOD: Same lock order prevents deadlock
public void method1() {
    synchronized (lock1) {
        synchronized (lock2) { }
    }
}

public void method2() {
    synchronized (lock1) {
        synchronized (lock2) { }
    }
}
```

### 2. Use tryLock with Timeout

```java
import java.util.concurrent.locks.*;

public class SafeLocking {
    
    private final Lock lock1 = new ReentrantLock();
    private final Lock lock2 = new ReentrantLock();
    
    public void safeMethod() throws InterruptedException {
        while (true) {
            boolean gotLock1 = lock1.tryLock(50, TimeUnit.MILLISECONDS);
            if (!gotLock1) continue;
            
            try {
                boolean gotLock2 = lock2.tryLock(50, TimeUnit.MILLISECONDS);
                if (!gotLock2) continue;
                
                try {
                    // Critical section
                    return;
                } finally {
                    lock2.unlock();
                }
            } finally {
                lock1.unlock();
            }
        }
    }
}
```

### 3. Avoid Nested Locks

```java
// BAD: Nested locks
public void badMethod() {
    synchronized (lock1) {
        synchronized (lock2) {
            // Work
        }
    }
}

// GOOD: Single lock or separate methods
public void goodMethod() {
    synchronized (lock1) {
        // Work with resource 1
    }
    
    synchronized (lock2) {
        // Work with resource 2
    }
}
```

---

## 10. Complete Monitoring Solution

```java
import org.springframework.boot.actuate.health.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.lang.management.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ComprehensiveDeadlockMonitor implements HealthIndicator {
    
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final AtomicInteger deadlockCount = new AtomicInteger(0);
    private volatile long lastDeadlockTime = 0;
    
    @Scheduled(fixedRate = 5000)
    public void monitorDeadlocks() {
        long[] deadlocked = threadMXBean.findDeadlockedThreads();
        
        if (deadlocked != null && deadlocked.length > 0) {
            deadlockCount.incrementAndGet();
            lastDeadlockTime = System.currentTimeMillis();
            
            ThreadInfo[] infos = threadMXBean.getThreadInfo(deadlocked, true, true);
            logDeadlock(infos);
            alertOps(infos);
        }
    }
    
    @Override
    public Health health() {
        long[] deadlocked = threadMXBean.findDeadlockedThreads();
        
        if (deadlocked == null || deadlocked.length == 0) {
            return Health.up()
                .withDetail("totalDeadlocksDetected", deadlockCount.get())
                .withDetail("lastDeadlock", lastDeadlockTime == 0 ? "Never" : new Date(lastDeadlockTime))
                .build();
        }
        
        return Health.down()
            .withDetail("currentDeadlocks", deadlocked.length)
            .withDetail("totalDeadlocksDetected", deadlockCount.get())
            .build();
    }
    
    private void logDeadlock(ThreadInfo[] infos) {
        System.err.println("=== DEADLOCK DETECTED ===");
        for (ThreadInfo info : infos) {
            System.err.println("Thread: " + info.getThreadName());
            System.err.println("Blocked on: " + info.getLockName());
            System.err.println("Owned by: " + info.getLockOwnerName());
        }
    }
    
    private void alertOps(ThreadInfo[] infos) {
        // Send alert via email, Slack, PagerDuty
    }
}
```

---

## Summary

| Method | Detection Type | Frequency | Use Case |
|--------|---------------|-----------|----------|
| **Actuator** | On-demand | Manual | Development/debugging |
| **Scheduled Monitor** | Automatic | Every N seconds | Production monitoring |
| **Health Indicator** | On-demand | Health check | Load balancer health |
| **JMX** | On-demand | Manual | Operations team |
| **Micrometer** | Continuous | Real-time | Metrics/dashboards |

**Best Practice**: Combine scheduled monitoring + health indicator + alerting for production systems.
