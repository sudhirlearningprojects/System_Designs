# Module 10: Production Deployment

## Overview

Moving RAG from prototype to production requires addressing scalability, reliability, cost optimization, and operational excellence.

---

## Architecture for Production RAG

```
                         ┌─────────────┐
                         │   CDN/WAF   │
                         └──────┬──────┘
                                │
                         ┌──────▼──────┐
                         │ API Gateway │ (Rate Limiting, Auth)
                         └──────┬──────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                  │
       ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
       │  RAG Service │  │  RAG Service │  │  RAG Service │ (Horizontal Scale)
       └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
              │                 │                  │
    ┌─────────┼─────────────────┼─────────────────┼──────────┐
    │         │                 │                  │          │
┌───▼───┐ ┌──▼───┐ ┌───────▼───────┐ ┌───▼───┐ ┌───▼────┐
│ Redis │ │Vector│ │  LLM Provider  │ │  Queue │ │ Object │
│ Cache │ │  DB  │ │(OpenAI/Bedrock)│ │(SQS)  │ │Storage │
└───────┘ └──────┘ └───────────────┘ └───────┘ └────────┘
```

---

## FastAPI Production Service

```python
from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from contextlib import asynccontextmanager
import time
import logging

logger = logging.getLogger(__name__)

# Lifespan: initialize expensive resources once
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    app.state.vectorstore = await init_vectorstore()
    app.state.llm = init_llm()
    app.state.reranker = init_reranker()
    yield
    # Shutdown
    await app.state.vectorstore.close()

app = FastAPI(lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"])

class QueryRequest(BaseModel):
    question: str
    top_k: int = 5
    filters: dict = {}

class QueryResponse(BaseModel):
    answer: str
    sources: list[dict]
    latency_ms: float
    model: str

@app.post("/v1/query", response_model=QueryResponse)
async def query_rag(request: QueryRequest, background_tasks: BackgroundTasks):
    start = time.time()
    
    try:
        # Retrieve
        docs = await app.state.vectorstore.asimilarity_search(
            request.question, k=request.top_k, filter=request.filters
        )
        
        # Rerank
        reranked = app.state.reranker.rerank(request.question, docs, top_n=3)
        
        # Generate
        context = "\n".join([d.page_content for d in reranked])
        answer = await app.state.llm.ainvoke(
            f"Context: {context}\nQuestion: {request.question}\nAnswer:"
        )
        
        latency = (time.time() - start) * 1000
        
        # Async logging (non-blocking)
        background_tasks.add_task(log_query, request.question, answer.content, latency)
        
        return QueryResponse(
            answer=answer.content,
            sources=[{"content": d.page_content[:200], "source": d.metadata.get("source")} for d in reranked],
            latency_ms=latency,
            model="gpt-4o",
        )
    except Exception as e:
        logger.error(f"Query failed: {e}")
        raise HTTPException(status_code=500, detail="Internal error")

@app.get("/health")
async def health():
    return {"status": "healthy"}
```

---

## Caching Strategy

```python
import hashlib
import json
from redis.asyncio import Redis

class RAGCache:
    """Multi-layer caching for RAG responses."""
    
    def __init__(self, redis_url: str):
        self.redis = Redis.from_url(redis_url)
        self.EXACT_TTL = 3600      # 1 hour for exact matches
        self.SEMANTIC_TTL = 1800   # 30 min for semantic matches
    
    async def get_cached(self, query: str) -> dict | None:
        # Layer 1: Exact match cache
        cache_key = f"rag:exact:{hashlib.sha256(query.encode()).hexdigest()}"
        cached = await self.redis.get(cache_key)
        if cached:
            return json.loads(cached)
        return None
    
    async def set_cache(self, query: str, response: dict):
        cache_key = f"rag:exact:{hashlib.sha256(query.encode()).hexdigest()}"
        await self.redis.setex(cache_key, self.EXACT_TTL, json.dumps(response))
    
    async def invalidate_by_source(self, source: str):
        """Invalidate cache when source documents change."""
        pattern = f"rag:*:source:{source}"
        keys = await self.redis.keys(pattern)
        if keys:
            await self.redis.delete(*keys)
```

---

## Async Ingestion Pipeline

```python
import boto3
from celery import Celery

celery_app = Celery("rag", broker="redis://localhost:6379")

@celery_app.task(bind=True, max_retries=3)
def ingest_document(self, s3_bucket: str, s3_key: str):
    """Background document ingestion task."""
    try:
        # Download from S3
        s3 = boto3.client("s3")
        content = s3.get_object(Bucket=s3_bucket, Key=s3_key)["Body"].read()
        
        # Process
        chunks = process_and_chunk(content, s3_key)
        
        # Embed and store
        vectorstore.add_documents(chunks)
        
        # Invalidate relevant caches
        cache.invalidate_by_source(s3_key)
        
        logger.info(f"Ingested {s3_key}: {len(chunks)} chunks")
    except Exception as e:
        logger.error(f"Ingestion failed: {e}")
        self.retry(countdown=60 * (2 ** self.request.retries))

# Trigger ingestion on S3 upload (via SQS/Lambda)
@app.post("/v1/ingest")
async def trigger_ingestion(bucket: str, key: str):
    ingest_document.delay(bucket, key)
    return {"status": "queued"}
```

---

## Monitoring & Observability

```python
from prometheus_client import Counter, Histogram, Gauge
import structlog

# Metrics
QUERY_COUNTER = Counter("rag_queries_total", "Total queries", ["status"])
QUERY_LATENCY = Histogram("rag_query_latency_seconds", "Query latency", buckets=[0.1, 0.5, 1, 2, 5, 10])
RETRIEVAL_QUALITY = Gauge("rag_retrieval_quality", "Average retrieval quality score")
ACTIVE_REQUESTS = Gauge("rag_active_requests", "Currently processing")

logger = structlog.get_logger()

async def monitored_query(query: str) -> dict:
    ACTIVE_REQUESTS.inc()
    start = time.time()
    
    try:
        result = await rag_pipeline(query)
        QUERY_COUNTER.labels(status="success").inc()
        
        # Log structured data
        logger.info("query_processed",
            query=query[:100],
            latency_ms=(time.time() - start) * 1000,
            num_sources=len(result["sources"]),
            model=result["model"],
        )
        return result
    except Exception as e:
        QUERY_COUNTER.labels(status="error").inc()
        logger.error("query_failed", query=query[:100], error=str(e))
        raise
    finally:
        QUERY_LATENCY.observe(time.time() - start)
        ACTIVE_REQUESTS.dec()
```

### Grafana Dashboard Queries
```
# P95 Latency
histogram_quantile(0.95, rate(rag_query_latency_seconds_bucket[5m]))

# Error Rate
rate(rag_queries_total{status="error"}[5m]) / rate(rag_queries_total[5m])

# Throughput
rate(rag_queries_total[1m])
```

---

## Cost Optimization

### Token Usage Tracking
```python
import tiktoken

class CostTracker:
    PRICING = {  # per 1M tokens
        "gpt-4o": {"input": 2.50, "output": 10.00},
        "gpt-4o-mini": {"input": 0.15, "output": 0.60},
        "text-embedding-3-small": {"input": 0.02},
    }
    
    def estimate_cost(self, model: str, input_text: str, output_text: str) -> float:
        encoder = tiktoken.encoding_for_model(model)
        input_tokens = len(encoder.encode(input_text))
        output_tokens = len(encoder.encode(output_text))
        
        pricing = self.PRICING[model]
        cost = (input_tokens * pricing["input"] + output_tokens * pricing.get("output", 0)) / 1_000_000
        return cost
```

### Cost Reduction Strategies

| Strategy | Savings | Implementation |
|----------|---------|----------------|
| Response caching | 30-70% | Redis semantic cache |
| Smaller context (rerank to top-3) | 40-60% | Reranker |
| GPT-4o-mini for simple queries | 95% | LLM router |
| Embedding dimension reduction | 50-90% storage | Matryoshka |
| Batch embedding calls | 20% | Batch API |
| Prompt compression | 30-50% | LLMLingua |

---

## Docker Deployment

```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

```yaml
# docker-compose.yml
services:
  rag-api:
    build: .
    ports: ["8000:8000"]
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - REDIS_URL=redis://redis:6379
      - QDRANT_URL=http://qdrant:6333
    depends_on: [redis, qdrant]
    deploy:
      replicas: 3
      resources:
        limits: { memory: 2G, cpus: "2" }
  
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  
  qdrant:
    image: qdrant/qdrant:latest
    ports: ["6333:6333"]
    volumes: ["./qdrant_data:/qdrant/storage"]
```

---

## AWS Production Architecture

```yaml
# Terraform sketch
resource "aws_ecs_service" "rag_api" {
  name            = "rag-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.rag_api.arn
  desired_count   = 3
  
  load_balancer {
    target_group_arn = aws_lb_target_group.rag.arn
    container_name   = "rag-api"
    container_port   = 8000
  }
}

resource "aws_elasticache_cluster" "rag_cache" {
  cluster_id      = "rag-cache"
  engine          = "redis"
  node_type       = "cache.r6g.large"
  num_cache_nodes = 2
}

# Use OpenSearch Serverless for vector storage
resource "aws_opensearchserverless_collection" "vectors" {
  name = "rag-vectors"
  type = "VECTORSEARCH"
}
```

---

## CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy RAG Service

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run unit tests
        run: pytest tests/ -v
      - name: Run RAG evaluation
        run: python evaluate.py --threshold 0.8
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}

  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to ECS
        run: |
          aws ecs update-service --cluster prod --service rag-api --force-new-deployment
```

---

## Scaling Considerations

| Component | Scaling Strategy | Bottleneck |
|-----------|-----------------|-----------|
| API Service | Horizontal (K8s/ECS autoscaling) | CPU/Memory |
| Vector DB | Sharding + replicas | Storage/Memory |
| LLM Calls | Rate limit management, queuing | API limits |
| Embeddings | Batch processing, caching | API throughput |
| Ingestion | Async workers (Celery/SQS) | I/O bound |

### Auto-Scaling Configuration
```python
# Scale based on queue depth and latency
scaling_policy = {
    "metric": "rag_query_latency_p95",
    "target": 2000,  # 2 seconds
    "scale_up_threshold": 3000,
    "scale_down_threshold": 1000,
    "min_instances": 2,
    "max_instances": 20,
    "cooldown_seconds": 60,
}
```

---

## Exercises

1. Deploy a RAG service with FastAPI + Docker + Redis caching
2. Implement cost tracking and optimize to <$0.01 per query
3. Set up Prometheus + Grafana monitoring for your RAG service
4. Build a CI/CD pipeline with automated RAG quality gates
5. Load test your service and implement auto-scaling based on latency
