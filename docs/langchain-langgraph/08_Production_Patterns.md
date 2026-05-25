# 8. Production Patterns

## Theory: What Makes AI Agents Hard in Production?

AI agents in production face unique challenges that traditional software doesn't:

```
┌───────────────────────────────────────────────────────────┐
│  PRODUCTION CHALLENGES FOR AI AGENTS                        │
├───────────────────────────────────────────────────────────┤
│                                                             │
│  1. NON-DETERMINISM                                         │
│     Same input → different output every time                 │
│     → Solution: Temperature=0, caching, eval suites         │
│                                                             │
│  2. LATENCY                                                 │
│     LLM calls take 1-10 seconds (not milliseconds)          │
│     → Solution: Streaming, caching, model tiering           │
│                                                             │
│  3. COST                                                    │
│     Each request costs real money ($0.01-$0.50)             │
│     → Solution: Caching, cheaper models, token optimization │
│                                                             │
│  4. RELIABILITY                                             │
│     LLM APIs have rate limits, timeouts, outages            │
│     → Solution: Fallbacks, retries, circuit breakers        │
│                                                             │
│  5. QUALITY DRIFT                                           │
│     Model updates can silently change behavior               │
│     → Solution: Eval suites, pinned model versions          │
│                                                             │
│  6. SAFETY                                                  │
│     Agents can hallucinate, leak PII, give harmful advice   │
│     → Solution: Guardrails, human-in-the-loop, monitoring   │
└───────────────────────────────────────────────────────────┘
```

### Defense-in-Depth for AI Agents

```
Layer 1: INPUT VALIDATION
  • Reject malformed requests
  • Detect prompt injection attempts
  • Rate limit per user

Layer 2: EXECUTION SAFETY
  • Recursion limits (prevent infinite loops)
  • Timeout per node (prevent hanging)
  • Tool execution sandboxing
  • Human approval for destructive actions

Layer 3: OUTPUT VALIDATION
  • Hallucination detection (grounding check)
  • PII scanning (never leak personal data)
  • Brand safety (appropriate tone/content)
  • Factual verification against sources

Layer 4: OPERATIONAL SAFETY
  • Cost budgets (kill switch if spending too much)
  • Quality monitoring (alert on degradation)
  • Automatic rollback on metric regression
  • Graceful degradation (fallback to simpler responses)
```

### Caching Theory for AI Agents

```
Why cache LLM responses?
  • Cost: Cached response = $0 (vs $0.01-0.10 per API call)
  • Latency: Cache hit = <10ms (vs 1-5 seconds for LLM)
  • Reliability: Cache works even if LLM API is down

Caching strategies:
  1. EXACT MATCH: Same prompt → same response (simple, high hit rate for FAQs)
  2. SEMANTIC CACHE: Similar prompts → same response (uses embeddings, lower threshold)
  3. PROMPT CACHING: Cache the static prefix (system prompt + context) at the API level

What to cache:
  • FAQ-style questions (high repetition)
  • Classification results (deterministic)
  • Retrieval results (same query = same docs)

What NOT to cache:
  • Personalized responses (user-specific context)
  • Time-sensitive answers ("what's my balance?")
  • Creative generation (users expect variety)
```

---

## Error Handling & Fallbacks

```python
from langchain_anthropic import ChatAnthropic
from langchain_openai import ChatOpenAI

# Model fallback chain
primary = ChatAnthropic(model="claude-sonnet-4-20250514", timeout=10)
secondary = ChatAnthropic(model="claude-3-5-haiku-20241022", timeout=10)
tertiary = ChatOpenAI(model="gpt-4o-mini", timeout=10)

model = primary.with_fallbacks([secondary, tertiary])
# If Claude Sonnet fails → try Haiku → try GPT-4o-mini

# Chain-level fallback
main_chain = complex_rag_chain.with_fallbacks([simple_chain])
```

### Retry with Backoff

```python
from langchain_core.runnables import RunnableConfig

chain_with_retry = chain.with_retry(
    stop_after_attempt=3,
    wait_exponential_jitter=True,
    retry_if_exception_type=(TimeoutError, RateLimitError),
)
```

---

## Caching

```python
from langchain_core.globals import set_llm_cache
from langchain_community.cache import RedisCache, SQLiteCache
import redis

# Redis cache (production)
set_llm_cache(RedisCache(redis_=redis.Redis(host="localhost", port=6379)))

# SQLite cache (development)
set_llm_cache(SQLiteCache(database_path=".langchain_cache.db"))

# Now identical prompts return cached results (free + instant)
# First call: hits API (~2s, costs tokens)
result1 = chain.invoke({"query": "What is Docker?"})
# Second call: returns from cache (~1ms, free)
result2 = chain.invoke({"query": "What is Docker?"})
```

### Semantic Cache (Similar Queries)

```python
from langchain_community.cache import RedisSemanticCache
from langchain_openai import OpenAIEmbeddings

set_llm_cache(RedisSemanticCache(
    redis_url="redis://localhost:6379",
    embedding=OpenAIEmbeddings(),
    score_threshold=0.95,  # How similar queries must be to hit cache
))

# "What is Docker?" and "Explain Docker" would hit same cache entry
```

---

## Rate Limiting

```python
from langchain_core.rate_limiters import InMemoryRateLimiter

# Global rate limiter
limiter = InMemoryRateLimiter(
    requests_per_second=10,
    check_every_n_seconds=0.1,
    max_bucket_size=20,  # Burst capacity
)

model = ChatAnthropic(
    model="claude-sonnet-4-20250514",
    rate_limiter=limiter,
)
```

---

## Testing LangGraph Agents

```python
import pytest
from unittest.mock import patch, MagicMock

# Unit test a node
def test_classify_intent():
    state = {"messages": [HumanMessage(content="I want to cancel")], "intent": None}
    result = classify_intent(state)
    assert result["intent"] == "cancellation"

# Integration test the full graph
@pytest.mark.asyncio
async def test_agent_end_to_end():
    app = graph.compile(checkpointer=MemorySaver())
    config = {"configurable": {"thread_id": "test-1"}}
    
    result = await app.ainvoke(
        {"messages": [HumanMessage(content="What's my subscription?")]},
        config=config,
    )
    
    assert len(result["messages"]) > 1
    assert "subscription" in result["messages"][-1].content.lower()
    assert result.get("escalated") is not True

# Test with mocked LLM (deterministic)
@pytest.fixture
def mock_model():
    with patch("langchain_anthropic.ChatAnthropic") as mock:
        mock.return_value.invoke.return_value = AIMessage(content="Mocked response")
        yield mock

def test_agent_with_mock(mock_model):
    result = agent_node({"messages": [HumanMessage(content="test")]})
    assert result["messages"][-1].content == "Mocked response"

# Evaluation test (quality gate)
def test_agent_quality():
    test_cases = load_test_dataset("golden_tests.json")
    results = evaluate_agent(app, test_cases)
    
    assert results["faithfulness"] > 0.85, f"Faithfulness too low: {results['faithfulness']}"
    assert results["relevance"] > 0.80, f"Relevance too low: {results['relevance']}"
    assert results["hallucination_rate"] < 0.05, f"Hallucination rate too high"
```

---

## Observability Integration

```python
import os

# LangSmith (automatic with env vars)
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "ls__..."
os.environ["LANGCHAIN_PROJECT"] = "production-agent"

# All LangChain/LangGraph operations are now traced automatically
# No code changes needed!

# Add custom metadata to traces
from langchain_core.runnables import RunnableConfig

config = RunnableConfig(
    tags=["production", "v2.1"],
    metadata={"user_id": "u-123", "session_id": "s-456"},
    callbacks=[],  # Custom callbacks
)

result = app.invoke({"messages": [HumanMessage(content="Help")]}, config=config)
```

---

## Production Checklist

### Before Launch
- [ ] All tools have error handling (never crash on tool failure)
- [ ] Fallback models configured (primary → secondary → tertiary)
- [ ] Rate limiting enabled (prevent cost explosion)
- [ ] Caching enabled (Redis for production)
- [ ] Recursion limit set (prevent infinite loops)
- [ ] Human-in-the-loop for destructive actions
- [ ] LangSmith tracing enabled
- [ ] Evaluation suite passing (>85% quality)
- [ ] Load tested (target concurrency)

### Monitoring
- [ ] Latency alerts (P95 > threshold)
- [ ] Error rate alerts (>5%)
- [ ] Cost alerts (daily budget)
- [ ] Quality alerts (eval score drops)
- [ ] Escalation rate alerts (>30%)

### Operational
- [ ] Checkpointer on durable storage (Postgres)
- [ ] Graceful shutdown (drain in-flight requests)
- [ ] Health check endpoint
- [ ] Structured logging (JSON)
- [ ] Secrets in environment variables (never in code)
- [ ] Docker image pinned (no `latest` tag)
