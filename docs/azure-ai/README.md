# Azure AI Services — Complete Guide

A comprehensive guide to Azure's AI platform: from Azure OpenAI to AI Search, Document Intelligence, and production ML pipelines.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Azure OpenAI Service](01_Azure_OpenAI.md) | GPT-4, embeddings, DALL-E, fine-tuning, content filtering |
| 2 | [Azure AI Search](02_AI_Search.md) | Vector search, hybrid search, semantic ranking, RAG |
| 3 | [Azure AI Studio](03_AI_Studio.md) | Prompt flow, model catalog, evaluation, deployment |
| 4 | [Azure Document Intelligence](04_Document_Intelligence.md) | OCR, form recognition, layout analysis, custom models |
| 5 | [Azure AI Speech & Language](05_Speech_Language.md) | Speech-to-text, text-to-speech, translation, NLU |
| 6 | [Azure Machine Learning](06_Azure_ML.md) | MLOps, pipelines, endpoints, AutoML, responsible AI |
| 7 | [Azure AI Content Safety](07_Content_Safety.md) | Text/image moderation, prompt shields, groundedness |
| 8 | [Production Patterns](08_Production_Patterns.md) | Architecture, networking, cost, monitoring, enterprise |

## 🏗️ Azure AI Ecosystem

```
┌─────────────────────────────────────────────────────────────────┐
│                      AZURE AI PLATFORM                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  FOUNDATION MODELS                                               │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Azure OpenAI: GPT-4o, GPT-4, o1, DALL-E 3, Whisper     │    │
│  │ Model Catalog: Llama 3, Mistral, Phi-3, Cohere, Jamba   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  AI SERVICES (Pre-built)                                         │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ AI Search    │ │ Document     │ │ Speech & Language       │  │
│  │ (RAG, vector)│ │ Intelligence │ │ (STT, TTS, translate)  │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ Content      │ │ Computer     │ │ Custom Vision /        │  │
│  │ Safety       │ │ Vision       │ │ Face API               │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│                                                                   │
│  DEVELOPMENT & ORCHESTRATION                                     │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │ AI Studio    │ │ Prompt Flow  │ │ Semantic Kernel         │  │
│  │ (portal)     │ │ (DAG-based)  │ │ (SDK for agents)       │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│                                                                   │
│  ML PLATFORM                                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Azure Machine Learning: Pipelines, Endpoints, AutoML,    │    │
│  │ Responsible AI, MLflow, Compute clusters                  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Start (Azure CLI)

```bash
# Login
az login

# Create resource group
az group create --name rg-ai-demo --location eastus

# Create Azure OpenAI resource
az cognitiveservices account create \
  --name my-openai-resource \
  --resource-group rg-ai-demo \
  --kind OpenAI \
  --sku S0 \
  --location eastus

# Deploy GPT-4o model
az cognitiveservices account deployment create \
  --name my-openai-resource \
  --resource-group rg-ai-demo \
  --deployment-name gpt-4o \
  --model-name gpt-4o \
  --model-version "2024-08-06" \
  --model-format OpenAI \
  --sku-name Standard \
  --sku-capacity 30  # 30K tokens per minute

# Get endpoint and key
az cognitiveservices account show \
  --name my-openai-resource \
  --resource-group rg-ai-demo \
  --query "properties.endpoint" -o tsv

az cognitiveservices account keys list \
  --name my-openai-resource \
  --resource-group rg-ai-demo
```

## Service Selection Guide

| Need | Azure Service | Alternative |
|------|--------------|-------------|
| Chat/completion (GPT-4) | Azure OpenAI | Direct OpenAI API |
| RAG / document search | Azure AI Search | Pinecone, Weaviate |
| PDF/form extraction | Document Intelligence | AWS Textract |
| Speech-to-text | Azure Speech | AWS Transcribe |
| Content moderation | Content Safety | OpenAI Moderation |
| Custom ML models | Azure ML | SageMaker, Vertex AI |
| Agent orchestration | Prompt Flow / Semantic Kernel | LangChain, LlamaIndex |
| Image generation | Azure OpenAI (DALL-E) | Stability AI |
| Embeddings | Azure OpenAI | Voyage AI, Cohere |
