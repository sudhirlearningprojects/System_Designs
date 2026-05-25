# 1. Claude Fundamentals & API

## Claude Models

### Model Selection Guide

| Use Case | Recommended Model | Why |
|----------|-------------------|-----|
| Complex reasoning, research, code generation | Claude 4 Opus | Highest intelligence |
| Production workloads, balanced cost/quality | Claude 4 Sonnet | Best value for most tasks |
| High-volume, low-latency (classification, extraction) | Claude 3.5 Haiku | Fastest, cheapest |
| Agentic coding, tool use | Claude 4 Sonnet | Best at tool use + reasoning |
| Long document analysis (100K+ tokens) | Any (200K context) | All support extended context |

### Model IDs

```python
# Latest models (use these)
OPUS = "claude-opus-4-20250514"
SONNET = "claude-sonnet-4-20250514"
HAIKU = "claude-3-5-haiku-20241022"

# Previous generation
SONNET_35 = "claude-3-5-sonnet-20241022"
```

---

## Messages API

### Basic Request

```python
import anthropic

client = anthropic.Anthropic()

response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=4096,
    system="You are a helpful coding assistant. Be concise.",
    messages=[
        {"role": "user", "content": "Write a Python function to merge two sorted arrays."}
    ],
    temperature=0.0,  # Deterministic output
)

print(response.content[0].text)
print(f"Tokens: {response.usage.input_tokens} in, {response.usage.output_tokens} out")
```

### Multi-Turn Conversation

```python
messages = [
    {"role": "user", "content": "What is a binary search tree?"},
    {"role": "assistant", "content": "A binary search tree (BST) is a data structure where each node has at most two children, with left children smaller and right children larger than the parent."},
    {"role": "user", "content": "How do I balance one?"},
]

response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=2048,
    messages=messages,
)
```

### System Prompt

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    system=[
        {
            "type": "text",
            "text": "You are an expert Python developer. Follow PEP 8. Use type hints.",
            "cache_control": {"type": "ephemeral"}  # Enable prompt caching
        }
    ],
    messages=[{"role": "user", "content": "Implement a thread-safe singleton."}]
)
```

---

## Streaming

### Basic Streaming

```python
with client.messages.stream(
    model="claude-sonnet-4-20250514",
    max_tokens=4096,
    messages=[{"role": "user", "content": "Explain microservices architecture."}]
) as stream:
    for text in stream.text_stream:
        print(text, end="", flush=True)

# Get final message with usage stats
final_message = stream.get_final_message()
print(f"\nTokens used: {final_message.usage.output_tokens}")
```

### Streaming with Events

```python
with client.messages.stream(
    model="claude-sonnet-4-20250514",
    max_tokens=4096,
    messages=[{"role": "user", "content": "Solve this step by step: 2x + 5 = 13"}]
) as stream:
    for event in stream:
        match event.type:
            case "message_start":
                print(f"Model: {event.message.model}")
            case "content_block_start":
                print(f"Block type: {event.content_block.type}")
            case "content_block_delta":
                if event.delta.type == "text_delta":
                    print(event.delta.text, end="")
            case "message_delta":
                print(f"\nStop reason: {event.delta.stop_reason}")
                print(f"Output tokens: {event.usage.output_tokens}")
```

### Async Streaming

```python
import asyncio
import anthropic

async def stream_response():
    client = anthropic.AsyncAnthropic()
    
    async with client.messages.stream(
        model="claude-sonnet-4-20250514",
        max_tokens=2048,
        messages=[{"role": "user", "content": "Write a haiku about coding."}]
    ) as stream:
        async for text in stream.text_stream:
            print(text, end="", flush=True)

asyncio.run(stream_response())
```

---

## Vision (Multi-Modal)

### Image Analysis

```python
import base64
import httpx

# From URL
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{
        "role": "user",
        "content": [
            {
                "type": "image",
                "source": {
                    "type": "url",
                    "url": "https://example.com/architecture-diagram.png"
                }
            },
            {
                "type": "text",
                "text": "Describe this system architecture diagram. What are the main components?"
            }
        ]
    }]
)

# From base64
with open("screenshot.png", "rb") as f:
    image_data = base64.standard_b64encode(f.read()).decode("utf-8")

response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{
        "role": "user",
        "content": [
            {
                "type": "image",
                "source": {
                    "type": "base64",
                    "media_type": "image/png",
                    "data": image_data
                }
            },
            {"type": "text", "text": "What error is shown in this screenshot?"}
        ]
    }]
)
```

### Multiple Images

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=2048,
    messages=[{
        "role": "user",
        "content": [
            {"type": "image", "source": {"type": "url", "url": "https://example.com/before.png"}},
            {"type": "image", "source": {"type": "url", "url": "https://example.com/after.png"}},
            {"type": "text", "text": "Compare these two UI designs. What changed?"}
        ]
    }]
)
```

---

## Prompt Caching

Cache frequently-used context to reduce cost and latency.

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    system=[
        {
            "type": "text",
            "text": "You are an expert on Adobe Creative Cloud products...",  # Long system prompt
            "cache_control": {"type": "ephemeral"}  # Cache this block
        }
    ],
    messages=[
        {
            "role": "user",
            "content": [
                {
                    "type": "text",
                    "text": "<large_document>... 50,000 tokens of documentation ...</large_document>",
                    "cache_control": {"type": "ephemeral"}  # Cache the document
                },
                {
                    "type": "text",
                    "text": "Based on the documentation above, how do I export a PDF?"
                }
            ]
        }
    ]
)

# Check cache usage
print(f"Cache read tokens: {response.usage.cache_read_input_tokens}")
print(f"Cache creation tokens: {response.usage.cache_creation_input_tokens}")
# Cached tokens cost 90% less on subsequent requests
```

**Caching Rules:**
- Minimum cacheable: 1,024 tokens (Haiku) or 2,048 tokens (Sonnet/Opus)
- Cache TTL: 5 minutes (refreshed on each use)
- Cost: 25% more to write cache, 90% less to read from cache
- Cache from the beginning of the message (prefix caching)

---

## Extended Thinking

Enable Claude to reason through complex problems before responding.

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=16000,
    thinking={
        "type": "enabled",
        "budget_tokens": 10000  # Max tokens for thinking
    },
    messages=[{
        "role": "user",
        "content": "Design a distributed consensus algorithm for a 5-node cluster that handles network partitions."
    }]
)

# Response contains thinking blocks + text blocks
for block in response.content:
    if block.type == "thinking":
        print(f"[THINKING]: {block.thinking}")
    elif block.type == "text":
        print(f"[RESPONSE]: {block.text}")
```

**When to use Extended Thinking:**
- Complex math/logic problems
- Multi-step planning
- Code architecture decisions
- Debugging complex issues
- Any task where "showing work" improves quality

---

## Batches API (Async Bulk Processing)

Process large volumes at 50% cost reduction.

```python
# Create a batch of requests
batch = client.batches.create(
    requests=[
        {
            "custom_id": f"request-{i}",
            "params": {
                "model": "claude-sonnet-4-20250514",
                "max_tokens": 1024,
                "messages": [{"role": "user", "content": f"Summarize: {doc}"}]
            }
        }
        for i, doc in enumerate(documents)
    ]
)

# Check batch status
batch_status = client.batches.retrieve(batch.id)
print(f"Status: {batch_status.processing_status}")
# processing, ended

# Get results when complete
if batch_status.processing_status == "ended":
    for result in client.batches.results(batch.id):
        print(f"{result.custom_id}: {result.result.message.content[0].text}")
```

**Batch API Details:**
- 50% cost reduction vs real-time API
- Results within 24 hours (usually much faster)
- Up to 100,000 requests per batch
- Ideal for: data processing, evaluations, content generation at scale

---

## Token Management

### Counting Tokens

```python
# Count tokens before sending (estimate)
count = client.count_tokens(
    model="claude-sonnet-4-20250514",
    messages=[{"role": "user", "content": "Hello, world!"}]
)
print(f"Input tokens: {count.input_tokens}")

# From response
response = client.messages.create(...)
print(f"Input: {response.usage.input_tokens}")
print(f"Output: {response.usage.output_tokens}")
print(f"Total cost: ${response.usage.input_tokens * 0.000003 + response.usage.output_tokens * 0.000015:.4f}")
```

### Token Budget Management

```python
class TokenBudgetManager:
    def __init__(self, max_input_tokens: int = 180000, max_output_tokens: int = 8192):
        self.max_input = max_input_tokens
        self.max_output = max_output_tokens
    
    def fit_messages(self, system: str, messages: list, max_context: int = None) -> list:
        """Truncate conversation to fit within token budget."""
        budget = max_context or self.max_input
        system_tokens = self._estimate_tokens(system)
        remaining = budget - system_tokens - 500  # Safety margin
        
        # Always keep first (system context) and last N messages
        fitted = []
        total = 0
        for msg in reversed(messages):
            msg_tokens = self._estimate_tokens(msg["content"])
            if total + msg_tokens > remaining:
                break
            fitted.insert(0, msg)
            total += msg_tokens
        
        return fitted
    
    def _estimate_tokens(self, text: str) -> int:
        # Rough estimate: ~4 chars per token for English
        return len(text) // 4
```

---

## Error Handling

```python
import anthropic
from anthropic import (
    APIError, AuthenticationError, RateLimitError,
    APIConnectionError, BadRequestError
)

def call_claude_with_retry(messages, max_retries=3):
    for attempt in range(max_retries):
        try:
            return client.messages.create(
                model="claude-sonnet-4-20250514",
                max_tokens=4096,
                messages=messages
            )
        except RateLimitError as e:
            # 429: Rate limited - wait and retry
            wait = min(2 ** attempt * 10, 60)
            print(f"Rate limited. Waiting {wait}s...")
            time.sleep(wait)
        except APIConnectionError:
            # Network error - retry
            time.sleep(2 ** attempt)
        except BadRequestError as e:
            # 400: Invalid request (don't retry)
            raise
        except AuthenticationError:
            # 401: Invalid API key (don't retry)
            raise
        except APIError as e:
            # 500+: Server error - retry
            if e.status_code >= 500:
                time.sleep(2 ** attempt)
            else:
                raise
    
    raise Exception("Max retries exceeded")
```

---

## Rate Limits

| Model | Requests/min | Input tokens/min | Output tokens/min |
|-------|-------------|-----------------|-------------------|
| Claude 4 Opus | 2,000 | 200,000 | 40,000 |
| Claude 4 Sonnet | 2,000 | 400,000 | 80,000 |
| Claude 3.5 Haiku | 4,000 | 800,000 | 160,000 |

**Headers to monitor:**
```
anthropic-ratelimit-requests-limit: 2000
anthropic-ratelimit-requests-remaining: 1999
anthropic-ratelimit-requests-reset: 2024-01-15T10:00:00Z
anthropic-ratelimit-tokens-limit: 400000
anthropic-ratelimit-tokens-remaining: 399000
```

---

## Next: [Prompt Engineering →](02_Prompt_Engineering.md)
