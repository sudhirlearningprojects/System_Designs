# 6. LangSmith (Observability & Evaluation)

## Setup (Zero-Code Tracing)

```bash
# Set environment variables — that's it!
export LANGCHAIN_TRACING_V2=true
export LANGCHAIN_API_KEY=ls__...
export LANGCHAIN_PROJECT=my-agent-production
```

All LangChain and LangGraph operations are automatically traced. No code changes.

---

## What Gets Traced

```
Every LangGraph invocation creates a trace showing:
├── Graph execution (total time, input/output)
│   ├── Node: classifier (150ms)
│   │   └── LLM call: claude-3-5-haiku (tokens: 150→10)
│   ├── Node: retriever (95ms)
│   │   └── Vector search: 5 results, top score 0.92
│   ├── Node: agent (1800ms)
│   │   └── LLM call: claude-sonnet-4 (tokens: 2400→350)
│   │   └── Tool call: get_subscription (120ms)
│   └── Node: guardrails (45ms)
└── Total: 2.1s, $0.04, 2810 tokens
```

---

## Evaluation

```python
from langsmith import Client, evaluate

client = Client()

# Create test dataset
dataset = client.create_dataset("agent-eval-v1")
client.create_examples(
    inputs=[{"query": "Cancel my plan"}, {"query": "How to export PDF?"}],
    outputs=[{"expected": "Process cancellation"}, {"expected": "File > Export > PDF"}],
    dataset_id=dataset.id,
)

# Define evaluator
def quality_evaluator(run, example):
    response = run.outputs["messages"][-1].content
    # Score with LLM-as-judge
    score = judge_quality(example.inputs["query"], response, example.outputs["expected"])
    return {"key": "quality", "score": score}

# Run evaluation
results = evaluate(
    lambda inputs: app.invoke({"messages": [HumanMessage(content=inputs["query"])]}),
    data="agent-eval-v1",
    evaluators=[quality_evaluator],
    experiment_prefix="v2.1",
)
```

---

## Production Monitoring

```python
from langsmith import Client

client = Client()

# Find problematic traces
failed_runs = client.list_runs(
    project_name="my-agent-production",
    filter='and(eq(status, "error"), gt(latency, 5000))',
    start_time=datetime.now() - timedelta(hours=1),
)

# User feedback
client.create_feedback(run_id=trace_id, key="user_rating", score=1.0)

# Annotation queue (human review)
client.create_annotation_queue("review-low-confidence")
```

---

## Key Integration Points

| Feature | How |
|---------|-----|
| Auto-tracing | Set `LANGCHAIN_TRACING_V2=true` |
| Custom metadata | Pass `config={"metadata": {...}}` to invoke |
| User feedback | `client.create_feedback(run_id, ...)` |
| Evaluation | `evaluate()` with custom scorers |
| Prompt versioning | `client.push_prompt()` / `client.pull_prompt()` |
| Alerting | LangSmith UI → Rules → Alert on metric threshold |

For comprehensive LangSmith documentation, see: [AI Agent Observability → LangSmith](../ai-agent-observability/02_LangSmith.md)

---

## Next: [LangServe & Deployment →](07_LangServe_Deployment.md)
