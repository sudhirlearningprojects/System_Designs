# Module 4: Vector Databases

## Overview

Vector databases store embeddings and enable fast similarity search at scale. Choosing the right one depends on scale, cost, and feature requirements.

## Vector Database Comparison (2024-2025)

| Database | Type | Max Vectors | Filtering | Hybrid Search | Pricing Model |
|----------|------|-------------|-----------|---------------|---------------|
| Pinecone Serverless | Managed | Billions | ✅ Advanced | ✅ | Pay-per-use |
| Weaviate | Self-hosted/Cloud | Billions | ✅ GraphQL | ✅ BM25+Vector | Open source + Cloud |
| Qdrant | Self-hosted/Cloud | Billions | ✅ Rich | ✅ Sparse+Dense | Open source + Cloud |
| pgvector | Extension | Millions | ✅ SQL | ✅ via pg_search | Free (PostgreSQL) |
| Milvus/Zilliz | Self-hosted/Cloud | Trillions | ✅ | ✅ | Open source + Cloud |
| ChromaDB | Embedded/Server | Millions | ✅ Basic | ❌ | Open source |
| Redis Vector | Extension | Millions | ✅ | ✅ | Redis license |

### Selection Guide
- **Startup/Prototype**: ChromaDB (zero config) or pgvector (if already using Postgres)
- **Production SaaS**: Pinecone Serverless (fully managed, zero ops)
- **Self-hosted scale**: Qdrant or Milvus
- **Existing Postgres**: pgvector with HNSW indexes
- **Hybrid search critical**: Weaviate or Qdrant
- **Cost-sensitive**: pgvector or self-hosted Qdrant

---

## Pinecone Serverless

```python
from pinecone import Pinecone, ServerlessSpec

pc = Pinecone(api_key="<your-key>")

# Create index
pc.create_index(
    name="rag-index",
    dimension=1024,
    metric="cosine",
    spec=ServerlessSpec(cloud="aws", region="us-east-1"),
)

index = pc.Index("rag-index")

# Upsert vectors with metadata
index.upsert(
    vectors=[
        {"id": "doc1", "values": [0.1, 0.2, ...], "metadata": {"source": "guide.pdf", "page": 1}},
        {"id": "doc2", "values": [0.3, 0.4, ...], "metadata": {"source": "guide.pdf", "page": 2}},
    ],
    namespace="documentation",
)

# Query with metadata filtering
results = index.query(
    vector=[0.15, 0.25, ...],
    top_k=5,
    filter={"source": {"$eq": "guide.pdf"}},
    include_metadata=True,
    namespace="documentation",
)
```

### With LangChain
```python
from langchain_pinecone import PineconeVectorStore
from langchain_openai import OpenAIEmbeddings

vectorstore = PineconeVectorStore(
    index_name="rag-index",
    embedding=OpenAIEmbeddings(model="text-embedding-3-small"),
    namespace="documentation",
)

# Add documents
vectorstore.add_documents(chunks)

# Search
results = vectorstore.similarity_search("How to deploy?", k=5, filter={"source": "guide.pdf"})
```

---

## Qdrant

```python
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct, Filter, FieldCondition, MatchValue

client = QdrantClient(url="http://localhost:6333")  # or cloud URL

# Create collection
client.create_collection(
    collection_name="documents",
    vectors_config=VectorParams(size=1024, distance=Distance.COSINE),
)

# Upsert with rich payload
client.upsert(
    collection_name="documents",
    points=[
        PointStruct(
            id=1,
            vector=[0.1, 0.2, ...],
            payload={"text": "RAG combines...", "source": "guide.pdf", "page": 1}
        ),
    ],
)

# Search with filtering
results = client.query_points(
    collection_name="documents",
    query=[0.15, 0.25, ...],
    limit=5,
    query_filter=Filter(
        must=[FieldCondition(key="source", match=MatchValue(value="guide.pdf"))]
    ),
)
```

### Qdrant Hybrid Search (Dense + Sparse)
```python
from qdrant_client.models import SparseVector, NamedSparseVector

# Create collection with both dense and sparse vectors
client.create_collection(
    collection_name="hybrid_docs",
    vectors_config=VectorParams(size=1024, distance=Distance.COSINE),
    sparse_vectors_config={"text-sparse": {}},
)

# Hybrid query (combines BM25-like sparse with dense semantic)
results = client.query_points(
    collection_name="hybrid_docs",
    query=[0.15, 0.25, ...],  # dense vector
    using="default",
    limit=10,
)
```

---

## pgvector (PostgreSQL)

Best choice if you already use PostgreSQL — no separate infrastructure needed.

```sql
-- Enable extension
CREATE EXTENSION vector;

-- Create table with vector column
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1024)  -- dimension must match your model
);

-- Create HNSW index (fast approximate search)
CREATE INDEX ON documents USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Insert
INSERT INTO documents (content, metadata, embedding)
VALUES ('RAG combines retrieval...', '{"source": "guide"}', '[0.1, 0.2, ...]');

-- Query (cosine similarity)
SELECT content, metadata, 1 - (embedding <=> $1) AS similarity
FROM documents
WHERE metadata->>'source' = 'guide'
ORDER BY embedding <=> $1
LIMIT 5;
```

### With Python + LangChain
```python
from langchain_postgres import PGVector
from langchain_openai import OpenAIEmbeddings

CONNECTION_STRING = "postgresql+psycopg://user:pass@localhost:5432/ragdb"

vectorstore = PGVector(
    embeddings=OpenAIEmbeddings(model="text-embedding-3-small"),
    collection_name="documents",
    connection=CONNECTION_STRING,
    use_jsonb=True,
)

vectorstore.add_documents(chunks)
results = vectorstore.similarity_search_with_score("deployment guide", k=5)
```

---

## Weaviate

```python
import weaviate
from weaviate.classes.config import Configure, Property, DataType

client = weaviate.connect_to_local()  # or connect_to_weaviate_cloud()

# Create collection with vectorizer
collection = client.collections.create(
    name="Document",
    vectorizer_config=Configure.Vectorizer.text2vec_openai(model="text-embedding-3-small"),
    properties=[
        Property(name="content", data_type=DataType.TEXT),
        Property(name="source", data_type=DataType.TEXT),
    ],
)

# Add objects (auto-vectorized)
collection.data.insert({"content": "RAG combines...", "source": "guide.pdf"})

# Hybrid search (combines BM25 + vector)
results = collection.query.hybrid(
    query="What is RAG?",
    alpha=0.7,  # 0=pure BM25, 1=pure vector
    limit=5,
    filters=weaviate.classes.query.Filter.by_property("source").equal("guide.pdf"),
)
```

---

## ChromaDB (Development & Small Scale)

```python
import chromadb
from chromadb.utils.embedding_functions import OpenAIEmbeddingFunction

client = chromadb.PersistentClient(path="./chroma_db")

embedding_fn = OpenAIEmbeddingFunction(
    api_key="<your-key>",
    model_name="text-embedding-3-small",
)

collection = client.get_or_create_collection(
    name="documents",
    embedding_function=embedding_fn,
    metadata={"hnsw:space": "cosine"},
)

# Add documents (auto-embeds)
collection.add(
    documents=["RAG combines retrieval...", "Vector databases store..."],
    metadatas=[{"source": "guide"}, {"source": "tutorial"}],
    ids=["doc1", "doc2"],
)

# Query
results = collection.query(
    query_texts=["What is RAG?"],
    n_results=5,
    where={"source": "guide"},
)
```

---

## Index Types & Performance

| Index Type | Build Time | Query Time | Recall | Memory |
|-----------|-----------|-----------|--------|--------|
| Flat (brute-force) | O(1) | O(n) | 100% | Low |
| IVF | Medium | O(√n) | 95-99% | Low |
| HNSW | Slow | O(log n) | 97-99.9% | High |
| PQ (Product Quantization) | Medium | O(n/compression) | 90-95% | Very Low |
| HNSW + PQ | Slow | O(log n) | 95-98% | Medium |

**Recommendation**: Use HNSW for most production workloads. Use IVF+PQ for billion-scale vectors with memory constraints.

### HNSW Tuning Parameters
```python
# m: Number of connections per node (higher = better recall, more memory)
# ef_construction: Build-time search width (higher = better index, slower build)
# ef_search: Query-time search width (higher = better recall, slower query)

# Typical production values:
# m=16, ef_construction=200, ef_search=100 → 98%+ recall @ <10ms
```

---

## Multi-Tenancy Patterns

### 1. Namespace Isolation (Pinecone)
```python
# Each tenant gets a namespace
index.upsert(vectors=[...], namespace=f"tenant_{tenant_id}")
index.query(vector=[...], namespace=f"tenant_{tenant_id}")
```

### 2. Metadata Filtering
```python
# Filter by tenant_id in metadata
results = vectorstore.similarity_search(
    "query",
    filter={"tenant_id": tenant_id},
    k=5,
)
```

### 3. Collection per Tenant (Qdrant/Weaviate)
```python
# Create separate collection per tenant
client.create_collection(f"tenant_{tenant_id}", vectors_config=...)
```

**Recommendation**: Use metadata filtering for <1000 tenants, namespace/collection isolation for larger deployments or strict data isolation requirements.

---

## Cost Optimization

| Strategy | Savings | Trade-off |
|----------|---------|-----------|
| Reduce dimensions (Matryoshka) | 50-90% storage | Slight recall drop |
| Binary quantization | 90%+ storage | 5-10% recall drop |
| Scalar quantization | 75% storage | 2-5% recall drop |
| Tiered storage (hot/cold) | 60-80% | Higher cold query latency |
| Batch upserts | 30-50% API cost | Slight ingestion delay |

```python
# Qdrant: Enable scalar quantization
client.create_collection(
    collection_name="documents",
    vectors_config=VectorParams(size=1024, distance=Distance.COSINE),
    quantization_config=models.ScalarQuantization(
        scalar=models.ScalarQuantizationConfig(type=models.ScalarType.INT8, quantile=0.99)
    ),
)
```

---

## Exercises

1. Set up pgvector locally and benchmark query performance with HNSW vs IVFFlat indexes
2. Compare retrieval quality between ChromaDB (development) and Pinecone (production)
3. Implement multi-tenancy with metadata filtering and verify data isolation
4. Benchmark query latency at different scales (10K, 100K, 1M vectors)
5. Implement hybrid search (BM25 + vector) using Weaviate or Qdrant
