# 🧠 Complete RAG (Retrieval-Augmented Generation) Course

A comprehensive, production-ready course on building RAG systems using the latest AI technologies and tools (2024-2025).

## 📚 Course Overview

This course takes you from RAG fundamentals to production-grade systems, covering the latest frameworks, embedding models, vector databases, and optimization techniques.

**Prerequisites**: Python 3.10+, basic ML/NLP knowledge, familiarity with APIs

**Duration**: ~40 hours of content across 12 modules

---

## 🗂️ Course Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | [Foundations](./01-Foundations.md) | RAG architecture, components, and when to use RAG |
| 2 | [Document Processing](./02-Document-Processing.md) | Ingestion, chunking strategies, and metadata extraction |
| 3 | [Embedding Models](./03-Embedding-Models.md) | Latest embedding models, fine-tuning, and selection |
| 4 | [Vector Databases](./04-Vector-Databases.md) | Pinecone, Weaviate, Qdrant, pgvector, Milvus comparison |
| 5 | [Retrieval Strategies](./05-Retrieval-Strategies.md) | Hybrid search, reranking, query transformation |
| 6 | [Advanced RAG Patterns](./06-Advanced-RAG-Patterns.md) | Agentic RAG, Graph RAG, Corrective RAG, Self-RAG |
| 7 | [LLM Integration](./07-LLM-Integration.md) | OpenAI, Anthropic, AWS Bedrock, local models |
| 8 | [Frameworks & Tools](./08-Frameworks-And-Tools.md) | LangChain, LlamaIndex, Haystack, Semantic Kernel |
| 9 | [Evaluation & Testing](./09-Evaluation-And-Testing.md) | RAGAS, DeepEval, LangSmith, custom metrics |
| 10 | [Production Deployment](./10-Production-Deployment.md) | Scaling, monitoring, CI/CD, cost optimization |
| 11 | [Security & Guardrails](./11-Security-And-Guardrails.md) | Prompt injection, PII handling, access control |
| 12 | [Capstone Projects](./12-Capstone-Projects.md) | End-to-end production RAG applications |

---

## 🛠️ Technology Stack (2024-2025)

| Category | Tools |
|----------|-------|
| **Frameworks** | LangChain v0.3, LlamaIndex v0.11, Haystack 2.x, Semantic Kernel |
| **Embeddings** | OpenAI text-embedding-3-large, Cohere embed-v4, Voyage AI, BGE-M3, Nomic Embed |
| **Vector DBs** | Pinecone Serverless, Weaviate, Qdrant, pgvector, ChromaDB, Milvus |
| **LLMs** | GPT-4o, Claude 3.5 Sonnet, Llama 3.1, Gemini 2.0, Mistral Large, AWS Nova |
| **Rerankers** | Cohere Rerank v3, Jina Reranker v2, BGE Reranker, FlashRank |
| **Evaluation** | RAGAS, DeepEval, LangSmith, Phoenix (Arize), TruLens |
| **Orchestration** | LangGraph, CrewAI, AutoGen, Amazon Bedrock Agents |
| **Observability** | LangSmith, Langfuse, Phoenix, Weights & Biases |
| **Deployment** | AWS Bedrock, Azure AI, GCP Vertex AI, Modal, Replicate |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        RAG Pipeline                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────┐    ┌──────────────┐    ┌────────────────┐         │
│  │  Query   │───▶│Query Transform│───▶│   Retriever    │         │
│  └──────────┘    └──────────────┘    └────────┬───────┘         │
│                                               │                   │
│  ┌──────────┐    ┌──────────────┐    ┌───────▼────────┐         │
│  │ Response │◀───│  Generator   │◀───│   Reranker     │         │
│  └──────────┘    └──────────────┘    └────────────────┘         │
│                                                                   │
├─────────────────────────────────────────────────────────────────┤
│                     Data Ingestion Pipeline                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────┐    ┌──────────────┐    ┌────────────────┐         │
│  │Documents │───▶│   Chunking   │───▶│   Embedding    │         │
│  └──────────┘    └──────────────┘    └────────┬───────┘         │
│                                               │                   │
│                                      ┌────────▼───────┐          │
│                                      │  Vector Store  │          │
│                                      └────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

```bash
# Create virtual environment
python -m venv rag-env
source rag-env/bin/activate

# Install core dependencies
pip install langchain langchain-openai langchain-community
pip install llama-index chromadb
pip install sentence-transformers
pip install ragas deepeval

# Set API keys
export OPENAI_API_KEY="<your-key>"
export ANTHROPIC_API_KEY="<your-key>"
```

**Minimal RAG in 10 lines:**
```python
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import PyPDFLoader

docs = PyPDFLoader("document.pdf").load()
chunks = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200).split_documents(docs)
vectorstore = Chroma.from_documents(chunks, OpenAIEmbeddings(model="text-embedding-3-small"))
retriever = vectorstore.as_retriever(search_kwargs={"k": 5})

# Query
results = retriever.invoke("What is the main topic?")
llm = ChatOpenAI(model="gpt-4o")
response = llm.invoke(f"Answer based on context: {results}\nQuestion: What is the main topic?")
```

---

## 🔬 Deep Dives

Advanced, production-focused deep dives on specific RAG architectures:

| Deep Dive | Stack | Description |
|-----------|-------|-------------|
| [Kafka + Flink + MongoDB](./deep-dives/README.md) | Confluent, Flink, MongoDB Atlas | Real-time streaming RAG with seconds-fresh data |

---

## 📖 How to Use This Course

1. **Sequential Learning**: Follow modules 1-12 in order for comprehensive understanding
2. **Project-Based**: Jump to Module 12 for hands-on projects after covering basics
3. **Reference Guide**: Use individual modules as reference for specific topics
4. **Hands-On**: Each module includes runnable code examples and exercises

---

## 📊 RAG vs Fine-tuning Decision Matrix

| Criteria | RAG | Fine-tuning | RAG + Fine-tuning |
|----------|-----|-------------|-------------------|
| Dynamic data | ✅ Best | ❌ Stale | ✅ Best |
| Cost | 💰 Medium | 💰💰 High | 💰💰💰 Highest |
| Latency | ~2-5s | ~0.5-1s | ~2-4s |
| Accuracy | High | Domain-specific | Highest |
| Hallucination | Low (grounded) | Medium | Lowest |
| Setup time | Hours | Days/Weeks | Weeks |
| Use case | Knowledge QA | Style/Format | Both |
