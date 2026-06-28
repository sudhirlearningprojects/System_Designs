# Module 9: Performance & Scaling

---

## 9.1 Performance Characteristics

### Baseline Benchmarks (MongoDB Atlas M50, 1536-dim, cosine)

| Vectors | numCandidates | Latency (p50) | Latency (p99) | Recall@10 |
|---------|--------------|---------------|---------------|-----------|
| 100K | 150 | 3ms | 8ms | 97% |
| 1M | 150 | 8ms | 20ms | 96% |
| 5M | 150 | 15ms | 40ms | 95% |
| 10M | 200 | 25ms | 60ms | 94% |
| 50M | 200 | 45ms | 120ms | 92% |

### Key Factors Affecting Performance

```
Latency = f(numVectors, dimensions, numCandidates, filters, quantization)

1. More vectors → higher latency (logarithmic with HNSW)
2. More dimensions → higher latency (linear)
3. Higher numCandidates → higher latency, better recall
4. Narrow filters → lower latency (fewer candidates)
5. Quantization → lower latency (faster distance computation)
```

---

## 9.2 Optimizing numCandidates

The single most impactful tuning parameter:

```python
import time

def benchmark_num_candidates(query_embedding, candidates_values=[50, 100, 150, 200, 500, 1000]):
    """Find optimal numCandidates for your workload."""
    results = []
    
    for nc in candidates_values:
        latencies = []
        for _ in range(100):  # 100 iterations for stable p50/p99
            start = time.perf_counter()
            list(collection.aggregate([
                {
                    "$vectorSearch": {
                        "index": "vector_index",
                        "path": "embedding",
                        "queryVector": query_embedding,
                        "numCandidates": nc,
                        "limit": 10
                    }
                }
            ]))
            latencies.append((time.perf_counter() - start) * 1000)
        
        latencies.sort()
        results.append({
            "numCandidates": nc,
            "p50_ms": latencies[49],
            "p99_ms": latencies[98],
            "avg_ms": sum(latencies) / len(latencies)
        })
    
    return results

# Typical output:
# numCandidates=50:  p50=4ms,  p99=12ms  (low recall)
# numCandidates=100: p50=6ms,  p99=18ms  (good balance)
# numCandidates=150: p50=8ms,  p99=25ms  (recommended default)
# numCandidates=500: p50=18ms, p99=50ms  (diminishing returns)
```

### Adaptive numCandidates

```python
def adaptive_search(query: str, target_results: int = 10, min_score: float = 0.7):
    """Dynamically adjust numCandidates if initial results are poor."""
    embedding = get_embedding(query)
    
    for multiplier in [10, 20, 40]:
        results = list(collection.aggregate([
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": embedding,
                    "numCandidates": min(target_results * multiplier, 10000),
                    "limit": target_results
                }
            },
            {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
            {"$match": {"score": {"$gte": min_score}}}
        ]))
        
        if len(results) >= target_results * 0.5:  # At least 50% of target
            return results
    
    return results  # Return best effort
```

---

## 9.3 Pre-Filtering vs Post-Filtering

### Pre-Filtering (MongoDB Default — Recommended)

```
Filter applied BEFORE vector search → only searches within filtered subset

Pros:
  ✅ Always returns exactly `limit` results matching filter
  ✅ Faster when filter is very selective (small subset)
  ✅ Accurate results

Cons:
  ❌ May be slower with non-selective filters on large datasets
  ❌ Requires filter fields indexed in vector search index
```

### Performance Impact of Filters

| Filter Selectivity | % of Collection | Impact on Latency |
|-------------------|----------------|-------------------|
| Very selective | <1% | Faster (tiny search space) |
| Selective | 1-10% | Similar to no filter |
| Moderate | 10-50% | Slightly slower |
| Non-selective | >50% | May be slower than no filter |

### Optimizing Filtered Searches

```python
# ❌ Bad: Non-selective filter with low numCandidates
{
    "$vectorSearch": {
        "filter": {"status": {"$ne": "deleted"}},  # 99% of docs match
        "numCandidates": 50,  # Too low
        "limit": 10
    }
}

# ✅ Good: Selective filter
{
    "$vectorSearch": {
        "filter": {"tenant_id": {"$eq": "tenant-123"}},  # 0.1% of docs
        "numCandidates": 200,
        "limit": 10
    }
}

# ✅ Good: Compound selective filter
{
    "$vectorSearch": {
        "filter": {
            "$and": [
                {"tenant_id": {"$eq": "tenant-123"}},
                {"category": {"$in": ["tech", "science"]}},
                {"created_at": {"$gte": "2024-01-01T00:00:00Z"}}
            ]
        },
        "numCandidates": 200,
        "limit": 10
    }
}
```

---

## 9.4 Quantization for Scale

### Enabling Quantization

```python
# Update existing index to add scalar quantization
collection.update_search_index(
    name="vector_index",
    definition={
        "fields": [{
            "type": "vector",
            "path": "embedding",
            "numDimensions": 1536,
            "similarity": "cosine",
            "quantization": "scalar"  # or "binary"
        }]
    }
)
```

### Quantization Impact on Memory

```
Collection: 10M documents, 1536 dimensions

No Quantization:
  Vector data:  10M × 1536 × 4 bytes = 61.4 GB
  HNSW graph:   10M × 256 bytes = 2.56 GB
  Total:        ~64 GB → Requires M50+ tier

Scalar Quantization (int8):
  Vector data:  10M × 1536 × 1 byte = 15.4 GB
  HNSW graph:   10M × 256 bytes = 2.56 GB
  Rescore data: 10M × 1536 × 4 bytes = 61.4 GB (on disk)
  In-memory:    ~18 GB → M40 tier possible
  
Binary Quantization (1-bit):
  Vector data:  10M × 1536 / 8 bytes = 1.92 GB
  HNSW graph:   10M × 256 bytes = 2.56 GB
  Rescore data: 10M × 1536 × 4 bytes = 61.4 GB (on disk)
  In-memory:    ~4.5 GB → M30 tier possible!
```

### When to Use Each

| Quantization | Use When | Recall Impact |
|---|---|---|
| None | < 1M vectors, accuracy critical | 100% (baseline) |
| Scalar | 1-50M vectors, production default | -1-2% |
| Binary | > 50M vectors, cost-sensitive | -5-10% |

---

## 9.5 Sharding for Massive Scale

For 100M+ vectors, shard across multiple nodes:

### Sharding Strategy

```
Option 1: Range-based sharding on tenant_id
  Shard 1: tenants A-M
  Shard 2: tenants N-Z
  → Good for multi-tenant with tenant-filtered queries

Option 2: Hashed sharding on _id
  Even distribution across shards
  → Good for general-purpose search (scatter-gather)

Option 3: Zone sharding on region
  Zone 1 (us-east): US data
  Zone 2 (eu-west): EU data
  → Good for data residency requirements
```

```javascript
// Enable sharding
sh.enableSharding("mydb")
sh.shardCollection("mydb.documents", { "tenant_id": "hashed" })

// Vector search works across shards (scatter-gather)
// Each shard searches locally, results merged by mongos
```

### Sharded Performance Characteristics

| Shards | Vectors/Shard | Total Vectors | p50 Latency | Throughput |
|--------|--------------|---------------|-------------|------------|
| 1 | 10M | 10M | 25ms | 400 QPS |
| 3 | 10M | 30M | 30ms | 1200 QPS |
| 6 | 10M | 60M | 35ms | 2400 QPS |
| 12 | 10M | 120M | 40ms | 4800 QPS |

---

## 9.6 Caching Strategies

### Layer 1: Application-Level Cache

```python
from functools import lru_cache
import hashlib

@lru_cache(maxsize=10000)
def cached_embedding(text_hash: str, text: str) -> tuple:
    """Cache embeddings to avoid recomputation."""
    return tuple(get_embedding(text))

def search_with_cache(query: str, limit: int = 10):
    query_hash = hashlib.md5(query.encode()).hexdigest()
    embedding = list(cached_embedding(query_hash, query))
    return vector_search(embedding, limit)
```

### Layer 2: Redis Cache for Frequent Queries

```python
import redis
import json

r = redis.Redis(host='localhost', port=6379)
CACHE_TTL = 3600  # 1 hour

def cached_vector_search(query: str, limit: int = 10):
    cache_key = f"vs:{hashlib.md5(query.encode()).hexdigest()}:{limit}"
    
    # Check cache
    cached = r.get(cache_key)
    if cached:
        return json.loads(cached)
    
    # Execute search
    results = semantic_search(query, limit)
    
    # Cache results
    r.setex(cache_key, CACHE_TTL, json.dumps(results, default=str))
    return results
```

### Layer 3: Embedding Cache in MongoDB

```python
# Store computed embeddings to avoid API calls
embedding_cache = db["embedding_cache"]
embedding_cache.create_index("text_hash", unique=True)
embedding_cache.create_index("created_at", expireAfterSeconds=86400 * 7)  # 7-day TTL

def get_cached_embedding(text: str) -> list[float]:
    text_hash = hashlib.sha256(text.encode()).hexdigest()
    
    cached = embedding_cache.find_one({"text_hash": text_hash})
    if cached:
        return cached["embedding"]
    
    embedding = get_embedding(text)
    embedding_cache.insert_one({
        "text_hash": text_hash,
        "embedding": embedding,
        "created_at": datetime.now(timezone.utc)
    })
    return embedding
```

---

## 9.7 Batch Ingestion Optimization

```python
from concurrent.futures import ThreadPoolExecutor
from pymongo import InsertOne

def optimized_bulk_ingest(documents: list[dict], batch_size: int = 100):
    """High-throughput ingestion with parallel embedding + bulk writes."""
    
    # Parallel embedding generation
    def embed_batch(texts):
        return get_openai_embeddings_batch(texts)
    
    total = len(documents)
    
    with ThreadPoolExecutor(max_workers=4) as executor:
        for i in range(0, total, batch_size):
            batch = documents[i:i + batch_size]
            texts = [d["content"] for d in batch]
            
            # Generate embeddings in parallel
            future = executor.submit(embed_batch, texts)
            embeddings = future.result()
            
            # Prepare bulk operations
            operations = []
            for doc, embedding in zip(batch, embeddings):
                doc["embedding"] = embedding
                doc["ingested_at"] = datetime.now(timezone.utc)
                operations.append(InsertOne(doc))
            
            # Bulk write (ordered=False for max throughput)
            collection.bulk_write(operations, ordered=False)
            
            print(f"Progress: {min(i + batch_size, total)}/{total}")
    
    print(f"✅ Ingested {total} documents")
```

---

## 9.8 Monitoring & Alerting

### Key Metrics to Track

```python
import time
from dataclasses import dataclass

@dataclass
class SearchMetrics:
    query: str
    latency_ms: float
    num_results: int
    top_score: float
    avg_score: float
    
def instrumented_search(query: str, limit: int = 10) -> tuple[list, SearchMetrics]:
    """Search with metrics collection."""
    start = time.perf_counter()
    
    embedding = get_embedding(query)
    results = list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": embedding,
                "numCandidates": 150,
                "limit": limit
            }
        },
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}}
    ]))
    
    latency = (time.perf_counter() - start) * 1000
    scores = [r["score"] for r in results]
    
    metrics = SearchMetrics(
        query=query,
        latency_ms=latency,
        num_results=len(results),
        top_score=max(scores) if scores else 0,
        avg_score=sum(scores) / len(scores) if scores else 0
    )
    
    # Store metrics
    db["search_metrics"].insert_one({
        "query": query,
        "latency_ms": metrics.latency_ms,
        "num_results": metrics.num_results,
        "top_score": metrics.top_score,
        "timestamp": datetime.now(timezone.utc)
    })
    
    # Alert on degradation
    if metrics.latency_ms > 100:
        print(f"⚠️ High latency: {metrics.latency_ms:.0f}ms for query: {query[:50]}")
    if metrics.top_score < 0.5:
        print(f"⚠️ Low relevance: top score {metrics.top_score:.3f} for query: {query[:50]}")
    
    return results, metrics
```

### Atlas Performance Advisor

Monitor via Atlas UI or API:
- **Search Index Stats**: Memory usage, document count
- **Query Performance**: Latency percentiles
- **Index Health**: Build status, staleness

```bash
# Atlas CLI - check index stats
atlas clusters search indexes list --clusterName myCluster --db mydb --collection documents
```

---

## 9.9 Cost Optimization

### Cost Factors

| Factor | Impact | Optimization |
|--------|--------|-------------|
| Cluster tier | Major | Right-size based on index memory needs |
| Embedding API calls | Moderate | Cache embeddings, batch calls |
| Storage | Minor | TTL indexes, archive old data |
| Network transfer | Minor | Keep search co-located with app |

### Cost Comparison (10M vectors, 1536-dim)

| Configuration | Monthly Cost (approx) |
|---|---|
| M50, no quantization | ~$1,800 |
| M40, scalar quantization | ~$1,200 |
| M30, binary quantization | ~$800 |
| M30, scalar + reduced dims (512) | ~$600 |

### Strategies to Reduce Cost

1. **Reduce dimensions**: Use 512 instead of 1536 (OpenAI supports native reduction)
2. **Use quantization**: Scalar = 4x memory savings
3. **Archive old vectors**: Move stale data to cheaper storage
4. **Shared cluster**: Multiple small collections on one cluster
5. **Cache aggressively**: Reduce query volume to Atlas

---

## Next: [Module 10 — Production Deployment →](10_Production_Deployment.md)
