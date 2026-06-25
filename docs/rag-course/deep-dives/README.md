# Deep Dives

Advanced, production-focused deep dives on specific RAG architectures.

## Available Deep Dives

### 🔄 Real-Time RAG with Confluent Kafka, Flink & MongoDB

Build a streaming RAG pipeline where documents become searchable within seconds of creation/modification.

| Part | Topic | Description |
|------|-------|-------------|
| [Part 1](./kafka-flink-mongodb-rag-part1.md) | Architecture & Overview | System design, data flow, tech stack |
| [Part 2](./kafka-flink-mongodb-rag-part2.md) | Confluent Kafka Setup | Topics, schemas, producers, CDC, connectors |
| [Part 3](./kafka-flink-mongodb-rag-part3.md) | Flink Stream Processing | Parsing, chunking, embedding jobs with exactly-once |
| [Part 4](./kafka-flink-mongodb-rag-part4.md) | MongoDB Atlas Vector Search | Schema design, vector indexes, hybrid search |
| [Part 5](./kafka-flink-mongodb-rag-part5.md) | RAG Application Layer | FastAPI service, retriever, generator, caching |
| [Part 6](./kafka-flink-mongodb-rag-part6.md) | Production & Monitoring | Deployment, Terraform, metrics, alerting, cost |
| [Part 7A](./kafka-flink-mongodb-rag-part7a.md) | Flink AI Inference — ONNX Runtime | Export models, Java ONNX operator, batched inference |
| [Part 7B](./kafka-flink-mongodb-rag-part7b.md) | PyFlink + GPU & DJL | PyFlink ONNX, CUDA GPU, Triton server, DJL |
| [Part 7C](./kafka-flink-mongodb-rag-part7c.md) | Model Management & A/B Testing | Hot-swap, A/B testing, hybrid strategy, re-embedding |

**Key Technologies**: Confluent Cloud, Apache Flink, MongoDB Atlas, ONNX Runtime, Triton, DJL, Cohere Rerank, Redis

**Architecture Highlights**:
- Real-time ingestion (seconds, not hours)
- Exactly-once processing guarantees
- Hybrid search (vector + full-text) in MongoDB
- Multi-tenant with RBAC
- Auto-scaling with Kubernetes

---

[← Back to Course Home](../README.md)
