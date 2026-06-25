# Module 1: RAG Foundations

## What is RAG?

Retrieval-Augmented Generation (RAG) combines information retrieval with generative AI. Instead of relying solely on an LLM's training data, RAG fetches relevant documents at query time and provides them as context to the LLM.

```
User Query → Retrieve Relevant Docs → Augment Prompt with Context → Generate Response
```

## Why RAG?

| Problem | How RAG Solves It |
|---------|-------------------|
| LLM knowledge cutoff | Retrieves up-to-date information |
| Hallucinations | Grounds responses in source documents |
| Domain specificity | Accesses proprietary/internal data |
| Token limits | Retrieves only relevant chunks |
| Auditability | Provides source citations |

## RAG Architecture Components

### 1. Indexing Pipeline (Offline)
```
Documents → Load → Split/Chunk → Embed → Store in Vector DB
```

### 2. Retrieval Pipeline (Online)
```
Query → Embed Query → Vector Search → Rerank → Top-K Documents
```

### 3. Generation Pipeline (Online)
```
Query + Retrieved Context → Prompt Template → LLM → Response
```

## The RAG Triad (Quality Dimensions)

1. **Context Relevance**: Are retrieved documents relevant to the query?
2. **Groundedness**: Is the response grounded in the retrieved context?
3. **Answer Relevance**: Does the response actually answer the question?

## Naive RAG vs Advanced RAG vs Modular RAG

### Naive RAG
```python
# Simple retrieve-and-generate
docs = retriever.get_relevant_documents(query)
response = llm(f"Context: {docs}\nQuestion: {query}")
```
- Single retrieval step
- No query transformation
- No reranking
- Prone to noise

### Advanced RAG
```
Query → Rewrite → Retrieve → Rerank → Filter → Generate → Validate
```
Improvements:
- Pre-retrieval: Query expansion, HyDE, step-back prompting
- Retrieval: Hybrid search (dense + sparse), multi-index
- Post-retrieval: Reranking, compression, deduplication

### Modular RAG (2024+)
```
Query → [Agent decides] → Route to appropriate pipeline → Adaptive retrieval
```
- Agentic routing decisions
- Iterative retrieval
- Self-correction loops
- Tool-augmented retrieval

## When NOT to Use RAG

- Simple factual questions (use LLM directly)
- Creative writing tasks
- Real-time data needs (<1 sec freshness) — use function calling
- When data fits in context window entirely
- Highly structured data queries — use Text-to-SQL

## Core Concepts

### Embeddings
Dense vector representations of text that capture semantic meaning.
```python
from openai import OpenAI
client = OpenAI()

response = client.embeddings.create(
    model="text-embedding-3-large",
    input="What is retrieval augmented generation?",
    dimensions=1024  # Matryoshka dimensionality reduction
)
embedding = response.data[0].embedding  # [0.023, -0.012, ...]
```

### Semantic Similarity
Measuring how similar two pieces of text are using cosine similarity:
```python
import numpy as np

def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

# score ∈ [-1, 1], higher = more similar
```

### Vector Search
Finding the K nearest neighbors to a query vector in high-dimensional space:
- **Exact (brute-force)**: O(n) — accurate but slow
- **Approximate (ANN)**: O(log n) — HNSW, IVF, PQ algorithms

## End-to-End Example: Basic RAG Pipeline

```python
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import WebBaseLoader
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser

# 1. Load documents
loader = WebBaseLoader("https://docs.example.com/guide")
docs = loader.load()

# 2. Split into chunks
splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
chunks = splitter.split_documents(docs)

# 3. Create vector store
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma.from_documents(chunks, embeddings)

# 4. Create retriever
retriever = vectorstore.as_retriever(search_type="mmr", search_kwargs={"k": 5})

# 5. Define prompt
prompt = ChatPromptTemplate.from_template("""
Answer the question based only on the following context:
{context}

Question: {question}

If the context doesn't contain relevant information, say "I don't have enough information to answer this question."
""")

# 6. Build chain
llm = ChatOpenAI(model="gpt-4o", temperature=0)
chain = (
    {"context": retriever, "question": RunnablePassthrough()}
    | prompt
    | llm
    | StrOutputParser()
)

# 7. Query
response = chain.invoke("How do I set up authentication?")
print(response)
```

## Key Metrics to Track

| Metric | Target | Tool |
|--------|--------|------|
| Context Precision | >0.8 | RAGAS |
| Context Recall | >0.9 | RAGAS |
| Faithfulness | >0.9 | RAGAS |
| Answer Relevance | >0.85 | RAGAS |
| Latency (P95) | <3s | Custom |
| Cost per query | <$0.01 | LangSmith |

## Exercises

1. Build a basic RAG pipeline over a PDF document
2. Compare responses with and without RAG for domain-specific questions
3. Experiment with different chunk sizes (256, 512, 1024, 2048) and observe quality changes
4. Measure retrieval quality by manually labeling relevance of top-5 results
