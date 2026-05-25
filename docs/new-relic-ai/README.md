# New Relic for AI Agent Observability — Complete Guide

A comprehensive guide to monitoring, tracing, and optimizing AI agents in production using New Relic's full observability platform.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [New Relic AI Monitoring Fundamentals](01_Fundamentals.md) | Architecture, AI monitoring features, setup, auto-instrumentation |
| 2 | [Tracing AI Agents](02_Tracing.md) | Distributed tracing for LLM calls, tool use, RAG, custom spans |
| 3 | [Metrics & Dashboards](03_Metrics_Dashboards.md) | Custom metrics, NRQL queries, AI dashboards, SLOs |
| 4 | [Alerting & Incident Response](04_Alerting.md) | Alert conditions, policies, workflows, runbooks |
| 5 | [RUM for AI Interfaces](05_RUM.md) | Browser monitoring, Core Web Vitals, user journey tracking |
| 6 | [Production Patterns](06_Production_Patterns.md) | Cost tracking, quality monitoring, A/B testing, enterprise |

## 🏗️ New Relic AI Observability Stack

```
┌─────────────────────────────────────────────────────────────────┐
│                NEW RELIC AI OBSERVABILITY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ AI MONITORING (LLM-specific)                             │    │
│  │ • Model invocations (tokens, latency, cost)             │    │
│  │ • Conversation traces (multi-turn, tool calls)          │    │
│  │ • Response quality (feedback, hallucination)            │    │
│  │ • Token usage analytics                                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ APM (Application Performance)                            │    │
│  │ • Distributed tracing (agent → LLM → tools → DB)       │    │
│  │ • Error tracking and analysis                            │    │
│  │ • Transaction breakdown                                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ BROWSER / RUM (Real User Monitoring)                     │    │
│  │ • Core Web Vitals for chat UI                           │    │
│  │ • Time-to-first-token (perceived latency)               │    │
│  │ • User frustration signals                               │    │
│  │ • Session replay for debugging                           │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ INFRASTRUCTURE                                           │    │
│  │ • GPU utilization (self-hosted models)                  │    │
│  │ • Container/K8s metrics                                  │    │
│  │ • Network latency to LLM providers                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ LOGS                                                     │    │
│  │ • Structured conversation logs                           │    │
│  │ • Error context with prompts/responses                   │    │
│  │ • Guardrail violation logs                               │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ NRQL (Query Language)                                    │    │
│  │ • Ad-hoc analysis of any AI metric                      │    │
│  │ • Custom dashboards                                      │    │
│  │ • Alert conditions                                       │    │
│  │ • SLO tracking                                           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Why New Relic for AI Agents?

| Capability | How New Relic Helps |
|-----------|---------------------|
| "Is the agent working?" | APM: Error rates, latency, throughput |
| "Is the agent fast enough?" | RUM: Time-to-first-token, perceived latency |
| "Is the agent giving good answers?" | AI Monitoring: Quality scores, feedback tracking |
| "Is the agent costing too much?" | Custom metrics: Token usage, cost per conversation |
| "Why did the agent fail?" | Distributed tracing: Full request path with LLM calls |
| "Are users happy?" | RUM + Custom events: CSAT, abandonment, frustration |
| "Is the infrastructure healthy?" | Infra monitoring: GPU, memory, network |

## New Relic AI Monitoring vs Other Tools

| Feature | New Relic | LangSmith | Datadog | Arize Phoenix |
|---------|-----------|-----------|---------|---------------|
| LLM tracing | ✅ | ✅ | ✅ | ✅ |
| APM (full app) | ✅ | ❌ | ✅ | ❌ |
| RUM (browser) | ✅ | ❌ | ✅ | ❌ |
| Infrastructure | ✅ | ❌ | ✅ | ❌ |
| Logs | ✅ | ❌ | ✅ | ❌ |
| Custom dashboards | ✅ (NRQL) | Limited | ✅ | Limited |
| Alerting | ✅ (advanced) | Basic | ✅ | ❌ |
| Evaluation/Evals | ❌ | ✅ | ❌ | ✅ |
| Prompt management | ❌ | ✅ | ❌ | ❌ |
| Free tier | 100GB/month | Limited | ❌ | ✅ (OSS) |
| **Best for** | Full-stack observability | LLM-specific debugging | Enterprise APM | Open-source, local |

**Key insight:** New Relic gives you the **full picture** (infrastructure + application + browser + AI) in one platform. LangSmith is deeper for LLM-specific debugging. Best practice: **use both** — New Relic for production monitoring, LangSmith for development/debugging.
