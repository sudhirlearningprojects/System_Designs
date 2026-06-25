# Part 5: Performance & Limits

## 5.1 Cold Start Benchmarking

**Concept**: Measure and understand cold starts across different configurations.

```python
# scripts/benchmark_cold_starts.py
"""
Benchmark cold starts across different Lambda configurations.
Measures: init duration, execution duration, memory used.
"""
import boto3
import json
import time
import statistics

lambda_client = boto3.client('lambda')
logs_client = boto3.client('logs')

CONFIGS = [
    {'name': 'bot-registry-128mb', 'memory': 128},
    {'name': 'bot-registry-256mb', 'memory': 256},
    {'name': 'bot-registry-512mb', 'memory': 512},
    {'name': 'bot-registry-1024mb', 'memory': 1024},
    {'name': 'bot-registry-2048mb', 'memory': 2048},
    {'name': 'bot-registry-10240mb', 'memory': 10240},
]


def force_cold_start(function_name):
    """Force cold start by updating env var (creates new execution environment)."""
    lambda_client.update_function_configuration(
        FunctionName=function_name,
        Environment={'Variables': {'COLD_START_TRIGGER': str(time.time())}}
    )
    time.sleep(5)  # Wait for update to propagate


def measure_invocation(function_name, payload):
    """Invoke and measure duration from response."""
    response = lambda_client.invoke(
        FunctionName=function_name,
        Payload=json.dumps(payload),
        LogType='Tail'  # Get log tail in response
    )

    # Parse REPORT line from logs
    log_tail = response['LogResult']  # Base64 encoded
    import base64
    logs = base64.b64decode(log_tail).decode()

    init_duration = 0
    duration = 0
    memory_used = 0

    for line in logs.split('\n'):
        if 'REPORT' in line:
            if 'Init Duration' in line:
                init_duration = float(line.split('Init Duration:')[1].split('ms')[0].strip())
            duration = float(line.split('Duration:')[1].split('ms')[0].strip())
            memory_used = int(line.split('Max Memory Used:')[1].split('MB')[0].strip())

    return {
        'init_duration_ms': init_duration,
        'duration_ms': duration,
        'memory_used_mb': memory_used,
        'is_cold_start': init_duration > 0
    }


def benchmark(iterations=10):
    """Run benchmark across all configurations."""
    results = {}

    for config in CONFIGS:
        name = config['name']
        print(f"\nBenchmarking: {name} ({config['memory']}MB)")

        cold_starts = []
        warm_starts = []

        for i in range(iterations):
            # Force cold start
            force_cold_start(name)
            result = measure_invocation(name, {'rawPath': '/bots', 'requestContext': {'http': {'method': 'GET'}}})
            cold_starts.append(result['init_duration_ms'] + result['duration_ms'])

            # Warm start (immediate re-invocation)
            result = measure_invocation(name, {'rawPath': '/bots', 'requestContext': {'http': {'method': 'GET'}}})
            warm_starts.append(result['duration_ms'])

        results[name] = {
            'memory_mb': config['memory'],
            'cold_start_avg_ms': round(statistics.mean(cold_starts), 1),
            'cold_start_p95_ms': round(sorted(cold_starts)[int(len(cold_starts)*0.95)], 1),
            'warm_start_avg_ms': round(statistics.mean(warm_starts), 1),
            'warm_start_p95_ms': round(sorted(warm_starts)[int(len(warm_starts)*0.95)], 1),
        }

    return results


if __name__ == '__main__':
    results = benchmark(iterations=10)
    print("\n" + "="*70)
    print(f"{'Config':<25} {'Cold Avg':<12} {'Cold P95':<12} {'Warm Avg':<12} {'Warm P95':<12}")
    print("="*70)
    for name, data in results.items():
        print(f"{name:<25} {data['cold_start_avg_ms']:<12} {data['cold_start_p95_ms']:<12} "
              f"{data['warm_start_avg_ms']:<12} {data['warm_start_p95_ms']:<12}")
```

### Expected Results

```
┌──────────────────────────────────────────────────────────────┐
│  Cold Start Benchmarks (Python 3.11, simple function)         │
├──────────────┬──────────────┬──────────────┬─────────────────┤
│ Memory       │ Cold Start   │ Warm Start   │ vCPUs           │
├──────────────┼──────────────┼──────────────┼─────────────────┤
│ 128 MB       │ ~350ms       │ ~5ms         │ 0.08            │
│ 256 MB       │ ~280ms       │ ~4ms         │ 0.16            │
│ 512 MB       │ ~220ms       │ ~3ms         │ 0.33            │
│ 1024 MB      │ ~180ms       │ ~2ms         │ 0.58            │
│ 1769 MB      │ ~160ms       │ ~2ms         │ 1.0 (full vCPU) │
│ 3538 MB      │ ~150ms       │ ~1ms         │ 2.0             │
│ 10240 MB     │ ~140ms       │ ~1ms         │ 6.0             │
└──────────────┴──────────────┴──────────────┴─────────────────┘

Key insight: Memory = CPU. At 1769 MB you get 1 full vCPU.
Diminishing returns above 1769MB unless CPU-bound.
```

---

## 5.2 Memory-CPU Relationship

```python
# functions/cpu_benchmark/handler.py
"""
CPU benchmark to demonstrate memory→CPU scaling.

Lambda features demonstrated:
- Memory directly controls CPU allocation
- 1769 MB = 1 vCPU (proportional scaling)
- Helps find optimal price/performance ratio
"""
import time
import math
import os


def handler(event, context):
    """Run CPU-intensive task and measure performance."""
    memory_mb = int(os.environ.get('AWS_LAMBDA_FUNCTION_MEMORY_SIZE', 128))
    iterations = event.get('iterations', 1_000_000)

    # CPU-bound task: calculate primes
    start = time.time()
    primes = sieve_of_eratosthenes(iterations)
    elapsed = time.time() - start

    # Memory-bound task: allocate and sort array
    start2 = time.time()
    data = list(range(iterations, 0, -1))
    data.sort()
    elapsed2 = time.time() - start2

    return {
        'memory_mb': memory_mb,
        'cpu_task_ms': round(elapsed * 1000, 1),
        'memory_task_ms': round(elapsed2 * 1000, 1),
        'primes_found': len(primes),
        'remaining_time_ms': context.get_remaining_time_in_millis()
    }


def sieve_of_eratosthenes(limit):
    """Classic CPU-bound algorithm."""
    is_prime = [True] * (limit + 1)
    is_prime[0] = is_prime[1] = False
    for i in range(2, int(math.sqrt(limit)) + 1):
        if is_prime[i]:
            for j in range(i*i, limit + 1, i):
                is_prime[j] = False
    return [i for i in range(limit + 1) if is_prime[i]]
```

### Optimal Memory Finder

```python
# scripts/find_optimal_memory.py
"""
Find the cheapest memory config for a given workload.
Price = (memory_mb / 1024) * duration_ms * $0.0000166667 per GB-second
"""

def calculate_cost(memory_mb, duration_ms):
    """Calculate Lambda cost for one invocation."""
    gb_seconds = (memory_mb / 1024) * (duration_ms / 1000)
    cost = gb_seconds * 0.0000166667
    return cost

# Simulated benchmark results
benchmarks = [
    (128, 2500),   # 128MB takes 2500ms
    (256, 1300),   # 256MB takes 1300ms (more CPU)
    (512, 680),    # 512MB takes 680ms
    (1024, 350),   # 1024MB takes 350ms
    (1769, 210),   # 1 vCPU takes 210ms
    (3538, 115),   # 2 vCPUs takes 115ms
    (10240, 55),   # 6 vCPUs takes 55ms
]

print(f"{'Memory':<10} {'Duration':<12} {'Cost/invoke':<15} {'Cost/million':<15}")
print("-" * 52)
for mem, dur in benchmarks:
    cost = calculate_cost(mem, dur)
    print(f"{mem:<10} {dur}ms{'':<7} ${cost:.8f}{'':<5} ${cost*1_000_000:.2f}")

# Output:
# Memory    Duration     Cost/invoke     Cost/million
# 128       2500ms       $0.00000521     $5.21
# 256       1300ms       $0.00000542     $5.42
# 512       680ms        $0.00000567     $5.67
# 1024      350ms        $0.00000583     $5.83
# 1769      210ms        $0.00000604     $6.04
# 3538      115ms        $0.00000662     $6.62
# 10240     55ms         $0.00000916     $9.16
#
# Winner: 128MB is cheapest BUT 2.5s response time!
# Best balance: 512-1024MB (fast + affordable)
```

---

## 5.3 Provisioned Concurrency

**Concept**: Pre-warm Lambda instances to eliminate cold starts. Costs money but guarantees <10ms init.

```bash
# Set provisioned concurrency for hot-path functions
aws lambda put-provisioned-concurrency-config \
  --function-name bot-registry \
  --qualifier '$LATEST' \
  --provisioned-concurrent-executions 5

# Check status
aws lambda get-provisioned-concurrency-config \
  --function-name bot-registry \
  --qualifier '$LATEST'
```

### When to Use Provisioned Concurrency

```
┌──────────────────────────────────────────────────────────────┐
│  Decision: Provisioned Concurrency                            │
│                                                                │
│  Use when:                                                     │
│  • P99 latency matters (< 100ms SLA)                         │
│  • Predictable traffic patterns                               │
│  • User-facing APIs (bot registry, leaderboard)              │
│                                                                │
│  Don't use when:                                               │
│  • Async/background processing (battle worker)                │
│  • Low traffic (< 1 req/min → instances die anyway)          │
│  • Cost-sensitive and cold starts acceptable                  │
│                                                                │
│  Cost: ~$0.015/GB-hour provisioned                            │
│  5 instances × 256MB × 24hr = $0.46/day = $13.80/month       │
│                                                                │
│  For this project: SKIP (free tier, cold starts acceptable)   │
└──────────────────────────────────────────────────────────────┘
```

---

## 5.4 SnapStart (Java Comparison)

**Concept**: SnapStart takes a snapshot of the initialized Lambda (after init) and restores it on cold start — reducing Java cold starts from 5s to <200ms.

```java
// functions/bot_registry_java/src/main/java/Handler.java
// Java version of bot-registry for SnapStart comparison

package botarena;

import com.amazonaws.services.lambda.runtime.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Java Lambda with SnapStart enabled.
 * 
 * Lambda features demonstrated:
 * - SnapStart (Corretto 11/17)
 * - CRaC (Coordinated Restore at Checkpoint)
 * - Init phase optimization
 */
public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Heavy initialization done ONCE and snapshotted
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {
        
        String method = event.getHttpMethod();
        // ... handle request
        
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody("{\"status\":\"ok\"}");
    }
}
```

```bash
# Deploy with SnapStart enabled
aws lambda create-function \
  --function-name bot-registry-java \
  --runtime java17 \
  --handler botarena.Handler::handleRequest \
  --zip-file fileb://target/bot-registry.jar \
  --role arn:aws:iam::123456789:role/LambdaRole \
  --memory-size 512 \
  --timeout 30 \
  --snap-start ApplyOn=PublishedVersions

# Publish version (SnapStart only works on published versions)
aws lambda publish-version --function-name bot-registry-java
```

### Cold Start Comparison

```
┌──────────────────────────────────────────────────────────┐
│  Cold Start: Python vs Java vs Java+SnapStart             │
├──────────────────────────────────────────────────────────┤
│  Python 3.11 (256MB):      ~280ms                        │
│  Java 17 (512MB):          ~4,500ms (JVM startup)        │
│  Java 17 + SnapStart:      ~180ms (restored snapshot)    │
│  Node.js 18 (256MB):       ~200ms                        │
│  Rust (custom runtime):    ~12ms                         │
├──────────────────────────────────────────────────────────┤
│  Winner: Rust (custom) < SnapStart Java < Python < Java  │
└──────────────────────────────────────────────────────────┘
```

---

## 5.5 Concurrency and Throttling

```python
# scripts/load_test.py
"""
Test Lambda concurrency limits and throttling behavior.
"""
import boto3
import json
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

lambda_client = boto3.client('lambda')
FUNCTION = 'bot-registry'


def invoke_once(i):
    """Single invocation."""
    try:
        start = time.time()
        response = lambda_client.invoke(
            FunctionName=FUNCTION,
            Payload=json.dumps({'rawPath': '/bots', 'requestContext': {'http': {'method': 'GET'}}})
        )
        elapsed = (time.time() - start) * 1000
        status = response['StatusCode']
        throttled = response.get('FunctionError') == 'TooManyRequestsException'
        return {'id': i, 'status': status, 'latency_ms': elapsed, 'throttled': throttled}
    except Exception as e:
        return {'id': i, 'error': str(e), 'throttled': 'Throttl' in str(e)}


def load_test(concurrent_requests=100, total_requests=500):
    """Send burst of requests to test concurrency."""
    results = []
    throttled = 0

    with ThreadPoolExecutor(max_workers=concurrent_requests) as executor:
        futures = [executor.submit(invoke_once, i) for i in range(total_requests)]
        for future in as_completed(futures):
            result = future.result()
            results.append(result)
            if result.get('throttled'):
                throttled += 1

    latencies = [r['latency_ms'] for r in results if 'latency_ms' in r]
    print(f"Total: {total_requests}, Throttled: {throttled}")
    print(f"Latency - Avg: {sum(latencies)/len(latencies):.0f}ms, "
          f"P95: {sorted(latencies)[int(len(latencies)*0.95)]:.0f}ms")


if __name__ == '__main__':
    # Test 1: Normal load
    print("=== Normal Load (10 concurrent) ===")
    load_test(concurrent_requests=10, total_requests=50)

    # Test 2: Burst (may hit account concurrency limit)
    print("\n=== Burst Load (100 concurrent) ===")
    load_test(concurrent_requests=100, total_requests=500)
```

### Concurrency Controls

```bash
# Account-level concurrency (default: 1000)
aws lambda get-account-settings

# Function-level reserved concurrency
# Guarantees capacity but LIMITS other functions
aws lambda put-function-concurrency \
  --function-name battle-worker \
  --reserved-concurrent-executions 50

# Function-level max concurrency on event source
aws lambda update-event-source-mapping \
  --uuid abc-123 \
  --scaling-config '{"MaximumConcurrency": 10}'
```

### Concurrency Model

```
Account limit: 1000 concurrent executions (default)
                    │
    ┌───────────────┼───────────────┐
    │               │               │
    ▼               ▼               ▼
┌────────┐    ┌────────┐    ┌────────────┐
│Reserved│    │Reserved│    │Unreserved  │
│bot-reg │    │battle  │    │(shared)    │
│  =50   │    │  =50   │    │  =900     │
└────────┘    └────────┘    └────────────┘

Burst behavior:
- Region-dependent initial burst (500-3000)
- Then scales at 500/minute
- If burst exceeds limit → 429 TooManyRequests
```

---

## 5.6 /tmp Storage (10GB)

```python
# functions/heavy_worker/handler.py
"""
Use /tmp for caching large files between invocations.
Same execution environment reuses /tmp (warm starts).
"""
import os
import json
import boto3

s3 = boto3.client('s3')
CACHE_DIR = '/tmp/cache'


def handler(event, context):
    """Process with local caching."""
    os.makedirs(CACHE_DIR, exist_ok=True)

    file_key = event['file_key']
    cache_path = f"{CACHE_DIR}/{file_key.replace('/', '_')}"

    # Check local cache first
    if os.path.exists(cache_path):
        with open(cache_path, 'r') as f:
            data = json.load(f)
        source = 'cache'
    else:
        # Download from S3
        response = s3.get_object(Bucket='bot-arena-replays', Key=file_key)
        data = json.loads(response['Body'].read())
        # Cache for next invocation
        with open(cache_path, 'w') as f:
            json.dump(data, f)
        source = 's3'

    # Monitor /tmp usage
    tmp_usage = sum(
        os.path.getsize(os.path.join(CACHE_DIR, f))
        for f in os.listdir(CACHE_DIR)
    )

    return {
        'source': source,
        'tmp_usage_mb': round(tmp_usage / 1024 / 1024, 2),
        'tmp_limit_mb': 10240
    }
```

---

## 5.7 Performance Summary

| Optimization | Impact | Cost | Use In Project |
|-------------|--------|------|----------------|
| **More memory** | Faster (more CPU) | Higher per-ms | Battle Worker (10GB) |
| **Provisioned Concurrency** | No cold starts | ~$14/month per instance | Bot Registry (skip for demo) |
| **SnapStart** | Java cold start fix | Free | Java version comparison |
| **Layers** | Smaller package = faster deploy | Free | Shared arena-common |
| **ARM64 (Graviton)** | 20% cheaper, same speed | 20% less | All functions |
| **/tmp caching** | Skip S3 on warm starts | Free | Replay access |
| **Reserved concurrency** | Guaranteed capacity | Free (limits others) | Battle Worker |

### ARM64 Graviton2

```bash
# Deploy on ARM64 (20% cheaper, often faster for Python)
aws lambda create-function \
  --function-name bot-registry \
  --architectures arm64 \
  --runtime python3.11 \
  --handler handler.handler \
  ...
```

---

## Next: [Part 6 - Observability & Cost →](Part6_Observability_and_Cost.md)
