# Module 8: Advanced Patterns

---

## 8.1 Multi-Tenant Vector Search

Isolate tenant data while sharing infrastructure:

```python
# Index definition with tenant filter
index_definition = {
    "fields": [
        {"type": "vector", "path": "embedding", "numDimensions": 1536, "similarity": "cosine"},
        {"type": "filter", "path": "tenant_id"},
        {"type": "filter", "path": "access_level"}
    ]
}

def tenant_search(query: str, tenant_id: str, limit: int = 10) -> list:
    embedding = get_embedding(query)
    
    return list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": embedding,
                "numCandidates": limit * 20,
                "limit": limit,
                "filter": {"tenant_id": {"$eq": tenant_id}}
            }
        },
        {"$project": {"content": 1, "title": 1, "score": {"$meta": "vectorSearchScore"}}}
    ]))
```

### Multi-Tenant Architecture Options

```
Option 1: Single Collection + Filter (recommended for <1000 tenants)
  ┌─────────────────────────────────────────┐
  │  Collection: documents                   │
  │  { tenant_id: "A", embedding: [...] }   │
  │  { tenant_id: "B", embedding: [...] }   │
  │  Filter: tenant_id = current_tenant      │
  └─────────────────────────────────────────┘

Option 2: Collection-per-Tenant (for strict isolation)
  ┌──────────────────┐  ┌──────────────────┐
  │ tenant_a_docs    │  │ tenant_b_docs    │
  │ (own vector idx) │  │ (own vector idx) │
  └──────────────────┘  └──────────────────┘

Option 3: Database-per-Tenant (enterprise, compliance)
  ┌──────────────────┐  ┌──────────────────┐
  │ DB: tenant_a     │  │ DB: tenant_b     │
  │ (full isolation) │  │ (full isolation) │
  └──────────────────┘  └──────────────────┘
```

---

## 8.2 Multi-Modal Search

Search across text, images, and other modalities:

```python
from openai import OpenAI
import base64

client = OpenAI()

def get_image_embedding(image_path: str) -> list[float]:
    """Get embedding for an image using CLIP-style model via OpenAI."""
    # For multimodal, use a model like voyage-multimodal-3
    import voyageai
    vo = voyageai.Client()
    
    with open(image_path, "rb") as f:
        image_b64 = base64.b64encode(f.read()).decode()
    
    result = vo.multimodal_embed(
        inputs=[[{"type": "image", "data": image_b64}]],
        model="voyage-multimodal-3"
    )
    return result.embeddings[0]

def get_text_embedding_multimodal(text: str) -> list[float]:
    """Get text embedding from same multimodal model."""
    import voyageai
    vo = voyageai.Client()
    
    result = vo.multimodal_embed(
        inputs=[[{"type": "text", "content": text}]],
        model="voyage-multimodal-3"
    )
    return result.embeddings[0]

# Schema for multimodal documents
multimodal_doc = {
    "type": "image",  # or "text", "video_frame"
    "content": "A sunset over the ocean",
    "file_url": "s3://bucket/image.jpg",
    "embedding": get_image_embedding("image.jpg"),  # Same vector space!
    "metadata": {"width": 1920, "height": 1080, "format": "jpg"}
}

# Text query finds similar images (cross-modal search)
def cross_modal_search(text_query: str, limit: int = 10):
    embedding = get_text_embedding_multimodal(text_query)
    return list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "multimodal_index",
                "path": "embedding",
                "queryVector": embedding,
                "numCandidates": 150,
                "limit": limit
            }
        }
    ]))
```

---

## 8.3 Re-Ranking

Two-stage retrieval: fast ANN → precise re-ranking:

```python
import cohere

co = cohere.ClientV2()

def search_with_reranking(query: str, limit: int = 5) -> list:
    """Stage 1: Vector search (fast). Stage 2: Re-rank (precise)."""
    
    # Stage 1: Retrieve candidates (over-fetch)
    embedding = get_embedding(query)
    candidates = list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": embedding,
                "numCandidates": 200,
                "limit": 30  # Fetch 30, re-rank to top 5
            }
        },
        {"$project": {"content": 1, "title": 1, "score": {"$meta": "vectorSearchScore"}}}
    ]))
    
    if not candidates:
        return []
    
    # Stage 2: Re-rank with Cohere (cross-encoder)
    rerank_response = co.rerank(
        query=query,
        documents=[c["content"] for c in candidates],
        model="rerank-english-v3.0",
        top_n=limit
    )
    
    # Map back to original documents
    results = []
    for r in rerank_response.results:
        doc = candidates[r.index]
        doc["rerank_score"] = r.relevance_score
        results.append(doc)
    
    return results
```

### When to Re-Rank

| Scenario | Re-Rank? | Reason |
|----------|----------|--------|
| RAG (accuracy critical) | ✅ Yes | Better context = better LLM output |
| High-volume search (>1000 QPS) | ❌ No | Too expensive/slow |
| Long documents | ✅ Yes | Cross-encoders handle length better |
| Quick autocomplete | ❌ No | Latency sensitive |

---

## 8.4 Metadata-Enriched Vectors

Store rich metadata alongside vectors for complex filtering:

```python
# Document with rich metadata
doc = {
    "content": "Introduction to machine learning...",
    "embedding": get_embedding("Introduction to machine learning..."),
    
    # Filterable metadata
    "metadata": {
        "category": "technology",
        "subcategory": "machine-learning",
        "difficulty": "beginner",
        "author_id": "auth-123",
        "language": "en",
        "word_count": 2500,
        "created_at": datetime(2025, 1, 15),
        "updated_at": datetime(2025, 3, 20),
        "tags": ["ml", "ai", "neural-networks"],
        "access_level": "public",
        "department": "engineering"
    }
}

# Complex filtered search
def advanced_filtered_search(
    query: str,
    categories: list[str] = None,
    difficulty: str = None,
    date_range: tuple = None,
    language: str = "en",
    limit: int = 10
):
    embedding = get_embedding(query)
    
    # Build compound filter
    filters = []
    if categories:
        filters.append({"metadata.category": {"$in": categories}})
    if difficulty:
        filters.append({"metadata.difficulty": {"$eq": difficulty}})
    if date_range:
        filters.append({"metadata.created_at": {"$gte": date_range[0], "$lte": date_range[1]}})
    if language:
        filters.append({"metadata.language": {"$eq": language}})
    
    filter_expr = {"$and": filters} if len(filters) > 1 else (filters[0] if filters else None)
    
    vector_search = {
        "index": "vector_index",
        "path": "embedding",
        "queryVector": embedding,
        "numCandidates": limit * 20,
        "limit": limit
    }
    if filter_expr:
        vector_search["filter"] = filter_expr
    
    return list(collection.aggregate([
        {"$vectorSearch": vector_search},
        {"$project": {"content": 1, "metadata": 1, "score": {"$meta": "vectorSearchScore"}}}
    ]))
```

---

## 8.5 Semantic Caching

Cache expensive embedding + LLM calls using vector similarity:

```python
class SemanticCache:
    """Cache RAG responses based on semantic similarity of questions."""
    
    def __init__(self, threshold: float = 0.95):
        self.cache_collection = db["query_cache"]
        self.threshold = threshold
    
    def get(self, question: str) -> dict | None:
        """Check cache for semantically similar question."""
        embedding = get_embedding(question)
        
        results = list(self.cache_collection.aggregate([
            {
                "$vectorSearch": {
                    "index": "cache_vector_index",
                    "path": "question_embedding",
                    "queryVector": embedding,
                    "numCandidates": 10,
                    "limit": 1
                }
            },
            {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
            {"$match": {"score": {"$gte": self.threshold}}}
        ]))
        
        if results:
            # Cache hit!
            return {"answer": results[0]["answer"], "cached": True}
        return None
    
    def set(self, question: str, answer: str, sources: list):
        """Store response in cache."""
        self.cache_collection.insert_one({
            "question": question,
            "question_embedding": get_embedding(question),
            "answer": answer,
            "sources": sources,
            "created_at": datetime.now(timezone.utc),
            "ttl": datetime.now(timezone.utc) + timedelta(hours=24)
        })
    
    def query(self, question: str) -> dict:
        """Query with cache check."""
        cached = self.get(question)
        if cached:
            return cached
        
        result = rag_query(question)
        self.set(question, result["answer"], result["sources"])
        return result

# TTL index to auto-expire cache entries
cache_collection.create_index("ttl", expireAfterSeconds=0)
```

---

## 8.6 Streaming Vector Search Results

For real-time applications, stream results as they're found:

```python
import asyncio
from motor.motor_asyncio import AsyncIOMotorClient

async_client = AsyncIOMotorClient("mongodb+srv://...")
async_collection = async_client["mydb"]["documents"]

async def streaming_search(query: str, limit: int = 10):
    """Async streaming vector search."""
    embedding = get_embedding(query)
    
    pipeline = [
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": embedding,
                "numCandidates": 150,
                "limit": limit
            }
        },
        {"$project": {"title": 1, "content": 1, "score": {"$meta": "vectorSearchScore"}}}
    ]
    
    async for doc in async_collection.aggregate(pipeline):
        yield doc  # Stream each result as it arrives

# FastAPI streaming endpoint
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()

@app.get("/search/stream")
async def stream_search(q: str):
    async def generate():
        async for result in streaming_search(q):
            yield json.dumps({"title": result["title"], "score": result["score"]}) + "\n"
    
    return StreamingResponse(generate(), media_type="application/x-ndjson")
```

---

## 8.7 Vector Search with Change Streams

React to new vectors in real-time:

```python
def watch_for_similar_content(threshold: float = 0.9):
    """Monitor new inserts and flag potentially duplicate content."""
    
    with collection.watch([{"$match": {"operationType": "insert"}}]) as stream:
        for change in stream:
            new_doc = change["fullDocument"]
            
            if "embedding" not in new_doc:
                continue
            
            # Search for similar existing content
            similar = list(collection.aggregate([
                {
                    "$vectorSearch": {
                        "index": "vector_index",
                        "path": "embedding",
                        "queryVector": new_doc["embedding"],
                        "numCandidates": 50,
                        "limit": 5,
                        "filter": {"_id": {"$ne": new_doc["_id"]}}
                    }
                },
                {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
                {"$match": {"score": {"$gte": threshold}}}
            ]))
            
            if similar:
                print(f"⚠️ Potential duplicate: '{new_doc['title']}' is {similar[0]['score']:.2%} similar to '{similar[0]['title']}'")
                # Flag for review
                collection.update_one(
                    {"_id": new_doc["_id"]},
                    {"$set": {"flags.potential_duplicate": True, "flags.similar_to": similar[0]["_id"]}}
                )
```

---

## 8.8 Agentic RAG (Tool-Augmented)

LLM decides when and how to search:

```python
import json

tools = [
    {
        "type": "function",
        "function": {
            "name": "vector_search",
            "description": "Search the knowledge base for relevant information",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Search query"},
                    "category": {"type": "string", "description": "Optional category filter"},
                    "limit": {"type": "integer", "description": "Number of results", "default": 5}
                },
                "required": ["query"]
            }
        }
    }
]

def agentic_rag(question: str) -> str:
    messages = [
        {"role": "system", "content": "You are a helpful assistant with access to a knowledge base. Use the vector_search tool to find relevant information before answering."},
        {"role": "user", "content": question}
    ]
    
    while True:
        response = openai_client.chat.completions.create(
            model="gpt-4o", messages=messages, tools=tools, tool_choice="auto"
        )
        
        msg = response.choices[0].message
        
        if msg.tool_calls:
            messages.append(msg)
            for tool_call in msg.tool_calls:
                args = json.loads(tool_call.function.arguments)
                results = tenant_search(args["query"], "default", args.get("limit", 5))
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": json.dumps([{"title": r["title"], "content": r["content"][:500]} for r in results])
                })
        else:
            return msg.content
```

---

## Next: [Module 9 — Performance & Scaling →](09_Performance_Scaling.md)
