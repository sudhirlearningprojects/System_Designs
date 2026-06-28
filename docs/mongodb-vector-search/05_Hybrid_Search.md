# Module 5: Hybrid Search

---

## 5.1 Why Hybrid Search?

Vector search alone has limitations:

| Scenario | Vector Search | Full-Text Search | Hybrid |
|----------|:---:|:---:|:---:|
| "MongoDB ACID transactions" | Good semantic match | Exact keyword match | ✅ Best |
| "error code E11000" | Poor (semantic miss) | ✅ Exact match | ✅ Best |
| "best NoSQL database for analytics" | ✅ Great semantic | Partial keyword | ✅ Best |
| Typo: "mongdb vector serch" | ✅ Tolerant | Poor | ✅ Best |

**Hybrid Search** = Vector Search + Full-Text Search, combined using **Reciprocal Rank Fusion (RRF)**.

---

## 5.2 Reciprocal Rank Fusion (RRF)

RRF combines rankings from multiple search methods without needing normalized scores:

```
RRF_score(doc) = Σ  1 / (k + rank_i(doc))
                 i

Where:
  k = 60 (constant, smoothing factor)
  rank_i = rank of document in search method i (1-based)
```

### Example

```
Vector Search Results:        Full-Text Results:
  Rank 1: Doc_A (score 0.95)    Rank 1: Doc_C
  Rank 2: Doc_B (score 0.88)    Rank 2: Doc_A
  Rank 3: Doc_C (score 0.82)    Rank 3: Doc_D
  Rank 4: Doc_D (score 0.75)    Rank 4: Doc_B

RRF Scores (k=60):
  Doc_A: 1/(60+1) + 1/(60+2) = 0.01639 + 0.01613 = 0.03252 ← Winner!
  Doc_B: 1/(60+2) + 1/(60+4) = 0.01613 + 0.01563 = 0.03176
  Doc_C: 1/(60+3) + 1/(60+1) = 0.01587 + 0.01639 = 0.03226
  Doc_D: 1/(60+4) + 1/(60+3) = 0.01563 + 0.01587 = 0.03150

Final Ranking: Doc_A > Doc_C > Doc_B > Doc_D
```

---

## 5.3 Implementing Hybrid Search in MongoDB

### Step 1: Create Both Indexes

```python
from pymongo.operations import SearchIndexModel

# Vector Search Index
vector_index = SearchIndexModel(
    definition={
        "fields": [
            {"type": "vector", "path": "embedding", "numDimensions": 1536, "similarity": "cosine"},
            {"type": "filter", "path": "category"}
        ]
    },
    name="vector_index",
    type="vectorSearch"
)

# Full-Text Search Index (Atlas Search)
text_index = SearchIndexModel(
    definition={
        "mappings": {
            "dynamic": False,
            "fields": {
                "title": {"type": "string", "analyzer": "lucene.standard"},
                "content": {"type": "string", "analyzer": "lucene.standard"},
                "category": {"type": "stringFacet"}
            }
        }
    },
    name="text_index",
    type="search"
)

collection.create_search_indexes([vector_index, text_index])
```

### Step 2: Hybrid Search with `$unionWith` + RRF

```python
def hybrid_search(query: str, limit: int = 10, vector_weight: float = 0.7):
    query_embedding = get_embedding(query)
    
    pipeline = [
        # Vector Search
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 150,
                "limit": 50
            }
        },
        {"$addFields": {"vs_score": {"$meta": "vectorSearchScore"}}},
        {"$project": {"title": 1, "content": 1, "vs_score": 1}},
        
        # Combine with full-text search via $unionWith
        {
            "$unionWith": {
                "coll": "documents",
                "pipeline": [
                    {
                        "$search": {
                            "index": "text_index",
                            "text": {
                                "query": query,
                                "path": ["title", "content"]
                            }
                        }
                    },
                    {"$limit": 50},
                    {"$addFields": {"fts_score": {"$meta": "searchScore"}}},
                    {"$project": {"title": 1, "content": 1, "fts_score": 1}}
                ]
            }
        },
        
        # Group by document, combine scores
        {
            "$group": {
                "_id": "$_id",
                "title": {"$first": "$title"},
                "content": {"$first": "$content"},
                "vs_score": {"$max": "$vs_score"},
                "fts_score": {"$max": "$fts_score"}
            }
        },
        
        # Calculate combined RRF score
        {
            "$addFields": {
                "vs_score": {"$ifNull": ["$vs_score", 0]},
                "fts_score": {"$ifNull": ["$fts_score", 0]},
            }
        },
        {
            "$addFields": {
                "combined_score": {
                    "$add": [
                        {"$multiply": ["$vs_score", vector_weight]},
                        {"$multiply": ["$fts_score", 1 - vector_weight]}
                    ]
                }
            }
        },
        
        # Sort and limit
        {"$sort": {"combined_score": -1}},
        {"$limit": limit},
        {"$project": {"title": 1, "content": 1, "combined_score": 1, "vs_score": 1, "fts_score": 1}}
    ]
    
    return list(collection.aggregate(pipeline))
```

### Step 3: RRF Implementation (Rank-Based)

```python
def hybrid_search_rrf(query: str, limit: int = 10, k: int = 60):
    """True RRF hybrid search using rank positions."""
    query_embedding = get_embedding(query)
    
    # Get vector search results with ranks
    vector_results = list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 150,
                "limit": 50
            }
        },
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        {"$project": {"title": 1, "score": 1}}
    ]))
    
    # Get full-text search results with ranks
    text_results = list(collection.aggregate([
        {
            "$search": {
                "index": "text_index",
                "text": {"query": query, "path": ["title", "content"]}
            }
        },
        {"$limit": 50},
        {"$addFields": {"score": {"$meta": "searchScore"}}},
        {"$project": {"title": 1, "score": 1}}
    ]))
    
    # Compute RRF scores
    rrf_scores = {}
    
    for rank, doc in enumerate(vector_results, 1):
        doc_id = str(doc["_id"])
        rrf_scores[doc_id] = rrf_scores.get(doc_id, {"doc": doc, "score": 0})
        rrf_scores[doc_id]["score"] += 1 / (k + rank)
    
    for rank, doc in enumerate(text_results, 1):
        doc_id = str(doc["_id"])
        rrf_scores[doc_id] = rrf_scores.get(doc_id, {"doc": doc, "score": 0})
        rrf_scores[doc_id]["score"] += 1 / (k + rank)
    
    # Sort by RRF score
    ranked = sorted(rrf_scores.values(), key=lambda x: x["score"], reverse=True)
    return [{"title": r["doc"]["title"], "rrf_score": r["score"]} for r in ranked[:limit]]
```

---

## 5.4 Hybrid Search with Geo Filters

Combine vector similarity + text relevance + geographic proximity:

```python
def geo_hybrid_search(query: str, lat: float, lng: float, radius_km: float, limit: int = 10):
    query_embedding = get_embedding(query)
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 200,
                "limit": 100,
                "filter": {
                    "location": {
                        "$geoWithin": {
                            "$centerSphere": [[lng, lat], radius_km / 6378.1]
                        }
                    }
                }
            }
        },
        {"$addFields": {"vector_score": {"$meta": "vectorSearchScore"}}},
        
        # Add distance calculation
        {
            "$addFields": {
                "distance_km": {
                    "$divide": [
                        {"$sqrt": {"$add": [
                            {"$pow": [{"$subtract": [{"$arrayElemAt": ["$location.coordinates", 0]}, lng]}, 2]},
                            {"$pow": [{"$subtract": [{"$arrayElemAt": ["$location.coordinates", 1]}, lat]}, 2]}
                        ]}},
                        0.009  # approximate degrees to km
                    ]
                }
            }
        },
        
        # Combined scoring: vector relevance + distance penalty
        {
            "$addFields": {
                "final_score": {
                    "$subtract": [
                        "$vector_score",
                        {"$multiply": [{"$divide": ["$distance_km", radius_km]}, 0.1]}
                    ]
                }
            }
        },
        
        {"$sort": {"final_score": -1}},
        {"$limit": limit}
    ]
    
    return list(collection.aggregate(pipeline))
```

---

## 5.5 Boosting Strategies

### Recency Boost

```python
{
    "$addFields": {
        "recency_factor": {
            "$divide": [
                1,
                {"$add": [
                    1,
                    {"$divide": [
                        {"$subtract": ["$$NOW", "$publishedAt"]},
                        86400000 * 30  # 30 days in ms
                    ]}
                ]}
            ]
        },
        "boosted_score": {
            "$add": [
                {"$multiply": ["$vector_score", 0.8]},
                {"$multiply": ["$recency_factor", 0.2]}
            ]
        }
    }
}
```

### Popularity Boost

```python
{
    "$addFields": {
        "popularity_factor": {
            "$min": [1, {"$divide": ["$viewCount", 10000]}]  # Cap at 1.0
        },
        "boosted_score": {
            "$add": [
                {"$multiply": ["$vector_score", 0.7]},
                {"$multiply": ["$popularity_factor", 0.3]}
            ]
        }
    }
}
```

---

## 5.6 When to Use Which Approach

| Use Case | Approach | Reason |
|----------|----------|--------|
| General Q&A | Vector only | Semantic understanding is key |
| Product search ("red Nike shoes size 10") | Hybrid | Mix of attributes + semantics |
| Error code lookup | Text only or Hybrid | Exact match critical |
| Similar article recommendation | Vector only | Pure semantic similarity |
| E-commerce with filters | Vector + filters | Semantic + structured |
| Legal document search | Hybrid + boost | Keywords + context + recency |

---

## Next: [Module 6 — Embedding Generation →](06_Embedding_Generation.md)
