# Real-Time Anomaly Explainer - AI-Powered Root Cause Analysis

A streaming pipeline that detects anomalies in real-time and uses RAG (Retrieval Augmented Generation) to automatically explain the probable root cause by searching similar past incidents, relevant runbooks, and correlated log patterns.

## 🎯 Problem Statement

When production systems generate alerts (high CPU, error spikes, latency degradation), engineers waste **15-30 minutes** manually correlating logs, metrics, and past incidents to understand *why* it's happening. This leads to:

- **Increased MTTR** (Mean Time To Resolution): Average 45 minutes for P1 incidents
- **On-call fatigue**: Engineers burned out from repetitive investigation
- **Knowledge silos**: Tribal knowledge trapped in individuals' heads
- **Repeated incidents**: Same root causes rediscovered every time
- **Revenue loss**: $5,600/minute average downtime cost for enterprises

## 💡 Solution

An intelligent streaming pipeline that:
1. **Ingests** real-time metrics and logs via Kafka
2. **Detects** anomalies using Apache Flink (sliding window + statistical methods)
3. **Correlates** anomalies with historical incidents using vector similarity search
4. **Explains** root causes using LLM with RAG context
5. **Notifies** engineers with actionable insights (not just "CPU is high")

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        DATA SOURCES                                       │
├─────────────────────────────────────────────────────────────────────────┤
│  Prometheus  │  CloudWatch  │  App Logs  │  APM Traces  │  K8s Events  │
└──────┬───────┴──────┬───────┴─────┬──────┴──────┬───────┴──────┬────────┘
       │              │             │             │              │
       ▼              ▼             ▼             ▼              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         KAFKA CLUSTER                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │
│  │ metrics-raw  │  │  logs-raw    │  │ events-raw   │                  │
│  │ (partitioned │  │ (partitioned │  │ (partitioned │                  │
│  │  by service) │  │  by service) │  │  by service) │                  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                  │
└─────────┼──────────────────┼──────────────────┼─────────────────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      APACHE FLINK CLUSTER                                │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐     │
│  │            ANOMALY DETECTION PIPELINE                           │     │
│  │                                                                 │     │
│  │  ┌─────────────┐   ┌──────────────┐   ┌───────────────────┐  │     │
│  │  │  Sliding    │──▶│  Z-Score /   │──▶│  Anomaly Event    │  │     │
│  │  │  Window     │   │  IQR / MAD   │   │  Emitter          │  │     │
│  │  │  (5min)     │   │  Detection   │   │                   │  │     │
│  │  └─────────────┘   └──────────────┘   └─────────┬─────────┘  │     │
│  │                                                   │            │     │
│  └───────────────────────────────────────────────────┼────────────┘     │
└──────────────────────────────────────────────────────┼──────────────────┘
                                                       │
                                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      RAG PIPELINE                                         │
│                                                                          │
│  ┌─────────────┐   ┌──────────────┐   ┌───────────────────────────┐   │
│  │  Embedding  │──▶│  Vector DB   │──▶│  Context Assembly          │   │
│  │  Service    │   │  (Pgvector/  │   │  (Top-K similar incidents  │   │
│  │  (OpenAI/   │   │   Qdrant)    │   │   + runbooks + logs)       │   │
│  │   Bedrock)  │   │              │   │                            │   │
│  └─────────────┘   └──────────────┘   └────────────┬──────────────┘   │
│                                                      │                   │
│  ┌───────────────────────────────────────────────────▼──────────────┐   │
│  │                    LLM INFERENCE                                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │  Prompt: "Given anomaly {metrics}, similar incidents        │ │   │
│  │  │  {context}, explain root cause and suggest fix"             │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └───────────────────────────────────────────────────┬──────────────┘   │
└──────────────────────────────────────────────────────┼──────────────────┘
                                                       │
                                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION & FEEDBACK                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────────┐     │
│  │  Slack   │  │PagerDuty │  │  Email   │  │  Feedback Loop     │     │
│  │  Bot     │  │  Alert   │  │  Digest  │  │  (Was this helpful?)│     │
│  └──────────┘  └──────────┘  └──────────┘  └────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start (POC)

```bash
# Start infrastructure
docker-compose up -d

# Seed vector DB with sample incidents
python seed_knowledge_base.py

# Start Flink anomaly detection job
./start-flink-job.sh

# Start RAG explainer service
./start-explainer.sh

# Simulate anomaly
python simulate_anomaly.py --type cpu_spike --service payment-service
```

## 📁 Documentation Structure

| Document | Description |
|----------|-------------|
| [System_Design.md](System_Design.md) | Complete HLD/LLD with theory |
| [POC_Implementation.md](POC_Implementation.md) | Step-by-step POC code |
| [Scale_and_Production.md](Scale_and_Production.md) | Scaling, HA, monitoring, security |
| [API_Documentation.md](API_Documentation.md) | API reference |

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Event Streaming | Apache Kafka | Ingest metrics/logs at scale |
| Stream Processing | Apache Flink | Real-time anomaly detection |
| Vector Database | Pgvector / Qdrant | Store & search incident embeddings |
| Embeddings | OpenAI / Amazon Bedrock | Convert text to vectors |
| LLM | Claude / GPT-4 | Generate root cause explanations |
| Application | Python (FastAPI) | RAG service & orchestration |
| Orchestration | Docker Compose | Local development |
| Monitoring | Prometheus + Grafana | Pipeline observability |

## 📊 Key Metrics

| Metric | Target |
|--------|--------|
| Anomaly detection latency | < 5 seconds |
| RAG explanation latency | < 10 seconds |
| End-to-end latency | < 15 seconds |
| Explanation accuracy | > 80% (human-rated) |
| False positive rate | < 5% |
| Knowledge base coverage | > 90% of known issues |
