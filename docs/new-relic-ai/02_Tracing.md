# 2. Tracing AI Agents with New Relic

## Theory: Why Distributed Tracing for AI Agents?

AI agents make **multiple external calls** per request: LLM API, vector DB, tools/APIs, guardrails. Without distributed tracing, you can't answer:
- "Why was this response slow?" (which call took longest?)
- "Why did the agent give a wrong answer?" (what context did it have?)
- "Where did the error occur?" (LLM? Tool? Retrieval?)

```
A single agent request may involve:
  1. Intent classification (LLM call #1) — 200ms
  2. RAG retrieval (vector DB query) — 100ms
  3. Reranking (LLM call #2 or API) — 150ms
  4. Tool execution (external API) — 300ms
  5. Response generation (LLM call #3) — 1500ms
  6. Guardrail check (LLM call #4 or classifier) — 100ms
  Total: 2350ms

Without tracing: "Average latency is 2.3s" (not actionable)
With tracing: "Response generation is 64% of latency — optimize there first"
```

---

## Complete Agent Trace Example

```python
import newrelic.agent
import anthropic
import time

client = anthropic.Anthropic()

@newrelic.agent.background_task(name='agent/handle_query')
def handle_query(query: str, user_id: str, session_id: str) -> dict:
    """Full agent pipeline — each step is a traced span."""
    
    start = time.time()
    
    # Transaction-level attributes (visible on all spans)
    newrelic.agent.add_custom_attribute('user_id', user_id)
    newrelic.agent.add_custom_attribute('session_id', session_id)
    newrelic.agent.add_custom_attribute('query', query[:200])  # Truncate for privacy
    
    # Step 1: Classify intent
    intent, confidence = classify_intent(query)
    newrelic.agent.add_custom_attribute('intent', intent)
    newrelic.agent.add_custom_attribute('intent_confidence', confidence)
    
    # Step 2: Retrieve context
    docs = retrieve_context(query, intent)
    newrelic.agent.add_custom_attribute('docs_retrieved', len(docs))
    
    # Step 3: Check if tool use is needed
    tool_results = None
    if needs_tool(intent, query):
        tool_results = execute_tools(query, user_id)
        newrelic.agent.add_custom_attribute('tools_used', ','.join(t['name'] for t in tool_results))
    
    # Step 4: Generate response
    response = generate_response(query, docs, tool_results, intent)
    
    # Step 5: Safety check
    safety_result = check_safety(response['text'], docs)
    newrelic.agent.add_custom_attribute('grounding_score', safety_result['grounding'])
    newrelic.agent.add_custom_attribute('safety_passed', safety_result['safe'])
    
    if not safety_result['safe']:
        newrelic.agent.add_custom_attribute('safety_block_reason', safety_result['reason'])
        response['text'] = "I'm not confident in my answer. Let me connect you with a specialist."
        newrelic.agent.add_custom_attribute('response_blocked', True)
    
    # Record total metrics
    total_latency = (time.time() - start) * 1000
    newrelic.agent.add_custom_attribute('total_latency_ms', total_latency)
    newrelic.agent.add_custom_attribute('total_tokens', response.get('total_tokens', 0))
    newrelic.agent.add_custom_attribute('cost_usd', response.get('cost', 0))
    
    # Record as custom event for NRQL analysis
    newrelic.agent.record_custom_event('AIAgentRequest', {
        'user_id': user_id,
        'session_id': session_id,
        'intent': intent,
        'intent_confidence': confidence,
        'docs_retrieved': len(docs),
        'tools_used': len(tool_results) if tool_results else 0,
        'total_latency_ms': total_latency,
        'total_tokens': response.get('total_tokens', 0),
        'cost_usd': response.get('cost', 0),
        'grounding_score': safety_result['grounding'],
        'safety_passed': safety_result['safe'],
        'model': response.get('model', 'unknown'),
        'agent_version': 'v2.1',
    })
    
    return response


@newrelic.agent.function_trace(name='agent/classify_intent')
def classify_intent(query: str) -> tuple:
    """Traced: Intent classification."""
    response = client.messages.create(
        model="claude-3-5-haiku-20241022",
        max_tokens=50,
        messages=[{"role": "user", "content": f"Classify intent (billing/technical/account/general): {query}"}],
    )
    intent = response.content[0].text.strip().lower()
    
    newrelic.agent.add_custom_span_attribute('classified_intent', intent)
    newrelic.agent.add_custom_span_attribute('classification_model', 'claude-3-5-haiku')
    newrelic.agent.add_custom_span_attribute('classification_tokens', response.usage.input_tokens + response.usage.output_tokens)
    
    return intent, 0.95


@newrelic.agent.function_trace(name='agent/retrieve_context')
def retrieve_context(query: str, intent: str) -> list:
    """Traced: RAG retrieval."""
    # Vector search (auto-instrumented if using supported client)
    results = vector_store.search(
        query=query,
        top_k=10,
        filter={"category": intent} if intent != "general" else None,
    )
    
    # Rerank
    reranked = reranker.rerank(query, [r.text for r in results], top_n=5)
    
    newrelic.agent.add_custom_span_attribute('search_top_k', 10)
    newrelic.agent.add_custom_span_attribute('reranked_top_n', 5)
    newrelic.agent.add_custom_span_attribute('top_score', reranked[0].score if reranked else 0)
    newrelic.agent.add_custom_span_attribute('filter_applied', intent != "general")
    
    return reranked


@newrelic.agent.function_trace(name='agent/execute_tools')
def execute_tools(query: str, user_id: str) -> list:
    """Traced: Tool execution."""
    results = []
    
    # Each tool call gets its own span
    with newrelic.agent.FunctionTrace(name=f'tool/get_subscription'):
        sub_result = get_subscription_api(user_id)
        newrelic.agent.add_custom_span_attribute('tool_name', 'get_subscription')
        newrelic.agent.add_custom_span_attribute('tool_success', sub_result is not None)
        results.append({'name': 'get_subscription', 'result': sub_result})
    
    return results


@newrelic.agent.function_trace(name='agent/generate_response')
def generate_response(query: str, docs: list, tools: list, intent: str) -> dict:
    """Traced: LLM response generation (auto-instrumented by New Relic)."""
    context = "\n\n".join([d.text for d in docs])
    tool_context = "\n".join([str(t['result']) for t in (tools or [])])
    
    # This LLM call is auto-instrumented — New Relic captures tokens, latency, model
    response = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=2048,
        system="You are a helpful support agent. Answer based on context only.",
        messages=[{"role": "user", "content": f"Context:\n{context}\n\nTools:\n{tool_context}\n\nQuestion: {query}"}],
        temperature=0,
    )
    
    return {
        'text': response.content[0].text,
        'model': response.model,
        'total_tokens': response.usage.input_tokens + response.usage.output_tokens,
        'cost': calculate_cost(response),
    }


@newrelic.agent.function_trace(name='agent/check_safety')
def check_safety(response_text: str, sources: list) -> dict:
    """Traced: Guardrail/safety check."""
    # Grounding check
    grounding_score = check_grounding(response_text, sources)
    
    # Toxicity check
    is_toxic = check_toxicity(response_text)
    
    # PII check
    has_pii = check_pii(response_text)
    
    safe = grounding_score > 0.7 and not is_toxic and not has_pii
    
    newrelic.agent.add_custom_span_attribute('grounding_score', grounding_score)
    newrelic.agent.add_custom_span_attribute('is_toxic', is_toxic)
    newrelic.agent.add_custom_span_attribute('has_pii', has_pii)
    newrelic.agent.add_custom_span_attribute('overall_safe', safe)
    
    return {
        'safe': safe,
        'grounding': grounding_score,
        'reason': 'hallucination' if grounding_score <= 0.7 else ('toxic' if is_toxic else ('pii' if has_pii else None)),
    }
```

---

## Trace Visualization in New Relic

```
New Relic One → Distributed Tracing → Select trace:

Transaction: agent/handle_query (2,350ms total)
├── agent/classify_intent (210ms)
│   └── External/api.anthropic.com (195ms) [claude-3-5-haiku, 60 tokens]
├── agent/retrieve_context (285ms)
│   ├── External/pinecone.io/query (95ms) [top_k=10, score=0.89]
│   └── External/api.cohere.com/rerank (180ms) [top_n=5]
├── agent/execute_tools (320ms)
│   └── tool/get_subscription (310ms) [success=true]
├── agent/generate_response (1,420ms)
│   └── External/api.anthropic.com (1,380ms) [claude-sonnet-4, 1850 tokens]
└── agent/check_safety (115ms)
    └── External/api.anthropic.com (100ms) [grounding check, score=0.94]

Attributes:
  user_id: u-456
  intent: billing (confidence: 0.95)
  docs_retrieved: 5
  tools_used: get_subscription
  total_tokens: 1910
  cost_usd: $0.0098
  grounding_score: 0.94
  safety_passed: true
```

---

## Tracing Multi-Turn Conversations

```python
# Link multiple requests in the same conversation
@newrelic.agent.background_task(name='agent/handle_query')
def handle_query(query: str, conversation_id: str, turn_number: int):
    # All turns in same conversation share this attribute
    newrelic.agent.add_custom_attribute('conversation_id', conversation_id)
    newrelic.agent.add_custom_attribute('turn_number', turn_number)
    
    # ... process query ...

# In New Relic, query all turns of a conversation:
# FROM Transaction SELECT * WHERE conversation_id = 'conv-123' ORDER BY turn_number
```

---

## Tracing Across Microservices

```python
# Service A: API Gateway
@newrelic.agent.web_transaction(name='api/chat')
def chat_endpoint(request):
    # New Relic automatically propagates trace context via headers
    response = requests.post(
        "http://agent-service/query",
        json={"query": request.json["query"]},
        headers=dict(request.headers),  # Trace headers propagated
    )
    return response.json()

# Service B: Agent Service
@newrelic.agent.web_transaction(name='agent/process')
def process_query(request):
    # This transaction is automatically linked to Service A's trace
    # Full distributed trace: Gateway → Agent → LLM → Vector DB → Tools
    result = handle_query(request.json["query"])
    return result
```

---

## Error Tracking for AI Agents

```python
# Errors are automatically captured, but add context:
try:
    response = client.messages.create(...)
except anthropic.RateLimitError as e:
    newrelic.agent.notice_error(
        error=e,
        attributes={
            'error_type': 'rate_limit',
            'model': 'claude-sonnet-4',
            'retry_after': e.response.headers.get('retry-after'),
        }
    )
    # Fallback to different model...
except anthropic.APIError as e:
    newrelic.agent.notice_error(
        error=e,
        attributes={
            'error_type': 'api_error',
            'status_code': e.status_code,
            'model': 'claude-sonnet-4',
        }
    )
```

---

## Next: [Metrics & Dashboards →](03_Metrics_Dashboards.md)
