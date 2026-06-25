# Part 6: Observability & Cost

## 6.1 Lambda Powertools (Structured Observability)

**Concept**: AWS Lambda Powertools provides structured logging, tracing, and metrics with minimal code.

```python
# functions/bot_registry/handler_with_powertools.py
"""
Bot Registry with full observability via Lambda Powertools.

Lambda features demonstrated:
- Structured JSON logging
- X-Ray tracing with custom subsegments
- CloudWatch custom metrics
- Correlation IDs across services
- Idempotency decorator
"""
import json
import boto3
from aws_lambda_powertools import Logger, Tracer, Metrics
from aws_lambda_powertools.event_handler import LambdaFunctionUrlResolver
from aws_lambda_powertools.metrics import MetricUnit
from aws_lambda_powertools.utilities.idempotency import (
    DynamoDBPersistenceLayer, idempotent
)

# Initialize Powertools
logger = Logger(service="bot-arena", log_uncaught_exceptions=True)
tracer = Tracer(service="bot-arena")
metrics = Metrics(service="bot-arena", namespace="BotArena")
app = LambdaFunctionUrlResolver()

# Idempotency store
persistence = DynamoDBPersistenceLayer(table_name="BotArenaIdempotency")

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')


@app.post("/bots")
@tracer.capture_method
def create_bot():
    """Create bot with full observability."""
    body = app.current_event.json_body

    logger.info("Creating bot", extra={
        "bot_name": body.get('name'),
        "author": body.get('author')
    })

    # Custom metric
    metrics.add_metric(name="BotCreated", unit=MetricUnit.Count, value=1)
    metrics.add_dimension(name="Author", value=body.get('author', 'unknown'))

    # Traced DynamoDB call
    with tracer.provider.in_subsegment("dynamodb_put") as subsegment:
        subsegment.put_annotation("bot_name", body.get('name'))
        bot_id = f"bot-{body['name'][:8].lower()}"
        table.put_item(Item={
            'PK': f'BOT#{bot_id}', 'SK': 'METADATA',
            'bot_id': bot_id, 'name': body['name'],
            'author': body['author'], 'strategy': body['strategy'],
            'elo_rating': 1000
        })

    logger.info("Bot created successfully", extra={"bot_id": bot_id})
    return {"bot_id": bot_id, "name": body['name']}


@app.get("/bots")
@tracer.capture_method
def list_bots():
    """List bots with tracing."""
    metrics.add_metric(name="BotListRequests", unit=MetricUnit.Count, value=1)

    result = table.scan(
        FilterExpression='SK = :sk',
        ExpressionAttributeValues={':sk': 'METADATA'},
        Limit=20
    )
    bots = [{'bot_id': i['bot_id'], 'name': i['name'], 'elo': int(i['elo_rating'])}
            for i in result.get('Items', []) if i['PK'].startswith('BOT#')]

    return {"bots": bots, "count": len(bots)}


@app.get("/leaderboard")
@tracer.capture_method
def get_leaderboard():
    """Get leaderboard with caching metrics."""
    metrics.add_metric(name="LeaderboardViews", unit=MetricUnit.Count, value=1)

    result = table.query(
        KeyConditionExpression='PK = :pk',
        ExpressionAttributeValues={':pk': 'LEADERBOARD'},
        Limit=10,
        ScanIndexForward=True  # Ascending SK = descending ELO
    )

    return {"leaderboard": result.get('Items', [])}


@logger.inject_lambda_context(log_event=True)
@tracer.capture_lambda_handler
@metrics.log_metrics(capture_cold_start_metric=True)
def handler(event, context):
    """Main handler with all Powertools decorators."""
    return app.resolve(event, context)
```

### Install Powertools Layer

```bash
# Use AWS-managed Powertools layer (no packaging needed!)
aws lambda update-function-configuration \
  --function-name bot-registry \
  --layers arn:aws:lambda:us-east-1:017000801446:layer:AWSLambdaPowertoolsPythonV2:51
```

### Log Output (Structured JSON)

```json
{
  "level": "INFO",
  "location": "create_bot:42",
  "message": "Creating bot",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "service": "bot-arena",
  "cold_start": false,
  "function_name": "bot-registry",
  "function_memory_size": 256,
  "function_request_id": "abc-123-xyz",
  "bot_name": "Berserker",
  "author": "player1",
  "xray_trace_id": "1-abc-def"
}
```

---

## 6.2 X-Ray Distributed Tracing

```bash
# Enable active tracing
aws lambda update-function-configuration \
  --function-name bot-registry \
  --tracing-config Mode=Active

aws lambda update-function-configuration \
  --function-name battle-worker \
  --tracing-config Mode=Active

aws lambda update-function-configuration \
  --function-name match-processor \
  --tracing-config Mode=Active
```

### Trace Visualization

```
[Client] → [Bot Registry] → [DynamoDB]
              200ms total
              ├── Init: 120ms (cold start)
              ├── Handler: 75ms
              │   ├── validate_input: 2ms
              │   ├── dynamodb_put: 45ms
              │   └── response_build: 1ms
              └── Overhead: 5ms

[SQS] → [Match Processor] → [Battle Worker] → [DynamoDB + S3 + EventBridge]
          800ms total
          ├── SQS Poll: 50ms
          ├── Parse messages: 2ms
          ├── Invoke battle-worker (async): 15ms
          └── Battle Worker execution:
              ├── Load bots from DDB: 30ms
              ├── Execute 100 rounds: 500ms
              ├── Write replay to S3: 80ms
              ├── Update DDB scores: 40ms
              └── Emit EventBridge: 10ms
```

---

## 6.3 CloudWatch Dashboard

```python
# scripts/create_dashboard.py
"""Create CloudWatch dashboard for Bot Arena monitoring."""
import boto3
import json

cw = boto3.client('cloudwatch')

dashboard_body = {
    "widgets": [
        {
            "type": "metric",
            "properties": {
                "title": "Lambda Invocations",
                "metrics": [
                    ["AWS/Lambda", "Invocations", "FunctionName", "bot-registry"],
                    ["AWS/Lambda", "Invocations", "FunctionName", "battle-worker"],
                    ["AWS/Lambda", "Invocations", "FunctionName", "match-processor"]
                ],
                "period": 60, "stat": "Sum"
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "Cold Starts",
                "metrics": [
                    ["BotArena", "ColdStart", "service", "bot-arena"]
                ],
                "period": 300, "stat": "Sum"
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "Duration (P95)",
                "metrics": [
                    ["AWS/Lambda", "Duration", "FunctionName", "bot-registry", {"stat": "p95"}],
                    ["AWS/Lambda", "Duration", "FunctionName", "battle-worker", {"stat": "p95"}]
                ],
                "period": 60
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "Errors & Throttles",
                "metrics": [
                    ["AWS/Lambda", "Errors", "FunctionName", "bot-registry"],
                    ["AWS/Lambda", "Errors", "FunctionName", "battle-worker"],
                    ["AWS/Lambda", "Throttles", "FunctionName", "bot-registry"]
                ],
                "period": 60, "stat": "Sum"
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "Game Metrics",
                "metrics": [
                    ["BotArena", "BotCreated", "service", "bot-arena"],
                    ["BotArena", "MatchesCompleted", "service", "bot-arena"],
                    ["BotArena", "LeaderboardViews", "service", "bot-arena"]
                ],
                "period": 300, "stat": "Sum"
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "SQS - Match Queue Depth",
                "metrics": [
                    ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", "match-queue"],
                    ["AWS/SQS", "ApproximateNumberOfMessagesNotVisible", "QueueName", "match-queue"]
                ],
                "period": 60, "stat": "Average"
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "DLQ Messages (Failures)",
                "metrics": [
                    ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", "match-dlq"]
                ],
                "period": 60, "stat": "Sum"
            }
        },
        {
            "type": "metric",
            "properties": {
                "title": "Concurrent Executions",
                "metrics": [
                    ["AWS/Lambda", "ConcurrentExecutions", "FunctionName", "bot-registry"],
                    ["AWS/Lambda", "ConcurrentExecutions", "FunctionName", "battle-worker"]
                ],
                "period": 60, "stat": "Maximum"
            }
        }
    ]
}

cw.put_dashboard(
    DashboardName='BotArena',
    DashboardBody=json.dumps(dashboard_body)
)
print("Dashboard created: https://console.aws.amazon.com/cloudwatch/home#dashboards:name=BotArena")
```

---

## 6.4 CloudWatch Alarms

```bash
# Alarm: High error rate
aws cloudwatch put-metric-alarm \
  --alarm-name "BotArena-HighErrors" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --dimensions Name=FunctionName,Value=battle-worker \
  --statistic Sum \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --alarm-actions arn:aws:sns:us-east-1:123456789:alerts

# Alarm: DLQ not empty (matches failing permanently)
aws cloudwatch put-metric-alarm \
  --alarm-name "BotArena-DLQNotEmpty" \
  --metric-name ApproximateNumberOfMessagesVisible \
  --namespace AWS/SQS \
  --dimensions Name=QueueName,Value=match-dlq \
  --statistic Sum \
  --period 60 \
  --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --evaluation-periods 1

# Alarm: Throttling detected
aws cloudwatch put-metric-alarm \
  --alarm-name "BotArena-Throttled" \
  --metric-name Throttles \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 60 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1
```

---

## 6.5 Cost Optimization Strategies

### Strategy 1: ARM64 (Graviton2)

```bash
# 20% cheaper, often same or better performance for Python
aws lambda update-function-configuration \
  --function-name bot-registry \
  --architectures arm64
```

### Strategy 2: Right-Size Memory

```
Rule of thumb:
- API handlers (I/O bound): 256-512 MB
- Data processing: 512-1024 MB
- Compute-heavy (battle): 1769 MB (1 vCPU) minimum
- ML inference: 3538-10240 MB

Use AWS Lambda Power Tuning tool:
https://github.com/alexcasalboni/aws-lambda-power-tuning
```

### Strategy 3: Minimize Duration

```python
# BAD: Initialize SDK inside handler (every invocation)
def handler(event, context):
    dynamodb = boto3.resource('dynamodb')  # Cold on every call!
    table = dynamodb.Table('BotArena')
    ...

# GOOD: Initialize outside handler (reused across warm invocations)
dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')

def handler(event, context):
    # table already initialized
    ...
```

### Strategy 4: Event Source Batching

```bash
# Process 10 SQS messages per invocation instead of 1
# Reduces invocations by 10x
aws lambda update-event-source-mapping \
  --uuid abc-123 \
  --batch-size 10 \
  --maximum-batching-window-in-seconds 5
```

### Strategy 5: Avoid API Gateway (Use Function URLs)

```
API Gateway REST API: $3.50 per million requests
API Gateway HTTP API: $1.00 per million requests
Lambda Function URL:  $0.00 (free!)

For this project: Function URLs save 100% on API costs
```

---

## 6.6 Complete Cost Breakdown

```
┌──────────────────────────────────────────────────────────────┐
│  Monthly Cost Estimate (moderate usage)                       │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  Lambda Compute:                                              │
│  • 200K invocations × 256MB × 200ms avg                     │
│  • GB-seconds: 200K × 0.25 × 0.2 = 10,000 GB-sec           │
│  • Free tier: 400,000 GB-sec                                 │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  Lambda Requests:                                             │
│  • 200K requests                                              │
│  • Free tier: 1,000,000 requests                             │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  DynamoDB:                                                    │
│  • ~1 GB storage, <25 WCU/RCU average                        │
│  • Free tier: 25 GB, 25 WCU, 25 RCU                         │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  SQS:                                                         │
│  • ~100K messages                                             │
│  • Free tier: 1,000,000 messages                             │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  S3:                                                          │
│  • ~500 MB replays                                            │
│  • Free tier: 5 GB                                           │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  Step Functions:                                              │
│  • ~2,000 state transitions                                   │
│  • Free tier: 4,000 state transitions                        │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  CloudWatch:                                                  │
│  • ~1 GB logs                                                 │
│  • Free tier: 5 GB ingestion + 5 GB storage                  │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  X-Ray:                                                       │
│  • ~10K traces                                                │
│  • Free tier: 100K traces                                    │
│  • Cost: $0.00 ✅                                            │
│                                                                │
│  ══════════════════════════════════════════════════            │
│  TOTAL MONTHLY COST: $0.00                                    │
│  ══════════════════════════════════════════════════            │
│                                                                │
│  If you exceed free tier (heavy load testing):                │
│  • Lambda: ~$0.50                                             │
│  • DynamoDB: ~$0.25                                           │
│  • Total: < $1.00/month                                       │
└──────────────────────────────────────────────────────────────┘
```

---

## 6.7 Cleanup Script

```bash
#!/bin/bash
# cleanup.sh - Remove all Bot Arena resources

echo "Cleaning up Bot Arena..."

# Delete Lambda functions
for fn in bot-registry battle-worker match-processor leaderboard-updater \
          replay-streamer replay-indexer match-notifier stats-aggregator; do
  aws lambda delete-function --function-name $fn 2>/dev/null
done

# Delete Layer
aws lambda delete-layer-version --layer-name arena-common --version-number 1

# Delete DynamoDB tables
aws dynamodb delete-table --table-name BotArena
aws dynamodb delete-table --table-name BotArenaIdempotency 2>/dev/null

# Delete SQS queues
aws sqs delete-queue --queue-url https://sqs.us-east-1.amazonaws.com/123456789/match-queue
aws sqs delete-queue --queue-url https://sqs.us-east-1.amazonaws.com/123456789/match-dlq

# Delete S3 bucket
aws s3 rb s3://bot-arena-replays --force

# Delete EventBridge rules
aws events remove-targets --rule match-completed-notify --ids notifier stats
aws events delete-rule --name match-completed-notify

# Delete Step Functions
aws stepfunctions delete-state-machine \
  --state-machine-arn arn:aws:states:us-east-1:123456789:stateMachine:tournament

# Delete CloudWatch dashboard
aws cloudwatch delete-dashboards --dashboard-names BotArena

echo "Cleanup complete!"
```

---

## 6.8 Final Learning Summary

### Lambda Features Exercised in This Project

| # | Feature | Where Used | Mastery Level |
|---|---------|-----------|:-------------:|
| 1 | Function URLs | Bot Registry API | ⭐⭐⭐ |
| 2 | Lambda Layers | Shared arena-common | ⭐⭐⭐ |
| 3 | Container Images | Battle Worker | ⭐⭐⭐ |
| 4 | 10GB Memory / 6 vCPU | Battle Worker | ⭐⭐ |
| 5 | 15-min Timeout | Long tournaments | ⭐⭐ |
| 6 | Response Streaming | Replay Streamer | ⭐⭐⭐ |
| 7 | SQS Event Source | Match Processor | ⭐⭐⭐ |
| 8 | DynamoDB Streams | Leaderboard Updater | ⭐⭐⭐ |
| 9 | EventBridge | Match event fan-out | ⭐⭐⭐ |
| 10 | S3 Event Trigger | Replay Indexer | ⭐⭐ |
| 11 | Step Functions | Tournament orchestration | ⭐⭐⭐ |
| 12 | Lambda@Edge | Edge auth | ⭐⭐ |
| 13 | Destinations | Async routing | ⭐⭐ |
| 14 | Dead Letter Queue | Failed matches | ⭐⭐⭐ |
| 15 | Provisioned Concurrency | Hot path (optional) | ⭐⭐ |
| 16 | SnapStart | Java comparison | ⭐⭐ |
| 17 | Recursive Detection | Safety net | ⭐ |
| 18 | EFS Mount | Shared model storage | ⭐⭐ |
| 19 | Powertools | Logging/Tracing/Metrics | ⭐⭐⭐ |
| 20 | X-Ray | Distributed tracing | ⭐⭐⭐ |
| 21 | ARM64 (Graviton) | Cost optimization | ⭐⭐ |
| 22 | /tmp Storage (10GB) | Replay caching | ⭐⭐ |
| 23 | Partial Batch Failures | SQS error handling | ⭐⭐⭐ |
| 24 | Event Filtering | DDB Stream filters | ⭐⭐ |
| 25 | Idempotency | Powertools decorator | ⭐⭐⭐ |

**Total Lambda capabilities explored: 25**
**Total AWS services integrated: 13**
**Total monthly cost: $0.00**

---

## ← [Back to README](README.md)
