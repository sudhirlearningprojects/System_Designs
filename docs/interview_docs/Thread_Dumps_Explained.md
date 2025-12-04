# Thread Dumps - Complete Guide

## What is a Thread Dump?

A **thread dump** is a snapshot of all threads running in a JVM at a specific moment, showing:
- Thread state (RUNNABLE, BLOCKED, WAITING)
- Stack trace (what each thread is doing)
- Locks held and waited for
- Thread priorities

**Use Cases**:
- Debugging deadlocks
- Finding performance bottlenecks
- Analyzing high CPU usage
- Investigating application hangs

---

## Thread States

| State | Description | Example |
|-------|-------------|---------|
| **NEW** | Thread created but not started | `Thread t = new Thread()` |
| **RUNNABLE** | Executing or ready to execute | Active processing |
| **BLOCKED** | Waiting for monitor lock | `synchronized` block |
| **WAITING** | Waiting indefinitely | `wait()`, `join()` |
| **TIMED_WAITING** | Waiting for specified time | `sleep()`, `wait(1000)` |
| **TERMINATED** | Thread completed | Finished execution |

---

## How to Generate Thread Dumps

### Method 1: jstack (Command Line)

```bash
# Find Java process ID
jps -l

# Output:
# 12345 com.example.Application
# 67890 org.apache.catalina.startup.Bootstrap

# Generate thread dump
jstack 12345 > thread_dump.txt

# Or with additional info
jstack -l 12345 > thread_dump_detailed.txt
```

---

### Method 2: kill -3 (Linux/Mac)

```bash
# Send SIGQUIT signal
kill -3 12345

# Thread dump written to stdout/stderr
# Check application logs or console
```

---

### Method 3: jcmd (Recommended)

```bash
# List Java processes
jcmd

# Generate thread dump
jcmd 12345 Thread.print > thread_dump.txt
```

---

### Method 4: VisualVM (GUI)

```bash
# Start VisualVM
jvisualvm

# Steps:
# 1. Select your application
# 2. Go to "Threads" tab
# 3. Click "Thread Dump" button
```

---

### Method 5: JConsole (GUI)

```bash
# Start JConsole
jconsole

# Steps:
# 1. Connect to your application
# 2. Go to "Threads" tab
# 3. Click "Detect Deadlock" or view thread details
```

---

### Method 6: Programmatically (Java Code)

```java
import java.lang.management.*;

public class ThreadDumpGenerator {
    
    public static String generateThreadDump() {
        StringBuilder dump = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append('"').append(threadInfo.getThreadName()).append('"');
            dump.append(" Id=").append(threadInfo.getThreadId());
            dump.append(" ").append(threadInfo.getThreadState());
            
            if (threadInfo.getLockName() != null) {
                dump.append(" on ").append(threadInfo.getLockName());
            }
            
            if (threadInfo.getLockOwnerName() != null) {
                dump.append(" owned by \"").append(threadInfo.getLockOwnerName())
                    .append("\" Id=").append(threadInfo.getLockOwnerId());
            }
            
            dump.append("\n");
            
            for (StackTraceElement ste : threadInfo.getStackTrace()) {
                dump.append("    at ").append(ste).append("\n");
            }
            
            dump.append("\n");
        }
        
        return dump.toString();
    }
    
    public static void main(String[] args) {
        String threadDump = generateThreadDump();
        System.out.println(threadDump);
    }
}
```

---

## Reading Thread Dumps

### Sample Thread Dump

```
"http-nio-8080-exec-1" #23 daemon prio=5 os_prio=0 tid=0x00007f8c4c001000 nid=0x1a2b waiting on condition [0x00007f8c3e5fe000]
   java.lang.Thread.State: TIMED_WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        - parking to wait for  <0x00000000e0a12345> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
        at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
        at java.util.concurrent.LinkedBlockingQueue.poll(LinkedBlockingQueue.java:467)
        at org.apache.tomcat.util.threads.TaskQueue.poll(TaskQueue.java:89)
        at org.apache.tomcat.util.threads.TaskQueue.poll(TaskQueue.java:32)
        at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1073)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1134)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:748)
```

### Breaking Down the Thread Dump

**Line 1: Thread Header**
```
"http-nio-8080-exec-1" #23 daemon prio=5 os_prio=0 tid=0x00007f8c4c001000 nid=0x1a2b waiting on condition
```

- `"http-nio-8080-exec-1"`: Thread name
- `#23`: Thread ID
- `daemon`: Daemon thread (vs user thread)
- `prio=5`: Java priority (1-10)
- `os_prio=0`: OS priority
- `tid=0x...`: Thread ID (hex)
- `nid=0x1a2b`: Native thread ID
- `waiting on condition`: Thread state description

**Line 2: Thread State**
```
java.lang.Thread.State: TIMED_WAITING (parking)
```

- Current state of the thread
- Additional info in parentheses

**Lines 3+: Stack Trace**
```
at sun.misc.Unsafe.park(Native Method)
at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
...
```

- Shows what the thread is doing
- Read from bottom to top (call stack)

---

## Common Patterns in Thread Dumps

### 1. Deadlock

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8c4c001000 (object 0x00000000e0a12345, a java.lang.Object),
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x00007f8c4c002000 (object 0x00000000e0a67890, a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
===================================================
"Thread-1":
        at com.example.Service.method1(Service.java:10)
        - waiting to lock <0x00000000e0a12345> (a java.lang.Object)
        - locked <0x00000000e0a67890> (a java.lang.Object)
        at com.example.Service.doWork(Service.java:5)

"Thread-2":
        at com.example.Service.method2(Service.java:20)
        - waiting to lock <0x00000000e0a67890> (a java.lang.Object)
        - locked <0x00000000e0a12345> (a java.lang.Object)
        at com.example.Service.doWork(Service.java:15)

Found 1 deadlock.
```

**Diagnosis**: Thread-1 holds lock A, wants lock B. Thread-2 holds lock B, wants lock A.

---

### 2. Thread Blocked on Synchronized

```
"Worker-Thread-1" #25 prio=5 os_prio=0 tid=0x00007f8c4c003000 nid=0x1a2c waiting for monitor entry [0x00007f8c3e6ff000]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.example.Service.processRequest(Service.java:50)
        - waiting to lock <0x00000000e0a12345> (a java.lang.Object)
        at com.example.Controller.handleRequest(Controller.java:30)
```

**Diagnosis**: Thread waiting to acquire synchronized lock.

---

### 3. Thread Waiting (wait/notify)

```
"Consumer-Thread" #26 prio=5 os_prio=0 tid=0x00007f8c4c004000 nid=0x1a2d in Object.wait() [0x00007f8c3e7ff000]
   java.lang.Thread.State: WAITING (on object monitor)
        at java.lang.Object.wait(Native Method)
        - waiting on <0x00000000e0a12345> (a java.util.LinkedList)
        at java.lang.Object.wait(Object.java:502)
        at com.example.Queue.take(Queue.java:40)
        - locked <0x00000000e0a12345> (a java.util.LinkedList)
```

**Diagnosis**: Thread waiting for notify() call.

---

### 4. Thread Sleeping

```
"Background-Worker" #27 prio=5 os_prio=0 tid=0x00007f8c4c005000 nid=0x1a2e waiting on condition [0x00007f8c3e8ff000]
   java.lang.Thread.State: TIMED_WAITING (sleeping)
        at java.lang.Thread.sleep(Native Method)
        at com.example.Worker.run(Worker.java:25)
```

**Diagnosis**: Thread sleeping for specified time.

---

### 5. Thread Doing I/O

```
"DB-Connection-Thread" #28 prio=5 os_prio=0 tid=0x00007f8c4c006000 nid=0x1a2f runnable [0x00007f8c3e9ff000]
   java.lang.Thread.State: RUNNABLE
        at java.net.SocketInputStream.socketRead0(Native Method)
        at java.net.SocketInputStream.socketRead(SocketInputStream.java:116)
        at java.net.SocketInputStream.read(SocketInputStream.java:171)
        at com.mysql.jdbc.MysqlIO.readFully(MysqlIO.java:3008)
        at com.mysql.jdbc.MysqlIO.reuseAndReadPacket(MysqlIO.java:3469)
```

**Diagnosis**: Thread waiting for I/O (database query).

---

## Spring Boot Thread Dump Monitoring

### 1. Spring Boot Actuator

**Setup**:

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.properties**:
```properties
# Enable thread dump endpoint
management.endpoints.web.exposure.include=threaddump,health,metrics
management.endpoint.threaddump.enabled=true
```

**Access Thread Dump**:
```bash
# Get thread dump via HTTP
curl http://localhost:8080/actuator/threaddump

# Or in browser
http://localhost:8080/actuator/threaddump
```

**Response (JSON)**:
```json
{
  "threads": [
    {
      "threadName": "http-nio-8080-exec-1",
      "threadId": 23,
      "blockedTime": -1,
      "blockedCount": 0,
      "waitedTime": -1,
      "waitedCount": 2,
      "lockName": null,
      "lockOwnerId": -1,
      "lockOwnerName": null,
      "inNative": false,
      "suspended": false,
      "threadState": "TIMED_WAITING",
      "stackTrace": [
        {
          "methodName": "park",
          "fileName": "Unsafe.java",
          "lineNumber": -2,
          "className": "sun.misc.Unsafe",
          "nativeMethod": true
        }
      ]
    }
  ]
}
```

---

### 2. Custom Thread Dump Endpoint

```java
@RestController
@RequestMapping("/admin")
public class ThreadDumpController {
    
    @GetMapping("/threaddump")
    public ResponseEntity<String> getThreadDump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        
        StringBuilder dump = new StringBuilder();
        dump.append("Thread Dump - ").append(new Date()).append("\n\n");
        
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append(formatThreadInfo(threadInfo));
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(dump.toString());
    }
    
    private String formatThreadInfo(ThreadInfo threadInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\"").append(threadInfo.getThreadName()).append("\"");
        sb.append(" Id=").append(threadInfo.getThreadId());
        sb.append(" ").append(threadInfo.getThreadState());
        sb.append("\n");
        
        for (StackTraceElement ste : threadInfo.getStackTrace()) {
            sb.append("    at ").append(ste).append("\n");
        }
        
        sb.append("\n");
        return sb.toString();
    }
}
```

---

### 3. Scheduled Thread Dump Collection

```java
@Component
public class ThreadDumpCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadDumpCollector.class);
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void collectThreadDump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        
        // Analyze for issues
        detectDeadlocks(threadMXBean);
        detectBlockedThreads(threadInfos);
        detectHighCPUThreads(threadInfos);
    }
    
    private void detectDeadlocks(ThreadMXBean threadMXBean) {
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null) {
            logger.error("DEADLOCK DETECTED! Threads: {}", Arrays.toString(deadlockedThreads));
            // Send alert
        }
    }
    
    private void detectBlockedThreads(ThreadInfo[] threadInfos) {
        for (ThreadInfo info : threadInfos) {
            if (info.getThreadState() == Thread.State.BLOCKED) {
                if (info.getBlockedTime() > 10000) { // Blocked > 10 seconds
                    logger.warn("Thread {} blocked for {} ms", 
                        info.getThreadName(), info.getBlockedTime());
                }
            }
        }
    }
    
    private void detectHighCPUThreads(ThreadInfo[] threadInfos) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        for (ThreadInfo info : threadInfos) {
            long cpuTime = threadMXBean.getThreadCpuTime(info.getThreadId());
            long cpuTimeMs = cpuTime / 1_000_000; // Convert to milliseconds
            
            if (cpuTimeMs > 60000) { // CPU time > 1 minute
                logger.warn("Thread {} using high CPU: {} ms", 
                    info.getThreadName(), cpuTimeMs);
            }
        }
    }
}
```

---

### 4. Thread Metrics with Micrometer

```java
@Configuration
public class ThreadMetricsConfig {
    
    @Bean
    public MeterBinder threadMetrics() {
        return (registry) -> {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            
            // Total thread count
            Gauge.builder("jvm.threads.count", threadBean, ThreadMXBean::getThreadCount)
                .description("Current thread count")
                .register(registry);
            
            // Daemon thread count
            Gauge.builder("jvm.threads.daemon", threadBean, ThreadMXBean::getDaemonThreadCount)
                .description("Current daemon thread count")
                .register(registry);
            
            // Peak thread count
            Gauge.builder("jvm.threads.peak", threadBean, ThreadMXBean::getPeakThreadCount)
                .description("Peak thread count")
                .register(registry);
            
            // Deadlocked threads
            Gauge.builder("jvm.threads.deadlocked", threadBean, 
                bean -> {
                    long[] deadlocked = bean.findDeadlockedThreads();
                    return deadlocked != null ? deadlocked.length : 0;
                })
                .description("Number of deadlocked threads")
                .register(registry);
        };
    }
}
```

**Access Metrics**:
```bash
curl http://localhost:8080/actuator/metrics/jvm.threads.count
curl http://localhost:8080/actuator/metrics/jvm.threads.deadlocked
```

---

### 5. Thread Dump on OutOfMemoryError

**JVM Arguments**:
```bash
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/logs/heapdump.hprof \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:/var/logs/gc.log \
     -jar application.jar
```

---

### 6. Automatic Thread Dump on High CPU

```java
@Component
public class HighCPUThreadDumper {
    
    private static final Logger logger = LoggerFactory.getLogger(HighCPUThreadDumper.class);
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkCPU() {
        double cpuLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        
        // If CPU load > 80%
        if (cpuLoad / availableProcessors > 0.8) {
            logger.warn("High CPU detected: {}%", cpuLoad * 100);
            generateThreadDump();
        }
    }
    
    private void generateThreadDump() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "/var/logs/threaddump_" + timestamp + ".txt";
            
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                for (ThreadInfo info : threadInfos) {
                    writer.println(formatThreadInfo(info));
                }
            }
            
            logger.info("Thread dump saved to: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to generate thread dump", e);
        }
    }
    
    private String formatThreadInfo(ThreadInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(info.getThreadName()).append("\" ");
        sb.append("Id=").append(info.getThreadId()).append(" ");
        sb.append(info.getThreadState()).append("\n");
        
        for (StackTraceElement ste : info.getStackTrace()) {
            sb.append("    at ").append(ste).append("\n");
        }
        
        return sb.toString();
    }
}
```

---

## Analyzing Thread Dumps

### Tools for Analysis

**1. FastThread (Online)**
- URL: https://fastthread.io/
- Upload thread dump file
- Automatic analysis and visualization

**2. Thread Dump Analyzer (TDA)**
- Standalone Java application
- Visual representation of threads
- Deadlock detection

**3. IBM Thread and Monitor Dump Analyzer**
- Free tool from IBM
- Detailed analysis
- Comparison of multiple dumps

**4. JProfiler**
- Commercial profiler
- Real-time thread monitoring
- CPU profiling

---

### Manual Analysis Steps

**Step 1: Identify Problem Threads**
```bash
# Find threads in BLOCKED state
grep -A 10 "BLOCKED" thread_dump.txt

# Find threads waiting
grep -A 10 "WAITING" thread_dump.txt

# Find deadlocks
grep -A 20 "deadlock" thread_dump.txt
```

**Step 2: Look for Patterns**
- Multiple threads blocked on same lock
- Threads stuck in same method
- High number of threads in WAITING state

**Step 3: Analyze Stack Traces**
- Identify application code (your packages)
- Look for long-running operations
- Check for database queries, external API calls

**Step 4: Compare Multiple Dumps**
- Take 3-5 thread dumps (10 seconds apart)
- Compare to see if threads are progressing
- Stuck threads indicate deadlock or infinite loop

---

## Common Issues and Solutions

### Issue 1: Deadlock

**Symptom**:
```
Found 1 deadlock.
Thread-1 waiting for lock held by Thread-2
Thread-2 waiting for lock held by Thread-1
```

**Solution**:
```java
// ❌ Bad: Nested locks in different order
synchronized(lock1) {
    synchronized(lock2) {
        // code
    }
}

// ✅ Good: Always acquire locks in same order
synchronized(lock1) {
    synchronized(lock2) {
        // code
    }
}

// ✅ Better: Use tryLock with timeout
Lock lock1 = new ReentrantLock();
Lock lock2 = new ReentrantLock();

if (lock1.tryLock(1, TimeUnit.SECONDS)) {
    try {
        if (lock2.tryLock(1, TimeUnit.SECONDS)) {
            try {
                // code
            } finally {
                lock2.unlock();
            }
        }
    } finally {
        lock1.unlock();
    }
}
```

---

### Issue 2: Thread Pool Exhaustion

**Symptom**:
```
All 200 threads in WAITING state
Waiting on LinkedBlockingQueue.take()
```

**Solution**:
```java
// Increase thread pool size
@Bean
public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);
    executor.setMaxPoolSize(200);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("async-");
    executor.initialize();
    return executor;
}
```

---

### Issue 3: Slow Database Queries

**Symptom**:
```
Multiple threads stuck in:
at com.mysql.jdbc.MysqlIO.readFully()
```

**Solution**:
- Add database connection timeout
- Optimize slow queries
- Add connection pooling
- Use async processing

---

## Best Practices

1. **Take Multiple Dumps**: 3-5 dumps, 10 seconds apart
2. **Automate Collection**: Schedule periodic dumps
3. **Monitor Metrics**: Track thread count, deadlocks
4. **Set Alerts**: Alert on deadlocks, high thread count
5. **Analyze Regularly**: Review dumps during incidents
6. **Keep History**: Store dumps for trend analysis
7. **Use Tools**: Automate analysis with FastThread, TDA

---

## Key Takeaways

1. **Thread Dump**: Snapshot of all threads in JVM
2. **Generation**: jstack, jcmd, Actuator endpoint
3. **States**: RUNNABLE, BLOCKED, WAITING, TIMED_WAITING
4. **Common Issues**: Deadlocks, blocked threads, thread exhaustion
5. **Spring Boot**: Actuator provides /actuator/threaddump endpoint
6. **Monitoring**: Schedule periodic dumps, track metrics
7. **Analysis**: Use tools like FastThread for automated analysis
8. **Best Practice**: Take multiple dumps to identify stuck threads

**Bottom Line**: Thread dumps are essential for debugging concurrency issues, deadlocks, and performance problems in production!
