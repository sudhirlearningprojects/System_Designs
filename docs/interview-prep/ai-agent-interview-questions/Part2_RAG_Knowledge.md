# Part 2: RAG, Knowledge Systems & Retrieval (Q11-Q20)

---

## Q11: "Your RAG system is returning irrelevant documents. How do you diagnose and fix this?"

**What They're Really Asking:** Can you systematically debug retrieval quality?

**Strong Answer:**

**Diagnosis checklist:**

1. **Check embedding quality**: Are query and document embeddings in the same semantic space? Try `cosine_similarity(embed("cancel subscription"), embed("how to cancel"))` — should be >0.8

2. **Check chunking**: Are chunks too large (diluted relevance) or too small (missing context)? Optimal: 256-512 tokens with 50-100 overlap

3. **Check query-document mismatch**: User says "get my money back" but docs say "refund policy" — vocabulary gap. Fix: HyDE (generate hypothetical answer, embed that instead)

4. **Check metadata filters**: Are filters too restrictive? Maybe `category="billing"` excludes a relevant doc in `category="policies"`

5. **Check top-K**: Maybe relevant doc is at position 8 but you only retrieve top-5. Fix: Retrieve more (top-20) then rerank to top-5

**Fixes (in order of impact):**
- Add **reranking** (cross-encoder) — biggest single improvement
- Switch to **hybrid search** (BM25 + vector) — catches keyword matches
- Improve **chunking** (semantic splitting, not fixed-size)
- Add **query expansion** (synonyms, rephrasing)
- Fine-tune **embedding model** on your domain data

**Key Points to Hit:**
- Systematic diagnosis (not guessing)
- Multiple potential root causes
- Reranking as highest-impact fix
- Hybrid search for vocabulary mismatch

**References:**
- "Benchmarking RAG" (LlamaIndex blog)
- RAGAS evaluation framework
- Cohere Rerank documentation
- "Precise Zero-Shot Dense Retrieval without Relevance Labels" (HyDE paper)

---

## Q12: "Explain the difference between naive RAG, advanced RAG, and agentic RAG."

**What They're Really Asking:** Do you know the evolution of RAG and when to use each level?

**Strong Answer:**

**Naive RAG** (baseline):
```
Query → Embed → Top-K vector search → Stuff into prompt → Generate
```
- Simple, fast, works for 60-70% of queries
- Fails on: complex queries, multi-hop reasoning, ambiguous questions

**Advanced RAG** (production):
```
Pre-retrieval: Query rewriting, HyDE, decomposition
Retrieval: Hybrid search (BM25 + vector), multi-index routing
Post-retrieval: Reranking (cross-encoder), filtering, compression
Generation: Iterative refinement, citation forcing
```
- Handles 85-90% of queries correctly
- Requires more engineering but dramatically better quality

**Agentic RAG** (state-of-the-art):
```
Agent decides WHEN, HOW, and HOW MANY TIMES to retrieve
Can: reformulate query, retrieve from multiple sources, verify answers
```
- Agent reasons: "I didn't find enough info → let me search differently"
- Multi-hop: Retrieve → reason → retrieve again with refined query
- Self-correction: "This doesn't answer the question → try different search"
- Handles 95%+ of queries including complex multi-step ones

**When to use each:**
- FAQ bot with simple questions → Naive RAG
- Production support system → Advanced RAG
- Research assistant, complex analysis → Agentic RAG

**Key Points to Hit:**
- Clear progression with trade-offs
- Specific techniques at each level
- Accuracy numbers (approximate)
- Cost/complexity trade-offs

**References:**
- "Retrieval-Augmented Generation for Large Language Models: A Survey" (Gao et al., 2024)
- LlamaIndex Advanced RAG documentation
- "Self-RAG: Learning to Retrieve, Generate, and Critique" (Asai et al., 2023)

---

## Q13: "How would you evaluate the quality of a RAG system? What metrics would you track?"

**What They're Really Asking:** Do you have a rigorous evaluation methodology?

**Strong Answer:**

**Retrieval Metrics (did we find the right docs?):**
| Metric | What It Measures | Target |
|--------|-----------------|--------|
| Recall@K | % of relevant docs in top-K | >0.85 |
| MRR | Rank of first relevant doc | >0.70 |
| NDCG@10 | Ranking quality (position-aware) | >0.75 |
| Context Precision | % of retrieved docs that are relevant | >0.80 |

**Generation Metrics (did we use docs well?):**
| Metric | What It Measures | Target |
|--------|-----------------|--------|
| Faithfulness | Is answer grounded in context? | >0.90 |
| Answer Relevancy | Does answer address the question? | >0.85 |
| Correctness | Is answer factually correct? | >0.90 |
| Hallucination Rate | % with fabricated info | <2% |

**End-to-End Metrics:**
| Metric | What It Measures | Target |
|--------|-----------------|--------|
| Task Completion | User's problem was solved | >85% |
| CSAT | User satisfaction | >4.2/5 |
| Latency | Time to answer | <3s |

**How I measure:**
1. **Golden dataset**: 200+ (question, expected_answer, relevant_docs) triples
2. **Automated eval**: RAGAS + LLM-as-judge on every deployment
3. **CI/CD gate**: Block deployment if faithfulness < 0.85
4. **Online monitoring**: Sample 5% of production for continuous eval

**Key Points to Hit:**
- Separate retrieval vs generation metrics
- Specific numbers/targets
- Automated in CI/CD
- Continuous monitoring (not just one-time)

**References:**
- RAGAS framework (ragas.io)
- DeepEval testing framework
- "Evaluating RAG Applications" (LlamaIndex documentation)
- TruLens evaluation library

---

## Q14: "How do you handle documents that change frequently in a RAG system?"

**What They're Really Asking:** Do you think about the operational aspects of RAG, not just the initial build?

**Strong Answer:**

**Strategies by update frequency:**

| Frequency | Strategy | Example |
|-----------|----------|---------|
| Real-time | Don't index — query live API | Stock prices, account balance |
| Hourly | Incremental indexing pipeline | News articles, support tickets |
| Daily | Scheduled re-index job | Product docs, FAQ updates |
| Weekly | Full re-index with validation | Policy documents, manuals |

**Implementation:**

1. **Change detection**: Hash each document. Only re-embed if hash changed.
2. **Incremental updates**: Upsert changed chunks, delete removed ones. Don't rebuild entire index.
3. **Versioning**: Keep old version until new one is validated. Atomic swap.
4. **Staleness indicator**: Track `last_updated` metadata. Deprioritize old docs in retrieval.
5. **Cache invalidation**: When doc changes → invalidate cached responses that cited it.

**Architecture:**
```
Document Source → Change Detector → Chunker → Embedder → Vector DB
                       ↓
              (only changed docs)
```

**Key Points to Hit:**
- Not everything needs real-time updates
- Incremental (not full rebuild)
- Validation before swap
- Cache invalidation

**References:**
- LlamaIndex IngestionPipeline with caching
- Bedrock Knowledge Base sync jobs
- Azure AI Search indexers (scheduled)

---

## Q15: "What's the difference between semantic search, keyword search, and hybrid search? When do you use each?"

**What They're Really Asking:** Do you understand retrieval fundamentals?

**Strong Answer:**

| Type | How It Works | Strength | Weakness |
|------|-------------|----------|----------|
| **Keyword (BM25)** | Term frequency matching | Exact terms, rare words, IDs | Misses synonyms, paraphrases |
| **Semantic (Vector)** | Embedding cosine similarity | Meaning, paraphrases, intent | Misses exact terms, numbers |
| **Hybrid** | Combine both with fusion | Best of both worlds | Slightly more complex |

**Examples:**
- "error code E-1234" → **Keyword wins** (exact match needed)
- "how do I get my money back" → **Semantic wins** (matches "refund policy")
- "cancel Pro subscription" → **Hybrid wins** (semantic for intent + keyword for "Pro")

**Fusion methods:**
- **Reciprocal Rank Fusion (RRF)**: `score = Σ 1/(k + rank_i)` — simple, effective
- **Weighted combination**: `0.7 * semantic + 0.3 * keyword` — tunable
- **Learned fusion**: Train a model to combine scores — best quality, most complex

**My default**: Always use hybrid (BM25 + vector + reranking). The 10% of queries where keyword matters are often the most important ones (error codes, product names, IDs).

**Key Points to Hit:**
- Concrete examples of when each wins
- Hybrid as default recommendation
- Fusion methods
- Reranking on top

**References:**
- "Hybrid Search" (Pinecone documentation)
- BM25 algorithm (Robertson et al.)
- Reciprocal Rank Fusion paper
- Azure AI Search hybrid search documentation

---

## Q16: "How would you build a RAG system that can answer questions across 10,000+ documents?"

**What They're Really Asking:** Can you handle scale? Do you think about indexing strategy?

**Strong Answer:**

At 10K+ documents, naive approaches break. My architecture:

**1. Hierarchical Indexing:**
```
Level 1: Document summaries (1 embedding per doc)
Level 2: Section summaries (1 per section)
Level 3: Chunk embeddings (many per doc)

Query flow: Search L1 → identify relevant docs → search L3 within those docs
```

**2. Metadata-Based Routing:**
- Tag documents by category, product, date
- Use metadata filters to narrow search space BEFORE vector search
- "Billing question" → only search billing docs (500 instead of 10,000)

**3. Multi-Index Architecture:**
```
Router (intent classifier)
  → billing_index (500 docs)
  → technical_index (3000 docs)
  → policy_index (200 docs)
  → product_index (6000 docs)
```

**4. Scalable Infrastructure:**
- Vector DB: Pinecone/Weaviate (managed, auto-scales)
- Embedding: Batch process with async (not one-by-one)
- Caching: Cache frequent queries (30%+ hit rate typical)

**5. Quality at Scale:**
- Reranking is critical (top-20 → rerank → top-5)
- Deduplication (same info in multiple docs)
- Freshness scoring (prefer recent docs)

**Key Points to Hit:**
- Hierarchical (not flat search over everything)
- Metadata routing (reduce search space)
- Infrastructure choices
- Quality doesn't degrade with scale

**References:**
- LlamaIndex ComposableGraph (multi-index)
- Pinecone namespaces and metadata filtering
- "Scaling RAG" (various engineering blogs)

---

## Q17: "What is contextual retrieval and how does it improve RAG quality?"

**What They're Really Asking:** Do you know cutting-edge RAG techniques?

**Strong Answer:**

**The Problem:**
Standard chunks lose context. A chunk saying "The policy allows 14 days" is meaningless without knowing WHICH policy.

**Contextual Retrieval (Anthropic's approach):**
Before embedding each chunk, prepend a context sentence generated by an LLM:

```
Original chunk: "Returns are accepted within 14 days of purchase."

With context: "This is from the Refund Policy section of the Customer 
Support documentation. Returns are accepted within 14 days of purchase."
```

**How it works:**
1. For each chunk, send (full_document + chunk) to a fast LLM
2. Ask: "Provide 1-2 sentences of context for this chunk within the document"
3. Prepend the context to the chunk before embedding
4. At query time, the enriched chunk matches better semantically

**Results (from Anthropic's benchmarks):**
- 49% reduction in retrieval failures (contextual embeddings alone)
- 67% reduction when combined with BM25 (hybrid + contextual)

**Implementation:**
```python
async def add_context(chunk: str, full_doc: str) -> str:
    response = await llm.generate(
        f"Document:\n{full_doc[:10000]}\n\nChunk:\n{chunk}\n\n"
        f"Provide 1-2 sentences of context for this chunk."
    )
    return f"{response}\n\n{chunk}"
```

**Key Points to Hit:**
- The problem it solves (chunks lose context)
- How it works (LLM generates context prefix)
- Quantified improvement (49-67%)
- Combine with hybrid search for best results

**References:**
- "Introducing Contextual Retrieval" (Anthropic blog, 2024)
- LlamaIndex contextual chunking implementation
- Prompt caching makes this cost-effective (cache the full document)

---

## Q18: "How do you prevent hallucination in a RAG system?"

**What They're Really Asking:** This is the #1 concern for enterprise AI. Do you have a comprehensive strategy?

**Strong Answer:**

**Multi-layer anti-hallucination strategy:**

**Layer 1: Retrieval Quality (prevent bad input)**
- Hybrid search + reranking (ensure relevant docs are found)
- Relevance threshold (drop chunks with score < 0.5)
- If no relevant docs found → say "I don't have information about that"

**Layer 2: Prompt Engineering (guide the model)**
- "Answer ONLY based on the provided context"
- "If the context doesn't contain the answer, say 'I don't know'"
- "Cite your sources using [Source: filename]"
- Lower temperature (0.0-0.3 for factual tasks)

**Layer 3: Output Verification (catch hallucinations)**
- **NLI-based check**: Does the response entail from the context? (Natural Language Inference)
- **Citation verification**: If response cites "Source A says X", verify X is actually in Source A
- **Self-consistency**: Generate 3 responses, flag if they disagree
- **Groundedness scoring**: Azure Content Safety or custom LLM judge

**Layer 4: Monitoring (detect in production)**
- Track groundedness scores over time
- Alert if score drops below threshold
- Human review queue for low-confidence responses

**Quantified approach:**
- Retrieval quality alone: reduces hallucination from ~15% to ~5%
- + Prompt engineering: reduces to ~3%
- + Output verification: reduces to <1%
- + Monitoring catches the remaining edge cases

**Key Points to Hit:**
- Multiple layers (not just one technique)
- Both prevention and detection
- Quantified impact
- "I don't know" is a valid answer

**References:**
- "Reducing Hallucination in RAG" (various research)
- Azure AI Content Safety groundedness detection
- RAGAS faithfulness metric
- "Self-Consistency Improves Chain of Thought Reasoning" (Wang et al., 2022)

---

## Q19: "How would you implement a RAG system that handles both text documents and images/diagrams?"

**What They're Really Asking:** Can you handle multi-modal RAG?

**Strong Answer:**

**Architecture for Multi-Modal RAG:**

```
Documents (with images) → Extract → Dual Encoding → Shared Vector Space
                                         ↓
Text chunks → Text embeddings ──────────→ Vector DB
Images → Image embeddings ──────────────→ (same index)
Image captions → Text embeddings ────────→ (same index)
```

**Implementation:**

1. **Document Processing:**
   - Extract text (OCR/Document AI)
   - Extract images with captions (Gemini/GPT-4V to describe each image)
   - Store both text chunks and image descriptions as embeddings

2. **Dual Encoding:**
   - Text: Standard text embedding model
   - Images: CLIP-style model OR generate text description → embed the description
   - Both end up in the same vector space

3. **Query Time:**
   - Text query → embed → search (finds both text chunks and image descriptions)
   - Image query → embed with CLIP → search (finds similar images and related text)

4. **Generation:**
   - Pass retrieved text + image URLs to multimodal LLM (Gemini, GPT-4V)
   - Model can reference both text and images in its answer

**Practical approach (simpler, works well):**
- Use Gemini/GPT-4V to describe every image in your docs
- Store descriptions as text chunks with metadata `{type: "image", url: "..."}`
- At generation time, include image URLs for the LLM to reference

**Key Points to Hit:**
- Two approaches: CLIP-style dual encoder vs describe-then-embed
- Simpler approach often works well enough
- Multimodal LLM at generation time
- Metadata to track image sources

**References:**
- CLIP (Radford et al., 2021) — "Learning Transferable Visual Models"
- LlamaIndex multi-modal RAG documentation
- Google Gemini multimodal capabilities
- ColPali (document retrieval using vision models)

---

## Q20: "What chunking strategy would you use for different document types?"

**What They're Really Asking:** Do you understand that one-size-fits-all chunking fails?

**Strong Answer:**

| Document Type | Strategy | Chunk Size | Why |
|--------------|----------|-----------|-----|
| **FAQ/Q&A** | One chunk per Q&A pair | Variable | Each pair is a complete unit |
| **Technical docs** | Markdown header splitting | 500-1000 tokens | Sections are logical units |
| **Legal contracts** | Clause-level splitting | 200-500 tokens | Each clause is independent |
| **Code** | Function/class level | Variable | Semantic units of code |
| **Conversations** | Per-turn or per-topic | Variable | Maintain speaker context |
| **Tables** | Keep table intact (don't split rows) | Variable | Tables lose meaning when split |
| **Long narratives** | Semantic splitting (topic change detection) | 500-800 tokens | Split where meaning changes |

**Universal principles:**
1. **Never split mid-sentence** (use sentence boundaries)
2. **Overlap** (100-200 tokens) to maintain context at boundaries
3. **Preserve structure** (don't split tables, code blocks, lists)
4. **Add metadata** (section title, page number, document title)
5. **Test empirically** (evaluate retrieval quality with different sizes)

**Advanced: Hierarchical chunking:**
```
Level 1: Full sections (2048 tokens) — for broad context
Level 2: Paragraphs (512 tokens) — for specific retrieval
Level 3: Sentences (128 tokens) — for precise matching

Search at Level 3, return Level 2 (auto-merging)
```

**Key Points to Hit:**
- Different strategies for different doc types
- Specific sizes with reasoning
- Hierarchical as advanced approach
- Always test empirically

**References:**
- LlamaIndex node parsers (Sentence, Semantic, Hierarchical, Markdown)
- "Chunking Strategies for RAG" (Pinecone blog)
- LangChain RecursiveCharacterTextSplitter documentation

---

## Next: [Part 3 — Safety, Guardrails & Evaluation →](Part3_Safety_Evaluation.md)
