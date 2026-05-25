# Claude AI — Complete Development Guide

A comprehensive guide to building with Claude AI: from API fundamentals to complex agentic systems and fine-tuning.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Claude Fundamentals & API](01_Fundamentals_and_API.md) | Models, API reference, messages, streaming, vision, token management |
| 2 | [Prompt Engineering](02_Prompt_Engineering.md) | System prompts, techniques, chain-of-thought, few-shot, XML tags |
| 3 | [Tool Use & Function Calling](03_Tool_Use.md) | Tool definitions, execution, parallel tools, error handling |
| 4 | [RAG & Knowledge Systems](04_RAG_and_Knowledge.md) | Embeddings, vector DBs, chunking, retrieval, contextual grounding |
| 5 | [Building AI Agents](05_Building_Agents.md) | Agent architectures, orchestration, memory, planning, multi-agent |
| 6 | [Fine-Tuning Claude](06_Fine_Tuning.md) | Custom models, training data, evaluation, deployment |
| 7 | [Production & Safety](07_Production_and_Safety.md) | Guardrails, content filtering, rate limits, cost optimization, monitoring |

## 🏗️ Claude Model Family (2024-2025)

| Model | Context | Best For | Cost (Input/Output per 1M tokens) |
|-------|---------|----------|-----|
| **Claude 4 Opus** | 200K | Most complex reasoning, research | $15 / $75 |
| **Claude 4 Sonnet** | 200K | Best balance of speed + intelligence | $3 / $15 |
| **Claude 3.5 Haiku** | 200K | Fast, cheap, high-volume tasks | $0.80 / $4 |
| **Claude 3.5 Sonnet** | 200K | Previous-gen balanced model | $3 / $15 |

## ⚡ Quick Start

```python
import anthropic

client = anthropic.Anthropic()  # Uses ANTHROPIC_API_KEY env var

message = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello, Claude!"}]
)
print(message.content[0].text)
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    YOUR APPLICATION                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ Agent Logic  │  │ Tool Router  │  │ Memory Manager   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────────┘  │
│         └──────────────────┼──────────────────┘             │
│                            ▼                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ANTHROPIC SDK                            │   │
│  │   Messages API │ Streaming │ Tool Use │ Batches      │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                 │
├────────────────────────────┼─────────────────────────────────┤
│                            ▼                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ANTHROPIC API                            │   │
│  │   Claude Models │ Fine-tuned Models │ Embeddings     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```
