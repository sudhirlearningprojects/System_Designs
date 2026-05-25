# 4. Alerting & Incident Response

## Alert Conditions for AI Agents

### Critical Alerts (P1 — Page immediately)

```sql
-- Agent error rate spike
-- Condition: NRQL alert
SELECT percentage(count(*), WHERE error IS NOT NULL)
FROM Transaction WHERE name LIKE 'agent/%'
-- Threshold: > 5% for 3 minutes
-- Priority: Critical

-- Hallucination rate spike
SELECT percentage(count(*), WHERE grounding_score < 0.5)
FROM AIAgentConversation
-- Threshold: > 5% for 5 minutes
-- Priority: Critical

-- Safety violation spike
SELECT count(*) FROM AIAgentConversation
WHERE safety_passed = false
-- Threshold: > 20 in 5 minutes
-- Priority: Critical

-- LLM provider outage (all requests failing)
SELECT percentage(count(*), WHERE error LIKE '%timeout%' OR error LIKE '%503%')
FROM Span WHERE ai.model IS NOT NULL
-- Threshold: > 50% for 2 minutes
-- Priority: Critical
```

### Warning Alerts (P2 — Investigate within 1 hour)

```sql
-- Latency degradation
SELECT percentile(total_latency_ms, 95)
FROM AIAgentConversation
-- Threshold: > 8000ms for 10 minutes
-- Priority: High

-- Escalation rate increase
SELECT percentage(count(*), WHERE escalated = true)
FROM AIAgentConversation
-- Threshold: > 30% for 15 minutes
-- Priority: High

-- Cost spike
SELECT sum(cost_usd) FROM AIAgentConversation
-- Threshold: > $50 in 1 hour (adjust to your budget)
-- Priority: High

-- Token usage anomaly
SELECT sum(total_tokens) FROM AIAgentConversation
-- Threshold: > 2x normal hourly average
-- Priority: High
```

### Informational Alerts (P3 — Review next business day)

```sql
-- Negative feedback trend
SELECT percentage(count(*), WHERE rating = 'negative')
FROM AIAgentFeedback
-- Threshold: > 20% for 1 hour
-- Priority: Medium

-- Tool failure rate
SELECT percentage(count(*), WHERE tool_success = false)
FROM AIAgentToolCall
-- Threshold: > 10% for 30 minutes
-- Priority: Medium

-- New intent category appearing (potential coverage gap)
SELECT uniqueCount(intent) FROM AIAgentConversation
-- Threshold: > baseline + 3 (new intents detected)
-- Priority: Low
```

---

## Alert Policy Configuration

```python
# Using New Relic API to create alert conditions programmatically
import requests

NR_API_KEY = "NRAK-..."
NR_ACCOUNT_ID = "123456"

def create_nrql_alert(name: str, nrql: str, threshold: float, 
                       duration_minutes: int, priority: str):
    """Create NRQL alert condition via API."""
    payload = {
        "data": {
            "type": "nrql",
            "name": name,
            "enabled": True,
            "nrql": {"query": nrql},
            "signal": {
                "aggregation_window": 60,
                "aggregation_method": "EVENT_FLOW",
            },
            "terms": [{
                "threshold": threshold,
                "threshold_duration": duration_minutes * 60,
                "threshold_occurrences": "ALL",
                "operator": "ABOVE",
                "priority": priority,
            }],
            "expiration": {
                "expiration_duration": 600,
                "open_violation_on_expiration": False,
                "close_violations_on_expiration": True,
            },
        }
    }
    
    response = requests.post(
        f"https://api.newrelic.com/v2/alerts_nrql_conditions.json",
        headers={"Api-Key": NR_API_KEY, "Content-Type": "application/json"},
        json=payload,
    )
    return response.json()

# Create alerts
create_nrql_alert(
    name="AI Agent - High Error Rate",
    nrql="SELECT percentage(count(*), WHERE error IS NOT NULL) FROM Transaction WHERE name LIKE 'agent/%'",
    threshold=5.0,
    duration_minutes=3,
    priority="critical",
)

create_nrql_alert(
    name="AI Agent - Hallucination Spike",
    nrql="SELECT percentage(count(*), WHERE grounding_score < 0.5) FROM AIAgentConversation",
    threshold=5.0,
    duration_minutes=5,
    priority="critical",
)
```

---

## Incident Response Workflow

```
ALERT FIRES: "AI Agent - Hallucination Rate > 5%"
│
├── 1. ACKNOWLEDGE (< 5 min)
│   └── Check: Is it a real issue or false positive?
│       NRQL: SELECT grounding_score, conversation_id FROM AIAgentConversation 
│             WHERE grounding_score < 0.5 SINCE 10 minutes ago LIMIT 10
│
├── 2. ASSESS IMPACT (< 10 min)
│   └── How many users affected?
│       NRQL: SELECT uniqueCount(user_id) FROM AIAgentConversation 
│             WHERE grounding_score < 0.5 SINCE 30 minutes ago
│
├── 3. MITIGATE (< 15 min)
│   ├── Option A: Rollback to previous agent version
│   ├── Option B: Increase guardrail strictness (block low-grounding responses)
│   └── Option C: Route affected intent to human agents
│
├── 4. ROOT CAUSE (< 1 hour)
│   └── Check distributed traces for affected conversations
│       - Was retrieval quality degraded? (stale index?)
│       - Did model behavior change? (provider update?)
│       - Was context truncated? (token limit hit?)
│
└── 5. RESOLVE & PREVENT
    ├── Fix root cause
    ├── Add failing cases to eval suite
    └── Update runbook
```

---

## Next: [RUM for AI Interfaces →](05_RUM.md)
