# Part 4: Advanced Patterns

## 4.1 Step Functions (Tournament Orchestration)

**Concept**: Orchestrate complex multi-step workflows that exceed Lambda's 15-minute limit. A tournament with 100 bots = 4,950 matches — impossible in a single Lambda.

### State Machine Definition

```json
{
  "Comment": "Bot Arena Tournament - Round Robin",
  "StartAt": "GeneratePairings",
  "States": {
    "GeneratePairings": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789:function:generate-pairings",
      "Next": "ProcessMatches",
      "ResultPath": "$.pairings"
    },
    "ProcessMatches": {
      "Type": "Map",
      "ItemsPath": "$.pairings",
      "MaxConcurrency": 10,
      "Iterator": {
        "StartAt": "ExecuteMatch",
        "States": {
          "ExecuteMatch": {
            "Type": "Task",
            "Resource": "arn:aws:lambda:us-east-1:123456789:function:battle-worker",
            "TimeoutSeconds": 900,
            "Retry": [
              {
                "ErrorEquals": ["States.TaskFailed", "Lambda.ServiceException"],
                "IntervalSeconds": 5,
                "MaxAttempts": 2,
                "BackoffRate": 2.0
              }
            ],
            "Catch": [
              {
                "ErrorEquals": ["States.ALL"],
                "Next": "MatchFailed",
                "ResultPath": "$.error"
              }
            ],
            "Next": "MatchSucceeded"
          },
          "MatchFailed": {
            "Type": "Pass",
            "Result": {"status": "FAILED"},
            "End": true
          },
          "MatchSucceeded": {
            "Type": "Pass",
            "End": true
          }
        }
      },
      "Next": "CalculateStandings"
    },
    "CalculateStandings": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789:function:calculate-standings",
      "Next": "PublishResults"
    },
    "PublishResults": {
      "Type": "Task",
      "Resource": "arn:aws:states:::sns:publish",
      "Parameters": {
        "TopicArn": "arn:aws:sns:us-east-1:123456789:tournament-results",
        "Message.$": "$.standings"
      },
      "End": true
    }
  }
}
```

### Generate Pairings Lambda

```python
# functions/generate_pairings/handler.py
"""
Generates all match pairings for a tournament.
Step Functions Map state iterates over these.
"""
import itertools
import uuid


def handler(event, context):
    """Generate round-robin pairings."""
    bot_ids = event['bot_ids']
    tournament_id = event.get('tournament_id', f"tourney-{uuid.uuid4().hex[:8]}")

    pairings = []
    for bot1, bot2 in itertools.combinations(bot_ids, 2):
        pairings.append({
            'match_id': f"{tournament_id}-{uuid.uuid4().hex[:6]}",
            'bot1_id': bot1,
            'bot2_id': bot2,
            'rounds': event.get('rounds_per_match', 50),
            'tournament_id': tournament_id
        })

    return pairings  # Step Functions Map iterates over this
```

### Start Tournament

```bash
# Start execution
aws stepfunctions start-execution \
  --state-machine-arn arn:aws:states:us-east-1:123456789:stateMachine:tournament \
  --input '{
    "bot_ids": ["bot-001","bot-002","bot-003","bot-004","bot-005"],
    "rounds_per_match": 50,
    "tournament_id": "tourney-weekly-01"
  }'
```

### Step Functions Features Demonstrated

| Feature | How Used | Learning |
|---------|----------|----------|
| **Map state** | Parallel match execution | Process arrays with concurrency control |
| **MaxConcurrency** | Limit to 10 parallel | Prevent Lambda throttling |
| **Retry** | Auto-retry failed matches | Exponential backoff built-in |
| **Catch** | Handle match errors | Error routing without try/catch |
| **Task timeout** | 900s per match | Individual step timeouts |
| **SDK integrations** | Direct SNS publish | Call AWS services without Lambda |
| **Express vs Standard** | Standard (long-running) | Express for <5 min, cheaper |

---

## 4.2 Response Streaming (Live Replay)

**Concept**: Stream match replays to clients in real-time instead of buffering the entire response. Lambda sends data chunks as they're generated.

```python
# functions/replay_streamer/handler.py
"""
Replay Streamer - Streams match replay round by round.

Lambda features demonstrated:
- Response streaming (awslambda.http_stream)
- Chunked transfer encoding
- Server-Sent Events (SSE) format
- Large response support (up to 20MB streamed vs 6MB buffered)
"""
import json
import boto3
import time


def handler(event, awslambda):
    """Stream replay data as Server-Sent Events."""

    # Get match ID from path
    match_id = event.get('rawPath', '').split('/replay/')[-1]

    # Set response headers for SSE
    response_metadata = {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive',
            'X-Match-Id': match_id
        }
    }

    # Start streaming
    response_stream = awslambda.response_stream
    response_stream.write_metadata(response_metadata)

    # Load replay from S3
    s3 = boto3.client('s3')
    try:
        obj = s3.get_object(
            Bucket='bot-arena-replays',
            Key=f'replays/{match_id}.json'
        )
        replay = json.loads(obj['Body'].read())
    except Exception as e:
        response_stream.write(f"event: error\ndata: {json.dumps({'error': str(e)})}\n\n")
        response_stream.close()
        return

    # Stream each round with a small delay (simulates real-time playback)
    response_stream.write(f"event: match_start\ndata: {json.dumps({'match_id': match_id, 'total_rounds': len(replay['rounds'])})}\n\n")

    for round_data in replay['rounds']:
        chunk = f"event: round\ndata: {json.dumps(round_data)}\n\n"
        response_stream.write(chunk)
        time.sleep(0.05)  # 50ms between rounds for playback effect

    # Send final result
    response_stream.write(f"event: match_end\ndata: {json.dumps({'winner': replay['winner']})}\n\n")
    response_stream.close()
```

### Deploy with Streaming Enabled

```bash
# Create function with RESPONSE_STREAM invoke mode
aws lambda create-function \
  --function-name replay-streamer \
  --runtime python3.11 \
  --handler handler.handler \
  --zip-file fileb://replay_streamer.zip \
  --role arn:aws:iam::123456789:role/LambdaRole \
  --timeout 300 \
  --memory-size 512

# Create Function URL with streaming
aws lambda create-function-url-config \
  --function-name replay-streamer \
  --auth-type NONE \
  --invoke-mode RESPONSE_STREAM  # Key: enables streaming!
```

### Client-Side Consumption

```javascript
// Browser: consume SSE stream from Lambda
const eventSource = new EventSource(
  'https://xyz.lambda-url.us-east-1.on.aws/replay/match-abc123'
);

eventSource.addEventListener('match_start', (e) => {
  const data = JSON.parse(e.data);
  console.log(`Match started! ${data.total_rounds} rounds`);
});

eventSource.addEventListener('round', (e) => {
  const round = JSON.parse(e.data);
  // Animate the battle
  updateBattleUI(round);
});

eventSource.addEventListener('match_end', (e) => {
  const result = JSON.parse(e.data);
  console.log(`Winner: ${result.winner}`);
  eventSource.close();
});
```

### Streaming vs Buffered Comparison

| Aspect | Buffered (default) | Streaming |
|--------|-------------------|-----------|
| Max response | 6 MB | 20 MB |
| TTFB | After full execution | Immediate |
| Use case | JSON APIs | SSE, large files, real-time |
| Function URL | `BUFFERED` mode | `RESPONSE_STREAM` mode |
| API Gateway | Supported | NOT supported (Function URL only) |
| Cost | Same | Same |

---

## 4.3 Lambda@Edge (Authentication)

**Concept**: Run Lambda at CloudFront edge locations for low-latency auth, URL rewriting, or A/B testing.

```python
# functions/edge_auth/handler.py
"""
Edge Authenticator - Runs at CloudFront edge.

Lambda features demonstrated:
- Lambda@Edge (viewer-request event)
- CloudFront integration
- JWT validation at edge (sub-ms latency)
- Must be deployed in us-east-1
- Max 5 second timeout, 128MB memory (viewer events)
"""
import json
import base64
import hmac
import hashlib
import time

# Hardcoded secret for demo (use Secrets Manager in prod)
SECRET = 'bot-arena-secret-key-2024'


def handler(event, context):
    """Validate auth token at CloudFront edge."""
    request = event['Records'][0]['cf']['request']
    headers = request.get('headers', {})

    # Extract token from Authorization header
    auth_header = headers.get('authorization', [{}])
    token = auth_header[0].get('value', '').replace('Bearer ', '') if auth_header else ''

    # Public paths (no auth needed)
    uri = request.get('uri', '')
    if uri in ['/', '/health', '/leaderboard']:
        return request  # Pass through

    # Validate token
    if not token or not validate_token(token):
        return {
            'status': '401',
            'statusDescription': 'Unauthorized',
            'headers': {
                'content-type': [{'value': 'application/json'}],
                'www-authenticate': [{'value': 'Bearer realm="bot-arena"'}]
            },
            'body': json.dumps({'error': 'Invalid or missing token'})
        }

    # Token valid - add player ID to header for downstream
    payload = decode_token(token)
    request['headers']['x-player-id'] = [{'key': 'X-Player-Id', 'value': payload.get('sub', '')}]

    return request


def validate_token(token):
    """Simple HMAC-based token validation."""
    try:
        parts = token.split('.')
        if len(parts) != 3:
            return False
        header_b64, payload_b64, signature = parts
        expected_sig = hmac.new(
            SECRET.encode(),
            f"{header_b64}.{payload_b64}".encode(),
            hashlib.sha256
        ).hexdigest()[:32]
        return hmac.compare_digest(signature, expected_sig)
    except:
        return False


def decode_token(token):
    """Decode token payload."""
    try:
        payload_b64 = token.split('.')[1]
        payload_b64 += '=' * (4 - len(payload_b64) % 4)
        return json.loads(base64.b64decode(payload_b64))
    except:
        return {}
```

### Lambda@Edge Constraints

| Constraint | Viewer Events | Origin Events |
|------------|:------------:|:------------:|
| Memory | 128 MB max | 10,240 MB |
| Timeout | 5 seconds | 30 seconds |
| Package size | 1 MB | 50 MB |
| Env variables | ❌ Not supported | ❌ Not supported |
| VPC | ❌ Not supported | ❌ Not supported |
| Layers | ❌ Not supported | ❌ Not supported |
| Region | us-east-1 only | us-east-1 only |
| Runtime | Node.js, Python | Node.js, Python |

---

## 4.4 Recursive Invocation Detection

**Concept**: Lambda can detect and stop recursive invocation loops that could cause runaway costs.

```python
# functions/recursive_demo/handler.py
"""
Demonstrates Lambda recursive invocation detection.

Scenario: A Lambda writes to SQS, which triggers the same Lambda.
AWS detects the loop after 16 recursive calls and stops it.

Lambda features demonstrated:
- Recursive loop detection
- _X_AMZN_TRACE_ID header tracking
- Automatic loop breaking (16 depth max)
"""
import json
import boto3
import os

sqs = boto3.client('sqs')
QUEUE_URL = os.environ['QUEUE_URL']


def handler(event, context):
    """
    Intentional recursive pattern for learning.
    In production, this would be a bug! Lambda detects and stops it.
    """
    # Check recursion depth (Lambda adds this header)
    depth = int(os.environ.get('_X_AMZN_LAMBDA_RECURSION_DEPTH', '0'))
    print(f"Recursion depth: {depth}")

    if depth >= 5:
        print("Stopping at depth 5 (self-imposed limit)")
        return {'stopped_at_depth': depth}

    # This would normally cause infinite recursion:
    # Lambda → SQS → Lambda → SQS → ...
    # AWS breaks it at depth 16 automatically
    for record in event.get('Records', []):
        data = json.loads(record['body'])
        data['depth'] = depth + 1

        sqs.send_message(
            QueueUrl=QUEUE_URL,
            MessageBody=json.dumps(data)
        )

    return {'processed': True, 'depth': depth}
```

### How AWS Detects Recursion

```
Lambda → SQS → Lambda → SQS → Lambda ...
   │                │                │
   └─ Trace ID propagated through chain
      AWS tracks depth via trace header
      At depth 16: invocation is DROPPED
      CloudWatch alarm fires
```

**Key Learning**: Enable recursive invocation detection in Lambda settings. Monitor CloudWatch metric `RecursiveInvocationsDropped`.

---

## 4.5 Lambda with EFS (Shared Persistent Storage)

**Concept**: Mount an EFS filesystem to Lambda for shared, persistent storage across invocations and functions.

```python
# functions/model_server/handler.py
"""
ML Model Server - Uses EFS to share large model files.

Lambda features demonstrated:
- EFS mount point (/mnt/models)
- Persistent storage across invocations (no re-download)
- Shared state between multiple Lambda functions
- VPC requirement for EFS access
"""
import json
import os
import pickle

MODEL_PATH = '/mnt/models/bot_evaluator.pkl'


def handler(event, context):
    """Evaluate bot strategy quality using ML model stored on EFS."""

    # First invocation: model loads from EFS (fast, no S3 download)
    # Subsequent invocations: model already in Lambda memory
    model = load_model()

    strategy = event.get('strategy', {})
    score = model.predict(strategy)

    return {
        'quality_score': score,
        'model_version': model.version,
        'model_path': MODEL_PATH
    }


def load_model():
    """Load model from EFS mount."""
    if os.path.exists(MODEL_PATH):
        with open(MODEL_PATH, 'rb') as f:
            return pickle.load(f)
    else:
        # Fallback: use simple heuristic
        return SimpleModel()


class SimpleModel:
    version = "1.0-heuristic"
    
    def predict(self, strategy):
        rules = strategy.get('rules', [])
        score = min(len(rules) * 20, 100)  # More rules = higher score
        return score
```

### EFS Configuration

```bash
# Lambda needs VPC access for EFS
aws lambda update-function-configuration \
  --function-name model-server \
  --vpc-config SubnetIds=subnet-abc123,SecurityGroupIds=sg-xyz789 \
  --file-system-configs '[{
    "Arn": "arn:aws:elasticfilesystem:us-east-1:123456789:access-point/fsap-abc123",
    "LocalMountPath": "/mnt/models"
  }]'
```

**Trade-off**: EFS adds cold start latency (~1-2s for VPC ENI attachment). Use only when shared persistent storage is required.

---

## 4.6 Lambda Destinations vs DLQ

```
┌─────────────────────────────────────────────────────────────┐
│  Destinations (newer, more flexible)                         │
│                                                               │
│  • Works for: Async invocations only                         │
│  • Routes: Both success AND failure                          │
│  • Targets: Lambda, SQS, SNS, EventBridge                   │
│  • Includes: Full request + response payload                 │
│  • Use for: Event-driven architectures                       │
│                                                               │
│  DLQ (older, simpler)                                        │
│                                                               │
│  • Works for: Async invocations + SQS triggers              │
│  • Routes: Failure only                                      │
│  • Targets: SQS, SNS only                                   │
│  • Includes: Original event payload only                     │
│  • Use for: Simple retry/alerting patterns                   │
└─────────────────────────────────────────────────────────────┘

Recommendation: Use Destinations for new code. DLQ for SQS event sources.
```

---

## 4.7 Key Learnings from Part 4

| Pattern | Capability | When to Use |
|---------|-----------|-------------|
| **Step Functions** | Multi-step orchestration | Workflows > 15 min, complex branching |
| **Response Streaming** | Chunked responses | Real-time UIs, large payloads, SSE |
| **Lambda@Edge** | Code at CDN | Auth, URL rewrite, A/B testing |
| **Recursive Detection** | Loop prevention | Safety net for event-driven chains |
| **EFS** | Shared filesystem | Large models, shared state |
| **Destinations** | Async routing | Success/failure handling without code |

---

## Next: [Part 5 - Performance & Limits →](Part5_Performance_and_Limits.md)
