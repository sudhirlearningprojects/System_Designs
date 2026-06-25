# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 5: RAG Application & Query Layer

---

## Application Architecture

```
Client Request
      │
      ▼
┌─────────────────┐
│   FastAPI App    │
│                  │
│  ┌───────────┐  │     ┌──────────────┐
│  │  Query    │──┼────▶│ MongoDB Atlas │
│  │  Router   │  │     │ Vector Search │
│  └─────┬─────┘  │     └──────────────┘
│        │        │
│  ┌─────▼─────┐  │     ┌──────────────┐
│  │  Reranker │──┼────▶│ Cohere/Local │
│  └─────┬─────┘  │     └──────────────┘
│        │        │
│  ┌─────▼─────┐  │     ┌──────────────┐
│  │ Generator │──┼────▶│  LLM (GPT-4o)│
│  └───────────┘  │     └──────────────┘
│                  │
└─────────────────┘
```

---

## Complete RAG Service

```python
# app/main.py
from fastapi import FastAPI, HTTPException, Depends
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from contextlib import asynccontextmanager
import time

from app.retriever import MongoDBRetriever
from app.generator import LLMGenerator
from app.auth import get_current_user, User
from app.cache import QueryCache

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.retriever = MongoDBRetriever()
    app.state.generator = LLMGenerator()
    app.state.cache = QueryCache()
    yield
    app.state.retriever.close()

app = FastAPI(title="Streaming RAG API", lifespan=lifespan)


class QueryRequest(BaseModel):
    question: str = Field(..., max_length=2000)
    top_k: int = Field(default=5, ge=1, le=20)
    filters: dict = Field(default_factory=dict)
    use_hybrid: bool = Field(default=True)
    stream: bool = Field(default=False)


class QueryResponse(BaseModel):
    answer: str
    sources: list[dict]
    latency_ms: float
    cached: bool = False


@app.post("/v1/query", response_model=QueryResponse)
async def query(request: QueryRequest, user: User = Depends(get_current_user)):
    start = time.time()
    
    # Check cache
    cached = await app.state.cache.get(request.question, user.tenant_id)
    if cached:
        return QueryResponse(**cached, latency_ms=(time.time()-start)*1000, cached=True)
    
    # Retrieve
    docs = await app.state.retriever.retrieve(
        query=request.question,
        tenant_id=user.tenant_id,
        user_roles=user.roles,
        k=request.top_k,
        filters=request.filters,
        use_hybrid=request.use_hybrid,
    )
    
    if not docs:
        return QueryResponse(
            answer="I couldn't find relevant information to answer your question.",
            sources=[],
            latency_ms=(time.time()-start)*1000,
        )
    
    # Generate
    answer = await app.state.generator.generate(
        query=request.question,
        context=docs,
    )
    
    response = QueryResponse(
        answer=answer,
        sources=[{"content": d["content"][:200], "source": d["metadata"].get("source_url", "")} for d in docs],
        latency_ms=(time.time()-start)*1000,
    )
    
    # Cache
    await app.state.cache.set(request.question, user.tenant_id, response.dict())
    
    return response


@app.post("/v1/query/stream")
async def query_stream(request: QueryRequest, user: User = Depends(get_current_user)):
    """Stream response tokens via SSE."""
    docs = await app.state.retriever.retrieve(
        query=request.question,
        tenant_id=user.tenant_id,
        user_roles=user.roles,
        k=request.top_k,
        use_hybrid=request.use_hybrid,
    )
    
    return StreamingResponse(
        app.state.generator.stream(query=request.question, context=docs),
        media_type="text/event-stream",
    )
```

---

## Retriever Module

```python
# app/retriever.py
from pymongo import MongoClient
from openai import AsyncOpenAI
import cohere
import numpy as np

class MongoDBRetriever:
    def __init__(self):
        self.client = MongoClient(
            "mongodb+srv://user:<password>@cluster.mongodb.net/",
            maxPoolSize=50,
            readPreference="secondaryPreferred",
        )
        self.collection = self.client["rag_db"]["document_chunks"]
        self.openai = AsyncOpenAI()
        self.cohere = cohere.AsyncClientV2()
        self.embedding_model = "text-embedding-3-small"
        self.embedding_dims = 1024
    
    async def retrieve(
        self,
        query: str,
        tenant_id: str,
        user_roles: list[str],
        k: int = 5,
        filters: dict = None,
        use_hybrid: bool = True,
    ) -> list[dict]:
        # Step 1: Embed query
        query_embedding = await self._embed_query(query)
        
        # Step 2: Vector search (with optional hybrid)
        if use_hybrid:
            results = await self._hybrid_search(query, query_embedding, tenant_id, user_roles, k * 3, filters)
        else:
            results = self._vector_search(query_embedding, tenant_id, user_roles, k * 3, filters)
        
        # Step 3: Rerank
        if results:
            results = await self._rerank(query, results, k)
        
        return results
    
    async def _embed_query(self, query: str) -> list[float]:
        response = await self.openai.embeddings.create(
            model=self.embedding_model,
            input=query,
            dimensions=self.embedding_dims,
        )
        return response.data[0].embedding
    
    def _vector_search(
        self, embedding: list[float], tenant_id: str, roles: list[str], k: int, filters: dict
    ) -> list[dict]:
        search_filter = {
            "$and": [
                {"tenant_id": {"$eq": tenant_id}},
                {"access_control": {"$in": roles}},
            ]
        }
        
        if filters:
            for field, value in filters.items():
                search_filter["$and"].append({f"metadata.{field}": {"$eq": value}})
        
        pipeline = [
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": embedding,
                    "numCandidates": min(k * 10, 200),
                    "limit": k,
                    "filter": search_filter,
                }
            },
            {
                "$project": {
                    "content": 1,
                    "metadata": 1,
                    "document_id": 1,
                    "chunk_index": 1,
                    "score": {"$meta": "vectorSearchScore"},
                    "embedding": 0,  # Exclude embedding from results
                }
            }
        ]
        
        return list(self.collection.aggregate(pipeline))
    
    async def _hybrid_search(
        self, query: str, embedding: list[float], tenant_id: str, roles: list[str], k: int, filters: dict
    ) -> list[dict]:
        # Vector results
        vector_results = self._vector_search(embedding, tenant_id, roles, k, filters)
        
        # Full-text results
        text_pipeline = [
            {
                "$search": {
                    "index": "text_search_index",
                    "compound": {
                        "must": [{"text": {"query": query, "path": "content"}}],
                        "filter": [
                            {"equals": {"path": "tenant_id", "value": tenant_id}},
                        ],
                    }
                }
            },
            {"$addFields": {"text_score": {"$meta": "searchScore"}}},
            {"$limit": k},
            {"$project": {"content": 1, "metadata": 1, "document_id": 1, "text_score": 1}},
        ]
        text_results = list(self.collection.aggregate(text_pipeline))
        
        # RRF fusion
        return self._rrf_merge(vector_results, text_results, k)
    
    async def _rerank(self, query: str, docs: list[dict], top_n: int) -> list[dict]:
        """Rerank using Cohere Rerank v3.5."""
        if not docs:
            return []
        
        response = await self.cohere.rerank(
            query=query,
            documents=[doc["content"] for doc in docs],
            model="rerank-v3.5",
            top_n=top_n,
        )
        
        reranked = []
        for result in response.results:
            doc = docs[result.index]
            doc["rerank_score"] = result.relevance_score
            reranked.append(doc)
        
        return reranked
    
    def _rrf_merge(self, vector_results: list, text_results: list, k: int, rrf_k: int = 60) -> list:
        scores = {}
        for rank, doc in enumerate(vector_results):
            doc_id = str(doc["_id"])
            scores[doc_id] = {"doc": doc, "score": 0.7 / (rank + rrf_k)}
        
        for rank, doc in enumerate(text_results):
            doc_id = str(doc["_id"])
            if doc_id in scores:
                scores[doc_id]["score"] += 0.3 / (rank + rrf_k)
            else:
                scores[doc_id] = {"doc": doc, "score": 0.3 / (rank + rrf_k)}
        
        sorted_results = sorted(scores.values(), key=lambda x: x["score"], reverse=True)
        return [item["doc"] for item in sorted_results[:k]]
    
    def close(self):
        self.client.close()
```

---

## Generator Module

```python
# app/generator.py
from openai import AsyncOpenAI
from typing import AsyncGenerator

class LLMGenerator:
    def __init__(self):
        self.client = AsyncOpenAI()
        self.model = "gpt-4o"
    
    async def generate(self, query: str, context: list[dict]) -> str:
        context_text = self._format_context(context)
        
        response = await self.client.chat.completions.create(
            model=self.model,
            temperature=0,
            max_tokens=2048,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": f"Context:\n{context_text}\n\nQuestion: {query}"},
            ],
        )
        return response.choices[0].message.content
    
    async def stream(self, query: str, context: list[dict]) -> AsyncGenerator[str, None]:
        context_text = self._format_context(context)
        
        stream = await self.client.chat.completions.create(
            model=self.model,
            temperature=0,
            max_tokens=2048,
            stream=True,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": f"Context:\n{context_text}\n\nQuestion: {query}"},
            ],
        )
        
        async for chunk in stream:
            if chunk.choices[0].delta.content:
                yield f"data: {chunk.choices[0].delta.content}\n\n"
        yield "data: [DONE]\n\n"
    
    def _format_context(self, docs: list[dict]) -> str:
        parts = []
        for i, doc in enumerate(docs, 1):
            source = doc.get("metadata", {}).get("source_url", "unknown")
            parts.append(f"[Source {i}: {source}]\n{doc['content']}")
        return "\n\n---\n\n".join(parts)


SYSTEM_PROMPT = """You are a helpful assistant that answers questions based on provided context.

Rules:
- Only use information from the context provided
- Cite sources using [Source N] format
- If the context doesn't contain the answer, say "I don't have enough information"
- Be concise and direct
- Do not hallucinate or add information not in the context"""
```

---

## LangChain Integration with MongoDB

```python
# Alternative: Use LangChain's MongoDB integration
from langchain_mongodb import MongoDBAtlasVectorSearch
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser

embeddings = OpenAIEmbeddings(model="text-embedding-3-small", dimensions=1024)

vectorstore = MongoDBAtlasVectorSearch(
    collection=collection,
    embedding=embeddings,
    index_name="vector_index",
    text_key="content",
    embedding_key="embedding",
    relevance_score_fn="cosine",
)

# Retriever with metadata filtering
retriever = vectorstore.as_retriever(
    search_type="similarity",
    search_kwargs={
        "k": 5,
        "pre_filter": {"tenant_id": {"$eq": "tenant_acme"}},
        "post_filter_pipeline": [
            {"$project": {"embedding": 0}},  # Exclude embedding from results
        ],
    },
)

# Build chain
prompt = ChatPromptTemplate.from_template("""
Answer based on context. Cite sources.
Context: {context}
Question: {question}
Answer:""")

def format_docs(docs):
    return "\n\n".join([d.page_content for d in docs])

chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}
    | prompt
    | ChatOpenAI(model="gpt-4o", temperature=0)
    | StrOutputParser()
)

answer = chain.invoke("How does our authentication work?")
```

---

## Conversation Memory with MongoDB

```python
# app/memory.py
from pymongo import MongoClient
from datetime import datetime, timedelta

class ConversationMemory:
    """Store chat history in MongoDB for multi-turn RAG."""
    
    def __init__(self, db):
        self.collection = db["conversations"]
        self.collection.create_index([("session_id", 1), ("timestamp", -1)])
        self.collection.create_index(
            [("timestamp", 1)], 
            expireAfterSeconds=24*60*60  # Auto-delete after 24 hours
        )
    
    async def add_turn(self, session_id: str, role: str, content: str):
        self.collection.insert_one({
            "session_id": session_id,
            "role": role,
            "content": content,
            "timestamp": datetime.utcnow(),
        })
    
    async def get_history(self, session_id: str, last_n: int = 5) -> list[dict]:
        cursor = self.collection.find(
            {"session_id": session_id},
            {"_id": 0, "role": 1, "content": 1},
        ).sort("timestamp", -1).limit(last_n)
        
        messages = list(cursor)
        messages.reverse()  # Chronological order
        return messages
    
    async def contextualize_query(self, query: str, session_id: str, llm) -> str:
        """Rewrite query using conversation history for standalone retrieval."""
        history = await self.get_history(session_id)
        
        if not history:
            return query
        
        history_text = "\n".join([f"{m['role']}: {m['content']}" for m in history])
        
        response = await llm.ainvoke(
            f"Given this chat history and new question, rewrite the question "
            f"to be standalone (not requiring history to understand).\n\n"
            f"History:\n{history_text}\n\n"
            f"New question: {query}\n\n"
            f"Standalone question:"
        )
        return response.content
```

---

## Cache Layer

```python
# app/cache.py
import hashlib
import json
from redis.asyncio import Redis

class QueryCache:
    def __init__(self, redis_url: str = "redis://localhost:6379"):
        self.redis = Redis.from_url(redis_url)
        self.ttl = 1800  # 30 minutes
    
    async def get(self, query: str, tenant_id: str) -> dict | None:
        key = self._cache_key(query, tenant_id)
        cached = await self.redis.get(key)
        return json.loads(cached) if cached else None
    
    async def set(self, query: str, tenant_id: str, response: dict):
        key = self._cache_key(query, tenant_id)
        await self.redis.setex(key, self.ttl, json.dumps(response))
    
    async def invalidate_tenant(self, tenant_id: str):
        """Invalidate all cache entries for a tenant (after new doc ingestion)."""
        pattern = f"rag:query:{tenant_id}:*"
        keys = []
        async for key in self.redis.scan_iter(match=pattern):
            keys.append(key)
        if keys:
            await self.redis.delete(*keys)
    
    def _cache_key(self, query: str, tenant_id: str) -> str:
        query_hash = hashlib.sha256(query.lower().strip().encode()).hexdigest()[:16]
        return f"rag:query:{tenant_id}:{query_hash}"
```

---

## Data Freshness Indicator

```python
# app/freshness.py

class FreshnessTracker:
    """Track data freshness and notify users about stale results."""
    
    def __init__(self, collection):
        self.collection = collection
    
    def get_freshness_info(self, document_ids: list[str]) -> dict:
        """Return freshness metadata for retrieved documents."""
        pipeline = [
            {"$match": {"document_id": {"$in": document_ids}}},
            {"$group": {
                "_id": None,
                "oldest_update": {"$min": "$updated_at"},
                "newest_update": {"$max": "$updated_at"},
                "total_chunks": {"$sum": 1},
            }},
        ]
        
        result = list(self.collection.aggregate(pipeline))
        if not result:
            return {"fresh": False, "message": "No data found"}
        
        info = result[0]
        age_seconds = (datetime.utcnow() - info["newest_update"]).total_seconds()
        
        return {
            "fresh": age_seconds < 300,  # <5 minutes = fresh
            "last_updated_seconds_ago": int(age_seconds),
            "oldest_source": info["oldest_update"].isoformat(),
            "newest_source": info["newest_update"].isoformat(),
        }
```

---

## Next: [Part 6 - Production Deployment & Monitoring](./kafka-flink-mongodb-rag-part6.md)
