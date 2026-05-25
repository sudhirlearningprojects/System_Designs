# 3. Arize Phoenix (Open-Source AI Observability)

## Overview

Arize Phoenix is an open-source observability tool for AI applications. It provides tracing, evaluation, and embeddings analysis — all running locally or self-hosted.

**Why Phoenix:**
- 100% open-source (Apache 2.0)
- Framework-agnostic (works with any LLM, any framework)
- Local-first (no data leaves your infrastructure)
- Built-in evaluators (hallucination, relevance, toxicity)
- Embeddings visualization and drift detection

---

## Setup

```python
# pip install arize-phoenix openinference-instrumentation-openai openinference-instrumentation-langchain

import phoenix as px

# Launch Phoenix UI (local)
session = px.launch_app()
print(f"Phoenix UI: {session.url}")  # http://localhost:6006

# Or connect to remote Phoenix server
# px.Client(endpoint="http://phoenix-server:6006")
```

---

## Auto-Instrumentation

### OpenAI / Anthropic

```python
from openinference.instrumentation.openai import OpenAIInstrumentor
from openinference.instrumentation.anthropic import AnthropicInstrumentor
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter

# Setup OpenTelemetry → Phoenix
tracer_provider = TracerProvider()
tracer_provider.add_span_processor(
    SimpleSpanProcessor(OTLPSpanExporter(endpoint="http://localhost:6006/v1/traces"))
)
trace.set_tracer_provider(tracer_provider)

# Instrument (auto-traces all API calls)
OpenAIInstrumentor().instrument()
AnthropicInstrumentor().instrument()

# Now all LLM calls are automatically traced!
import anthropic
client = anthropic.Anthropic()
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}]
)
# → Trace appears in Phoenix UI with tokens, latency, cost
```

### LangChain

```python
from openinference.instrumentation.langchain import LangChainInstrumentor

LangChainInstrumentor().instrument()

# All LangChain chains, agents, retrievers are now traced
from langchain_anthropic import ChatAnthropic
from langchain.agents import create_tool_calling_agent

llm = ChatAnthropic(model="claude-sonnet-4-20250514")
agent = create_tool_calling_agent(llm, tools, prompt)
# Every invocation creates a full trace in Phoenix
```

### Custom Instrumentation

```python
from openinference.instrumentation import using_attributes
from opentelemetry import trace

tracer = trace.get_tracer("my-agent")

def handle_query(query: str, user_id: str) -> str:
    # Add session/user context
    with using_attributes(
        session_id=f"session-{user_id}",
        user_id=user_id,
        metadata={"environment": "production", "version": "2.1"},
        tags=["customer-support"],
    ):
        with tracer.start_as_current_span("agent_pipeline") as span:
            span.set_attribute("input.value", query)
            
            # Retrieval span
            with tracer.start_as_current_span("retrieval") as ret_span:
                docs = retrieve(query)
                ret_span.set_attribute("retrieval.documents", str([d.page_content[:100] for d in docs]))
            
            # Generation span (auto-instrumented if using OpenAI/Anthropic instrumentor)
            response = generate(query, docs)
            
            span.set_attribute("output.value", response)
            return response
```

---

## Evaluation with Phoenix

### Built-in Evaluators

```python
from phoenix.evals import (
    HallucinationEvaluator,
    RelevanceEvaluator,
    QAEvaluator,
    ToxicityEvaluator,
    run_evals,
)
from phoenix.evals.models import AnthropicModel

# Setup eval model
eval_model = AnthropicModel(model="claude-3-5-haiku-20241022")

# Get traces from Phoenix
traces_df = px.Client().get_spans_dataframe(project_name="my-agent")

# Run hallucination evaluation
hallucination_eval = HallucinationEvaluator(eval_model)
relevance_eval = RelevanceEvaluator(eval_model)

results = run_evals(
    dataframe=traces_df,
    evaluators=[hallucination_eval, relevance_eval],
    provide_explanation=True,
)

# Results include score + explanation for each trace
print(results[["label", "score", "explanation"]].head())
```

### Custom Evaluator

```python
from phoenix.evals import llm_classify

# Define custom evaluation criteria
EVAL_TEMPLATE = """You are evaluating an AI agent's response quality.

[Query]: {query}
[Response]: {response}
[Retrieved Context]: {context}

Evaluate on these criteria:
1. Does the response directly answer the question? (yes/no)
2. Is all information in the response supported by the context? (yes/no)
3. Is the response concise without unnecessary information? (yes/no)
4. Is the tone professional and helpful? (yes/no)

Provide a score from 0-4 (count of "yes" answers) and brief explanation.
Format: Score: N\nExplanation: ..."""

results = llm_classify(
    dataframe=traces_df,
    model=eval_model,
    template=EVAL_TEMPLATE,
    rails=["0", "1", "2", "3", "4"],
    provide_explanation=True,
)
```

---

## Embeddings Analysis & Drift Detection

```python
import phoenix as px
import pandas as pd
import numpy as np

# Analyze embedding quality and drift
primary_embeddings = pd.DataFrame({
    "embedding": list(current_embeddings),  # Today's query embeddings
    "query": current_queries,
    "cluster": cluster_labels,
})

reference_embeddings = pd.DataFrame({
    "embedding": list(baseline_embeddings),  # Last week's embeddings
    "query": baseline_queries,
    "cluster": baseline_clusters,
})

# Launch Phoenix with embedding comparison
session = px.launch_app(
    primary=px.Inferences(primary_embeddings, "production"),
    reference=px.Inferences(reference_embeddings, "baseline"),
)

# Phoenix UI shows:
# - UMAP visualization of embedding clusters
# - Drift detection (distribution shift between primary/reference)
# - Cluster analysis (new topics appearing, topics disappearing)
# - Outlier detection (queries far from any cluster)
```

### Programmatic Drift Detection

```python
from phoenix.evals import compute_embedding_drift

drift_score = compute_embedding_drift(
    primary_embeddings=current_embeddings,
    reference_embeddings=baseline_embeddings,
    method="psi",  # Population Stability Index
)

if drift_score > 0.2:  # Significant drift
    alert(f"Embedding drift detected: PSI={drift_score:.3f}")
    # Possible causes:
    # - New types of queries appearing
    # - Knowledge base became stale
    # - User behavior changed
```

---

## Production Deployment

### Docker Deployment

```yaml
# docker-compose.yml
services:
  phoenix:
    image: arizephoenix/phoenix:latest
    ports:
      - "6006:6006"
      - "4317:4317"  # OTLP gRPC
    environment:
      - PHOENIX_SQL_DATABASE_URL=postgresql://user:pass@postgres:5432/phoenix
      - PHOENIX_ENABLE_AUTH=true
    volumes:
      - phoenix-data:/data

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: phoenix
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    volumes:
      - pg-data:/var/lib/postgresql/data

volumes:
  phoenix-data:
  pg-data:
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: phoenix
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: phoenix
          image: arizephoenix/phoenix:latest
          ports:
            - containerPort: 6006
            - containerPort: 4317
          env:
            - name: PHOENIX_SQL_DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: phoenix-secrets
                  key: database-url
```

---

## Next: [Weights & Biases →](04_Weights_and_Biases.md)
