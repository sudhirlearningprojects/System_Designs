# 3. Retrieval & Query Engines

## Theory: Retrieval is the Bottleneck

In RAG systems, **retrieval quality determines answer quality**. A perfect LLM cannot generate a correct answer if the relevant information was never retrieved.

```
Retrieval Quality → Answer Quality

Good retrieval + Good LLM = Great answers ✅
Good retrieval + Bad LLM  = Decent answers (LLM just summarizes)
Bad retrieval  + Good LLM = Hallucination ❌ (LLM invents from bad context)
Bad retrieval  + Bad LLM  = Garbage ❌❌
```

### Retriever Types

| Retriever | How It Works | Precision | Recall | Speed |
|-----------|-------------|-----------|--------|-------|
| **Vector** | Cosine similarity on embeddings | Medium | High | Fast |
| **BM25** | Term frequency matching | High (exact) | Low (semantic) | Fast |
| **Hybrid** | Vector + BM25 combined | High | High | Medium |
| **Auto-Merging** | Retrieve small, return big | High | High | Medium |
| **Recursive** | Follow node relationships | Very High | Medium | Slow |
| **Knowledge Graph** | Graph traversal | High (relations) | Medium | Medium |
| **Router** | Route to best retriever | Depends | Depends | Varies |

---

## Retrievers

```python
from llama_index.core import VectorStoreIndex

# Basic vector retriever
retriever = index.as_retriever(
    similarity_top_k=5,
    # Filters
    filters=MetadataFilters(
        filters=[MetadataFilter(key="category", value="billing")]
    ),
)

nodes = retriever.retrieve("How do I get a refund?")
for node in nodes:
    print(f"Score: {node.score:.3f} | {node.text[:100]}...")
```

### Router Retriever

```python
from llama_index.core.retrievers import RouterRetriever
from llama_index.core.selectors import LLMSingleSelector
from llama_index.core.tools import RetrieverTool

# Route queries to the most appropriate retriever
tools = [
    RetrieverTool.from_defaults(
        retriever=billing_index.as_retriever(),
        description="Retrieves billing, payment, and subscription information",
    ),
    RetrieverTool.from_defaults(
        retriever=technical_index.as_retriever(),
        description="Retrieves technical documentation, troubleshooting guides",
    ),
    RetrieverTool.from_defaults(
        retriever=policy_index.as_retriever(),
        description="Retrieves company policies, terms of service, privacy policy",
    ),
]

retriever = RouterRetriever(
    selector=LLMSingleSelector.from_defaults(),
    retriever_tools=tools,
)

# LLM decides which retriever to use based on query
nodes = retriever.retrieve("What's your data retention policy?")
# → Routes to policy_index
```

---

## Query Engines

```python
from llama_index.core.query_engine import RetrieverQueryEngine
from llama_index.core.response_synthesizers import get_response_synthesizer
from llama_index.core.postprocessor import SimilarityPostprocessor

# Full control over query engine components
query_engine = RetrieverQueryEngine(
    retriever=hybrid_retriever,
    response_synthesizer=get_response_synthesizer(
        response_mode="compact",
        use_async=True,
    ),
    node_postprocessors=[
        SimilarityPostprocessor(similarity_cutoff=0.5),
        reranker,
    ],
)

# Query with metadata
response = query_engine.query("How do I export a PDF?")
print(response.response)                    # Generated answer
print(response.source_nodes)               # Retrieved sources
print(response.metadata)                   # Query metadata
```

### Response Modes

```python
# Compact: Stuff all context into one prompt (fast, simple)
synthesizer = get_response_synthesizer(response_mode="compact")

# Refine: Iterate through nodes, refining answer each time (thorough)
synthesizer = get_response_synthesizer(response_mode="refine")

# Tree Summarize: Summarize in tree structure (many nodes)
synthesizer = get_response_synthesizer(response_mode="tree_summarize")

# Custom prompt
from llama_index.core.prompts import PromptTemplate

custom_prompt = PromptTemplate(
    "Context:\n{context_str}\n\nQuestion: {query_str}\n\n"
    "Answer concisely. If you can't answer from context, say 'I don't know'.\n"
    "Always cite your sources using [Source: filename].\n\nAnswer:"
)

synthesizer = get_response_synthesizer(
    response_mode="compact",
    text_qa_template=custom_prompt,
)
```

---

## Query Transformations

```python
from llama_index.core.query_engine import (
    SubQuestionQueryEngine,
    MultiStepQueryEngine,
)
from llama_index.core.indices.query.query_transform import HyDEQueryTransform

# Sub-question decomposition
# "Compare pricing of Pro vs Enterprise" →
#   Sub-Q1: "What is Pro plan pricing?"
#   Sub-Q2: "What is Enterprise plan pricing?"
sub_question_engine = SubQuestionQueryEngine.from_defaults(
    query_engine_tools=tools,
    use_async=True,
)

# Multi-step (iterative refinement)
# Asks follow-up questions based on initial answer
multi_step_engine = MultiStepQueryEngine(
    query_engine=base_engine,
    query_transform=StepDecomposeQueryTransform(),
    num_steps=3,
)

# HyDE (Hypothetical Document Embeddings)
hyde_engine = TransformQueryEngine(
    query_engine=base_engine,
    query_transform=HyDEQueryTransform(include_original=True),
)
```

---

## Streaming

```python
# Stream response tokens
query_engine = index.as_query_engine(streaming=True)
streaming_response = query_engine.query("Explain microservices")

for token in streaming_response.response_gen:
    print(token, end="", flush=True)

# Async streaming
async for token in await query_engine.aquery("Explain microservices"):
    print(token, end="")
```

---

## Next: [Agents & Tools →](04_Agents.md)
