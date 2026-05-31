# OpenAI — Complete Development Guide

From API fundamentals to Assistants, fine-tuning, and production AI agent development.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [API Fundamentals](01_API_Fundamentals.md) | Models, chat completions, streaming, vision, structured output |
| 2 | [Function Calling & Tools](02_Function_Calling.md) | Tool definitions, parallel calls, structured output |
| 3 | [Assistants API & Agents](03_Assistants_Agents.md) | Assistants, threads, runs, code interpreter, file search |
| 4 | [Fine-Tuning](04_Fine_Tuning.md) | Data prep, training, evaluation, deployment |
| 5 | [Embeddings & RAG](05_Embeddings_RAG.md) | Embedding models, vector search, RAG patterns |
| 6 | [Realtime & Speech](06_Realtime_Speech.md) | Realtime API, Whisper, TTS |
| 7 | [Safety & Moderation](07_Safety.md) | Moderation API, content filtering, best practices |
| 8 | [Production Patterns](08_Production.md) | Rate limits, cost, batching, error handling |

## Model Family (2024-2025)

| Model | Context | Best For | Cost (in/out per 1M) |
|-------|---------|----------|---------------------|
| **GPT-4o** | 128K | Best overall, multimodal | $2.50 / $10.00 |
| **GPT-4o-mini** | 128K | Fast, cheap, high-volume | $0.15 / $0.60 |
| **o1** | 200K | Complex reasoning, math, code | $15.00 / $60.00 |
| **o1-mini** | 128K | Fast reasoning | $3.00 / $12.00 |
| **GPT-4 Turbo** | 128K | Previous gen (legacy) | $10.00 / $30.00 |

## Quick Start

```python
from openai import OpenAI

client = OpenAI()  # Uses OPENAI_API_KEY env var

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello!"}],
)
print(response.choices[0].message.content)
```
