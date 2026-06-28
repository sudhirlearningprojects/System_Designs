# Module 4: Basic Vector Search

---

## 4.1 The `$vectorSearch` Aggregation Stage

`$vectorSearch` is a **first stage** in the aggregation pipeline (like `$search`). It cannot appear after other stages.

### Basic Syntax

```javascript
db.collection.aggregate([
  {
    $vectorSearch: {
      index: "vector_index",         // Required: index name
      path: "embedding",             // Required: field containing vectors
      queryVector: [0.1, -0.2, ...], // Required: your query vector
      numCandidates: 150,            // Required: ANN exploration (recall control)
      limit: 10,                     // Required: number of results
      filter: { ... }               // Optional: pre-filter expression
    }
  }
])
```

### Parameters Explained

| Parameter | Required | Description |
|-----------|----------|-------------|
| `index` | ✅ | Name of the vector search index |
| `path` | ✅ | Field path containing the vector embedding |
| `queryVector` | ✅ | The query embedding (must match index dimensions) |
| `numCandidates` | ✅ | Number of candidates to consider (higher = better recall) |
| `limit` | ✅ | Max documents to return |
| `filter` | ❌ | Pre-filter expression on indexed filter fields |

### numCandidates Guidelines

```
numCandidates must be >= limit and <= 10,000

Rule of thumb:
  - numCandidates = 10× limit → good recall (~95%)
  - numCandidates = 20× limit → great recall (~98%)
  - numCandidates = 50× limit → excellent recall (~99.5%)
  
  With filters:
  - numCandidates = 20-50× limit (filters reduce candidate pool)
```

---

## 4.2 Complete Query Examples

### Python — Basic Semantic Search

```python
from config import collection, get_embedding

def semantic_search(query: str, limit: int = 10) -> list:
    query_embedding = get_embedding(query)
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": limit * 15,
                "limit": limit
            }
        },
        {
            "$project": {
                "title": 1,
                "content": 1,
                "score": {"$meta": "vectorSearchScore"},
                "_id": 0
            }
        }
    ]
    
    return list(collection.aggregate(pipeline))

# Usage
results = semantic_search("How does MongoDB handle distributed transactions?")
for r in results:
    print(f"[{r['score']:.4f}] {r['title']}")
```

### Python — Search with Filters

```python
def filtered_search(query: str, category: str, min_date=None, limit: int = 10) -> list:
    query_embedding = get_embedding(query)
    
    # Build filter
    filter_expr = {"category": {"$eq": category}}
    if min_date:
        filter_expr = {
            "$and": [
                {"category": {"$eq": category}},
                {"publishedAt": {"$gte": min_date}}
            ]
        }
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": limit * 20,  # Higher with filters
                "limit": limit,
                "filter": filter_expr
            }
        },
        {
            "$project": {
                "title": 1,
                "category": 1,
                "publishedAt": 1,
                "score": {"$meta": "vectorSearchScore"}
            }
        }
    ]
    
    return list(collection.aggregate(pipeline))

# Usage
from datetime import datetime
results = filtered_search(
    query="machine learning best practices",
    category="technology",
    min_date=datetime(2024, 1, 1)
)
```

### Node.js — Basic Search

```javascript
const { collection, getEmbedding } = require('./config');

async function semanticSearch(query, limit = 10) {
  const queryEmbedding = await getEmbedding(query);
  
  const results = await collection.aggregate([
    {
      $vectorSearch: {
        index: 'vector_index',
        path: 'embedding',
        queryVector: queryEmbedding,
        numCandidates: limit * 15,
        limit: limit
      }
    },
    {
      $project: {
        title: 1,
        content: 1,
        score: { $meta: 'vectorSearchScore' },
        _id: 0
      }
    }
  ]).toArray();
  
  return results;
}
```

### Java — Spring Boot

```java
@Service
public class VectorSearchService {
    
    private final MongoTemplate mongoTemplate;
    
    public List<Document> search(String query, int limit) {
        float[] queryVector = embeddingService.getEmbedding(query);
        
        List<AggregationOperation> pipeline = List.of(
            context -> new Document("$vectorSearch", new Document()
                .append("index", "vector_index")
                .append("path", "embedding")
                .append("queryVector", Arrays.asList(ArrayUtils.toObject(queryVector)))
                .append("numCandidates", limit * 15)
                .append("limit", limit)),
            Aggregation.project("title", "content")
                .and(MetaExpression.meta("vectorSearchScore")).as("score")
        );
        
        Aggregation aggregation = Aggregation.newAggregation(pipeline);
        return mongoTemplate.aggregate(aggregation, "documents", Document.class)
            .getMappedResults();
    }
}
```

---

## 4.3 Working with Scores

### Score Interpretation

```python
# Cosine similarity scores in MongoDB Vector Search:
#   1.0    = Perfect match (identical vectors)
#   0.9+   = Very high similarity
#   0.7-0.9 = Good similarity (typical for relevant results)
#   0.5-0.7 = Moderate similarity
#   < 0.5  = Low similarity (probably not relevant)

def search_with_threshold(query: str, threshold: float = 0.7) -> list:
    query_embedding = get_embedding(query)
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 200,
                "limit": 50  # Fetch more, then filter by score
            }
        },
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        {"$match": {"score": {"$gte": threshold}}},  # Score threshold
        {"$limit": 10},
        {"$project": {"title": 1, "score": 1, "_id": 0}}
    ]
    
    return list(collection.aggregate(pipeline))
```

### Score Normalization Across Metrics

| Similarity Metric | Raw Range | MongoDB Score Range | "Good" Score |
|-------------------|-----------|---------------------|--------------|
| Cosine | [-1, 1] | [0, 1] | > 0.7 |
| Euclidean | [0, ∞) | (0, 1] via `1/(1+d)` | > 0.8 |
| Dot Product | (-∞, ∞) | Normalized | Depends on data |

---

## 4.4 Pagination

Vector search doesn't support traditional skip/limit pagination efficiently. Use these patterns:

### Pattern 1: Score-based Cursor (Recommended)

```python
def paginated_search(query: str, page_size: int = 10, last_score: float = None, last_id=None):
    query_embedding = get_embedding(query)
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 200,
                "limit": 100  # Fetch enough for multiple pages
            }
        },
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
    ]
    
    # Apply cursor for pagination
    if last_score is not None and last_id is not None:
        pipeline.append({
            "$match": {
                "$or": [
                    {"score": {"$lt": last_score}},
                    {"score": last_score, "_id": {"$gt": last_id}}
                ]
            }
        })
    
    pipeline.append({"$limit": page_size})
    pipeline.append({"$project": {"title": 1, "score": 1}})
    
    results = list(collection.aggregate(pipeline))
    
    # Return cursor info for next page
    next_cursor = None
    if results:
        last = results[-1]
        next_cursor = {"score": last["score"], "id": str(last["_id"])}
    
    return {"results": results, "next_cursor": next_cursor}
```

### Pattern 2: Over-fetch and Slice

```python
def simple_pagination(query: str, page: int = 1, page_size: int = 10):
    """Simple but less efficient — fetches all and slices."""
    query_embedding = get_embedding(query)
    skip = (page - 1) * page_size
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 200,
                "limit": skip + page_size  # Fetch enough to cover the page
            }
        },
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        {"$skip": skip},
        {"$limit": page_size},
        {"$project": {"title": 1, "score": 1, "_id": 0}}
    ]
    
    return list(collection.aggregate(pipeline))
```

---

## 4.5 Post-Processing with Aggregation Pipeline

Since `$vectorSearch` is the first stage, you can chain any aggregation stages after it:

```python
def advanced_search(query: str):
    query_embedding = get_embedding(query)
    
    pipeline = [
        # Stage 1: Vector search
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 200,
                "limit": 50
            }
        },
        # Stage 2: Add score
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        
        # Stage 3: Lookup related data
        {
            "$lookup": {
                "from": "authors",
                "localField": "authorId",
                "foreignField": "_id",
                "as": "author"
            }
        },
        {"$unwind": "$author"},
        
        # Stage 4: Filter by score
        {"$match": {"score": {"$gte": 0.7}}},
        
        # Stage 5: Group by category
        {
            "$group": {
                "_id": "$category",
                "topResults": {"$push": {"title": "$title", "score": "$score"}},
                "avgScore": {"$avg": "$score"},
                "count": {"$sum": 1}
            }
        },
        
        # Stage 6: Sort
        {"$sort": {"avgScore": -1}},
        
        # Stage 7: Final projection
        {
            "$project": {
                "category": "$_id",
                "topResults": {"$slice": ["$topResults", 3]},
                "avgScore": {"$round": ["$avgScore", 4]},
                "count": 1
            }
        }
    ]
    
    return list(collection.aggregate(pipeline))
```

---

## 4.6 Error Handling

```python
from pymongo.errors import OperationFailure

def safe_vector_search(query: str, limit: int = 10):
    try:
        query_embedding = get_embedding(query)
        
        if len(query_embedding) != 1536:
            raise ValueError(f"Expected 1536 dims, got {len(query_embedding)}")
        
        results = list(collection.aggregate([
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": query_embedding,
                    "numCandidates": min(limit * 15, 10000),
                    "limit": limit
                }
            },
            {"$project": {"title": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
        
        return {"success": True, "results": results}
        
    except OperationFailure as e:
        if "index not found" in str(e):
            return {"success": False, "error": "Vector index not ready"}
        elif "numCandidates" in str(e):
            return {"success": False, "error": "numCandidates must be >= limit"}
        raise
    except Exception as e:
        return {"success": False, "error": str(e)}
```

---

## 4.7 Common Mistakes & Fixes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Wrong dimensions | `"dimension mismatch"` error | Ensure query vector matches index `numDimensions` |
| numCandidates < limit | Validation error | Set `numCandidates >= limit` |
| numCandidates > 10000 | Validation error | Cap at 10000, reduce limit if needed |
| No filter index | Filter silently ignored | Add `"type": "filter"` fields to index |
| Using `$vectorSearch` after `$match` | Pipeline error | `$vectorSearch` must be the first stage |
| Embedding model mismatch | Poor results (low scores) | Use same model for indexing and querying |
| Missing `$meta` for score | No score in output | Use `{"$meta": "vectorSearchScore"}` |

---

## Next: [Module 5 — Hybrid Search →](05_Hybrid_Search.md)
