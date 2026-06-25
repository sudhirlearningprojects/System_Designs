# AWS Lambda Deep Dive - "ServerlessBot Arena"

## 🎮 Project: AI Bot Battle Arena (Serverless)

An innovative multiplayer bot-battle simulation platform built **entirely on Lambda** that exercises every Lambda capability, integration pattern, and edge case — while staying within **AWS Free Tier** (~$0-2/month).

### Why This Project?

Most Lambda tutorials show a simple API → Lambda → DynamoDB. This project pushes Lambda to its absolute limits:
- **15-minute long-running orchestrations** (Step Functions)
- **10GB memory + 6 vCPU** compute-heavy workloads
- **Streaming responses** (Response streaming)
- **Container images** (up to 10GB)
- **Provisioned concurrency** vs cold starts
- **Lambda Layers** for shared code
- **Event source mappings** (SQS, Kinesis, DynamoDB Streams)
- **Lambda@Edge** and **CloudFront Functions**
- **Lambda URLs** (built-in HTTPS without API Gateway)
- **Recursive invocation detection**
- **SnapStart** (Java cold start optimization)
- **Powertools** for observability

---

## 📚 Document Structure

| Part | Title | Concepts Covered |
|------|-------|-----------------|
| [Part 1](Part1_Architecture_and_Concepts.md) | Architecture & Lambda Concepts | All Lambda features, limits, pricing, architecture |
| [Part 2](Part2_Core_Implementation.md) | Core Implementation | Function URLs, Layers, Container images, DynamoDB |
| [Part 3](Part3_Event_Driven_Patterns.md) | Event-Driven Patterns | SQS, SNS, EventBridge, Kinesis, DynamoDB Streams |
| [Part 4](Part4_Advanced_Patterns.md) | Advanced Patterns | Step Functions, Streaming, Recursion, Edge |
| [Part 5](Part5_Performance_and_Limits.md) | Performance & Limits | Cold starts, memory tuning, concurrency, SnapStart |
| [Part 6](Part6_Observability_and_Cost.md) | Observability & Cost | X-Ray, Powertools, CloudWatch, cost optimization |

---

## 🎯 What You'll Build

```
┌──────────────────────────────────────────────────────────────┐
│                   ServerlessBot Arena                          │
│                                                                │
│  Players submit bot strategies (code) that battle each other  │
│  Everything runs on Lambda — zero servers, zero containers    │
│                                                                │
│  ┌──────────┐  ┌───────────┐  ┌─────────────┐  ┌─────────┐ │
│  │ Bot      │  │ Match     │  │ Replay      │  │ Leader- │ │
│  │ Registry │  │ Engine    │  │ Streamer    │  │ board   │ │
│  │ (CRUD)   │  │ (Battle)  │  │ (Real-time) │  │ (Rank)  │ │
│  └──────────┘  └───────────┘  └─────────────┘  └─────────┘ │
│                                                                │
│  Lambda Features Exercised:                                    │
│  ✓ Function URLs        ✓ Layers           ✓ Containers      │
│  ✓ Step Functions       ✓ Streaming        ✓ Provisioned CC  │
│  ✓ Event Sources (SQS)  ✓ DDB Streams     ✓ EventBridge     │
│  ✓ Lambda@Edge          ✓ Destinations    ✓ Dead Letter Q    │
│  ✓ Powertools           ✓ X-Ray           ✓ SnapStart        │
│  ✓ Recursive detection  ✓ 10GB memory     ✓ EFS mount        │
└──────────────────────────────────────────────────────────────┘
```

---

## 💰 Cost Estimate (Free Tier)

| Service | Free Tier | Project Usage | Monthly Cost |
|---------|-----------|---------------|-------------|
| Lambda | 1M requests + 400K GB-sec | ~200K requests | $0.00 |
| DynamoDB | 25 GB + 25 WCU/RCU | ~1 GB, 5 WCU | $0.00 |
| SQS | 1M requests | ~100K messages | $0.00 |
| SNS | 1M publishes | ~50K publishes | $0.00 |
| EventBridge | Free for AWS events | ~10K custom events | $0.00 |
| Step Functions | 4000 state transitions | ~2000 transitions | $0.00 |
| S3 | 5 GB | ~500 MB | $0.00 |
| CloudWatch | 5 GB logs | ~1 GB | $0.00 |
| API Gateway | 1M REST calls | ~50K calls | $0.00 |
| X-Ray | 100K traces | ~10K traces | $0.00 |
| **Total** | | | **~$0.00** |

---

## 🚀 Quick Start

```bash
# Prerequisites
npm install -g aws-cdk
pip install aws-lambda-powertools boto3

# Deploy
cd infrastructure
cdk deploy --all

# Register a bot
curl -X POST https://<function-url>/bots \
  -d '{"name":"Aggressive","strategy":"always_attack","author":"player1"}'

# Start a match
curl -X POST https://<function-url>/matches \
  -d '{"bot1":"bot-001","bot2":"bot-002","rounds":100}'

# Watch replay (streaming response)
curl -N https://<function-url>/replay/match-001
```

---

## 📋 Prerequisites

- AWS Account (Free Tier eligible)
- AWS CLI configured
- Node.js 18+ (for CDK)
- Python 3.11+ (for Lambda functions)
- Docker (for container-based Lambda)
