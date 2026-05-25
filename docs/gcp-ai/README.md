# Google Cloud AI — Complete Guide

A comprehensive guide to Google Cloud's AI platform: from Vertex AI and Gemini to Document AI, Speech, and production ML pipelines.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Vertex AI & Gemini](01_Vertex_AI_Gemini.md) | Gemini models, generation, multimodal, grounding, tuning |
| 2 | [Vertex AI Search & RAG](02_Search_RAG.md) | Vertex AI Search, RAG Engine, vector search |
| 3 | [Vertex AI Agents](03_Agents.md) | Agent Builder, tools, datastores, conversation |
| 4 | [Document AI & Vision](04_Document_Vision.md) | OCR, form parsing, custom extractors, Vision API |
| 5 | [Speech & Translation](05_Speech_Translation.md) | Speech-to-text, text-to-speech, Cloud Translation |
| 6 | [Vertex AI ML Platform](06_ML_Platform.md) | Training, pipelines, endpoints, AutoML, Feature Store |
| 7 | [Safety & Responsible AI](07_Safety.md) | Safety filters, grounding, evaluation, bias detection |
| 8 | [Production Patterns](08_Production_Patterns.md) | Architecture, VPC-SC, cost, monitoring, enterprise |

## 🏗️ GCP AI Ecosystem

```
┌─────────────────────────────────────────────────────────────────┐
│                     GOOGLE CLOUD AI PLATFORM                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  FOUNDATION MODELS (Vertex AI)                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Gemini 2.0 Flash/Pro │ Gemini 1.5 Pro (2M context)      │    │
│  │ Imagen 3 │ Chirp (speech) │ Codey │ Embeddings          │    │
│  │ Model Garden: Claude, Llama, Mistral (partner models)    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  AI APPLICATIONS                                                 │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Vertex AI    │ │ Vertex AI    │ │ Vertex AI Agent        │  │
│  │ Search       │ │ RAG Engine   │ │ Builder                │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│                                                                   │
│  AI SERVICES (Pre-built)                                         │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Document AI  │ │ Speech-to-   │ │ Cloud Translation      │  │
│  │ (OCR, forms) │ │ Text / TTS   │ │ (130+ languages)       │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Vision AI    │ │ Natural      │ │ Video AI               │  │
│  │              │ │ Language API │ │                         │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│                                                                   │
│  ML PLATFORM                                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Vertex AI: Training, Pipelines, Endpoints, AutoML,       │    │
│  │ Feature Store, Model Registry, Experiments, TensorBoard   │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Start (gcloud CLI)

```bash
# Authenticate
gcloud auth login
gcloud config set project my-project-id

# Enable APIs
gcloud services enable aiplatform.googleapis.com
gcloud services enable generativelanguage.googleapis.com

# Quick Gemini call via gcloud
gcloud ai models generate-content \
  --model=gemini-2.0-flash \
  --region=us-central1 \
  --contents="Explain Google Cloud AI in 3 sentences."
```

## Gemini Model Family

| Model | Context | Best For | Pricing (per 1M tokens) |
|-------|---------|----------|------------------------|
| **Gemini 2.0 Flash** | 1M | Fast, multimodal, agents | $0.10 / $0.40 |
| **Gemini 2.0 Flash Thinking** | 1M | Complex reasoning | $0.10 / $0.40 |
| **Gemini 1.5 Pro** | 2M | Long context, analysis | $1.25 / $5.00 |
| **Gemini 1.5 Flash** | 1M | High volume, low cost | $0.075 / $0.30 |

## GCP vs AWS vs Azure (AI Services)

| Capability | GCP | AWS | Azure |
|-----------|-----|-----|-------|
| LLM Gateway | Vertex AI (Gemini) | Bedrock | Azure OpenAI |
| Unique Strength | 2M context, multimodal native | Multi-provider (Claude, Llama) | GPT-4, enterprise integration |
| Managed RAG | Vertex AI Search / RAG Engine | Bedrock Knowledge Bases | AI Search + OpenAI |
| Document AI | Document AI | Textract | Document Intelligence |
| ML Platform | Vertex AI | SageMaker | Azure ML |
| AI Safety | Safety filters + grounding | Bedrock Guardrails | Content Safety |
| Edge/Mobile | MediaPipe, TFLite | — | — |
