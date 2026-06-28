# Module 11: Real-World Projects

---

## Project 1: Semantic Document Search Engine

A full-stack search engine for internal knowledge bases (Confluence/Notion replacement).

### Architecture

```
┌───────────┐     ┌──────────┐     ┌───────────────┐     ┌──────────┐
│  Web UI   │────▶│ FastAPI  │────▶│ MongoDB Atlas │────▶│  OpenAI  │
│ (React)   │◀────│ Backend  │◀────│ Vector Search │     │   API    │
└───────────┘     └──────────┘     └───────────────┘     └──────────┘
                       │
                  ┌────▼────┐
                  │  Redis  │
                  │ (Cache) │
                  └─────────┘
```

### Implementation

```python
# app.py — Complete semantic search API
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel
from datetime import datetime, timezone
from config import collection, get_embedding, get_openai_embeddings_batch
import hashlib

app = FastAPI(title="Semantic Search Engine")

class DocumentInput(BaseModel):
    title: str
    content: str
    category: str = "general"
    tags: list[str] = []

class SearchQuery(BaseModel):
    query: str
    category: str | None = None
    limit: int = 10
    min_score: float = 0.5

@app.post("/documents")
async def ingest_document(doc: DocumentInput):
    """Ingest a document with automatic chunking and embedding."""
    chunks = semantic_chunks(doc.content)
    
    docs_to_insert = []
    for i, chunk in enumerate(chunks):
        text_for_embedding = f"{doc.title}\n\n{chunk}"
        embedding = get_embedding(text_for_embedding)
        
        docs_to_insert.append({
            "title": doc.title,
            "content": chunk,
            "embedding": embedding,
            "category": doc.category,
            "tags": doc.tags,
            "chunk_index": i,
            "total_chunks": len(chunks),
            "content_hash": hashlib.sha256(chunk.encode()).hexdigest(),
            "created_at": datetime.now(timezone.utc)
        })
    
    result = collection.insert_many(docs_to_insert)
    return {"inserted": len(result.inserted_ids), "chunks": len(chunks)}

@app.post("/search")
async def search(query: SearchQuery):
    """Semantic search with optional filters."""
    embedding = get_embedding(query.query)
    
    filter_expr = None
    if query.category:
        filter_expr = {"category": {"$eq": query.category}}
    
    vs_params = {
        "index": "vector_index",
        "path": "embedding",
        "queryVector": embedding,
        "numCandidates": query.limit * 15,
        "limit": query.limit * 2  # Over-fetch for score filtering
    }
    if filter_expr:
        vs_params["filter"] = filter_expr
    
    results = list(collection.aggregate([
        {"$vectorSearch": vs_params},
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        {"$match": {"score": {"$gte": query.min_score}}},
        {"$limit": query.limit},
        {"$project": {"title": 1, "content": 1, "category": 1, "score": 1, "tags": 1, "_id": 0}}
    ]))
    
    return {"results": results, "total": len(results)}

@app.post("/ask")
async def ask_question(query: SearchQuery):
    """RAG-powered Q&A."""
    # Retrieve
    embedding = get_embedding(query.query)
    docs = list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": embedding,
                "numCandidates": 150,
                "limit": 5
            }
        },
        {"$project": {"title": 1, "content": 1, "score": {"$meta": "vectorSearchScore"}}}
    ]))
    
    context = "\n\n---\n\n".join([f"[{d['title']}]: {d['content']}" for d in docs])
    
    # Generate
    from openai import OpenAI
    client = OpenAI()
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": "Answer based on the provided context. Cite sources."},
            {"role": "user", "content": f"Context:\n{context}\n\nQuestion: {query.query}"}
        ],
        temperature=0.1
    )
    
    return {
        "answer": response.choices[0].message.content,
        "sources": [{"title": d["title"], "score": d["score"]} for d in docs]
    }
```

---

## Project 2: AI Customer Support Chatbot

A conversational support bot with RAG over product documentation.

```python
# chatbot.py
from collections import deque
from datetime import datetime, timezone

class SupportChatbot:
    def __init__(self, tenant_id: str):
        self.tenant_id = tenant_id
        self.sessions = {}  # session_id -> conversation history
    
    def chat(self, session_id: str, message: str) -> dict:
        """Handle a chat message with context-aware RAG."""
        
        # Get or create session
        if session_id not in self.sessions:
            self.sessions[session_id] = deque(maxlen=20)
        history = self.sessions[session_id]
        
        # Contextualize query
        search_query = self._contextualize(message, history)
        
        # Retrieve relevant docs
        embedding = get_embedding(search_query)
        docs = list(collection.aggregate([
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": embedding,
                    "numCandidates": 150,
                    "limit": 5,
                    "filter": {"tenant_id": {"$eq": self.tenant_id}}
                }
            },
            {"$project": {"content": 1, "title": 1, "url": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
        
        context = "\n\n".join([d["content"] for d in docs if d["score"] > 0.6])
        
        # Generate response
        messages = [
            {"role": "system", "content": """You are a helpful customer support agent. 
Answer based on the provided knowledge base. If you can't find the answer, say so and suggest contacting human support.
Be concise and friendly."""}
        ]
        for h in history:
            messages.append({"role": h["role"], "content": h["content"]})
        messages.append({"role": "user", "content": f"Knowledge Base:\n{context}\n\nUser: {message}"})
        
        from openai import OpenAI
        response = OpenAI().chat.completions.create(
            model="gpt-4o-mini", messages=messages, temperature=0.3, max_tokens=500
        )
        answer = response.choices[0].message.content
        
        # Update history
        history.append({"role": "user", "content": message})
        history.append({"role": "assistant", "content": answer})
        
        # Log interaction
        db["chat_logs"].insert_one({
            "session_id": session_id,
            "tenant_id": self.tenant_id,
            "message": message,
            "response": answer,
            "sources": [d["title"] for d in docs[:3]],
            "timestamp": datetime.now(timezone.utc)
        })
        
        return {
            "response": answer,
            "sources": [{"title": d["title"], "url": d.get("url"), "score": d["score"]} for d in docs[:3]],
            "confidence": max([d["score"] for d in docs]) if docs else 0
        }
    
    def _contextualize(self, message: str, history: deque) -> str:
        """Rewrite message with conversation context for better retrieval."""
        if not history:
            return message
        
        recent = list(history)[-4:]  # Last 2 exchanges
        history_text = "\n".join([f"{'User' if h['role']=='user' else 'Agent'}: {h['content']}" for h in recent])
        
        from openai import OpenAI
        result = OpenAI().chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": f"Rewrite as standalone search query:\nHistory:\n{history_text}\nLatest: {message}\nStandalone query:"}],
            temperature=0, max_tokens=100
        )
        return result.choices[0].message.content
```

---

## Project 3: E-Commerce Product Recommendation Engine

```python
# recommendations.py

class RecommendationEngine:
    def __init__(self):
        self.products = db["products"]  # Products with embeddings
        self.interactions = db["user_interactions"]  # User behavior
    
    def similar_products(self, product_id: str, limit: int = 10) -> list:
        """Find products similar to a given product."""
        product = self.products.find_one({"_id": product_id})
        if not product or "embedding" not in product:
            return []
        
        return list(self.products.aggregate([
            {
                "$vectorSearch": {
                    "index": "product_vector_index",
                    "path": "embedding",
                    "queryVector": product["embedding"],
                    "numCandidates": limit * 15,
                    "limit": limit + 1,  # +1 to exclude self
                    "filter": {"_id": {"$ne": product_id}}
                }
            },
            {"$limit": limit},
            {"$project": {"name": 1, "price": 1, "image_url": 1, "category": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
    
    def personalized_recommendations(self, user_id: str, limit: int = 20) -> list:
        """Hybrid: user profile embedding + collaborative filtering."""
        
        # Get user's recent interactions
        recent = list(self.interactions.find(
            {"user_id": user_id, "type": {"$in": ["view", "purchase", "add_to_cart"]}},
            sort=[("timestamp", -1)],
            limit=10
        ))
        
        if not recent:
            return self._trending_products(limit)
        
        # Build user preference vector (weighted average of interacted products)
        weights = {"purchase": 3.0, "add_to_cart": 2.0, "view": 1.0}
        product_ids = [i["product_id"] for i in recent]
        products = list(self.products.find({"_id": {"$in": product_ids}, "embedding": {"$exists": True}}))
        
        if not products:
            return self._trending_products(limit)
        
        # Weighted average embedding
        import numpy as np
        embeddings = []
        w = []
        for interaction in recent:
            product = next((p for p in products if p["_id"] == interaction["product_id"]), None)
            if product:
                embeddings.append(product["embedding"])
                w.append(weights.get(interaction["type"], 1.0))
        
        user_embedding = np.average(embeddings, axis=0, weights=w).tolist()
        
        # Search for products similar to user's preference
        already_seen = set(product_ids)
        results = list(self.products.aggregate([
            {
                "$vectorSearch": {
                    "index": "product_vector_index",
                    "path": "embedding",
                    "queryVector": user_embedding,
                    "numCandidates": 200,
                    "limit": limit + len(already_seen)
                }
            },
            {"$match": {"_id": {"$nin": list(already_seen)}}},
            {"$limit": limit},
            {"$project": {"name": 1, "price": 1, "image_url": 1, "category": 1, "rating": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
        
        return results
    
    def search_products(self, query: str, filters: dict = None, limit: int = 20) -> list:
        """Semantic product search with metadata filters."""
        embedding = get_embedding(query)
        
        filter_expr = {}
        if filters:
            conditions = []
            if "category" in filters:
                conditions.append({"category": {"$eq": filters["category"]}})
            if "min_price" in filters:
                conditions.append({"price": {"$gte": filters["min_price"]}})
            if "max_price" in filters:
                conditions.append({"price": {"$lte": filters["max_price"]}})
            if "in_stock" in filters and filters["in_stock"]:
                conditions.append({"stock": {"$gt": 0}})
            if conditions:
                filter_expr = {"$and": conditions} if len(conditions) > 1 else conditions[0]
        
        vs_params = {
            "index": "product_vector_index",
            "path": "embedding",
            "queryVector": embedding,
            "numCandidates": limit * 20,
            "limit": limit
        }
        if filter_expr:
            vs_params["filter"] = filter_expr
        
        return list(self.products.aggregate([
            {"$vectorSearch": vs_params},
            {"$project": {"name": 1, "price": 1, "image_url": 1, "description": 1, "rating": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
    
    def _trending_products(self, limit: int) -> list:
        """Fallback: return trending products."""
        return list(self.products.find(
            {"trending": True},
            {"name": 1, "price": 1, "image_url": 1, "category": 1}
        ).sort("view_count", -1).limit(limit))
```

---

## Project 4: Code Search Engine

Search code repositories by natural language descriptions:

```python
# code_search.py
import voyageai

vo = voyageai.Client()

def embed_code(code: str) -> list[float]:
    result = vo.embed([code], model="voyage-code-3", input_type="document")
    return result.embeddings[0]

def embed_code_query(query: str) -> list[float]:
    result = vo.embed([query], model="voyage-code-3", input_type="query")
    return result.embeddings[0]

class CodeSearchEngine:
    def __init__(self):
        self.collection = db["code_snippets"]
    
    def index_repository(self, repo_path: str):
        """Index all code files in a repository."""
        import os
        
        extensions = {".py", ".js", ".ts", ".java", ".go", ".rs", ".cpp"}
        docs = []
        
        for root, dirs, files in os.walk(repo_path):
            dirs[:] = [d for d in dirs if d not in {"node_modules", ".git", "venv", "__pycache__"}]
            for file in files:
                if any(file.endswith(ext) for ext in extensions):
                    filepath = os.path.join(root, file)
                    with open(filepath, "r", errors="ignore") as f:
                        content = f.read()
                    
                    # Split into functions/classes
                    chunks = self._split_code(content, filepath)
                    for chunk in chunks:
                        docs.append({
                            "code": chunk["code"],
                            "filepath": filepath,
                            "language": file.split(".")[-1],
                            "function_name": chunk.get("name", ""),
                            "description": chunk.get("docstring", ""),
                            "embedding": embed_code(chunk["code"]),
                            "repo": os.path.basename(repo_path)
                        })
        
        if docs:
            self.collection.insert_many(docs)
            print(f"Indexed {len(docs)} code snippets")
    
    def search(self, query: str, language: str = None, limit: int = 10) -> list:
        """Natural language code search."""
        embedding = embed_code_query(query)
        
        filter_expr = {"language": {"$eq": language}} if language else None
        
        vs_params = {
            "index": "code_vector_index",
            "path": "embedding",
            "queryVector": embedding,
            "numCandidates": 150,
            "limit": limit
        }
        if filter_expr:
            vs_params["filter"] = filter_expr
        
        return list(self.collection.aggregate([
            {"$vectorSearch": vs_params},
            {"$project": {"code": 1, "filepath": 1, "language": 1, "function_name": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
    
    def _split_code(self, content: str, filepath: str) -> list[dict]:
        """Simple code splitting by functions/classes."""
        # For production, use tree-sitter for proper AST parsing
        chunks = []
        current_chunk = ""
        
        for line in content.split("\n"):
            if line.startswith(("def ", "class ", "function ", "func ", "pub fn ")):
                if current_chunk.strip():
                    chunks.append({"code": current_chunk.strip(), "name": ""})
                current_chunk = line + "\n"
            else:
                current_chunk += line + "\n"
        
        if current_chunk.strip():
            chunks.append({"code": current_chunk.strip(), "name": ""})
        
        # If no functions found, chunk the whole file
        if not chunks:
            chunks = [{"code": content[:2000], "name": os.path.basename(filepath)}]
        
        return chunks
```

---

## Project 5: Multi-Language Document QA

Support for 100+ languages using multilingual embeddings:

```python
# multilingual_qa.py
import cohere

co = cohere.ClientV2()

def embed_multilingual(text: str, input_type: str = "search_document") -> list[float]:
    response = co.embed(
        texts=[text],
        model="embed-multilingual-v3.0",
        input_type=input_type,
        embedding_types=["float"]
    )
    return response.embeddings.float_[0]

class MultilingualQA:
    """Search in any language, get results in any language."""
    
    def search(self, query: str, target_language: str = None, limit: int = 5) -> list:
        # Multilingual embedding maps all languages to same space
        embedding = embed_multilingual(query, input_type="search_query")
        
        filter_expr = None
        if target_language:
            filter_expr = {"metadata.language": {"$eq": target_language}}
        
        vs_params = {
            "index": "multilingual_index",
            "path": "embedding",
            "queryVector": embedding,
            "numCandidates": 150,
            "limit": limit
        }
        if filter_expr:
            vs_params["filter"] = filter_expr
        
        results = list(collection.aggregate([
            {"$vectorSearch": vs_params},
            {"$project": {"content": 1, "title": 1, "metadata.language": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
        
        return results

# Example: Query in Japanese, find English documents
qa = MultilingualQA()
results = qa.search("MongoDBのベクトル検索とは何ですか？")  # "What is MongoDB vector search?" in Japanese
# Returns English, Spanish, French, etc. documents about MongoDB vector search!
```

---

## Summary

These projects demonstrate:
- **Project 1**: Full-stack search with ingestion pipeline
- **Project 2**: Conversational RAG with session management
- **Project 3**: E-commerce recommendations with user behavior
- **Project 4**: Code search with specialized embeddings
- **Project 5**: Cross-language search with multilingual models

Each can be deployed on MongoDB Atlas with the patterns from Modules 9-10.

---

## 🎓 Course Complete!

Go back to: [Course Index →](README.md)
