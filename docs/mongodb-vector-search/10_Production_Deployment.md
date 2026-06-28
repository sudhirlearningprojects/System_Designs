# Module 10: Production Deployment

---

## 10.1 Production Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                        Production Architecture                      │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐          │
│  │   CDN    │◄───│  API Gateway │◄───│  Load Balancer  │          │
│  └──────────┘    │  (Rate Limit)│    └─────────────────┘          │
│                  └──────┬───────┘                                   │
│                         │                                           │
│           ┌─────────────┼─────────────┐                            │
│           ▼             ▼             ▼                             │
│  ┌──────────────┐ ┌──────────┐ ┌──────────────┐                   │
│  │  Search API  │ │ Ingest   │ │  Admin API   │                   │
│  │  (Read-Heavy)│ │ Workers  │ │              │                   │
│  └──────┬───────┘ └────┬─────┘ └──────────────┘                   │
│         │               │                                           │
│         ▼               ▼                                           │
│  ┌───────────────────────────────────┐                             │
│  │          Redis Cache              │                             │
│  │  (Embeddings + Query Results)     │                             │
│  └───────────────────┬───────────────┘                             │
│                      │                                              │
│                      ▼                                              │
│  ┌───────────────────────────────────┐                             │
│  │     MongoDB Atlas (M50+)          │                             │
│  │  ┌─────────┐  ┌─────────────────┐│                             │
│  │  │ Vector  │  │ Operational     ││                             │
│  │  │ Index   │  │ Data + Metadata ││                             │
│  │  └─────────┘  └─────────────────┘│                             │
│  └───────────────────────────────────┘                             │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 10.2 Security Best Practices

### Network Security

```python
# 1. Use Private Endpoints (AWS PrivateLink / Azure Private Link)
# Configure in Atlas UI: Network Access → Private Endpoint

# 2. IP Whitelist - restrict to application servers only
# Atlas UI: Network Access → IP Access List

# 3. TLS/SSL (always enabled on Atlas)
client = MongoClient(
    "mongodb+srv://...",
    tls=True,
    tlsCAFile="/path/to/ca-cert.pem"  # For custom CA
)
```

### Authentication & Authorization

```python
# Database user with minimal permissions
# Atlas UI: Database Access → Add Database User

# Read-only user for search API
{
    "roles": [
        {"role": "read", "db": "vector_db"}
    ]
}

# Read-write user for ingestion
{
    "roles": [
        {"role": "readWrite", "db": "vector_db"}
    ]
}
```

### API Key Security

```python
# Never embed API keys in code
import os
from cryptography.fernet import Fernet

class SecureConfig:
    def __init__(self):
        # Use environment variables or AWS Secrets Manager
        self.mongodb_uri = os.environ["MONGODB_URI"]
        self.openai_key = os.environ["OPENAI_API_KEY"]
    
    @staticmethod
    def from_aws_secrets(secret_name: str):
        """Load from AWS Secrets Manager."""
        import boto3
        client = boto3.client("secretsmanager")
        response = client.get_secret_value(SecretId=secret_name)
        secrets = json.loads(response["SecretString"])
        return secrets
```

### Data Security

```python
# Field-level encryption for sensitive metadata
from pymongo.encryption import ClientEncryption

# Encrypt PII fields before storage
encrypted_doc = {
    "content": "public content...",
    "embedding": [...],
    "metadata": {
        "user_email": encrypt_field("user@example.com"),  # Encrypted
        "tenant_id": "tenant-123"  # Not encrypted (used in filters)
    }
}
```

---

## 10.3 Error Handling & Resilience

```python
import time
from pymongo.errors import (
    ConnectionFailure, ServerSelectionTimeoutError, 
    OperationFailure, ExecutionTimeout
)

class ResilientVectorSearch:
    def __init__(self, collection, max_retries=3):
        self.collection = collection
        self.max_retries = max_retries
    
    def search(self, query_embedding: list, limit: int = 10, filter_expr=None) -> list:
        """Vector search with retry, timeout, and fallback."""
        
        pipeline = [
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": query_embedding,
                    "numCandidates": min(limit * 15, 10000),
                    "limit": limit,
                    **({"filter": filter_expr} if filter_expr else {})
                }
            },
            {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
            {"$project": {"content": 1, "title": 1, "score": 1}}
        ]
        
        for attempt in range(self.max_retries):
            try:
                results = list(self.collection.aggregate(
                    pipeline,
                    maxTimeMS=5000  # 5-second timeout
                ))
                return results
                
            except ExecutionTimeout:
                # Reduce numCandidates for faster retry
                pipeline[0]["$vectorSearch"]["numCandidates"] = limit * 5
                
            except (ConnectionFailure, ServerSelectionTimeoutError):
                if attempt < self.max_retries - 1:
                    time.sleep(2 ** attempt)  # Exponential backoff
                    continue
                raise
                
            except OperationFailure as e:
                if "index not found" in str(e):
                    raise RuntimeError("Vector search index not available")
                raise
        
        return []  # Fallback: empty results
```

---

## 10.4 CI/CD for Vector Search

### Index Management in CI/CD

```python
# deploy_indexes.py — Run in CI/CD pipeline
import sys
from pymongo import MongoClient
from pymongo.operations import SearchIndexModel

def deploy_vector_indexes(uri: str, db_name: str):
    """Idempotent index deployment."""
    client = MongoClient(uri)
    collection = client[db_name]["documents"]
    
    desired_indexes = {
        "vector_index": {
            "fields": [
                {"type": "vector", "path": "embedding", "numDimensions": 1536, "similarity": "cosine", "quantization": "scalar"},
                {"type": "filter", "path": "tenant_id"},
                {"type": "filter", "path": "category"},
                {"type": "filter", "path": "created_at"}
            ]
        }
    }
    
    # Get existing indexes
    existing = {idx["name"]: idx for idx in collection.list_search_indexes()}
    
    for name, definition in desired_indexes.items():
        if name in existing:
            # Update if definition changed
            existing_def = existing[name].get("latestDefinition", {})
            if existing_def != definition:
                print(f"Updating index: {name}")
                collection.update_search_index(name=name, definition=definition)
            else:
                print(f"Index unchanged: {name}")
        else:
            # Create new
            print(f"Creating index: {name}")
            collection.create_search_index(
                SearchIndexModel(definition=definition, name=name, type="vectorSearch")
            )
    
    # Wait for indexes to be ready
    wait_for_indexes(collection, list(desired_indexes.keys()))

def wait_for_indexes(collection, index_names, timeout=300):
    """Wait for all indexes to reach READY state."""
    start = time.time()
    while time.time() - start < timeout:
        all_ready = True
        for idx in collection.list_search_indexes():
            if idx["name"] in index_names and idx["status"] != "READY":
                all_ready = False
                print(f"  {idx['name']}: {idx['status']}")
        if all_ready:
            print("✅ All indexes ready")
            return
        time.sleep(10)
    
    print("⚠️ Timeout waiting for indexes")
    sys.exit(1)

if __name__ == "__main__":
    deploy_vector_indexes(os.environ["MONGODB_URI"], os.environ["MONGODB_DATABASE"])
```

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy-indexes.yml
name: Deploy Vector Search Indexes

on:
  push:
    paths: ['infra/indexes/**']
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - run: pip install pymongo[srv]
      - run: python infra/indexes/deploy_indexes.py
        env:
          MONGODB_URI: ${{ secrets.MONGODB_URI }}
          MONGODB_DATABASE: vector_search_db
```

---

## 10.5 Embedding Model Migration

When you need to change embedding models (e.g., upgrade from text-embedding-ada-002 to text-embedding-3-small):

```python
def migrate_embeddings(
    old_model: str,
    new_model: str,
    new_dimensions: int,
    batch_size: int = 100
):
    """Zero-downtime embedding model migration."""
    
    # Step 1: Create new vector field and index
    new_index = SearchIndexModel(
        definition={
            "fields": [{
                "type": "vector",
                "path": "embedding_v2",  # New field
                "numDimensions": new_dimensions,
                "similarity": "cosine"
            }]
        },
        name="vector_index_v2",
        type="vectorSearch"
    )
    collection.create_search_index(new_index)
    
    # Step 2: Backfill new embeddings
    cursor = collection.find(
        {"embedding_v2": {"$exists": False}},
        {"content": 1, "title": 1}
    ).batch_size(batch_size)
    
    batch = []
    for doc in cursor:
        batch.append(doc)
        if len(batch) >= batch_size:
            texts = [f"{d['title']}\n{d['content']}" for d in batch]
            embeddings = get_openai_embeddings_batch(texts, model=new_model)
            
            operations = [
                UpdateOne(
                    {"_id": d["_id"]},
                    {"$set": {"embedding_v2": emb, "embedding_model": new_model}}
                )
                for d, emb in zip(batch, embeddings)
            ]
            collection.bulk_write(operations)
            batch = []
    
    # Process remaining
    if batch:
        texts = [f"{d['title']}\n{d['content']}" for d in batch]
        embeddings = get_openai_embeddings_batch(texts, model=new_model)
        operations = [
            UpdateOne({"_id": d["_id"]}, {"$set": {"embedding_v2": emb}})
            for d, emb in zip(batch, embeddings)
        ]
        collection.bulk_write(operations)
    
    # Step 3: Switch application to use vector_index_v2
    # Step 4: Drop old index after verification
    # collection.drop_search_index("vector_index")
    
    print("✅ Migration complete. Switch app to 'vector_index_v2' and verify before dropping old index.")
```

---

## 10.6 Health Checks

```python
from fastapi import FastAPI, status
from fastapi.responses import JSONResponse

app = FastAPI()

@app.get("/health/vector-search")
async def vector_search_health():
    """Health check for vector search capability."""
    checks = {}
    
    # Check 1: MongoDB connectivity
    try:
        collection.database.command("ping")
        checks["mongodb"] = "ok"
    except Exception as e:
        checks["mongodb"] = f"error: {e}"
    
    # Check 2: Vector index status
    try:
        indexes = list(collection.list_search_indexes(name="vector_index"))
        if indexes and indexes[0]["status"] == "READY":
            checks["vector_index"] = "ok"
        else:
            checks["vector_index"] = f"status: {indexes[0]['status'] if indexes else 'not found'}"
    except Exception as e:
        checks["vector_index"] = f"error: {e}"
    
    # Check 3: Test query latency
    try:
        start = time.perf_counter()
        test_vector = [0.0] * 1536
        list(collection.aggregate([
            {"$vectorSearch": {"index": "vector_index", "path": "embedding", "queryVector": test_vector, "numCandidates": 10, "limit": 1}}
        ]))
        latency = (time.perf_counter() - start) * 1000
        checks["search_latency_ms"] = round(latency, 1)
        checks["search"] = "ok" if latency < 200 else "degraded"
    except Exception as e:
        checks["search"] = f"error: {e}"
    
    # Overall status
    all_ok = all(v == "ok" for k, v in checks.items() if k not in ["search_latency_ms"])
    status_code = status.HTTP_200_OK if all_ok else status.HTTP_503_SERVICE_UNAVAILABLE
    
    return JSONResponse(content=checks, status_code=status_code)
```

---

## 10.7 Observability

### Structured Logging

```python
import structlog

logger = structlog.get_logger()

def search_with_logging(query: str, user_id: str, limit: int = 10):
    log = logger.bind(user_id=user_id, query_length=len(query), limit=limit)
    
    start = time.perf_counter()
    try:
        results = semantic_search(query, limit)
        latency = (time.perf_counter() - start) * 1000
        
        log.info("vector_search_success",
            latency_ms=round(latency, 1),
            num_results=len(results),
            top_score=results[0]["score"] if results else 0
        )
        return results
        
    except Exception as e:
        latency = (time.perf_counter() - start) * 1000
        log.error("vector_search_failed", error=str(e), latency_ms=round(latency, 1))
        raise
```

### Prometheus Metrics

```python
from prometheus_client import Counter, Histogram, Gauge

SEARCH_REQUESTS = Counter('vector_search_requests_total', 'Total search requests', ['status'])
SEARCH_LATENCY = Histogram('vector_search_latency_seconds', 'Search latency', buckets=[.005, .01, .025, .05, .1, .25, .5, 1])
SEARCH_SCORE = Histogram('vector_search_top_score', 'Top result score', buckets=[.1, .2, .3, .4, .5, .6, .7, .8, .9, 1])
INDEX_STATUS = Gauge('vector_index_ready', 'Index readiness', ['index_name'])
```

---

## 10.8 Disaster Recovery

### Backup Strategy

```
MongoDB Atlas provides:
  ✅ Continuous backups (point-in-time recovery)
  ✅ Cloud provider snapshots
  ✅ Cross-region replication

Vector search indexes:
  ⚠️ Indexes are NOT included in backups
  ⚠️ After restore, indexes must be recreated
  → Keep index definitions in version control (deploy_indexes.py)
```

### Multi-Region Deployment

```python
# Primary cluster: us-east-1 (read/write)
# Secondary cluster: eu-west-1 (read-only, disaster recovery)

# Application-level routing
class MultiRegionSearch:
    def __init__(self):
        self.primary = MongoClient(os.environ["MONGODB_PRIMARY_URI"])
        self.secondary = MongoClient(os.environ["MONGODB_SECONDARY_URI"])
    
    def search(self, query_embedding, limit=10):
        try:
            return self._execute_search(self.primary, query_embedding, limit)
        except (ConnectionFailure, ServerSelectionTimeoutError):
            # Failover to secondary
            return self._execute_search(self.secondary, query_embedding, limit)
```

---

## Next: [Module 11 — Real-World Projects →](11_Projects.md)
