# 2. RAG with LangChain

## Theory: What is RAG?

Retrieval-Augmented Generation (RAG) solves the fundamental limitation of LLMs: they only know what was in their training data. RAG gives models access to external, up-to-date, domain-specific knowledge at inference time.

### Why RAG?

| Problem | Without RAG | With RAG |
|---------|-------------|----------|
| Knowledge cutoff | Model doesn't know recent events | Retrieves latest docs |
| Hallucination | Model invents plausible-sounding facts | Grounds answers in real sources |
| Domain expertise | Generic knowledge only | Access to proprietary docs |
| Verifiability | Can't cite sources | Provides citations |
| Cost | Fine-tuning is expensive | No retraining needed |

### RAG Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    RAG PIPELINE                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  OFFLINE (Indexing)                                          │
│  ┌──────┐   ┌──────┐   ┌──────┐   ┌──────────────────┐    │
│  │ Load │ → │Split │ → │Embed │ → │ Store in Vector DB│    │
│  │ Docs │   │Chunks│   │      │   │ (Pinecone/Chroma) │    │
│  └──────┘   └──────┘   └──────┘   └──────────────────┘    │
│                                                              │
│  ONLINE (Query Time)                                         │
│  ┌──────┐   ┌────────┐   ┌────────┐   ┌──────────────┐    │
│  │Query │ → │Embed   │ → │Search  │ → │ LLM Generate │    │
│  │      │   │Query   │   │Top-K   │   │ with Context │    │
│  └──────┘   └────────┘   └────────┘   └──────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Key Concepts

**Chunking**: Documents are split into smaller pieces because:
- LLMs have limited context windows
- Smaller chunks = more precise retrieval
- Embedding models work better on focused text

**Embeddings**: Convert text to dense vectors (arrays of numbers) that capture semantic meaning. Similar texts have similar vectors (high cosine similarity).

**Vector Search**: Find the K most similar chunks to the user's query by comparing embedding vectors. This is approximate nearest neighbor (ANN) search — O(log N) not O(N).

**Retrieval Strategies**:
- **Similarity search**: Pure cosine similarity (fast, simple)
- **MMR (Maximum Marginal Relevance)**: Balance relevance with diversity (avoid redundant results)
- **Self-query**: LLM generates metadata filters from natural language
- **Parent document**: Search small chunks, return full parent documents
- **Hybrid**: Combine keyword (BM25) + semantic search

### RAG Quality Dimensions

| Dimension | Question | Failure Mode |
|-----------|----------|---------------|
| **Context Precision** | Are retrieved docs relevant? | Irrelevant docs dilute context |
| **Context Recall** | Were all needed docs found? | Missing critical information |
| **Faithfulness** | Is answer grounded in context? | Hallucination despite good retrieval |
| **Answer Relevancy** | Does answer address the question? | Correct info but wrong focus |

---

## RAG Pipeline

```
Documents → Load → Split → Embed → Store → Retrieve → Generate
```

## Document Loaders

```python
from langchain_community.document_loaders import (
    PyPDFLoader, TextLoader, WebBaseLoader,
    DirectoryLoader, UnstructuredMarkdownLoader,
)

# PDF
docs = PyPDFLoader("manual.pdf").load()

# Web page
docs = WebBaseLoader("https://docs.example.com/guide").load()

# Directory of files
docs = DirectoryLoader("./docs/", glob="**/*.md", loader_cls=UnstructuredMarkdownLoader).load()

# Each doc has: page_content (text) + metadata (source, page, etc.)
```

## Text Splitters

```python
from langchain_text_splitters import RecursiveCharacterTextSplitter, MarkdownHeaderTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=200,
    separators=["\n\n", "\n", ". ", " ", ""],
)
chunks = splitter.split_documents(docs)

# Markdown-aware splitting
md_splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=[("#", "h1"), ("##", "h2"), ("###", "h3")]
)
```

## Embeddings & Vector Stores

```python
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma, FAISS, Pinecone

embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

# Create vector store
vectorstore = Chroma.from_documents(chunks, embeddings, persist_directory="./chroma_db")

# Or Pinecone (production)
from langchain_pinecone import PineconeVectorStore
vectorstore = PineconeVectorStore.from_documents(chunks, embeddings, index_name="my-index")

# Retriever
retriever = vectorstore.as_retriever(
    search_type="mmr",  # Maximum Marginal Relevance (diversity)
    search_kwargs={"k": 5, "fetch_k": 20},
)
```

## Complete RAG Chain

```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser

prompt = ChatPromptTemplate.from_template("""Answer based ONLY on the context below.
If you can't answer from the context, say "I don't have that information."

Context: {context}

Question: {question}

Answer:""")

def format_docs(docs):
    return "\n\n".join(f"[{d.metadata.get('source', 'unknown')}]: {d.page_content}" for d in docs)

rag_chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}
    | prompt
    | ChatAnthropic(model="claude-sonnet-4-20250514")
    | StrOutputParser()
)

answer = rag_chain.invoke("How do I export a PDF?")
```

## Advanced: Parent Document Retriever

```python
from langchain.retrievers import ParentDocumentRetriever
from langchain.storage import InMemoryStore

# Store full documents, search on small chunks
parent_splitter = RecursiveCharacterTextSplitter(chunk_size=2000)
child_splitter = RecursiveCharacterTextSplitter(chunk_size=400)

store = InMemoryStore()
retriever = ParentDocumentRetriever(
    vectorstore=vectorstore,
    docstore=store,
    child_splitter=child_splitter,
    parent_splitter=parent_splitter,
)
retriever.add_documents(docs)
# Searches small chunks but returns full parent documents
```

## Advanced: Self-Query Retriever

```python
from langchain.retrievers.self_query.base import SelfQueryRetriever

retriever = SelfQueryRetriever.from_llm(
    llm=ChatAnthropic(model="claude-3-5-haiku-20241022"),
    vectorstore=vectorstore,
    document_contents="Product documentation for Adobe Creative Cloud",
    metadata_field_info=[
        {"name": "product", "type": "string", "description": "Product name (photoshop, illustrator, etc.)"},
        {"name": "category", "type": "string", "description": "Doc category (tutorial, troubleshooting, faq)"},
    ],
)
# User: "Photoshop tutorials about layers"
# → Automatically adds filter: product="photoshop", category="tutorial"
```

---

## Next: [Tools & Tool Calling →](03_Tools.md)
