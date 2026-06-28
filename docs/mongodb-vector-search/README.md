# MongoDB Vector Search — Complete In-Depth Course

> **Last Updated**: June 2025 | MongoDB 8.0 + Atlas Vector Search 2.0

---

## 📋 Course Overview

This course provides a comprehensive, production-ready guide to MongoDB Atlas Vector Search — from fundamentals to advanced RAG architectures, hybrid search, quantization, and real-world deployment patterns.

---

## 📚 Course Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | [Foundations](01_Foundations.md) | Vector embeddings, similarity metrics, ANN algorithms |
| 2 | [Setup & Configuration](02_Setup_Configuration.md) | Atlas cluster setup, index creation, SDK integration |
| 3 | [Index Types & Architecture](03_Index_Architecture.md) | HNSW, IVF, quantization, index lifecycle |
| 4 | [Basic Vector Search](04_Basic_Vector_Search.md) | `$vectorSearch` aggregation, kNN queries, filters |
| 5 | [Hybrid Search](05_Hybrid_Search.md) | Combining vector + full-text + geo with Reciprocal Rank Fusion |
| 6 | [Embedding Generation](06_Embedding_Generation.md) | OpenAI, Cohere, Voyage AI, local models, chunking strategies |
| 7 | [RAG Architecture](07_RAG_Architecture.md) | Retrieval-Augmented Generation end-to-end with MongoDB |
| 8 | [Advanced Patterns](08_Advanced_Patterns.md) | Multi-tenant, multi-modal, re-ranking, metadata filtering |
| 9 | [Performance & Scaling](09_Performance_Scaling.md) | Quantization, sharding, pre-filtering, benchmarking |
| 10 | [Production Deployment](10_Production_Deployment.md) | Monitoring, cost optimization, security, CI/CD |
| 11 | [Real-World Projects](11_Projects.md) | Semantic search engine, AI chatbot, recommendation system |

---

## 🎯 Prerequisites

- MongoDB Atlas account (M10+ cluster for Vector Search)
- Basic understanding of MongoDB CRUD and aggregation pipeline
- Python 3.10+ or Node.js 18+ (code examples in both)
- Familiarity with AI/ML concepts (helpful but not required)

---

## 🆕 What's New in 2025

| Feature | Version | Description |
|---------|---------|-------------|
| `$vectorSearch` stage | 7.0.2+ | Native aggregation pipeline stage |
| Binary Quantization | 8.0 | 32x memory reduction with minimal recall loss |
| Scalar Quantization | 8.0 | 4x memory reduction, better recall than BQ |
| Pre-filter optimization | 8.0 | Filter before ANN for better performance |
| Automatic embedding | Atlas 2025 | Built-in embedding generation (no external API) |
| Hybrid search with RRF | 7.0.2+ | Reciprocal Rank Fusion for combining scores |
| 4096-dimension support | 8.0 | Up to 4096 dimensions per vector |
| Streaming ingestion | 8.0 | Real-time vector index updates |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                          │
├─────────────┬──────────────┬──────────────┬─────────────────────┤
│  Embedding  │   Query      │   RAG        │   Hybrid Search     │
│  Pipeline   │   Engine     │   Orchestrator│  Coordinator        │
├─────────────┴──────────────┴──────────────┴─────────────────────┤
│                     MongoDB Atlas Vector Search                    │
├─────────────┬──────────────┬──────────────┬─────────────────────┤
│  HNSW Index │  IVF Index   │  Quantized   │  Full-Text Index    │
│  (default)  │  (large-scale)│  Index       │  (Atlas Search)     │
├─────────────┴──────────────┴──────────────┴─────────────────────┤
│                     MongoDB Atlas Cluster                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  Shard 1 │  │  Shard 2 │  │  Shard 3 │  │  Shard N │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Quick Start (5 minutes)

```python
from pymongo import MongoClient
from openai import OpenAI

# 1. Connect
client = MongoClient("mongodb+srv://<user>:<pass>@cluster.mongodb.net/")
collection = client["mydb"]["documents"]

# 2. Generate embedding
openai = OpenAI()
embedding = openai.embeddings.create(
    input="What is vector search?",
    model="text-embedding-3-small"
).data[0].embedding

# 3. Search
results = collection.aggregate([
    {
        "$vectorSearch": {
            "index": "vector_index",
            "path": "embedding",
            "queryVector": embedding,
            "numCandidates": 150,
            "limit": 10
        }
    },
    {"$project": {"title": 1, "score": {"$meta": "vectorSearchScore"}}}
])

for doc in results:
    print(f"{doc['title']} — Score: {doc['score']:.4f}")
```

---

## 📖 How to Use This Course

1. **Beginners**: Start from Module 1 and proceed sequentially
2. **Experienced with MongoDB**: Skip to Module 3 for index architecture
3. **Building RAG apps**: Jump to Modules 6-7 for embedding + RAG patterns
4. **Production optimization**: Focus on Modules 9-10

---

## 🔗 Resources

- [MongoDB Atlas Vector Search Documentation](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [MongoDB University — Vector Search Course](https://learn.mongodb.com/)
- [MongoDB AI Integrations (LangChain, LlamaIndex)](https://www.mongodb.com/developer/products/atlas/vector-search-ai/)
