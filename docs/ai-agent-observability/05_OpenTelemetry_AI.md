# 5. OpenTelemetry for AI (OpenLLMetry)

## Overview

OpenLLMetry extends OpenTelemetry with semantic conventions for LLM applications. It provides vendor-neutral instrumentation that works with any OTel-compatible backend (Jaeger, Grafana Tempo, Datadog, New Relic, etc.).

**Why OpenTelemetry for AI:**
- Vendor-neutral (switch backends without code changes)
- Integrates with existing observability stack
- Standard semantic conventions for LLM spans
- Works with any LLM provider (OpenAI, Anthropic, Cohere, etc.)
- Distributed tracing across microservices

---

## Setup

```python
# pip install openllmetry-sdk opentelemetry-exporter-otlp

from traceloop.sdk import Traceloop

# Initialize with your backend
Traceloop.init(
    app_name="my-ai-agent",
    api_endpoint="http://otel-collector:4318",  # OTLP HTTP endpoint
    # Or use specific backends:
    # api_endpoint="https://api.honeycomb.io",
    # headers={"x-honeycomb-team": "your-api-key"},
)

# Auto-instruments: OpenAI, Anthropic, Cohere, LangChain, LlamaIndex,
# Pinecone, Chroma, Weaviate, and more
```

---

## Semantic Conventions for LLM Spans

```python
# OpenLLMetry automatically adds these attributes to LLM spans:

# LLM Call Attributes
"gen_ai.system" = "anthropic"              # Provider
"gen_ai.request.model" = "claude-sonnet-4" # Model
"gen_ai.request.max_tokens" = 4096
"gen_ai.request.temperature" = 0.7
"gen_ai.response.model" = "claude-sonnet-4-20250514"
"gen_ai.usage.input_tokens" = 1500
"gen_ai.usage.output_tokens" = 350
"gen_ai.usage.total_tokens" = 1850
"gen_ai.response.finish_reason" = "end_turn"

# Prompt/Completion Content (optional, can be disabled for privacy)
"gen_ai.prompt.0.role" = "system"
"gen_ai.prompt.0.content" = "You are a helpful assistant..."
"gen_ai.prompt.1.role" = "user"
"gen_ai.prompt.1.content" = "How do I cancel?"
"gen_ai.completion.0.role" = "assistant"
"gen_ai.completion.0.content" = "To cancel your subscription..."

# Vector DB Attributes
"db.system" = "pinecone"
"db.operation" = "query"
"db.vector.query.top_k" = 5
"db.vector.query.results_count" = 5

# Tool/Function Call Attributes
"gen_ai.tool.name" = "get_subscription"
"gen_ai.tool.input" = '{"user_id": "u-123"}'
"gen_ai.tool.output" = '{"plan": "Pro", "status": "active"}'
```

---

## Custom Instrumentation with Decorators

```python
from traceloop.sdk.decorators import workflow, task, agent, tool

@workflow(name="customer_support")
def handle_support_query(query: str, user_id: str) -> str:
    """Top-level workflow span."""
    intent = classify_intent(query)
    context = retrieve_context(query)
    response = generate_response(query, context, intent)
    return response

@task(name="intent_classification")
def classify_intent(query: str) -> str:
    """Task span — a unit of work."""
    # LLM call is auto-instrumented
    response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=50,
        messages=[{"role": "user", "content": f"Classify: {query}"}]
    )
    return response.content[0].text

@task(name="rag_retrieval")
def retrieve_context(query: str) -> list:
    """Retrieval span — vector DB call is auto-instrumented."""
    results = pinecone_index.query(embed(query), top_k=5)
    return results

@agent(name="response_generator")
def generate_response(query: str, context: list, intent: str) -> str:
    """Agent span — may involve multiple LLM calls and tool use."""
    # All nested LLM calls and tool calls are child spans
    response = run_agent_loop(query, context, intent)
    return response

@tool(name="get_user_subscription")
def get_subscription(user_id: str) -> dict:
    """Tool span — external API call."""
    return api_client.get(f"/users/{user_id}/subscription")
```

---

## Distributed Tracing Across Services

```python
# Service A: API Gateway
from opentelemetry import trace
from opentelemetry.propagate import inject

tracer = trace.get_tracer("api-gateway")

async def handle_request(request):
    with tracer.start_as_current_span("api_request") as span:
        span.set_attribute("user_id", request.user_id)
        
        # Propagate trace context to downstream service
        headers = {}
        inject(headers)  # Adds traceparent header
        
        # Call agent service with trace context
        response = await http_client.post(
            "http://agent-service/query",
            json={"query": request.query},
            headers=headers  # Trace context propagated!
        )
        return response

# Service B: Agent Service (receives trace context automatically)
# The trace continues seamlessly across services
@workflow(name="agent_pipeline")
async def process_query(query: str) -> str:
    # This span is a child of the API Gateway span
    return await run_agent(query)
```

---

## Exporting to Different Backends

### Jaeger

```python
from opentelemetry.exporter.jaeger.thrift import JaegerExporter

Traceloop.init(
    app_name="my-agent",
    exporter=JaegerExporter(agent_host_name="jaeger", agent_port=6831),
)
```

### Grafana Tempo

```python
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

Traceloop.init(
    app_name="my-agent",
    exporter=OTLPSpanExporter(endpoint="http://tempo:4317"),
)
```

### Datadog

```python
Traceloop.init(
    app_name="my-agent",
    api_endpoint="https://trace.agent.datadoghq.com",
    headers={"DD-API-KEY": os.environ["DD_API_KEY"]},
)
```

### New Relic

```python
Traceloop.init(
    app_name="my-agent",
    api_endpoint="https://otlp.nr-data.net:4318",
    headers={"api-key": os.environ["NEW_RELIC_LICENSE_KEY"]},
)
```

---

## Custom Metrics

```python
from opentelemetry import metrics

meter = metrics.get_meter("ai-agent-metrics")

# Counters
token_counter = meter.create_counter("gen_ai.tokens.total", description="Total tokens used")
error_counter = meter.create_counter("gen_ai.errors.total", description="Total errors")
hallucination_counter = meter.create_counter("gen_ai.hallucinations.total")

# Histograms
latency_histogram = meter.create_histogram("gen_ai.latency", unit="ms")
cost_histogram = meter.create_histogram("gen_ai.cost", unit="usd")
quality_histogram = meter.create_histogram("gen_ai.quality_score")

# Usage
def track_llm_call(model, tokens_in, tokens_out, latency_ms, cost):
    token_counter.add(tokens_in + tokens_out, {"model": model, "type": "total"})
    latency_histogram.record(latency_ms, {"model": model})
    cost_histogram.record(cost, {"model": model})

def track_quality(score, conversation_id):
    quality_histogram.record(score, {"conversation_id": conversation_id})
    if score < 0.7:
        error_counter.add(1, {"type": "low_quality"})
```

---

## Privacy Controls

```python
# Disable content logging (for privacy/compliance)
Traceloop.init(
    app_name="my-agent",
    disable_batch=False,
    # Don't log prompt/completion content
    association_properties={
        "log_prompts": False,
        "log_completions": False,
    }
)

# Or selectively redact
from traceloop.sdk import set_association_properties

def handle_query(query: str):
    # Only log metadata, not content
    set_association_properties({
        "user_id": user_id,
        "intent": intent,
        # Don't include PII
    })
```

---

## Next: [Datadog & New Relic AI →](06_Datadog_NewRelic.md)
