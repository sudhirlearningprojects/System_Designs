# 6. Advanced RAG Patterns

## Theory: Why Basic RAG Fails

Basic RAG (embed → retrieve top-K → generate) has fundamental limitations:

```
┌─────────────────────────────────────────────────────────────┐
│  BASIC RAG FAILURE MODES                                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. RETRIEVAL FAILURES                                       │
│     • Query-document vocabulary mismatch                    │
│     • Important info buried in low-ranked chunks            │
│     • Chunks too small (missing context) or too large       │
│     • Metadata not leveraged for filtering                  │
│                                                              │
│  2. CONTEXT FAILURES                                         │
│     • Retrieved chunks are relevant but redundant           │
│     • Missing relationships between chunks                  │
│     • Context window overflow (too many chunks)             │
│     • Wrong granularity (need summary, got details)         │
│                                                              │
│  3. GENERATION FAILURES                                      │
│     • LLM ignores context and hallucinates                  │
│     • LLM can't synthesize across multiple chunks           │
│     • Answer is correct but doesn't address the question    │
│     • Lost in the middle (LLM ignores middle of context)    │
└─────────────────────────────────────────────────────────────┘
```

### Advanced RAG Strategies

```
PRE-RETRIEVAL (improve the query)
├── Query Decomposition: Break complex query into sub-queries
├── HyDE: Generate hypothetical answer, embed THAT instead
├── Step-Back Prompting: Ask a more general question first
└── Query Expansion: Add synonyms and related terms

RETRIEVAL (improve what's found)
├── Hybrid Search: Combine semantic + keyword (BM25)
├── Recursive Retrieval: Retrieve summaries → drill into details
├── Auto-Merging: Small chunks retrieved → return parent chunk
├── Metadata Filtering: Use structured filters + semantic search
└── Multi-Index: Route to specialized indices per topic

POST-RETRIEVAL (improve what's used)
├── Reranking: Re-score retrieved chunks with cross-encoder
├── Diversity: MMR to avoid redundant chunks
├── Compression: Summarize chunks to fit more in context
└── Relevance Filtering: Drop chunks below score threshold

GENERATION (improve the answer)
├── Citation: Force model to cite specific chunks
├── Refine: Iteratively improve answer with each chunk
├── Tree Summarize: Hierarchical synthesis for many chunks
└── Verification: Check answer against retrieved sources
```

---

## Hybrid Search (BM25 + Semantic)

```python
from llama_index.core import VectorStoreIndex
from llama_index.retrievers.bm25 import BM25Retriever
from llama_index.core.retrievers import QueryFusionRetriever

# Semantic retriever
vector_retriever = index.as_retriever(similarity_top_k=5)

# Keyword retriever (BM25)
bm25_retriever = BM25Retriever.from_defaults(
    nodes=nodes,  # Same nodes as vector index
    similarity_top_k=5,
)

# Fusion: combine results with Reciprocal Rank Fusion
hybrid_retriever = QueryFusionRetriever(
    retrievers=[vector_retriever, bm25_retriever],
    mode="reciprocal_rerank",  # RRF scoring
    similarity_top_k=5,
    num_queries=1,  # Don't generate sub-queries
)

# Use in query engine
from llama_index.core.query_engine import RetrieverQueryEngine
query_engine = RetrieverQueryEngine.from_args(hybrid_retriever)
response = query_engine.query("How to configure SSL certificates?")
```

**Why Hybrid?**
- Semantic search: Great for meaning ("How do I fix login issues?" → finds "authentication troubleshooting")
- BM25: Great for exact terms ("error code E-1234" → finds exact match)
- Combined: Best of both worlds

---

## Reranking

```python
from llama_index.postprocessor.cohere_rerank import CohereRerank
from llama_index.core.postprocessor import SentenceTransformerRerank

# Cohere Reranker (API-based, high quality)
reranker = CohereRerank(top_n=3, model="rerank-english-v3.0")

# Local reranker (no API call, faster)
reranker = SentenceTransformerRerank(
    model="cross-encoder/ms-marco-MiniLM-L-6-v2",
    top_n=3,
)

# Apply in query engine
query_engine = index.as_query_engine(
    similarity_top_k=10,  # Retrieve more initially
    node_postprocessors=[reranker],  # Then rerank to top 3
)
```

**Theory: Why Reranking Works**
- Bi-encoder (embedding search): Encodes query and doc separately → fast but less accurate
- Cross-encoder (reranker): Encodes query+doc together → slow but much more accurate
- Strategy: Use bi-encoder for recall (top-20), cross-encoder for precision (top-3)

---

## Recursive Retrieval (Small-to-Big)

```python
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.retrievers import RecursiveRetriever
from llama_index.core import SummaryIndex

# Strategy: Index summaries, retrieve details
# 1. Create detailed chunks
detail_parser = SentenceSplitter(chunk_size=256)
detail_nodes = detail_parser.get_nodes_from_documents(documents)

# 2. Create summary for each document
summary_index = SummaryIndex.from_documents(documents)

# 3. Link summaries to detail nodes
# When summary is retrieved, follow link to get full detail

# Auto-merging retriever (built-in)
from llama_index.core.node_parser import HierarchicalNodeParser
from llama_index.core.retrievers import AutoMergingRetriever

# Create hierarchical nodes (sentence → paragraph → section)
node_parser = HierarchicalNodeParser.from_defaults(
    chunk_sizes=[2048, 512, 128]  # Section → paragraph → sentence
)
nodes = node_parser.get_nodes_from_documents(documents)

# Build index on leaf nodes (smallest chunks)
leaf_nodes = [n for n in nodes if not n.child_nodes]
index = VectorStoreIndex(leaf_nodes)

# Auto-merging: if enough child nodes are retrieved, return parent instead
retriever = AutoMergingRetriever(
    vector_retriever=index.as_retriever(similarity_top_k=12),
    storage_context=index.storage_context,
    verbose=True,
)
# If 3+ sentences from same paragraph are retrieved → return full paragraph
```

**Theory: Why Recursive/Hierarchical?**
- Small chunks = better retrieval precision (find the exact relevant sentence)
- Large chunks = better generation context (LLM needs surrounding context)
- Solution: Search on small, return big (auto-merge small → parent)

---

## Knowledge Graph Index

```python
from llama_index.core import KnowledgeGraphIndex

# Build knowledge graph from documents
kg_index = KnowledgeGraphIndex.from_documents(
    documents,
    max_triplets_per_chunk=5,
    include_embeddings=True,
)

# Query with graph traversal
query_engine = kg_index.as_query_engine(
    include_text=True,  # Include source text alongside graph
    response_mode="tree_summarize",
)
response = query_engine.query("What is the relationship between Product A and Product B?")

# Visualize the graph
from pyvis.network import Network
g = kg_index.get_networkx_graph()
net = Network(notebook=True)
net.from_nx(g)
net.show("knowledge_graph.html")
```

**When to use Knowledge Graphs:**
- Relationship-heavy data ("Who reports to whom?", "What depends on what?")
- Multi-hop reasoning ("What products does Company X's CEO's team use?")
- Structured data with clear entities and relationships

---

## Query Transformations

### HyDE (Hypothetical Document Embeddings)

```python
from llama_index.core.indices.query.query_transform import HyDEQueryTransform

# Theory: Instead of embedding the QUESTION, generate a hypothetical ANSWER
# and embed that. The hypothetical answer is closer in embedding space to
# real answers than the question is.

hyde = HyDEQueryTransform(include_original=True)
query_engine = TransformQueryEngine(
    query_engine=base_query_engine,
    query_transform=hyde,
)

# User asks: "What causes memory leaks in Java?"
# HyDE generates: "Memory leaks in Java are caused by objects that are no longer
#                   needed but still referenced, preventing garbage collection..."
# This hypothetical answer is embedded and used for retrieval
# → Finds more relevant chunks than embedding the question directly
```

### Sub-Question Query Engine

```python
from llama_index.core.query_engine import SubQuestionQueryEngine
from llama_index.core.tools import QueryEngineTool

# For complex questions that span multiple topics
tools = [
    QueryEngineTool.from_defaults(
        query_engine=billing_engine,
        description="Answers questions about billing, pricing, and subscriptions",
    ),
    QueryEngineTool.from_defaults(
        query_engine=technical_engine,
        description="Answers technical questions about product features and bugs",
    ),
]

# Decomposes complex query into sub-questions, routes each to appropriate engine
query_engine = SubQuestionQueryEngine.from_defaults(query_engine_tools=tools)

# "Compare the Pro plan features with the Enterprise plan pricing"
# → Sub-Q1: "What are the Pro plan features?" → technical_engine
# → Sub-Q2: "What is the Enterprise plan pricing?" → billing_engine
# → Synthesize both answers
```

---

## Production RAG Pipeline

```python
from llama_index.core import VectorStoreIndex, Settings
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.postprocessor import SimilarityPostprocessor, CohereRerank
from llama_index.core.query_engine import RetrieverQueryEngine
from llama_index.core.response_synthesizers import get_response_synthesizer

def build_production_rag(documents, vector_store):
    """Production RAG with all advanced patterns."""
    
    # 1. Chunking with overlap
    parser = SentenceSplitter(chunk_size=512, chunk_overlap=50)
    nodes = parser.get_nodes_from_documents(documents)
    
    # 2. Index with external vector store
    index = VectorStoreIndex(
        nodes,
        vector_store=vector_store,
        show_progress=True,
    )
    
    # 3. Hybrid retriever
    vector_retriever = index.as_retriever(similarity_top_k=10)
    bm25_retriever = BM25Retriever.from_defaults(nodes=nodes, similarity_top_k=10)
    
    hybrid = QueryFusionRetriever(
        retrievers=[vector_retriever, bm25_retriever],
        mode="reciprocal_rerank",
        similarity_top_k=10,
    )
    
    # 4. Post-processing pipeline
    postprocessors = [
        SimilarityPostprocessor(similarity_cutoff=0.5),  # Drop low-relevance
        CohereRerank(top_n=5, model="rerank-english-v3.0"),  # Rerank to top 5
    ]
    
    # 5. Response synthesis
    synthesizer = get_response_synthesizer(
        response_mode="compact",  # Stuff into one prompt
        use_async=True,
    )
    
    # 6. Assemble query engine
    query_engine = RetrieverQueryEngine(
        retriever=hybrid,
        node_postprocessors=postprocessors,
        response_synthesizer=synthesizer,
    )
    
    return query_engine
```

---

## Next: [Evaluation & Observability →](07_Evaluation.md)
