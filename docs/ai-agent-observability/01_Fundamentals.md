# 1. AI Agent Observability — Fundamentals & Architecture

## The Problem

AI agents are **non-deterministic**, **multi-step**, and **expensive**. Traditional monitoring tells you *if* something broke, but AI observability tells you *why the agent gave a bad answer*.

```
Traditional Bug: HTTP 500 → Check logs → Fix code → Deploy
AI Agent Bug:   "Agent said wrong thing" → WHY?
                ├── Bad prompt? (prompt engineering issue)
                ├── Bad retrieval? (RAG/knowledge issue)
                ├── Wrong tool called? (routing issue)
                ├── Hallucination? (model issue)
                ├── Context too long? (truncation issue)
                └── Guardrail too strict? (safety issue)
```

---

## Five Pillars of AI Agent Observability

### 1. Tracing (What happened?)

Full execution trace of every agent interaction:

```
Trace: conversation-abc-123 (total: 2.3s, cost: $0.04)
│
├── Span: input_guardrail (12ms) ✅
│   └── safety_check: PASS
│
├── Span: intent_classification (180ms, $0.001)
│   ├── model: claude-3-5-haiku
│   ├── tokens_in: 150, tokens_out: 10
│   └── result: "subscription_billing"
│
├── Span: rag_retrieval (95ms)
│   ├── query: "cancel subscription refund"
│   ├── top_k: 5
│   ├── relevance_scores: [0.92, 0.87, 0.81, 0.65, 0.52]
│   └── sources: ["billing-faq.md", "cancel-policy.md", ...]
│
├── Span: tool_call: get_subscription (120ms)
│   ├── input: {"user_id": "u-123"}
│   ├── output: {"plan": "Pro", "status": "active", "renewal": "2024-02-15"}
│   └── status: SUCCESS
│
├── Span: llm_generation (1800ms, $0.035)
│   ├── model: claude-sonnet-4
│   ├── tokens_in: 2400, tokens_out: 350
│   ├── temperature: 0.3
│   └── stop_reason: end_turn
│
└── Span: output_guardrail (45ms) ✅
    ├── hallucination_check: PASS (grounding: 0.94)
    ├── brand_check: PASS
    └── pii_check: PASS
```

### 2. Evaluation (How good was it?)

Automated quality scoring of every response:

| Dimension | Method | Score |
|-----------|--------|-------|
| Faithfulness | Is response grounded in retrieved context? | 0.94 |
| Relevance | Does it answer the user's question? | 0.88 |
| Helpfulness | Would a human find this useful? | 4.2/5 |
| Harmlessness | Is it safe and appropriate? | 1.0 |
| Correctness | Are facts accurate? | 0.91 |

### 3. Metrics (Aggregate health)

```
┌─────────────────────────────────────────────────────────┐
│  REAL-TIME DASHBOARD                                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Conversations/min: 847    │  Avg Latency: 2.1s         │
│  Task Completion: 84.2%    │  P95 Latency: 4.8s         │
│  Escalation Rate: 12.3%   │  Error Rate: 0.8%           │
│  CSAT: 4.3/5              │  Cost/Conv: $0.08           │
│                                                          │
│  ⚠️ Alerts:                                             │
│  • Hallucination rate ↑ 3.2% (threshold: 2%)           │
│  • Tool "get_billing" latency ↑ 500ms                  │
│  • Token usage ↑ 40% (possible prompt regression)       │
└─────────────────────────────────────────────────────────┘
```

### 4. Debugging (Root cause analysis)

When something goes wrong, you need to:
1. Find the failing conversation
2. See the full trace (every LLM call, tool call, retrieval)
3. See the exact prompt sent to the model
4. See the exact response received
5. Understand WHY it failed (wrong retrieval? bad prompt? model error?)

### 5. Continuous Improvement (Feedback loop)

```
Production Data → Identify Failures → Create Test Cases → 
Improve Prompts/RAG → Evaluate → Deploy → Monitor
```

---

## Key Metrics for AI Agents

### Latency Metrics

| Metric | Definition | Target | Alert |
|--------|-----------|--------|-------|
| Time to First Token (TTFT) | Time until streaming starts | <1s | >3s |
| Total Response Time | End-to-end latency | <3s | >8s |
| LLM Latency | Time spent in model inference | <2s | >5s |
| Tool Execution Time | Time for external API calls | <500ms | >2s |
| Retrieval Latency | Vector search time | <100ms | >500ms |

### Quality Metrics

| Metric | Definition | Target | Alert |
|--------|-----------|--------|-------|
| Task Completion Rate | % of queries successfully resolved | >85% | <70% |
| Hallucination Rate | % of responses with fabricated info | <2% | >5% |
| Faithfulness Score | Grounding in retrieved context | >0.9 | <0.7 |
| Relevance Score | Answer addresses the question | >0.85 | <0.7 |
| Guardrail Trigger Rate | % blocked by safety filters | <5% | >15% |

### Cost Metrics

| Metric | Definition | Target | Alert |
|--------|-----------|--------|-------|
| Cost per Conversation | Total LLM + infra cost | <$0.10 | >$0.50 |
| Tokens per Request | Average token consumption | <3000 | >8000 |
| Cache Hit Rate | % of requests served from cache | >30% | <10% |
| Cost per Resolution | Cost for successfully resolved queries | <$0.15 | >$1.00 |

### Business Metrics

| Metric | Definition | Target | Alert |
|--------|-----------|--------|-------|
| CSAT | Customer satisfaction (1-5) | >4.2 | <3.5 |
| Resolution Rate | % resolved without human | >70% | <50% |
| Escalation Rate | % requiring human handoff | <20% | >35% |
| Repeat Contact Rate | Same user, same issue within 7d | <10% | >20% |
| Deflection Rate | Tickets avoided by agent | >40% | <25% |

---

## Architecture Patterns

### Pattern 1: Inline Instrumentation

```python
# Instrument directly in agent code
import time
from opentelemetry import trace

tracer = trace.get_tracer("ai-agent")

async def handle_message(query: str) -> str:
    with tracer.start_as_current_span("agent_request") as span:
        span.set_attribute("query", query)
        span.set_attribute("query_length", len(query))
        
        # Intent classification
        with tracer.start_as_current_span("intent_classification") as intent_span:
            intent = await classify_intent(query)
            intent_span.set_attribute("intent", intent)
        
        # RAG retrieval
        with tracer.start_as_current_span("rag_retrieval") as rag_span:
            docs = await retrieve(query)
            rag_span.set_attribute("num_docs", len(docs))
            rag_span.set_attribute("top_score", docs[0].score if docs else 0)
        
        # LLM generation
        with tracer.start_as_current_span("llm_call") as llm_span:
            start = time.time()
            response = await call_llm(query, docs)
            llm_span.set_attribute("model", response.model)
            llm_span.set_attribute("tokens_in", response.usage.input_tokens)
            llm_span.set_attribute("tokens_out", response.usage.output_tokens)
            llm_span.set_attribute("latency_ms", (time.time() - start) * 1000)
        
        return response.content
```

### Pattern 2: Middleware/Decorator Pattern

```python
from functools import wraps

def trace_llm_call(func):
    @wraps(func)
    async def wrapper(*args, **kwargs):
        span_name = f"llm_call.{func.__name__}"
        with tracer.start_as_current_span(span_name) as span:
            start = time.time()
            try:
                result = await func(*args, **kwargs)
                span.set_attribute("status", "success")
                span.set_attribute("tokens", result.usage.total_tokens)
                return result
            except Exception as e:
                span.set_attribute("status", "error")
                span.set_attribute("error", str(e))
                raise
            finally:
                span.set_attribute("duration_ms", (time.time() - start) * 1000)
    return wrapper

@trace_llm_call
async def generate_response(prompt, context):
    return await client.messages.create(...)
```

### Pattern 3: Proxy/Gateway Pattern

```
Client → Agent → LLM Proxy (logs everything) → LLM API
                     │
                     ▼
              Observability Backend
              (traces, metrics, costs)
```

```python
class ObservableLLMClient:
    """Proxy that wraps any LLM client with observability."""
    
    def __init__(self, client, logger):
        self.client = client
        self.logger = logger
    
    async def create_message(self, **kwargs):
        request_id = str(uuid.uuid4())
        start = time.time()
        
        # Log request
        self.logger.log_request(request_id, kwargs)
        
        try:
            response = await self.client.messages.create(**kwargs)
            
            # Log response
            self.logger.log_response(request_id, {
                "model": response.model,
                "tokens_in": response.usage.input_tokens,
                "tokens_out": response.usage.output_tokens,
                "latency_ms": (time.time() - start) * 1000,
                "stop_reason": response.stop_reason,
                "cost": self._calculate_cost(response),
            })
            
            return response
        except Exception as e:
            self.logger.log_error(request_id, str(e))
            raise
```

---

## Interview Questions & Answers

### "How would you detect hallucinations in production?"

**Answer framework:**
1. **Factual grounding check**: Compare response claims against retrieved context using NLI (Natural Language Inference) model
2. **Self-consistency**: Ask the model the same question multiple times; inconsistent answers suggest hallucination
3. **Citation verification**: If response cites sources, verify the cited content actually supports the claim
4. **Confidence scoring**: Low model confidence (high entropy in token probabilities) correlates with hallucination
5. **Human feedback loop**: Sample responses for human review; use as training signal

### "What SLOs would you define for an AI agent?"

```
SLO 1: Availability
  - 99.9% of requests get a response (not error)
  - Measured: success_count / total_count over 30-day window

SLO 2: Latency
  - 95% of responses complete within 5 seconds
  - Measured: p95(response_time) over 1-hour windows

SLO 3: Quality
  - 90% of responses score ≥4/5 on automated eval
  - Measured: avg(quality_score) over 24-hour window

SLO 4: Safety
  - <0.1% of responses trigger post-hoc safety violations
  - Measured: safety_violations / total_responses over 7-day window

Error Budget:
  - Quality SLO at 90% means 10% error budget
  - If quality drops to 88%, we've consumed 20% of budget → investigate
  - If quality drops to 85%, we've consumed 50% → freeze deployments
```

### "How do you debug a conversation where the agent gave a wrong answer?"

**Step-by-step:**
1. **Find the trace** → Search by conversation_id or user_id
2. **Check retrieval** → Were relevant documents retrieved? (relevance scores)
3. **Check context** → Was the right context passed to the LLM? (token truncation?)
4. **Check prompt** → Was the system prompt correct? (version mismatch?)
5. **Check model output** → Did the model hallucinate despite good context?
6. **Check guardrails** → Did a guardrail incorrectly modify the response?
7. **Check tools** → Did a tool return incorrect data?
8. **Root cause** → Categorize: retrieval_failure | prompt_issue | model_hallucination | tool_error | context_overflow

---

## Next: [LangSmith →](02_LangSmith.md)
