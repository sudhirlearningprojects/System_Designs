# Module 9: Evaluation & Testing

## Overview

You can't improve what you can't measure. RAG evaluation is multi-dimensional — you need to assess retrieval quality, generation quality, and end-to-end performance.

---

## The RAG Evaluation Framework

```
┌─────────────────────────────────────────────────┐
│              End-to-End Metrics                   │
│  (Answer Correctness, Latency, Cost)            │
├────────────────────┬────────────────────────────┤
│  Retrieval Metrics │  Generation Metrics         │
│  - Context Precision│  - Faithfulness            │
│  - Context Recall   │  - Answer Relevance        │
│  - NDCG, MRR       │  - Hallucination Rate      │
│  - Hit Rate        │  - Completeness            │
└────────────────────┴────────────────────────────┘
```

---

## RAGAS (Retrieval Augmented Generation Assessment)

The standard framework for RAG evaluation:

```python
from ragas import evaluate
from ragas.metrics import (
    context_precision,
    context_recall,
    faithfulness,
    answer_relevancy,
    answer_correctness,
)
from datasets import Dataset

# Prepare evaluation dataset
eval_data = {
    "question": [
        "What is the deployment process?",
        "How do I configure authentication?",
    ],
    "answer": [
        "The deployment process involves...",  # RAG-generated answer
        "Authentication is configured by...",
    ],
    "contexts": [
        ["Deploy using kubectl apply...", "Configuration requires..."],  # Retrieved docs
        ["Auth setup involves JWT tokens...", "Configure OAuth in settings..."],
    ],
    "ground_truth": [
        "Deploy using kubectl apply -f deployment.yaml...",  # Expected answer
        "Set up OAuth2 with client ID and secret...",
    ],
}

dataset = Dataset.from_dict(eval_data)

# Evaluate
results = evaluate(
    dataset,
    metrics=[
        context_precision,    # Are retrieved docs relevant?
        context_recall,       # Did we retrieve all relevant docs?
        faithfulness,         # Is the answer grounded in context?
        answer_relevancy,     # Does the answer address the question?
        answer_correctness,   # Is the answer factually correct?
    ],
)

print(results)
# {'context_precision': 0.87, 'context_recall': 0.92, 
#  'faithfulness': 0.95, 'answer_relevancy': 0.89, 'answer_correctness': 0.85}
```

### RAGAS Metrics Explained

| Metric | What It Measures | Range | Target |
|--------|-----------------|-------|--------|
| Context Precision | % of retrieved docs that are relevant | 0-1 | >0.8 |
| Context Recall | % of relevant docs that were retrieved | 0-1 | >0.9 |
| Faithfulness | Is answer supported by context? | 0-1 | >0.9 |
| Answer Relevancy | Does answer address the question? | 0-1 | >0.85 |
| Answer Correctness | Factual accuracy vs ground truth | 0-1 | >0.8 |

---

## DeepEval

More comprehensive evaluation with custom metrics:

```python
from deepeval import evaluate
from deepeval.metrics import (
    AnswerRelevancyMetric,
    FaithfulnessMetric,
    ContextualRelevancyMetric,
    HallucinationMetric,
    ToxicityMetric,
)
from deepeval.test_case import LLMTestCase

# Define test cases
test_case = LLMTestCase(
    input="What is the refund policy?",
    actual_output="Refunds are available within 30 days...",
    expected_output="Full refund within 30 days of purchase...",
    retrieval_context=["Our refund policy allows returns within 30 days..."],
)

# Run metrics
metrics = [
    AnswerRelevancyMetric(threshold=0.7, model="gpt-4o"),
    FaithfulnessMetric(threshold=0.8, model="gpt-4o"),
    ContextualRelevancyMetric(threshold=0.7, model="gpt-4o"),
    HallucinationMetric(threshold=0.5, model="gpt-4o"),
]

evaluate(test_cases=[test_case], metrics=metrics)
```

### Custom Metrics with DeepEval
```python
from deepeval.metrics import BaseMetric
from deepeval.test_case import LLMTestCase

class CitationAccuracyMetric(BaseMetric):
    def __init__(self, threshold: float = 0.8):
        self.threshold = threshold
        self.score = 0
    
    def measure(self, test_case: LLMTestCase) -> float:
        # Check if citations in answer actually reference provided context
        citations_in_answer = extract_citations(test_case.actual_output)
        valid_citations = sum(
            1 for c in citations_in_answer 
            if any(c in ctx for ctx in test_case.retrieval_context)
        )
        self.score = valid_citations / max(len(citations_in_answer), 1)
        self.success = self.score >= self.threshold
        return self.score
    
    def is_successful(self) -> bool:
        return self.success
    
    @property
    def __name__(self):
        return "Citation Accuracy"
```

---

## LangSmith (Observability + Evaluation)

```python
import os
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "<your-key>"
os.environ["LANGCHAIN_PROJECT"] = "rag-production"

from langsmith import Client
from langsmith.evaluation import evaluate as ls_evaluate

client = Client()

# Create evaluation dataset
dataset = client.create_dataset("rag-test-set")
client.create_examples(
    inputs=[{"question": "What is RAG?"}],
    outputs=[{"answer": "RAG combines retrieval with generation..."}],
    dataset_id=dataset.id,
)

# Define evaluator
def correctness_evaluator(run, example):
    """Custom evaluator comparing output to expected."""
    prediction = run.outputs["answer"]
    reference = example.outputs["answer"]
    # Use LLM to judge
    score = llm.invoke(
        f"Rate similarity 0-1.\nPrediction: {prediction}\nReference: {reference}\nScore:"
    )
    return {"score": float(score.content.strip())}

# Run evaluation
results = ls_evaluate(
    rag_chain.invoke,
    data="rag-test-set",
    evaluators=[correctness_evaluator],
)
```

---

## Phoenix (Arize) - Open Source Observability

```python
import phoenix as px
from phoenix.trace.openai import OpenAIInstrumentor

# Launch Phoenix UI
session = px.launch_app()

# Auto-instrument OpenAI calls
OpenAIInstrumentor().instrument()

# All RAG pipeline calls are now traced
# View at http://localhost:6006
# - Retrieval quality per query
# - LLM latency and token usage
# - Embedding drift detection
```

---

## Building an Evaluation Dataset

### Synthetic Test Data Generation
```python
from ragas.testset.generator import TestsetGenerator
from ragas.testset.evolutions import simple, reasoning, multi_context

# Auto-generate test questions from your documents
generator = TestsetGenerator.from_langchain(
    generator_llm=ChatOpenAI(model="gpt-4o"),
    critic_llm=ChatOpenAI(model="gpt-4o"),
    embeddings=OpenAIEmbeddings(),
)

testset = generator.generate_with_langchain_docs(
    documents=your_documents,
    test_size=50,
    distributions={
        simple: 0.4,        # Simple factual questions
        reasoning: 0.3,     # Requires reasoning over context
        multi_context: 0.3, # Requires multiple chunks
    },
)

test_df = testset.to_pandas()
```

### Manual Evaluation Template
```python
evaluation_template = {
    "question": "",
    "expected_answer": "",
    "expected_sources": [],
    "difficulty": "easy|medium|hard",
    "type": "factual|reasoning|comparison|multi_hop",
    "tags": [],
}

# Create diverse test set covering:
# - Different question types (what, how, why, compare)
# - Different difficulty levels
# - Edge cases (no relevant docs, ambiguous queries)
# - Multi-hop questions requiring multiple retrievals
```

---

## Retrieval-Only Evaluation

Evaluate retrieval independently from generation:

```python
from sklearn.metrics import ndcg_score
import numpy as np

def evaluate_retrieval(test_set: list[dict], retriever, k: int = 5) -> dict:
    """Evaluate retrieval quality independently."""
    metrics = {"hit_rate": [], "mrr": [], "precision": [], "recall": []}
    
    for item in test_set:
        query = item["question"]
        relevant_ids = set(item["relevant_doc_ids"])
        
        # Retrieve
        results = retriever.invoke(query)
        retrieved_ids = [doc.metadata.get("id") for doc in results[:k]]
        
        # Hit Rate: Is at least one relevant doc in top-k?
        hit = any(rid in relevant_ids for rid in retrieved_ids)
        metrics["hit_rate"].append(1.0 if hit else 0.0)
        
        # MRR: Reciprocal rank of first relevant result
        for rank, rid in enumerate(retrieved_ids, 1):
            if rid in relevant_ids:
                metrics["mrr"].append(1.0 / rank)
                break
        else:
            metrics["mrr"].append(0.0)
        
        # Precision@k
        relevant_retrieved = sum(1 for rid in retrieved_ids if rid in relevant_ids)
        metrics["precision"].append(relevant_retrieved / k)
        
        # Recall@k
        metrics["recall"].append(relevant_retrieved / len(relevant_ids))
    
    return {k: np.mean(v) for k, v in metrics.items()}
```

---

## A/B Testing RAG Configurations

```python
class RAGExperiment:
    """A/B test different RAG configurations."""
    
    def __init__(self, configs: dict, test_set: list):
        self.configs = configs  # {"baseline": config_a, "experiment": config_b}
        self.test_set = test_set
    
    def run(self) -> dict:
        results = {}
        for name, config in self.configs.items():
            pipeline = self._build_pipeline(config)
            scores = []
            for item in self.test_set:
                response = pipeline.invoke(item["question"])
                score = self._evaluate(item, response)
                scores.append(score)
            results[name] = {
                "avg_score": np.mean(scores),
                "p95_latency": np.percentile([s["latency"] for s in scores], 95),
                "avg_cost": np.mean([s["cost"] for s in scores]),
            }
        return results

# Example: Test chunk_size impact
configs = {
    "chunk_500": {"chunk_size": 500, "model": "gpt-4o-mini"},
    "chunk_1000": {"chunk_size": 1000, "model": "gpt-4o-mini"},
    "chunk_1000_rerank": {"chunk_size": 1000, "model": "gpt-4o-mini", "rerank": True},
}
```

---

## Continuous Evaluation in Production

```python
import random
from datetime import datetime

class ProductionEvaluator:
    """Sample and evaluate production queries continuously."""
    
    def __init__(self, sample_rate: float = 0.05):
        self.sample_rate = sample_rate
    
    def should_evaluate(self) -> bool:
        return random.random() < self.sample_rate
    
    def evaluate_and_log(self, query: str, response: str, context: list):
        if not self.should_evaluate():
            return
        
        # Async evaluation (don't block response)
        metrics = {
            "timestamp": datetime.utcnow().isoformat(),
            "query": query,
            "faithfulness": self._check_faithfulness(response, context),
            "relevance": self._check_relevance(query, response),
            "context_quality": self._check_context(query, context),
        }
        
        # Log to monitoring system
        self._log_metrics(metrics)
        
        # Alert if quality drops
        if metrics["faithfulness"] < 0.7:
            self._alert("Low faithfulness detected", metrics)
```

---

## Exercises

1. Create an evaluation dataset of 50 questions using RAGAS synthetic generation
2. Evaluate your RAG pipeline using all RAGAS metrics and identify weakest dimension
3. Set up LangSmith tracing and create a regression test suite
4. Implement retrieval-only evaluation and optimize for Hit Rate >0.95
5. Run A/B test comparing two chunking strategies with statistical significance
