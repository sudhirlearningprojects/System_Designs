# Part 2: Core Implementation

## 2.1 DynamoDB Table Design

```bash
# Single-table design for all game data
aws dynamodb create-table \
  --table-name BotArena \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes '[
    {"IndexName":"GSI1","KeySchema":[{"AttributeName":"GSI1PK","KeyType":"HASH"},{"AttributeName":"GSI1SK","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}}
  ]' \
  --billing-mode PAY_PER_REQUEST \
  --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES
```

### Access Patterns

| Access Pattern | PK | SK | GSI1PK | GSI1SK |
|----------------|----|----|--------|--------|
| Get bot by ID | `BOT#<id>` | `METADATA` | `AUTHOR#<name>` | `BOT#<id>` |
| List bots by author | — | — | `AUTHOR#<name>` | `BOT#*` |
| Get match by ID | `MATCH#<id>` | `METADATA` | `STATUS#PENDING` | `<timestamp>` |
| Get match rounds | `MATCH#<id>` | `ROUND#<n>` | — | — |
| Leaderboard | `LEADERBOARD` | `RANK#<padded>` | — | — |
| Bot stats | `BOT#<id>` | `STATS` | — | — |

---

## 2.2 Lambda Layer (Shared Code)

**Concept**: Layers let you share code/libraries across multiple functions without bundling them in each deployment package.

```python
# layer/python/arena_common/__init__.py
"""Shared code used by all Lambda functions in the Arena."""

# layer/python/arena_common/models.py
from dataclasses import dataclass, asdict
from typing import Optional
import uuid
import time

@dataclass
class Bot:
    bot_id: str
    name: str
    author: str
    strategy: dict
    version: int = 1
    elo_rating: int = 1000
    created_at: float = None

    def __post_init__(self):
        if not self.bot_id:
            self.bot_id = f"bot-{uuid.uuid4().hex[:8]}"
        if not self.created_at:
            self.created_at = time.time()

    def to_dynamo(self):
        return {
            'PK': f"BOT#{self.bot_id}",
            'SK': 'METADATA',
            'GSI1PK': f"AUTHOR#{self.author}",
            'GSI1SK': f"BOT#{self.bot_id}",
            **asdict(self)
        }

@dataclass
class Match:
    match_id: str
    bot1_id: str
    bot2_id: str
    rounds: int = 100
    status: str = 'PENDING'  # PENDING, RUNNING, COMPLETED, FAILED
    winner: Optional[str] = None
    created_at: float = None

    def __post_init__(self):
        if not self.match_id:
            self.match_id = f"match-{uuid.uuid4().hex[:8]}"
        if not self.created_at:
            self.created_at = time.time()

    def to_dynamo(self):
        return {
            'PK': f"MATCH#{self.match_id}",
            'SK': 'METADATA',
            'GSI1PK': f"STATUS#{self.status}",
            'GSI1SK': str(self.created_at),
            **asdict(self)
        }
```

```python
# layer/python/arena_common/engine.py
"""Battle engine logic shared across functions."""
import random

MOVES = ['ATTACK', 'DEFEND', 'HEAL', 'COUNTER']

DAMAGE_TABLE = {
    ('ATTACK', 'ATTACK'): (15, 15),    # Both take damage
    ('ATTACK', 'DEFEND'): (3, 5),      # Defender blocks, reflects
    ('ATTACK', 'HEAL'): (20, 0),       # Attack interrupts heal
    ('ATTACK', 'COUNTER'): (0, 30),    # Counter punishes attacker
    ('DEFEND', 'DEFEND'): (0, 0),      # Stalemate
    ('DEFEND', 'HEAL'): (0, 0),        # Both safe, healer gains HP
    ('DEFEND', 'COUNTER'): (0, 25),    # Counter bypasses defense
    ('HEAL', 'HEAL'): (0, 0),          # Both heal
    ('HEAL', 'COUNTER'): (0, 0),       # Counter whiffs on heal
    ('COUNTER', 'COUNTER'): (20, 20),  # Double counter = chaos
}

def resolve_round(move1, move2, hp1, hp2):
    """Resolve a single round. Returns (new_hp1, new_hp2, events)."""
    dmg1, dmg2 = DAMAGE_TABLE.get((move1, move2), (0, 0))
    # Add randomness (±5)
    dmg1 = max(0, dmg1 + random.randint(-5, 5))
    dmg2 = max(0, dmg2 + random.randint(-5, 5))

    new_hp1 = hp1 - dmg1
    new_hp2 = hp2 - dmg2

    # Heal logic
    if move1 == 'HEAL' and move2 != 'ATTACK':
        new_hp1 = min(100, new_hp1 + 15)
    if move2 == 'HEAL' and move1 != 'ATTACK':
        new_hp2 = min(100, new_hp2 + 15)

    return max(0, new_hp1), max(0, new_hp2), {'dmg_to_1': dmg1, 'dmg_to_2': dmg2}


def evaluate_strategy(strategy, game_state):
    """Evaluate bot strategy rules to pick a move."""
    rules = strategy.get('rules', [])
    sorted_rules = sorted(rules, key=lambda r: r.get('priority', 99))

    for rule in sorted_rules:
        if 'default' in rule:
            return rule['default']
        condition = rule.get('if', '')
        if evaluate_condition(condition, game_state):
            return rule['then']

    return 'ATTACK'  # Fallback


def evaluate_condition(condition, state):
    """Simple condition evaluator."""
    try:
        # Replace variables with values
        expr = condition
        for key, val in state.items():
            expr = expr.replace(key, repr(val))
        return eval(expr)  # Safe in sandboxed Lambda
    except:
        return False
```

### Deploy the Layer

```bash
# Package the layer
cd layer
zip -r arena-layer.zip python/

# Publish layer
aws lambda publish-layer-version \
  --layer-name arena-common \
  --zip-file fileb://arena-layer.zip \
  --compatible-runtimes python3.11 python3.12 \
  --description "Shared models and battle engine for Bot Arena"

# Output: LayerVersionArn: arn:aws:lambda:us-east-1:123456789:layer:arena-common:1
```

---

## 2.3 Bot Registry (Lambda Function URL)

**Lambda Feature**: Function URLs provide a dedicated HTTPS endpoint for your function — **no API Gateway needed**, saving cost and complexity.

```python
# functions/bot_registry/handler.py
"""
Bot Registry - CRUD operations via Lambda Function URL.

Lambda features demonstrated:
- Function URL (free HTTPS endpoint)
- Lambda Layer (shared models)
- DynamoDB integration
- Input validation
- Structured error responses
"""
import json
import boto3
from arena_common.models import Bot  # From Layer

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')


def handler(event, context):
    """Route based on HTTP method and path."""
    method = event.get('requestContext', {}).get('http', {}).get('method', 'GET')
    path = event.get('rawPath', '/')

    routes = {
        ('POST', '/bots'): create_bot,
        ('GET', '/bots'): list_bots,
        ('GET', '/bots/'): get_bot,  # /bots/<id>
        ('DELETE', '/bots/'): delete_bot,
    }

    # Match route
    for (m, p), func in routes.items():
        if method == m and path.startswith(p):
            return func(event)

    return response(404, {'error': 'Not found'})


def create_bot(event):
    """Create a new bot. Validates strategy schema."""
    body = json.loads(event.get('body', '{}'))

    # Validate required fields
    if not body.get('name') or not body.get('strategy'):
        return response(400, {'error': 'name and strategy required'})

    if not body.get('author'):
        return response(400, {'error': 'author required'})

    # Validate strategy format
    rules = body['strategy'].get('rules', [])
    if not rules:
        return response(400, {'error': 'strategy must have at least one rule'})

    # Validate moves
    valid_moves = {'ATTACK', 'DEFEND', 'HEAL', 'COUNTER'}
    for rule in rules:
        move = rule.get('then') or rule.get('default')
        if move and move not in valid_moves:
            return response(400, {'error': f'Invalid move: {move}. Valid: {valid_moves}'})

    # Create bot
    bot = Bot(
        bot_id='',
        name=body['name'],
        author=body['author'],
        strategy=body['strategy']
    )

    table.put_item(Item=bot.to_dynamo())

    return response(201, {'bot_id': bot.bot_id, 'name': bot.name, 'elo': bot.elo_rating})


def get_bot(event):
    """Get bot by ID."""
    path = event.get('rawPath', '')
    bot_id = path.split('/bots/')[-1]

    if not bot_id:
        return response(400, {'error': 'bot_id required'})

    result = table.get_item(Key={'PK': f'BOT#{bot_id}', 'SK': 'METADATA'})
    item = result.get('Item')

    if not item:
        return response(404, {'error': 'Bot not found'})

    return response(200, {
        'bot_id': item['bot_id'],
        'name': item['name'],
        'author': item['author'],
        'elo_rating': int(item['elo_rating']),
        'strategy': item['strategy']
    })


def list_bots(event):
    """List all bots (paginated)."""
    params = event.get('queryStringParameters') or {}
    limit = int(params.get('limit', '20'))

    # Scan (fine for demo; in prod use GSI)
    result = table.scan(
        FilterExpression='SK = :sk',
        ExpressionAttributeValues={':sk': 'METADATA'},
        Limit=limit
    )

    bots = [{'bot_id': i['bot_id'], 'name': i['name'], 'elo': int(i['elo_rating'])}
            for i in result.get('Items', []) if i['PK'].startswith('BOT#')]

    return response(200, {'bots': bots, 'count': len(bots)})


def delete_bot(event):
    """Delete a bot."""
    path = event.get('rawPath', '')
    bot_id = path.split('/bots/')[-1]

    table.delete_item(Key={'PK': f'BOT#{bot_id}', 'SK': 'METADATA'})
    return response(200, {'deleted': bot_id})


def response(status_code, body):
    return {
        'statusCode': status_code,
        'headers': {'Content-Type': 'application/json'},
        'body': json.dumps(body)
    }
```

### Deploy with Function URL

```bash
# Create function
zip -j bot_registry.zip functions/bot_registry/handler.py

aws lambda create-function \
  --function-name bot-registry \
  --runtime python3.11 \
  --handler handler.handler \
  --zip-file fileb://bot_registry.zip \
  --role arn:aws:iam::123456789:role/LambdaRole \
  --layers arn:aws:lambda:us-east-1:123456789:layer:arena-common:1 \
  --timeout 10 \
  --memory-size 256

# Create Function URL (NO API Gateway needed!)
aws lambda create-function-url-config \
  --function-name bot-registry \
  --auth-type NONE  # For demo; use AWS_IAM in production

# Add permission for public access
aws lambda add-permission \
  --function-name bot-registry \
  --action lambda:InvokeFunctionUrl \
  --principal "*" \
  --statement-id public-url \
  --function-url-auth-type NONE

# Output: https://abc123xyz.lambda-url.us-east-1.on.aws/
```

### Test It

```bash
# Create a bot
curl -X POST https://abc123xyz.lambda-url.us-east-1.on.aws/bots \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Berserker",
    "author": "player1",
    "strategy": {
      "rules": [
        {"if": "my_health < 20", "then": "HEAL", "priority": 1},
        {"default": "ATTACK"}
      ]
    }
  }'

# List bots
curl https://abc123xyz.lambda-url.us-east-1.on.aws/bots

# Get specific bot
curl https://abc123xyz.lambda-url.us-east-1.on.aws/bots/bot-a1b2c3d4
```

---

## 2.4 Battle Worker (Container Image Lambda)

**Lambda Feature**: Package functions as Docker containers up to 10GB. Use custom runtimes, binaries, ML models — anything that fits in a container.

```dockerfile
# functions/battle_worker/Dockerfile
FROM public.ecr.aws/lambda/python:3.11

# Install dependencies
COPY requirements.txt .
RUN pip install -r requirements.txt

# Copy function code
COPY handler.py .
COPY engine/ ./engine/

CMD ["handler.handler"]
```

```python
# functions/battle_worker/handler.py
"""
Battle Worker - Executes matches with full game simulation.

Lambda features demonstrated:
- Container image deployment (custom Dockerfile)
- 10GB memory allocation (heavy compute)
- 15-minute timeout (long matches)
- /tmp storage (10GB for replay files)
- Environment variables for configuration
"""
import json
import boto3
import time
import os
from arena_common.models import Match
from arena_common.engine import resolve_round, evaluate_strategy

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('BotArena')
s3 = boto3.client('s3')
eventbridge = boto3.client('events')

REPLAY_BUCKET = os.environ.get('REPLAY_BUCKET', 'bot-arena-replays')
MAX_ROUNDS = int(os.environ.get('MAX_ROUNDS', '100'))


def handler(event, context):
    """Execute a match between two bots."""
    # Can be triggered by SQS, Step Functions, or direct invocation
    match_id = event.get('match_id')
    bot1_id = event.get('bot1_id')
    bot2_id = event.get('bot2_id')
    rounds = min(event.get('rounds', 100), MAX_ROUNDS)

    # Load bot strategies
    bot1 = load_bot(bot1_id)
    bot2 = load_bot(bot2_id)

    if not bot1 or not bot2:
        return {'error': 'Bot not found', 'match_id': match_id}

    # Update match status
    update_match_status(match_id, 'RUNNING')
    emit_event('match.started', {'match_id': match_id})

    # Execute battle
    replay = []
    hp1, hp2 = 100, 100
    last_move1, last_move2 = None, None

    for round_num in range(1, rounds + 1):
        # Build game state for each bot
        state1 = {
            'my_health': hp1, 'opponent_health': hp2,
            'round': round_num, 'opponent_last_move': last_move2
        }
        state2 = {
            'my_health': hp2, 'opponent_health': hp1,
            'round': round_num, 'opponent_last_move': last_move1
        }

        # Get moves
        move1 = evaluate_strategy(bot1['strategy'], state1)
        move2 = evaluate_strategy(bot2['strategy'], state2)

        # Resolve
        hp1, hp2, events = resolve_round(move1, move2, hp1, hp2)

        # Record round
        replay.append({
            'round': round_num,
            'bot1_move': move1, 'bot2_move': move2,
            'bot1_hp': hp1, 'bot2_hp': hp2,
            **events
        })

        last_move1, last_move2 = move1, move2

        # Check for KO
        if hp1 <= 0 or hp2 <= 0:
            break

    # Determine winner
    if hp1 > hp2:
        winner = bot1_id
    elif hp2 > hp1:
        winner = bot2_id
    else:
        winner = 'DRAW'

    # Store replay in S3
    replay_key = f"replays/{match_id}.json"
    s3.put_object(
        Bucket=REPLAY_BUCKET,
        Key=replay_key,
        Body=json.dumps({'match_id': match_id, 'rounds': replay, 'winner': winner}),
        ContentType='application/json'
    )

    # Update match and bot stats
    update_match_status(match_id, 'COMPLETED', winner=winner)
    update_elo(bot1_id, bot2_id, winner)

    # Emit completion event
    emit_event('match.completed', {
        'match_id': match_id, 'winner': winner,
        'rounds_played': len(replay), 'final_hp': [hp1, hp2]
    })

    return {
        'match_id': match_id,
        'winner': winner,
        'rounds_played': len(replay),
        'bot1_final_hp': hp1,
        'bot2_final_hp': hp2
    }


def load_bot(bot_id):
    result = table.get_item(Key={'PK': f'BOT#{bot_id}', 'SK': 'METADATA'})
    return result.get('Item')


def update_match_status(match_id, status, winner=None):
    update_expr = 'SET #s = :s, updated_at = :t'
    expr_values = {':s': status, ':t': str(time.time())}
    if winner:
        update_expr += ', winner = :w'
        expr_values[':w'] = winner

    table.update_item(
        Key={'PK': f'MATCH#{match_id}', 'SK': 'METADATA'},
        UpdateExpression=update_expr,
        ExpressionAttributeNames={'#s': 'status'},
        ExpressionAttributeValues=expr_values
    )


def update_elo(bot1_id, bot2_id, winner):
    """Update ELO ratings."""
    K = 32
    bot1 = load_bot(bot1_id)
    bot2 = load_bot(bot2_id)
    r1, r2 = int(bot1['elo_rating']), int(bot2['elo_rating'])

    expected1 = 1 / (1 + 10**((r2 - r1) / 400))
    actual1 = 1 if winner == bot1_id else 0 if winner == bot2_id else 0.5

    new_r1 = round(r1 + K * (actual1 - expected1))
    new_r2 = round(r2 + K * ((1 - actual1) - (1 - expected1)))

    table.update_item(
        Key={'PK': f'BOT#{bot1_id}', 'SK': 'METADATA'},
        UpdateExpression='SET elo_rating = :r',
        ExpressionAttributeValues={':r': new_r1}
    )
    table.update_item(
        Key={'PK': f'BOT#{bot2_id}', 'SK': 'METADATA'},
        UpdateExpression='SET elo_rating = :r',
        ExpressionAttributeValues={':r': new_r2}
    )


def emit_event(detail_type, detail):
    eventbridge.put_events(Entries=[{
        'Source': 'bot-arena',
        'DetailType': detail_type,
        'Detail': json.dumps(detail),
        'EventBusName': 'default'
    }])
```

### Build & Deploy Container

```bash
# Create ECR repository
aws ecr create-repository --repository-name bot-arena/battle-worker

# Build and push
docker build -t battle-worker functions/battle_worker/
docker tag battle-worker:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/bot-arena/battle-worker:latest
aws ecr get-login-password | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/bot-arena/battle-worker:latest

# Create Lambda from container (10GB memory, 15 min timeout)
aws lambda create-function \
  --function-name battle-worker \
  --package-type Image \
  --code ImageUri=123456789.dkr.ecr.us-east-1.amazonaws.com/bot-arena/battle-worker:latest \
  --role arn:aws:iam::123456789:role/LambdaRole \
  --timeout 900 \
  --memory-size 10240 \
  --environment Variables="{REPLAY_BUCKET=bot-arena-replays,MAX_ROUNDS=100}"
```

---

## 2.5 Infrastructure as Code (CDK)

```python
# infrastructure/app.py
from aws_cdk import (
    App, Stack, Duration, RemovalPolicy,
    aws_lambda as _lambda,
    aws_dynamodb as dynamodb,
    aws_s3 as s3,
    aws_sqs as sqs,
    aws_iam as iam,
)
from constructs import Construct


class BotArenaStack(Stack):
    def __init__(self, scope: Construct, id: str, **kwargs):
        super().__init__(scope, id, **kwargs)

        # DynamoDB table
        table = dynamodb.Table(self, "BotArena",
            table_name="BotArena",
            partition_key=dynamodb.Attribute(name="PK", type=dynamodb.AttributeType.STRING),
            sort_key=dynamodb.Attribute(name="SK", type=dynamodb.AttributeType.STRING),
            billing_mode=dynamodb.BillingMode.PAY_PER_REQUEST,
            stream=dynamodb.StreamViewType.NEW_AND_OLD_IMAGES,
            removal_policy=RemovalPolicy.DESTROY,
        )
        table.add_global_secondary_index(
            index_name="GSI1",
            partition_key=dynamodb.Attribute(name="GSI1PK", type=dynamodb.AttributeType.STRING),
            sort_key=dynamodb.Attribute(name="GSI1SK", type=dynamodb.AttributeType.STRING),
        )

        # S3 bucket for replays
        replay_bucket = s3.Bucket(self, "ReplayBucket",
            bucket_name="bot-arena-replays",
            removal_policy=RemovalPolicy.DESTROY,
        )

        # Match queue
        dlq = sqs.Queue(self, "MatchDLQ", queue_name="match-dlq")
        match_queue = sqs.Queue(self, "MatchQueue",
            queue_name="match-queue",
            visibility_timeout=Duration.minutes(16),
            dead_letter_queue=sqs.DeadLetterQueue(queue=dlq, max_receive_count=3),
        )

        # Lambda Layer
        layer = _lambda.LayerVersion(self, "ArenaCommon",
            code=_lambda.Code.from_asset("../layer"),
            compatible_runtimes=[_lambda.Runtime.PYTHON_3_11],
            description="Shared arena code",
        )

        # Bot Registry (Function URL)
        bot_registry = _lambda.Function(self, "BotRegistry",
            function_name="bot-registry",
            runtime=_lambda.Runtime.PYTHON_3_11,
            handler="handler.handler",
            code=_lambda.Code.from_asset("../functions/bot_registry"),
            layers=[layer],
            timeout=Duration.seconds(10),
            memory_size=256,
        )
        bot_registry.add_function_url(auth_type=_lambda.FunctionUrlAuthType.NONE)
        table.grant_read_write_data(bot_registry)

        # Battle Worker (Container)
        battle_worker = _lambda.DockerImageFunction(self, "BattleWorker",
            function_name="battle-worker",
            code=_lambda.DockerImageCode.from_image_asset("../functions/battle_worker"),
            timeout=Duration.minutes(15),
            memory_size=10240,
            environment={
                "REPLAY_BUCKET": replay_bucket.bucket_name,
                "MAX_ROUNDS": "100",
            },
        )
        table.grant_read_write_data(battle_worker)
        replay_bucket.grant_write(battle_worker)


app = App()
BotArenaStack(app, "BotArenaStack")
app.synth()
```

---

## 2.6 Key Learnings from Part 2

| Feature | What You Learned |
|---------|-----------------|
| **Function URLs** | Free HTTPS endpoint, no API Gateway cost, supports streaming |
| **Lambda Layers** | Share code across functions, max 5 layers, 250MB unzipped |
| **Container Images** | Full Docker control, up to 10GB image, custom runtimes |
| **10GB Memory** | Proportional CPU (6 vCPUs at 10GB), good for compute-heavy |
| **15-min Timeout** | Sufficient for complex simulations, but Step Functions better for longer |
| **DynamoDB Single-Table** | Efficient access patterns, streams for event-driven |
| **CDK** | Infrastructure as code, reproducible deployments |

---

## Next: [Part 3 - Event-Driven Patterns →](Part3_Event_Driven_Patterns.md)
