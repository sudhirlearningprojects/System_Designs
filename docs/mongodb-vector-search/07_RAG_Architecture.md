# Module 7: RAG Architecture with MongoDB

---

## 7.1 What is RAG?

**Retrieval-Augmented Generation (RAG)** grounds LLM responses in your private data:

```
┌───────────────────────────────────────────────────────────────┐
│                        RAG Pipeline                             │
│                                                                 │
│  User Query ──→ Embed Query ──→ Vector Search ──→ Retrieve     │
│       │                              │              Documents   │
│       │                              │                │         │
│       │                              │                ▼         │
│       └─────────────────────────────────────→ LLM Prompt       │
│                                               (Query + Context) │
│                                                     │          │
│                                                     ▼          │
│                                              Generated Answer   │
└───────────────────────────────────────────────────────────────┘
```

**Why RAG instead of fine-tuning?**
- No training cost
- Data stays current (real-time updates)
- Source attribution (citations)
- No hallucination from training data
- Works with any LLM

---

## 7.2 Basic RAG Implementation

```python
from openai import OpenAI
from config import collection, get_embedding

openai_client = OpenAI()

def rag_query(question: str, limit: int = 5) -> dict:
    """Complete RAG pipeline: retrieve → augment → generate."""
    
    # Step 1: Retrieve relevant documents
    query_embedding = get_embedding(question)
    
    results = list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 150,
                "limit": limit
            }
        },
        {
            "$project": {
                "content": 1,
                "title": 1,
                "score": {"$meta": "vectorSearchScore"},
                "_id": 0
            }
        }
    ]))
    
    # Step 2: Build context from retrieved documents
    context = "\n\n---\n\n".join([
        f"Source: {r['title']}\n{r['content']}" for r in results
    ])
    
    # Step 3: Generate answer with LLM
    response = openai_client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {
                "role": "system",
                "content": """You are a helpful assistant. Answer the user's question based ONLY on the provided context. 
If the context doesn't contain the answer, say "I don't have enough information to answer this."
Always cite which source document your answer comes from."""
            },
            {
                "role": "user",
                "content": f"Context:\n{context}\n\n---\n\nQuestion: {question}"
            }
        ],
        temperature=0.1,
        max_tokens=1000
    )
    
    return {
        "answer": response.choices[0].message.content,
        "sources": [{"title": r["title"], "score": r["score"]} for r in results],
        "tokens_used": response.usage.total_tokens
    }
```

---

## 7.3 Advanced RAG Patterns

### Pattern 1: Query Expansion (Multi-Query RAG)

Generate multiple search queries to improve recall:

```python
def multi_query_rag(question: str) -> dict:
    # Generate alternative queries
    expansion = openai_client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{
            "role": "user",
            "content": f"""Generate 3 alternative search queries for: "{question}"
Return as JSON array of strings. Make them semantically diverse."""
        }],
        response_format={"type": "json_object"},
        temperature=0.7
    )
    
    queries = json.loads(expansion.choices[0].message.content)["queries"]
    queries.append(question)  # Include original
    
    # Search with all queries, deduplicate results
    all_results = {}
    for q in queries:
        embedding = get_embedding(q)
        results = list(collection.aggregate([
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": embedding,
                    "numCandidates": 100,
                    "limit": 5
                }
            },
            {"$addFields": {"score": {"$meta": "vectorSearchScore"}}}
        ]))
        for r in results:
            doc_id = str(r["_id"])
            if doc_id not in all_results or r["score"] > all_results[doc_id]["score"]:
                all_results[doc_id] = r
    
    # Take top results by score
    top_results = sorted(all_results.values(), key=lambda x: x["score"], reverse=True)[:5]
    
    # Generate answer with enriched context
    context = "\n\n---\n\n".join([r["content"] for r in top_results])
    return generate_answer(question, context, top_results)
```

### Pattern 2: Contextual Compression

Compress retrieved chunks to only relevant parts:

```python
def compressed_rag(question: str) -> dict:
    # Retrieve more documents than needed
    results = vector_search(question, limit=10)
    
    # Compress each result to only relevant sentences
    compressed_contexts = []
    for r in results:
        compression = openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{
                "role": "user",
                "content": f"""Extract ONLY the sentences relevant to the question from this text.
Question: {question}
Text: {r['content']}
Return only the relevant sentences, nothing else. If nothing is relevant, return "NOT_RELEVANT"."""
            }],
            temperature=0,
            max_tokens=300
        )
        compressed = compression.choices[0].message.content
        if compressed != "NOT_RELEVANT":
            compressed_contexts.append(compressed)
    
    context = "\n\n".join(compressed_contexts[:5])
    return generate_answer(question, context)
```

### Pattern 3: Parent-Child Retrieval

Retrieve small chunks but return parent (larger) context:

```python
def parent_child_rag(question: str) -> dict:
    """Search on small chunks, return parent documents for context."""
    query_embedding = get_embedding(question)
    
    # Search child chunks
    child_results = list(collection.aggregate([
        {
            "$vectorSearch": {
                "index": "vector_index",
                "path": "embedding",
                "queryVector": query_embedding,
                "numCandidates": 150,
                "limit": 5
            }
        },
        {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        
        # Lookup parent document
        {
            "$lookup": {
                "from": "parent_documents",
                "localField": "metadata.source_id",
                "foreignField": "_id",
                "as": "parent"
            }
        },
        {"$unwind": "$parent"},
        
        # Return parent content (larger context)
        {
            "$project": {
                "child_content": "$content",
                "parent_content": "$parent.content",
                "title": "$parent.title",
                "score": 1
            }
        }
    ]))
    
    # Use parent content for LLM (more context)
    context = "\n\n---\n\n".join([r["parent_content"] for r in child_results])
    return generate_answer(question, context)
```

### Pattern 4: Self-RAG (Reflective)

LLM evaluates retrieval quality and decides to re-search if needed:

```python
def self_rag(question: str, max_iterations: int = 3) -> dict:
    for i in range(max_iterations):
        results = vector_search(question, limit=5)
        context = "\n\n".join([r["content"] for r in results])
        
        # Ask LLM to evaluate if context is sufficient
        evaluation = openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{
                "role": "user",
                "content": f"""Evaluate if this context can answer the question.
Question: {question}
Context: {context[:2000]}

Respond with JSON: {{"sufficient": true/false, "missing": "what information is missing", "refined_query": "better search query if not sufficient"}}"""
            }],
            response_format={"type": "json_object"},
            temperature=0
        )
        
        eval_result = json.loads(evaluation.choices[0].message.content)
        
        if eval_result["sufficient"]:
            return generate_answer(question, context, results)
        
        # Refine query and search again
        question = eval_result["refined_query"]
    
    # Final attempt with best available context
    return generate_answer(question, context, results)
```

---

## 7.4 RAG with LangChain + MongoDB

```python
from langchain_mongodb import MongoDBAtlasVectorSearch
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate
from pymongo import MongoClient

# Setup
client = MongoClient("mongodb+srv://...")
collection = client["mydb"]["documents"]

vector_store = MongoDBAtlasVectorSearch(
    collection=collection,
    embedding=OpenAIEmbeddings(model="text-embedding-3-small"),
    index_name="vector_index",
    text_key="content",
    embedding_key="embedding"
)

# Create retriever
retriever = vector_store.as_retriever(
    search_type="similarity",
    search_kwargs={"k": 5, "score_threshold": 0.7}
)

# Custom prompt
prompt = PromptTemplate(
    template="""Use the following context to answer the question. If you cannot find the answer, say so.

Context: {context}

Question: {question}

Answer:""",
    input_variables=["context", "question"]
)

# RAG chain
qa_chain = RetrievalQA.from_chain_type(
    llm=ChatOpenAI(model="gpt-4o", temperature=0),
    chain_type="stuff",
    retriever=retriever,
    chain_type_kwargs={"prompt": prompt},
    return_source_documents=True
)

# Query
result = qa_chain.invoke({"query": "How does MongoDB handle sharding?"})
print(result["result"])
```

---

## 7.5 RAG with LlamaIndex + MongoDB

```python
from llama_index.core import VectorStoreIndex, StorageContext
from llama_index.vector_stores.mongodb import MongoDBAtlasVectorSearch
from llama_index.embeddings.openai import OpenAIEmbedding
from llama_index.llms.openai import OpenAI
from pymongo import MongoClient

# Setup
mongo_client = MongoClient("mongodb+srv://...")
store = MongoDBAtlasVectorSearch(
    mongo_client,
    db_name="mydb",
    collection_name="documents",
    vector_index_name="vector_index"
)

storage_context = StorageContext.from_defaults(vector_store=store)

# Create index
index = VectorStoreIndex.from_vector_store(
    vector_store=store,
    embed_model=OpenAIEmbedding(model="text-embedding-3-small")
)

# Query engine
query_engine = index.as_query_engine(
    llm=OpenAI(model="gpt-4o", temperature=0),
    similarity_top_k=5
)

response = query_engine.query("What is MongoDB Atlas Vector Search?")
print(response.response)
print(f"Sources: {[n.metadata for n in response.source_nodes]}")
```

---

## 7.6 Conversational RAG (Chat with Memory)

```python
from collections import deque

class ConversationalRAG:
    def __init__(self, max_history: int = 10):
        self.history = deque(maxlen=max_history)
    
    def chat(self, question: str) -> str:
        # Step 1: Contextualize question with history
        if self.history:
            history_text = "\n".join([f"User: {h['q']}\nAssistant: {h['a']}" for h in self.history])
            contextualized = openai_client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[{
                    "role": "user",
                    "content": f"""Given this conversation history, rewrite the user's question as a standalone question.
History: {history_text}
Latest question: {question}
Standalone question:"""
                }],
                temperature=0, max_tokens=200
            ).choices[0].message.content
        else:
            contextualized = question
        
        # Step 2: Vector search with contextualized query
        embedding = get_embedding(contextualized)
        results = list(collection.aggregate([
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": embedding,
                    "numCandidates": 150,
                    "limit": 5
                }
            },
            {"$project": {"content": 1, "title": 1, "score": {"$meta": "vectorSearchScore"}}}
        ]))
        
        context = "\n\n".join([r["content"] for r in results])
        
        # Step 3: Generate response with history
        messages = [
            {"role": "system", "content": "Answer based on the provided context. Be conversational."}
        ]
        for h in self.history:
            messages.append({"role": "user", "content": h["q"]})
            messages.append({"role": "assistant", "content": h["a"]})
        messages.append({"role": "user", "content": f"Context:\n{context}\n\nQuestion: {question}"})
        
        response = openai_client.chat.completions.create(
            model="gpt-4o", messages=messages, temperature=0.2
        )
        answer = response.choices[0].message.content
        
        # Step 4: Save to history
        self.history.append({"q": question, "a": answer})
        return answer

# Usage
rag = ConversationalRAG()
print(rag.chat("What is vector search?"))
print(rag.chat("How does it compare to full-text search?"))  # Uses context from previous
print(rag.chat("Show me an example"))  # Understands "it" = vector search
```

---

## 7.7 RAG Evaluation

```python
def evaluate_rag(test_cases: list[dict]) -> dict:
    """Evaluate RAG quality with automated metrics."""
    scores = {"relevance": [], "faithfulness": [], "answer_quality": []}
    
    for case in test_cases:
        result = rag_query(case["question"])
        
        # Evaluate with LLM-as-judge
        evaluation = openai_client.chat.completions.create(
            model="gpt-4o",
            messages=[{
                "role": "user",
                "content": f"""Evaluate this RAG response. Score each 1-5.

Question: {case['question']}
Expected Answer: {case.get('expected', 'N/A')}
Generated Answer: {result['answer']}
Retrieved Sources: {[s['title'] for s in result['sources']]}

Rate as JSON:
- relevance: Are retrieved sources relevant to the question? (1-5)
- faithfulness: Does the answer only use info from sources? (1-5)  
- answer_quality: Is the answer correct and complete? (1-5)"""
            }],
            response_format={"type": "json_object"},
            temperature=0
        )
        
        eval_scores = json.loads(evaluation.choices[0].message.content)
        for key in scores:
            scores[key].append(eval_scores[key])
    
    return {k: sum(v)/len(v) for k, v in scores.items()}
```

---

## 7.8 RAG Best Practices

| Practice | Why |
|----------|-----|
| Chunk size 500-1000 tokens | Balance between specificity and context |
| Overlap 10-20% | Prevent information loss at boundaries |
| Add metadata to chunks | Enable filtered retrieval |
| Use contextual embedding | Prepend title/section to text before embedding |
| Retrieve 5-10 documents | Balance context quality vs token cost |
| Score threshold filtering | Remove low-relevance results (< 0.7) |
| Cite sources | Build trust, enable verification |
| Monitor and evaluate | Track answer quality, retrieval precision |
| Cache frequent queries | Reduce latency and cost |
| Handle "no answer" gracefully | When context is insufficient |

---

## Next: [Module 8 — Advanced Patterns →](08_Advanced_Patterns.md)
