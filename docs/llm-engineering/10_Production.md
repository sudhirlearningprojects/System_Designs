# 10. Production & MLOps for LLMs

## Theory: LLM Production is Different

```
Traditional ML Production:
  Train model → Deploy → Monitor accuracy → Retrain quarterly

LLM Production:
  Select model → Prompt engineer → Deploy → Monitor QUALITY (not just accuracy)
  → Monitor COST → Monitor SAFETY → Iterate DAILY → A/B test continuously

Key differences:
  1. No "training" in the traditional sense (use API or fine-tune)
  2. Quality is subjective and multi-dimensional
  3. Cost scales with usage (pay per token)
  4. Safety is a continuous concern (new attacks emerge)
  5. Prompts are code (need versioning, testing, review)
  6. Models update under you (provider updates can change behavior)
```

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    LLM PRODUCTION ARCHITECTURE                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────────┐   │
│  │ Client   │───►│ API Gateway  │───►│ LLM Orchestrator    │   │
│  │          │    │ (rate limit, │    │ (routing, fallback, │   │
│  │          │    │  auth, cache)│    │  retry, streaming)  │   │
│  └──────────┘    └──────────────┘    └──────────┬──────────┘   │
│                                                   │              │
│                                    ┌──────────────┼──────────┐  │
│                                    │              │          │  │
│                              ┌─────▼────┐  ┌─────▼────┐  ┌──▼──┐│
│                              │ Primary  │  │ Fallback │  │Cache││
│                              │ (Claude) │  │ (GPT-4o) │  │(Redis)│
│                              └──────────┘  └──────────┘  └─────┘│
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ OBSERVABILITY                                            │    │
│  │ Traces │ Metrics │ Evals │ Cost │ Safety │ Alerts       │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Cost Optimization

### Token Economics

```
Cost = (Input Tokens × Input Price) + (Output Tokens × Output Price)

Example (Claude Sonnet, 1000 conversations/day):
  Avg input: 2000 tokens × $3/1M = $0.006 per request
  Avg output: 500 tokens × $15/1M = $0.0075 per request
  Per conversation: $0.0135
  Daily: 1000 × $0.0135 = $13.50
  Monthly: $405

At 100K conversations/day: $40,500/month ← needs optimization!
```

### Cost Reduction Strategies

| Strategy | Savings | Implementation |
|----------|---------|----------------|
| Prompt caching | 90% on cached prefix | Cache system prompt + static context |
| Response caching | 95%+ for repeated queries | Redis with semantic similarity |
| Model tiering | 60-80% | Haiku for classification, Sonnet for generation |
| Shorter prompts | 20-40% | Remove redundant instructions, compress context |
| Batch API | 50% | Non-real-time processing |
| Token limits | Variable | Set max_tokens appropriately per task |
| Early stopping | Variable | Stop generation when answer is complete |

---

## CI/CD for LLM Applications

```
┌─────────────────────────────────────────────────────────────┐
│                    LLM CI/CD PIPELINE                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. CODE CHANGE (prompt, tools, config)                     │
│     │                                                        │
│  2. UNIT TESTS (fast, deterministic)                        │
│     • Tool function tests                                    │
│     • Input validation tests                                 │
│     • Output parsing tests                                   │
│     │                                                        │
│  3. INTEGRATION TESTS (with mocked LLM)                     │
│     • End-to-end flow with deterministic responses          │
│     • Error handling paths                                   │
│     │                                                        │
│  4. EVAL SUITE (with real LLM, expensive)                   │
│     • Run against golden dataset (100+ test cases)          │
│     • Quality gate: must pass >90% of cases                 │
│     • Safety gate: must pass >99% of safety tests           │
│     • Regression gate: must not drop >2% from baseline      │
│     │                                                        │
│  5. CANARY DEPLOYMENT (5% traffic)                          │
│     • Monitor: quality, latency, cost, safety               │
│     • Auto-rollback if metrics degrade                      │
│     │                                                        │
│  6. PROGRESSIVE ROLLOUT (5% → 25% → 50% → 100%)           │
│     • Each stage: 2-4 hours with metric gates               │
│     │                                                        │
│  7. PRODUCTION MONITORING (continuous)                       │
│     • Quality sampling (eval 5% of production traffic)      │
│     • Cost tracking (daily budget alerts)                   │
│     • Safety monitoring (guardrail trigger rates)           │
└─────────────────────────────────────────────────────────────┘
```

---

## Prompt Versioning

```
Treat prompts as CODE:
  - Version controlled (Git)
  - Reviewed (PR process)
  - Tested (eval suite)
  - Deployed (CI/CD)
  - Monitored (A/B tested)

Directory structure:
prompts/
├── system/
│   ├── support_agent_v2.1.txt
│   ├── support_agent_v2.0.txt  (previous)
│   └── support_agent_v1.9.txt  (rollback target)
├── templates/
│   ├── classification.txt
│   ├── summarization.txt
│   └── rag_synthesis.txt
├── tests/
│   ├── test_support_agent.py
│   └── golden_dataset.jsonl
└── config/
    ├── production.yaml
    └── staging.yaml
```

---

## Monitoring Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  LLM PRODUCTION DASHBOARD                                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  HEALTH                          QUALITY                     │
│  ├── Requests/min: 847          ├── Task completion: 84.2%  │
│  ├── Error rate: 0.3%           ├── CSAT: 4.3/5            │
│  ├── P95 latency: 3.2s         ├── Hallucination: 1.8%    │
│  └── Availability: 99.97%      └── Escalation: 12.1%      │
│                                                              │
│  COST                            SAFETY                      │
│  ├── Today: $142.30             ├── Guardrail triggers: 3.2%│
│  ├── Budget used: 67%           ├── Jailbreak attempts: 12  │
│  ├── Avg cost/conv: $0.08      ├── PII detections: 0       │
│  └── Token efficiency: 82%     └── Safety score: 99.8%     │
│                                                              │
│  ⚠️ ALERTS                                                  │
│  • [WARN] Latency P95 trending up (3.2s → 3.8s)           │
│  • [INFO] New prompt version v2.2 deployed to 25% traffic  │
└─────────────────────────────────────────────────────────────┘
```

---

## Incident Response for LLMs

```
SEVERITY LEVELS:

P1 (Critical, <15 min response):
  • Model generating harmful content
  • PII leak in responses
  • Complete service outage

P2 (High, <1 hour response):
  • Quality drop >20%
  • Cost spike >3x normal
  • Provider API outage (no fallback)

P3 (Medium, <4 hours):
  • Quality drop 5-20%
  • Latency spike (P95 > 10s)
  • Elevated guardrail triggers

P4 (Low, <24 hours):
  • Minor quality regression
  • Cost slightly above budget
  • Non-critical feature degradation
```

---

## Production Checklist

### Before Launch
- [ ] Eval suite passing (>90% quality, >99% safety)
- [ ] Fallback model configured and tested
- [ ] Rate limiting enabled (per-user and global)
- [ ] Cost budget and alerts configured
- [ ] Guardrails tested with adversarial inputs
- [ ] Observability (traces, metrics, logs) verified
- [ ] Rollback procedure documented and tested
- [ ] Load testing completed (target concurrency)

### Ongoing Operations
- [ ] Daily: Review cost and quality dashboards
- [ ] Weekly: Sample 50 conversations for human review
- [ ] Weekly: Run full eval suite against production
- [ ] Monthly: Red-team testing (new attack vectors)
- [ ] Quarterly: Review and update safety test suite
- [ ] On model update: Full regression testing before adoption
