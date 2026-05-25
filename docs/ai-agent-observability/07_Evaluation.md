# 7. Evaluation Frameworks

## Why Evaluation Matters

```
Without evaluation: "The agent seems to work fine" (vibes-based)
With evaluation: "The agent scores 87% faithfulness, 92% relevance,
                  with 2.1% hallucination rate — a 5% improvement over v1"
```

---

## RAGAS (RAG Assessment)

The standard framework for evaluating RAG pipelines.

### Metrics

| Metric | What It Measures | Range |
|--------|-----------------|-------|
| **Faithfulness** | Is the answer grounded in the context? | 0-1 |
| **Answer Relevancy** | Does the answer address the question? | 0-1 |
| **Context Precision** | Are retrieved docs relevant? (ranking quality) | 0-1 |
| **Context Recall** | Were all needed docs retrieved? | 0-1 |
| **Answer Correctness** | Is the answer factually correct? | 0-1 |

### Implementation

```python
from ragas import evaluate
from ragas.metrics import (
    faithfulness,
    answer_relevancy,
    context_precision,
    context_recall,
    answer_correctness,
)
from datasets import Dataset

# Prepare evaluation dataset
eval_data = {
    "question": [
        "How do I cancel my subscription?",
        "What's the refund policy?",
    ],
    "answer": [
        "Go to Settings > Subscription > Cancel Plan.",
        "Refunds are available within 14 days of purchase.",
    ],
    "contexts": [
        ["To cancel, navigate to Settings > Subscription > Cancel Plan. You'll be asked to confirm."],
        ["Our refund policy allows returns within 14 days. Contact support for processing."],
    ],
    "ground_truth": [
        "Navigate to Settings > Subscription > Cancel Plan to cancel.",
        "Refunds are processed within 14 days of the original purchase date.",
    ],
}

dataset = Dataset.from_dict(eval_data)

# Run evaluation
results = evaluate(
    dataset,
    metrics=[faithfulness, answer_relevancy, context_precision, context_recall],
    llm=ChatAnthropic(model="claude-sonnet-4-20250514"),
    embeddings=HuggingFaceEmbeddings(),
)

print(results)
# {'faithfulness': 0.94, 'answer_relevancy': 0.91, 'context_precision': 0.88, 'context_recall': 0.85}
```

### RAGAS in CI/CD

```python
def evaluate_rag_pipeline(pipeline, test_dataset) -> bool:
    """Gate deployment on RAG quality metrics."""
    
    # Generate answers
    answers = []
    contexts = []
    for item in test_dataset:
        result = pipeline.query(item["question"])
        answers.append(result["answer"])
        contexts.append(result["retrieved_contexts"])
    
    # Evaluate
    eval_dataset = Dataset.from_dict({
        "question": [item["question"] for item in test_dataset],
        "answer": answers,
        "contexts": contexts,
        "ground_truth": [item["ground_truth"] for item in test_dataset],
    })
    
    results = evaluate(eval_dataset, metrics=[faithfulness, answer_relevancy])
    
    # Quality gates
    THRESHOLDS = {
        "faithfulness": 0.85,
        "answer_relevancy": 0.80,
    }
    
    passed = all(results[metric] >= threshold for metric, threshold in THRESHOLDS.items())
    
    if not passed:
        print(f"❌ Quality gate FAILED: {results}")
    else:
        print(f"✅ Quality gate PASSED: {results}")
    
    return passed
```

---

## DeepEval (Testing Framework)

Unit testing for LLM applications — integrates with pytest.

```python
# pip install deepeval

from deepeval import assert_test
from deepeval.test_case import LLMTestCase
from deepeval.metrics import (
    AnswerRelevancyMetric,
    FaithfulnessMetric,
    HallucinationMetric,
    ToxicityMetric,
    GEval,
)

# Define test cases
def test_agent_response_quality():
    test_case = LLMTestCase(
        input="How do I export a PDF in Photoshop?",
        actual_output="Go to File > Export > Export As, then select PDF from the format dropdown.",
        retrieval_context=[
            "To export as PDF: File > Export > Export As > Select PDF format > Click Export"
        ],
        expected_output="Use File > Export > Export As and choose PDF format.",
    )
    
    # Multiple metrics
    relevancy = AnswerRelevancyMetric(threshold=0.8, model="gpt-4")
    faithfulness = FaithfulnessMetric(threshold=0.9, model="gpt-4")
    hallucination = HallucinationMetric(threshold=0.5, model="gpt-4")  # Lower = less hallucination
    
    assert_test(test_case, [relevancy, faithfulness, hallucination])


# Custom metric with G-Eval
def test_helpfulness():
    helpfulness_metric = GEval(
        name="Helpfulness",
        criteria="Determine if the response is helpful and actionable for the user",
        evaluation_steps=[
            "Does the response directly address the user's question?",
            "Are the instructions clear and easy to follow?",
            "Is the response complete (no missing steps)?",
        ],
        evaluation_params=[
            LLMTestCaseParams.INPUT,
            LLMTestCaseParams.ACTUAL_OUTPUT,
        ],
        threshold=0.7,
    )
    
    test_case = LLMTestCase(
        input="My Photoshop is crashing on startup",
        actual_output="Try resetting preferences by holding Ctrl+Alt+Shift while launching Photoshop.",
    )
    
    assert_test(test_case, [helpfulness_metric])


# Run with pytest
# pytest test_agent.py --deepeval
```

### Conversational Testing

```python
from deepeval.test_case import ConversationalTestCase
from deepeval.metrics import ConversationRelevancyMetric

def test_multi_turn_conversation():
    test_case = ConversationalTestCase(
        turns=[
            {"input": "I want to cancel my subscription", "actual_output": "I can help with that. Which plan would you like to cancel?"},
            {"input": "The Creative Cloud All Apps plan", "actual_output": "I'll process the cancellation for your Creative Cloud All Apps plan. Before I do, would you like to hear about our retention offer?"},
            {"input": "No, just cancel it", "actual_output": "Done. Your Creative Cloud All Apps plan has been cancelled. You'll retain access until the end of your billing period on Feb 15, 2024."},
        ]
    )
    
    metric = ConversationRelevancyMetric(threshold=0.8)
    assert_test(test_case, [metric])
```

---

## LLM-as-Judge (Custom Evaluators)

### Pairwise Comparison

```python
import anthropic

def pairwise_judge(query: str, response_a: str, response_b: str) -> dict:
    """Compare two responses and pick the better one."""
    client = anthropic.Anthropic()
    
    response = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=500,
        messages=[{
            "role": "user",
            "content": f"""Compare these two AI assistant responses to the same query.

Query: {query}

Response A: {response_a}

Response B: {response_b}

Evaluate on:
1. Accuracy (factual correctness)
2. Helpfulness (actionable, solves the problem)
3. Clarity (well-structured, easy to understand)
4. Conciseness (no unnecessary information)

Which is better overall? Respond as JSON:
{{"winner": "A" or "B" or "tie", "reasoning": "brief explanation", "scores": {{"A": {{"accuracy": 1-5, "helpfulness": 1-5}}, "B": {{"accuracy": 1-5, "helpfulness": 1-5}}}}}}"""
        }]
    )
    return json.loads(response.content[0].text)
```

### Rubric-Based Scoring

```python
SCORING_RUBRIC = """Score the AI response on a 1-5 scale:

5 - Excellent: Perfectly answers the question, well-structured, actionable
4 - Good: Answers correctly with minor issues (slightly verbose, missing one detail)
3 - Acceptable: Partially answers but missing important information
2 - Poor: Mostly irrelevant or contains significant errors
1 - Terrible: Completely wrong, harmful, or nonsensical

Consider:
- Factual accuracy (verified against context)
- Completeness (all aspects of question addressed)
- Actionability (user can follow the instructions)
- Tone (professional, empathetic, appropriate)"""

def score_response(query: str, response: str, context: str = "") -> dict:
    client = anthropic.Anthropic()
    
    result = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=200,
        messages=[{
            "role": "user",
            "content": f"""{SCORING_RUBRIC}

Query: {query}
Context (ground truth): {context}
AI Response: {response}

Score as JSON: {{"score": N, "reasoning": "brief explanation"}}"""
        }]
    )
    return json.loads(result.content[0].text)
```

### Batch Evaluation Pipeline

```python
import asyncio
from dataclasses import dataclass

@dataclass
class EvalResult:
    query: str
    response: str
    faithfulness: float
    relevance: float
    helpfulness: float
    hallucination_free: bool
    overall: float

async def evaluate_batch(test_cases: list, agent_fn, concurrency: int = 10) -> list:
    """Evaluate a batch of test cases with concurrent LLM-as-judge calls."""
    
    semaphore = asyncio.Semaphore(concurrency)
    
    async def evaluate_single(case):
        async with semaphore:
            # Get agent response
            response = await agent_fn(case["query"])
            
            # Run evaluations in parallel
            faithfulness_task = score_faithfulness(response, case.get("context", ""))
            relevance_task = score_relevance(case["query"], response)
            helpfulness_task = score_helpfulness(case["query"], response)
            
            faith, rel, help_score = await asyncio.gather(
                faithfulness_task, relevance_task, helpfulness_task
            )
            
            return EvalResult(
                query=case["query"],
                response=response,
                faithfulness=faith,
                relevance=rel,
                helpfulness=help_score,
                hallucination_free=faith > 0.8,
                overall=(faith + rel + help_score) / 3,
            )
    
    results = await asyncio.gather(*[evaluate_single(case) for case in test_cases])
    
    # Aggregate metrics
    avg_overall = sum(r.overall for r in results) / len(results)
    hallucination_rate = 1 - sum(r.hallucination_free for r in results) / len(results)
    
    print(f"Overall Quality: {avg_overall:.2%}")
    print(f"Hallucination Rate: {hallucination_rate:.2%}")
    print(f"Faithfulness: {sum(r.faithfulness for r in results) / len(results):.2%}")
    
    return results
```

---

## Evaluation Best Practices

| Practice | Why |
|----------|-----|
| **Use multiple metrics** | Single metric is misleading; combine faithfulness + relevance + helpfulness |
| **Include negative cases** | Test what agent should NOT do (refuse harmful requests, say "I don't know") |
| **Test edge cases** | Ambiguous queries, multi-language, very long inputs, adversarial inputs |
| **Version your eval sets** | Track how quality changes over time with same test cases |
| **Run in CI/CD** | Block deployments that regress quality below threshold |
| **Calibrate judges** | Verify LLM-as-judge agrees with human ratings (>80% agreement) |
| **Sample size** | Minimum 100 test cases for statistical significance |
| **Stratify by category** | Separate scores for billing, technical, creative queries |

---

## Next: [Production Patterns →](08_Production_Patterns.md)
