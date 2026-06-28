# Module 3: Index Types & Architecture

---

## 3.1 HNSW Deep Dive (MongoDB's Default)

### How HNSW Works Internally

HNSW (Hierarchical Navigable Small World) builds a multi-layer graph:

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 3 (1/8 nodes):   [A]───────────────────[H]           │
│                          │                     │             │
│ Layer 2 (1/4 nodes):   [A]──[C]─────[F]────[H]            │
│                          │    │       │      │             │
│ Layer 1 (1/2 nodes):   [A]─[B]─[C]─[D]─[F]─[G]─[H]      │
│                          │  │  │  │  │  │  │  │           │
│ Layer 0 (all nodes):   [A][B][C][D][E][F][G][H][I][J]     │
└─────────────────────────────────────────────────────────────┘

Search for query Q:
  1. Enter at Layer 3 → greedy walk to nearest (H)
  2. Drop to Layer 2 → greedy walk from H to F
  3. Drop to Layer 1 → greedy walk from F to D
  4. Drop to Layer 0 → local neighborhood search around D → find [D, E, C]
```

### HNSW Parameters in MongoDB

| Parameter | MongoDB Name | Default | Effect |
|-----------|-------------|---------|--------|
| M | (internal) | 16 | Connections per node. ↑ = better recall, ↑ memory |
| efConstruction | (internal) | 200 | Build quality. ↑ = slower build, better graph |
| efSearch | numCandidates | — | Query exploration. ↑ = better recall, ↑ latency |

### Memory Formula

```
Memory per vector ≈ (dimensions × 4 bytes) + (M × 2 × 4 bytes) + overhead

Example (1536-dim, M=16):
  Vector data:     1536 × 4 = 6,144 bytes
  Graph links:     16 × 2 × 4 = 128 bytes
  Overhead:        ~50 bytes
  Total:           ~6,322 bytes per vector
  
  1M vectors ≈ 6.3 GB
  10M vectors ≈ 63 GB
  100M vectors ≈ 630 GB
```

---

## 3.2 Quantization (MongoDB 8.0+)

Quantization compresses vectors to reduce memory while preserving search quality.

### Binary Quantization (BQ)

Converts each float32 dimension to a single bit (1 if ≥ 0, else 0):

```
Original (float32):  [0.12, -0.34, 0.56, -0.78, 0.91, -0.02]
Binary:              [1,     0,     1,     0,     1,     0    ]

Compression: 32x memory reduction
Storage: 1536 dims → 192 bytes (instead of 6,144 bytes)
```

**Trade-offs**:
- ✅ 32x memory reduction
- ✅ Extremely fast (Hamming distance = XOR + popcount)
- ❌ ~5-10% recall loss for high dimensions
- ❌ Works best with ≥768 dimensions

### Scalar Quantization (SQ)

Maps float32 values to int8 (256 levels):

```
Original (float32):  [0.12, -0.34, 0.56, -0.78, 0.91, -0.02]
Quantized (int8):    [83,    42,   148,    14,   231,   126  ]

Compression: 4x memory reduction
Storage: 1536 dims → 1,536 bytes (instead of 6,144 bytes)
```

**Trade-offs**:
- ✅ 4x memory reduction
- ✅ ~1-2% recall loss (much better than BQ)
- ✅ Good balance of compression and accuracy
- ❌ Slower than BQ, faster than full precision

### Quantization Comparison

| Metric | No Quantization | Scalar (int8) | Binary (1-bit) |
|--------|----------------|---------------|-----------------|
| Memory per 1536-dim vector | 6,144 B | 1,536 B | 192 B |
| Memory for 10M vectors | 63 GB | 15.4 GB | 1.9 GB |
| Recall@10 | 100% | ~98-99% | ~90-95% |
| Search Latency | Baseline | ~0.8x | ~0.3x |
| Best for | Small datasets | Production default | Massive scale |

### Index Definition with Quantization

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 1536,
      "similarity": "cosine",
      "quantization": "scalar"
    }
  ]
}
```

### Rescoring (Automatic in MongoDB)

MongoDB uses a two-phase approach with quantized indexes:

```
Phase 1: Fast search with quantized vectors → top K candidates
Phase 2: Re-rank candidates using original float32 vectors → final results

This gives you the speed of quantization with the accuracy of full precision.
```

---

## 3.3 Multiple Vector Indexes

You can create multiple vector indexes on the same collection:

```python
# Index 1: Title embeddings (smaller model, faster)
title_index = SearchIndexModel(
    definition={
        "fields": [{
            "type": "vector",
            "path": "title_embedding",
            "numDimensions": 384,
            "similarity": "cosine"
        }]
    },
    name="title_vector_index",
    type="vectorSearch"
)

# Index 2: Content embeddings (larger model, more accurate)
content_index = SearchIndexModel(
    definition={
        "fields": [{
            "type": "vector",
            "path": "content_embedding",
            "numDimensions": 1536,
            "similarity": "cosine"
        }]
    },
    name="content_vector_index",
    type="vectorSearch"
)

# Index 3: Image embeddings (multimodal)
image_index = SearchIndexModel(
    definition={
        "fields": [{
            "type": "vector",
            "path": "image_embedding",
            "numDimensions": 512,
            "similarity": "cosine"
        }]
    },
    name="image_vector_index",
    type="vectorSearch"
)

collection.create_search_indexes([title_index, content_index, image_index])
```

---

## 3.4 Index Lifecycle & Updates

### How Index Updates Work

```
┌──────────────────────────────────────────────────────────┐
│                  Index Update Flow                         │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  Document Insert/Update ──→ Write to Collection           │
│                              │                            │
│                              ▼                            │
│                         Change Stream detected            │
│                              │                            │
│                              ▼                            │
│                    Background Index Update                 │
│                    (async, near real-time)                 │
│                              │                            │
│                              ▼                            │
│                    Vector searchable (~1-2 sec)            │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

**Key behaviors**:
- New documents become searchable within **1-2 seconds** of insertion
- Index rebuilds (definition changes) happen in background — old index remains queryable
- Status transitions: `READY` → `STALE` (during rebuild) → `READY`

### Monitoring Index Build Progress

```python
import time

while True:
    indexes = list(collection.list_search_indexes(name="vector_index"))
    if indexes:
        status = indexes[0].get("status")
        print(f"Index status: {status}")
        if status == "READY":
            break
    time.sleep(5)
```

---

## 3.5 Filter Index Design

### Pre-filtering Architecture

```
Query: "Find similar documents in category='tech' after 2024-01-01"

Without filter index:
  1. ANN search → top 1000 candidates
  2. Apply filters → maybe only 50 match → poor results

With filter index (pre-filtering):
  1. Narrow to documents matching filters → subset
  2. ANN search within filtered subset → accurate results
```

### Filter Index Best Practices

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    // ✅ DO: Index fields used in vector search filters
    { "type": "filter", "path": "tenantId" },
    { "type": "filter", "path": "category" },
    { "type": "filter", "path": "status" },
    { "type": "filter", "path": "createdAt" },
    
    // ❌ DON'T: Index fields never used in vector search filters
    // { "type": "filter", "path": "title" }      // searched via text, not filtered
    // { "type": "filter", "path": "embedding" }   // never filter on the vector itself
  ]
}
```

### Supported Filter Operations

```javascript
// Equality
{ "filter": { "category": { "$eq": "technology" } } }

// In array
{ "filter": { "category": { "$in": ["tech", "science"] } } }

// Not equal
{ "filter": { "status": { "$ne": "archived" } } }

// Range (numeric/date)
{ "filter": { "createdAt": { "$gte": ISODate("2024-01-01"), "$lt": ISODate("2025-01-01") } } }

// Compound
{ "filter": { "$and": [
    { "tenantId": { "$eq": "tenant-1" } },
    { "status": { "$in": ["active", "pending"] } },
    { "score": { "$gte": 0.5 } }
] } }
```

---

## 3.6 Index Sizing & Capacity Planning

### Memory Requirements Calculator

```python
def calculate_index_memory(
    num_vectors: int,
    dimensions: int,
    quantization: str = "none",  # "none", "scalar", "binary"
    m: int = 16,  # HNSW connections
    num_filter_fields: int = 0,
    avg_filter_field_size: int = 50  # bytes
) -> dict:
    
    # Vector storage
    if quantization == "binary":
        vector_bytes = dimensions / 8
    elif quantization == "scalar":
        vector_bytes = dimensions
    else:
        vector_bytes = dimensions * 4
    
    # HNSW graph overhead
    graph_bytes = m * 2 * 8  # bidirectional links, 8 bytes each
    
    # Filter index overhead
    filter_bytes = num_filter_fields * avg_filter_field_size
    
    # Per-vector total
    per_vector = vector_bytes + graph_bytes + filter_bytes + 64  # 64 bytes overhead
    
    # Total
    total_bytes = num_vectors * per_vector
    
    return {
        "per_vector_bytes": per_vector,
        "total_gb": total_bytes / (1024**3),
        "recommended_ram_gb": total_bytes * 1.3 / (1024**3)  # 30% headroom
    }

# Example
result = calculate_index_memory(
    num_vectors=10_000_000,
    dimensions=1536,
    quantization="scalar",
    num_filter_fields=3
)
print(f"Total index size: {result['total_gb']:.1f} GB")
print(f"Recommended RAM: {result['recommended_ram_gb']:.1f} GB")
```

### Atlas Tier Recommendations

| Vectors | Dimensions | Quantization | Recommended Tier |
|---------|-----------|--------------|-----------------|
| < 100K | 1536 | None | M10 |
| 100K - 1M | 1536 | None | M30 |
| 1M - 5M | 1536 | Scalar | M40 |
| 5M - 20M | 1536 | Scalar | M50 |
| 20M - 100M | 1536 | Binary | M60+ |
| > 100M | 1536 | Binary | M80+ (sharded) |

---

## Next: [Module 4 — Basic Vector Search →](04_Basic_Vector_Search.md)
