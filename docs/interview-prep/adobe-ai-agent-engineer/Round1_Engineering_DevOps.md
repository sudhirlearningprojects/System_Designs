# Round 1 — Software Engineering, Coding, Observability, DevOps & Cloud

## What They're Evaluating

- Can you design and build production-grade AI agent systems?
- Do you understand distributed systems, APIs, and cloud infrastructure?
- Can you instrument, monitor, and debug complex AI pipelines?
- Do you write clean, testable, performant code?

---

## Part A: Software Engineering & Coding

### 1. System Design for AI Agents

**Expect questions like:**
- "Design an AI agent system that handles customer support for Adobe Creative Cloud"
- "Design the orchestration layer for a multi-step AI workflow"
- "How would you build a system that routes customer queries to the right agent?"

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

### 2. Coding Topics to Prepare

#### Data Structures & Algorithms (Medium-Hard)

Focus areas for AI agent systems:
- **Graph traversal**: Agent workflow DAGs, dependency resolution
- **Queue/Priority Queue**: Task scheduling, message processing
- **Trie/String matching**: Intent classification, entity extraction
- **Sliding window**: Rate limiting, token counting
- **Dynamic programming**: Optimal tool selection, cost minimization

#### Coding Patterns Likely Asked

```python
# 1. Implement a conversation memory with sliding window + summarization
class ConversationMemory:
    def __init__(self, max_tokens=4000, summary_threshold=3000):
        self.messages = []
        self.max_tokens = max_tokens
        self.summary_threshold = summary_threshold
    
    def add(self, role, content):
        self.messages.append({"role": role, "content": content})
        if self.token_count() > self.summary_threshold:
            self._summarize_old_messages()
    
    def get_context(self):
        """Return messages fitting within token budget"""
        # Keep system prompt + recent messages + summary of old
        pass

# 2. Implement retry with exponential backoff for LLM API calls
class LLMClient:
    async def call_with_retry(self, prompt, max_retries=3):
        for attempt in range(max_retries):
            try:
                response = await self.llm.generate(prompt)
                if self._passes_guardrails(response):
                    return response
                # Retry with modified prompt if guardrails fail
                prompt = self._add_guardrail_reminder(prompt)
            except RateLimitError:
                await asyncio.sleep(2 ** attempt)
            except TimeoutError:
                if attempt == max_retries - 1:
                    return self._fallback_response()
        return self._escalate_to_human()

# 3. Implement a tool router that selects optimal tool based on query
class ToolRouter:
    def __init__(self, tools: List[Tool]):
        self.tools = tools
        self.embeddings = self._embed_tool_descriptions()
    
    def route(self, query: str, context: dict) -> Tool:
        """Select best tool using semantic similarity + rule-based filters"""
        query_embedding = embed(query)
        scores = cosine_similarity(query_embedding, self.embeddings)
        
        # Filter by preconditions
        eligible = [t for t, s in zip(self.tools, scores) 
                   if t.precondition_met(context)]
        
        # Return highest scoring eligible tool
        return max(eligible, key=lambda t: scores[self.tools.index(t)])
```

#### System Design Coding

```java
// Implement a circuit breaker for LLM API calls
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
}
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

## Part B: Observability

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

## Part C: DevOps & Cloud Operations

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
