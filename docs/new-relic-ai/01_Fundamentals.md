# 1. New Relic AI Monitoring — Fundamentals

## Theory: Full-Stack Observability for AI

Traditional observability answers: "Is the service up? Is it fast?"
AI observability must also answer: "Is the agent giving correct, safe, helpful answers?"

```
┌─────────────────────────────────────────────────────────────┐
│  OBSERVABILITY LAYERS FOR AI AGENTS                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Layer 5: BUSINESS (New Relic + Custom)                     │
│  "Is the agent achieving business goals?"                   │
│  → CSAT, resolution rate, deflection rate, cost/resolution  │
│                                                              │
│  Layer 4: AI QUALITY (New Relic AI Monitoring)              │
│  "Is the agent giving good answers?"                        │
│  → Token usage, model performance, feedback, hallucination  │
│                                                              │
│  Layer 3: APPLICATION (New Relic APM)                       │
│  "Is the application working correctly?"                    │
│  → Latency, errors, throughput, distributed traces          │
│                                                              │
│  Layer 2: USER EXPERIENCE (New Relic Browser/RUM)           │
│  "Is the user having a good experience?"                    │
│  → Core Web Vitals, TTFT, interaction latency, JS errors   │
│                                                              │
│  Layer 1: INFRASTRUCTURE (New Relic Infra)                  │
│  "Is the infrastructure healthy?"                           │
│  → CPU, memory, GPU, network, container health             │
└─────────────────────────────────────────────────────────────┘
```

### What New Relic AI Monitoring Captures Automatically

| Data Point | Description | How It's Used |
|-----------|-------------|---------------|
| `ai.model` | Model name (gpt-4o, claude-sonnet) | Filter by model |
| `ai.tokens.input` | Input token count | Cost calculation |
| `ai.tokens.output` | Output token count | Cost calculation |
| `ai.response.time` | LLM response latency | Performance monitoring |
| `ai.completion.id` | Unique completion ID | Trace correlation |
| `ai.conversation.id` | Conversation thread ID | Multi-turn analysis |
| `ai.feedback.rating` | User feedback (1-5 or thumbs) | Quality tracking |
| `ai.request.model` | Requested model | Routing analysis |
| `ai.response.finish_reason` | Why generation stopped | Error detection |
| `ai.error` | Error type if failed | Error tracking |

---

## Setup

### Install New Relic Agent

```bash
# Python
pip install newrelic

# Generate config
newrelic-admin generate-config YOUR_LICENSE_KEY newrelic.ini

# Run with agent
NEW_RELIC_CONFIG_FILE=newrelic.ini newrelic-admin run-program python app.py
```

### Configuration (newrelic.ini)

```ini
[newrelic]
license_key = YOUR_LICENSE_KEY
app_name = AI Support Agent (Production)

# AI Monitoring (enable LLM tracking)
ai_monitoring.enabled = true
ai_monitoring.streaming.enabled = true
ai_monitoring.record_content.enabled = true  # Log prompts/responses (disable for privacy)

# Distributed tracing
distributed_tracing.enabled = true
span_events.enabled = true

# Custom events (for AI metrics)
custom_insights_events.enabled = true

# Transaction tracer
transaction_tracer.enabled = true
transaction_tracer.record_sql = obfuscated

# Error collector
error_collector.enabled = true
error_collector.ignore_classes = 
```

### Environment Variables (Alternative)

```bash
export NEW_RELIC_LICENSE_KEY="YOUR_KEY"
export NEW_RELIC_APP_NAME="AI Support Agent"
export NEW_RELIC_AI_MONITORING_ENABLED=true
export NEW_RELIC_AI_MONITORING_RECORD_CONTENT_ENABLED=true
export NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true
export NEW_RELIC_LOG_LEVEL=info
```

---

## Auto-Instrumentation

New Relic automatically instruments popular LLM libraries:

### Supported Libraries (Auto-Instrumented)

| Library | What's Captured | Version |
|---------|----------------|---------|
| `openai` | Chat completions, embeddings, images | ≥1.0 |
| `anthropic` | Messages, streaming | ≥0.20 |
| `boto3` (Bedrock) | Invoke model, converse | ≥1.28 |
| `langchain` | Chains, agents, retrievers, tools | ≥0.1 |
| `llama-index` | Query engines, retrievers | ≥0.10 |
| `google-generativeai` | Gemini generate_content | ≥0.3 |
| `cohere` | Chat, embed, rerank | ≥5.0 |

### How Auto-Instrumentation Works

```python
# Just import and use — New Relic instruments automatically!
import newrelic.agent
newrelic.agent.initialize('newrelic.ini')

# These calls are automatically traced:
import anthropic
client = anthropic.Anthropic()

response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}]
)
# New Relic automatically captures:
# - Model name, tokens in/out, latency
# - Full prompt and response (if record_content enabled)
# - Error if request fails
# - Distributed trace span

# LangChain is also auto-instrumented:
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage

llm = ChatAnthropic(model="claude-sonnet-4-20250514")
response = llm.invoke([HumanMessage(content="Hello!")])
# Automatically traced as part of the LangChain chain
```

### What Auto-Instrumentation Creates

```
Transaction: WebTransaction/Function/handle_query
│
├── Span: External/api.anthropic.com/messages (1200ms)
│   ├── ai.model: claude-sonnet-4-20250514
│   ├── ai.tokens.input: 150
│   ├── ai.tokens.output: 320
│   ├── ai.response.time: 1180ms
│   ├── ai.completion.id: msg_abc123
│   └── ai.request.temperature: 0.7
│
├── Span: External/pinecone.io/query (85ms)
│   ├── db.system: pinecone
│   ├── db.operation: query
│   └── db.vector.top_k: 5
│
└── Span: External/api.anthropic.com/messages (800ms)
    ├── ai.model: claude-3-5-haiku-20241022
    ├── ai.tokens.input: 50
    ├── ai.tokens.output: 10
    └── ai.purpose: classification
```

---

## Custom Instrumentation (Beyond Auto)

```python
import newrelic.agent

# ============ CUSTOM TRANSACTION ============
@newrelic.agent.background_task(name='agent_conversation')
def handle_conversation(user_id: str, query: str):
    """Wrap entire agent pipeline as a transaction."""
    
    # Add custom attributes to the transaction
    newrelic.agent.add_custom_attribute('user_id', user_id)
    newrelic.agent.add_custom_attribute('query_length', len(query))
    newrelic.agent.add_custom_attribute('agent_version', 'v2.1')
    
    # Process...
    result = process_query(query)
    
    # Add result attributes
    newrelic.agent.add_custom_attribute('response_length', len(result.text))
    newrelic.agent.add_custom_attribute('tools_called', result.tool_count)
    newrelic.agent.add_custom_attribute('escalated', result.escalated)
    
    return result

# ============ CUSTOM SPANS (Function Traces) ============
@newrelic.agent.function_trace(name='intent_classification')
def classify_intent(query: str) -> str:
    """Creates a span within the current transaction."""
    # LLM call here is auto-instrumented
    response = client.messages.create(...)
    
    # Add span-level attributes
    newrelic.agent.add_custom_span_attribute('intent', response.content[0].text)
    newrelic.agent.add_custom_span_attribute('confidence', 0.95)
    
    return response.content[0].text

@newrelic.agent.function_trace(name='rag_retrieval')
def retrieve_context(query: str) -> list:
    """Trace retrieval separately from generation."""
    results = vector_store.search(query, top_k=5)
    
    newrelic.agent.add_custom_span_attribute('num_results', len(results))
    newrelic.agent.add_custom_span_attribute('top_score', results[0].score if results else 0)
    newrelic.agent.add_custom_span_attribute('retrieval_method', 'hybrid')
    
    return results

@newrelic.agent.function_trace(name='guardrail_check')
def check_guardrails(response: str) -> dict:
    """Trace safety checks."""
    result = safety_check(response)
    
    newrelic.agent.add_custom_span_attribute('guardrail_passed', result['safe'])
    newrelic.agent.add_custom_span_attribute('grounding_score', result['grounding'])
    
    if not result['safe']:
        newrelic.agent.add_custom_span_attribute('block_reason', result['reason'])
    
    return result

# ============ CUSTOM EVENTS ============
def record_ai_event(event_type: str, data: dict):
    """Record custom event for NRQL querying."""
    newrelic.agent.record_custom_event(event_type, data)

# Usage:
record_ai_event('AIAgentConversation', {
    'conversation_id': conv_id,
    'user_id': user_id,
    'intent': 'billing',
    'model': 'claude-sonnet-4',
    'tokens_in': 1500,
    'tokens_out': 350,
    'latency_ms': 2100,
    'cost_usd': 0.0098,
    'task_completed': True,
    'escalated': False,
    'feedback_score': 4,
    'tools_used': 'get_subscription,search_docs',
    'guardrail_triggered': False,
    'grounding_score': 0.94,
    'agent_version': 'v2.1',
})

record_ai_event('AIAgentToolCall', {
    'conversation_id': conv_id,
    'tool_name': 'get_subscription',
    'tool_latency_ms': 120,
    'tool_success': True,
    'tool_input_size': 50,
    'tool_output_size': 200,
})

record_ai_event('AIAgentFeedback', {
    'conversation_id': conv_id,
    'message_id': msg_id,
    'rating': 'positive',  # or 'negative'
    'comment': 'Very helpful!',
})

# ============ CUSTOM METRICS ============
newrelic.agent.record_custom_metric('AI/Tokens/Total', tokens_in + tokens_out)
newrelic.agent.record_custom_metric('AI/Cost/PerConversation', cost_usd)
newrelic.agent.record_custom_metric('AI/Quality/GroundingScore', grounding_score)
newrelic.agent.record_custom_metric('AI/Quality/HallucinationDetected', 1 if hallucinated else 0)
```

---

## AI Monitoring UI (New Relic One)

### What You See in the Dashboard

```
New Relic One → AI Monitoring:

┌─────────────────────────────────────────────────────────────┐
│  AI RESPONSES                                                │
│  ├── Total responses: 45,230 (last 24h)                    │
│  ├── Average response time: 2.1s                            │
│  ├── Error rate: 0.3%                                       │
│  ├── Average tokens/response: 1,850                         │
│  └── Models used: claude-sonnet-4 (80%), haiku (20%)       │
│                                                              │
│  MODEL COMPARISON                                            │
│  ┌────────────────┬──────────┬──────────┬─────────┐        │
│  │ Model          │ Latency  │ Tokens   │ Errors  │        │
│  ├────────────────┼──────────┼──────────┼─────────┤        │
│  │ claude-sonnet-4│ 2.3s     │ 2,100    │ 0.2%    │        │
│  │ claude-haiku   │ 0.8s     │ 800      │ 0.5%    │        │
│  │ gpt-4o         │ 1.9s     │ 1,900    │ 0.3%    │        │
│  └────────────────┴──────────┴──────────┴─────────┘        │
│                                                              │
│  CONVERSATION TRACES (click to see full trace)              │
│  • conv-123: 3 turns, 2.1s total, tools: get_subscription  │
│  • conv-456: 5 turns, 8.3s total, ESCALATED                │
│  • conv-789: 1 turn, 0.9s, feedback: negative ⚠️           │
└─────────────────────────────────────────────────────────────┘
```

---

## Feedback Tracking

```python
# Record user feedback linked to specific AI response
from newrelic.agent import record_llm_feedback_event

def record_feedback(trace_id: str, rating: str, message_id: str, category: str = None):
    """Record user feedback for an AI response."""
    record_llm_feedback_event(
        trace_id=trace_id,
        rating=rating,  # "positive", "negative", or numeric 1-5
        category=category,  # "helpful", "accurate", "fast", etc.
        message=f"Feedback for message {message_id}",
        metadata={
            "message_id": message_id,
            "agent_version": "v2.1",
        }
    )

# In your API endpoint:
@app.post("/feedback")
async def submit_feedback(request: FeedbackRequest):
    record_feedback(
        trace_id=request.trace_id,
        rating="positive" if request.thumbs_up else "negative",
        message_id=request.message_id,
    )
```

---

## Next: [Tracing AI Agents →](02_Tracing.md)
