# 6. Production Patterns

## Cost Tracking

```sql
-- Daily cost by model
SELECT sum(cost_usd) FROM AIAgentConversation
FACET model SINCE 7 days ago TIMESERIES 1 day

-- Cost per conversation trend
SELECT average(cost_usd) FROM AIAgentConversation
SINCE 30 days ago TIMESERIES 1 day

-- Budget burn rate
SELECT rate(sum(cost_usd), 1 day) as 'Daily Burn Rate'
FROM AIAgentConversation SINCE 1 hour ago
```

## Quality Monitoring

```sql
-- Quality score over time (detect drift)
SELECT average(grounding_score) as 'Grounding',
       percentage(count(*), WHERE task_completed = true) as 'Completion'
FROM AIAgentConversation SINCE 7 days ago TIMESERIES 1 hour

-- Compare agent versions
SELECT average(grounding_score), average(cost_usd), average(total_latency_ms)
FROM AIAgentConversation FACET agent_version SINCE 7 days ago
```

## Complete Monitoring Setup

```python
import newrelic.agent

class ProductionAgentMonitor:
    """Wrap your agent with full New Relic monitoring."""
    
    @newrelic.agent.background_task(name='agent/handle')
    def handle(self, query: str, user_id: str) -> str:
        newrelic.agent.add_custom_attribute('user_id', user_id)
        
        try:
            result = self.agent.run(query)
            
            newrelic.agent.record_custom_event('AIAgentConversation', {
                'user_id': user_id,
                'model': result.model,
                'total_tokens': result.tokens,
                'cost_usd': result.cost,
                'total_latency_ms': result.latency_ms,
                'grounding_score': result.grounding,
                'task_completed': result.completed,
                'escalated': result.escalated,
                'agent_version': 'v2.1',
            })
            
            return result.text
            
        except Exception as e:
            newrelic.agent.notice_error(e, attributes={'user_id': user_id})
            raise
```

## Key Takeaways

| What to Monitor | NRQL Source | Alert Threshold |
|----------------|-------------|-----------------|
| Error rate | Transaction errors | >5% for 3 min |
| Latency P95 | AIAgentConversation.total_latency_ms | >8s for 10 min |
| Hallucination | AIAgentConversation.grounding_score < 0.5 | >5% for 5 min |
| Cost/hour | AIAgentConversation.cost_usd | >$50/hour |
| Escalation rate | AIAgentConversation.escalated | >30% for 15 min |
| TTFT (user-facing) | PageAction.ai_first_token.ttft_ms | P95 >5s |
