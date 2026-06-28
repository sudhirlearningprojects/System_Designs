# Module 13: Performance & Production

## 1. Thread Pool Management

### Understanding Camel's Threading Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    CamelContext Thread Pools                      │
│                                                                  │
│  Default Thread Pool (shared):                                  │
│    - Core: 10 threads                                           │
│    - Max: 20 threads                                            │
│    - Used by: multicast, splitter (parallel), wireTap           │
│                                                                  │
│  Component-Specific Pools:                                      │
│    - Kafka Consumer: consumersCount threads per route            │
│    - SEDA: concurrentConsumers threads per queue                │
│    - HTTP Server: platform thread pool                          │
│    - Scheduler: 1 thread per scheduled route                    │
│                                                                  │
│  Custom Thread Pools:                                           │
│    - Created via ThreadPoolProfile                              │
│    - Assigned to specific routes/patterns                       │
└─────────────────────────────────────────────────────────────────┘
```

### Custom Thread Pool Configuration

```java
@Configuration
public class CamelThreadPoolConfig {

    @Bean
    public ThreadPoolProfile highThroughputProfile() {
        ThreadPoolProfile profile = new ThreadPoolProfile("highThroughput");
        profile.setPoolSize(20);
        profile.setMaxPoolSize(50);
        profile.setMaxQueueSize(1000);
        profile.setKeepAliveTime(60L);
        profile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
        return profile;
    }
}

// Use in route
from("kafka:high-volume-events")
    .split(body()).executorService("highThroughput")
        .parallelProcessing()
        .to("direct:process-event")
    .end();
```

### SEDA Concurrent Consumers

```java
// SEDA = in-memory async queue with dedicated thread pool
from("kafka:orders")
    .to("seda:process-orders?concurrentConsumers=10&size=5000");

from("seda:process-orders?concurrentConsumers=10")
    .bean("orderService", "process");
// 10 threads consuming from this internal queue
```

---

## 2. Async Processing

### Using SEDA for Non-Blocking

```java
from("platform-http:/api/orders")
    .routeId("order-api")
    // Return 202 immediately, process async
    .wireTap("seda:process-order-async")
    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
    .setBody(constant("{\"status\":\"accepted\",\"message\":\"Processing\"}"));

from("seda:process-order-async?concurrentConsumers=5")
    .routeId("order-async-processor")
    .bean("orderService", "process")
    .to("kafka:order-events");
```

### Async HTTP Calls with Netty

```java
from("kafka:enrichment-requests")
    .routeId("async-enrichment")
    .split(body()).parallelProcessing()
        // Netty HTTP is non-blocking (NIO)
        .toD("netty-http:http://enrichment-service/api/enrich/${body.id}"
            + "?connectTimeout=3000"
            + "&requestTimeout=5000"
            + "&workerCount=16")           // NIO worker threads
    .end();
```

---

## 3. Backpressure Handling

### Throttling Consumers

```java
// Throttle Kafka consumption rate
from("kafka:high-volume?maxPollRecords=50&pollTimeoutMs=500")
    .throttle(1000).timePeriodMillis(1000)   // Max 1000/sec
        .asyncDelayed()                       // Non-blocking throttle
    .to("direct:process");
```

### SEDA Queue as Buffer (with bounded size)

```java
from("kafka:burst-traffic")
    .to("seda:buffer?size=10000&blockWhenFull=true");
    // If queue full → blocks Kafka consumer → triggers rebalance after maxPollIntervalMs
    // Better: use size + failIfFull and handle overflow

from("kafka:burst-traffic")
    .doTry()
        .to("seda:buffer?size=10000&failIfFull=true")
    .doCatch(IllegalStateException.class)
        .log("Buffer full! Sending to overflow queue")
        .to("kafka:overflow-queue")
    .end();
```

### Dynamic Throttling Based on System Load

```java
@Component("adaptiveThrottle")
public class AdaptiveThrottle {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public long getCurrentLimit() {
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();
        
        if (cpuUsage > 80 || memoryUsage > 85) {
            return 100;   // Aggressive throttle
        } else if (cpuUsage > 60) {
            return 500;   // Moderate
        }
        return 2000;      // Full speed
    }
}

from("kafka:events")
    .throttle(method("adaptiveThrottle", "getCurrentLimit"))
        .timePeriodMillis(1000)
    .to("direct:process");
```

---

## 4. Connection Pooling

### HTTP Connection Pool

```java
@Bean("httpConnectionManager")
public HttpClientConnectionManager connectionManager() {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(200);           // Total connections across all routes
    cm.setDefaultMaxPerRoute(50);  // Per destination
    return cm;
}

from("direct:call-service")
    .to("http://service/api?httpClientConnectionManager=#httpConnectionManager");
```

### Database Connection Pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000        # 5 min
      connection-timeout: 5000    # 5 sec
      max-lifetime: 1800000       # 30 min
      leak-detection-threshold: 60000
```

---

## 5. Memory Optimization

### Streaming for Large Files

```java
// BAD: Loads entire file into memory
from("file:/data/large-files")
    .split(body().tokenize("\n"))     // Loads all lines, then splits
    .to("direct:process");

// GOOD: Stream processing (line by line, constant memory)
from("file:/data/large-files")
    .split(body().tokenize("\n")).streaming()   // .streaming() = constant memory
    .to("direct:process");

// GOOD: For very large files, use streaming with SEDA buffer
from("file:/data/huge-files")
    .split(body().tokenize("\n")).streaming()
    .to("seda:process-line?size=1000&blockWhenFull=true");
```

### Claim Check for Large Payloads

```java
// Store large payload, pass reference through pipeline
from("kafka:large-payloads")
    .claimCheck(ClaimCheckOperation.Push)        // Store body in claim check
    .setBody(simple("${exchange.exchangeId}"))   // Pass lightweight reference
    .to("direct:lightweight-processing")
    .claimCheck(ClaimCheckOperation.Pop)          // Restore original body
    .to("direct:final-step");
```

---

## 6. JVM Tuning for Camel

```bash
# Production JVM flags
JAVA_OPTS="-server \
  -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.net.preferIPv4Stack=true \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heap-dump.hprof"
```

| Setting | Recommendation |
|---------|---------------|
| Heap size | 1-4 GB (depends on route complexity) |
| GC | G1GC for < 8GB, ZGC for > 8GB |
| Metaspace | Default is fine unless many components |
| Thread stack | Default 1MB (reduce to 512KB for many SEDA consumers) |

---

## 7. Production Checklist

### Route Configuration

```java
from("kafka:orders")
    .routeId("order-processor")                    // ✅ Always set routeId
    .autoStartup(true)                             // ✅ Explicit
    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)  // ✅ Graceful
    .log(LoggingLevel.DEBUG, "Received: ${body}")  // ✅ Use DEBUG not INFO
    .to("direct:process");
```

### Configuration Checklist

| Area | Setting | Production Value |
|------|---------|-----------------|
| Shutdown | `camel.springboot.shutdown-timeout` | 30-60s |
| Kafka | `autoCommitEnable` | false (manual commit) |
| Kafka | `maxPollRecords` | 100-500 (tuned per use case) |
| Kafka | `requestRequiredAcks` | all |
| HTTP | `connectTimeout` | 5000ms |
| HTTP | `socketTimeout` | 10000ms |
| Error | `maximumRedeliveries` | 3-5 |
| Error | `backOffMultiplier` | 2.0 |
| SEDA | `size` | bounded (not unlimited) |
| Thread pool | `maxPoolSize` | bounded |
| Metrics | enabled | true |
| Health | enabled | true |
| Tracing | enabled | true (sample rate in prod) |

---

## 8. Deployment Patterns

### Blue-Green with Kafka

```
                    ┌─────────────────┐
                    │  Kafka Topic    │
                    │  (orders)       │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
     ┌────────▼───┐   ┌────▼────┐   ┌────▼────┐
     │ Blue (v1)  │   │ Green(v2)│   │         │
     │ group=v1   │   │ group=v2 │   │   ...   │
     └────────────┘   └──────────┘   └─────────┘
     
During deployment:
1. Deploy Green (v2) with NEW consumer group
2. Green starts consuming from latest offset
3. Verify Green is healthy
4. Stop Blue (v1)
5. Green takes over all partitions
```

### Canary Deployment

```java
// Route a percentage of traffic to new version
from("kafka:orders")
    .loadBalance()
        .weighted(true, "90,10")  // 90% to v1, 10% to v2
        .to("direct:process-v1", "direct:process-v2")
    .end();
```

---

## 9. Capacity Planning

| Workload | Approximate Throughput (single JVM) |
|----------|--------------------------------------|
| Simple routing (direct → kafka) | 50K-100K msg/sec |
| JSON marshal/unmarshal | 10K-30K msg/sec |
| HTTP call per message | 500-2K msg/sec (depends on latency) |
| DB write per message | 1K-5K msg/sec |
| File processing (streaming) | 10K-50K lines/sec |
| Complex EIP (split + aggregate) | 5K-20K msg/sec |

**Scaling formula:**
```
Required instances = Peak messages/sec ÷ Per-instance throughput × Safety factor(1.5)

Example: 100K orders/sec with JSON + DB write (5K/instance)
  = 100,000 / 5,000 × 1.5 = 30 instances
```
