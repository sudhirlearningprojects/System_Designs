# 2. LangSmith (LangChain Observability)

## Overview

LangSmith is LangChain's observability platform for tracing, evaluating, and monitoring LLM applications. It provides full visibility into every step of your agent's execution.

**Key Capabilities:**
- Full execution traces (every LLM call, tool call, retrieval)
- Automated evaluation with custom evaluators
- Dataset management for testing
- Prompt versioning and A/B testing
- Production monitoring and alerting
- Annotation queues for human review

---

## Setup & Integration

```python
# Install
# pip install langsmith langchain langchain-anthropic

# Environment variables
import os
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "ls__..."
os.environ["LANGCHAIN_PROJECT"] = "my-agent-production"

# That's it! All LangChain operations are now traced automatically.
```

### Manual Tracing (Without LangChain)

```python
from langsmith import traceable, Client
from langsmith.run_trees import RunTree
import anthropic

client = anthropic.Anthropic()
ls_client = Client()

@traceable(name="agent_response", run_type="chain")
def handle_query(query: str, user_id: str) -> str:
    """Traced function — appears as a span in LangSmith."""
    
    # This nested call creates a child span
    intent = classify_intent(query)
    
    # Another child span
    context = retrieve_context(query)
    
    # LLM call span
    response = generate_response(query, context, intent)
    
    return response

@traceable(name="classify_intent", run_type="llm")
def classify_intent(query: str) -> str:
    response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=50,
        messages=[{"role": "user", "content": f"Classify intent: {query}"}]
    )
    return response.content[0].text

@traceable(name="retrieve_context", run_type="retriever")
def retrieve_context(query: str) -> list:
    # Vector search
    results = vector_db.search(query, top_k=5)
    return results

@traceable(name="generate_response", run_type="llm")
def generate_response(query: str, context: list, intent: str) -> str:
    response = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=2048,
        system="You are a helpful assistant.",
        messages=[{"role": "user", "content": f"Context: {context}\n\nQuestion: {query}"}]
    )
    return response.content[0].text
```

### Tracing with Metadata

```python
@traceable(
    name="agent_conversation",
    metadata={"version": "2.1.0", "environment": "production"},
    tags=["customer-support", "billing"]
)
def handle_conversation(query: str, user_id: str, session_id: str) -> str:
    # Add runtime metadata to the trace
    from langsmith import get_current_run_tree
    run = get_current_run_tree()
    if run:
        run.metadata["user_id"] = user_id
        run.metadata["session_id"] = session_id
        run.metadata["query_category"] = "billing"
    
    return process_query(query)
```

---

## Evaluation with LangSmith

### Create Dataset

```python
from langsmith import Client

client = Client()

# Create evaluation dataset
dataset = client.create_dataset("customer-support-eval", description="Golden test cases for support agent")

# Add examples
examples = [
    {
        "inputs": {"query": "How do I cancel my subscription?"},
        "outputs": {"expected": "To cancel, go to Account Settings > Subscription > Cancel Plan."}
    },
    {
        "inputs": {"query": "I was charged twice this month"},
        "outputs": {"expected": "I'll look into the duplicate charge. Can you provide your account email?"}
    },
    {
        "inputs": {"query": "What's the weather today?"},
        "outputs": {"expected": "I can only help with product-related questions. Is there something about your account I can assist with?"}
    },
]

for example in examples:
    client.create_example(
        inputs=example["inputs"],
        outputs=example["outputs"],
        dataset_id=dataset.id
    )
```

### Run Evaluation

```python
from langsmith.evaluation import evaluate, LangChainStringEvaluator

# Define your agent function
def my_agent(inputs: dict) -> dict:
    response = handle_query(inputs["query"])
    return {"response": response}

# Custom evaluator
def relevance_evaluator(run, example) -> dict:
    """Score if response is relevant to the query."""
    response = run.outputs["response"]
    query = example.inputs["query"]
    
    # Use LLM as judge
    score_response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=100,
        messages=[{"role": "user", "content": f"""Score the relevance of this response to the query (1-5):
Query: {query}
Response: {response}
Score (just the number):"""}]
    )
    score = int(score_response.content[0].text.strip())
    return {"key": "relevance", "score": score / 5.0}

def hallucination_evaluator(run, example) -> dict:
    """Check if response contains hallucinated information."""
    response = run.outputs["response"]
    # Check against known facts...
    return {"key": "hallucination_free", "score": 1.0}  # 1.0 = no hallucination

# Run evaluation
results = evaluate(
    my_agent,
    data="customer-support-eval",
    evaluators=[
        relevance_evaluator,
        hallucination_evaluator,
        LangChainStringEvaluator("helpfulness"),
    ],
    experiment_prefix="agent-v2.1",
    max_concurrency=4,
)

print(f"Average relevance: {results['relevance'].mean():.2f}")
print(f"Hallucination-free rate: {results['hallucination_free'].mean():.2%}")
```

### Comparative Evaluation (A/B)

```python
# Compare two agent versions
results_v1 = evaluate(agent_v1, data="customer-support-eval", experiment_prefix="v1")
results_v2 = evaluate(agent_v2, data="customer-support-eval", experiment_prefix="v2")

# LangSmith UI shows side-by-side comparison with statistical significance
```

---

## Production Monitoring

### Filtering & Searching Traces

```python
# Search for failed conversations
runs = client.list_runs(
    project_name="my-agent-production",
    filter='eq(status, "error")',
    start_time=datetime.now() - timedelta(hours=1),
)

# Search by metadata
runs = client.list_runs(
    project_name="my-agent-production",
    filter='has(metadata, {"user_id": "u-123"})',
)

# Search by latency
runs = client.list_runs(
    project_name="my-agent-production",
    filter='gt(latency, 5000)',  # > 5 seconds
)

# Search by feedback score
runs = client.list_runs(
    project_name="my-agent-production",
    filter='lt(feedback_score, 3)',  # Low satisfaction
)
```

### User Feedback Integration

```python
from langsmith import Client

ls_client = Client()

# After user gives feedback (thumbs up/down)
def record_feedback(run_id: str, score: float, comment: str = ""):
    ls_client.create_feedback(
        run_id=run_id,
        key="user_satisfaction",
        score=score,  # 0.0 (bad) to 1.0 (good)
        comment=comment,
    )

# In your API endpoint
@app.post("/feedback")
async def submit_feedback(request: FeedbackRequest):
    record_feedback(
        run_id=request.trace_id,
        score=1.0 if request.rating == "helpful" else 0.0,
        comment=request.comment
    )
```

### Online Evaluation (Continuous)

```python
from langsmith.evaluation import evaluate_on_runs

# Continuously evaluate production traces
def run_online_eval():
    """Run every hour on recent production traces."""
    recent_runs = client.list_runs(
        project_name="my-agent-production",
        start_time=datetime.now() - timedelta(hours=1),
        run_type="chain",
    )
    
    for run in recent_runs:
        # Score each production trace
        score = evaluate_single_run(run)
        
        # Record as feedback
        client.create_feedback(
            run_id=run.id,
            key="auto_eval_quality",
            score=score,
        )
        
        # Alert if quality drops
        if score < 0.7:
            alert_team(run.id, score, run.inputs, run.outputs)
```

---

## Prompt Management

```python
from langsmith import Client
from langchain_core.prompts import ChatPromptTemplate

client = Client()

# Push prompt to LangSmith Hub
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful customer support agent for {company}. Be concise and empathetic."),
    ("human", "{query}")
])

client.push_prompt("customer-support-v2", object=prompt, tags=["production", "support"])

# Pull prompt (version controlled)
prompt = client.pull_prompt("customer-support-v2")

# Use specific version
prompt = client.pull_prompt("customer-support-v2:abc123")
```

---

## Best Practices

1. **Trace everything**: Every LLM call, tool call, and retrieval should be a span
2. **Add metadata**: user_id, session_id, version, environment on every trace
3. **Create golden datasets**: 100+ test cases covering edge cases
4. **Run evals in CI/CD**: Block deployments if quality regresses
5. **Monitor cost**: Track tokens and cost per conversation
6. **Sample for human review**: Send 5% of traces to annotation queue
7. **Set up alerts**: Latency spikes, error rate increases, quality drops

---

## Next: [Arize Phoenix →](03_Arize_Phoenix.md)
