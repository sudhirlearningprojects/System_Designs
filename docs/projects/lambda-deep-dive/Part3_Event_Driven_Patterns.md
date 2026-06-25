# Part 3: Event-Driven Patterns

## 3.1 SQS Event Source Mapping (Match Queue)

**Concept**: Lambda polls SQS, processes messages in batches, supports partial batch failure reporting.

```python
# functions/match_processor/handler.py
"""
Match Processor - Consumes match requests from SQS.

Lambda features demonstrated:
- SQS event source mapping
- Batch processing (up to 10 messages)
- Partial batch failure reporting
- Dead Letter Queue integration
- Reserved concurrency (prevent overwhelming downstream)
"""
import json
import boto3

lambda_client = boto3.client('lambda')


def handler(event, context):
    """Process batch of match requests from SQS."""
    batch_item_failures = []

    for record in event['Records']:
        try:
            body = json.loads(record['body'])
            process_match_request(body)
        except Exception as e:
            # Report this specific message as failed (will retry)
            batch_item_failures.append({
                'itemIdentifier': record['messageId']
            })
            print(f"Failed to process {record['messageId']}: {e}")

    # Partial batch failure - only failed messages return to queue
    return {'batchItemFailures': batch_item_failures}


def process_match_request(body):
    """Invoke battle worker for this match."""
    lambda_client.invoke(
        FunctionName='battle-worker',
        InvocationType='Event',  # Async - don't wait
        Payload=json.dumps({
            'match_id': body['match_id'],
            'bot1_id': body['bot1_id'],
            'bot2_id': body['bot2_id'],
            'rounds': body.get('rounds', 100)
        })
    )
```

### Configure SQS Trigger

```bash
# Create event source mapping
aws lambda create-event-source-mapping \
  --function-name match-processor \
  --event-source-arn arn:aws:sqs:us-east-1:123456789:match-queue \
  --batch-size 10 \
  --maximum-batching-window-in-seconds 5 \
  --function-response-types ReportBatchItemFailures

# Set reserved concurrency (prevent runaway scaling)
aws lambda put-function-concurrency \
  --function-name match-processor \
  --reserved-concurrent-executions 5
```

### Sending Match Requests to Queue

```python
# In bot_registry or a matchmaking function
import boto3
import json
import uuid

sqs = boto3.client('sqs')
QUEUE_URL = 'https://sqs.us-east-1.amazonaws.com/123456789/match-queue'

def queue_match(bot1_id, bot2_id, rounds=100):
    """Submit a match to the queue."""
    match_id = f"match-{uuid.uuid4().hex[:8]}"

    sqs.send_message(
        QueueUrl=QUEUE_URL,
        MessageBody=json.dumps({
            'match_id': match_id,
            'bot1_id': bot1_id,
            'bot2_id': bot2_id,
            'rounds': rounds
        }),
        MessageGroupId=match_id,  # FIFO queue: ensures ordering
        MessageDeduplicationId=match_id  # Prevent duplicate matches
    )
    return match_id
```

---

## 3.2 DynamoDB Streams (Leaderboard Updater)

**Concept**: React to DynamoDB changes in real-time. When a bot's ELO changes, recalculate leaderboard.

```python
# functions/leaderboard_updater/handler.py
"""
Leaderboard Updater - Triggered by DynamoDB Streams.

Lambda features demonstrated:
- DynamoDB Streams event source
- Stream record parsing (INSERT, MODIFY, REMOVE)
- Filtering (only process relevant changes)
- Batch processing of stream records
"""
import json
import boto3
from boto3.dynamodb.types import TypeDeserializer

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')
deserializer = TypeDeserializer()


def handler(event, context):
    """Process DynamoDB stream records."""
    for record in event['Records']:
        # Only care about MODIFY events on BOT items
        if record['eventName'] != 'MODIFY':
            continue

        new_image = deserialize_record(record['dynamodb']['NewImage'])
        old_image = deserialize_record(record['dynamodb']['OldImage'])

        # Only process if it's a bot and ELO changed
        if not new_image.get('PK', '').startswith('BOT#'):
            continue

        old_elo = int(old_image.get('elo_rating', 0))
        new_elo = int(new_image.get('elo_rating', 0))

        if old_elo != new_elo:
            update_leaderboard(
                bot_id=new_image['bot_id'],
                bot_name=new_image['name'],
                new_elo=new_elo
            )

    return {'statusCode': 200}


def update_leaderboard(bot_id, bot_name, new_elo):
    """Update the leaderboard entry for this bot."""
    # Use zero-padded ELO for sort order (descending)
    padded_elo = f"{9999 - new_elo:04d}"

    table.put_item(Item={
        'PK': 'LEADERBOARD',
        'SK': f"RANK#{padded_elo}#{bot_id}",
        'bot_id': bot_id,
        'bot_name': bot_name,
        'elo_rating': new_elo
    })


def deserialize_record(record):
    """Convert DynamoDB JSON to regular Python dict."""
    return {k: deserializer.deserialize(v) for k, v in record.items()}
```

### Configure DynamoDB Stream Trigger

```bash
# Get stream ARN
STREAM_ARN=$(aws dynamodb describe-table --table-name BotArena \
  --query 'Table.LatestStreamArn' --output text)

# Create event source mapping with filter
aws lambda create-event-source-mapping \
  --function-name leaderboard-updater \
  --event-source-arn $STREAM_ARN \
  --starting-position LATEST \
  --batch-size 100 \
  --maximum-batching-window-in-seconds 10 \
  --filter-criteria '{
    "Filters": [
      {"Pattern": "{\"dynamodb\":{\"NewImage\":{\"PK\":{\"S\":[{\"prefix\":\"BOT#\"}]}}}}"}
    ]
  }'
```

**Key Learning**: Event source filtering reduces invocations (and cost) by letting Lambda skip irrelevant records before your code runs.

---

## 3.3 EventBridge (Match Events Fan-Out)

**Concept**: Decouple event producers from consumers. Battle Worker emits events; multiple consumers react independently.

```python
# functions/event_handlers/match_notifier.py
"""
Match Notifier - Triggered by EventBridge when match completes.

Lambda features demonstrated:
- EventBridge rule trigger
- SNS integration for notifications
- Event pattern matching
"""
import json
import boto3
import os

sns = boto3.client('sns')
TOPIC_ARN = os.environ['NOTIFICATION_TOPIC_ARN']


def handler(event, context):
    """Notify players when their bot's match completes."""
    detail = event['detail']
    match_id = detail['match_id']
    winner = detail['winner']

    message = (
        f"🏆 Match {match_id} completed!\n"
        f"Winner: {winner}\n"
        f"Rounds: {detail['rounds_played']}\n"
        f"Final HP: {detail['final_hp']}"
    )

    sns.publish(
        TopicArn=TOPIC_ARN,
        Subject=f"Match Result: {match_id}",
        Message=message,
        MessageAttributes={
            'event_type': {'DataType': 'String', 'StringValue': 'match.completed'},
            'winner': {'DataType': 'String', 'StringValue': winner}
        }
    )

    return {'statusCode': 200}
```

```python
# functions/event_handlers/stats_aggregator.py
"""
Stats Aggregator - Also triggered by EventBridge match events.
Demonstrates multiple consumers on same event.
"""
import json
import boto3

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')


def handler(event, context):
    """Update bot statistics after match."""
    detail = event['detail']
    winner = detail['winner']

    # Update win/loss counters for both bots
    # (bot IDs would be in the event detail)
    for bot_id in [detail.get('bot1_id'), detail.get('bot2_id')]:
        if not bot_id:
            continue

        is_winner = (bot_id == winner)
        table.update_item(
            Key={'PK': f'BOT#{bot_id}', 'SK': 'STATS'},
            UpdateExpression='ADD total_matches :one, wins :w, losses :l',
            ExpressionAttributeValues={
                ':one': 1,
                ':w': 1 if is_winner else 0,
                ':l': 0 if is_winner else 1
            }
        )
```

### EventBridge Rules

```bash
# Rule: Match completed → Notify
aws events put-rule \
  --name match-completed-notify \
  --event-pattern '{
    "source": ["bot-arena"],
    "detail-type": ["match.completed"]
  }'

aws events put-targets \
  --rule match-completed-notify \
  --targets '[
    {"Id":"notifier","Arn":"arn:aws:lambda:us-east-1:123456789:function:match-notifier"},
    {"Id":"stats","Arn":"arn:aws:lambda:us-east-1:123456789:function:stats-aggregator"}
  ]'

# Rule: Match started (for monitoring)
aws events put-rule \
  --name match-started \
  --event-pattern '{
    "source": ["bot-arena"],
    "detail-type": ["match.started"]
  }'
```

---

## 3.4 S3 Event Notifications (Replay Processing)

**Concept**: Trigger Lambda when objects are created in S3.

```python
# functions/replay_indexer/handler.py
"""
Replay Indexer - Triggered when replay JSON is written to S3.

Lambda features demonstrated:
- S3 event trigger (s3:ObjectCreated:*)
- Reading S3 objects in Lambda
- Cross-service coordination
"""
import json
import boto3
import urllib.parse

s3 = boto3.client('s3')
dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')


def handler(event, context):
    """Index new replay files for quick lookup."""
    for record in event['Records']:
        bucket = record['s3']['bucket']['name']
        key = urllib.parse.unquote_plus(record['s3']['object']['key'])

        # Read replay
        response = s3.get_object(Bucket=bucket, Key=key)
        replay = json.loads(response['Body'].read())

        # Index in DynamoDB for fast lookup
        match_id = replay['match_id']
        rounds_played = len(replay['rounds'])
        winner = replay['winner']

        table.update_item(
            Key={'PK': f'MATCH#{match_id}', 'SK': 'METADATA'},
            UpdateExpression='SET replay_key = :k, rounds_played = :r',
            ExpressionAttributeValues={
                ':k': key,
                ':r': rounds_played
            }
        )

    return {'processed': len(event['Records'])}
```

---

## 3.5 Lambda Destinations (Async Success/Failure Routing)

**Concept**: Route async Lambda results to different targets based on success or failure — without code changes.

```bash
# Configure destinations for battle-worker
# On SUCCESS → send result to EventBridge
aws lambda put-function-event-invoke-config \
  --function-name battle-worker \
  --destination-config '{
    "OnSuccess": {
      "Destination": "arn:aws:events:us-east-1:123456789:event-bus/default"
    },
    "OnFailure": {
      "Destination": "arn:aws:sqs:us-east-1:123456789:match-dlq"
    }
  }' \
  --maximum-retry-attempts 2 \
  --maximum-event-age-in-seconds 3600
```

```
┌────────────────┐
│ Match Request  │
│ (Async invoke) │
└───────┬────────┘
        │
        ▼
┌────────────────┐
│ Battle Worker  │
│   (Lambda)     │
└───┬────────┬───┘
    │        │
 SUCCESS   FAILURE
    │        │
    ▼        ▼
┌────────┐ ┌────────┐
│ Event  │ │  DLQ   │
│ Bridge │ │ (SQS)  │
└────────┘ └────────┘
    │
    ├──► Notifier
    ├──► Stats Aggregator
    └──► Replay Indexer
```

**Key Learning**: Destinations replace the need for try/catch + manual routing. Lambda handles it automatically for async invocations.

---

## 3.6 SNS Fan-Out Pattern

```python
# functions/tournament_starter/handler.py
"""
Tournament Starter - Publishes to SNS to fan out match creation.

Lambda features demonstrated:
- SNS publish with message attributes
- Fan-out pattern (1 message → N subscribers)
- Message filtering on subscriber side
"""
import json
import boto3
import itertools

sns = boto3.client('sns')
TOPIC_ARN = 'arn:aws:sns:us-east-1:123456789:tournament-events'


def handler(event, context):
    """Start a round-robin tournament between N bots."""
    bot_ids = event['bot_ids']
    tournament_id = event['tournament_id']

    # Generate all pairings
    pairings = list(itertools.combinations(bot_ids, 2))

    for i, (bot1, bot2) in enumerate(pairings):
        sns.publish(
            TopicArn=TOPIC_ARN,
            Message=json.dumps({
                'tournament_id': tournament_id,
                'match_number': i + 1,
                'bot1_id': bot1,
                'bot2_id': bot2,
                'rounds': 50
            }),
            MessageAttributes={
                'event_type': {'DataType': 'String', 'StringValue': 'tournament.match'},
                'priority': {'DataType': 'String', 'StringValue': 'high'}
            }
        )

    return {'tournament_id': tournament_id, 'total_matches': len(pairings)}
```

---

## 3.7 Event-Driven Architecture Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    Event Flow Diagram                             │
│                                                                   │
│  [User] ──POST /matches──► [Bot Registry] ──► [SQS Queue]       │
│                                                    │              │
│                                                    ▼              │
│                                            [Match Processor]      │
│                                            (SQS event source)     │
│                                                    │              │
│                                              invoke(async)        │
│                                                    │              │
│                                                    ▼              │
│                                            [Battle Worker]        │
│                                            (Container, 10GB)      │
│                                              │         │          │
│                                           success   failure       │
│                                              │         │          │
│                              ┌────────────────┘         ▼         │
│                              ▼                      [DLQ]         │
│                       [EventBridge]                               │
│                         │    │    │                               │
│                         ▼    ▼    ▼                               │
│                   [Notify][Stats][Index]                          │
│                                                                   │
│  Meanwhile:                                                       │
│  [DynamoDB] ──stream──► [Leaderboard Updater]                    │
│  [S3 replay] ──event──► [Replay Indexer]                         │
└─────────────────────────────────────────────────────────────────┘

Total Lambda functions: 7
Total AWS services integrated: 7 (DDB, SQS, SNS, S3, EventBridge, Lambda, CloudWatch)
Monthly cost: $0 (Free Tier)
```

---

## 3.8 Key Learnings

| Pattern | Feature | Insight |
|---------|---------|---------|
| **SQS → Lambda** | Event source mapping | Lambda auto-scales pollers; use `ReportBatchItemFailures` |
| **DDB Streams → Lambda** | Stream processing | Filter patterns reduce invocations by 90%+ |
| **EventBridge → Lambda** | Fan-out | Multiple targets per rule; content-based filtering |
| **S3 → Lambda** | Object triggers | Only `s3:ObjectCreated:*` (not modify); prefix filters |
| **SNS → Lambda** | Pub/Sub | Message attribute filtering at subscription level |
| **Destinations** | Async routing | No code needed for success/failure routing |
| **DLQ** | Error handling | Messages retry 3x before DLQ; monitor DLQ depth |

---

## Next: [Part 4 - Advanced Patterns →](Part4_Advanced_Patterns.md)
