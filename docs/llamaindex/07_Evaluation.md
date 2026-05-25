# 7. Evaluation & Observability

## Theory: Evaluating RAG Systems

RAG evaluation requires measuring both **retrieval quality** and **generation quality** independently.

```
┌─────────────────────────────────────────────────────────────┐
│  RAG EVALUATION DIMENSIONS                                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  RETRIEVAL QUALITY (Did we find the right information?)     │
│  ├── Context Precision: Are retrieved docs relevant?        │
│  ├── Context Recall: Did we find ALL relevant docs?         │
│  └── Context Relevancy: Is retrieved context useful?        │
│                                                              │
│  GENERATION QUALITY (Did we use the information well?)      │
│  ├── Faithfulness: Is answer grounded in context?           │
│  ├── Answer Relevancy: Does answer address the question?    │
│  ├── Correctness: Is the answer factually correct?          │
│  └── Harmlessness: Is the answer safe and appropriate?      │
│                                                              │
│  END-TO-END QUALITY                                          │
│  ├── Answer Correctness: Final answer vs ground truth       │
│  └── User Satisfaction: Would a human find this helpful?    │
└─────────────────────────────────────────────────────────────┘
```

---

## Built-in Evaluation

```python
from llama_index.core.evaluation import (
    FaithfulnessEvaluator,
    RelevancyEvaluator,
    CorrectnessEvaluator,
    BatchEvalRunner,
)
from llama_index.llms.anthropic import Anthropic

eval_llm = Anthropic(model="claude-sonnet-4-20250514")

# Faithfulness: Is the response grounded in retrieved context?
faithfulness_evaluator = FaithfulnessEvaluator(llm=eval_llm)

# Relevancy: Does the response answer the question?
relevancy_evaluator = RelevancyEvaluator(llm=eval_llm)

# Correctness: Is the response factually correct? (needs reference answer)
correctness_evaluator = CorrectnessEvaluator(llm=eval_llm)

# Evaluate single response
query = "What is the refund policy?"
response = query_engine.query(query)

faith_result = await faithfulness_evaluator.aevaluate_response(query=query, response=response)
print(f"Faithfulness: {faith_result.score} | {faith_result.feedback}")

rel_result = await relevancy_evaluator.aevaluate_response(query=query, response=response)
print(f"Relevancy: {rel_result.score} | {rel_result.feedback}")
```

### Batch Evaluation

```python
from llama_index.core.evaluation import BatchEvalRunner

# Evaluate on a dataset
eval_questions = [
    "How do I cancel my subscription?",
    "What's the refund policy?",
    "How to export PDF in Photoshop?",
]
reference_answers = [
    "Go to Settings > Subscription > Cancel",
    "14-day refund window from purchase date",
    "File > Export > Export As > PDF",
]

runner = BatchEvalRunner(
    evaluators={
        "faithfulness": faithfulness_evaluator,
        "relevancy": relevancy_evaluator,
    },
    workers=4,  # Parallel evaluation
)

eval_results = await runner.aevaluate_queries(
    query_engine=query_engine,
    queries=eval_questions,
)

# Aggregate results
for metric, results in eval_results.items():
    scores = [r.score for r in results if r.score is not None]
    print(f"{metric}: {sum(scores)/len(scores):.2%}")
```

---

## Retrieval Evaluation

```python
from llama_index.core.evaluation import RetrieverEvaluator

# Evaluate retrieval quality (needs labeled relevant docs)
retriever_evaluator = RetrieverEvaluator.from_metric_names(
    ["mrr", "hit_rate"],  # Mean Reciprocal Rank, Hit Rate
    retriever=index.as_retriever(similarity_top_k=5),
)

# Evaluation dataset: query → expected relevant doc IDs
eval_dataset = [
    {"query": "refund policy", "expected_ids": ["doc-billing-3", "doc-policy-1"]},
    {"query": "export PDF", "expected_ids": ["doc-photoshop-export"]},
]

results = await retriever_evaluator.aevaluate_dataset(eval_dataset)
print(f"MRR: {results['mrr']:.3f}")
print(f"Hit Rate: {results['hit_rate']:.3f}")
```

---

## Observability Integration

```python
# LlamaTrace (built-in)
from llama_index.core import set_global_handler

# Arize Phoenix
set_global_handler("arize_phoenix")

# LangSmith
import os
os.environ["LANGCHAIN_TRACING_V2"] = "true"
# LlamaIndex auto-traces when LangSmith env vars are set

# Weights & Biases
set_global_handler("wandb", run_args={"project": "my-rag-app"})

# Custom callback
from llama_index.core.callbacks import CallbackManager, LlamaDebugHandler

debug_handler = LlamaDebugHandler()
callback_manager = CallbackManager([debug_handler])
Settings.callback_manager = callback_manager

# After query, inspect events
for event in debug_handler.get_events():
    print(f"{event.event_type}: {event.payload}")
```

---

## Next: [Production & Deployment →](08_Production.md)
