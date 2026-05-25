# 8. Production Patterns

## Cost Tracking & Optimization

### Real-Time Cost Dashboard

```python
from dataclasses import dataclass, field
from collections import defaultdict
import time

PRICING = {
    "claude-opus-4-20250514": {"input": 15.0, "output": 75.0},
    "claude-sonnet-4-20250514": {"input": 3.0, "output": 15.0},
    "claude-3-5-haiku-20241022": {"input": 0.80, "output": 4.0},
    "gpt-4-turbo": {"input": 10.0, "output": 30.0},
    "gpt-4o": {"input": 2.5, "output": 10.0},
    "gpt-4o-mini": {"input": 0.15, "output": 0.60},
}

class CostTracker:
    def __init__(self, daily_budget: float = 1000.0):
        self.daily_budget = daily_budget
        self.costs = defaultdict(lambda: {"input_tokens": 0, "output_tokens": 0, "cost": 0.0, "calls": 0})
        self.daily_total = 0.0
        self.alerts_sent = set()
    
    def record(self, model: str, input_tokens: int, output_tokens: int, metadata: dict = None):
        pricing = PRICING.get(model, {"input": 5.0, "output": 15.0})
        cost = (input_tokens * pricing["input"] + output_tokens * pricing["output"]) / 1_000_000
        
        self.costs[model]["input_tokens"] += input_tokens
        self.costs[model]["output_tokens"] += output_tokens
        self.costs[model]["cost"] += cost
        self.costs[model]["calls"] += 1
        self.daily_total += cost
        
        # Budget alerts
        utilization = self.daily_total / self.daily_budget
        if utilization > 0.8 and "80%" not in self.alerts_sent:
            self._alert(f"⚠️ 80% of daily budget consumed: ${self.daily_total:.2f}/${self.daily_budget}")
            self.alerts_sent.add("80%")
        if utilization > 0.95 and "95%" not in self.alerts_sent:
            self._alert(f"🚨 95% of daily budget consumed! Consider throttling.")
            self.alerts_sent.add("95%")
        
        return cost
    
    def get_report(self) -> dict:
        return {
            "daily_total": f"${self.daily_total:.2f}",
            "budget_utilization": f"{self.daily_total / self.daily_budget:.1%}",
            "by_model": {
                model: {
                    "calls": data["calls"],
                    "tokens": data["input_tokens"] + data["output_tokens"],
                    "cost": f"${data['cost']:.2f}",
                    "avg_cost_per_call": f"${data['cost'] / max(data['calls'], 1):.4f}",
                }
                for model, data in self.costs.items()
            }
        }
```

### Cost Optimization Strategies

```python
class CostOptimizer:
    """Automatically optimize costs based on query complexity."""
    
    def __init__(self):
        self.model_tiers = {
            "simple": "claude-3-5-haiku-20241022",    # $0.80/$4
            "standard": "claude-sonnet-4-20250514",    # $3/$15
            "complex": "claude-opus-4-20250514",       # $15/$75
        }
        self.cache = SemanticCache(ttl=3600)
    
    async def route_and_execute(self, query: str, context: dict) -> dict:
        # 1. Check cache first (free!)
        cached = self.cache.get(query)
        if cached:
            return {"response": cached, "cost": 0.0, "source": "cache"}
        
        # 2. Classify complexity
        complexity = await self._classify_complexity(query)
        model = self.model_tiers[complexity]
        
        # 3. Execute with selected model
        response = await self._call_llm(model, query, context)
        
        # 4. Cache if appropriate
        if complexity in ["simple", "standard"]:
            self.cache.set(query, response["content"])
        
        return response
    
    async def _classify_complexity(self, query: str) -> str:
        """Use cheapest model to classify query complexity."""
        result = await client.messages.create(
            model="claude-3-5-haiku-20241022",
            max_tokens=10,
            messages=[{"role": "user", "content": f"Classify complexity (simple/standard/complex): {query}"}]
        )
        return result.content[0].text.strip().lower()
```

---

## Guardrail Monitoring

```python
class GuardrailMonitor:
    """Track and alert on guardrail triggers."""
    
    def __init__(self):
        self.triggers = defaultdict(int)
        self.total_requests = 0
        self.window_start = time.time()
        self.window_size = 3600  # 1 hour
    
    def record_check(self, guardrail_name: str, triggered: bool, details: dict = None):
        self.total_requests += 1
        if triggered:
            self.triggers[guardrail_name] += 1
            self._check_alert_threshold(guardrail_name)
            self._log_trigger(guardrail_name, details)
    
    def _check_alert_threshold(self, guardrail_name: str):
        rate = self.triggers[guardrail_name] / max(self.total_requests, 1)
        
        thresholds = {
            "hallucination": 0.05,      # Alert if >5% hallucination
            "toxicity": 0.01,           # Alert if >1% toxic
            "pii_leak": 0.001,          # Alert if >0.1% PII leaks
            "prompt_injection": 0.02,   # Alert if >2% injection attempts
            "off_topic": 0.10,          # Alert if >10% off-topic
            "brand_violation": 0.03,    # Alert if >3% brand issues
        }
        
        threshold = thresholds.get(guardrail_name, 0.05)
        if rate > threshold:
            self._alert(f"🚨 Guardrail '{guardrail_name}' trigger rate: {rate:.1%} (threshold: {threshold:.1%})")
    
    def get_dashboard(self) -> dict:
        return {
            "total_requests": self.total_requests,
            "guardrail_triggers": dict(self.triggers),
            "trigger_rates": {
                name: count / max(self.total_requests, 1)
                for name, count in self.triggers.items()
            },
            "window_hours": (time.time() - self.window_start) / 3600,
        }
```

---

## A/B Testing AI Agents

```python
import hashlib
import random

class AgentABTest:
    """A/B test different agent configurations."""
    
    def __init__(self, experiment_name: str, variants: dict, traffic_split: dict):
        """
        variants: {"control": AgentConfig(...), "treatment": AgentConfig(...)}
        traffic_split: {"control": 0.9, "treatment": 0.1}
        """
        self.experiment_name = experiment_name
        self.variants = variants
        self.traffic_split = traffic_split
        self.metrics = defaultdict(lambda: defaultdict(list))
    
    def assign_variant(self, user_id: str) -> str:
        """Deterministic assignment based on user_id (consistent experience)."""
        hash_val = int(hashlib.md5(f"{self.experiment_name}:{user_id}".encode()).hexdigest(), 16)
        normalized = (hash_val % 10000) / 10000.0
        
        cumulative = 0.0
        for variant, weight in self.traffic_split.items():
            cumulative += weight
            if normalized < cumulative:
                return variant
        return list(self.variants.keys())[0]
    
    def record_outcome(self, variant: str, metrics: dict):
        """Record metrics for a completed interaction."""
        for key, value in metrics.items():
            self.metrics[variant][key].append(value)
    
    def get_results(self) -> dict:
        """Statistical comparison of variants."""
        results = {}
        for variant, data in self.metrics.items():
            results[variant] = {
                metric: {
                    "mean": np.mean(values),
                    "std": np.std(values),
                    "n": len(values),
                    "ci_95": (np.mean(values) - 1.96 * np.std(values) / np.sqrt(len(values)),
                              np.mean(values) + 1.96 * np.std(values) / np.sqrt(len(values))),
                }
                for metric, values in data.items()
            }
        
        # Statistical significance test
        if len(self.metrics) == 2:
            variants = list(self.metrics.keys())
            for metric in self.metrics[variants[0]]:
                from scipy import stats
                t_stat, p_value = stats.ttest_ind(
                    self.metrics[variants[0]][metric],
                    self.metrics[variants[1]][metric]
                )
                results["significance"] = results.get("significance", {})
                results["significance"][metric] = {
                    "t_statistic": t_stat,
                    "p_value": p_value,
                    "significant": p_value < 0.05,
                }
        
        return results

# Usage
experiment = AgentABTest(
    experiment_name="prompt-v2-test",
    variants={
        "control": {"system_prompt": "You are a helpful assistant.", "model": "claude-sonnet-4"},
        "treatment": {"system_prompt": "You are an expert support agent. Be concise.", "model": "claude-sonnet-4"},
    },
    traffic_split={"control": 0.5, "treatment": 0.5}
)

# In request handler
variant = experiment.assign_variant(user_id)
config = experiment.variants[variant]
response = await run_agent(query, config)

# After interaction
experiment.record_outcome(variant, {
    "task_completion": 1.0 if resolved else 0.0,
    "latency_ms": latency,
    "csat": user_rating,
    "cost": cost,
})
```

---

## Incident Response Playbook

### Severity Levels

| Level | Criteria | Response Time | Example |
|-------|----------|---------------|---------|
| **P1** | Agent causing harm or data loss | <15 min | Leaking PII, wrong financial info |
| **P2** | Agent completely failing (>50% error) | <1 hour | Model API down, all responses failing |
| **P3** | Quality degradation (metrics below SLO) | <4 hours | Hallucination rate spike, CSAT drop |
| **P4** | Minor issue, no user impact | <24 hours | Increased latency, cost spike |

### Incident Response Steps

```
P1 INCIDENT: Agent leaking customer PII
─────────────────────────────────────────
1. DETECT (automated alert or user report)
   └── Alert: "PII detected in agent response" from guardrail monitor

2. MITIGATE (immediate, <15 min)
   └── Disable the affected agent capability
   └── Route all traffic to human agents
   └── Notify security team

3. INVESTIGATE (root cause)
   └── Pull traces from LangSmith/Phoenix for affected conversations
   └── Identify: Was PII in retrieved context? In training data? In tool output?
   └── Scope: How many users affected? What data was exposed?

4. FIX
   └── Add PII scrubbing to retrieval pipeline
   └── Add output guardrail for PII detection
   └── Update system prompt: "Never include personal information in responses"

5. VERIFY
   └── Run eval suite with PII test cases
   └── Canary deploy with 1% traffic
   └── Monitor guardrail metrics for 24h

6. POST-MORTEM
   └── Document timeline, root cause, fix
   └── Add test cases to prevent regression
   └── Update runbook
```

---

## Interview-Ready Summary

### "Design the observability system for an AI agent platform"

```
1. TRACING (LangSmith or Phoenix)
   - Every LLM call, tool call, retrieval as spans
   - Full prompt/response logging (with PII redaction)
   - Latency breakdown per component

2. EVALUATION (RAGAS + Custom)
   - Automated quality scoring on every response
   - Faithfulness, relevance, helpfulness metrics
   - LLM-as-judge for nuanced quality assessment

3. METRICS (Prometheus/Datadog)
   - Latency (TTFT, total), error rate, throughput
   - Token usage, cost per conversation
   - Task completion, escalation rate, CSAT

4. ALERTING
   - Hallucination rate > 5% → P2
   - Error rate > 5% → P2
   - Latency p95 > 10s → P3
   - Cost per conversation > $0.50 → P4
   - Guardrail trigger rate spike → P3

5. DEBUGGING
   - Search traces by user_id, conversation_id, error type
   - See exact prompts and responses
   - Identify: retrieval failure vs prompt issue vs model error

6. CONTINUOUS IMPROVEMENT
   - A/B test prompt changes
   - Track quality over time (regression detection)
   - Human review queue for low-confidence responses
   - Feedback loop: user ratings → eval dataset → improve
```

### Key Numbers to Know

| Metric | Good | Acceptable | Bad |
|--------|------|-----------|-----|
| Task completion | >85% | 70-85% | <70% |
| Hallucination rate | <2% | 2-5% | >5% |
| P95 latency | <3s | 3-8s | >8s |
| Cost/conversation | <$0.10 | $0.10-0.50 | >$0.50 |
| CSAT | >4.2/5 | 3.5-4.2 | <3.5 |
| Escalation rate | <15% | 15-30% | >30% |
| Cache hit rate | >30% | 10-30% | <10% |
| Guardrail trigger | <5% | 5-15% | >15% |
