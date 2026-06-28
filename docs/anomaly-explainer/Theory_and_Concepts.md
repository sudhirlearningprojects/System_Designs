# Theory & Concepts

## 1. RAG (Retrieval Augmented Generation)

### What is RAG?

RAG is a technique that enhances LLM responses by first retrieving relevant information from a knowledge base, then using that information as context for generation.

```
Traditional LLM:
  Question → LLM (relies on training data) → Answer (may hallucinate)

RAG:
  Question → Embed → Search KB → Retrieve Context → LLM + Context → Grounded Answer
```

### Why RAG over Fine-Tuning?

| Aspect | Fine-Tuning | RAG |
|--------|-------------|-----|
| Knowledge updates | Retrain ($$$, days) | Add docs (instant) |
| Traceability | Black box | See which docs were used |
| Hallucination | Still possible | Grounded in real data |
| Cost | $100s-$1000s per training | $0.01 per query |
| Domain specificity | Good | Excellent (your data) |
| Setup time | Days-weeks | Hours |

### RAG Architecture Patterns

**Naive RAG:**
```
Query → Embed → Top-K search → Stuff into prompt → LLM
```
- Simple but limited context window
- No re-ranking, may get irrelevant results

**Advanced RAG (what we use):**
```
Query → Embed → Top-K search → Re-rank → Filter by metadata → Assemble prompt → LLM
```
- Service-aware filtering (prioritize same-service incidents)
- Similarity threshold (drop results below 0.6)
- Metadata boosting (recent incidents weighted higher)

**Agentic RAG (future):**
```
Query → Agent decides what to search → Multi-step retrieval → Synthesize → LLM
```
- Agent can query logs, metrics APIs, deployment history
- Iterative: if first search insufficient, refine and search again

### Chunking Strategy for Incidents

```
Each incident = 1 document (not chunked further)
Why: Incidents are self-contained (title + description + root_cause + resolution)
     Chunking would lose context between cause and fix.

For runbooks (longer docs):
  - Chunk by section headers
  - Each chunk gets parent runbook metadata
  - Overlap: 2 sentences between chunks
```

### Embedding Models Comparison

| Model | Dimensions | Speed | Quality | Cost/1M tokens |
|-------|-----------|-------|---------|---------------|
| text-embedding-3-small | 1536 | Fast | Good | $0.02 |
| text-embedding-3-large | 3072 | Medium | Best | $0.13 |
| Amazon Titan Embed v2 | 1024 | Fast | Good | $0.02 |
| Cohere embed-v3 | 1024 | Fast | Good | $0.10 |
| all-MiniLM-L6-v2 (open) | 384 | Fastest | Okay | Free |

**Our choice**: `text-embedding-3-small` — best cost/quality for incident text.

---

## 2. Vector Databases

### How Vector Search Works

```
Traditional DB: SELECT * FROM incidents WHERE service = 'payment'  (exact match)
Vector DB:      Find 5 nearest neighbors to this 1536-dim vector  (semantic similarity)
```

**Distance Metrics:**
- **Cosine Similarity**: Measures angle between vectors (direction matters, not magnitude)
  - Range: [-1, 1] (1 = identical, 0 = orthogonal)
  - Best for: text similarity
  
- **Euclidean (L2)**: Straight-line distance
  - Range: [0, ∞) (0 = identical)
  - Best for: when magnitude matters

- **Dot Product**: Combines direction + magnitude
  - Range: (-∞, ∞)
  - Best for: recommendation systems

### Indexing Algorithms

**IVF (Inverted File Index) - used in Pgvector:**
```
1. Cluster vectors into N cells using k-means
2. At query time, only search nearest C cells (not all vectors)
3. Trade-off: more cells = faster but less accurate

                    ┌─── Cell 1 (payment incidents) ───┐
                    │  vec1, vec2, vec3, ...            │
Query vector ──►   ├─── Cell 2 (auth incidents) ──────┤  ← Search only 
                    │  vec4, vec5, vec6, ...            │    nearest cells
                    ├─── Cell 3 (order incidents) ─────┤
                    │  vec7, vec8, vec9, ...            │
                    └──────────────────────────────────┘
```

**HNSW (Hierarchical Navigable Small World) - used in Qdrant:**
```
Multi-layer graph where higher layers have fewer, well-connected nodes.
Search starts at top layer, navigates down to find exact nearest neighbors.

Layer 2:  A ─── B ─── C           (few nodes, long connections)
Layer 1:  A ─ D ─ B ─ E ─ C       (more nodes, medium connections)  
Layer 0:  A D F B G E H C I       (all nodes, short connections)

Benefit: O(log N) search time, very fast for high-dimensional vectors
```

### Pgvector vs Qdrant vs Pinecone

| Feature | Pgvector | Qdrant | Pinecone |
|---------|----------|--------|----------|
| Setup | PostgreSQL extension | Standalone/Docker | Managed SaaS |
| Scale | < 5M vectors | < 1B vectors | Unlimited |
| Filtering | SQL WHERE | Native metadata filters | Namespace + metadata |
| Index | IVFFlat, HNSW | HNSW | Proprietary |
| Latency | 5-50ms | 1-10ms | 10-50ms |
| Cost | Free (self-hosted) | Free/Paid | $70+/mo |
| Best for | POC, small scale | Production | Enterprise, no-ops |

---

## 3. Apache Kafka

### Why Kafka for This System?

1. **Decoupling**: Metrics producers don't know about anomaly detectors
2. **Buffering**: If Flink is down, metrics are retained (not lost)
3. **Replay**: Can reprocess historical metrics for testing new detection algorithms
4. **Fan-out**: Multiple consumers can read same data (detection + archival + monitoring)
5. **Ordering**: Per-partition ordering ensures metrics arrive in sequence per service

### Key Concepts Applied

**Partitioning by service:**
```
Topic: metrics.raw (6 partitions)
  Partition 0: payment-service metrics    ← All payment data here
  Partition 1: auth-service metrics       ← All auth data here
  Partition 2: order-service metrics
  ...

Why: Flink windows are per-service. Same partition = no shuffle needed.
```

**Consumer Groups:**
```
Group "anomaly-detector":  Flink job (reads all partitions)
Group "metrics-archiver":  Writes to S3 for long-term storage
Group "real-time-dashboard": Grafana live streaming

Each group gets independent read position. Adding groups doesn't affect others.
```

**Exactly-Once Semantics:**
```
Producer: enable.idempotence=true, acks=all
Consumer: Flink checkpointing (offsets committed with state)
Result: Each metric processed exactly once, even after failures
```

---

## 4. Apache Flink

### Why Flink over Alternatives?

| Feature | Flink | Spark Streaming | Kafka Streams |
|---------|-------|----------------|---------------|
| Model | True streaming | Micro-batch | True streaming |
| Latency | Milliseconds | Seconds | Milliseconds |
| State management | Managed (RocksDB) | External | RocksDB |
| Exactly-once | Yes (checkpoints) | Yes | Yes |
| Windowing | Event-time + watermarks | Processing-time | Yes |
| Scale | Thousands of nodes | Thousands | Per-app |
| Complexity | Medium | Low | Low |
| Best for | Complex streaming | Batch + streaming | Simple transforms |

**Our choice**: Flink — we need event-time windows with exactly-once state.

### Windowing for Anomaly Detection

**Sliding Window** (our approach):
```
Time: ──────────────────────────────────────────────►
       │←── Window 1 (5min) ──→│
            │←── Window 2 (5min) ──→│
                 │←── Window 3 (5min) ──→│
       Slide = 30 seconds

Each window computes: mean, stddev, z-score for latest value
Overlapping windows give smooth detection (no edge effects)
```

**Why 5-min window, 30-sec slide?**
- 5 min = enough data points (60 at 5s interval) for stable statistics
- 30 sec slide = detect anomalies within 30 seconds of occurrence
- Trade-off: shorter window = faster detection but noisier stats

### State & Checkpointing

```
                    ┌─────────────────────────┐
                    │     Flink Job State      │
                    │                          │
                    │  service_a: {            │
                    │    cpu: [45,43,47,...]   │ ← Sliding window values
                    │    mem: [60,62,61,...]   │
                    │  }                       │
                    │  service_b: {...}        │
                    └────────────┬────────────┘
                                 │
                    Checkpoint every 30s
                                 │
                    ┌────────────▼────────────┐
                    │    S3 / RocksDB         │
                    │    (durable state)      │
                    └─────────────────────────┘

On failure: Restore state from last checkpoint, replay Kafka from checkpoint offset
Result: No data loss, no duplicate processing
```

### Watermarks & Late Data

```
Real events:     [t=1] [t=2] [t=3] ... [t=5]  [t=3.5 arrives late!]
Watermark:       "I've seen all events up to t=4"

Strategy for this system:
- Allow 10 seconds of out-of-order-ness (bounded lateness)
- Late events beyond 10s are dropped (acceptable for metrics)
- Better: use processing time with bounded delay (simpler for metrics)
```

---

## 5. Statistical Anomaly Detection

### Z-Score Method

```
z = (x - μ) / σ

Properties:
- |z| > 2: unusual (5% of normal data)
- |z| > 3: anomalous (0.3% of normal data)  ← Our threshold
- |z| > 4: highly anomalous (0.006%)
- |z| > 5: extreme (0.00006%)

Assumption: Data is roughly normally distributed
Limitation: Sensitive to outliers in training window
```

### Modified Z-Score (MAD-based) — more robust

```
MAD = median(|xi - median(X)|)
Modified Z = 0.6745 * (x - median(X)) / MAD

Benefit: Uses median instead of mean → robust to outliers
When to use: If your metrics have occasional spikes in baseline
```

### IQR (Interquartile Range)

```
Q1 = 25th percentile
Q3 = 75th percentile
IQR = Q3 - Q1

Lower fence: Q1 - 1.5 * IQR
Upper fence: Q3 + 1.5 * IQR

Anomaly if value < lower fence OR value > upper fence

Benefit: Non-parametric, no normality assumption
```

### Choosing the Right Method

| Method | Best When | Weakness |
|--------|-----------|----------|
| Z-Score | Data is normally distributed | Sensitive to outliers |
| Modified Z (MAD) | Data has occasional outliers | Slower to compute |
| IQR | Non-Gaussian distributions | Less sensitive to gradual drift |
| Isolation Forest | High-dimensional, complex patterns | Needs training data |
| Prophet | Strong seasonality (daily/weekly) | Heavyweight, batch-oriented |

**Our approach**: Z-score for POC (simple, fast), with IQR as secondary check.

---

## 6. Putting It All Together

### End-to-End Latency Breakdown

```
Metric emitted by service         0ms
  ↓
Kafka producer sends              ~5ms (linger.ms)
  ↓
Kafka broker persists             ~10ms
  ↓
Flink consumer fetches            ~50ms (fetch interval)
  ↓
Window computation + Z-score      ~1ms
  ↓
Anomaly emitted to Kafka          ~10ms
  ↓
RAG service consumes              ~50ms
  ↓
Embedding generation              ~200ms (API call)
  ↓
Vector search                     ~20ms
  ↓
LLM generation                    ~3-8 seconds
  ↓
Slack notification sent           ~200ms
  ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL: ~4-9 seconds end-to-end
```

### Why This Matters

Traditional incident response:
```
Alert fires → Engineer wakes up → Opens laptop → Checks dashboard → 
Searches logs → Searches past incidents → Forms hypothesis → 
Verifies → Takes action

Time: 15-45 minutes
```

With Anomaly Explainer:
```
Alert fires → Explanation arrives in Slack (within 10s) → 
Engineer reads context → Takes action

Time: 2-5 minutes
```

**Time saved per incident: 10-40 minutes**
**At 10 incidents/week: 2-7 hours/week saved per team**
