# 4. Weights & Biases (W&B Weave)

## Overview

W&B Weave is purpose-built for tracing and evaluating AI applications. It integrates with W&B's experiment tracking for a complete ML lifecycle view.

---

## Setup & Tracing

```python
import weave
import anthropic

# Initialize Weave
weave.init("my-agent-project")

# Auto-trace Anthropic calls
client = weave.wrap(anthropic.Anthropic())

# All calls are now traced with inputs, outputs, latency, tokens
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}]
)
# → Trace visible in W&B UI with full details
```

### Custom Operations

```python
@weave.op()
def classify_intent(query: str) -> str:
    """Traced as a Weave operation."""
    response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=50,
        messages=[{"role": "user", "content": f"Classify: {query}"}]
    )
    return response.content[0].text

@weave.op()
def retrieve_context(query: str) -> list:
    results = vector_db.search(query, top_k=5)
    return [r.text for r in results]

@weave.op()
def agent_pipeline(query: str, user_id: str) -> str:
    """Full agent pipeline — all nested ops create child spans."""
    intent = classify_intent(query)
    context = retrieve_context(query)
    response = generate_response(query, context, intent)
    return response
```

---

## Evaluation with Weave

```python
# Define evaluation dataset
dataset = weave.Dataset(
    name="support-eval-v1",
    rows=[
        {"query": "How to cancel?", "expected": "Go to Settings > Cancel"},
        {"query": "Refund policy?", "expected": "14-day refund window"},
    ]
)

# Define scorer
@weave.op()
def relevance_scorer(query: str, model_output: str, expected: str) -> dict:
    """Score response relevance using LLM-as-judge."""
    judge_response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=50,
        messages=[{"role": "user", "content": f"Score relevance 1-5. Query: {query}, Response: {model_output}. Just the number:"}]
    )
    score = int(judge_response.content[0].text.strip()) / 5.0
    return {"relevance": score}

# Run evaluation
evaluation = weave.Evaluation(dataset=dataset, scorers=[relevance_scorer])
results = await evaluation.evaluate(agent_pipeline)
print(results)  # Aggregated scores + per-example breakdown
```

---

## Experiment Tracking Integration

```python
import wandb

# Track agent experiments alongside model training
run = wandb.init(project="ai-agent", name="prompt-v2.1")

# Log agent metrics
wandb.log({
    "task_completion_rate": 0.87,
    "avg_latency_ms": 2100,
    "hallucination_rate": 0.02,
    "cost_per_conversation": 0.08,
    "csat": 4.3,
})

# Log prompt as artifact (version control)
artifact = wandb.Artifact("system-prompt", type="prompt")
artifact.add_file("prompts/system_v2.1.txt")
run.log_artifact(artifact)

# Compare experiments in W&B dashboard
run.finish()
```

---

## Next: [OpenTelemetry for AI →](05_OpenTelemetry_AI.md)
