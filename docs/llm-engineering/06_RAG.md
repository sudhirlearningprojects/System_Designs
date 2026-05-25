# 6. RAG & Grounding

> **Full coverage**: See [LlamaIndex → Advanced RAG](../llamaindex/06_Advanced_RAG.md) and [LangChain → RAG](../langchain-langgraph/02_RAG.md) for comprehensive RAG implementation guides.

## Theory: Why RAG?

```
LLM LIMITATIONS:
  1. Knowledge cutoff (doesnt know recent events)
  2. No access to private data (your documents, databases)
  3. Hallucination (invents plausible-sounding facts)
  4. No citations (cant prove where info came from)

RAG SOLVES ALL FOUR:
  1. Retrieve latest documents → always up-to-date
  2. Index your private data → domain-specific answers
  3. Ground in retrieved context → reduces hallucination
  4. Track source documents → provide citations
```

### RAG vs Fine-Tuning

| Aspect | RAG | Fine-Tuning |
|--------|-----|-------------|
| Knowledge updates | Instant (update docs) | Requires retraining |
| Cost | Per-query (retrieval + tokens) | Upfront training cost |
| Hallucination | Reduced (grounded) | Can still hallucinate |
| Citations | Built-in | Not available |
| Best for | Factual QA, documentation | Style, format, behavior |

### Advanced RAG Taxonomy

```
NAIVE RAG: Embed → Retrieve top-K → Generate
  Problem: 60-70% accuracy on complex queries

ADVANCED RAG: 
  Pre-retrieval: Query rewriting, decomposition, HyDE
  Retrieval: Hybrid search, multi-index routing
  Post-retrieval: Reranking, compression, filtering
  Generation: Iterative refinement, citation forcing
  Problem solved: 85-95% accuracy

AGENTIC RAG:
  Agent decides WHEN and HOW to retrieve
  Can do multi-hop retrieval (retrieve → reason → retrieve again)
  Can combine retrieval with tool use
  Problem solved: Handles complex, multi-step queries
```

---

## Next: [AI Agents & Tool Use →](07_Agents.md)
