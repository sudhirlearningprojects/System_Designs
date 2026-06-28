# Module 1: Foundations of Vector Search

---

## 1.1 What Are Vector Embeddings?

Vector embeddings are dense numerical representations of data (text, images, audio) in a high-dimensional space where **semantic similarity** corresponds to **geometric proximity**.

```
"MongoDB is a database"    → [0.12, -0.34, 0.56, ..., 0.78]  (1536 dims)
"Mongo stores documents"   → [0.11, -0.33, 0.55, ..., 0.77]  (close!)
"The weather is sunny"     → [0.89, 0.12, -0.67, ..., 0.03]  (far away)
```

### Why Vectors Matter

| Traditional Search | Vector Search |
|---|---|
| Exact keyword match | Semantic meaning |
| "car" ≠ "automobile" | "car" ≈ "automobile" |
| Boolean relevance | Continuous similarity score |
| No understanding of context | Context-aware retrieval |

---

## 1.2 Similarity Metrics

MongoDB Atlas Vector Search supports three distance metrics:

### Cosine Similarity (default, recommended for text)

Measures the angle between two vectors. Normalized, so vector magnitude doesn't matter.

```
cosine_similarity(A, B) = (A · B) / (||A|| × ||B||)

Range: [-1, 1] → normalized to [0, 1] in MongoDB
  1.0 = identical direction
  0.5 = orthogonal
  0.0 = opposite direction
```

**Best for**: Text embeddings (OpenAI, Cohere) where vectors are already normalized.

### Euclidean Distance (L2)

Measures the straight-line distance between two points in vector space.

```
euclidean(A, B) = √(Σ(ai - bi)²)

Range: [0, ∞) → MongoDB normalizes to score = 1 / (1 + distance)
  Score 1.0 = identical
  Score → 0  = very different
```

**Best for**: Image embeddings, when magnitude matters.

### Dot Product

Raw dot product of two vectors. Fastest computation.

```
dot_product(A, B) = Σ(ai × bi)

Range: (-∞, ∞) → normalized in MongoDB
```

**Best for**: Pre-normalized vectors, maximum performance scenarios.

### Choosing a Metric

```
┌─────────────────────────────────────────────────────┐
│ Are your vectors normalized (unit length)?           │
│   ├─ YES → Dot Product (fastest) or Cosine          │
│   └─ NO                                             │
│       ├─ Does magnitude matter?                     │
│       │   ├─ YES → Euclidean                        │
│       │   └─ NO  → Cosine (handles normalization)   │
└─────────────────────────────────────────────────────┘
```

---

## 1.3 Approximate Nearest Neighbor (ANN) Algorithms

Exact kNN is O(n×d) — too slow for millions of vectors. ANN trades perfect accuracy for massive speed gains.

### HNSW (Hierarchical Navigable Small World) — MongoDB's Default

A graph-based algorithm with multiple layers forming a "highway system":

```
Layer 3: [A] ←────────────────→ [D]          (express highway)
           |                       |
Layer 2: [A] ← [B] ──→ [C] ──→ [D]          (highway)
           |     |       |       |
Layer 1: [A]-[B]-[C]-[D]-[E]-[F]-[G]-[H]    (local roads)
           |  |  |  |  |  |  |  |
Layer 0: [all nodes connected densely]        (every street)
```

**How it works**:
1. Start at the top layer (fewest nodes)
2. Greedily navigate to the closest node
3. Drop to the next layer and repeat
4. At layer 0, explore local neighborhood

**Parameters**:
- `m` (default 16): Max connections per node. Higher = better recall, more memory
- `efConstruction` (default 200): Build-time exploration factor. Higher = better index quality
- `efSearch` (numCandidates): Query-time exploration. Higher = better recall, slower

**Complexity**:
- Build: O(N × log(N))
- Search: O(log(N))
- Memory: O(N × M × layers)

### IVF (Inverted File Index) — For Very Large Datasets

Partitions vectors into clusters (Voronoi cells), then searches only relevant clusters:

```
┌────────────┬────────────┬────────────┐
│ Cluster 1  │ Cluster 2  │ Cluster 3  │
│  ● ● ●    │  ▲ ▲ ▲    │  ■ ■ ■    │
│   ● ●     │   ▲ ▲     │   ■ ■     │
│    ●       │    ▲ ▲    │    ■      │
└────────────┴────────────┴────────────┘
     Query ★ is closest to Cluster 2
     → Only search vectors in Cluster 2 (and maybe 1)
```

**Parameters**:
- `nlist`: Number of clusters (√N is a good starting point)
- `nprobe`: Number of clusters to search (trade-off: recall vs speed)

---

## 1.4 Recall vs Latency Trade-off

```
Recall (%)
100│          ●────────── Exact kNN (impractical at scale)
   │       ●
 95│     ●              HNSW (efSearch=200)
   │   ●
 90│  ●                 HNSW (efSearch=100)
   │ ●
 80│●                   IVF (nprobe=1)
   └──────────────────────── Latency (ms)
   0   1   5   10   50  100
```

**Recall** = % of true nearest neighbors found by ANN
- 95-99% recall is typical for production HNSW
- Controlled by `numCandidates` in MongoDB (maps to efSearch)
- Rule of thumb: `numCandidates` = 10-20× `limit`

---

## 1.5 Vector Dimensions and Model Selection

| Model | Provider | Dimensions | Best For |
|-------|----------|------------|----------|
| text-embedding-3-small | OpenAI | 1536 | General text (cost-effective) |
| text-embedding-3-large | OpenAI | 3072 | High-accuracy text |
| embed-english-v3.0 | Cohere | 1024 | English text |
| embed-multilingual-v3.0 | Cohere | 1024 | Multi-language |
| voyage-3 | Voyage AI | 1024 | Code + text |
| all-MiniLM-L6-v2 | Sentence Transformers | 384 | Fast, local, free |
| nomic-embed-text-v1.5 | Nomic | 768 | Open-source, good quality |
| mxbai-embed-large-v1 | Mixedbread | 1024 | Open-source, top quality |

### Dimension Reduction (MongoDB 8.0+)

OpenAI's `text-embedding-3-*` models support native dimension reduction:

```python
# Full dimensions (1536)
embedding = openai.embeddings.create(input="hello", model="text-embedding-3-small").data[0].embedding

# Reduced to 512 dimensions (saves 66% storage, ~2% recall loss)
embedding = openai.embeddings.create(
    input="hello", model="text-embedding-3-small", dimensions=512
).data[0].embedding
```

---

## 1.6 MongoDB Vector Search vs Competitors

| Feature | MongoDB Atlas | Pinecone | Weaviate | Qdrant | pgvector |
|---------|--------------|----------|----------|--------|----------|
| Max Dimensions | 4096 | 20000 | 65535 | 65535 | 2000 |
| Algorithms | HNSW | Proprietary | HNSW | HNSW | HNSW/IVF |
| Quantization | Binary, Scalar | Yes | PQ, BQ | Scalar, PQ | No |
| Hybrid Search | Native (RRF) | Yes | BM25+Vector | Sparse+Dense | Manual |
| Metadata Filtering | Pre-filter | Post-filter | Pre-filter | Pre-filter | WHERE |
| Transactions | Full ACID | No | No | No | Full ACID |
| Operational DB | ✅ Yes | ❌ No | ❌ No | ❌ No | ✅ Yes |
| Managed Service | Atlas | Serverless | Cloud | Cloud | Various |

**Key MongoDB Advantage**: Your vectors live alongside your operational data — no synchronization between separate vector DB and application DB.

---

## 1.7 When to Use Vector Search

✅ **Use Vector Search when**:
- Semantic/meaning-based search (not just keywords)
- Recommendation systems (similar items/users)
- RAG (Retrieval-Augmented Generation) for LLMs
- Image/audio similarity
- Anomaly detection
- Deduplication

❌ **Don't use Vector Search when**:
- Exact keyword matching is sufficient
- Structured queries (price > 100, date = today)
- Sorting by a specific field
- Full-text search without semantic understanding

---

## 1.8 Key Terminology

| Term | Definition |
|------|-----------|
| **Embedding** | Dense vector representation of data |
| **Dimension** | Number of elements in a vector (e.g., 1536) |
| **ANN** | Approximate Nearest Neighbor — fast similarity search |
| **HNSW** | Graph-based ANN algorithm used by MongoDB |
| **Recall** | Fraction of true nearest neighbors returned |
| **numCandidates** | How many candidates HNSW explores (affects recall) |
| **Pre-filtering** | Apply metadata filters before ANN search |
| **Post-filtering** | Apply metadata filters after ANN search |
| **RRF** | Reciprocal Rank Fusion — combines multiple rankings |
| **Quantization** | Compress vectors to use less memory |

---

## Next: [Module 2 — Setup & Configuration →](02_Setup_Configuration.md)
