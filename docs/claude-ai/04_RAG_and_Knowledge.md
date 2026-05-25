# 4. RAG & Knowledge Systems

## RAG Architecture with Claude

```
┌──────────────────────────────────────────────────────────────┐
│                     RAG PIPELINE                               │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  INGESTION (Offline)                                          │
│  Documents → Chunk → Embed → Store in Vector DB              │
│                                                               │
│  RETRIEVAL (Online)                                           │
│  Query → Embed → Search Vector DB → Top-K chunks             │
│                                                               │
│  GENERATION                                                   │
│  System Prompt + Retrieved Chunks + User Query → Claude → Answer │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## Chunking Strategies

### Recursive Character Splitting

```python
from langchain.text_splitter import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,        # ~250 tokens
    chunk_overlap=200,      # Overlap for context continuity
    separators=["\n\n", "\n", ". ", " ", ""],
    length_function=len,
)

chunks = splitter.split_text(document_text)
```

### Semantic Chunking

```python
import numpy as np
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')

def semantic_chunk(text: str, max_chunk_size: int = 1000, similarity_threshold: float = 0.7):
    """Split text at semantic boundaries (where topic changes)."""
    sentences = text.split('. ')
    embeddings = model.encode(sentences)
    
    chunks = []
    current_chunk = [sentences[0]]
    
    for i in range(1, len(sentences)):
        similarity = np.dot(embeddings[i], embeddings[i-1]) / (
            np.linalg.norm(embeddings[i]) * np.linalg.norm(embeddings[i-1])
        )
        
        current_text = '. '.join(current_chunk)
        if similarity < similarity_threshold or len(current_text) > max_chunk_size:
            chunks.append(current_text)
            current_chunk = [sentences[i]]
        else:
            current_chunk.append(sentences[i])
    
    if current_chunk:
        chunks.append('. '.join(current_chunk))
    
    return chunks
```

### Contextual Chunking (Anthropic's Approach)

```python
# Add context to each chunk using Claude (Contextual Retrieval)
async def add_context_to_chunk(chunk: str, full_document: str) -> str:
    """Use Claude to generate context for each chunk."""
    response = await client.messages.create(
        model="claude-3-5-haiku-20241022",  # Fast + cheap for this
        max_tokens=200,
        messages=[{
            "role": "user",
            "content": f"""<document>
{full_document[:20000]}
</document>

<chunk>
{chunk}
</chunk>

Provide a brief context (1-2 sentences) explaining where this chunk fits 
within the overall document. This context will be prepended to the chunk 
for better retrieval. Be specific and concise."""
        }]
    )
    context = response.content[0].text
    return f"{context}\n\n{chunk}"
```

---

## Embedding & Vector Storage

### Using Voyage AI (Anthropic's Recommended Embeddings)

```python
import voyageai

vo = voyageai.Client()  # Uses VOYAGE_API_KEY

# Embed documents
doc_embeddings = vo.embed(
    texts=chunks,
    model="voyage-3",  # Best for retrieval
    input_type="document"
).embeddings

# Embed query
query_embedding = vo.embed(
    texts=["How do I export a PDF in Photoshop?"],
    model="voyage-3",
    input_type="query"
).embeddings[0]
```

### Vector DB Integration (Pinecone)

```python
from pinecone import Pinecone

pc = Pinecone(api_key="...")
index = pc.Index("adobe-knowledge-base")

# Upsert chunks with metadata
vectors = [
    {
        "id": f"chunk-{i}",
        "values": embedding,
        "metadata": {
            "text": chunk,
            "source": "photoshop-help-docs",
            "product": "photoshop",
            "section": "export",
            "last_updated": "2024-01-15"
        }
    }
    for i, (chunk, embedding) in enumerate(zip(chunks, doc_embeddings))
]
index.upsert(vectors=vectors, batch_size=100)

# Query
results = index.query(
    vector=query_embedding,
    top_k=5,
    include_metadata=True,
    filter={"product": {"$eq": "photoshop"}}
)
```

---

## Complete RAG Implementation

```python
import anthropic
import voyageai

class ClaudeRAG:
    def __init__(self):
        self.claude = anthropic.Anthropic()
        self.voyage = voyageai.Client()
        self.index = self._init_vector_db()
    
    def answer(self, query: str, product: str = None, top_k: int = 5) -> str:
        # 1. Retrieve relevant chunks
        chunks = self._retrieve(query, product, top_k)
        
        # 2. Build context
        context = self._format_context(chunks)
        
        # 3. Generate answer with Claude
        response = self.claude.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=2048,
            system="""You are a helpful assistant that answers questions based on the provided context.

<rules>
- ONLY use information from the provided context to answer
- If the context doesn't contain the answer, say "I don't have information about that in my knowledge base"
- Cite your sources using [Source: filename] format
- Be concise and actionable
- If the user's question is ambiguous, ask for clarification
</rules>""",
            messages=[{
                "role": "user",
                "content": f"""<context>
{context}
</context>

<question>
{query}
</question>

Answer the question based ONLY on the provided context. Cite sources."""
            }]
        )
        
        return response.content[0].text
    
    def _retrieve(self, query: str, product: str, top_k: int) -> list:
        # Embed query
        query_emb = self.voyage.embed(
            texts=[query], model="voyage-3", input_type="query"
        ).embeddings[0]
        
        # Search with optional filter
        filter_dict = {"product": {"$eq": product}} if product else None
        results = self.index.query(
            vector=query_emb, top_k=top_k,
            include_metadata=True, filter=filter_dict
        )
        
        return [match.metadata for match in results.matches]
    
    def _format_context(self, chunks: list) -> str:
        formatted = []
        for i, chunk in enumerate(chunks, 1):
            formatted.append(
                f"<source id=\"{i}\" file=\"{chunk['source']}\">\n{chunk['text']}\n</source>"
            )
        return "\n\n".join(formatted)
```

---

## Hybrid Search (BM25 + Semantic)

```python
from rank_bm25 import BM25Okapi

class HybridRetriever:
    def __init__(self, chunks: list, embeddings: list):
        self.chunks = chunks
        self.embeddings = embeddings
        # BM25 for keyword matching
        tokenized = [chunk.lower().split() for chunk in chunks]
        self.bm25 = BM25Okapi(tokenized)
    
    def search(self, query: str, top_k: int = 5, alpha: float = 0.7) -> list:
        """Hybrid search: alpha * semantic + (1-alpha) * BM25"""
        # Semantic scores
        query_emb = embed_query(query)
        semantic_scores = cosine_similarity([query_emb], self.embeddings)[0]
        
        # BM25 scores
        bm25_scores = self.bm25.get_scores(query.lower().split())
        
        # Normalize both to [0, 1]
        semantic_norm = (semantic_scores - semantic_scores.min()) / (semantic_scores.max() - semantic_scores.min() + 1e-8)
        bm25_norm = (bm25_scores - bm25_scores.min()) / (bm25_scores.max() - bm25_scores.min() + 1e-8)
        
        # Combine
        combined = alpha * semantic_norm + (1 - alpha) * bm25_norm
        
        # Return top-k
        top_indices = combined.argsort()[-top_k:][::-1]
        return [{"text": self.chunks[i], "score": combined[i]} for i in top_indices]
```

---

## Reranking

```python
# Use Voyage AI reranker or Cohere reranker for better precision
reranked = vo.rerank(
    query="How to export PDF in Photoshop",
    documents=[chunk["text"] for chunk in retrieved_chunks],
    model="rerank-2",
    top_k=3
)

# Or use Claude itself as a reranker
def claude_rerank(query: str, chunks: list, top_k: int = 3) -> list:
    response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=500,
        messages=[{
            "role": "user",
            "content": f"""Given the query: "{query}"

Rank these passages by relevance (most relevant first). Return ONLY the indices.

{chr(10).join(f'[{i}] {chunk[:200]}' for i, chunk in enumerate(chunks))}

Return format: [index1, index2, index3, ...]"""
        }]
    )
    indices = json.loads(response.content[0].text)
    return [chunks[i] for i in indices[:top_k]]
```

---

## Evaluation

```python
# Evaluate RAG quality using Claude as judge
def evaluate_rag_response(query: str, response: str, ground_truth: str, context: str) -> dict:
    eval_response = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=500,
        messages=[{
            "role": "user",
            "content": f"""Evaluate this RAG system response.

<query>{query}</query>
<context_provided>{context[:2000]}</context_provided>
<system_response>{response}</system_response>
<ground_truth>{ground_truth}</ground_truth>

Score each dimension 1-5:
1. Faithfulness: Does the response only use information from the context? (no hallucination)
2. Relevance: Does it answer the user's question?
3. Completeness: Does it cover all important aspects?
4. Conciseness: Is it appropriately brief without losing information?

Respond as JSON: {{"faithfulness": N, "relevance": N, "completeness": N, "conciseness": N, "explanation": "..."}}"""
        }]
    )
    return json.loads(eval_response.content[0].text)
```

---

## Next: [Building AI Agents →](05_Building_Agents.md)
