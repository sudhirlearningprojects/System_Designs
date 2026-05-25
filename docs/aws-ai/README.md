# AWS AI & ML Services — Complete Guide

A comprehensive guide to AWS's AI platform: from Amazon Bedrock to SageMaker, Kendra, and production ML pipelines.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Amazon Bedrock](01_Bedrock.md) | Foundation models, agents, knowledge bases, guardrails |
| 2 | [Amazon Bedrock Agents & RAG](02_Bedrock_Agents_RAG.md) | Knowledge bases, agents with tools, action groups |
| 3 | [Amazon SageMaker](03_SageMaker.md) | Training, endpoints, pipelines, JumpStart, fine-tuning |
| 4 | [Amazon Kendra & OpenSearch](04_Kendra_OpenSearch.md) | Enterprise search, vector search, RAG |
| 5 | [Amazon Textract & Comprehend](05_Textract_Comprehend.md) | Document extraction, NLP, entity recognition |
| 6 | [Amazon Transcribe & Polly](06_Transcribe_Polly.md) | Speech-to-text, text-to-speech |
| 7 | [Amazon Q & CodeWhisperer](07_Amazon_Q.md) | AI assistant for business and developers |
| 8 | [Production Patterns](08_Production_Patterns.md) | Architecture, VPC, cost, monitoring, enterprise |

## 🏗️ AWS AI Ecosystem

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS AI PLATFORM                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  FOUNDATION MODELS (Amazon Bedrock)                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Claude (Anthropic) │ Llama (Meta) │ Titan (Amazon)       │    │
│  │ Mistral │ Cohere │ AI21 │ Stability AI                   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  AI SERVICES (Pre-built, API-based)                              │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Textract     │ │ Comprehend   │ │ Transcribe & Polly     │  │
│  │ (document AI)│ │ (NLP)        │ │ (speech)               │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Rekognition  │ │ Translate    │ │ Personalize            │  │
│  │ (vision)     │ │ (translation)│ │ (recommendations)      │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│                                                                   │
│  SEARCH & RETRIEVAL                                              │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Kendra       │ │ OpenSearch   │ │ Bedrock Knowledge Base │  │
│  │ (enterprise) │ │ (vector)     │ │ (managed RAG)          │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│                                                                   │
│  ML PLATFORM                                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ SageMaker: Training, Endpoints, Pipelines, JumpStart,    │    │
│  │ Studio, Canvas, Ground Truth, Feature Store               │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  AI ASSISTANTS                                                   │
│  ┌──────────────┐ ┌──────────────────────────────────────────┐  │
│  │ Amazon Q     │ │ Amazon Q Developer (CodeWhisperer)       │  │
│  │ (business)   │ │ (code generation, IDE integration)       │  │
│  └──────────────┘ └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Start (AWS CLI)

```bash
# Configure AWS CLI
aws configure
# Enter: Access Key, Secret Key, Region (us-east-1), Output (json)

# List available Bedrock models
aws bedrock list-foundation-models --query "modelSummaries[].modelId" --output table

# Invoke Claude on Bedrock
aws bedrock-runtime invoke-model \
  --model-id anthropic.claude-sonnet-4-20250514-v1:0 \
  --content-type application/json \
  --body '{"anthropic_version":"bedrock-2023-05-31","max_tokens":1024,"messages":[{"role":"user","content":"Hello!"}]}' \
  output.json

cat output.json | jq '.content[0].text'
```

## Service Selection Guide

| Need | AWS Service | When to Use |
|------|------------|-------------|
| Chat/LLM (Claude, Llama) | **Bedrock** | Production LLM apps, multi-model |
| RAG (managed) | **Bedrock Knowledge Bases** | Quick RAG setup, S3 documents |
| AI Agents | **Bedrock Agents** | Tool-calling agents with AWS integration |
| Enterprise search | **Kendra** | Structured enterprise docs, connectors |
| Vector search (custom) | **OpenSearch Serverless** | Custom RAG, high control |
| Custom model training | **SageMaker** | Fine-tuning, custom ML models |
| Document extraction | **Textract** | PDFs, forms, tables, invoices |
| NLP (entities, sentiment) | **Comprehend** | Text analysis without custom models |
| Speech-to-text | **Transcribe** | Audio/video transcription |
| Text-to-speech | **Polly** | Voice generation, neural voices |
| Image/video analysis | **Rekognition** | Object detection, face analysis |
| Content moderation | **Bedrock Guardrails** | LLM safety, topic filtering |
| Code generation | **Amazon Q Developer** | IDE integration, code review |

## AWS vs Azure vs GCP (AI Services)

| Capability | AWS | Azure | GCP |
|-----------|-----|-------|-----|
| LLM Gateway | Bedrock | Azure OpenAI | Vertex AI |
| Best Model Access | Claude, Llama, Mistral | GPT-4, Claude (limited) | Gemini, Claude |
| Managed RAG | Bedrock Knowledge Bases | AI Search + OpenAI | Vertex AI Search |
| Enterprise Search | Kendra | AI Search | Enterprise Search |
| Document AI | Textract | Document Intelligence | Document AI |
| Speech | Transcribe/Polly | Speech Service | Speech-to-Text |
| ML Platform | SageMaker | Azure ML | Vertex AI |
| AI Safety | Bedrock Guardrails | Content Safety | Responsible AI |
