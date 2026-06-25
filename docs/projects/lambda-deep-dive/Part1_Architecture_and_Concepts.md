# Part 1: Architecture & Lambda Concepts

## 1.1 The "ServerlessBot Arena" Idea

**Concept**: Players write simple bot strategies (attack, defend, heal, counter). Bots are paired in matches and battle over N rounds. The entire system — registration, matchmaking, battle execution, replay streaming, leaderboard — runs on Lambda with zero servers.

**Why it's innovative for learning**:
- Each subsystem exercises a **different Lambda capability**
- You'll hit and understand **every Lambda limit**
- Integrates with 10+ AWS services
- Costs $0 on Free Tier

---

## 1.2 Full Architecture

```
                    ┌─────────────────────────────┐
                    │     CloudFront (CDN)         │
                    │  + Lambda@Edge (auth)        │
                    └──────────┬──────────────────┘
                               │
                    ┌──────────▼──────────────────┐
                    │   API Gateway (WebSocket)    │──── Real-time updates
                    │   + Lambda Function URLs     │──── REST API (no APIGW cost)
                    └──────────┬──────────────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌─────────────────┐ ┌──────────────────┐ ┌────────────────────┐
│  Bot Registry   │ │  Match Engine    │ │  Replay Streamer   │
│  (Function URL) │ │  (Step Function) │ │  (Response Stream) │
│                 │ │                  │ │                    │
│  • CRUD bots    │ │  • Matchmaking   │ │  • Stream moves    │
│  • Validate     │ │  • Execute rounds│ │  • SSE format      │
│  • Store DDB    │ │  • Score calc    │ │  • Chunked replay  │
└────────┬────────┘ └────────┬─────────┘ └────────────────────┘
         │                   │
         │                   │ (triggers)
         ▼                   ▼
┌─────────────────┐ ┌──────────────────┐ ┌────────────────────┐
│  DynamoDB       │ │  SQS Queue       │ │  S3 (Replays)      │
│  (Bots, Scores) │ │  (Match Queue)   │ │  (Match history)   │
│                 │ │                  │ │                    │
│  + DDB Streams ─┼─┤  + DLQ           │ │  + Event Notif.    │
└────────┬────────┘ └────────┬─────────┘ └────────┬───────────┘
         │                   │                     │
         ▼                   ▼                     ▼
┌─────────────────┐ ┌──────────────────┐ ┌────────────────────┐
│  Leaderboard    │ │  Battle Worker   │ │  Analytics Lambda  │
│  Updater        │ │  (Heavy compute) │ │  (Kinesis consumer)│
│  (DDB Stream)   │ │  (10GB mem)      │ │                    │
│                 │ │  (Container img) │ │  • Win rates       │
│  • Rank calc    │ │                  │ │  • Strategy stats  │
│  • ELO rating   │ │  • Run N rounds  │ │  • Trends          │
└─────────────────┘ │  • Score moves   │ └────────────────────┘
                    │  • Write replay  │
                    └──────────────────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │  EventBridge     │
                    │  (Match events)  │
                    │                  │
                    │  • match.started │
                    │  • match.ended   │
                    │  • bot.ranked    │
                    └──────────────────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │  SNS Topic       │
                    │  (Notifications) │
                    │                  │
                    │  • Email results │
                    │  • Push alerts   │
                    └──────────────────┘
```

---

## 1.3 Lambda Features Map (What Each Component Teaches)

| Component | Lambda Feature | Why It Matters |
|-----------|---------------|----------------|
| Bot Registry | **Function URLs** | Free HTTPS endpoint without API Gateway |
| Bot Validator | **Lambda Layers** | Share validation logic across functions |
| Match Engine | **Step Functions** | Orchestrate multi-step workflows |
| Battle Worker | **Container Image** (10GB) | Heavy compute, custom runtime |
| Battle Worker | **10GB Memory / 6 vCPU** | Max compute power testing |
| Replay Streamer | **Response Streaming** | Stream large responses incrementally |
| Leaderboard | **DynamoDB Streams** | React to data changes |
| Match Queue | **SQS Event Source** | Batch processing, partial failures |
| Analytics | **Kinesis Consumer** | Real-time stream processing |
| Auth at Edge | **Lambda@Edge** | Run code at CDN edge locations |
| Notifications | **Destinations** | Async success/failure routing |
| Failed Matches | **Dead Letter Queue** | Handle poison messages |
| Event Fan-out | **EventBridge** | Decouple with event bus |
| Cold Start Test | **SnapStart** (Java) | JVM cold start optimization |
| Hot Path | **Provisioned Concurrency** | Eliminate cold starts |
| Observability | **Powertools + X-Ray** | Structured logging, tracing |
| Recursion Guard | **Recursive Detection** | Prevent infinite loops |
| Shared Storage | **EFS Mount** | Persist data across invocations |

---

## 1.4 Lambda Limits Reference (What We'll Test)

| Limit | Value | How We Test It |
|-------|-------|----------------|
| Max memory | 10,240 MB | Battle Worker with large game state |
| Max timeout | 15 minutes | Long tournament simulations |
| Max payload (sync) | 6 MB | Large bot strategy uploads |
| Max payload (async) | 256 KB | Event-driven match triggers |
| Max response (streaming) | 20 MB | Replay streaming |
| Max /tmp storage | 10 GB | Cache game assets |
| Max layers | 5 per function | Shared libraries |
| Max layer size | 250 MB (unzipped) | ML model in layer |
| Max container image | 10 GB | Container-based worker |
| Max concurrency (account) | 1000 (default) | Load test matchmaking |
| Max burst concurrency | 3000 (region-dependent) | Flash tournament |
| Cold start (zip) | 100-500ms | Measure and compare |
| Cold start (container) | 1-5s | Measure and compare |
| Max environment vars | 4 KB total | Config management |
| Max function URL payload | 6 MB | Large requests |

---

## 1.5 Game Design (Simple but Exercises Lambda)

### Bot Strategy Schema
```json
{
  "name": "CounterPuncher",
  "author": "player1",
  "version": 1,
  "strategy": {
    "rules": [
      {"if": "opponent_last_move == ATTACK", "then": "DEFEND", "priority": 1},
      {"if": "my_health < 30", "then": "HEAL", "priority": 2},
      {"if": "opponent_health < 20", "then": "ATTACK", "priority": 3},
      {"default": "ATTACK"}
    ]
  }
}
```

### Battle Mechanics
```
Moves: ATTACK (deal 10-25 dmg), DEFEND (block 50-80%), HEAL (+15 HP), COUNTER (risky 2x)

Rock-Paper-Scissors element:
  ATTACK beats HEAL (interrupt)
  DEFEND beats ATTACK (reflect 5 dmg)
  COUNTER beats DEFEND (bypass, deal 2x)
  HEAL beats nothing but recovers HP

Each bot starts with 100 HP.
Match = 100 rounds or until one bot reaches 0 HP.
```

---

## 1.6 AWS Services Used (Minimal Cost)

```
┌─────────────────────────────────────────────────────┐
│  Service                │ Purpose          │ Cost    │
├─────────────────────────┼──────────────────┼─────────┤
│  Lambda                 │ ALL compute      │ Free*   │
│  DynamoDB               │ Bot/Score store  │ Free*   │
│  SQS                    │ Match queue      │ Free*   │
│  SNS                    │ Notifications    │ Free*   │
│  S3                     │ Replay storage   │ Free*   │
│  Step Functions         │ Orchestration    │ Free*   │
│  EventBridge            │ Event routing    │ Free*   │
│  CloudWatch             │ Logs/Metrics     │ Free*   │
│  X-Ray                  │ Tracing          │ Free*   │
│  Kinesis (optional)     │ Analytics stream │ ~$0.36  │
│  API Gateway WebSocket  │ Real-time        │ Free*   │
│  CloudFront             │ CDN + Edge       │ Free*   │
│  EFS (optional)         │ Shared storage   │ ~$0.30  │
├─────────────────────────┼──────────────────┼─────────┤
│  TOTAL                  │                  │ $0-1/mo │
└─────────────────────────┴──────────────────┴─────────┘
* Within AWS Free Tier limits
```

---

## 1.7 Project Phases

```
Phase 1: Foundation (Part 2)
  → Bot Registry with Function URLs
  → DynamoDB table design
  → Lambda Layers for shared code
  → Container-based Battle Worker

Phase 2: Event-Driven (Part 3)
  → SQS match queue + batch processing
  → DynamoDB Streams → Leaderboard
  → EventBridge for fan-out
  → SNS notifications
  → S3 event triggers

Phase 3: Advanced (Part 4)
  → Step Functions tournament orchestration
  → Response Streaming for replays
  → Lambda@Edge for auth
  → Recursive invocation patterns
  → Destinations for async flows

Phase 4: Performance (Part 5)
  → Cold start benchmarking
  → Memory/CPU tuning (128MB → 10GB)
  → Provisioned concurrency
  → SnapStart (Java comparison)
  → Concurrency limits and throttling

Phase 5: Production (Part 6)
  → Powertools (logging, tracing, metrics)
  → X-Ray distributed tracing
  → CloudWatch dashboards
  → Cost optimization strategies
  → Error handling patterns
```

---

## Next: [Part 2 - Core Implementation →](Part2_Core_Implementation.md)
