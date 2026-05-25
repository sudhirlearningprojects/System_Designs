# 8. Production & Deployment

## Theory: RAG in Production

Moving from prototype to production requires addressing:

```
PROTOTYPE                          PRODUCTION
─────────                          ──────────
In-memory vector store      →      Managed vector DB (Pinecone, Weaviate)
SimpleDirectoryReader       →      LlamaParse (complex document parsing)
Single index                →      Multi-index with routing
No caching                  →      Redis/semantic cache
No evaluation               →      Continuous eval pipeline
Local execution             →      Containerized, auto-scaling
No monitoring               →      Full observability stack
```

---

## LlamaParse (Document Parsing)

LlamaParse is Anthropic-powered document parsing that handles complex layouts (tables, figures, multi-column PDFs).

```python
from llama_parse import LlamaParse

# Parse complex documents (tables, figures, multi-column)
parser = LlamaParse(
    api_key="llx-...",
    result_type="markdown",           # or "text"
    num_workers=4,                    # Parallel parsing
    verbose=True,
    language="en",
    # Advanced options
    parsing_instruction="Extract all tables as markdown tables. Preserve headers.",
)

# Parse files
documents = parser.load_data(["./complex_report.pdf", "./financial_statement.pdf"])

# Use with ingestion pipeline
from llama_index.core import VectorStoreIndex
index = VectorStoreIndex.from_documents(documents)
```

**Why LlamaParse over PyPDF?**
- Tables: Preserves table structure (PyPDF loses it)
- Figures: Extracts figure captions and descriptions
- Multi-column: Correctly reads column order
- Headers/footers: Removes noise
- OCR: Handles scanned documents

---

## LlamaCloud (Managed RAG)

```python
from llama_index.indices.managed.llama_cloud import LlamaCloudIndex

# Create managed index (no infrastructure to manage)
index = LlamaCloudIndex.from_documents(
    documents,
    name="my-production-index",
    project_name="customer-support",
)

# Query (same API as local index)
query_engine = index.as_query_engine()
response = query_engine.query("What is the refund policy?")

# Benefits:
# - Managed vector store (no Pinecone/Chroma to manage)
# - Automatic re-indexing on document updates
# - Built-in evaluation and monitoring
# - Optimized retrieval (auto-tuned parameters)
```

---

## Production Deployment Pattern

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from llama_index.core import VectorStoreIndex, StorageContext, load_index_from_storage
from llama_index.vector_stores.pinecone import PineconeVectorStore
from llama_index.llms.anthropic import Anthropic
from llama_index.core import Settings
import redis

app = FastAPI()

# Global configuration
Settings.llm = Anthropic(model="claude-sonnet-4-20250514", temperature=0, max_retries=3)
Settings.embed_model = OpenAIEmbedding(model="text-embedding-3-small")

# Initialize index (once at startup)
vector_store = PineconeVectorStore(pinecone_index=pinecone_index)
storage_context = StorageContext.from_defaults(vector_store=vector_store)
index = load_index_from_storage(storage_context)

# Cache
cache = redis.Redis(host="localhost", port=6379)

class QueryRequest(BaseModel):
    query: str
    user_id: str
    filters: dict = {}

class QueryResponse(BaseModel):
    answer: str
    sources: list[dict]
    cached: bool

@app.post("/query", response_model=QueryResponse)
async def query_endpoint(request: QueryRequest):
    # Check cache
    cache_key = f"rag:{hash(request.query)}"
    cached = cache.get(cache_key)
    if cached:
        return QueryResponse(**json.loads(cached), cached=True)
    
    # Build query engine with filters
    query_engine = index.as_query_engine(
        similarity_top_k=5,
        filters=MetadataFilters.from_dict(request.filters) if request.filters else None,
        response_mode="compact",
    )
    
    # Query
    response = await query_engine.aquery(request.query)
    
    result = QueryResponse(
        answer=response.response,
        sources=[{"text": n.text[:200], "score": n.score, "source": n.metadata.get("source")} 
                 for n in response.source_nodes],
        cached=False,
    )
    
    # Cache for 1 hour
    cache.setex(cache_key, 3600, json.dumps(result.dict()))
    
    return result

@app.get("/health")
async def health():
    return {"status": "healthy", "index_size": len(index.docstore.docs)}
```

---

## Docker Deployment

```dockerfile
FROM python:3.11-slim
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

---

## Production Checklist

- [ ] External vector store (Pinecone/Weaviate, not in-memory)
- [ ] LlamaParse for complex documents (PDFs with tables)
- [ ] Response caching (Redis) for repeated queries
- [ ] Evaluation pipeline running on every deployment
- [ ] Observability (LlamaTrace/Phoenix/LangSmith)
- [ ] Rate limiting on query endpoint
- [ ] Error handling with fallback responses
- [ ] Document update pipeline (re-index on changes)
- [ ] Metadata filtering for multi-tenant isolation
- [ ] Health check endpoint
- [ ] Structured logging (JSON)
- [ ] Cost tracking (tokens per query)
