# 5. Embeddings & RAG

## Embedding Models

| Model | Dimensions | Max Tokens | Cost (per 1M) | Best For |
|-------|-----------|-----------|---------------|----------|
| text-embedding-3-small | 1536 | 8191 | $0.02 | Cost-effective, most use cases |
| text-embedding-3-large | 3072 | 8191 | $0.13 | Highest quality retrieval |
| text-embedding-ada-002 | 1536 | 8191 | $0.10 | Legacy (use 3-small instead) |

## Generate Embeddings

```python
# Single text
response = client.embeddings.create(
    model="text-embedding-3-small",
    input="How do I cancel my subscription?",
)
embedding = response.data[0].embedding  # List of 1536 floats

# Batch (up to 2048 inputs)
response = client.embeddings.create(
    model="text-embedding-3-small",
    input=["text 1", "text 2", "text 3", ...],  # Up to 2048
)
embeddings = [item.embedding for item in response.data]

# Reduced dimensions (save storage, slight quality trade-off)
response = client.embeddings.create(
    model="text-embedding-3-small",
    input="Hello world",
    dimensions=512,  # Reduce from 1536 to 512
)
```

## RAG Pattern

```python
import numpy as np

class SimpleRAG:
    def __init__(self):
        self.client = OpenAI()
        self.documents = []
        self.embeddings = []
    
    def index(self, documents: list[str]):
        self.documents = documents
        response = self.client.embeddings.create(
            model="text-embedding-3-small",
            input=documents,
        )
        self.embeddings = np.array([d.embedding for d in response.data])
    
    def query(self, question: str, top_k: int = 5) -> str:
        # Embed query
        q_response = self.client.embeddings.create(
            model="text-embedding-3-small", input=[question]
        )
        q_emb = np.array(q_response.data[0].embedding)
        
        # Cosine similarity
        scores = np.dot(self.embeddings, q_emb) / (
            np.linalg.norm(self.embeddings, axis=1) * np.linalg.norm(q_emb)
        )
        top_indices = scores.argsort()[-top_k:][::-1]
        
        # Generate with context
        context = "\n\n".join([self.documents[i] for i in top_indices])
        response = self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "Answer based ONLY on context. Cite sources."},
                {"role": "user", "content": f"Context:\n{context}\n\nQuestion: {question}"},
            ],
        )
        return response.choices[0].message.content
```

---

## Next: [Realtime & Speech →](06_Realtime_Speech.md)
