# Large Language Models — Complete Engineering Guide

From theory to production: understanding, building, fine-tuning, and deploying LLMs with safety and automation.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [LLM Theory & Architecture](01_Theory.md) | Transformer architecture, attention, training objectives, scaling laws |
| 2 | [Pre-Training](02_PreTraining.md) | Data pipelines, tokenization, training at scale, infrastructure |
| 3 | [Fine-Tuning](03_FineTuning.md) | SFT, RLHF, DPO, LoRA, QLoRA, data preparation, evaluation |
| 4 | [Inference & Serving](04_Inference.md) | KV-cache, quantization, speculative decoding, batching, serving |
| 5 | [Prompt Engineering & In-Context Learning](05_Prompting.md) | Techniques, chain-of-thought, few-shot, structured output |
| 6 | [RAG & Grounding](06_RAG.md) | Retrieval-augmented generation, knowledge bases, grounding |
| 7 | [AI Agents & Tool Use](07_Agents.md) | Agent architectures, planning, tool calling, multi-agent |
| 8 | [Safety & Alignment](08_Safety.md) | RLHF, constitutional AI, red-teaming, guardrails, bias |
| 9 | [Evaluation & Benchmarks](09_Evaluation.md) | Metrics, benchmarks, LLM-as-judge, human eval, A/B testing |
| 10 | [Production & MLOps](10_Production.md) | Deployment, monitoring, cost optimization, CI/CD for LLMs |

## 🧠 LLM Landscape (2024-2025)

| Model Family | Creator | Parameters | Context | Open/Closed |
|-------------|---------|-----------|---------|-------------|
| GPT-4o / o1 | OpenAI | Unknown | 128K | Closed |
| Claude 4 (Opus/Sonnet) | Anthropic | Unknown | 200K | Closed |
| Gemini 2.0 | Google | Unknown | 2M | Closed |
| Llama 3.1 | Meta | 8B/70B/405B | 128K | Open |
| Mistral Large | Mistral | 123B | 128K | Open-weight |
| DeepSeek V3 | DeepSeek | 671B (MoE) | 128K | Open |
| Qwen 2.5 | Alibaba | 0.5B-72B | 128K | Open |
| Command R+ | Cohere | 104B | 128K | Closed |

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    LLM LIFECYCLE                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  PHASE 1: PRE-TRAINING                                           │
│  Internet-scale data → Tokenize → Train on next-token prediction │
│  Cost: $1M-$100M+ │ Time: weeks-months │ Data: trillions tokens  │
│                                                                   │
│  PHASE 2: POST-TRAINING (Alignment)                              │
│  SFT (instruction following) → RLHF/DPO (human preferences)     │
│  Cost: $10K-$1M │ Time: days-weeks │ Data: 10K-1M examples       │
│                                                                   │
│  PHASE 3: DEPLOYMENT                                             │
│  Quantize → Optimize → Serve → Monitor → Iterate                │
│  Ongoing cost: inference compute                                  │
│                                                                   │
│  PHASE 4: APPLICATION                                            │
│  Prompt engineering → RAG → Agents → Safety → Evaluation         │
│  Build products on top of the model                              │
└─────────────────────────────────────────────────────────────────┘
```

## Key Concepts Quick Reference

| Concept | What It Is | Why It Matters |
|---------|-----------|----------------|
| **Transformer** | Architecture based on self-attention | Foundation of all modern LLMs |
| **Tokenization** | Text → integer sequences | Determines vocabulary and efficiency |
| **Attention** | Mechanism to focus on relevant parts | Enables understanding of context |
| **Scaling Laws** | Performance improves predictably with scale | Guides resource allocation |
| **RLHF** | Training with human preference feedback | Makes models helpful and safe |
| **LoRA** | Low-rank adaptation for efficient fine-tuning | Fine-tune with 100x less compute |
| **Quantization** | Reduce precision (fp32→int4) | 4x memory reduction, faster inference |
| **KV-Cache** | Cache attention computations | Enables fast autoregressive generation |
| **RAG** | Retrieve external knowledge at inference | Reduces hallucination, adds knowledge |
| **Guardrails** | Safety filters on input/output | Prevents harmful or incorrect responses |
