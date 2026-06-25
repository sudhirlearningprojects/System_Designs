# Module 2: Document Processing & Ingestion

## Overview

Document processing is the foundation of RAG quality. Poor ingestion = poor retrieval = poor answers.

```
Raw Documents → Parse → Clean → Chunk → Enrich Metadata → Embed → Store
```

## Document Loading

### Supported Formats & Loaders

```python
from langchain_community.document_loaders import (
    PyPDFLoader,           # PDF files
    UnstructuredWordDocumentLoader,  # DOCX
    UnstructuredMarkdownLoader,      # Markdown
    CSVLoader,             # CSV/tabular data
    JSONLoader,            # JSON/JSONL
    WebBaseLoader,         # Web pages
    GitLoader,             # Git repositories
    NotionDBLoader,        # Notion databases
    ConfluenceLoader,      # Confluence pages
    S3FileLoader,          # AWS S3
    UnstructuredHTMLLoader,# HTML files
)
```

### Advanced: Unstructured.io (Best for Complex Documents)
```python
from unstructured.partition.auto import partition

# Handles PDFs with tables, images, headers automatically
elements = partition(filename="complex_report.pdf", strategy="hi_res")

# Elements include: Title, NarrativeText, Table, Image, ListItem, etc.
for element in elements:
    print(f"{element.category}: {element.text[:100]}")
```

### Advanced: LlamaParse (2024 - Best for Tables/Charts)
```python
from llama_parse import LlamaParse

parser = LlamaParse(
    api_key="<your-key>",
    result_type="markdown",  # or "text"
    parsing_instruction="Extract all tables with headers preserved",
    use_vendor_multimodal_model=True,  # GPT-4V for complex layouts
    vendor_multimodal_model_name="anthropic-sonnet-4-20250514"
)

documents = parser.load_data("financial_report.pdf")
```

### Multimodal Document Processing
```python
# Extract text from images in documents using vision models
from langchain_openai import ChatOpenAI

vision_llm = ChatOpenAI(model="gpt-4o")

# Describe images/charts found in documents
response = vision_llm.invoke([
    {"type": "text", "text": "Describe this chart in detail for RAG indexing:"},
    {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{image_b64}"}}
])
```

---

## Chunking Strategies

Chunking is the most impactful step. Wrong chunking = irrelevant retrieval.

### 1. Fixed-Size Chunking
```python
from langchain.text_splitter import CharacterTextSplitter

splitter = CharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=200,
    separator="\n"
)
```
**When to use**: Simple documents, uniform content

### 2. Recursive Character Splitting (Most Common)
```python
from langchain.text_splitter import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=200,
    separators=["\n\n", "\n", ". ", " ", ""],  # Try each in order
    length_function=len,
)
```
**When to use**: General purpose, most documents

### 3. Semantic Chunking (2024 - Context-Aware)
```python
from langchain_experimental.text_splitter import SemanticChunker
from langchain_openai import OpenAIEmbeddings

splitter = SemanticChunker(
    OpenAIEmbeddings(model="text-embedding-3-small"),
    breakpoint_threshold_type="percentile",  # or "standard_deviation", "interquartile"
    breakpoint_threshold_amount=95,
)
chunks = splitter.split_documents(docs)
```
**How it works**: Splits where semantic similarity between consecutive sentences drops significantly.

### 4. Agentic Chunking (2024 - LLM-Driven)
```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="gpt-4o-mini")

# LLM decides chunk boundaries based on content
prompt = """Given the following text, identify logical section boundaries.
Return the start and end character positions for each coherent chunk.
Each chunk should be self-contained and discuss a single topic.

Text: {text}
"""
```
**When to use**: High-value documents where quality justifies cost

### 5. Document-Structure-Aware Chunking
```python
from langchain.text_splitter import MarkdownHeaderTextSplitter

headers_to_split_on = [
    ("#", "Header 1"),
    ("##", "Header 2"),
    ("###", "Header 3"),
]

splitter = MarkdownHeaderTextSplitter(headers_to_split_on=headers_to_split_on)
chunks = splitter.split_text(markdown_content)
# Each chunk retains header hierarchy as metadata
```

### 6. Code-Aware Chunking
```python
from langchain.text_splitter import Language, RecursiveCharacterTextSplitter

python_splitter = RecursiveCharacterTextSplitter.from_language(
    language=Language.PYTHON,
    chunk_size=2000,
    chunk_overlap=200,
)
# Respects function/class boundaries
```

### Chunking Strategy Comparison

| Strategy | Quality | Speed | Cost | Best For |
|----------|---------|-------|------|----------|
| Fixed-size | ⭐⭐ | ⭐⭐⭐⭐⭐ | Free | Prototyping |
| Recursive | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Free | General use |
| Semantic | ⭐⭐⭐⭐ | ⭐⭐⭐ | Low | Quality-focused |
| Agentic | ⭐⭐⭐⭐⭐ | ⭐⭐ | High | High-value docs |
| Structure-aware | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Free | Markdown/HTML |

---

## Chunk Size Selection Guide

| Document Type | Recommended Size | Overlap |
|---------------|-----------------|---------|
| Technical docs | 1000-1500 tokens | 200 |
| Legal contracts | 500-800 tokens | 150 |
| Code files | 1500-2000 tokens | 200 |
| FAQ/QA pairs | 200-500 tokens | 50 |
| Research papers | 800-1200 tokens | 200 |
| Chat logs | 500-1000 tokens | 100 |

**Rule of thumb**: Chunk should contain enough context to answer a question independently.

---

## Metadata Enrichment

Metadata dramatically improves retrieval quality through filtering.

```python
from langchain_core.documents import Document

# Enrich chunks with metadata
enriched_chunks = []
for chunk in chunks:
    enriched_chunks.append(Document(
        page_content=chunk.page_content,
        metadata={
            "source": chunk.metadata.get("source"),
            "page": chunk.metadata.get("page"),
            "section": extract_section(chunk),
            "doc_type": "technical_guide",
            "created_date": "2024-01-15",
            "author": "engineering_team",
            "keywords": extract_keywords(chunk.page_content),
            "summary": generate_summary(chunk.page_content),  # LLM-generated
            "language": detect_language(chunk.page_content),
        }
    ))
```

### Auto-Generated Metadata with LLMs
```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)

def enrich_metadata(chunk_text: str) -> dict:
    response = llm.invoke(f"""Extract metadata from this text chunk:
    - topic: main topic (1-3 words)
    - entities: key entities mentioned
    - question: a question this chunk could answer
    
    Text: {chunk_text}
    
    Return as JSON.""")
    return parse_json(response.content)
```

---

## Contextual Chunking (Anthropic's Approach - 2024)

Add document-level context to each chunk for better retrieval:

```python
def add_contextual_header(chunk: Document, full_document: str) -> Document:
    """Prepend document context to each chunk (Anthropic's contextual retrieval)."""
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
    
    context = llm.invoke(f"""Given the full document and a specific chunk, 
    provide a brief context (2-3 sentences) that situates this chunk within the 
    overall document. This context should help a retriever understand what this 
    chunk is about without seeing the full document.
    
    Full Document (first 5000 chars): {full_document[:5000]}
    
    Chunk: {chunk.page_content}
    
    Context:""")
    
    chunk.page_content = f"{context.content}\n\n{chunk.page_content}"
    return chunk
```

This technique improved retrieval by 49% in Anthropic's benchmarks.

---

## Data Cleaning Pipeline

```python
import re

def clean_text(text: str) -> str:
    # Remove excessive whitespace
    text = re.sub(r'\s+', ' ', text)
    # Remove special characters that don't add meaning
    text = re.sub(r'[^\w\s.,!?;:()\-\[\]{}"\'/]', '', text)
    # Fix encoding issues
    text = text.encode('utf-8', errors='ignore').decode('utf-8')
    # Remove repeated punctuation
    text = re.sub(r'([.!?])\1+', r'\1', text)
    return text.strip()

def deduplicate_chunks(chunks: list, threshold: float = 0.95) -> list:
    """Remove near-duplicate chunks using MinHash."""
    from datasketch import MinHash, MinHashLSH
    
    lsh = MinHashLSH(threshold=threshold, num_perm=128)
    unique_chunks = []
    
    for i, chunk in enumerate(chunks):
        mh = MinHash(num_perm=128)
        for word in chunk.page_content.split():
            mh.update(word.encode('utf8'))
        
        if not lsh.query(mh):
            lsh.insert(f"chunk_{i}", mh)
            unique_chunks.append(chunk)
    
    return unique_chunks
```

---

## Multi-Modal Ingestion Pipeline

```python
class MultiModalIngestionPipeline:
    def __init__(self):
        self.text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        self.embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
        self.vision_llm = ChatOpenAI(model="gpt-4o")
    
    def process_pdf(self, path: str) -> list[Document]:
        """Process PDF with text, tables, and images."""
        from unstructured.partition.pdf import partition_pdf
        
        elements = partition_pdf(path, strategy="hi_res", extract_images_in_pdf=True)
        
        documents = []
        for element in elements:
            if element.category == "Table":
                # Keep tables as single chunks with table metadata
                documents.append(Document(
                    page_content=element.text,
                    metadata={"type": "table", "source": path}
                ))
            elif element.category == "Image":
                # Describe image using vision model
                description = self._describe_image(element.metadata.image_path)
                documents.append(Document(
                    page_content=description,
                    metadata={"type": "image_description", "source": path}
                ))
            else:
                documents.append(Document(
                    page_content=element.text,
                    metadata={"type": "text", "source": path}
                ))
        
        return documents
```

---

## Incremental Ingestion (Production Pattern)

```python
import hashlib
from datetime import datetime

class IncrementalIngestionService:
    def __init__(self, vectorstore, metadata_store):
        self.vectorstore = vectorstore
        self.metadata_store = metadata_store  # Track what's been ingested
    
    def ingest_if_changed(self, documents: list[Document]):
        """Only process new or modified documents."""
        to_process = []
        
        for doc in documents:
            content_hash = hashlib.sha256(doc.page_content.encode()).hexdigest()
            existing = self.metadata_store.get(doc.metadata["source"])
            
            if not existing or existing["hash"] != content_hash:
                to_process.append(doc)
                # Delete old vectors if updating
                if existing:
                    self.vectorstore.delete(filter={"source": doc.metadata["source"]})
        
        if to_process:
            chunks = self.text_splitter.split_documents(to_process)
            self.vectorstore.add_documents(chunks)
            
            # Update metadata store
            for doc in to_process:
                self.metadata_store.upsert({
                    "source": doc.metadata["source"],
                    "hash": hashlib.sha256(doc.page_content.encode()).hexdigest(),
                    "ingested_at": datetime.utcnow().isoformat(),
                    "chunk_count": len([c for c in chunks if c.metadata["source"] == doc.metadata["source"]])
                })
```

---

## Exercises

1. Compare chunking strategies on the same document — measure retrieval quality for 10 test queries
2. Implement contextual chunking and measure improvement in retrieval precision
3. Build a multi-format ingestion pipeline (PDF + DOCX + HTML + Markdown)
4. Create an incremental ingestion system that only re-processes changed documents
5. Experiment with chunk sizes: plot chunk_size vs retrieval_precision curve
