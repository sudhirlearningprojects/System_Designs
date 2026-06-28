# System Design - Real-Time Anomaly Explainer

## 1. Problem Deep Dive

### Why Existing Solutions Fail

| Tool | What it does | What it lacks |
|------|-------------|---------------|
| Prometheus/Grafana | Detects threshold breaches | No *why* explanation |
| PagerDuty | Routes alerts | No correlation with past incidents |
| Datadog APM | Shows traces | No automated root cause synthesis |
| ELK Stack | Searches logs | Requires manual query crafting |

**The Gap**: Tools tell you *what* is broken, not *why* it broke or *how* to fix it.

### User Story

> As an on-call engineer, when I get a "CPU spike on payment-service" alert at 3 AM, I want to immediately know: "This looks like the memory leak from INC-2847 (March 3rd) caused by unclosed DB connections during batch processing. Fix: restart pods + apply connection pool patch."

---

## 2. Core Concepts

### 2.1 Anomaly Detection (Apache Flink)

**Z-Score Method**:
```
z = (x - μ) / σ

where:
  x = current metric value
  μ = rolling mean over window
  σ = rolling standard deviation
  
If |z| > 3 → ANOMALY
```

**Why Flink?**
- True streaming (not micro-batch like Spark Streaming)
- Event-time processing with watermarks (handles late data)
- Exactly-once state guarantees
- Sub-second latency for sliding windows

### 2.2 RAG (Retrieval Augmented Generation)

**How it works**:
1. Convert anomaly context to an embedding vector
2. Search vector DB for semantically similar past incidents
3. Retrieve top-K matching documents (incidents, runbooks, logs)
4. Feed context + anomaly info to LLM
5. LLM generates explanation grounded in real historical data

**Why RAG over fine-tuning?**
- No model training needed (faster iteration)
- Knowledge updates instantly (just add new docs)
- Traceable: you can see which incidents influenced the answer
- Cheaper: no GPU training costs

### 2.3 Vector Database

**Embedding similarity search**:
```
Query: "payment-service CPU at 95%, error rate 12%, latency p99 = 3.2s"
         ↓ embed
    [0.23, -0.45, 0.67, ...] (1536-dim vector)
         ↓ cosine similarity
    Top matches:
      1. INC-2847: "payment-service CPU spike due to connection leak" (0.94)
      2. INC-3102: "auth-service CPU from runaway thread" (0.78)
      3. INC-1923: "payment-service latency from DB lock contention" (0.75)
```

---

## 3. High-Level Design

### 3.1 Component Diagram

```
┌──────────────────────────────────────────────────────────┐
│                    INGESTION LAYER                         │
│                                                           │
│  Kafka Topics:                                           │
│  • metrics.raw     (CPU, memory, disk, network)          │
│  • logs.raw        (application logs, structured)        │
│  • events.raw      (deploys, config changes, k8s)        │
└───────────────────────────┬──────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                  DETECTION LAYER (Flink)                   │
│                                                           │
│  Jobs:                                                   │
│  • MetricAnomalyDetector (sliding window + Z-score)      │
│  • LogPatternDetector (error rate spike detection)        │
│  • CorrelationEngine (multi-signal correlation)          │
│                                                           │
│  Output: anomalies.detected topic                        │
└───────────────────────────┬──────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                 EXPLANATION LAYER (RAG)                    │
│                                                           │
│  • EmbeddingService → converts anomaly to vector         │
│  • VectorSearch → finds similar past incidents           │
│  • ContextAssembler → builds LLM prompt                  │
│  • LLMService → generates explanation                    │
└───────────────────────────┬──────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                  DELIVERY LAYER                            │
│                                                           │
│  • SlackNotifier → rich message with explanation         │
│  • PagerDutyEnricher → adds context to existing alert    │
│  • DashboardWriter → stores for historical view          │
│  • FeedbackCollector → "Was this helpful?" reactions     │
└──────────────────────────────────────────────────────────┘
```

### 3.2 Data Flow

```
1. Prometheus scrapes metrics every 15s
2. Kafka Connect pushes to `metrics.raw` topic
3. Flink consumes, computes 5-min sliding window stats
4. If Z-score > 3 → emit AnomalyEvent to `anomalies.detected`
5. RAG Service consumes AnomalyEvent
6. Embeds anomaly context → searches vector DB
7. Retrieves top-5 similar incidents + relevant runbooks
8. Sends to LLM with structured prompt
9. Posts explanation to Slack within 15 seconds
```

### 3.3 Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Stream processor | Flink over Spark | True streaming, lower latency |
| Vector DB | Pgvector (POC), Qdrant (prod) | Pgvector = simple setup; Qdrant = better scale |
| Embedding model | text-embedding-3-small | Good quality, cheap, fast |
| LLM | Claude 3.5 Sonnet | Best reasoning, structured output |
| Communication | Kafka everywhere | Decouples components, replay capability |
| State | Flink managed state | Exactly-once, checkpointed |

---

## 4. Low-Level Design

### 4.1 Kafka Topic Schema

**metrics.raw** (Avro):
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "service": "payment-service",
  "instance": "pod-payment-7d4f8-abc12",
  "metrics": {
    "cpu_percent": 87.5,
    "memory_percent": 72.3,
    "error_rate": 0.12,
    "latency_p99_ms": 3200,
    "request_count": 1520
  },
  "labels": {
    "region": "us-east-1",
    "env": "production",
    "team": "payments"
  }
}
```

**anomalies.detected**:
```json
{
  "anomaly_id": "anom-uuid-123",
  "timestamp": "2024-01-15T10:30:15Z",
  "service": "payment-service",
  "severity": "HIGH",
  "anomaly_type": "CPU_SPIKE",
  "current_value": 95.2,
  "baseline_mean": 45.0,
  "baseline_stddev": 8.5,
  "z_score": 5.9,
  "window_duration_sec": 300,
  "correlated_signals": [
    {"metric": "error_rate", "z_score": 3.2},
    {"metric": "latency_p99", "z_score": 4.1}
  ],
  "recent_events": [
    {"type": "DEPLOYMENT", "time": "2024-01-15T10:25:00Z", "detail": "v2.3.1 deployed"}
  ]
}
```

### 4.2 Vector DB Schema (Pgvector)

```sql
CREATE EXTENSION vector;

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    root_cause TEXT NOT NULL,
    resolution TEXT NOT NULL,
    service TEXT NOT NULL,
    severity TEXT NOT NULL,
    metrics_snapshot JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    embedding vector(1536)
);

CREATE INDEX idx_incidents_embedding 
    ON incidents USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE TABLE runbooks (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    service TEXT NOT NULL,
    tags TEXT[],
    embedding vector(1536)
);

CREATE INDEX idx_runbooks_embedding 
    ON runbooks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 50);
```

### 4.3 LLM Prompt Template

```
You are an expert SRE analyzing a production anomaly. Based on the current anomaly 
and similar past incidents, provide a root cause explanation and suggested fix.

## Current Anomaly
- Service: {service}
- Type: {anomaly_type}
- Current Value: {current_value} (baseline: {baseline_mean} ± {baseline_stddev})
- Z-Score: {z_score}
- Correlated Signals: {correlated_signals}
- Recent Events: {recent_events}

## Similar Past Incidents
{similar_incidents}

## Relevant Runbooks
{runbook_excerpts}

## Instructions
1. Identify the most likely root cause based on pattern matching with past incidents
2. Explain WHY this is happening (not just what)
3. Provide immediate mitigation steps
4. Suggest long-term fix if applicable
5. Rate your confidence (HIGH/MEDIUM/LOW)

Respond in this JSON format:
{
  "root_cause": "...",
  "explanation": "...",
  "immediate_fix": ["step1", "step2"],
  "long_term_fix": "...",
  "confidence": "HIGH|MEDIUM|LOW",
  "similar_incident_id": "...",
  "estimated_impact": "..."
}
```

### 4.4 Flink Job Design

```
Source(Kafka metrics.raw)
    │
    ▼
KeyBy(service_name)
    │
    ▼
SlidingWindow(size=5min, slide=30sec)
    │
    ▼
ProcessWindowFunction:
  - Compute mean, stddev for each metric
  - Calculate Z-score for latest value
  - If |z| > threshold → emit AnomalyEvent
    │
    ▼
AsyncIO(enrich with recent deploys/events)
    │
    ▼
Sink(Kafka anomalies.detected)
```
