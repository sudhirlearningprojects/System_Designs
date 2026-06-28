# Scaling, High Availability & Production Readiness

## 1. Scaling the System

### 1.1 Kafka Scaling

| Dimension | POC | Production | How |
|-----------|-----|-----------|-----|
| Partitions | 6 | 100+ per topic | Partition by service_name for locality |
| Brokers | 1 | 5-9 | Multi-AZ deployment |
| Throughput | 1K msg/s | 1M msg/s | Partition parallelism + compression |
| Retention | 1 day | 7 days | Tiered storage (hot/cold) |

**Partition Strategy:**
```
metrics.raw → partitioned by service_name (hash)
  - Ensures all metrics for same service go to same partition
  - Flink can process per-service windows without shuffling

anomalies.detected → partitioned by service_name
  - Ensures ordered processing per service
```

**Key Configs:**
```properties
# Producer
linger.ms=5
batch.size=65536
compression.type=lz4
acks=1  # Speed over durability for metrics

# Consumer (Flink)
fetch.min.bytes=50000
max.poll.records=1000
```

### 1.2 Flink Scaling

| Dimension | POC | Production | How |
|-----------|-----|-----------|-----|
| Task slots | 4 | 100+ | Horizontal TaskManager scaling |
| Parallelism | 1 | = Kafka partitions | 1 slot per partition |
| State size | MBs | 100s GB | RocksDB state backend |
| Checkpointing | 60s | 30s | Incremental checkpoints |

**Scaling approach:**
```
                     ┌─────────────────────┐
                     │   Job Manager       │
                     │   (Leader Election) │
                     └─────────┬───────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
    ┌────▼────┐          ┌────▼────┐          ┌────▼────┐
    │ TM-1    │          │ TM-2    │          │ TM-3    │
    │ Slots:4 │          │ Slots:4 │          │ Slots:4 │
    │ Svc:A-D │          │ Svc:E-H │          │ Svc:I-L │
    └─────────┘          └─────────┘          └─────────┘

Each TaskManager handles a subset of services (keyed by service_name)
```

**State Management:**
```yaml
# flink-conf.yaml
state.backend: rocksdb
state.checkpoints.dir: s3://flink-checkpoints/anomaly-detector
state.backend.incremental: true
execution.checkpointing.interval: 30s
execution.checkpointing.min-pause: 10s
restart-strategy: fixed-delay
restart-strategy.fixed-delay.attempts: 3
restart-strategy.fixed-delay.delay: 10s
```

### 1.3 Vector Database Scaling

**POC: Pgvector (single node)**
- Good for < 1M vectors
- Simple setup, familiar PostgreSQL

**Production: Qdrant Cluster**
- Distributed, supports billions of vectors
- Built-in replication and sharding
- Filtered search (e.g., filter by service before vector search)

```yaml
# Qdrant cluster config
collections:
  incidents:
    vectors:
      size: 1536
      distance: Cosine
    shard_number: 6
    replication_factor: 2
    optimizers:
      indexing_threshold: 20000
      memmap_threshold: 50000
```

**Scaling path:**
| Scale | Solution | Vectors | Latency |
|-------|----------|---------|---------|
| < 100K | Pgvector | Small | 5-10ms |
| 100K - 10M | Qdrant single | Medium | 2-5ms |
| 10M - 1B | Qdrant cluster | Large | 5-15ms |
| > 1B | Pinecone / Weaviate managed | Massive | 10-50ms |

### 1.4 RAG Service Scaling

```
                    ┌──────────────┐
                    │   API GW /   │
                    │   ALB        │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │ RAG-1   │      │ RAG-2   │      │ RAG-3   │
    │ (ECS)   │      │ (ECS)   │      │ (ECS)   │
    └─────────┘      └─────────┘      └─────────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
              ┌────────────┼────────────────┐
              │            │                │
         ┌────▼───┐  ┌────▼───┐      ┌────▼────┐
         │Vector  │  │ Redis  │      │ LLM API │
         │  DB    │  │ Cache  │      │ (Bedrock│
         └────────┘  └────────┘      │/OpenAI) │
                                     └─────────┘
```

**Horizontal scaling:**
- Stateless service → auto-scale on CPU/request count
- Redis cache for embedding dedup (same anomaly = same embedding)
- Connection pooling for vector DB
- LLM request queuing with priority

---

## 2. High Availability

### 2.1 Multi-AZ Deployment

```
┌─────────────── Region: us-east-1 ───────────────────┐
│                                                       │
│  ┌─── AZ-1a ────┐  ┌─── AZ-1b ────┐  ┌─── AZ-1c ──┐
│  │               │  │               │  │             │
│  │ Kafka Broker1 │  │ Kafka Broker2 │  │ Kafka Brk3 │
│  │ Flink TM-1   │  │ Flink TM-2   │  │ Flink TM-3 │
│  │ RAG Svc (x2) │  │ RAG Svc (x2) │  │ RAG (x2)   │
│  │ Qdrant Node1 │  │ Qdrant Node2 │  │ Qdrant Nd3 │
│  │ Redis Primary │  │ Redis Replica│  │ Redis Repl │
│  │               │  │               │  │             │
│  └───────────────┘  └───────────────┘  └─────────────┘
│                                                       │
└───────────────────────────────────────────────────────┘
```

### 2.2 Failure Modes & Mitigation

| Component | Failure Mode | Mitigation |
|-----------|-------------|------------|
| Kafka broker | Node crash | Replication factor=3, min.insync=2 |
| Flink TaskManager | OOM/crash | Checkpoint recovery, auto-restart |
| Vector DB | Node down | Replication factor=2, read from replica |
| LLM API | Rate limit/timeout | Circuit breaker, fallback to cached explanation |
| Redis | Node crash | Sentinel/Cluster mode, graceful degradation |
| RAG Service | Crash | K8s restarts, ALB health checks |

### 2.3 Graceful Degradation

If LLM is unavailable:
```python
def generate_explanation_with_fallback(anomaly, similar_incidents, runbooks):
    try:
        return generate_explanation(anomaly, similar_incidents, runbooks)
    except (TimeoutError, RateLimitError):
        # Fallback: return similar incident directly without LLM synthesis
        if similar_incidents and similar_incidents[0]["similarity"] > 0.8:
            return Explanation(
                anomaly_id=anomaly.anomaly_id,
                root_cause=f"Likely similar to: {similar_incidents[0]['title']}",
                explanation=similar_incidents[0]["root_cause"],
                immediate_fix=similar_incidents[0]["resolution"].split(". "),
                long_term_fix="See full incident report",
                confidence="MEDIUM",
                similar_incident_id=similar_incidents[0]["id"],
                estimated_impact="Unknown (LLM unavailable)",
            )
        # Last resort: raw anomaly info
        return Explanation(
            anomaly_id=anomaly.anomaly_id,
            root_cause=f"{anomaly.anomaly_type} detected on {anomaly.service}",
            explanation=f"Z-score: {anomaly.z_score}. Manual investigation needed.",
            immediate_fix=["Check service dashboards", "Review recent deployments"],
            long_term_fix="N/A",
            confidence="LOW",
            similar_incident_id=None,
            estimated_impact="Unknown",
        )
```

---

## 3. Resilience Patterns

### 3.1 Circuit Breaker (for LLM calls)

```python
from circuitbreaker import circuit

@circuit(failure_threshold=5, recovery_timeout=60)
def call_llm(prompt: str) -> str:
    response = openai_client.chat.completions.create(
        model=LLM_MODEL,
        messages=[{"role": "user", "content": prompt}],
        timeout=30,
    )
    return response.choices[0].message.content
```

### 3.2 Retry with Exponential Backoff

```python
import tenacity

@tenacity.retry(
    stop=tenacity.stop_after_attempt(3),
    wait=tenacity.wait_exponential(multiplier=1, min=2, max=30),
    retry=tenacity.retry_if_exception_type((TimeoutError, ConnectionError)),
)
def search_vector_db(query_embedding: list[float]) -> list[dict]:
    # Vector search with retry
    ...
```

### 3.3 Deduplication

```python
def is_duplicate(anomaly: AnomalyEvent) -> bool:
    """Prevent explaining the same anomaly repeatedly."""
    key = f"dedup:{anomaly.service}:{anomaly.anomaly_type}"
    if redis_client.get(key):
        return True
    redis_client.setex(key, 300, "1")  # 5-minute cooldown
    return False
```

### 3.4 Backpressure Handling

```python
# If RAG pipeline is overwhelmed, prioritize by severity
import heapq

class PriorityAnomalyQueue:
    PRIORITY_MAP = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3}

    def __init__(self, max_size=1000):
        self.heap = []
        self.max_size = max_size

    def push(self, anomaly: AnomalyEvent):
        priority = self.PRIORITY_MAP.get(anomaly.severity, 3)
        if len(self.heap) >= self.max_size:
            # Drop lowest priority
            if priority < self.heap[-1][0]:
                heapq.heapreplace(self.heap, (priority, anomaly))
        else:
            heapq.heappush(self.heap, (priority, anomaly))

    def pop(self) -> AnomalyEvent | None:
        if self.heap:
            return heapq.heappop(self.heap)[1]
        return None
```
