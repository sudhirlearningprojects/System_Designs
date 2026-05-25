# 2. Ingestion & Indexing

## Theory: The Ingestion Pipeline

Ingestion transforms raw data into queryable knowledge. The quality of your RAG system is **80% determined by ingestion quality**.

```
Raw Data → Reader → Documents → Transformations → Nodes → Index → Storage
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                  ▼
              Node Parser      Metadata Extractor   Embedding
              (chunking)       (title, summary)     (vectorize)
```

### Chunking Theory

**The chunking dilemma:**
- Too small: Loses context, retrieves fragments
- Too large: Dilutes relevance, wastes tokens
- Optimal: Captures one complete idea/concept per chunk

**Strategies:**
| Strategy | How | Best For |
|----------|-----|----------|
| Fixed-size | Split every N characters | Simple, predictable |
| Sentence | Split on sentence boundaries | General text |
| Semantic | Split where topic changes | Technical docs |
| Hierarchical | Multiple sizes (sentence → paragraph → section) | Complex docs |
| Document-aware | Split on headers, sections, pages | Structured docs (Markdown, HTML) |

---

## Data Readers (LlamaHub)

```python
from llama_index.core import SimpleDirectoryReader
from llama_index.readers.web import SimpleWebPageReader
from llama_index.readers.database import DatabaseReader
from llama_index.readers.notion import NotionPageReader

# Local files (auto-detects format: PDF, DOCX, TXT, MD, CSV, etc.)
documents = SimpleDirectoryReader(
    input_dir="./data",
    recursive=True,
    required_exts=[".pdf", ".md", ".txt"],
    filename_as_id=True,
).load_data()

# Web pages
documents = SimpleWebPageReader(html_to_text=True).load_data(
    urls=["https://docs.example.com/guide"]
)

# Database
reader = DatabaseReader(uri="postgresql://user:pass@host/db")
documents = reader.load_data(query="SELECT content, title FROM articles WHERE active=true")

# Notion
reader = NotionPageReader(integration_token="secret_...")
documents = reader.load_data(page_ids=["page-id-1", "page-id-2"])

# Custom metadata
for doc in documents:
    doc.metadata["ingestion_date"] = "2024-01-15"
    doc.metadata["source_type"] = "internal_docs"
```

---

## Node Parsers

```python
from llama_index.core.node_parser import (
    SentenceSplitter,
    SemanticSplitterNodeParser,
    HierarchicalNodeParser,
    MarkdownNodeParser,
)
from llama_index.embeddings.openai import OpenAIEmbedding

# Sentence splitter (most common)
parser = SentenceSplitter(
    chunk_size=1024,      # Max characters per chunk
    chunk_overlap=200,    # Overlap between chunks
    paragraph_separator="\n\n",
)

# Semantic splitter (split where meaning changes)
parser = SemanticSplitterNodeParser(
    embed_model=OpenAIEmbedding(),
    buffer_size=1,            # Sentences to group
    breakpoint_percentile_threshold=95,  # Similarity threshold for split
)

# Hierarchical (multi-level: section → paragraph → sentence)
parser = HierarchicalNodeParser.from_defaults(
    chunk_sizes=[2048, 512, 128],  # Three levels
)

# Markdown-aware (respects headers and structure)
parser = MarkdownNodeParser()

# Apply
nodes = parser.get_nodes_from_documents(documents)
print(f"Created {len(nodes)} nodes from {len(documents)} documents")
```

---

## Ingestion Pipeline

```python
from llama_index.core.ingestion import IngestionPipeline
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.extractors import (
    TitleExtractor,
    SummaryExtractor,
    QuestionsAnsweredExtractor,
    KeywordExtractor,
)
from llama_index.embeddings.openai import OpenAIEmbedding

# Full pipeline with transformations
pipeline = IngestionPipeline(
    transformations=[
        # 1. Split into chunks
        SentenceSplitter(chunk_size=1024, chunk_overlap=200),
        
        # 2. Extract metadata (uses LLM)
        TitleExtractor(nodes=5),           # Infer title from content
        SummaryExtractor(summaries=["self"]),  # Generate summary per chunk
        KeywordExtractor(keywords=5),      # Extract keywords
        QuestionsAnsweredExtractor(questions=3),  # What questions does this answer?
        
        # 3. Generate embeddings
        OpenAIEmbedding(model="text-embedding-3-small"),
    ],
    vector_store=vector_store,  # Optional: store directly
)

# Run pipeline
nodes = pipeline.run(documents=documents, show_progress=True)

# Pipeline with caching (skip already-processed docs)
from llama_index.core.ingestion import IngestionCache
from llama_index.core.storage.docstore import SimpleDocumentStore

pipeline = IngestionPipeline(
    transformations=[...],
    cache=IngestionCache(),  # Deduplicates based on content hash
    docstore=SimpleDocumentStore(),  # Track processed documents
)
```

---

## Storage

```python
from llama_index.core import StorageContext, VectorStoreIndex
from llama_index.vector_stores.pinecone import PineconeVectorStore
from llama_index.vector_stores.chroma import ChromaVectorStore
import chromadb

# Chroma (local, development)
chroma_client = chromadb.PersistentClient(path="./chroma_db")
collection = chroma_client.get_or_create_collection("my_collection")
vector_store = ChromaVectorStore(chroma_collection=collection)

# Pinecone (managed, production)
from pinecone import Pinecone
pc = Pinecone(api_key="...")
pinecone_index = pc.Index("my-index")
vector_store = PineconeVectorStore(pinecone_index=pinecone_index)

# Build index with external storage
storage_context = StorageContext.from_defaults(vector_store=vector_store)
index = VectorStoreIndex.from_documents(
    documents,
    storage_context=storage_context,
    show_progress=True,
)

# Persist everything (docstore + index_store + vector_store)
index.storage_context.persist(persist_dir="./storage")

# Reload without re-embedding
from llama_index.core import load_index_from_storage
storage_context = StorageContext.from_defaults(persist_dir="./storage")
index = load_index_from_storage(storage_context)
```

---

## Next: [Retrieval & Query Engines →](03_Retrieval.md)
