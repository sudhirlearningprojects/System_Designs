# LlamaIndex — Complete Data Framework for LLM Applications

A comprehensive guide to LlamaIndex: the data framework that connects LLMs to your private data through intelligent indexing, retrieval, and agentic workflows.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Core Concepts & Architecture](01_Core_Concepts.md) | Philosophy, data connectors, nodes, indices, query engine |
| 2 | [Ingestion & Indexing](02_Ingestion.md) | Readers, transformations, node parsers, embeddings, storage |
| 3 | [Retrieval & Query Engines](03_Retrieval.md) | Retrievers, response synthesis, query transformations, routing |
| 4 | [Agents & Tools](04_Agents.md) | ReAct agent, tool abstractions, function calling, workflows |
| 5 | [LlamaIndex Workflows](05_Workflows.md) | Event-driven orchestration, steps, context, streaming |
| 6 | [Advanced RAG Patterns](06_Advanced_RAG.md) | Hybrid search, reranking, recursive retrieval, knowledge graphs |
| 7 | [Evaluation & Observability](07_Evaluation.md) | Faithfulness, relevance, LlamaTrace, integration with observability tools |
| 8 | [Production & Deployment](08_Production.md) | LlamaParse, LlamaCloud, managed indices, deployment patterns |

## 🎯 LlamaIndex vs LangChain

| Aspect | LlamaIndex | LangChain |
|--------|-----------|-----------|
| **Primary focus** | Data indexing & retrieval (RAG) | Agent orchestration & chains |
| **Strength** | Best-in-class RAG, structured data | Flexible composition, tool use |
| **Indexing** | Deep (10+ index types, hierarchical) | Basic (vector store wrapper) |
| **Agents** | Workflows (event-driven) | LangGraph (graph-based) |
| **Data connectors** | 160+ (LlamaHub) | 700+ (community) |
| **When to use** | RAG-heavy apps, document QA | Multi-step agents, tool-heavy apps |
| **Can combine?** | ✅ Use LlamaIndex retriever in LangChain/LangGraph |

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      LLAMAINDEX ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    QUERY LAYER                            │    │
│  │  Query Engine │ Chat Engine │ Agents │ Workflows          │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │                  RETRIEVAL LAYER                          │    │
│  │  Retrievers │ Routers │ Rerankers │ Query Transforms     │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │                   INDEX LAYER                             │    │
│  │  VectorStoreIndex │ SummaryIndex │ KnowledgeGraphIndex   │    │
│  │  TreeIndex │ KeywordTableIndex │ ComposableGraph          │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │                 INGESTION LAYER                           │    │
│  │  Readers │ Node Parsers │ Transformations │ Embeddings    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │                   DATA LAYER                              │    │
│  │  Documents │ PDFs │ APIs │ Databases │ Web │ Notion │ ... │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Start

```python
# pip install llama-index llama-index-llms-anthropic llama-index-embeddings-openai

from llama_index.core import VectorStoreIndex, SimpleDirectoryReader, Settings
from llama_index.llms.anthropic import Anthropic
from llama_index.embeddings.openai import OpenAIEmbedding

# Configure global settings
Settings.llm = Anthropic(model="claude-sonnet-4-20250514")
Settings.embed_model = OpenAIEmbedding(model="text-embedding-3-small")

# Load documents → Index → Query (3 lines!)
documents = SimpleDirectoryReader("./data").load_data()
index = VectorStoreIndex.from_documents(documents)
response = index.as_query_engine().query("What is the refund policy?")
print(response)
```

## Package Structure (Latest)

```
llama-index-core              # Core abstractions
llama-index-llms-anthropic    # Claude integration
llama-index-llms-openai       # OpenAI integration
llama-index-embeddings-openai # Embedding models
llama-index-vector-stores-*   # Pinecone, Chroma, Weaviate, etc.
llama-index-readers-*         # Data connectors (LlamaHub)
llama-index-agent-openai      # OpenAI function calling agent
llama-index-callbacks-*       # Observability integrations
```
