# 2. Azure AI Search (formerly Cognitive Search)

## Overview

Azure AI Search is a fully managed search service with vector search, semantic ranking, and integrated AI enrichment — the backbone for enterprise RAG systems.

### Key Capabilities

| Feature | Description |
|---------|-------------|
| **Vector Search** | Store and query embeddings (HNSW, exhaustive KNN) |
| **Hybrid Search** | Combine keyword (BM25) + vector in one query |
| **Semantic Ranking** | Microsoft's cross-encoder reranker (L2 reranking) |
| **AI Enrichment** | Auto-extract text from PDFs, images, Office docs |
| **Integrated Vectorization** | Auto-embed documents using Azure OpenAI |
| **Skillsets** | Pipeline of AI transformations during indexing |

---

## Setup (Azure CLI)

```bash
# Create search service
az search service create \
  --name my-ai-search \
  --resource-group rg-ai \
  --sku standard \
  --location eastus \
  --partition-count 1 \
  --replica-count 1

# Get admin key
az search admin-key show \
  --service-name my-ai-search \
  --resource-group rg-ai

# Get endpoint
# https://my-ai-search.search.windows.net
```

### SKU Selection

| SKU | Vectors | Storage | Use Case |
|-----|---------|---------|----------|
| Free | 3 indexes, no vector | 50 MB | Testing |
| Basic | 5 indexes, vector | 2 GB | Small apps |
| Standard (S1) | 50 indexes, vector | 25 GB | Production |
| Standard (S2) | 200 indexes, vector | 100 GB | Large scale |
| Standard (S3) | 200 indexes, vector | 200 GB | Enterprise |

---

## Create Index with Vector Fields

```python
# pip install azure-search-documents

from azure.search.documents.indexes import SearchIndexClient
from azure.search.documents.indexes.models import (
    SearchIndex, SearchField, SearchFieldDataType,
    VectorSearch, HnswAlgorithmConfiguration, VectorSearchProfile,
    SemanticConfiguration, SemanticSearch, SemanticPrioritizedFields, SemanticField,
    SearchableField, SimpleField, FilterableField,
)
from azure.core.credentials import AzureKeyCredential

# Client
endpoint = "https://my-ai-search.search.windows.net"
credential = AzureKeyCredential("your-admin-key")
index_client = SearchIndexClient(endpoint=endpoint, credential=credential)

# Define index schema
index = SearchIndex(
    name="documents",
    fields=[
        SimpleField(name="id", type=SearchFieldDataType.String, key=True, filterable=True),
        SearchableField(name="content", type=SearchFieldDataType.String, analyzer_name="en.microsoft"),
        SearchableField(name="title", type=SearchFieldDataType.String),
        FilterableField(name="category", type=SearchFieldDataType.String, facetable=True),
        FilterableField(name="source", type=SearchFieldDataType.String),
        SimpleField(name="last_updated", type=SearchFieldDataType.DateTimeOffset, filterable=True),
        # Vector field
        SearchField(
            name="content_vector",
            type=SearchFieldDataType.Collection(SearchFieldDataType.Single),
            searchable=True,
            vector_search_dimensions=1536,  # text-embedding-3-small
            vector_search_profile_name="my-vector-profile",
        ),
    ],
    # Vector search configuration
    vector_search=VectorSearch(
        algorithms=[
            HnswAlgorithmConfiguration(name="my-hnsw", parameters={"m": 4, "efConstruction": 400, "efSearch": 500}),
        ],
        profiles=[
            VectorSearchProfile(name="my-vector-profile", algorithm_configuration_name="my-hnsw"),
        ],
    ),
    # Semantic search configuration (L2 reranking)
    semantic_search=SemanticSearch(
        configurations=[
            SemanticConfiguration(
                name="my-semantic-config",
                prioritized_fields=SemanticPrioritizedFields(
                    title_field=SemanticField(field_name="title"),
                    content_fields=[SemanticField(field_name="content")],
                ),
            )
        ]
    ),
)

# Create index
index_client.create_or_update_index(index)
print(f"Index '{index.name}' created")
```

---

## Index Documents

```python
from azure.search.documents import SearchClient
from openai import AzureOpenAI

search_client = SearchClient(endpoint=endpoint, index_name="documents", credential=credential)
openai_client = AzureOpenAI(azure_endpoint=aoai_endpoint, api_key=aoai_key, api_version="2024-10-21")

def embed_text(text: str) -> list[float]:
    """Generate embedding using Azure OpenAI."""
    response = openai_client.embeddings.create(model="text-embedding-3-small", input=[text])
    return response.data[0].embedding

# Prepare documents
documents = [
    {
        "id": "doc-1",
        "title": "Cancellation Policy",
        "content": "You can cancel your subscription at any time. Go to Settings > Subscription > Cancel.",
        "category": "billing",
        "source": "help-center",
        "last_updated": "2024-01-15T00:00:00Z",
        "content_vector": embed_text("You can cancel your subscription at any time..."),
    },
    {
        "id": "doc-2",
        "title": "Refund Policy",
        "content": "Refunds are available within 14 days of purchase. Contact support for processing.",
        "category": "billing",
        "source": "help-center",
        "last_updated": "2024-01-10T00:00:00Z",
        "content_vector": embed_text("Refunds are available within 14 days..."),
    },
]

# Upload (supports up to 1000 docs per batch)
result = search_client.upload_documents(documents)
print(f"Uploaded {len(result)} documents")
```

---

## Search Queries

### Vector Search

```python
from azure.search.documents.models import VectorizedQuery

query = "How do I get my money back?"
query_vector = embed_text(query)

results = search_client.search(
    search_text=None,  # No keyword search
    vector_queries=[
        VectorizedQuery(
            vector=query_vector,
            k_nearest_neighbors=5,
            fields="content_vector",
        )
    ],
)

for result in results:
    print(f"Score: {result['@search.score']:.4f} | {result['title']}: {result['content'][:100]}")
```

### Hybrid Search (Vector + Keyword)

```python
# Best of both worlds: semantic understanding + exact term matching
results = search_client.search(
    search_text="refund 14 days",  # Keyword (BM25)
    vector_queries=[
        VectorizedQuery(vector=query_vector, k_nearest_neighbors=5, fields="content_vector")
    ],
    select=["id", "title", "content", "category"],
    filter="category eq 'billing'",  # OData filter
    top=5,
)
```

### Hybrid + Semantic Ranking (Best Quality)

```python
results = search_client.search(
    search_text="How do I get a refund?",
    vector_queries=[
        VectorizedQuery(vector=query_vector, k_nearest_neighbors=50, fields="content_vector")
    ],
    query_type="semantic",
    semantic_configuration_name="my-semantic-config",
    top=5,
)

for result in results:
    print(f"Score: {result['@search.reranker_score']:.4f}")  # Semantic reranker score
    print(f"Title: {result['title']}")
    print(f"Captions: {result.get('@search.captions', [])}")  # Extractive captions
```

---

## Complete RAG Implementation

```python
from openai import AzureOpenAI
from azure.search.documents import SearchClient
from azure.search.documents.models import VectorizedQuery

class AzureRAG:
    """Production RAG using Azure AI Search + Azure OpenAI."""
    
    def __init__(self, search_endpoint, search_key, index_name, aoai_endpoint, aoai_key):
        self.search_client = SearchClient(
            endpoint=search_endpoint, index_name=index_name,
            credential=AzureKeyCredential(search_key)
        )
        self.openai_client = AzureOpenAI(
            azure_endpoint=aoai_endpoint, api_key=aoai_key, api_version="2024-10-21"
        )
    
    def query(self, question: str, top_k: int = 5, filter: str = None) -> dict:
        # 1. Embed query
        query_vector = self._embed(question)
        
        # 2. Hybrid search with semantic ranking
        results = self.search_client.search(
            search_text=question,
            vector_queries=[
                VectorizedQuery(vector=query_vector, k_nearest_neighbors=50, fields="content_vector")
            ],
            query_type="semantic",
            semantic_configuration_name="my-semantic-config",
            filter=filter,
            top=top_k,
        )
        
        # 3. Format context
        sources = []
        context_parts = []
        for r in results:
            sources.append({"title": r["title"], "id": r["id"], "score": r.get("@search.reranker_score", 0)})
            context_parts.append(f"[{r['title']}]: {r['content']}")
        
        context = "\n\n".join(context_parts)
        
        # 4. Generate answer
        response = self.openai_client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "Answer based ONLY on the provided context. Cite sources. If you can't answer, say so."},
                {"role": "user", "content": f"Context:\n{context}\n\nQuestion: {question}"},
            ],
            temperature=0,
            max_tokens=500,
        )
        
        return {
            "answer": response.choices[0].message.content,
            "sources": sources,
            "tokens_used": response.usage.total_tokens,
        }
    
    def _embed(self, text: str) -> list[float]:
        response = self.openai_client.embeddings.create(model="text-embedding-3-small", input=[text])
        return response.data[0].embedding

# Usage
rag = AzureRAG(
    search_endpoint="https://my-search.search.windows.net",
    search_key="...",
    index_name="documents",
    aoai_endpoint="https://my-aoai.openai.azure.com/",
    aoai_key="...",
)

result = rag.query("What is the refund policy?", filter="category eq 'billing'")
print(result["answer"])
print(f"Sources: {result['sources']}")
```

---

## "On Your Data" (Zero-Code RAG)

Azure OpenAI can directly query AI Search without custom code:

```python
response = openai_client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "What is the refund policy?"}],
    extra_body={
        "data_sources": [{
            "type": "azure_search",
            "parameters": {
                "endpoint": "https://my-search.search.windows.net",
                "index_name": "documents",
                "authentication": {"type": "api_key", "key": search_key},
                "query_type": "vector_semantic_hybrid",
                "embedding_dependency": {
                    "type": "deployment_name",
                    "deployment_name": "text-embedding-3-small",
                },
                "semantic_configuration": "my-semantic-config",
                "top_n_documents": 5,
            }
        }]
    }
)

print(response.choices[0].message.content)
# Includes citations automatically!
print(response.choices[0].message.context["citations"])
```

---

## Integrated Vectorization (Auto-Embed)

```bash
# Create indexer that auto-embeds documents using Azure OpenAI
# No need to call embedding API yourself!

# 1. Create data source (Blob Storage)
# 2. Create skillset with Azure OpenAI embedding skill
# 3. Create indexer that runs the pipeline

# Documents in Blob → AI Search extracts text → Embeds → Indexes
# All automatic, scheduled (e.g., every hour)
```

---

## Next: [Azure AI Studio →](03_AI_Studio.md)
