# 7. Production & Safety

## Production Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRODUCTION DEPLOYMENT                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐     ┌──────────────┐     ┌─────────────────┐  │
│  │ Load        │────►│ Input        │────►│ Claude API      │  │
│  │ Balancer    │     │ Guardrails   │     │ (with retry)    │  │
│  └─────────────┘     └──────────────┘     └────────┬────────┘  │
│                                                      │           │
│  ┌─────────────┐     ┌──────────────┐     ┌────────▼────────┐  │
│  │ Response    │◄────│ Output       │◄────│ Tool Execution  │  │
│  │ to Client   │     │ Guardrails   │     │ Layer           │  │
│  └─────────────┘     └──────────────┘     └─────────────────┘  │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ SUPPORTING INFRASTRUCTURE                                │    │
│  │ Redis (cache) │ Vector DB │ Metrics │ Logging │ Alerts  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Cost Optimization

### Strategy Matrix

| Technique | Savings | Implementation |
|-----------|---------|----------------|
| **Prompt caching** | 90% on cached tokens | Add `cache_control` to static content |
| **Model tiering** | 60-80% | Use Haiku for classification, Sonnet for generation |
| **Response caching** | 95%+ for repeated queries | Redis cache with semantic similarity |
| **Batch API** | 50% | Use for non-real-time processing |
| **Token optimization** | 20-40% | Shorter prompts, structured output |
| **Early stopping** | Variable | Stop generation when answer is complete |

### Model Tiering

```python
class ModelRouter:
    """Route requests to cheapest model that can handle them."""
    
    MODELS = {
        "simple": "claude-3-5-haiku-20241022",     # $0.80/$4 per 1M
        "standard": "claude-sonnet-4-20250514",     # $3/$15 per 1M
        "complex": "claude-opus-4-20250514",        # $15/$75 per 1M
    }
    
    def select_model(self, query: str, context: dict) -> str:
        # Simple: classification, extraction, yes/no questions
        if context.get("task_type") in ["classify", "extract", "validate"]:
            return self.MODELS["simple"]
        
        # Complex: multi-step reasoning, code generation, analysis
        if context.get("complexity") == "high" or len(query) > 2000:
            return self.MODELS["complex"]
        
        # Standard: everything else
        return self.MODELS["standard"]
```

### Response Caching

```python
import hashlib
import redis
import json

class SemanticCache:
    def __init__(self):
        self.redis = redis.Redis()
        self.ttl = 3600  # 1 hour cache
    
    def get(self, query: str, system: str) -> str | None:
        cache_key = self._make_key(query, system)
        cached = self.redis.get(cache_key)
        return cached.decode() if cached else None
    
    def set(self, query: str, system: str, response: str):
        cache_key = self._make_key(query, system)
        self.redis.setex(cache_key, self.ttl, response)
    
    def _make_key(self, query: str, system: str) -> str:
        content = f"{system}:{query}"
        return f"claude_cache:{hashlib.sha256(content.encode()).hexdigest()}"

# Usage
cache = SemanticCache()

def call_claude(query: str, system: str) -> str:
    # Check cache first
    cached = cache.get(query, system)
    if cached:
        return cached  # Free!
    
    # Call API
    response = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=2048,
        system=system,
        messages=[{"role": "user", "content": query}]
    )
    
    result = response.content[0].text
    cache.set(query, system, result)
    return result
```

### Cost Tracking

```python
class CostTracker:
    PRICING = {
        "claude-opus-4-20250514": {"input": 15.0, "output": 75.0},
        "claude-sonnet-4-20250514": {"input": 3.0, "output": 15.0},
        "claude-3-5-haiku-20241022": {"input": 0.80, "output": 4.0},
    }
    
    def __init__(self):
        self.total_cost = 0.0
        self.requests = 0
    
    def record(self, model: str, input_tokens: int, output_tokens: int):
        pricing = self.PRICING[model]
        cost = (input_tokens * pricing["input"] + output_tokens * pricing["output"]) / 1_000_000
        self.total_cost += cost
        self.requests += 1
        return cost
    
    def get_stats(self) -> dict:
        return {
            "total_cost": f"${self.total_cost:.4f}",
            "total_requests": self.requests,
            "avg_cost_per_request": f"${self.total_cost / max(self.requests, 1):.4f}",
        }
```

---

## Safety & Guardrails

### Multi-Layer Safety

```python
class SafetyPipeline:
    """Multi-layer safety checks for production AI agents."""
    
    def __init__(self):
        self.client = anthropic.Anthropic()
    
    async def check_input(self, user_input: str) -> tuple[bool, str]:
        """Pre-processing safety checks."""
        
        # Layer 1: Pattern matching (fast, no API call)
        if self._contains_injection_patterns(user_input):
            return False, "blocked_injection"
        
        # Layer 2: PII detection
        if self._contains_pii(user_input):
            return False, "contains_pii"
        
        # Layer 3: Content classification (API call)
        classification = await self._classify_content(user_input)
        if classification["harmful"]:
            return False, f"harmful_content: {classification['category']}"
        
        return True, "safe"
    
    async def check_output(self, response: str, context: dict) -> tuple[bool, str]:
        """Post-processing safety checks."""
        
        # Layer 1: Factual grounding check
        if context.get("sources"):
            grounding_score = await self._check_grounding(response, context["sources"])
            if grounding_score < 0.7:
                return False, f"low_grounding: {grounding_score}"
        
        # Layer 2: Brand safety
        if not self._brand_safe(response):
            return False, "brand_violation"
        
        # Layer 3: No unauthorized commitments
        if self._contains_commitments(response):
            return False, "unauthorized_commitment"
        
        return True, "safe"
    
    def _contains_injection_patterns(self, text: str) -> bool:
        patterns = [
            "ignore previous instructions",
            "ignore all instructions",
            "you are now",
            "new system prompt",
            "disregard",
            "override",
            "jailbreak",
        ]
        text_lower = text.lower()
        return any(p in text_lower for p in patterns)
    
    def _contains_pii(self, text: str) -> bool:
        import re
        patterns = [
            r'\b\d{3}-\d{2}-\d{4}\b',  # SSN
            r'\b\d{16}\b',              # Credit card
            r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b',  # Email
        ]
        return any(re.search(p, text) for p in patterns)
    
    async def _classify_content(self, text: str) -> dict:
        response = self.client.messages.create(
            model="claude-3-5-haiku-20241022",
            max_tokens=100,
            messages=[{
                "role": "user",
                "content": f"""Classify if this text contains harmful content.
Categories: violence, hate_speech, self_harm, illegal_activity, none

Text: "{text[:500]}"

JSON: {{"harmful": true/false, "category": "category_name", "confidence": 0.0-1.0}}"""
            }]
        )
        return json.loads(response.content[0].text)
```

---

## Monitoring & Observability

### Key Metrics Dashboard

```python
from dataclasses import dataclass, field
from collections import defaultdict
import time

@dataclass
class AgentMetrics:
    # Latency
    response_times: list = field(default_factory=list)
    time_to_first_token: list = field(default_factory=list)
    
    # Quality
    task_completions: int = 0
    task_failures: int = 0
    escalations: int = 0
    guardrail_triggers: int = 0
    
    # Cost
    total_tokens_in: int = 0
    total_tokens_out: int = 0
    total_cost_usd: float = 0.0
    
    # Safety
    input_blocks: int = 0
    output_blocks: int = 0
    hallucination_detections: int = 0
    
    def record_request(self, latency_ms: float, tokens_in: int, tokens_out: int, 
                       cost: float, success: bool, escalated: bool = False):
        self.response_times.append(latency_ms)
        self.total_tokens_in += tokens_in
        self.total_tokens_out += tokens_out
        self.total_cost_usd += cost
        if success:
            self.task_completions += 1
        else:
            self.task_failures += 1
        if escalated:
            self.escalations += 1
    
    def get_summary(self) -> dict:
        n = len(self.response_times)
        sorted_times = sorted(self.response_times)
        return {
            "total_requests": n,
            "p50_latency_ms": sorted_times[n // 2] if n else 0,
            "p95_latency_ms": sorted_times[int(n * 0.95)] if n else 0,
            "p99_latency_ms": sorted_times[int(n * 0.99)] if n else 0,
            "success_rate": self.task_completions / max(n, 1),
            "escalation_rate": self.escalations / max(n, 1),
            "guardrail_trigger_rate": self.guardrail_triggers / max(n, 1),
            "total_cost": f"${self.total_cost_usd:.2f}",
            "avg_cost_per_request": f"${self.total_cost_usd / max(n, 1):.4f}",
            "total_tokens": self.total_tokens_in + self.total_tokens_out,
        }
```

### Logging Best Practices

```python
import structlog

logger = structlog.get_logger()

async def handle_agent_request(request):
    request_id = str(uuid.uuid4())
    
    logger.info("agent_request_started",
        request_id=request_id,
        user_id=request.user_id,
        query_length=len(request.query),
        product_context=request.product
    )
    
    try:
        response = await agent.run(request.query)
        
        logger.info("agent_request_completed",
            request_id=request_id,
            latency_ms=response.latency_ms,
            model=response.model_used,
            tokens_in=response.tokens_in,
            tokens_out=response.tokens_out,
            tool_calls=response.tool_calls_count,
            cost_usd=response.cost,
            success=True
        )
        return response
    
    except Exception as e:
        logger.error("agent_request_failed",
            request_id=request_id,
            error=str(e),
            error_type=type(e).__name__
        )
        raise
```

---

## Rate Limit Handling

```python
import asyncio
from asyncio import Semaphore

class RateLimitedClient:
    """Client with built-in rate limiting and retry logic."""
    
    def __init__(self, max_concurrent: int = 10, requests_per_minute: int = 1000):
        self.client = anthropic.AsyncAnthropic()
        self.semaphore = Semaphore(max_concurrent)
        self.rpm_limit = requests_per_minute
        self.request_times = []
    
    async def create_message(self, **kwargs) -> anthropic.types.Message:
        async with self.semaphore:
            await self._wait_for_rate_limit()
            
            for attempt in range(5):
                try:
                    response = await self.client.messages.create(**kwargs)
                    self.request_times.append(time.time())
                    return response
                except anthropic.RateLimitError:
                    wait = min(2 ** attempt * 5, 60)
                    logger.warning(f"Rate limited. Waiting {wait}s (attempt {attempt + 1})")
                    await asyncio.sleep(wait)
                except anthropic.APIError as e:
                    if e.status_code >= 500:
                        await asyncio.sleep(2 ** attempt)
                    else:
                        raise
            
            raise Exception("Max retries exceeded")
    
    async def _wait_for_rate_limit(self):
        """Ensure we don't exceed RPM limit."""
        now = time.time()
        self.request_times = [t for t in self.request_times if now - t < 60]
        if len(self.request_times) >= self.rpm_limit:
            sleep_time = 60 - (now - self.request_times[0])
            await asyncio.sleep(max(0, sleep_time))
```

---

## Deployment Checklist

### Pre-Launch
- [ ] Load testing: Verify system handles expected traffic
- [ ] Guardrails tested: Adversarial testing with red-team prompts
- [ ] Fallback paths: What happens when Claude API is down?
- [ ] Cost projections: Estimated monthly cost at expected volume
- [ ] Rate limits: Confirmed tier supports your traffic
- [ ] Prompt caching: Enabled for static content
- [ ] Monitoring: Dashboards and alerts configured
- [ ] Logging: Structured logs with request IDs for debugging

### Post-Launch
- [ ] Monitor error rates (target: <1%)
- [ ] Monitor latency (target: p95 < 5s)
- [ ] Monitor cost per request (set budget alerts)
- [ ] Monitor guardrail trigger rate (investigate spikes)
- [ ] Review conversation samples weekly (quality audit)
- [ ] Track user satisfaction (CSAT, thumbs up/down)
- [ ] A/B test prompt improvements continuously
- [ ] Update knowledge base as products change

---

## Summary

| Topic | Key Takeaway |
|-------|-------------|
| **API** | Use streaming for UX, batches for bulk, caching for cost |
| **Prompts** | XML tags, system prompts for rules, few-shot for format |
| **Tools** | Define clearly, handle errors, confirm destructive actions |
| **RAG** | Contextual chunking + hybrid search + reranking |
| **Agents** | ReAct for simple, Plan-Execute for complex, Router for multi-domain |
| **Fine-tuning** | Last resort after prompts/RAG; need 500+ quality examples |
| **Production** | Model tiering, caching, guardrails, monitoring, cost tracking |
