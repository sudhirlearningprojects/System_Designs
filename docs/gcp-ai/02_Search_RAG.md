# 2. Vertex AI Search & RAG

## Overview

Google Cloud offers two RAG approaches:
1. **Vertex AI Search** — Fully managed search + answer generation (zero-code RAG)
2. **Vertex AI RAG Engine** — Programmatic RAG with more control (API-based)
3. **Vector Search** — Low-level vector similarity search (build your own)

---

## Vertex AI Search (Managed RAG)

### Setup (gcloud)

```bash
# Create search datastore
gcloud discovery-engine data-stores create my-docs-store \
  --location=global \
  --collection=default_collection \
  --type=CONTENT

# Import documents from GCS
gcloud discovery-engine documents import \
  --data-store=my-docs-store \
  --location=global \
  --collection=default_collection \
  --source=gs://my-bucket/documents/ \
  --auto-generate-ids

# Create search app (engine)
gcloud discovery-engine engines create my-search-app \
  --location=global \
  --collection=default_collection \
  --data-store-ids=my-docs-store \
  --search-tier=enterprise \
  --industry-vertical=GENERIC
```

### Query with Answer Generation

```python
from google.cloud import discoveryengine_v1 as discoveryengine

client = discoveryengine.SearchServiceClient()

# Search with AI-generated answer (RAG)
request = discoveryengine.SearchRequest(
    serving_config=f"projects/my-project/locations/global/collections/default_collection/engines/my-search-app/servingConfigs/default_search",
    query="What is the refund policy?",
    page_size=5,
    content_search_spec=discoveryengine.SearchRequest.ContentSearchSpec(
        summary_spec=discoveryengine.SearchRequest.ContentSearchSpec.SummarySpec(
            summary_result_count=3,
            include_citations=True,
            model_spec=discoveryengine.SearchRequest.ContentSearchSpec.SummarySpec.ModelSpec(
                version="gemini-1.5-flash-002/answer_gen/v1",
            ),
        ),
        extractive_content_spec=discoveryengine.SearchRequest.ContentSearchSpec.ExtractiveContentSpec(
            max_extractive_answer_count=3,
        ),
    ),
)

response = client.search(request)

# AI-generated answer
print(f"Answer: {response.summary.summary_text}")

# Citations
for citation in response.summary.summary_with_metadata.citations:
    print(f"  Source: {citation.sources[0].reference_index}")

# Search results
for result in response.results:
    doc = result.document
    print(f"Title: {doc.derived_struct_data.get('title', 'N/A')}")
    print(f"Snippet: {doc.derived_struct_data.get('snippets', [{}])[0].get('snippet', '')[:200]}")
```

---

## Vertex AI RAG Engine (Programmatic)

```python
from vertexai.preview import rag

# Create RAG corpus (knowledge base)
corpus = rag.create_corpus(
    display_name="support-docs",
    description="Customer support documentation",
)

# Import files from GCS
rag.import_files(
    corpus_name=corpus.name,
    paths=["gs://my-bucket/docs/"],
    chunk_size=512,
    chunk_overlap=100,
    transformation_config=rag.TransformationConfig(
        chunking_config=rag.ChunkingConfig(
            chunk_size=512,
            chunk_overlap=100,
        )
    ),
)

# Query (retrieve + generate)
response = rag.retrieval_query(
    rag_resources=[rag.RagResource(rag_corpus=corpus.name)],
    text="How do I cancel my subscription?",
    similarity_top_k=5,
    vector_distance_threshold=0.5,
)

# Retrieved contexts
for context in response.contexts.contexts:
    print(f"Score: {context.score:.3f}")
    print(f"Text: {context.text[:200]}")
    print(f"Source: {context.source_uri}")

# Use with Gemini for generation
from vertexai.generative_models import GenerativeModel, Tool
from vertexai.preview.generative_models import grounding

model = GenerativeModel("gemini-2.0-flash-001")
response = model.generate_content(
    "What is the refund policy?",
    tools=[Tool.from_retrieval(
        grounding.Retrieval(source=grounding.VertexRagStore(rag_corpora=[corpus.name], similarity_top_k=5))
    )],
)
print(response.text)
```

---

## Vertex AI Vector Search (Low-Level)

For maximum control — build your own vector search index.

```python
from google.cloud import aiplatform

aiplatform.init(project="my-project", location="us-central1")

# Create index
index = aiplatform.MatchingEngineIndex.create_tree_ah_index(
    display_name="document-embeddings",
    dimensions=768,
    approximate_neighbors_count=50,
    distance_measure_type="DOT_PRODUCT_DISTANCE",
    shard_size="SHARD_SIZE_SMALL",
)

# Create index endpoint (for serving)
index_endpoint = aiplatform.MatchingEngineIndexEndpoint.create(
    display_name="doc-search-endpoint",
    public_endpoint_enabled=True,
)

# Deploy index to endpoint
index_endpoint.deploy_index(
    index=index,
    deployed_index_id="deployed-doc-index",
    machine_type="e2-standard-2",
    min_replica_count=1,
    max_replica_count=5,
)

# Query (nearest neighbor search)
response = index_endpoint.find_neighbors(
    deployed_index_id="deployed-doc-index",
    queries=[query_embedding],  # Your query vector
    num_neighbors=10,
)

for neighbor in response[0]:
    print(f"ID: {neighbor.id}, Distance: {neighbor.distance:.4f}")
```

### Batch Update Index

```python
# Update index with new embeddings (from JSONL in GCS)
# Format: {"id": "doc-1", "embedding": [0.1, 0.2, ...], "restricts": [{"namespace": "category", "allow_list": ["billing"]}]}

index.update_embeddings(
    contents_delta_uri="gs://my-bucket/embeddings/",
    is_complete_overwrite=False,  # Incremental update
)
```

---

## Complete RAG Pipeline (GCP)

```python
import vertexai
from vertexai.generative_models import GenerativeModel, Part
from vertexai.language_models import TextEmbeddingModel, TextEmbeddingInput
from google.cloud import aiplatform

class GCPRagPipeline:
    """Production RAG using Vertex AI Vector Search + Gemini."""
    
    def __init__(self, project, location, index_endpoint_id, deployed_index_id):
        vertexai.init(project=project, location=location)
        self.embed_model = TextEmbeddingModel.from_pretrained("text-embedding-005")
        self.gen_model = GenerativeModel("gemini-2.0-flash-001")
        self.index_endpoint = aiplatform.MatchingEngineIndexEndpoint(index_endpoint_id)
        self.deployed_index_id = deployed_index_id
        self.doc_store = {}  # In production: Firestore or BigQuery
    
    def query(self, question: str, top_k: int = 5) -> dict:
        # 1. Embed query
        query_emb = self.embed_model.get_embeddings(
            [TextEmbeddingInput(text=question, task_type="RETRIEVAL_QUERY")]
        )[0].values
        
        # 2. Vector search
        neighbors = self.index_endpoint.find_neighbors(
            deployed_index_id=self.deployed_index_id,
            queries=[query_emb],
            num_neighbors=top_k,
        )[0]
        
        # 3. Fetch document content
        contexts = []
        for neighbor in neighbors:
            doc = self.doc_store.get(neighbor.id, {"text": "Unknown"})
            contexts.append(doc["text"])
        
        # 4. Generate with Gemini
        context_str = "\n\n---\n\n".join(contexts)
        response = self.gen_model.generate_content(
            f"Context:\n{context_str}\n\nQuestion: {question}\n\nAnswer based ONLY on context. Cite sources.",
            generation_config={"temperature": 0, "max_output_tokens": 500},
        )
        
        return {
            "answer": response.text,
            "sources": [{"id": n.id, "score": n.distance} for n in neighbors],
        }
```

---

## Next: [Vertex AI Agents →](03_Agents.md)
