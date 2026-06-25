# Module 3: Embedding Models

## Overview

Embeddings convert text into dense vectors that capture semantic meaning. The quality of your embeddings directly determines retrieval quality.

## Latest Embedding Models (2024-2025)

### Comparison Table

| Model | Dimensions | Max Tokens | MTEB Score | Cost/1M tokens | Best For |
|-------|-----------|------------|------------|----------------|----------|
| OpenAI text-embedding-3-large | 3072 (configurable) | 8191 | 64.6 | $0.13 | General purpose |
| OpenAI text-embedding-3-small | 1536 (configurable) | 8191 | 62.3 | $0.02 | Cost-effective |
| Cohere embed-v4 | 1024 | 512 | 66.2 | $0.10 | Multilingual |
| Voyage AI voyage-3-large | 1024 | 32000 | 67.1 | $0.18 | Long context, code |
| BGE-M3 (open source) | 1024 | 8192 | 65.0 | Free (self-host) | Multilingual, hybrid |
| Nomic Embed v1.5 | 768 | 8192 | 62.2 | Free (open) | Budget-friendly |
| Jina Embeddings v3 | 1024 | 8192 | 65.5 | $0.02 | Multilingual |
| AWS Titan Embeddings v2 | 1024 | 8192 | 61.8 | $0.02 | AWS ecosystem |
| Gemini text-embedding-004 | 768 | 2048 | 66.0 | Free (limited) | Google ecosystem |

### Selection Guide
- **Best overall**: Voyage AI voyage-3-large or Cohere embed-v4
- **Best value**: OpenAI text-embedding-3-small
- **Best open-source**: BGE-M3
- **Best for code**: Voyage Code 3
- **Best for long documents**: Voyage AI (32K context)
- **Best multilingual**: Cohere embed-v4 or BGE-M3

---

## Using Embedding Models

### OpenAI Embeddings (Matryoshka Support)
```python
from openai import OpenAI

client = OpenAI()

# Full dimensions (3072)
response = client.embeddings.create(
    model="text-embedding-3-large",
    input=["What is RAG?", "Retrieval augmented generation explained"],
)

# Reduced dimensions (Matryoshka) - saves storage + faster search
response = client.embeddings.create(
    model="text-embedding-3-large",
    input="What is RAG?",
    dimensions=1024,  # Can reduce to 256, 512, 1024, etc.
)
```

### Cohere Embed v4
```python
import cohere

co = cohere.ClientV2(api_key="<your-key>")

# Separate input_type for documents vs queries
doc_embeddings = co.embed(
    texts=["RAG combines retrieval with generation"],
    model="embed-v4.0",
    input_type="search_document",
    embedding_types=["float"],
).embeddings.float_

query_embeddings = co.embed(
    texts=["What is RAG?"],
    model="embed-v4.0",
    input_type="search_query",
    embedding_types=["float"],
).embeddings.float_
```

### Voyage AI
```python
import voyageai

vo = voyageai.Client(api_key="<your-key>")

# Supports up to 32K tokens
result = vo.embed(
    ["Long document content here..."],
    model="voyage-3-large",
    input_type="document",  # or "query"
)
embeddings = result.embeddings
```

### Open Source: BGE-M3 (Self-Hosted)
```python
from FlagEmbedding import BGEM3FlagModel

model = BGEM3FlagModel("BAAI/bge-m3", use_fp16=True)

# Supports dense + sparse + colbert embeddings
output = model.encode(
    ["What is RAG?"],
    return_dense=True,
    return_sparse=True,
    return_colbert_vecs=True,
)

dense_embedding = output["dense_vecs"]     # For semantic search
sparse_embedding = output["lexical_weights"]  # For keyword search (hybrid)
colbert_vecs = output["colbert_vecs"]      # For late interaction (reranking)
```

### AWS Bedrock Titan Embeddings
```python
import boto3
import json

bedrock = boto3.client("bedrock-runtime", region_name="us-east-1")

response = bedrock.invoke_model(
    modelId="amazon.titan-embed-text-v2:0",
    body=json.dumps({
        "inputText": "What is retrieval augmented generation?",
        "dimensions": 1024,
        "normalize": True,
    })
)

embedding = json.loads(response["body"].read())["embedding"]
```

---

## Matryoshka Embeddings (Dimension Reduction)

Modern embedding models support Matryoshka Representation Learning (MRL), allowing you to truncate dimensions while preserving most of the semantic information.

```python
# OpenAI: text-embedding-3-large supports MRL
# Full: 3072 dims → ~100% quality
# 1024 dims → ~99% quality (67% storage savings)
# 512 dims  → ~97% quality (83% storage savings)
# 256 dims  → ~94% quality (92% storage savings)

from langchain_openai import OpenAIEmbeddings

# Use fewer dimensions for cost/speed optimization
embeddings = OpenAIEmbeddings(
    model="text-embedding-3-large",
    dimensions=1024,  # Reduce from 3072 default
)
```

**Storage Impact**:
- 1M vectors @ 3072 dims (float32) = ~12 GB
- 1M vectors @ 1024 dims (float32) = ~4 GB
- 1M vectors @ 256 dims (float32) = ~1 GB

---

## Embedding Fine-Tuning

When off-the-shelf models don't capture your domain semantics well enough.

### Using Sentence Transformers
```python
from sentence_transformers import SentenceTransformer, InputExample, losses
from torch.utils.data import DataLoader

# Load base model
model = SentenceTransformer("BAAI/bge-base-en-v1.5")

# Prepare training data (query, positive_passage, negative_passage)
train_examples = [
    InputExample(texts=["What is k8s?", "Kubernetes is a container orchestration platform"]),
    InputExample(texts=["Deploy pods", "kubectl apply -f deployment.yaml creates pods"]),
]

train_dataloader = DataLoader(train_examples, shuffle=True, batch_size=16)
train_loss = losses.MultipleNegativesRankingLoss(model)

# Fine-tune
model.fit(
    train_objectives=[(train_dataloader, train_loss)],
    epochs=3,
    warmup_steps=100,
    output_path="./fine-tuned-embeddings",
)
```

### Using OpenAI Fine-Tuned Embeddings (2024)
```python
# Prepare training file (JSONL format)
# {"prompt": "query text", "completion": "relevant document text"}

from openai import OpenAI
client = OpenAI()

# Create fine-tuning job
job = client.fine_tuning.jobs.create(
    training_file="file-abc123",
    model="text-embedding-3-small",
    hyperparameters={"n_epochs": 3}
)
```

### Synthetic Training Data Generation
```python
def generate_training_pairs(documents: list[str], llm) -> list[dict]:
    """Generate (query, document) pairs using LLM."""
    pairs = []
    for doc in documents:
        # Generate questions that this document answers
        response = llm.invoke(f"""Generate 3 diverse questions that the following 
        passage could answer. Return as JSON array.
        
        Passage: {doc}""")
        
        questions = parse_json(response.content)
        for q in questions:
            pairs.append({"query": q, "positive": doc})
    
    return pairs
```

---

## Embedding Best Practices

### 1. Query vs Document Embeddings
Some models use different modes for queries and documents:
```python
# Cohere, Voyage AI, and BGE models support this
# Documents: embedded with "search_document" type
# Queries: embedded with "search_query" type

# This improves asymmetric search (short query → long document)
```

### 2. Prefix Instructions (for models that support it)
```python
# BGE models use instruction prefixes
from sentence_transformers import SentenceTransformer

model = SentenceTransformer("BAAI/bge-large-en-v1.5")

# For queries, prepend instruction
query_embedding = model.encode("Represent this sentence for searching: What is RAG?")

# For documents, no prefix needed
doc_embedding = model.encode("RAG combines retrieval with generation...")
```

### 3. Batch Processing for Efficiency
```python
from langchain_openai import OpenAIEmbeddings

embeddings_model = OpenAIEmbeddings(
    model="text-embedding-3-small",
    chunk_size=1000,  # Batch size for API calls
)

# Process all chunks at once (handles batching internally)
all_texts = [chunk.page_content for chunk in chunks]
vectors = embeddings_model.embed_documents(all_texts)
```

### 4. Caching Embeddings
```python
from langchain.embeddings import CacheBackedEmbeddings
from langchain.storage import LocalFileStore

store = LocalFileStore("./embedding_cache/")
cached_embeddings = CacheBackedEmbeddings.from_bytes_store(
    OpenAIEmbeddings(model="text-embedding-3-small"),
    store,
    namespace="text-embedding-3-small",
)
# Subsequent calls with same text return cached vectors instantly
```

---

## Late Interaction Models (ColBERT)

ColBERT produces per-token embeddings for more nuanced matching:

```python
from ragatouille import RAGPretrainedModel

# ColBERTv2 - state-of-the-art retrieval
RAG = RAGPretrainedModel.from_pretrained("colbert-ir/colbertv2.0")

# Index documents
RAG.index(
    collection=["doc1 text", "doc2 text", "doc3 text"],
    index_name="my_index",
)

# Search with late interaction (more accurate than single-vector)
results = RAG.search(query="What is RAG?", k=5)
```

**When to use ColBERT**: When you need maximum retrieval accuracy and can afford higher storage (stores per-token vectors).

---

## Multi-Vector Embeddings

Generate multiple embeddings per document for different aspects:

```python
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

llm = ChatOpenAI(model="gpt-4o-mini")
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

def multi_vector_embed(document: str) -> dict:
    """Create multiple representations of a document."""
    # 1. Original text embedding
    original_vec = embeddings.embed_query(document)
    
    # 2. Summary embedding
    summary = llm.invoke(f"Summarize in 2 sentences: {document}").content
    summary_vec = embeddings.embed_query(summary)
    
    # 3. Hypothetical questions embedding
    questions = llm.invoke(
        f"Generate 3 questions this text answers: {document}"
    ).content
    question_vec = embeddings.embed_query(questions)
    
    return {
        "original": original_vec,
        "summary": summary_vec,
        "questions": question_vec,
        "document": document,  # Store original for retrieval
    }
```

---

## Exercises

1. Compare retrieval quality across 3 embedding models using the same dataset
2. Fine-tune an embedding model on your domain data and measure improvement
3. Benchmark Matryoshka dimensions (256 vs 512 vs 1024 vs 3072) on your use case
4. Implement embedding caching and measure latency improvement
5. Try ColBERT with RAGatouille and compare to single-vector search
