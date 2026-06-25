# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 6: Production Deployment & Monitoring

---

## Production Architecture (AWS)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         AWS Region (us-east-1)                        │
│                                                                       │
│  ┌─────────────┐    ┌──────────────────────────────────┐           │
│  │ Route 53    │    │  CloudFront (API caching)         │           │
│  └──────┬──────┘    └───────────────┬──────────────────┘           │
│         │                           │                               │
│  ┌──────▼───────────────────────────▼──────┐                       │
│  │           Application Load Balancer      │                       │
│  └──────────────────┬──────────────────────┘                       │
│                     │                                               │
│  ┌──────────────────▼──────────────────────┐                       │
│  │     ECS Fargate / EKS (RAG Service)      │                       │
│  │     (Auto-scaling: 2-20 tasks)           │                       │
│  └────┬──────────┬──────────┬──────────────┘                       │
│       │          │          │                                       │
│  ┌────▼────┐ ┌───▼────┐ ┌──▼─────────────┐                       │
│  │ElastiCache│ │Confluent│ │ MongoDB Atlas  │                       │
│  │ (Redis)  │ │ Cloud   │ │ (M40+ cluster) │                       │
│  └──────────┘ │ Kafka   │ └───────────────┘                       │
│               │ + Flink │                                           │
│               └─────────┘                                           │
│                                                                       │
│  ┌──────────────────────────────────────────┐                       │
│  │  Monitoring: CloudWatch + Datadog/Grafana │                       │
│  └──────────────────────────────────────────┘                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Confluent Cloud Setup

```terraform
# terraform/confluent.tf
resource "confluent_kafka_cluster" "rag" {
  display_name = "rag-production"
  availability = "MULTI_ZONE"
  cloud        = "AWS"
  region       = "us-east-1"

  dedicated {
    cku = 2  # 2 CKUs for production throughput
  }
}

resource "confluent_kafka_topic" "raw_documents" {
  kafka_cluster { id = confluent_kafka_cluster.rag.id }
  topic_name       = "raw.documents"
  partitions_count = 12
  config = {
    "retention.ms"      = "604800000"  # 7 days
    "cleanup.policy"    = "delete"
    "compression.type"  = "lz4"
    "max.message.bytes" = "10485760"
  }
}

resource "confluent_kafka_topic" "embeddings" {
  kafka_cluster { id = confluent_kafka_cluster.rag.id }
  topic_name       = "embeddings"
  partitions_count = 24
  config = {
    "retention.ms"   = "86400000"  # 1 day
    "cleanup.policy" = "delete"
  }
}

# Flink compute pool
resource "confluent_flink_compute_pool" "rag_processing" {
  display_name = "rag-flink-pool"
  cloud        = "AWS"
  region       = "us-east-1"
  max_cfu      = 20  # Confluent Flink Units
}
```

---

## MongoDB Atlas Production Config

```terraform
# terraform/mongodb.tf
resource "mongodbatlas_advanced_cluster" "rag" {
  project_id   = var.atlas_project_id
  name         = "rag-production"
  cluster_type = "REPLICASET"

  replication_specs {
    region_configs {
      provider_name = "AWS"
      region_name   = "US_EAST_1"
      priority      = 7

      electable_specs {
        instance_size = "M40"  # Production tier with vector search
        node_count    = 3
      }

      analytics_specs {
        instance_size = "M40"
        node_count    = 1  # Analytics node for vector search workloads
      }
    }
  }

  advanced_configuration {
    javascript_enabled = false
    oplog_size_mb      = 2048
  }
}

# Vector Search Index
resource "mongodbatlas_search_index" "vector" {
  project_id  = var.atlas_project_id
  cluster_name = mongodbatlas_advanced_cluster.rag.name
  database    = "rag_db"
  collection  = "document_chunks"
  name        = "vector_index"
  type        = "vectorSearch"

  fields = jsonencode([
    {
      type          = "vector"
      path          = "embedding"
      numDimensions = 1024
      similarity    = "cosine"
    },
    { type = "filter", path = "tenant_id" },
    { type = "filter", path = "access_control" },
  ])
}
```

---

## ECS/EKS Deployment

```yaml
# kubernetes/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rag-api
  template:
    metadata:
      labels:
        app: rag-api
    spec:
      containers:
        - name: rag-api
          image: <account>.dkr.ecr.us-east-1.amazonaws.com/rag-api:latest
          ports:
            - containerPort: 8000
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          env:
            - name: MONGODB_URI
              valueFrom:
                secretKeyRef:
                  name: rag-secrets
                  key: mongodb-uri
            - name: OPENAI_API_KEY
              valueFrom:
                secretKeyRef:
                  name: rag-secrets
                  key: openai-api-key
            - name: REDIS_URL
              value: "redis://rag-cache.xxxxx.ng.0001.use1.cache.amazonaws.com:6379"
          livenessProbe:
            httpGet:
              path: /health
              port: 8000
            initialDelaySeconds: 10
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /health
              port: 8000
            initialDelaySeconds: 5
            periodSeconds: 10
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rag-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      pods:
        metric:
          name: rag_query_latency_p95
        target:
          type: AverageValue
          averageValue: "3000m"  # Scale up if P95 > 3s
```

---

## Monitoring & Observability

### Prometheus Metrics

```python
# app/metrics.py
from prometheus_client import Counter, Histogram, Gauge, Info

# Ingestion metrics
DOCS_INGESTED = Counter("rag_documents_ingested_total", "Total documents ingested", ["source", "tenant"])
INGESTION_LAG = Gauge("rag_ingestion_lag_seconds", "Kafka consumer lag in seconds", ["topic"])
CHUNKS_CREATED = Counter("rag_chunks_created_total", "Total chunks created", ["tenant"])

# Query metrics
QUERIES_TOTAL = Counter("rag_queries_total", "Total queries", ["status", "tenant"])
QUERY_LATENCY = Histogram(
    "rag_query_latency_seconds", "Query latency",
    buckets=[0.1, 0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 10.0]
)
RETRIEVAL_LATENCY = Histogram("rag_retrieval_latency_seconds", "Retrieval latency", buckets=[0.05, 0.1, 0.25, 0.5, 1.0])
RERANK_LATENCY = Histogram("rag_rerank_latency_seconds", "Rerank latency", buckets=[0.05, 0.1, 0.25, 0.5])
LLM_LATENCY = Histogram("rag_llm_latency_seconds", "LLM generation latency", buckets=[0.5, 1.0, 2.0, 3.0, 5.0])

# Quality metrics
RETRIEVAL_SCORES = Histogram("rag_retrieval_score", "Top-1 retrieval similarity score", buckets=[0.5, 0.6, 0.7, 0.8, 0.9, 0.95])
CACHE_HITS = Counter("rag_cache_hits_total", "Cache hit count", ["type"])
EMPTY_RESULTS = Counter("rag_empty_results_total", "Queries with no results", ["tenant"])

# Cost metrics
TOKENS_USED = Counter("rag_tokens_used_total", "Total tokens consumed", ["model", "type"])
EMBEDDING_CALLS = Counter("rag_embedding_api_calls_total", "Embedding API calls")

# System metrics
VECTOR_DB_CONNECTIONS = Gauge("rag_mongodb_connections_active", "Active MongoDB connections")
DOCUMENTS_INDEXED = Gauge("rag_documents_indexed_total", "Total documents in vector store", ["tenant"])
```

### Grafana Dashboard Panels

```json
{
  "panels": [
    {
      "title": "Query Latency (P95)",
      "query": "histogram_quantile(0.95, rate(rag_query_latency_seconds_bucket[5m]))"
    },
    {
      "title": "Queries per Second",
      "query": "rate(rag_queries_total[1m])"
    },
    {
      "title": "Error Rate",
      "query": "rate(rag_queries_total{status='error'}[5m]) / rate(rag_queries_total[5m]) * 100"
    },
    {
      "title": "Kafka Consumer Lag",
      "query": "rag_ingestion_lag_seconds"
    },
    {
      "title": "Cache Hit Rate",
      "query": "rate(rag_cache_hits_total[5m]) / rate(rag_queries_total[5m]) * 100"
    },
    {
      "title": "Retrieval Quality (Avg Score)",
      "query": "histogram_quantile(0.5, rate(rag_retrieval_score_bucket[5m]))"
    },
    {
      "title": "Cost (Tokens/min)",
      "query": "rate(rag_tokens_used_total[1m])"
    },
    {
      "title": "Documents Ingested (Rate)",
      "query": "rate(rag_documents_ingested_total[5m])"
    }
  ]
}
```

---

## Alerting Rules

```yaml
# prometheus/alerts.yml
groups:
  - name: rag_alerts
    rules:
      - alert: HighQueryLatency
        expr: histogram_quantile(0.95, rate(rag_query_latency_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "RAG P95 latency > 5s"
      
      - alert: HighErrorRate
        expr: rate(rag_queries_total{status="error"}[5m]) / rate(rag_queries_total[5m]) > 0.05
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "RAG error rate > 5%"
      
      - alert: KafkaLagHigh
        expr: rag_ingestion_lag_seconds > 300
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kafka ingestion lag > 5 minutes"
      
      - alert: LowRetrievalQuality
        expr: histogram_quantile(0.5, rate(rag_retrieval_score_bucket[30m])) < 0.6
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Median retrieval score dropped below 0.6"
      
      - alert: EmptyResultsSpike
        expr: rate(rag_empty_results_total[10m]) > 0.2 * rate(rag_queries_total[10m])
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: ">20% queries returning empty results"
```

---

## End-to-End Health Check

```python
# app/health.py
from fastapi import APIRouter
from pymongo import MongoClient
from redis.asyncio import Redis
from confluent_kafka import Consumer

router = APIRouter()

@router.get("/health")
async def health_check():
    checks = {}
    
    # MongoDB
    try:
        db.command("ping")
        checks["mongodb"] = "healthy"
    except Exception as e:
        checks["mongodb"] = f"unhealthy: {e}"
    
    # Redis
    try:
        await redis.ping()
        checks["redis"] = "healthy"
    except Exception as e:
        checks["redis"] = f"unhealthy: {e}"
    
    # Kafka (check consumer lag)
    try:
        # Check if consumer is not too far behind
        lag = get_consumer_lag("embeddings", "rag-app")
        checks["kafka"] = f"healthy (lag: {lag})"
    except Exception as e:
        checks["kafka"] = f"unhealthy: {e}"
    
    # Vector Search Index
    try:
        result = list(collection.aggregate([
            {"$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": [0.0] * 1024,
                "numCandidates": 1,
                "limit": 1,
            }}
        ]))
        checks["vector_search"] = "healthy"
    except Exception as e:
        checks["vector_search"] = f"unhealthy: {e}"
    
    overall = "healthy" if all("healthy" in v for v in checks.values()) else "degraded"
    return {"status": overall, "checks": checks}
```

---

## Cost Analysis

### Monthly Cost Estimate (Medium Scale)

| Component | Spec | Monthly Cost |
|-----------|------|-------------|
| Confluent Cloud Kafka | Dedicated, 2 CKU | ~$2,400 |
| Confluent Flink | 10 CFU avg | ~$1,500 |
| MongoDB Atlas | M40, 3 nodes | ~$1,200 |
| ECS Fargate | 3-10 tasks (2vCPU, 4GB) | ~$600 |
| ElastiCache Redis | r6g.large, 2 nodes | ~$400 |
| OpenAI Embeddings | ~10M chunks/month | ~$200 |
| OpenAI GPT-4o | ~500K queries/month | ~$3,000 |
| **Total** | | **~$9,300/month** |

### Cost Optimization Strategies

| Strategy | Savings | Trade-off |
|----------|---------|-----------|
| GPT-4o-mini instead of GPT-4o | ~90% LLM cost | Slight quality drop |
| Cache hit rate >50% | ~50% LLM+embedding cost | Stale responses |
| Matryoshka 512 dims | 50% MongoDB storage | ~2% recall drop |
| Confluent Basic cluster | 60% Kafka cost | Single-zone, lower throughput |
| Self-hosted embeddings | 95% embedding cost | Infra management |
| MongoDB M30 tier | 40% Atlas cost | Lower vector search perf |

---

## CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy RAG Pipeline

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run unit tests
        run: pytest tests/ -v --cov=app
      
      - name: Run integration tests (with testcontainers)
        run: pytest tests/integration/ -v
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
      
      - name: Run RAG quality evaluation
        run: |
          python scripts/evaluate_rag.py \
            --test-set tests/eval/test_questions.json \
            --min-faithfulness 0.85 \
            --min-relevance 0.80

  deploy-flink:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Deploy Flink jobs
        run: |
          # Package and submit Flink jobs
          mvn clean package -f flink-jobs/pom.xml
          confluent flink statement create \
            --compute-pool ${{ vars.FLINK_POOL_ID }} \
            --statement "$(cat flink-jobs/chunking-job.sql)"

  deploy-api:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build and push Docker image
        run: |
          docker build -t rag-api:${{ github.sha }} .
          docker push <account>.dkr.ecr.us-east-1.amazonaws.com/rag-api:${{ github.sha }}
      
      - name: Deploy to EKS
        run: |
          kubectl set image deployment/rag-api \
            rag-api=<account>.dkr.ecr.us-east-1.amazonaws.com/rag-api:${{ github.sha }}
          kubectl rollout status deployment/rag-api --timeout=300s
```

---

## Disaster Recovery

| Scenario | Recovery Strategy | RTO |
|----------|-------------------|-----|
| MongoDB node failure | Auto-failover (replica set) | <30s |
| Kafka broker failure | Multi-AZ replication | <60s |
| Flink job crash | Auto-restart from checkpoint | <5min |
| Full region failure | Cross-region MongoDB replica + Confluent MRC | <15min |
| Embedding API outage | Queue in Kafka, replay when recovered | Eventual |
| Data corruption | Kafka replay from offset + MongoDB point-in-time recovery | <1hr |

---

## Summary: Complete Pipeline Flow

```
1. Data Change (any source)
        ↓
2. Kafka Producer → raw.documents topic
        ↓
3. Flink Job 1: Parse & Clean → cleaned.documents
        ↓
4. Flink Job 2: Chunk (deduplicated) → chunked.documents
        ↓
5. Flink Job 3: Batch Embed (with backpressure) → embeddings
        ↓
6. MongoDB Kafka Sink Connector → document_chunks collection
        ↓
7. Atlas Vector Search Index (auto-updated)
        ↓
8. RAG API: Query → Retrieve → Rerank → Generate → Response

End-to-end latency: Document change → Available for retrieval: ~5-30 seconds
Query latency: P95 < 3 seconds
```

---

## Further Reading

- [Confluent Kafka Documentation](https://docs.confluent.io/)
- [Apache Flink Documentation](https://flink.apache.org/docs/)
- [MongoDB Atlas Vector Search](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [LangChain MongoDB Integration](https://python.langchain.com/docs/integrations/vectorstores/mongodb_atlas)

---

## Back to Course

- [← Part 5: RAG Application & Query Layer](./kafka-flink-mongodb-rag-part5.md)
- [← Course Home](../README.md)
