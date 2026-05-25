# 1. Core Concepts & Architecture

## Theory: The LlamaIndex Philosophy

### The Data Problem for LLMs

LLMs are powerful but have a fundamental limitation: they can only reason over data in their context window. Your private data (documents, databases, APIs) lives outside this window.

```
┌─────────────────────────────────────────────────────────────┐
│  THE GAP                                                     │
│                                                              │
│  Your Data                    LLM Context Window             │
│  ┌──────────────────┐        ┌──────────────────┐          │
│  │ 10,000 documents │        │ ~200K tokens     │          │
│  │ 500 GB of PDFs   │   ≫    │ (~150K words)    │          │
│  │ 50 databases     │        │                  │          │
│  │ 100 APIs         │        │ Can't fit it all!│          │
│  └──────────────────┘        └──────────────────┘          │
│                                                              │
│  LlamaIndex bridges this gap through INTELLIGENT RETRIEVAL  │
└─────────────────────────────────────────────────────────────┘
```

### LlamaIndex's Approach

LlamaIndex solves this with a **data framework** that:
1. **Ingests** data from any source (160+ connectors)
2. **Structures** data into optimized representations (nodes, indices)
3. **Retrieves** the most relevant pieces for any query
4. **Synthesizes** answers using LLMs grounded in your data

### Key Insight: Indices as Data Structures

Just as databases use B-trees and hash tables for efficient lookup, LlamaIndex uses **indices** optimized for LLM queries:

| Index Type | Data Structure | Best For | How It Works |
|-----------|---------------|----------|--------------|
| **VectorStoreIndex** | Embedding vectors | Semantic search | Embed chunks → cosine similarity search |
| **SummaryIndex** | Sequential list | Summarization | Feed all nodes to LLM (or summarize in tree) |
| **TreeIndex** | Hierarchical tree | Multi-level summarization | Bottom-up summarization, top-down query |
| **KeywordTableIndex** | Keyword → node map | Keyword-based lookup | Extract keywords, match to query keywords |
| **KnowledgeGraphIndex** | Triplet graph | Relationship queries | Extract (subject, predicate, object) triplets |
| **ComposableGraph** | Graph of indices | Multi-document QA | Route queries to appropriate sub-index |

---

## Core Abstractions

### Documents and Nodes

```
Document: Raw input (a PDF, web page, database row)
    │
    ▼ (Node Parser / Transformation)
    │
Node: Atomic unit of data in LlamaIndex
    ├── text: The content
    ├── metadata: Source, page number, section, etc.
    ├── relationships: Parent, child, next, previous nodes
    └── embedding: Vector representation (optional, computed on index)
```

**Why Nodes, not just chunks?**
- Nodes maintain **relationships** (parent document, sibling chunks)
- Nodes carry **metadata** (enables filtering)
- Nodes are **referenceable** (citations, source tracking)
- Nodes can be **hierarchically organized** (sentence → paragraph → section → document)

```python
from llama_index.core import Document
from llama_index.core.node_parser import SentenceSplitter

# Document
doc = Document(
    text="LlamaIndex is a data framework for LLM applications...",
    metadata={"source": "docs.md", "category": "overview", "date": "2024-01-15"},
)

# Parse into nodes
parser = SentenceSplitter(chunk_size=1024, chunk_overlap=200)
nodes = parser.get_nodes_from_documents([doc])

# Each node has:
print(nodes[0].text)                    # Content
print(nodes[0].metadata)               # Inherited + added metadata
print(nodes[0].relationships)          # Links to other nodes
print(nodes[0].node_id)                # Unique identifier
```

### The Query Pipeline

```
User Query
    │
    ▼
┌─────────────────────┐
│  Query Transform    │  (Optional: decompose, HyDE, step-back)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Retriever          │  (Find relevant nodes from index)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Node Postprocessor │  (Rerank, filter, deduplicate)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  Response Synthesizer│  (Generate answer from nodes + query)
└─────────┬───────────┘
          │
          ▼
    Response (with source citations)
```

### Response Synthesis Strategies

| Strategy | How It Works | Best For |
|----------|-------------|----------|
| **Compact** | Stuff all nodes into one prompt | Short contexts (<4K tokens) |
| **Refine** | Iterate: answer with node 1, refine with node 2, ... | Long contexts, detailed answers |
| **Tree Summarize** | Summarize nodes in tree structure, combine | Many nodes, summarization |
| **Simple** | Concatenate node texts (no LLM synthesis) | When you just want raw text |
| **Accumulate** | Generate answer per node, then combine | When each source needs separate treatment |

---

## Settings (Global Configuration)

```python
from llama_index.core import Settings
from llama_index.llms.anthropic import Anthropic
from llama_index.embeddings.openai import OpenAIEmbedding

# Global settings (apply to all operations)
Settings.llm = Anthropic(model="claude-sonnet-4-20250514", temperature=0)
Settings.embed_model = OpenAIEmbedding(model="text-embedding-3-small")
Settings.chunk_size = 1024
Settings.chunk_overlap = 200
Settings.num_output = 512  # Max output tokens

# Or configure per-component (overrides global)
index = VectorStoreIndex.from_documents(documents, embed_model=custom_embed)
query_engine = index.as_query_engine(llm=different_llm)
```

---

## Index Types — Deep Dive

### VectorStoreIndex (Most Common)

```python
from llama_index.core import VectorStoreIndex, StorageContext
from llama_index.vector_stores.pinecone import PineconeVectorStore

# In-memory (development)
index = VectorStoreIndex.from_documents(documents)

# With external vector store (production)
vector_store = PineconeVectorStore(pinecone_index=pinecone_index)
storage_context = StorageContext.from_defaults(vector_store=vector_store)
index = VectorStoreIndex.from_documents(documents, storage_context=storage_context)

# Query
query_engine = index.as_query_engine(
    similarity_top_k=5,
    response_mode="compact",
)
response = query_engine.query("What is the cancellation policy?")
print(response.response)           # Generated answer
print(response.source_nodes)       # Retrieved nodes with scores
```

### SummaryIndex (Full Context)

```python
from llama_index.core import SummaryIndex

# Useful when you want to process ALL documents (not just top-k)
index = SummaryIndex.from_documents(documents)

# Query processes all nodes (good for summarization)
query_engine = index.as_query_engine(response_mode="tree_summarize")
response = query_engine.query("Summarize the key points of this document")
```

### ComposableGraph (Multi-Index)

```python
from llama_index.core.composability import ComposableGraph

# Create separate indices for different document types
billing_index = VectorStoreIndex.from_documents(billing_docs)
technical_index = VectorStoreIndex.from_documents(technical_docs)
policy_index = VectorStoreIndex.from_documents(policy_docs)

# Compose into a graph with summaries
graph = ComposableGraph.from_indices(
    SummaryIndex,  # Root index type
    [billing_index, technical_index, policy_index],
    index_summaries=[
        "Billing and payment information",
        "Technical documentation and troubleshooting",
        "Company policies and procedures",
    ],
)

# Query routes to appropriate sub-index
query_engine = graph.as_query_engine()
response = query_engine.query("How do I get a refund?")  # Routes to billing_index
```

---

## Chat Engine (Multi-Turn)

```python
from llama_index.core.chat_engine import CondensePlusContextChatEngine

# Chat engine maintains conversation history
chat_engine = index.as_chat_engine(
    chat_mode="condense_plus_context",  # Reformulates query with history
    verbose=True,
)

# Multi-turn conversation
response1 = chat_engine.chat("What products do you offer?")
response2 = chat_engine.chat("How much does the Pro plan cost?")  # Understands context
response3 = chat_engine.chat("Can I get a discount?")  # Remembers previous messages

# Reset conversation
chat_engine.reset()
```

### Chat Modes

| Mode | How It Works | Best For |
|------|-------------|----------|
| `simple` | Append history to prompt | Short conversations |
| `condense_question` | LLM reformulates query using history | Ambiguous follow-ups |
| `condense_plus_context` | Reformulate + retrieve fresh context | Production (recommended) |
| `context` | Always retrieve, include history | When context changes often |
| `react` | Full ReAct agent with tools | Complex multi-step queries |

---

## Persistence

```python
from llama_index.core import StorageContext, load_index_from_storage

# Save index to disk
index.storage_context.persist(persist_dir="./storage")

# Load from disk (no re-embedding needed!)
storage_context = StorageContext.from_defaults(persist_dir="./storage")
index = load_index_from_storage(storage_context)
```

---

## Next: [Ingestion & Indexing →](02_Ingestion.md)
