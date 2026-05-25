# 6. Datadog & New Relic — Enterprise AI Monitoring

## Datadog LLM Observability

### Setup

```python
# pip install ddtrace

# Auto-instrument (traces all LLM calls)
# DD_LLMOBS_ENABLED=1 DD_LLMOBS_ML_APP=my-agent ddtrace-run python app.py

from ddtrace.llmobs import LLMObs
from ddtrace.llmobs.decorators import workflow, task, tool, agent, retrieval

LLMObs.enable(
    ml_app="customer-support-agent",
    api_key="dd-api-key",
    site="datadoghq.com",
    agentless_enabled=True,
)
```

### Tracing Agent Workflows

```python
@workflow
def handle_support_query(query: str, user_id: str) -> str:
    LLMObs.annotate(
        input_data=query,
        metadata={"user_id": user_id, "channel": "web"},
        tags={"team": "support", "priority": "normal"},
    )
    
    intent = classify_intent(query)
    context = retrieve_docs(query)
    response = generate_response(query, context)
    
    LLMObs.annotate(output_data=response)
    return response

@task
def classify_intent(query: str) -> str:
    # Datadog auto-captures LLM call details
    response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=50,
        messages=[{"role": "user", "content": f"Classify: {query}"}]
    )
    return response.content[0].text

@retrieval
def retrieve_docs(query: str) -> list:
    results = vector_db.search(query, top_k=5)
    LLMObs.annotate(
        input_data=query,
        output_data=[{"content": r.text, "score": r.score} for r in results],
    )
    return results

@tool
def get_subscription(user_id: str) -> dict:
    return api_client.get(f"/subscriptions/{user_id}")
```

### Evaluation Submission

```python
from ddtrace.llmobs import LLMObs

# Submit evaluation scores for traces
LLMObs.submit_evaluation(
    span_context=current_span,
    label="quality",
    metric_type="score",
    value=4.2,  # 1-5 scale
)

LLMObs.submit_evaluation(
    span_context=current_span,
    label="hallucination",
    metric_type="categorical",
    value="none",  # none, minor, major
)
```

### Datadog Dashboard Queries

```
# Average response latency by model
avg:llmobs.request.duration{ml_app:customer-support-agent} by {model_name}

# Token usage over time
sum:llmobs.tokens.total{ml_app:customer-support-agent}.as_count()

# Error rate
sum:llmobs.request.error{ml_app:customer-support-agent}.as_count() / 
sum:llmobs.request.count{ml_app:customer-support-agent}.as_count()

# Cost per hour
sum:llmobs.request.cost{ml_app:customer-support-agent}.rollup(sum, 3600)
```

---

## New Relic AI Monitoring

### Setup

```python
# pip install newrelic

# newrelic.ini or environment variables
# NEW_RELIC_LICENSE_KEY=...
# NEW_RELIC_APP_NAME=my-ai-agent
# NEW_RELIC_AI_MONITORING_ENABLED=true

import newrelic.agent
newrelic.agent.initialize('newrelic.ini')

# Auto-instruments OpenAI, LangChain, AWS Bedrock
# For Anthropic, use custom instrumentation:
```

### Custom AI Monitoring

```python
import newrelic.agent

@newrelic.agent.function_trace(name='agent_pipeline')
def handle_query(query: str) -> str:
    # Record custom AI event
    newrelic.agent.record_custom_event('AIAgentRequest', {
        'query': query[:200],
        'query_length': len(query),
        'timestamp': time.time(),
    })
    
    response = process_query(query)
    
    # Record LLM metrics
    newrelic.agent.record_custom_metric('AI/ResponseLatency', response.latency_ms)
    newrelic.agent.record_custom_metric('AI/TokensUsed', response.total_tokens)
    newrelic.agent.record_custom_metric('AI/Cost', response.cost)
    newrelic.agent.record_custom_metric('AI/QualityScore', response.quality_score)
    
    # Record AI event for analytics
    newrelic.agent.record_custom_event('AIAgentResponse', {
        'model': response.model,
        'tokens_in': response.input_tokens,
        'tokens_out': response.output_tokens,
        'latency_ms': response.latency_ms,
        'cost_usd': response.cost,
        'intent': response.intent,
        'tool_calls': response.num_tool_calls,
        'quality_score': response.quality_score,
        'escalated': response.escalated,
    })
    
    return response.content
```

### NRQL Queries (New Relic Query Language)

```sql
-- Average latency by model
SELECT average(latency_ms) FROM AIAgentResponse 
FACET model SINCE 1 hour ago TIMESERIES

-- Cost per conversation (last 24h)
SELECT sum(cost_usd) / uniqueCount(conversation_id) as 'Cost/Conv'
FROM AIAgentResponse SINCE 24 hours ago

-- Hallucination rate
SELECT percentage(count(*), WHERE quality_score < 0.7) as 'Low Quality Rate'
FROM AIAgentResponse SINCE 1 hour ago TIMESERIES

-- Top escalation reasons
SELECT count(*) FROM AIAgentResponse 
WHERE escalated = true FACET intent SINCE 24 hours ago

-- Token usage trend
SELECT sum(tokens_in + tokens_out) FROM AIAgentResponse 
SINCE 7 days ago TIMESERIES 1 hour

-- P95 latency by intent
SELECT percentile(latency_ms, 95) FROM AIAgentResponse 
FACET intent SINCE 1 hour ago
```

### Alerting

```yaml
# New Relic alert conditions
- name: "AI Agent High Error Rate"
  type: NRQL
  query: "SELECT percentage(count(*), WHERE error = true) FROM AIAgentResponse"
  threshold: 5  # Alert if >5% errors
  duration: 5   # For 5 minutes

- name: "AI Agent High Latency"
  type: NRQL
  query: "SELECT percentile(latency_ms, 95) FROM AIAgentResponse"
  threshold: 8000  # Alert if P95 > 8s
  duration: 3

- name: "AI Agent Cost Spike"
  type: NRQL
  query: "SELECT sum(cost_usd) FROM AIAgentResponse SINCE 1 hour ago"
  threshold: 100  # Alert if >$100/hour
  
- name: "Hallucination Rate Spike"
  type: NRQL
  query: "SELECT percentage(count(*), WHERE quality_score < 0.7) FROM AIAgentResponse"
  threshold: 10  # Alert if >10% low quality
  duration: 10
```

---

## RUM (Real User Monitoring) for Agent UI

```javascript
// New Relic Browser agent for chat widget
newrelic.addPageAction('agent_message_sent', {
  conversationId: convId,
  messageLength: message.length,
});

newrelic.addPageAction('agent_ttft', {
  conversationId: convId,
  timeToFirstToken: ttftMs,
});

newrelic.addPageAction('agent_response_complete', {
  conversationId: convId,
  totalLatency: totalMs,
  responseLength: response.length,
});

newrelic.addPageAction('agent_feedback', {
  conversationId: convId,
  rating: 'positive',  // or 'negative'
  messageId: msgId,
});

newrelic.addPageAction('agent_escalation', {
  conversationId: convId,
  reason: 'user_requested',
  messagesBeforeEscalation: messageCount,
});
```

---

## Next: [Evaluation Frameworks →](07_Evaluation.md)
