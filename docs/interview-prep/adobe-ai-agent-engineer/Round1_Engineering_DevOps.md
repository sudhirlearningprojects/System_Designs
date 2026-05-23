# Round 1 — Software Engineering, Coding, Observability, DevOps & Cloud

## What They're Evaluating

- Can you write clean, efficient code with strong DSA fundamentals? (Java, Python, JavaScript)
- Can you design and build production-grade RESTful APIs and data pipelines?
- Do you understand observability deeply? (New Relic, Splunk, Nagios, RUM)
- Can you operate on public clouds (AWS, Azure, GCP)?
- Can you design AI agent systems that integrate with existing products?
- Do you understand web servers, middleware, and Linux operations?

---

## Part A: Data Structures & Algorithms

### What to Expect

The JD explicitly lists **Algorithms** and **Data Structures** as key skills. Expect LeetCode Medium-Hard level problems, likely with a practical twist related to agent systems.

### High-Priority Topics

| Topic | Why It Matters for This Role | Example Problem |
|-------|------------------------------|------------------|
| **Graphs (BFS/DFS)** | Agent workflow DAGs, dependency resolution | Find execution order for multi-step agent plan |
| **Trees/Tries** | Intent classification, routing trees | Autocomplete for agent suggestions |
| **Hash Maps** | Caching, deduplication, session management | LRU cache for agent responses |
| **Queues/Priority Queues** | Task scheduling, message processing | Priority-based agent request handling |
| **Sliding Window** | Rate limiting, token counting, RUM metrics | Token budget management for LLM calls |
| **Dynamic Programming** | Optimal tool selection, cost minimization | Minimum cost to resolve a query (model selection) |
| **String Matching** | Entity extraction, pattern detection | Extract user intent from natural language |
| **Heap** | Top-K results, real-time analytics | Top-K most common agent failures |

### Practice Problems (Agent-Themed)

```python
# 1. Agent Task Scheduler (Topological Sort)
# Given agent tasks with dependencies, find valid execution order
def schedule_agent_tasks(tasks: List[str], dependencies: List[Tuple[str, str]]) -> List[str]:
    graph = defaultdict(list)
    in_degree = defaultdict(int)
    for task, dep in dependencies:
        graph[dep].append(task)
        in_degree[task] += 1
    
    queue = deque([t for t in tasks if in_degree[t] == 0])
    order = []
    while queue:
        task = queue.popleft()
        order.append(task)
        for next_task in graph[task]:
            in_degree[next_task] -= 1
            if in_degree[next_task] == 0:
                queue.append(next_task)
    
    return order if len(order) == len(tasks) else []  # cycle detection


# 2. Token Budget Allocator (Knapsack variant)
# Given multiple tools with token costs and value scores,
# select tools that maximize value within token budget
def select_tools(tools: List[dict], token_budget: int) -> List[str]:
    # tools = [{"name": "search", "tokens": 500, "value": 8}, ...]
    n = len(tools)
    dp = [[0] * (token_budget + 1) for _ in range(n + 1)]
    
    for i in range(1, n + 1):
        for w in range(token_budget + 1):
            dp[i][w] = dp[i-1][w]
            if tools[i-1]["tokens"] <= w:
                dp[i][w] = max(dp[i][w], 
                    dp[i-1][w - tools[i-1]["tokens"]] + tools[i-1]["value"])
    
    # Backtrack to find selected tools
    selected = []
    w = token_budget
    for i in range(n, 0, -1):
        if dp[i][w] != dp[i-1][w]:
            selected.append(tools[i-1]["name"])
            w -= tools[i-1]["tokens"]
    return selected


# 3. Rate Limiter (Sliding Window)
class SlidingWindowRateLimiter:
    def __init__(self, max_requests: int, window_seconds: int):
        self.max_requests = max_requests
        self.window = window_seconds
        self.requests = defaultdict(deque)  # user_id -> timestamps
    
    def allow_request(self, user_id: str, timestamp: float) -> bool:
        queue = self.requests[user_id]
        # Remove expired timestamps
        while queue and queue[0] <= timestamp - self.window:
            queue.popleft()
        
        if len(queue) < self.max_requests:
            queue.append(timestamp)
            return True
        return False


# 4. Conversation Context Window (Sliding Window on Tokens)
def truncate_conversation(messages: List[dict], max_tokens: int) -> List[dict]:
    """Keep most recent messages that fit within token budget.
    Always keep system message (first) + most recent messages."""
    system_msg = messages[0] if messages[0]["role"] == "system" else None
    user_msgs = messages[1:] if system_msg else messages
    
    result = []
    token_count = count_tokens(system_msg["content"]) if system_msg else 0
    
    # Add messages from most recent, stop when budget exceeded
    for msg in reversed(user_msgs):
        msg_tokens = count_tokens(msg["content"])
        if token_count + msg_tokens > max_tokens:
            break
        result.append(msg)
        token_count += msg_tokens
    
    result.reverse()
    return ([system_msg] if system_msg else []) + result


# 5. LRU Cache for Agent Responses
class AgentResponseCache:
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = OrderedDict()
    
    def get(self, query_hash: str) -> Optional[str]:
        if query_hash in self.cache:
            self.cache.move_to_end(query_hash)
            return self.cache[query_hash]
        return None
    
    def put(self, query_hash: str, response: str):
        if query_hash in self.cache:
            self.cache.move_to_end(query_hash)
        else:
            if len(self.cache) >= self.capacity:
                self.cache.popitem(last=False)
        self.cache[query_hash] = response
```

### Java Coding (JD mentions Java)

```java
// Implement a concurrent request deduplicator for agent API calls
// If same query is in-flight, wait for result instead of making duplicate call
public class RequestDeduplicator<K, V> {
    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();
    
    public V executeOrWait(K key, Callable<V> action) throws Exception {
        CompletableFuture<V> future = new CompletableFuture<>();
        CompletableFuture<V> existing = inFlight.putIfAbsent(key, future);
        
        if (existing != null) {
            // Another thread is already executing this request
            return existing.get(30, TimeUnit.SECONDS);
        }
        
        try {
            V result = action.call();
            future.complete(result);
            return result;
        } catch (Exception e) {
            future.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key);
        }
    }
}
```

---

## Part B: System Design & RESTful APIs

### 1. System Design for AI Agents

**Expect questions like:**
- "Design an AI agent system that handles customer support for Adobe Creative Cloud"
- "Design the orchestration layer for a multi-step AI workflow"
- "Design a RESTful API for a conversational agent platform"
- "Design a data pipeline that ingests user interactions for agent improvement"

#### Reference Architecture: AI Agent Platform

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                               │
│   Web Chat │ In-Product Widget │ Mobile │ API │ Voice            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                     API GATEWAY                                   │
│   Rate Limiting │ Auth │ Request Routing │ Session Management    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  AGENT ORCHESTRATOR                               │
│                                                                   │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │   Router    │  │  Planner     │  │   Execution Engine     │ │
│  │ (intent     │  │ (decompose   │  │   (tool calling,       │ │
│  │  detection) │  │  into steps) │  │    API invocation)     │ │
│  └─────────────┘  └──────────────┘  └────────────────────────┘ │
│                                                                   │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │  Memory     │  │  Guardrails  │  │   Human Handoff        │ │
│  │ (context,   │  │ (safety,     │  │   (escalation,         │ │
│  │  history)   │  │  brand)      │  │    routing)            │ │
│  └─────────────┘  └──────────────┘  └────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                    TOOL/API LAYER                                 │
│  Adobe APIs │ Knowledge Base │ CRM │ Billing │ Product Config   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                    DATA & MODEL LAYER                             │
│  LLM (GPT-4/Claude) │ Embeddings │ Vector DB │ Fine-tuned Models│
└─────────────────────────────────────────────────────────────────┘
```

#### Key Design Decisions to Discuss

| Decision | Options | Trade-offs |
|----------|---------|------------|
| Agent architecture | Single agent vs Multi-agent | Complexity vs specialization |
| LLM selection | GPT-4 vs Claude vs open-source | Cost vs quality vs latency |
| Memory | Short-term (context window) vs Long-term (vector DB) | Relevance vs cost |
| Orchestration | LangChain vs custom vs cloud-native | Flexibility vs maintenance |
| Tool calling | Function calling vs ReAct vs Plan-and-Execute | Reliability vs latency |
| Guardrails | Pre-processing vs post-processing vs both | Safety vs latency |

#### Agent Orchestration Patterns

**Pattern 1: Router Agent (Intent-Based)**
```
User Query → Intent Classifier → Route to Specialist Agent
                                    ├── Billing Agent
                                    ├── Technical Support Agent
                                    ├── Creative Assistant Agent
                                    └── Account Management Agent
```

**Pattern 2: Plan-and-Execute**
```
User Query → Planner (decompose into steps)
          → Step 1: Retrieve user account info
          → Step 2: Check subscription status
          → Step 3: Generate resolution
          → Step 4: Execute action (refund/upgrade/fix)
          → Synthesize response
```

**Pattern 3: ReAct (Reasoning + Acting)**
```
Loop:
  Thought: "User wants to cancel subscription. I need to check their plan first."
  Action: call_api(get_subscription, user_id=123)
  Observation: {plan: "Creative Cloud All Apps", status: "active", renewal: "2024-02-15"}
  Thought: "They have an active plan. I should offer retention before canceling."
  Action: generate_response(retention_offer)
```

---

### 2. RESTful API Design

**JD emphasizes**: RESTful API services (XML/JSON), data pipelines

#### Design a Conversational Agent API

```yaml
# Core Agent API
POST /api/v1/conversations
  Headers: Authorization: Bearer <token>
  Body: { "context": { "product": "photoshop", "page": "export" } }
  Response: { "conversationId": "conv-123", "greeting": "Hi! How can I help?" }

POST /api/v1/conversations/{id}/messages
  Body: { "content": "How do I export as PDF?", "attachments": [] }
  Response: {
    "messageId": "msg-456",
    "content": "Here's how to export...",
    "sources": [{"title": "Export Guide", "url": "..."}],
    "suggestions": ["PDF settings", "Batch export"],
    "metadata": { "model": "gpt-4", "tokens": 450, "latency_ms": 1200 }
  }

POST /api/v1/conversations/{id}/feedback
  Body: { "messageId": "msg-456", "rating": "helpful", "comment": "" }

POST /api/v1/conversations/{id}/escalate
  Body: { "reason": "complex_issue", "summary": "auto-generated" }
  Response: { "ticketId": "T-789", "estimatedWait": "5 min" }

GET /api/v1/agents/{agentId}/metrics
  Response: { "taskCompletion": 0.85, "avgLatency": 1.4, "csat": 4.2 }

# Webhook for async tool results
POST /api/v1/webhooks/tool-complete
  Body: { "executionId": "exec-123", "result": {...}, "status": "success" }
```

#### API Design Best Practices to Discuss

| Principle | Implementation |
|-----------|----------------|
| Versioning | `/api/v1/` prefix, header-based for minor versions |
| Pagination | Cursor-based for conversation history |
| Rate limiting | 429 with `Retry-After` header, per-user quotas |
| Idempotency | `Idempotency-Key` header for POST requests |
| Error format | RFC 7807 Problem Details (`type`, `title`, `status`, `detail`) |
| Streaming | SSE for token streaming, WebSocket for bidirectional |
| HATEOAS | Include `_links` for next actions |

#### Data Pipeline Design

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Agent Events │────►│ Kafka/Kinesis │────►│ Processing   │
│ (interactions│     │ (streaming)  │     │ (Spark/Flink)│
│  feedback,   │     └──────────────┘     └──────┬───────┘
│  tool calls) │                                  │
└──────────────┘                           ┌──────▼───────┐
                                           │ Data Lake    │
                                           │ (S3/ADLS)    │
                                           └──────┬───────┘
                                                  │
                              ┌────────────────────┼────────────────┐
                              │                    │                │
                       ┌──────▼───────┐  ┌────────▼─────┐  ┌──────▼──────┐
                       │ Analytics    │  │ ML Training  │  │ Real-time   │
                       │ (Redshift/BQ)│  │ (fine-tuning)│  │ Dashboard   │
                       └──────────────┘  └──────────────┘  └─────────────┘
```

### 3. System Design Coding

```java
// Circuit breaker for LLM API calls (Java - JD mentions Java)
public class LLMCircuitBreaker {
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private volatile Instant lastFailureTime;
    
    private final int failureThreshold = 5;
    private final Duration resetTimeout = Duration.ofSeconds(30);
    
    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (state.get() == State.OPEN) {
            if (shouldAttemptReset()) {
                state.set(State.HALF_OPEN);
            } else {
                return fallback.get();
            }
        }
        
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            return fallback.get();
        }
    }
    
    private boolean shouldAttemptReset() {
        return Duration.between(lastFailureTime, Instant.now()).compareTo(resetTimeout) > 0;
    }
    
    private void onSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }
    
    private void onFailure() {
        lastFailureTime = Instant.now();
        if (failureCount.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN);
        }
    }
    
    enum State { CLOSED, OPEN, HALF_OPEN }
}
```

```python
# Retry with exponential backoff for LLM API calls (Python - JD mentions Python)
import asyncio
from typing import Optional

class LLMClient:
    def __init__(self, primary_model: str, fallback_model: str):
        self.primary = primary_model
        self.fallback = fallback_model
    
    async def call_with_retry(
        self, prompt: str, max_retries: int = 3
    ) -> dict:
        for attempt in range(max_retries):
            try:
                response = await self._call_llm(self.primary, prompt)
                if self._passes_guardrails(response):
                    return {"response": response, "model": self.primary}
                # Guardrail failed - retry with stricter prompt
                prompt = self._add_safety_prefix(prompt)
            except RateLimitError:
                await asyncio.sleep(2 ** attempt + random.uniform(0, 1))
            except TimeoutError:
                if attempt == max_retries - 1:
                    # Fallback to cheaper/faster model
                    return await self._call_fallback(prompt)
        
        return {"response": None, "escalate": True}
    
    async def _call_fallback(self, prompt: str) -> dict:
        try:
            response = await self._call_llm(self.fallback, prompt)
            return {"response": response, "model": self.fallback, "degraded": True}
        except Exception:
            return {"response": None, "escalate": True}
```

---

### 3. API Design for Agent Systems

**Design a REST/WebSocket API for a conversational agent:**

```yaml
# REST API
POST /api/v1/conversations
  → Create new conversation session

POST /api/v1/conversations/{id}/messages
  Body: { "content": "How do I cancel my subscription?", "attachments": [] }
  Response: { "messageId": "...", "status": "processing" }

GET /api/v1/conversations/{id}/messages
  → Get conversation history

POST /api/v1/conversations/{id}/feedback
  Body: { "messageId": "...", "rating": "helpful", "comment": "..." }

POST /api/v1/conversations/{id}/handoff
  → Escalate to human agent

# WebSocket (real-time streaming)
WS /api/v1/conversations/{id}/stream
  Client → { "type": "message", "content": "..." }
  Server → { "type": "token", "content": "I" }
  Server → { "type": "token", "content": " can" }
  Server → { "type": "token", "content": " help" }
  Server → { "type": "tool_call", "tool": "get_subscription", "status": "executing" }
  Server → { "type": "tool_result", "result": {...} }
  Server → { "type": "complete", "messageId": "..." }
```

---

## Part C: Observability (Critical — JD Emphasizes This)

### JD-Specific Tools You MUST Know

| Tool | Category | What to Demonstrate |
|------|----------|---------------------|
| **New Relic** | APM + Browser RUM | Transaction tracing, custom attributes, error analytics, SLA dashboards |
| **Splunk** | Log aggregation + SIEM | SPL queries, index management, dashboards, alerts, correlation |
| **Nagios/Icinga** | Infrastructure monitoring | Host/service checks, plugins, alerting, escalation policies |
| **RUM Tools** | Frontend performance | Core Web Vitals (LCP, FID, CLS), session replay, user journey tracking |

### Splunk Queries for AI Agent Monitoring

```spl
# Find all failed agent conversations in last hour
index=agents status=error earliest=-1h
| stats count by error_type, agent_name
| sort -count

# Average response latency by model
index=agents event_type=llm_call
| stats avg(latency_ms) as avg_latency, p95(latency_ms) as p95_latency by model
| sort -avg_latency

# Hallucination detection (grounding score < threshold)
index=agents event_type=response grounding_score<0.7
| table _time, conversation_id, user_query, response, grounding_score
| sort -_time

# Cost tracking per conversation
index=agents event_type=llm_call
| eval cost = tokens_used * 0.00003
| stats sum(cost) as total_cost by conversation_id
| where total_cost > 0.50
| sort -total_cost

# User abandonment pattern
index=agents event_type=conversation_end
| eval abandoned = if(resolution_status="abandoned", 1, 0)
| timechart span=1h avg(abandoned) as abandonment_rate
```

### New Relic Custom Instrumentation

```python
import newrelic.agent

@newrelic.agent.function_trace(name='agent_orchestration')
async def handle_message(conversation_id: str, message: str):
    # Add custom attributes for agent-specific tracing
    newrelic.agent.add_custom_attribute('conversation_id', conversation_id)
    newrelic.agent.add_custom_attribute('message_length', len(message))
    
    # Trace intent classification
    with newrelic.agent.FunctionTrace(name='intent_classification'):
        intent = await classify_intent(message)
        newrelic.agent.add_custom_attribute('intent', intent)
    
    # Trace LLM call
    with newrelic.agent.FunctionTrace(name='llm_call'):
        response = await call_llm(message, intent)
        newrelic.agent.add_custom_attribute('model', response.model)
        newrelic.agent.add_custom_attribute('tokens_used', response.tokens)
        newrelic.agent.add_custom_attribute('latency_ms', response.latency_ms)
    
    # Trace guardrail check
    with newrelic.agent.FunctionTrace(name='guardrail_check'):
        safe = await check_guardrails(response.content)
        newrelic.agent.add_custom_attribute('guardrail_passed', safe)
    
    # Record custom metrics
    newrelic.agent.record_custom_metric('Agent/ResponseLatency', response.latency_ms)
    newrelic.agent.record_custom_metric('Agent/TokensUsed', response.tokens)
    
    return response
```

### RUM Implementation for Agent Chat Widget

```javascript
// Real User Monitoring for agent chat interface
class AgentRUM {
  constructor() {
    this.sessionId = crypto.randomUUID();
    this.messageSentAt = null;
  }

  trackMessageSent() {
    this.messageSentAt = performance.now();
  }

  trackTimeToFirstToken(messageId) {
    const ttft = performance.now() - this.messageSentAt;
    // Report to New Relic Browser
    if (window.newrelic) {
      window.newrelic.addPageAction('agent_ttft', {
        ttft_ms: ttft,
        messageId,
        sessionId: this.sessionId
      });
    }
  }

  trackResponseComplete(messageId) {
    const totalTime = performance.now() - this.messageSentAt;
    if (window.newrelic) {
      window.newrelic.addPageAction('agent_response_complete', {
        total_ms: totalTime,
        messageId
      });
    }
  }

  trackUserFrustration(signal) {
    // Detect: rapid re-sends, rage clicks, immediate escalation
    if (window.newrelic) {
      window.newrelic.addPageAction('agent_frustration', {
        signal,
        sessionId: this.sessionId
      });
    }
  }

  // Core Web Vitals for chat widget
  trackWebVitals() {
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (window.newrelic) {
          window.newrelic.addPageAction('web_vital', {
            name: entry.name,
            value: entry.value
          });
        }
      }
    }).observe({ type: 'largest-contentful-paint', buffered: true });
  }
}
```

### Nagios/Icinga Health Checks for Agent Infrastructure

```bash
#!/bin/bash
# Custom Nagios plugin: check_agent_health.sh
# Checks if AI agent service is responding correctly

AGENT_URL="http://localhost:8080/health"
TIMEOUT=5
WARNING_LATENCY=2000  # ms
CRITICAL_LATENCY=5000 # ms

START=$(date +%s%N)
RESPONSE=$(curl -s -o /tmp/agent_health.json -w "%{http_code}" --max-time $TIMEOUT $AGENT_URL)
END=$(date +%s%N)
LATENCY=$(( (END - START) / 1000000 ))

if [ "$RESPONSE" != "200" ]; then
    echo "CRITICAL - Agent health endpoint returned $RESPONSE"
    exit 2
fi

# Check internal health status
STATUS=$(jq -r '.status' /tmp/agent_health.json)
if [ "$STATUS" != "healthy" ]; then
    echo "CRITICAL - Agent reports unhealthy: $(jq -r '.reason' /tmp/agent_health.json)"
    exit 2
fi

if [ $LATENCY -gt $CRITICAL_LATENCY ]; then
    echo "CRITICAL - Agent response time ${LATENCY}ms exceeds ${CRITICAL_LATENCY}ms"
    exit 2
elif [ $LATENCY -gt $WARNING_LATENCY ]; then
    echo "WARNING - Agent response time ${LATENCY}ms exceeds ${WARNING_LATENCY}ms"
    exit 1
fi

echo "OK - Agent healthy, response time ${LATENCY}ms | latency=${LATENCY}ms;${WARNING_LATENCY};${CRITICAL_LATENCY}"
exit 0
```

---

### AI Agent Observability Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY LAYERS                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  METRICS (Prometheus/Datadog)                                │
│  • LLM latency (p50, p95, p99)                             │
│  • Token usage per request                                   │
│  • Tool call success/failure rates                          │
│  • Agent task completion rate                                │
│  • Cost per conversation                                     │
│  • Guardrail trigger rate                                    │
│                                                              │
│  LOGGING (Structured JSON → Loki/CloudWatch)                │
│  • Full conversation traces                                  │
│  • Tool call inputs/outputs                                  │
│  • Guardrail violations                                      │
│  • Error context with prompt/response                        │
│                                                              │
│  TRACING (OpenTelemetry → Jaeger/Tempo)                     │
│  • End-to-end request flow                                   │
│  • LLM call spans (with token counts)                       │
│  • Tool execution spans                                      │
│  • Guardrail check spans                                     │
│                                                              │
│  AI-SPECIFIC MONITORING                                      │
│  • Hallucination detection (factual grounding score)        │
│  • Sentiment drift (user frustration detection)             │
│  • Response quality scoring (automated eval)                │
│  • Conversation abandonment rate                             │
│  • Human escalation triggers                                 │
└─────────────────────────────────────────────────────────────┘
```

### Key Metrics for AI Agents

| Category | Metric | Target | Alert Threshold |
|----------|--------|--------|-----------------|
| Latency | Time to first token | <1s | >3s |
| Latency | Total response time | <5s | >10s |
| Quality | Task completion rate | >85% | <70% |
| Quality | Hallucination rate | <2% | >5% |
| Quality | Guardrail trigger rate | <5% | >15% |
| Cost | Cost per conversation | <$0.10 | >$0.50 |
| Reliability | Agent error rate | <1% | >5% |
| Experience | Human escalation rate | <15% | >30% |
| Experience | User satisfaction (CSAT) | >4.2/5 | <3.5/5 |

### Tracing an Agent Request

```
Trace: conversation-abc-123
│
├── Span: api-gateway (2ms)
│   └── Auth, rate limit check
│
├── Span: intent-classification (150ms)
│   └── LLM call: classify intent
│       ├── tokens_in: 200
│       ├── tokens_out: 15
│       └── result: "subscription_cancellation"
│
├── Span: agent-planning (300ms)
│   └── LLM call: generate plan
│       ├── tokens_in: 800
│       ├── tokens_out: 150
│       └── plan: [get_account, check_subscription, offer_retention]
│
├── Span: tool-execution (500ms)
│   ├── Span: get_account_api (120ms)
│   ├── Span: check_subscription_api (80ms)
│   └── Span: retention_offers_api (200ms)
│
├── Span: response-generation (400ms)
│   └── LLM call: generate response
│       ├── tokens_in: 1200
│       ├── tokens_out: 300
│       └── guardrail_check: PASS
│
└── Span: guardrails (50ms)
    ├── brand_alignment: PASS
    ├── factual_grounding: PASS (score: 0.95)
    └── safety_check: PASS

Total: 1.4s | Cost: $0.03 | Tokens: 2665
```

### Alerting for AI Systems

```yaml
# Prometheus alerting rules for AI agents
groups:
  - name: ai-agent-alerts
    rules:
      - alert: HighHallucinationRate
        expr: |
          rate(agent_hallucination_detected_total[5m]) 
          / rate(agent_responses_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Hallucination rate >5% — agent may be generating incorrect information"
          action: "Check knowledge base freshness, review recent prompt changes"

      - alert: AgentLatencyHigh
        expr: histogram_quantile(0.95, rate(agent_response_duration_seconds_bucket[5m])) > 10
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "P95 agent response time >10s"

      - alert: EscalationRateSpike
        expr: |
          rate(agent_human_escalation_total[15m]) 
          / rate(agent_conversations_total[15m]) > 0.3
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "30%+ conversations escalating to human — agent may be failing"

      - alert: CostPerConversationHigh
        expr: agent_cost_per_conversation_dollars > 0.50
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "Cost per conversation exceeding $0.50 budget"
```

---

## Part D: DevOps & Cloud Operations

### CI/CD for AI Agents

```
┌─────────────────────────────────────────────────────────────┐
│                    AI AGENT CI/CD PIPELINE                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. CODE CHANGE (prompt, tools, orchestration logic)         │
│     │                                                        │
│  2. UNIT TESTS                                               │
│     • Tool function tests                                    │
│     • Guardrail logic tests                                  │
│     • Routing logic tests                                    │
│     │                                                        │
│  3. INTEGRATION TESTS                                        │
│     • LLM mock tests (deterministic)                        │
│     • API integration tests                                  │
│     • End-to-end conversation tests                         │
│     │                                                        │
│  4. EVAL SUITE (AI-specific)                                │
│     • Run against golden dataset (100+ test conversations)  │
│     • Measure: accuracy, hallucination, latency, cost       │
│     • Compare against baseline (regression detection)       │
│     • Gate: must pass >90% of eval cases                    │
│     │                                                        │
│  5. CANARY DEPLOYMENT                                        │
│     • 5% traffic → new agent version                        │
│     • Monitor: CSAT, completion rate, escalation rate        │
│     • Auto-rollback if metrics degrade                      │
│     │                                                        │
│  6. PROGRESSIVE ROLLOUT                                      │
│     • 5% → 25% → 50% → 100%                               │
│     • Each stage: 1-4 hours with metric gates               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Infrastructure for AI Agents

```yaml
# Kubernetes deployment for agent service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-agent-service
spec:
  replicas: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
        - name: agent
          image: adobe/ai-agent:v2.1.0
          resources:
            requests:
              cpu: "1"
              memory: 2Gi
            limits:
              cpu: "2"
              memory: 4Gi
          env:
            - name: LLM_PROVIDER
              value: "azure-openai"
            - name: LLM_MODEL
              value: "gpt-4-turbo"
            - name: VECTOR_DB_URL
              valueFrom:
                secretKeyRef:
                  name: agent-secrets
                  key: vector-db-url
            - name: GUARDRAIL_STRICTNESS
              value: "high"
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 8080
            initialDelaySeconds: 10
          livenessProbe:
            httpGet:
              path: /health/live
              port: 8080
```

### Cloud Architecture (AWS/Azure)

```
┌─────────────────────────────────────────────────────────────┐
│                    PRODUCTION ARCHITECTURE                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  CloudFront/CDN → ALB → EKS (Agent Service)                │
│                              │                               │
│                    ┌─────────┼─────────┐                    │
│                    │         │         │                     │
│              ┌─────▼───┐ ┌──▼────┐ ┌──▼──────────┐        │
│              │ Azure   │ │Redis  │ │ Vector DB   │         │
│              │ OpenAI  │ │(cache,│ │ (Pinecone/  │         │
│              │ (LLM)   │ │session)│ │  Weaviate)  │         │
│              └─────────┘ └───────┘ └─────────────┘         │
│                                                              │
│              ┌─────────┐ ┌───────┐ ┌─────────────┐         │
│              │ S3      │ │Kafka  │ │ DynamoDB    │         │
│              │(prompts,│ │(events│ │(conversation│         │
│              │ docs)   │ │ logs) │ │  history)   │         │
│              └─────────┘ └───────┘ └─────────────┘         │
│                                                              │
│  Observability: Datadog/Prometheus + Grafana + PagerDuty    │
└─────────────────────────────────────────────────────────────┘
```

### Key DevOps Topics to Discuss

1. **Prompt versioning**: How to version-control and deploy prompt changes (treat prompts as code)
2. **A/B testing agents**: Traffic splitting between agent versions
3. **Rollback strategy**: How to quickly revert a bad agent deployment
4. **Cost management**: Token budgets, caching strategies, model selection per query complexity
5. **Scaling**: Auto-scaling based on conversation volume, not just CPU
6. **Disaster recovery**: What happens when LLM provider is down? (fallback to simpler model, cached responses, human handoff)

---

## Practice Questions

### Coding
1. Implement a token-aware conversation truncation algorithm
2. Design a rate limiter that accounts for different LLM model costs
3. Build a retry mechanism with fallback to cheaper models on timeout
4. Implement a simple intent classifier using embeddings + cosine similarity

### System Design
1. "Design Adobe's AI customer support agent system"
2. "Design a system that can answer questions about a user's Creative Cloud projects"
3. "Design the guardrail system that prevents an AI agent from giving harmful advice"
4. "Design a knowledge base ingestion pipeline for agent grounding"

### Observability
1. "How would you detect when an AI agent starts hallucinating in production?"
2. "Design the monitoring dashboard for an AI agent platform"
3. "How would you debug a conversation where the agent gave a wrong answer?"
4. "What SLOs would you define for an AI agent system?"

### DevOps
1. "How would you deploy a prompt change safely to production?"
2. "Design the CI/CD pipeline for an AI agent with automated evaluation"
3. "How would you handle an LLM provider outage?"
4. "How would you manage costs as agent usage scales 10x?"

---

## Next: [Round 2 — Frontend & Experience Engineering →](Round2_Frontend_Experience.md)
