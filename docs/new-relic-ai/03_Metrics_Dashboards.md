# 3. Metrics & Dashboards

## NRQL (New Relic Query Language) for AI Agents

NRQL is SQL-like and queries all New Relic data. Master these queries for AI agent monitoring.

---

## Essential NRQL Queries

### Performance

```sql
-- Average response latency by model (last hour)
SELECT average(ai.response.time) as 'Avg Latency (ms)'
FROM Span WHERE ai.model IS NOT NULL
FACET ai.model SINCE 1 hour ago TIMESERIES

-- P95 latency trend (detect degradation)
SELECT percentile(ai.response.time, 95) as 'P95 Latency'
FROM Span WHERE ai.model IS NOT NULL
SINCE 24 hours ago TIMESERIES 15 minutes

-- Slowest conversations (for debugging)
SELECT max(duration) as 'Duration', latest(conversation_id)
FROM Transaction WHERE name LIKE 'agent/%'
SINCE 1 hour ago FACET conversation_id LIMIT 10

-- Latency breakdown by component
SELECT average(duration) FROM Span
WHERE name IN ('agent/classify_intent', 'agent/retrieve_context', 'agent/generate_response', 'agent/check_safety')
FACET name SINCE 1 hour ago
```

### Token Usage & Cost

```sql
-- Total tokens consumed (last 24h)
SELECT sum(ai.tokens.input) as 'Input Tokens', sum(ai.tokens.output) as 'Output Tokens',
       sum(ai.tokens.input) + sum(ai.tokens.output) as 'Total Tokens'
FROM Span WHERE ai.model IS NOT NULL SINCE 24 hours ago

-- Cost per conversation
SELECT average(cost_usd) as 'Avg Cost', max(cost_usd) as 'Max Cost',
       sum(cost_usd) as 'Total Cost'
FROM AIAgentConversation SINCE 24 hours ago

-- Cost trend by model (detect cost spikes)
SELECT sum(cost_usd) FROM AIAgentConversation
FACET model SINCE 7 days ago TIMESERIES 1 hour

-- Top 10 most expensive conversations
SELECT cost_usd, conversation_id, total_tokens, intent
FROM AIAgentConversation
WHERE cost_usd > 0.10
SINCE 24 hours ago ORDER BY cost_usd DESC LIMIT 10

-- Daily cost projection
SELECT rate(sum(cost_usd), 1 day) as 'Projected Daily Cost'
FROM AIAgentConversation SINCE 1 hour ago
```

### Quality & Safety

```sql
-- Task completion rate
SELECT percentage(count(*), WHERE task_completed = true) as 'Completion Rate'
FROM AIAgentConversation SINCE 24 hours ago TIMESERIES 1 hour

-- Hallucination rate (grounding score < 0.7)
SELECT percentage(count(*), WHERE grounding_score < 0.7) as 'Hallucination Rate'
FROM AIAgentConversation SINCE 24 hours ago TIMESERIES 1 hour

-- Escalation rate
SELECT percentage(count(*), WHERE escalated = true) as 'Escalation Rate'
FROM AIAgentConversation SINCE 24 hours ago TIMESERIES 1 hour

-- Safety violations by type
SELECT count(*) FROM AIAgentConversation
WHERE safety_passed = false
FACET safety_block_reason SINCE 7 days ago

-- Average grounding score trend
SELECT average(grounding_score) as 'Avg Grounding'
FROM AIAgentConversation SINCE 7 days ago TIMESERIES 1 hour

-- Feedback distribution
SELECT count(*) FROM AIAgentFeedback
FACET rating SINCE 7 days ago

-- Negative feedback with context
SELECT conversation_id, intent, model, grounding_score
FROM AIAgentConversation
WHERE conversation_id IN (
  SELECT conversation_id FROM AIAgentFeedback WHERE rating = 'negative' SINCE 24 hours ago
) SINCE 24 hours ago
```

### Tool Usage

```sql
-- Tool call frequency
SELECT count(*) FROM AIAgentToolCall
FACET tool_name SINCE 24 hours ago

-- Tool success rate
SELECT percentage(count(*), WHERE tool_success = true) as 'Success Rate'
FROM AIAgentToolCall FACET tool_name SINCE 24 hours ago

-- Tool latency
SELECT average(tool_latency_ms), percentile(tool_latency_ms, 95)
FROM AIAgentToolCall FACET tool_name SINCE 24 hours ago

-- Failed tool calls (for debugging)
SELECT tool_name, count(*) as 'Failures'
FROM AIAgentToolCall WHERE tool_success = false
FACET tool_name SINCE 24 hours ago
```

### User Experience

```sql
-- Conversations per user (engagement)
SELECT uniqueCount(conversation_id) as 'Conversations'
FROM AIAgentConversation FACET user_id SINCE 7 days ago LIMIT 20

-- Average conversation length (turns)
SELECT average(turn_number) as 'Avg Turns'
FROM AIAgentConversation SINCE 24 hours ago TIMESERIES 1 hour

-- Abandonment rate (conversations with no resolution)
SELECT percentage(count(*), WHERE conversation_status = 'abandoned') as 'Abandonment'
FROM AIAgentConversation SINCE 24 hours ago TIMESERIES 1 hour

-- Time to resolution
SELECT average(resolution_time_seconds) as 'Avg Resolution Time (s)'
FROM AIAgentConversation WHERE task_completed = true
SINCE 24 hours ago TIMESERIES 1 hour
```

---

## Production Dashboard (JSON Export)

```json
{
  "name": "AI Agent Production Dashboard",
  "pages": [
    {
      "name": "Overview",
      "widgets": [
        {
          "title": "Conversations / Minute",
          "nrql": "SELECT rate(count(*), 1 minute) FROM AIAgentConversation SINCE 30 minutes ago TIMESERIES"
        },
        {
          "title": "Task Completion Rate",
          "nrql": "SELECT percentage(count(*), WHERE task_completed = true) FROM AIAgentConversation SINCE 1 hour ago"
        },
        {
          "title": "P95 Latency",
          "nrql": "SELECT percentile(total_latency_ms, 95) FROM AIAgentConversation SINCE 1 hour ago TIMESERIES 5 minutes"
        },
        {
          "title": "Hourly Cost",
          "nrql": "SELECT sum(cost_usd) FROM AIAgentConversation SINCE 24 hours ago TIMESERIES 1 hour"
        },
        {
          "title": "Hallucination Rate",
          "nrql": "SELECT percentage(count(*), WHERE grounding_score < 0.7) FROM AIAgentConversation SINCE 1 hour ago TIMESERIES 5 minutes"
        },
        {
          "title": "Escalation Rate",
          "nrql": "SELECT percentage(count(*), WHERE escalated = true) FROM AIAgentConversation SINCE 1 hour ago TIMESERIES 5 minutes"
        },
        {
          "title": "Model Distribution",
          "nrql": "SELECT count(*) FROM AIAgentConversation FACET model SINCE 24 hours ago"
        },
        {
          "title": "Error Rate",
          "nrql": "SELECT percentage(count(*), WHERE error IS NOT NULL) FROM Transaction WHERE name LIKE 'agent/%' SINCE 1 hour ago TIMESERIES"
        }
      ]
    }
  ]
}
```

---

## SLOs (Service Level Objectives)

```sql
-- Define SLOs in New Relic:

-- SLO 1: Availability (99.9%)
-- "99.9% of agent requests return a response (not error)"
SELECT percentage(count(*), WHERE error IS NULL) as 'Availability'
FROM Transaction WHERE name LIKE 'agent/%' SINCE 30 days ago
-- Target: > 99.9%

-- SLO 2: Latency (95% under 5s)
-- "95% of responses complete within 5 seconds"
SELECT percentage(count(*), WHERE total_latency_ms < 5000) as 'Latency SLO'
FROM AIAgentConversation SINCE 30 days ago
-- Target: > 95%

-- SLO 3: Quality (90% grounded)
-- "90% of responses have grounding score > 0.8"
SELECT percentage(count(*), WHERE grounding_score > 0.8) as 'Quality SLO'
FROM AIAgentConversation SINCE 30 days ago
-- Target: > 90%

-- Error Budget Remaining
SELECT 100 - (percentage(count(*), WHERE error IS NOT NULL)) as 'Error Budget Used (%)'
FROM Transaction WHERE name LIKE 'agent/%' SINCE 30 days ago
-- If > 0.1% errors consumed, budget is being used
```

---

## Comparison Queries (A/B Testing)

```sql
-- Compare agent versions
SELECT average(total_latency_ms) as 'Latency',
       percentage(count(*), WHERE task_completed = true) as 'Completion',
       average(grounding_score) as 'Grounding',
       average(cost_usd) as 'Cost'
FROM AIAgentConversation
FACET agent_version SINCE 7 days ago

-- Compare models
SELECT average(ai.response.time) as 'Latency',
       average(ai.tokens.output) as 'Output Tokens'
FROM Span WHERE ai.model IS NOT NULL
FACET ai.model SINCE 24 hours ago
```

---

## Next: [Alerting & Incident Response →](04_Alerting.md)
