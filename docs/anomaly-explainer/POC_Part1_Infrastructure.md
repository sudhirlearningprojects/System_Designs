# POC Implementation - Part 1: Infrastructure & Data Ingestion

## 1. Docker Compose (Infrastructure)

```yaml
# docker-compose.yml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_NUM_PARTITIONS: 6

  postgres:
    image: pgvector/pgvector:pg16
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: anomaly_explainer
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  flink-jobmanager:
    image: flink:1.18-java17
    ports: ["8081:8081"]
    command: jobmanager
    environment:
      FLINK_PROPERTIES: |
        jobmanager.rpc.address: flink-jobmanager
        state.checkpoints.dir: file:///tmp/flink-checkpoints
        state.backend: rocksdb

  flink-taskmanager:
    image: flink:1.18-java17
    depends_on: [flink-jobmanager]
    command: taskmanager
    environment:
      FLINK_PROPERTIES: |
        jobmanager.rpc.address: flink-jobmanager
        taskmanager.numberOfTaskSlots: 4

volumes:
  pgdata:
```

## 2. Database Initialization

```sql
-- init.sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Incident knowledge base
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    root_cause TEXT NOT NULL,
    resolution TEXT NOT NULL,
    service TEXT NOT NULL,
    severity TEXT CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    metrics_snapshot JSONB,
    tags TEXT[],
    created_at TIMESTAMP DEFAULT NOW(),
    embedding vector(1536)
);

-- Runbooks
CREATE TABLE runbooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    service TEXT NOT NULL,
    tags TEXT[],
    embedding vector(1536)
);

-- Anomaly history (for feedback loop)
CREATE TABLE anomaly_explanations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    anomaly_id TEXT UNIQUE NOT NULL,
    service TEXT NOT NULL,
    anomaly_type TEXT NOT NULL,
    explanation JSONB NOT NULL,
    similar_incidents UUID[],
    feedback TEXT CHECK (feedback IN ('HELPFUL', 'NOT_HELPFUL', 'PARTIALLY_HELPFUL')),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for vector search
CREATE INDEX idx_incidents_embedding ON incidents 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_runbooks_embedding ON runbooks 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);
CREATE INDEX idx_incidents_service ON incidents(service);
CREATE INDEX idx_anomaly_service ON anomaly_explanations(service);
```

## 3. Kafka Metrics Producer (Simulator)

```python
# metrics_producer.py
import json
import time
import random
import uuid
from datetime import datetime, timezone
from kafka import KafkaProducer

SERVICES = [
    "payment-service", "auth-service", "order-service",
    "user-service", "notification-service", "search-service"
]

# Baseline metrics per service (mean, stddev)
BASELINES = {
    "payment-service": {"cpu": (45, 8), "memory": (60, 5), "error_rate": (0.01, 0.005), "latency_p99": (200, 30)},
    "auth-service": {"cpu": (30, 6), "memory": (40, 4), "error_rate": (0.005, 0.002), "latency_p99": (50, 10)},
    "order-service": {"cpu": (55, 10), "memory": (65, 7), "error_rate": (0.02, 0.008), "latency_p99": (300, 50)},
    "user-service": {"cpu": (25, 5), "memory": (35, 3), "error_rate": (0.003, 0.001), "latency_p99": (30, 5)},
    "notification-service": {"cpu": (20, 4), "memory": (30, 3), "error_rate": (0.01, 0.004), "latency_p99": (100, 20)},
    "search-service": {"cpu": (50, 9), "memory": (70, 6), "error_rate": (0.008, 0.003), "latency_p99": (150, 25)},
}


def create_producer():
    return KafkaProducer(
        bootstrap_servers=["localhost:9092"],
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8"),
    )


def generate_normal_metric(service: str) -> dict:
    """Generate a normal metric data point."""
    b = BASELINES[service]
    return {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "service": service,
        "instance": f"pod-{service[:4]}-{uuid.uuid4().hex[:5]}",
        "metrics": {
            "cpu_percent": max(0, min(100, random.gauss(b["cpu"][0], b["cpu"][1]))),
            "memory_percent": max(0, min(100, random.gauss(b["memory"][0], b["memory"][1]))),
            "error_rate": max(0, random.gauss(b["error_rate"][0], b["error_rate"][1])),
            "latency_p99_ms": max(1, random.gauss(b["latency_p99"][0], b["latency_p99"][1])),
            "request_count": random.randint(500, 2000),
        },
        "labels": {
            "region": "us-east-1",
            "env": "production",
        },
    }


def generate_anomalous_metric(service: str, anomaly_type: str) -> dict:
    """Generate an anomalous metric data point."""
    metric = generate_normal_metric(service)
    b = BASELINES[service]

    if anomaly_type == "cpu_spike":
        metric["metrics"]["cpu_percent"] = b["cpu"][0] + b["cpu"][1] * random.uniform(4, 7)
        metric["metrics"]["latency_p99_ms"] = b["latency_p99"][0] * random.uniform(3, 8)
    elif anomaly_type == "error_spike":
        metric["metrics"]["error_rate"] = b["error_rate"][0] * random.uniform(10, 50)
        metric["metrics"]["latency_p99_ms"] = b["latency_p99"][0] * random.uniform(2, 5)
    elif anomaly_type == "memory_leak":
        metric["metrics"]["memory_percent"] = min(99, b["memory"][0] + b["memory"][1] * random.uniform(4, 6))
        metric["metrics"]["cpu_percent"] = b["cpu"][0] * random.uniform(1.5, 2.5)
    elif anomaly_type == "latency_spike":
        metric["metrics"]["latency_p99_ms"] = b["latency_p99"][0] * random.uniform(5, 15)

    return metric


def run_producer(anomaly_after_seconds=60, anomaly_service="payment-service", anomaly_type="cpu_spike"):
    producer = create_producer()
    start_time = time.time()
    anomaly_triggered = False

    print(f"[Producer] Starting metrics stream. Anomaly will trigger after {anomaly_after_seconds}s")
    print(f"[Producer] Anomaly: {anomaly_type} on {anomaly_service}")

    while True:
        elapsed = time.time() - start_time

        for service in SERVICES:
            if elapsed > anomaly_after_seconds and service == anomaly_service and not anomaly_triggered:
                print(f"\n🚨 [Producer] INJECTING ANOMALY: {anomaly_type} on {service}")
                anomaly_triggered = True

            if anomaly_triggered and service == anomaly_service:
                metric = generate_anomalous_metric(service, anomaly_type)
            else:
                metric = generate_normal_metric(service)

            producer.send("metrics.raw", key=service, value=metric)

        producer.flush()
        time.sleep(5)  # Emit every 5 seconds


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--anomaly-after", type=int, default=60)
    parser.add_argument("--service", type=str, default="payment-service")
    parser.add_argument("--type", type=str, default="cpu_spike",
                        choices=["cpu_spike", "error_spike", "memory_leak", "latency_spike"])
    args = parser.parse_args()

    run_producer(args.anomaly_after, args.service, args.type)
```

## 4. Knowledge Base Seeder

```python
# seed_knowledge_base.py
import os
import json
import uuid
import psycopg2
from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

SAMPLE_INCIDENTS = [
    {
        "title": "Payment service CPU spike due to connection pool exhaustion",
        "description": "CPU spiked to 95% on payment-service pods. Error rate increased to 15%. "
                       "p99 latency went from 200ms to 3.2s. Caused by unclosed database connections "
                       "during batch reconciliation job that runs at 10:00 UTC.",
        "root_cause": "Database connection pool exhausted. Batch reconciliation job opened connections "
                      "without closing them due to missing finally block in TransactionReconciler.java:142.",
        "resolution": "1. Immediate: Restart pods to release connections. "
                      "2. Fix: Added try-with-resources in TransactionReconciler. "
                      "3. Prevention: Added connection pool monitoring alert at 80% utilization.",
        "service": "payment-service",
        "severity": "HIGH",
        "metrics_snapshot": {"cpu": 95, "memory": 72, "error_rate": 0.15, "latency_p99": 3200},
        "tags": ["connection-pool", "database", "batch-job", "cpu-spike"],
    },
    {
        "title": "Auth service memory leak from JWT token cache",
        "description": "Memory usage grew from 40% to 92% over 6 hours on auth-service. "
                       "Eventually caused OOM kills. Heap dump showed millions of expired JWT tokens "
                       "held in LRU cache without TTL.",
        "root_cause": "JWT token validation cache (ConcurrentHashMap) had no eviction policy. "
                      "Every validated token was cached indefinitely. With 50K req/sec, memory exhausted in ~6h.",
        "resolution": "1. Immediate: Rolling restart of auth pods. "
                      "2. Fix: Replaced HashMap with Caffeine cache (maxSize=100K, TTL=5min). "
                      "3. Prevention: Added JVM heap usage alert at 75%.",
        "service": "auth-service",
        "severity": "HIGH",
        "metrics_snapshot": {"cpu": 45, "memory": 92, "error_rate": 0.08, "latency_p99": 800},
        "tags": ["memory-leak", "cache", "jwt", "oom"],
    },
    {
        "title": "Order service latency spike from N+1 query in new feature",
        "description": "Latency p99 jumped from 300ms to 4.5s after v2.3.1 deployment. "
                       "CPU increased to 70%. Database connection wait times spiked.",
        "root_cause": "New 'order history' feature in v2.3.1 had N+1 query problem. "
                      "For each order, it fetched line items individually instead of batch. "
                      "Users with 100+ orders triggered 100+ DB queries per request.",
        "resolution": "1. Immediate: Feature-flagged the order history endpoint. "
                      "2. Fix: Added @EntityGraph to OrderRepository for eager loading. "
                      "3. Prevention: Added slow query alerting (>100ms).",
        "service": "order-service",
        "severity": "MEDIUM",
        "metrics_snapshot": {"cpu": 70, "memory": 65, "error_rate": 0.05, "latency_p99": 4500},
        "tags": ["n+1-query", "deployment", "database", "latency"],
    },
    {
        "title": "Payment service error spike from downstream PSP timeout",
        "description": "Error rate spiked to 30% on payment-service. All errors were TimeoutException "
                       "from Stripe API calls. Stripe was experiencing degraded performance in us-east-1.",
        "root_cause": "Stripe API latency increased from 200ms to 15s due to their infrastructure issue. "
                      "Our timeout was set to 10s, causing cascade of retries which amplified the problem.",
        "resolution": "1. Immediate: Enabled circuit breaker to fail-fast. "
                      "2. Fix: Reduced timeout to 3s, added fallback to PayPal processor. "
                      "3. Prevention: Circuit breaker auto-opens after 50% failure rate.",
        "service": "payment-service",
        "severity": "CRITICAL",
        "metrics_snapshot": {"cpu": 60, "memory": 55, "error_rate": 0.30, "latency_p99": 10000},
        "tags": ["downstream-failure", "timeout", "stripe", "circuit-breaker"],
    },
    {
        "title": "Search service CPU spike from regex injection in search query",
        "description": "CPU hit 100% on all search-service pods. Thread dump showed all threads stuck "
                       "in regex evaluation. A user submitted a malicious search pattern causing catastrophic backtracking.",
        "root_cause": "Search input was passed directly to regex engine without sanitization. "
                      "Pattern '(a+)+$' with input 'aaaaaaaaaaaaaaaaaaaab' caused exponential backtracking.",
        "resolution": "1. Immediate: Rate-limited the search endpoint to 10 req/sec per user. "
                      "2. Fix: Replaced regex with Elasticsearch query DSL. Sanitized all user input. "
                      "3. Prevention: Added regex complexity limit and timeout (100ms max).",
        "service": "search-service",
        "severity": "HIGH",
        "metrics_snapshot": {"cpu": 100, "memory": 70, "error_rate": 0.45, "latency_p99": 30000},
        "tags": ["regex", "dos", "security", "cpu-spike", "user-input"],
    },
    {
        "title": "Notification service queue backup from email provider rate limit",
        "description": "Notification delivery latency increased from 2s to 45min. "
                       "Kafka consumer lag grew to 2M messages. SendGrid returned 429 rate limit errors.",
        "root_cause": "Marketing campaign triggered 5M emails in 1 hour, exceeding SendGrid rate limit (1000/sec). "
                      "Retry logic without backoff caused thundering herd, worsening the situation.",
        "resolution": "1. Immediate: Paused marketing campaign, drained queue with rate limiter. "
                      "2. Fix: Added exponential backoff + jitter to retry logic. "
                      "3. Prevention: Separate queues for transactional vs marketing notifications.",
        "service": "notification-service",
        "severity": "MEDIUM",
        "metrics_snapshot": {"cpu": 35, "memory": 50, "error_rate": 0.60, "latency_p99": 45000},
        "tags": ["rate-limit", "queue-backup", "email", "thundering-herd"],
    },
    {
        "title": "Payment service memory spike after Kubernetes node migration",
        "description": "Memory usage jumped to 88% on payment-service after node drain. "
                       "All pods rescheduled to fewer nodes, causing resource contention.",
        "root_cause": "Kubernetes node maintenance drained 3 nodes simultaneously. Pods rescheduled "
                      "to remaining 2 nodes without resource quotas, causing memory pressure.",
        "resolution": "1. Immediate: Scaled up node pool to 5 nodes. "
                      "2. Fix: Added PodDisruptionBudget (maxUnavailable=1). "
                      "3. Prevention: Resource quotas per namespace, pod anti-affinity rules.",
        "service": "payment-service",
        "severity": "MEDIUM",
        "metrics_snapshot": {"cpu": 75, "memory": 88, "error_rate": 0.03, "latency_p99": 500},
        "tags": ["kubernetes", "node-drain", "memory", "scheduling"],
    },
    {
        "title": "User service cascading failure from unhealthy dependency",
        "description": "User service returning 503 for 40% of requests. Thread pool exhausted. "
                       "Root cause traced to profile-image-service being down but no circuit breaker.",
        "root_cause": "profile-image-service crashed (OOM). User-service called it synchronously for avatar URLs. "
                      "Without circuit breaker, threads blocked waiting for 30s timeout, exhausting thread pool.",
        "resolution": "1. Immediate: Restarted profile-image-service, increased user-service thread pool. "
                      "2. Fix: Made avatar fetch async with fallback to default image. "
                      "3. Prevention: Added Resilience4j circuit breaker (threshold=50%, wait=30s).",
        "service": "user-service",
        "severity": "HIGH",
        "metrics_snapshot": {"cpu": 85, "memory": 60, "error_rate": 0.40, "latency_p99": 30000},
        "tags": ["cascading-failure", "circuit-breaker", "thread-pool", "dependency"],
    },
]


def get_embedding(text: str) -> list:
    """Get embedding from OpenAI."""
    response = client.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding


def seed_database():
    conn = psycopg2.connect(
        host="localhost", port=5432,
        dbname="anomaly_explainer", user="admin", password="admin123"
    )
    cur = conn.cursor()

    print("🌱 Seeding incident knowledge base...")

    for incident in SAMPLE_INCIDENTS:
        # Create embedding from combined text
        embed_text = f"{incident['title']}. {incident['description']}. Root cause: {incident['root_cause']}"
        embedding = get_embedding(embed_text)

        cur.execute("""
            INSERT INTO incidents (title, description, root_cause, resolution, service, severity, metrics_snapshot, tags, embedding)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT DO NOTHING
        """, (
            incident["title"], incident["description"], incident["root_cause"],
            incident["resolution"], incident["service"], incident["severity"],
            json.dumps(incident["metrics_snapshot"]), incident["tags"],
            str(embedding)
        ))
        print(f"  ✓ Seeded: {incident['title'][:60]}...")

    conn.commit()
    cur.close()
    conn.close()
    print(f"\n✅ Seeded {len(SAMPLE_INCIDENTS)} incidents")


if __name__ == "__main__":
    seed_database()
```

## 5. Project Dependencies

```txt
# requirements.txt
kafka-python==2.0.2
psycopg2-binary==2.9.9
openai==1.12.0
fastapi==0.109.0
uvicorn==0.27.0
pydantic==2.5.0
redis==5.0.1
numpy==1.26.0
httpx==0.26.0
python-dotenv==1.0.0
```

```env
# .env
OPENAI_API_KEY=sk-your-key-here
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=anomaly_explainer
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123
REDIS_URL=redis://localhost:6379
LLM_MODEL=gpt-4o
EMBEDDING_MODEL=text-embedding-3-small
ANOMALY_THRESHOLD_ZSCORE=3.0
VECTOR_SEARCH_TOP_K=5
```
