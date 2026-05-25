# LangChain & LangGraph — Complete AI Agent Development Guide

From development to production: building, deploying, and operating AI agents with the LangChain ecosystem.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [LangChain Core](01_LangChain_Core.md) | Chat models, prompts, output parsers, chains, LCEL |
| 2 | [RAG with LangChain](02_RAG.md) | Document loaders, splitters, embeddings, vector stores, retrievers |
| 3 | [Tools & Tool Calling](03_Tools.md) | Built-in tools, custom tools, tool calling agents, structured output |
| 4 | [LangGraph Fundamentals](04_LangGraph_Fundamentals.md) | StateGraph, nodes, edges, conditional routing, checkpointing |
| 5 | [LangGraph Advanced Agents](05_LangGraph_Advanced.md) | Multi-agent, human-in-the-loop, subgraphs, streaming, persistence |
| 6 | [LangSmith (Observability)](06_LangSmith.md) | Tracing, evaluation, datasets, prompt hub, monitoring |
| 7 | [LangServe & Deployment](07_LangServe_Deployment.md) | LangServe, LangGraph Cloud, Docker, Kubernetes, production |
| 8 | [Production Patterns](08_Production_Patterns.md) | Error handling, fallbacks, caching, rate limiting, testing |

## 🏗️ LangChain Ecosystem (2024-2025)

```
┌─────────────────────────────────────────────────────────────────┐
│                    LANGCHAIN ECOSYSTEM                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    LANGGRAPH                              │    │
│  │  Stateful, multi-actor agent orchestration               │    │
│  │  (graphs, cycles, persistence, human-in-the-loop)        │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │                  LANGCHAIN CORE                           │    │
│  │  ┌──────────┐ ┌──────────┐ ┌────────┐ ┌─────────────┐ │    │
│  │  │  Models  │ │ Prompts  │ │ Tools  │ │  Retrievers │ │    │
│  │  │(Chat/LLM)│ │(Templates│ │(Actions│ │  (RAG)      │ │    │
│  │  └──────────┘ │ + LCEL)  │ │ + APIs)│ └─────────────┘ │    │
│  │               └──────────┘ └────────┘                    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────▼───────────────────────────────┐    │
│  │              LANGCHAIN INTEGRATIONS                        │    │
│  │  Anthropic │ OpenAI │ Pinecone │ Chroma │ Postgres │ ... │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │    LANGSMITH     │  │   LANGSERVE      │  │  LANGGRAPH   │  │
│  │  (Observability) │  │   (Deployment)   │  │    CLOUD     │  │
│  │  Tracing, Evals  │  │   REST APIs      │  │  (Managed)   │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Package Structure (Latest)

```
langchain-core          # Base abstractions (Runnables, LCEL, messages)
langchain               # Chains, agents, retrieval strategies
langchain-community     # Third-party integrations
langchain-anthropic     # Anthropic/Claude integration
langchain-openai        # OpenAI integration
langgraph              # Stateful agent orchestration
langsmith              # Observability & evaluation
langserve              # Deploy as REST API
```

## ⚡ Quick Start

```python
# pip install langchain langchain-anthropic langgraph langsmith

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage

model = ChatAnthropic(model="claude-sonnet-4-20250514")
response = model.invoke([HumanMessage(content="Hello!")])
print(response.content)
```

## When to Use What

| Need | Use |
|------|-----|
| Simple LLM call with prompt | LangChain Core (ChatModel + PromptTemplate) |
| Chain multiple steps | LCEL (LangChain Expression Language) |
| RAG (search + answer) | LangChain Retrievers + Chains |
| Single-turn tool use | LangChain Tool Calling |
| Multi-step agent with state | **LangGraph** |
| Agent with human approval | **LangGraph** (interrupt + checkpointing) |
| Multi-agent collaboration | **LangGraph** (subgraphs) |
| Production deployment | LangServe or LangGraph Cloud |
| Monitoring & evaluation | LangSmith |
