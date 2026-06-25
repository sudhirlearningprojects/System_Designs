# Module 5: Retrieval Strategies

## Overview

Retrieval is the most critical component of RAG. Better retrieval directly translates to better answers.

```
Query → [Transform] → [Retrieve] → [Rerank] → [Filter] → Top-K Context
```

---

## Retrieval Methods

### 1. Dense Retrieval (Semantic Search)
```python
# Standard vector similarity search
retriever = vectorstore.as_retriever(
    search_type="similarity",
    search_kwargs={"k": 10},
)
```

### 2. Sparse Retrieval (BM25/Keyword)
```python
from langchain_community.retrievers import BM25Retriever

bm25_retriever = BM25Retriever.from_documents(documents, k=10)
results = bm25_retriever.invoke("kubernetes pod deployment")
```

### 3. Hybrid Search (Dense + Sparse) — Best Practice
```python
from langchain.retrievers import EnsembleRetriever

# Combine semantic + keyword search
dense_retriever = vectorstore.as_retriever(search_kwargs={"k": 10})
sparse_retriever = BM25Retriever.from_documents(documents, k=10)

hybrid_retriever = EnsembleRetriever(
    retrievers=[dense_retriever, sparse_retriever],
    weights=[0.7, 0.3],  # Favor semantic, but include keyword matches
)
```

### 4. Maximum Marginal Relevance (MMR) — Diversity
```python
# Reduce redundancy in retrieved documents
retriever = vectorstore.as_retriever(
    search_type="mmr",
    search_kwargs={
        "k": 5,
        "fetch_k": 20,    # Fetch 20, then diversify to 5
        "lambda_mult": 0.7,  # 0=max diversity, 1=max relevance
    },
)
```

### 5. Similarity Score Threshold
```python
retriever = vectorstore.as_retriever(
    search_type="similarity_score_threshold",
    search_kwargs={"score_threshold": 0.75, "k": 10},
)
# Only returns documents above the similarity threshold
```

---

## Query Transformation Techniques

Poor queries → poor retrieval. Transform queries before searching.

### 1. Query Rewriting (LLM-Based)
```python
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)

rewrite_prompt = ChatPromptTemplate.from_template("""
Rewrite the following query to be more specific and suitable for semantic search.
Add relevant technical terms that might appear in documentation.

Original query: {query}
Rewritten query:""")

chain = rewrite_prompt | llm
rewritten = chain.invoke({"query": "how do I make it faster?"})
# Output: "How to optimize application performance and reduce latency?"
```

### 2. HyDE (Hypothetical Document Embeddings)
Generate a hypothetical answer, embed it, and use that for retrieval:

```python
from langchain_core.prompts import ChatPromptTemplate

hyde_prompt = ChatPromptTemplate.from_template("""
Write a detailed passage that would answer the following question.
Write as if you're writing documentation.

Question: {query}
Passage:""")

def hyde_retrieval(query: str, vectorstore, llm):
    # Generate hypothetical document
    hypothetical_doc = (hyde_prompt | llm).invoke({"query": query}).content
    
    # Embed the hypothetical doc and search
    results = vectorstore.similarity_search(hypothetical_doc, k=5)
    return results
```
**When to use**: Short queries that lack context; improves recall by 10-20%.

### 3. Multi-Query Retrieval
Generate multiple perspectives of the same query:

```python
from langchain.retrievers.multi_query import MultiQueryRetriever

retriever = MultiQueryRetriever.from_llm(
    retriever=vectorstore.as_retriever(search_kwargs={"k": 5}),
    llm=ChatOpenAI(model="gpt-4o-mini", temperature=0.3),
)

# Internally generates 3+ query variations and combines results
results = retriever.invoke("What are the system requirements?")
```

### 4. Step-Back Prompting
Ask a broader question first to get context:

```python
step_back_prompt = ChatPromptTemplate.from_template("""
Given the specific question, generate a more general "step-back" question 
that would help provide broader context for answering.

Specific question: {query}
Step-back question:""")

# Query: "Why does my Python 3.11 async function hang?"
# Step-back: "How does Python asyncio event loop handle coroutine execution?"
```

### 5. Query Decomposition (Sub-Questions)
```python
decompose_prompt = ChatPromptTemplate.from_template("""
Break this complex question into 2-4 simpler sub-questions that can be 
answered independently. Return as JSON array.

Question: {query}
Sub-questions:""")

# Query: "Compare the performance and cost of Pinecone vs Qdrant for a 10M vector use case"
# Sub-questions: 
#   1. "What is Pinecone's query performance at 10M vectors?"
#   2. "What is Qdrant's query performance at 10M vectors?"
#   3. "What is Pinecone's pricing for 10M vectors?"
#   4. "What is Qdrant's pricing for 10M vectors?"
```

---

## Reranking

Reranking takes initial retrieval results and reorders them using a more powerful model.

### Why Rerank?
- Initial retrieval (bi-encoder) is fast but approximate
- Rerankers (cross-encoders) are slow but accurate
- Pattern: Retrieve many → Rerank to few

### Cohere Rerank v3
```python
from langchain_cohere import CohereRerank
from langchain.retrievers import ContextualCompressionRetriever

# Initial retrieval: get 20 docs
base_retriever = vectorstore.as_retriever(search_kwargs={"k": 20})

# Rerank to top 5
reranker = CohereRerank(model="rerank-v3.5", top_n=5)
compression_retriever = ContextualCompressionRetriever(
    base_compressor=reranker,
    base_retriever=base_retriever,
)

results = compression_retriever.invoke("How to set up authentication?")
```

### Jina Reranker
```python
from langchain_community.document_compressors import JinaRerank

reranker = JinaRerank(model="jina-reranker-v2-base-multilingual", top_n=5)
```

### Open Source: FlashRank (Free, Local)
```python
from langchain_community.document_compressors import FlashrankRerank

reranker = FlashrankRerank(model="ms-marco-MiniLM-L-12-v2", top_n=5)
compression_retriever = ContextualCompressionRetriever(
    base_compressor=reranker,
    base_retriever=base_retriever,
)
```

### Cross-Encoder Reranking (Sentence Transformers)
```python
from sentence_transformers import CrossEncoder

model = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-12-v2")

# Score query-document pairs
pairs = [(query, doc.page_content) for doc in initial_results]
scores = model.predict(pairs)

# Sort by score
ranked_results = sorted(zip(scores, initial_results), reverse=True)
top_5 = [doc for _, doc in ranked_results[:5]]
```

### Reranker Comparison

| Reranker | Speed | Quality | Cost | Multilingual |
|----------|-------|---------|------|-------------|
| Cohere Rerank v3.5 | Fast | ⭐⭐⭐⭐⭐ | $2/1K queries | ✅ |
| Jina Reranker v2 | Fast | ⭐⭐⭐⭐ | $0.02/1K | ✅ |
| FlashRank | Very Fast | ⭐⭐⭐ | Free | ❌ |
| BGE Reranker v2 | Medium | ⭐⭐⭐⭐ | Free (self-host) | ✅ |
| ColBERT (late interaction) | Slow | ⭐⭐⭐⭐⭐ | Free (self-host) | ❌ |

---

## Parent Document Retriever

Retrieve small chunks for precision, but return the larger parent document for context:

```python
from langchain.retrievers import ParentDocumentRetriever
from langchain.storage import InMemoryStore
from langchain.text_splitter import RecursiveCharacterTextSplitter

# Small chunks for retrieval (high precision)
child_splitter = RecursiveCharacterTextSplitter(chunk_size=200, chunk_overlap=50)

# Large chunks for context (more information)
parent_splitter = RecursiveCharacterTextSplitter(chunk_size=2000, chunk_overlap=200)

store = InMemoryStore()

retriever = ParentDocumentRetriever(
    vectorstore=vectorstore,
    docstore=store,
    child_splitter=child_splitter,
    parent_splitter=parent_splitter,
)

retriever.add_documents(documents)
# Searches against small chunks, returns parent (large) documents
results = retriever.invoke("specific technical detail")
```

---

## Self-Query Retriever (Structured Filtering)

Automatically extracts metadata filters from natural language queries:

```python
from langchain.retrievers.self_query.base import SelfQueryRetriever
from langchain.chains.query_constructor.schema import AttributeInfo

metadata_field_info = [
    AttributeInfo(name="source", description="Document source file", type="string"),
    AttributeInfo(name="page", description="Page number", type="integer"),
    AttributeInfo(name="date", description="Publication date", type="string"),
    AttributeInfo(name="author", description="Document author", type="string"),
]

retriever = SelfQueryRetriever.from_llm(
    llm=ChatOpenAI(model="gpt-4o-mini", temperature=0),
    vectorstore=vectorstore,
    document_contents="Technical documentation about software systems",
    metadata_field_info=metadata_field_info,
)

# Query: "What did John write about authentication after 2024?"
# Auto-generates filter: author="John" AND date>"2024-01-01"
results = retriever.invoke("What did John write about authentication after 2024?")
```

---

## Contextual Compression

Remove irrelevant parts of retrieved documents before sending to LLM:

```python
from langchain.retrievers.document_compressors import LLMChainExtractor

compressor = LLMChainExtractor.from_llm(ChatOpenAI(model="gpt-4o-mini"))

compression_retriever = ContextualCompressionRetriever(
    base_compressor=compressor,
    base_retriever=vectorstore.as_retriever(search_kwargs={"k": 10}),
)

# Returns only the relevant portions of each document
results = compression_retriever.invoke("What are the authentication methods?")
```

---

## Multi-Index Retrieval

Search across multiple vector stores and combine results:

```python
from langchain.retrievers import MergerRetriever

# Different indexes for different content types
code_retriever = code_vectorstore.as_retriever(search_kwargs={"k": 3})
docs_retriever = docs_vectorstore.as_retriever(search_kwargs={"k": 3})
faq_retriever = faq_vectorstore.as_retriever(search_kwargs={"k": 3})

# Combine results from all indexes
merged_retriever = MergerRetriever(
    retrievers=[code_retriever, docs_retriever, faq_retriever]
)
```

---

## Production Retrieval Pipeline

```python
class ProductionRetriever:
    """Multi-stage retrieval with query transformation, hybrid search, and reranking."""
    
    def __init__(self, vectorstore, llm):
        self.vectorstore = vectorstore
        self.llm = llm
        self.reranker = CohereRerank(model="rerank-v3.5", top_n=5)
    
    def retrieve(self, query: str, k: int = 5) -> list:
        # Stage 1: Query transformation
        enhanced_query = self._rewrite_query(query)
        
        # Stage 2: Multi-query expansion
        queries = self._generate_sub_queries(enhanced_query)
        
        # Stage 3: Hybrid retrieval (dense + sparse)
        all_results = []
        for q in queries:
            dense_results = self.vectorstore.similarity_search(q, k=10)
            all_results.extend(dense_results)
        
        # Stage 4: Deduplicate
        unique_results = self._deduplicate(all_results)
        
        # Stage 5: Rerank
        reranked = self.reranker.compress_documents(unique_results, query)
        
        return reranked[:k]
    
    def _rewrite_query(self, query: str) -> str:
        return self.llm.invoke(
            f"Rewrite for semantic search: {query}"
        ).content
    
    def _generate_sub_queries(self, query: str) -> list[str]:
        response = self.llm.invoke(
            f"Generate 3 search queries to answer: {query}. Return as JSON array."
        ).content
        return [query] + parse_json(response)
    
    def _deduplicate(self, docs: list) -> list:
        seen = set()
        unique = []
        for doc in docs:
            content_hash = hash(doc.page_content[:200])
            if content_hash not in seen:
                seen.add(content_hash)
                unique.append(doc)
        return unique
```

---

## Exercises

1. Implement hybrid search (BM25 + vector) and compare to vector-only on 20 test queries
2. Add Cohere Rerank to your pipeline and measure precision improvement
3. Implement HyDE and compare retrieval quality on ambiguous queries
4. Build a parent document retriever and compare context quality
5. Create a multi-stage retrieval pipeline with query rewriting + reranking
