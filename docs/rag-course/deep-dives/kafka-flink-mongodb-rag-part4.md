# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 4: MongoDB Atlas Vector Search

---

## Why MongoDB for RAG?

MongoDB Atlas serves as a **unified storage layer** — storing both source documents and their vector embeddings in a single collection. This eliminates the dual-write problem and simplifies the architecture.

| Advantage | Description |
|-----------|-------------|
| **Unified storage** | Vectors + metadata + full text in one document |
| **Atlas Vector Search** | Native HNSW-based vector search (GA since 2024) |
| **Full-text search** | Atlas Search for hybrid retrieval |
| **Rich filtering** | Complex queries on metadata alongside vector search |
| **Change streams** | Real-time notifications on data changes |
| **Horizontal scaling** | Sharding for billion-scale collections |

---

## Collection Schema Design

```python
from pymongo import MongoClient
from datetime import datetime

client = MongoClient("mongodb+srv://user:<password>@cluster.mongodb.net/")
db = client["rag_db"]

# Document chunks collection
# Each document in this collection represents one chunk with its embedding
SAMPLE_DOCUMENT = {
    "_id": "doc123_chunk_0",              # chunk_id
    "document_id": "doc123",               # Parent document reference
    "chunk_index": 0,
    "total_chunks": 5,
    "content": "Retrieval Augmented Generation combines...",
    "content_hash": "sha256:abc123...",
    "embedding": [0.023, -0.012, ...],     # 1024-dim vector
    "embedding_model": "text-embedding-3-small",
    "metadata": {
        "source": "confluence",
        "source_url": "https://wiki.company.com/page/123",
        "title": "RAG Architecture Guide",
        "author": "engineering",
        "department": "platform",
        "last_modified": "2024-11-15T10:30:00Z",
        "content_type": "text/html",
        "language": "en",
    },
    "tenant_id": "tenant_acme",
    "access_control": ["role:engineer", "team:platform"],
    "created_at": datetime(2024, 11, 15, 10, 30),
    "updated_at": datetime(2024, 11, 15, 10, 30),
    "ttl_expires_at": None,  # Optional TTL for auto-deletion
}
```

---

## Atlas Vector Search Index

Create via MongoDB Atlas UI or programmatically:

```python
# Create Vector Search Index via Atlas Admin API
import requests

def create_vector_search_index(cluster_name: str, db_name: str, collection_name: str):
    """Create Atlas Vector Search index."""
    
    index_definition = {
        "name": "vector_index",
        "type": "vectorSearch",
        "definition": {
            "fields": [
                {
                    "type": "vector",
                    "path": "embedding",
                    "numDimensions": 1024,
                    "similarity": "cosine",  # cosine | euclidean | dotProduct
                },
                {
                    "type": "filter",
                    "path": "tenant_id",
                },
                {
                    "type": "filter",
                    "path": "access_control",
                },
                {
                    "type": "filter",
                    "path": "metadata.department",
                },
                {
                    "type": "filter",
                    "path": "metadata.source",
                },
            ]
        }
    }
    
    # Via pymongo (Atlas 7.0+)
    db.command({
        "createSearchIndexes": collection_name,
        "indexes": [index_definition]
    })
```

### Full-Text Search Index (for Hybrid Search)
```python
full_text_index = {
    "name": "text_search_index",
    "type": "search",
    "definition": {
        "mappings": {
            "dynamic": False,
            "fields": {
                "content": {
                    "type": "string",
                    "analyzer": "lucene.standard",
                },
                "metadata.title": {
                    "type": "string",
                    "analyzer": "lucene.standard",
                },
                "tenant_id": {
                    "type": "token",  # Exact match only
                },
            }
        }
    }
}

db.command({
    "createSearchIndexes": "document_chunks",
    "indexes": [full_text_index]
})
```

---

## MongoDB Indexes (Standard)

```python
from pymongo import IndexModel, ASCENDING, DESCENDING

collection = db["document_chunks"]

# Standard indexes for operational queries
collection.create_indexes([
    IndexModel([("document_id", ASCENDING)]),
    IndexModel([("tenant_id", ASCENDING), ("document_id", ASCENDING)]),
    IndexModel([("content_hash", ASCENDING)], unique=True),
    IndexModel([("created_at", DESCENDING)]),
    IndexModel([("ttl_expires_at", ASCENDING)], expireAfterSeconds=0),  # TTL index
])
```

---

## Vector Search Queries

### Basic Vector Search
```python
from pymongo import MongoClient

def vector_search(query_embedding: list[float], tenant_id: str, k: int = 5) -> list:
    """Perform vector search with tenant isolation."""
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": k * 10,  # HNSW candidates (higher = better recall)
                "limit": k,
                "filter": {
                    "tenant_id": {"$eq": tenant_id}
                }
            }
        },
        {
            "$project": {
                "_id": 1,
                "content": 1,
                "metadata": 1,
                "document_id": 1,
                "chunk_index": 1,
                "score": {"$meta": "vectorSearchScore"},
            }
        }
    ]
    
    results = list(collection.aggregate(pipeline))
    return results
```

### Vector Search with Access Control
```python
def secure_vector_search(
    query_embedding: list[float],
    tenant_id: str,
    user_roles: list[str],
    k: int = 5,
    filters: dict = None,
) -> list:
    """Vector search with RBAC filtering."""
    
    # Build filter combining tenant isolation + access control
    search_filter = {
        "$and": [
            {"tenant_id": {"$eq": tenant_id}},
            {"access_control": {"$in": user_roles}},  # User must have at least one role
        ]
    }
    
    # Add optional metadata filters
    if filters:
        if "source" in filters:
            search_filter["$and"].append({"metadata.source": {"$eq": filters["source"]}})
        if "department" in filters:
            search_filter["$and"].append({"metadata.department": {"$eq": filters["department"]}})
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": k * 20,
                "limit": k,
                "filter": search_filter,
            }
        },
        {
            "$project": {
                "content": 1,
                "metadata": 1,
                "document_id": 1,
                "score": {"$meta": "vectorSearchScore"},
            }
        }
    ]
    
    return list(collection.aggregate(pipeline))
```

### Hybrid Search (Vector + Full-Text)
```python
def hybrid_search(
    query_text: str,
    query_embedding: list[float],
    tenant_id: str,
    k: int = 5,
    vector_weight: float = 0.7,
) -> list:
    """Combine vector search with full-text search using RRF."""
    
    # Vector search results
    vector_pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": k * 10,
                "limit": k * 2,
                "filter": {"tenant_id": {"$eq": tenant_id}},
            }
        },
        {"$addFields": {"vs_score": {"$meta": "vectorSearchScore"}}},
        {"$project": {"content": 1, "metadata": 1, "document_id": 1, "vs_score": 1}},
    ]
    
    # Full-text search results
    text_pipeline = [
        {
            "$search": {
                "index": "text_search_index",
                "compound": {
                    "must": [{"text": {"query": query_text, "path": "content"}}],
                    "filter": [{"equals": {"path": "tenant_id", "value": tenant_id}}],
                }
            }
        },
        {"$addFields": {"text_score": {"$meta": "searchScore"}}},
        {"$limit": k * 2},
        {"$project": {"content": 1, "metadata": 1, "document_id": 1, "text_score": 1}},
    ]
    
    vector_results = list(collection.aggregate(vector_pipeline))
    text_results = list(collection.aggregate(text_pipeline))
    
    # Reciprocal Rank Fusion
    return reciprocal_rank_fusion(vector_results, text_results, k, vector_weight)


def reciprocal_rank_fusion(
    vector_results: list, text_results: list, k: int, vector_weight: float, rrf_k: int = 60
) -> list:
    """Merge results using RRF scoring."""
    scores = {}
    
    for rank, doc in enumerate(vector_results):
        doc_id = str(doc["_id"])
        scores[doc_id] = scores.get(doc_id, {"doc": doc, "score": 0})
        scores[doc_id]["score"] += vector_weight * (1 / (rank + rrf_k))
    
    text_weight = 1 - vector_weight
    for rank, doc in enumerate(text_results):
        doc_id = str(doc["_id"])
        scores[doc_id] = scores.get(doc_id, {"doc": doc, "score": 0})
        scores[doc_id]["score"] += text_weight * (1 / (rank + rrf_k))
    
    sorted_results = sorted(scores.values(), key=lambda x: x["score"], reverse=True)
    return [item["doc"] for item in sorted_results[:k]]
```

---

## Document CRUD Operations (Sink from Kafka)

```python
from pymongo import UpdateOne, DeleteMany
from pymongo.operations import SearchIndexModel

class MongoDBSinkHandler:
    """Handle upserts and deletions from Kafka embeddings topic."""
    
    def __init__(self, collection):
        self.collection = collection
    
    def upsert_chunk(self, chunk_data: dict):
        """Upsert a single chunk with its embedding."""
        self.collection.update_one(
            {"_id": chunk_data["chunk_id"]},
            {
                "$set": {
                    "document_id": chunk_data["document_id"],
                    "chunk_index": chunk_data.get("chunk_index", 0),
                    "content": chunk_data["content"],
                    "content_hash": chunk_data.get("content_hash"),
                    "embedding": chunk_data["embedding"],
                    "embedding_model": chunk_data["embedding_model"],
                    "metadata": chunk_data["metadata"],
                    "tenant_id": chunk_data["tenant_id"],
                    "access_control": chunk_data.get("access_control", []),
                    "updated_at": datetime.utcnow(),
                },
                "$setOnInsert": {
                    "created_at": datetime.utcnow(),
                }
            },
            upsert=True,
        )
    
    def bulk_upsert(self, chunks: list[dict]):
        """Batch upsert for throughput."""
        operations = [
            UpdateOne(
                {"_id": chunk["chunk_id"]},
                {"$set": {
                    "document_id": chunk["document_id"],
                    "content": chunk["content"],
                    "embedding": chunk["embedding"],
                    "embedding_model": chunk["embedding_model"],
                    "metadata": chunk["metadata"],
                    "tenant_id": chunk["tenant_id"],
                    "access_control": chunk.get("access_control", []),
                    "updated_at": datetime.utcnow(),
                }},
                upsert=True,
            )
            for chunk in chunks
        ]
        
        result = self.collection.bulk_write(operations, ordered=False)
        return result.upserted_count + result.modified_count
    
    def delete_document_chunks(self, document_id: str, tenant_id: str):
        """Delete all chunks for a document."""
        result = self.collection.delete_many({
            "document_id": document_id,
            "tenant_id": tenant_id,
        })
        return result.deleted_count
```

---

## Change Streams (Real-time Notifications)

Use MongoDB change streams to trigger downstream actions:

```python
def watch_for_changes(collection):
    """Watch for new embeddings and trigger cache invalidation."""
    
    pipeline = [
        {"$match": {"operationType": {"$in": ["insert", "update", "delete"]}}},
    ]
    
    with collection.watch(pipeline, full_document="updateLookup") as stream:
        for change in stream:
            op = change["operationType"]
            doc_id = change["documentKey"]["_id"]
            
            if op in ["insert", "update"]:
                # Invalidate cache for this document
                cache.invalidate(change["fullDocument"]["document_id"])
                
                # Update search analytics
                metrics.increment("documents_indexed")
                
            elif op == "delete":
                cache.invalidate(doc_id)
                metrics.increment("documents_deleted")
```

---

## MongoDB Performance Tuning

### Connection Pooling
```python
client = MongoClient(
    "mongodb+srv://...",
    maxPoolSize=100,
    minPoolSize=10,
    maxIdleTimeMS=30000,
    connectTimeoutMS=5000,
    serverSelectionTimeoutMS=5000,
    retryWrites=True,
    retryReads=True,
    w="majority",
    readPreference="secondaryPreferred",  # Read from replicas for vector search
)
```

### Vector Search Performance Tips

| Setting | Recommendation | Impact |
|---------|---------------|--------|
| numCandidates | 10x-20x of limit | Higher recall, slower query |
| Dimensions | Use Matryoshka (1024 instead of 3072) | 3x faster search |
| Filter selectivity | Pre-filter on indexed fields | Reduces search space |
| Sharding | Shard by tenant_id | Isolate workloads |
| Read preference | secondaryPreferred | Offload reads from primary |

---

## Next: [Part 5 - RAG Application & Query Layer](./kafka-flink-mongodb-rag-part5.md)
