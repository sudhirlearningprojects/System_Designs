# Module 6: Embedding Generation

---

## 6.1 Choosing an Embedding Model

### Model Comparison (2025)

| Model | Provider | Dims | MTEB Score | Cost/1M tokens | Best For |
|-------|----------|------|-----------|----------------|----------|
| text-embedding-3-large | OpenAI | 3072 | 64.6 | $0.13 | Highest quality |
| text-embedding-3-small | OpenAI | 1536 | 62.3 | $0.02 | Cost-effective |
| embed-english-v3.0 | Cohere | 1024 | 64.5 | $0.10 | English text |
| embed-multilingual-v3.0 | Cohere | 1024 | 66.3 | $0.10 | Multi-language |
| voyage-3 | Voyage AI | 1024 | 67.1 | $0.06 | Code + text |
| voyage-code-3 | Voyage AI | 1024 | — | $0.06 | Code search |
| nomic-embed-text-v1.5 | Nomic | 768 | 62.0 | Free (local) | Open-source |
| mxbai-embed-large-v1 | Mixedbread | 1024 | 64.7 | Free (local) | Open-source |
| all-MiniLM-L6-v2 | SBERT | 384 | 56.3 | Free (local) | Fast, lightweight |
| jina-embeddings-v3 | Jina AI | 1024 | 65.5 | $0.02 | Long context (8K) |

### Decision Framework

```
Budget constraint?
  ├─ YES: Free/local models
  │   ├─ Speed priority → all-MiniLM-L6-v2 (384d, 14ms)
  │   ├─ Quality priority → mxbai-embed-large-v1 (1024d)
  │   └─ Multi-language → multilingual-e5-large (1024d)
  │
  └─ NO: API-based models
      ├─ General text → OpenAI text-embedding-3-small
      ├─ Highest quality → Cohere embed-v3 or Voyage-3
      ├─ Code search → Voyage-code-3
      └─ Multi-language → Cohere embed-multilingual-v3
```

---

## 6.2 Embedding with OpenAI

```python
from openai import OpenAI

client = OpenAI()

def get_openai_embedding(text: str, model="text-embedding-3-small", dimensions=None) -> list[float]:
    """Generate embedding with optional dimension reduction."""
    text = text.replace("\n", " ").strip()[:8191]  # Max 8191 tokens
    
    params = {"input": text, "model": model}
    if dimensions:
        params["dimensions"] = dimensions  # Native dim reduction (3-small/3-large only)
    
    response = client.embeddings.create(**params)
    return response.data[0].embedding

# Batch embedding (more efficient)
def get_openai_embeddings_batch(texts: list[str], model="text-embedding-3-small") -> list[list[float]]:
    """Batch embed up to 2048 texts at once."""
    texts = [t.replace("\n", " ").strip()[:8191] for t in texts]
    response = client.embeddings.create(input=texts, model=model)
    return [item.embedding for item in sorted(response.data, key=lambda x: x.index)]

# Usage with dimension reduction
embedding_512 = get_openai_embedding("Hello world", dimensions=512)  # 512 dims instead of 1536
```

---

## 6.3 Embedding with Cohere

```python
import cohere

co = cohere.ClientV2(api_key="...")

def get_cohere_embedding(text: str, input_type="search_document") -> list[float]:
    """
    input_type options:
      - "search_document" → for documents being indexed
      - "search_query"    → for search queries
      - "classification"  → for classification tasks
      - "clustering"      → for clustering tasks
    """
    response = co.embed(
        texts=[text],
        model="embed-english-v3.0",
        input_type=input_type,
        embedding_types=["float"]
    )
    return response.embeddings.float_[0]

# IMPORTANT: Use different input_type for indexing vs querying!
doc_embedding = get_cohere_embedding("MongoDB is a document database", input_type="search_document")
query_embedding = get_cohere_embedding("What is MongoDB?", input_type="search_query")
```

---

## 6.4 Embedding with Voyage AI

```python
import voyageai

vo = voyageai.Client(api_key="...")

def get_voyage_embedding(text: str, input_type="document") -> list[float]:
    """input_type: 'document' for indexing, 'query' for searching."""
    result = vo.embed([text], model="voyage-3", input_type=input_type)
    return result.embeddings[0]

# For code
def get_code_embedding(code: str, input_type="document") -> list[float]:
    result = vo.embed([code], model="voyage-code-3", input_type=input_type)
    return result.embeddings[0]
```

---

## 6.5 Local Embedding Models (No API Cost)

### Using Sentence Transformers

```python
from sentence_transformers import SentenceTransformer

# Load once, reuse
model = SentenceTransformer("nomic-ai/nomic-embed-text-v1.5", trust_remote_code=True)

def get_local_embedding(text: str) -> list[float]:
    # Nomic requires prefix for different tasks
    embedding = model.encode(f"search_document: {text}", normalize_embeddings=True)
    return embedding.tolist()

def get_local_query_embedding(text: str) -> list[float]:
    embedding = model.encode(f"search_query: {text}", normalize_embeddings=True)
    return embedding.tolist()

# Batch embedding (GPU-accelerated)
def batch_embed(texts: list[str], batch_size=64) -> list[list[float]]:
    prefixed = [f"search_document: {t}" for t in texts]
    embeddings = model.encode(prefixed, batch_size=batch_size, normalize_embeddings=True)
    return embeddings.tolist()
```

### Using Ollama (Local LLM inference)

```python
import requests

def get_ollama_embedding(text: str, model="nomic-embed-text") -> list[float]:
    response = requests.post("http://localhost:11434/api/embeddings", json={
        "model": model,
        "prompt": text
    })
    return response.json()["embedding"]
```

---

## 6.6 Chunking Strategies

Large documents must be split into chunks before embedding (most models have 512-8192 token limits).

### Strategy 1: Fixed-Size Chunking

```python
def fixed_size_chunks(text: str, chunk_size: int = 500, overlap: int = 100) -> list[str]:
    """Split text into fixed-size character chunks with overlap."""
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunks.append(text[start:end])
        start = end - overlap
    return chunks
```

### Strategy 2: Semantic Chunking (Recommended)

```python
from langchain_text_splitters import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=200,
    separators=["\n\n", "\n", ". ", " ", ""],
    length_function=len
)

def semantic_chunks(text: str) -> list[str]:
    return splitter.split_text(text)
```

### Strategy 3: Paragraph-Based

```python
def paragraph_chunks(text: str, max_tokens: int = 500) -> list[str]:
    """Split by paragraphs, merge small ones."""
    paragraphs = text.split("\n\n")
    chunks = []
    current = ""
    
    for para in paragraphs:
        if len(current) + len(para) < max_tokens * 4:  # ~4 chars per token
            current += "\n\n" + para if current else para
        else:
            if current:
                chunks.append(current.strip())
            current = para
    
    if current:
        chunks.append(current.strip())
    return chunks
```

### Strategy 4: Document-Aware Chunking

```python
def document_aware_chunks(doc: dict) -> list[dict]:
    """Create chunks that preserve document context."""
    chunks = []
    content_chunks = semantic_chunks(doc["content"])
    
    for i, chunk in enumerate(content_chunks):
        chunks.append({
            "content": chunk,
            "title": doc["title"],
            "chunk_index": i,
            "total_chunks": len(content_chunks),
            # Contextual header improves retrieval quality
            "text_for_embedding": f"Title: {doc['title']}\n\n{chunk}",
            "metadata": {
                "source_id": doc["_id"],
                "section": f"chunk_{i+1}_of_{len(content_chunks)}"
            }
        })
    return chunks
```

### Chunking Size Guidelines

| Embedding Model | Max Tokens | Recommended Chunk | Overlap |
|-----------------|-----------|-------------------|---------|
| OpenAI text-embedding-3-* | 8191 | 500-1000 tokens | 100-200 |
| Cohere embed-v3 | 512 | 300-450 tokens | 50-100 |
| Voyage-3 | 32000 | 500-2000 tokens | 100-300 |
| nomic-embed-text | 8192 | 500-1000 tokens | 100-200 |
| all-MiniLM-L6 | 256 | 150-230 tokens | 30-50 |

---

## 6.7 Ingestion Pipeline

Complete pipeline from raw documents to searchable vectors:

```python
import hashlib
from datetime import datetime, timezone

def ingest_documents(documents: list[dict], batch_size: int = 100):
    """Full ingestion pipeline: chunk → embed → store."""
    
    all_chunks = []
    for doc in documents:
        chunks = document_aware_chunks(doc)
        all_chunks.extend(chunks)
    
    # Batch embed
    for i in range(0, len(all_chunks), batch_size):
        batch = all_chunks[i:i + batch_size]
        texts = [c["text_for_embedding"] for c in batch]
        
        # Get embeddings in batch
        embeddings = get_openai_embeddings_batch(texts)
        
        # Prepare MongoDB documents
        mongo_docs = []
        for chunk, embedding in zip(batch, embeddings):
            mongo_docs.append({
                "content": chunk["content"],
                "title": chunk["title"],
                "embedding": embedding,
                "chunk_index": chunk["chunk_index"],
                "total_chunks": chunk["total_chunks"],
                "metadata": chunk["metadata"],
                "content_hash": hashlib.sha256(chunk["content"].encode()).hexdigest(),
                "ingested_at": datetime.now(timezone.utc)
            })
        
        # Upsert (avoid duplicates)
        operations = []
        for doc in mongo_docs:
            operations.append(
                UpdateOne(
                    {"content_hash": doc["content_hash"]},
                    {"$set": doc},
                    upsert=True
                )
            )
        
        collection.bulk_write(operations)
        print(f"Ingested batch {i//batch_size + 1}: {len(batch)} chunks")
    
    print(f"✅ Total ingested: {len(all_chunks)} chunks from {len(documents)} documents")
```

---

## 6.8 Embedding Best Practices

1. **Same model for indexing and querying** — Never mix models
2. **Use instruction prefixes** — Models like Nomic, E5 require "search_document:" / "search_query:" prefixes
3. **Normalize embeddings** — For cosine similarity, ensure unit vectors
4. **Batch operations** — Always batch embed (50-2048 texts per call)
5. **Cache embeddings** — Store embeddings in MongoDB, don't re-compute
6. **Chunk size matters** — Too large = diluted semantics. Too small = lost context
7. **Add contextual headers** — Prepend title/section to chunks before embedding
8. **Handle empty/short text** — Skip or pad very short texts (< 10 chars)
9. **Rate limit awareness** — OpenAI: 3000 RPM, Cohere: 100 RPM free tier
10. **Version your embeddings** — Store model name/version alongside vectors for future migration

---

## Next: [Module 7 — RAG Architecture →](07_RAG_Architecture.md)
