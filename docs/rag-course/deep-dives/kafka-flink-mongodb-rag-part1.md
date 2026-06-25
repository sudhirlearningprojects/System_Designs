# Deep Dive: Building RAG with Confluent Kafka, Flink & MongoDB

## Part 1: Architecture & Overview

---

## Why Kafka + Flink + MongoDB for RAG?

Traditional RAG systems have a critical weakness: **stale data**. Documents are ingested once and become outdated. This architecture solves that by creating a **real-time, streaming RAG pipeline** where:

- **Confluent Kafka** — Event backbone for real-time document streaming and change data capture
- **Apache Flink** — Stream processing for real-time chunking, enrichment, and embedding generation
- **MongoDB Atlas** — Vector store + document store in one (Atlas Vector Search)

### The Problem with Batch RAG

```
Traditional RAG:
Document Updated → [Hours/Days Later] → Re-indexed → Available for Retrieval

Streaming RAG (This Architecture):
Document Updated → Kafka Event → Flink Processing → MongoDB Vector Store → Available (seconds)
```

### Key Benefits

| Benefit | Description |
|---------|-------------|
| **Real-time freshness** | Documents available for retrieval within seconds of creation/update |
| **Exactly-once processing** | Kafka + Flink guarantee no duplicates or missed documents |
| **Scalable ingestion** | Handle millions of document changes per second |
| **Unified storage** | MongoDB stores both vectors and source documents (no dual-write) |
| **CDC support** | Capture changes from any database via Kafka Connect |
| **Backpressure handling** | Flink manages throughput spikes gracefully |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        DATA SOURCES                                       │
├─────────────┬──────────────┬─────────────┬──────────────────────────────┤
│ PostgreSQL  │ MySQL        │ S3/GCS      │ REST APIs / Webhooks          │
│ (CDC)       │ (CDC)        │ (New Files) │ (Slack, Confluence, etc.)     │
└──────┬──────┴──────┬───────┴──────┬──────┴──────────────┬───────────────┘
       │             │              │                      │
       ▼             ▼              ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     CONFLUENT KAFKA CLUSTER                               │
│                                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │
│  │ raw.documents│  │ enriched.docs│  │ embeddings   │                  │
│  │   (topic)    │  │   (topic)    │  │   (topic)    │                  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                  │
│         │                  │                  │                           │
│  ┌──────┴──────────────────┴──────────────────┴──────┐                  │
│  │              Schema Registry (Avro/JSON)           │                  │
│  └───────────────────────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      APACHE FLINK CLUSTER                                 │
│                                                                           │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│  │ Document Parser  │  │ Chunking Engine  │  │ Embedding Service│       │
│  │ & Cleaner        │→ │ (Semantic/Fixed) │→ │ (Async Batched)  │       │
│  └─────────────────┘  └──────────────────┘  └────────┬─────────┘       │
│                                                        │                  │
└────────────────────────────────────────────────────────┼──────────────────┘
                                                         │
                                                         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      MONGODB ATLAS                                        │
│                                                                           │
│  ┌──────────────────────┐  ┌────────────────────────────────┐           │
│  │ documents collection │  │ Atlas Vector Search Index       │           │
│  │ (source + chunks +   │  │ (HNSW, cosine similarity)      │           │
│  │  embeddings + meta)  │  │                                │           │
│  └──────────────────────┘  └────────────────────────────────┘           │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      RAG APPLICATION LAYER                                │
│                                                                           │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐  ┌───────────┐        │
│  │ Query API │  │ Retriever    │  │ Reranker   │  │ LLM       │        │
│  │ (FastAPI) │→ │ (Vector +   │→ │ (Cohere/   │→ │ (GPT-4o / │        │
│  │           │  │  Full-text)  │  │  Local)    │  │  Claude)  │        │
│  └──────────┘  └──────────────┘  └────────────┘  └───────────┘        │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

```
1. Source Change Detected (CDC / Webhook / File Upload)
       │
       ▼
2. Kafka Producer publishes to `raw.documents` topic
       │
       ▼
3. Flink Job 1: Parse & Clean
   - Extract text from PDF/DOCX/HTML
   - Remove boilerplate, fix encoding
   - Publish to `cleaned.documents`
       │
       ▼
4. Flink Job 2: Chunk & Enrich
   - Split into semantic chunks
   - Extract metadata (entities, topics, dates)
   - Generate contextual headers
   - Publish to `chunked.documents`
       │
       ▼
5. Flink Job 3: Embed
   - Batch embed chunks (OpenAI / local model)
   - Handle rate limits with backpressure
   - Publish to `embeddings` topic
       │
       ▼
6. MongoDB Kafka Sink Connector
   - Upsert documents with vectors into MongoDB
   - Atlas Vector Search index auto-updates
       │
       ▼
7. RAG Query (Real-time)
   - User query → Vector search on MongoDB
   - Retrieve chunks → Rerank → LLM generates answer
```

---

## Technology Versions

| Component | Version | Notes |
|-----------|---------|-------|
| Confluent Platform | 7.6+ | Or Confluent Cloud |
| Apache Kafka | 3.7+ | Via Confluent |
| Apache Flink | 1.19+ | With Kafka connector |
| MongoDB Atlas | 7.0+ | Atlas Vector Search GA |
| Python | 3.11+ | Flink PyFlink or Java |
| pymongo | 4.7+ | With vector search support |

---

## Prerequisites

```bash
# Docker Compose for local development
# Confluent Platform + Flink + MongoDB
docker compose up -d

# Python dependencies
pip install confluent-kafka pymongo langchain-mongodb
pip install apache-flink openai sentence-transformers
pip install langchain-openai cohere
```

---

## Next Parts

- [Part 2: Confluent Kafka Setup & Producers](./kafka-flink-mongodb-rag-part2.md)
- [Part 3: Flink Stream Processing Pipeline](./kafka-flink-mongodb-rag-part3.md)
- [Part 4: MongoDB Atlas Vector Search](./kafka-flink-mongodb-rag-part4.md)
- [Part 5: RAG Application & Query Layer](./kafka-flink-mongodb-rag-part5.md)
- [Part 6: Production Deployment & Monitoring](./kafka-flink-mongodb-rag-part6.md)
- [Part 7A: Flink AI Model Inference — ONNX Runtime](./kafka-flink-mongodb-rag-part7a.md)
- [Part 7B: PyFlink + GPU Inference & DJL](./kafka-flink-mongodb-rag-part7b.md)
- [Part 7C: Model Management, A/B Testing & Hybrid Strategy](./kafka-flink-mongodb-rag-part7c.md)
