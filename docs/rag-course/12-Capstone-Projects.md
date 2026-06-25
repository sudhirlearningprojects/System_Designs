# Module 12: Capstone Projects

## Overview

Apply everything you've learned by building production-grade RAG applications end-to-end.

---

## Project 1: Enterprise Knowledge Base Assistant

Build a multi-tenant knowledge base chatbot for an enterprise with access control.

### Requirements
- Ingest documents from multiple sources (Confluence, S3, Google Drive)
- Multi-tenant isolation with role-based access control
- Conversation memory (multi-turn chat)
- Citation with source links
- Admin dashboard for analytics

### Architecture
```
User → API Gateway (Auth) → RAG Service → Vector DB (Qdrant)
                                        → LLM (GPT-4o / Claude)
                                        → Redis (Cache + Sessions)
Admin → Dashboard → Metrics (Prometheus) + Ingestion Queue (Celery)
```

### Implementation Skeleton

```python
# main.py - FastAPI application
from fastapi import FastAPI, Depends
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.rag = EnterpriseRAG()
    await app.state.rag.initialize()
    yield

app = FastAPI(lifespan=lifespan)

class EnterpriseRAG:
    def __init__(self):
        self.vectorstore = QdrantVectorStore()
        self.llm = LLMRouter()  # Routes to cheap/expensive model
        self.reranker = CohereRerank()
        self.cache = SemanticCache()
        self.guardrails = GuardrailPipeline()
        self.memory = ConversationMemory()
    
    async def chat(self, user: User, message: str, session_id: str) -> dict:
        # 1. Input validation + injection detection
        safe_input = await self.guardrails.validate_input(message)
        
        # 2. Check cache
        cached = await self.cache.get(message, user.tenant_id)
        if cached:
            return cached
        
        # 3. Get conversation history
        history = await self.memory.get_history(session_id, last_n=5)
        
        # 4. Contextualize query with history
        standalone_query = await self._contextualize(message, history)
        
        # 5. Retrieve with access control
        docs = await self.vectorstore.search(
            query=standalone_query,
            filter={"tenant_id": user.tenant_id, "access_level": {"$in": user.roles}},
            k=10,
        )
        
        # 6. Rerank
        reranked = await self.reranker.rerank(standalone_query, docs, top_n=5)
        
        # 7. Generate with citations
        response = await self.llm.generate(
            query=message,
            context=reranked,
            history=history,
            system_prompt=ENTERPRISE_SYSTEM_PROMPT,
        )
        
        # 8. Output guardrails
        safe_output = await self.guardrails.validate_output(response, reranked)
        
        # 9. Save to memory + cache
        await self.memory.add(session_id, message, safe_output["answer"])
        await self.cache.set(message, safe_output, user.tenant_id)
        
        return safe_output
```

### Key Features to Implement
- [ ] Multi-source ingestion pipeline (S3, Confluence, web)
- [ ] Tenant-isolated vector storage
- [ ] Conversation memory with Redis
- [ ] Streaming responses via WebSocket
- [ ] Admin: ingestion status, query analytics, cost tracking
- [ ] Automatic document re-indexing on source changes

---

## Project 2: Code Assistant RAG

Build a RAG system for answering questions about a codebase.

### Requirements
- Index entire Git repositories (code + docs + comments)
- Understand code structure (functions, classes, imports)
- Answer "how does X work?" and "where is Y implemented?" questions
- Support multiple languages (Python, Java, TypeScript)

### Architecture
```
Git Repo → Code Parser → AST Chunking → Embeddings → Vector DB
                                                          ↓
User Query → Query Classifier → [Code Search | Doc Search | Hybrid]
                                                          ↓
                                    Rerank → LLM (code-specialized) → Response
```

### Implementation

```python
from tree_sitter_languages import get_parser
from langchain.text_splitter import Language, RecursiveCharacterTextSplitter

class CodeRAG:
    def __init__(self):
        self.code_splitter = RecursiveCharacterTextSplitter.from_language(
            language=Language.PYTHON, chunk_size=2000, chunk_overlap=200
        )
        self.embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
        self.vectorstore = Qdrant()
        self.llm = ChatOpenAI(model="gpt-4o")  # Best for code understanding
    
    def index_repository(self, repo_path: str):
        """Index a code repository with structure-aware chunking."""
        for file_path in glob(f"{repo_path}/**/*.py", recursive=True):
            content = open(file_path).read()
            
            # Parse AST for function/class boundaries
            chunks = self.code_splitter.split_text(content)
            
            documents = []
            for chunk in chunks:
                documents.append(Document(
                    page_content=chunk,
                    metadata={
                        "file_path": file_path,
                        "language": "python",
                        "type": self._detect_type(chunk),  # function, class, module
                        "imports": self._extract_imports(chunk),
                    }
                ))
            
            self.vectorstore.add_documents(documents)
    
    def query(self, question: str) -> str:
        # Classify: is this about code location, behavior, or architecture?
        query_type = self._classify_query(question)
        
        if query_type == "location":
            # Search by function/class name in metadata
            docs = self.vectorstore.similarity_search(
                question, k=5, filter={"type": {"$in": ["function", "class"]}}
            )
        elif query_type == "architecture":
            # Broader search across module-level docs
            docs = self.vectorstore.similarity_search(question, k=10)
        else:
            # Hybrid search
            docs = self.vectorstore.similarity_search(question, k=8)
        
        return self._generate_code_answer(question, docs)
```

---

## Project 3: Multi-Modal RAG (Documents with Images & Tables)

Build RAG over complex documents containing text, tables, charts, and diagrams.

### Requirements
- Process PDFs with embedded tables and charts
- Extract and describe images using vision models
- Table-aware retrieval (query specific cells/rows)
- Generate answers referencing specific figures/tables

### Implementation

```python
from unstructured.partition.pdf import partition_pdf
from langchain_openai import ChatOpenAI

class MultiModalRAG:
    def __init__(self):
        self.vision_llm = ChatOpenAI(model="gpt-4o")
        self.text_llm = ChatOpenAI(model="gpt-4o")
        self.vectorstore = Chroma()
    
    def ingest_document(self, pdf_path: str):
        """Process PDF preserving tables and images."""
        elements = partition_pdf(
            pdf_path, 
            strategy="hi_res",
            extract_images_in_pdf=True,
            infer_table_structure=True,
        )
        
        for element in elements:
            if element.category == "Table":
                # Store table as markdown + generate description
                table_md = element.metadata.text_as_html
                description = self._describe_table(table_md)
                self._store(description, {"type": "table", "content": table_md})
                
            elif element.category == "Image":
                # Describe image with vision model
                description = self._describe_image(element.metadata.image_path)
                self._store(description, {"type": "image", "path": element.metadata.image_path})
                
            else:
                self._store(element.text, {"type": "text"})
    
    def _describe_image(self, image_path: str) -> str:
        import base64
        with open(image_path, "rb") as f:
            b64 = base64.b64encode(f.read()).decode()
        
        response = self.vision_llm.invoke([
            {"type": "text", "text": "Describe this image/chart in detail for search indexing:"},
            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{b64}"}},
        ])
        return response.content
    
    def query(self, question: str) -> str:
        docs = self.vectorstore.similarity_search(question, k=5)
        
        # Include original table HTML or image reference in context
        context_parts = []
        for doc in docs:
            if doc.metadata.get("type") == "table":
                context_parts.append(f"[TABLE]\n{doc.metadata['content']}\n[/TABLE]")
            elif doc.metadata.get("type") == "image":
                context_parts.append(f"[FIGURE: {doc.page_content}]")
            else:
                context_parts.append(doc.page_content)
        
        context = "\n\n".join(context_parts)
        return self.text_llm.invoke(
            f"Answer using the context (including tables and figures):\n{context}\n\nQ: {question}"
        ).content
```

---

## Project 4: Conversational RAG with Tool Use

Build a chat assistant that can query databases, search the web, and access internal APIs alongside document retrieval.

### Implementation with LangGraph

```python
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode
from langchain_core.tools import tool

@tool
def search_documentation(query: str) -> str:
    """Search internal documentation."""
    docs = vectorstore.similarity_search(query, k=5)
    return "\n".join([d.page_content for d in docs])

@tool
def query_database(sql: str) -> str:
    """Execute SQL query on the analytics database."""
    # Validate SQL (prevent injection)
    if not is_safe_sql(sql):
        return "Query rejected: only SELECT statements allowed"
    result = db.execute(sql)
    return json.dumps(result[:20])

@tool
def search_web(query: str) -> str:
    """Search the web for current information."""
    from langchain_community.tools import TavilySearchResults
    return TavilySearchResults(max_results=3).invoke(query)

@tool
def get_user_profile(user_id: str) -> str:
    """Get user profile information from CRM."""
    profile = crm_api.get_user(user_id)
    return json.dumps(profile)

# Agent with tools
tools = [search_documentation, query_database, search_web, get_user_profile]
llm_with_tools = ChatOpenAI(model="gpt-4o").bind_tools(tools)

# Build LangGraph agent
from langgraph.prebuilt import create_react_agent

agent = create_react_agent(llm_with_tools, tools)

# Use with conversation memory
from langgraph.checkpoint.memory import MemorySaver
memory = MemorySaver()

app = agent.compile(checkpointer=memory)
config = {"configurable": {"thread_id": "session_123"}}

# Multi-turn conversation
result = app.invoke({"messages": [("user", "How many active users do we have?")]}, config)
result = app.invoke({"messages": [("user", "Compare that with last month")]}, config)
```

---

## Project 5: Real-Time RAG with Live Data

Build RAG over continuously updating data sources (Slack, emails, tickets).

### Architecture
```
Data Sources → Kafka → Ingestion Workers → Vector DB
                                               ↓
User Query → RAG Service (with freshness awareness) → Response
```

### Implementation
```python
from kafka import KafkaConsumer
import asyncio

class RealTimeIngestionWorker:
    """Continuously ingest new data from streaming sources."""
    
    def __init__(self, vectorstore, embeddings):
        self.vectorstore = vectorstore
        self.embeddings = embeddings
        self.consumer = KafkaConsumer(
            "documents-stream",
            bootstrap_servers="localhost:9092",
            group_id="rag-ingestion",
        )
    
    async def run(self):
        for message in self.consumer:
            event = json.loads(message.value)
            
            if event["type"] == "new_document":
                await self._ingest(event)
            elif event["type"] == "document_updated":
                await self._update(event)
            elif event["type"] == "document_deleted":
                await self._delete(event)
    
    async def _ingest(self, event: dict):
        chunks = chunk_document(event["content"])
        for chunk in chunks:
            chunk.metadata["ingested_at"] = datetime.utcnow().isoformat()
            chunk.metadata["source_id"] = event["id"]
        await self.vectorstore.aadd_documents(chunks)

class FreshnessAwareRetriever:
    """Boost recently updated documents in retrieval."""
    
    def retrieve(self, query: str, k: int = 5, max_age_hours: int = 24) -> list:
        # Retrieve with time-based boosting
        results = self.vectorstore.similarity_search(query, k=k*2)
        
        # Score: relevance * freshness_boost
        scored = []
        now = datetime.utcnow()
        for doc in results:
            age_hours = (now - parse(doc.metadata["ingested_at"])).total_seconds() / 3600
            freshness_boost = 1.0 + max(0, (max_age_hours - age_hours) / max_age_hours)
            scored.append((doc, freshness_boost))
        
        scored.sort(key=lambda x: x[1], reverse=True)
        return [doc for doc, _ in scored[:k]]
```

---

## Evaluation Criteria

For each capstone project, evaluate:

| Criterion | Weight | Target |
|-----------|--------|--------|
| Retrieval Quality (RAGAS metrics) | 25% | >0.85 |
| Answer Quality (Faithfulness) | 25% | >0.90 |
| Latency (P95) | 15% | <3s |
| Cost per query | 10% | <$0.02 |
| Security (injection resistance) | 15% | Pass all tests |
| Code quality & architecture | 10% | Clean, testable |

---

## Submission Checklist

- [ ] Working code with README and setup instructions
- [ ] Docker Compose for local development
- [ ] Evaluation results on 50+ test questions
- [ ] Performance benchmarks (latency, throughput)
- [ ] Security test results (injection attempts)
- [ ] Cost analysis per query
- [ ] Architecture diagram
- [ ] Monitoring setup (at minimum, structured logging)
