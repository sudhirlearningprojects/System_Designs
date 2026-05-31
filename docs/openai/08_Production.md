# 8. Production Patterns

## Rate Limits & Retry

```python
from openai import OpenAI, RateLimitError, APIError
import time

def call_with_retry(client, max_retries=5, **kwargs):
    for attempt in range(max_retries):
        try:
            return client.chat.completions.create(**kwargs)
        except RateLimitError as e:
            wait = min(2 ** attempt * 5, 60)
            print(f"Rate limited. Waiting {wait}s (attempt {attempt + 1})")
            time.sleep(wait)
        except APIError as e:
            if e.status_code >= 500:
                time.sleep(2 ** attempt)
            else:
                raise
    raise Exception("Max retries exceeded")

# Rate limit tiers (as of 2024):
# Tier 1: 500 RPM, 200K TPM (new accounts)
# Tier 2: 5000 RPM, 2M TPM
# Tier 3: 5000 RPM, 10M TPM
# Tier 4: 10000 RPM, 50M TPM
# Tier 5: 10000 RPM, 300M TPM
```

## Batch API (50% Cost Reduction)

```python
# For non-real-time processing — 50% cheaper, results within 24h

# Create batch file
import json

requests = []
for i, query in enumerate(queries):
    requests.append({
        "custom_id": f"request-{i}",
        "method": "POST",
        "url": "/v1/chat/completions",
        "body": {
            "model": "gpt-4o-mini",
            "messages": [{"role": "user", "content": query}],
            "max_tokens": 500,
        },
    })

# Write to JSONL
with open("batch_input.jsonl", "w") as f:
    for req in requests:
        f.write(json.dumps(req) + "\n")

# Upload and create batch
batch_file = client.files.create(file=open("batch_input.jsonl", "rb"), purpose="batch")
batch = client.batches.create(
    input_file_id=batch_file.id,
    endpoint="/v1/chat/completions",
    completion_window="24h",
)

# Check status
batch = client.batches.retrieve(batch.id)
print(f"Status: {batch.status}, Completed: {batch.request_counts.completed}/{batch.request_counts.total}")

# Download results
if batch.status == "completed":
    result_file = client.files.content(batch.output_file_id)
    results = [json.loads(line) for line in result_file.text.strip().split("\n")]
```

## Cost Tracking

```python
PRICING = {
    "gpt-4o": {"input": 2.50, "output": 10.00},
    "gpt-4o-mini": {"input": 0.15, "output": 0.60},
    "o1": {"input": 15.00, "output": 60.00},
    "text-embedding-3-small": {"input": 0.02},
}

def calculate_cost(model: str, input_tokens: int, output_tokens: int) -> float:
    p = PRICING.get(model, {"input": 5.0, "output": 15.0})
    return (input_tokens * p["input"] + output_tokens * p.get("output", 0)) / 1_000_000

# Track per request
response = client.chat.completions.create(model="gpt-4o", messages=messages)
cost = calculate_cost("gpt-4o", response.usage.prompt_tokens, response.usage.completion_tokens)
print(f"Cost: ${cost:.4f}")
```

## Model Routing (Cost Optimization)

```python
def select_model(query: str, complexity: str) -> str:
    """Route to cheapest model that can handle the task."""
    if complexity == "simple":  # FAQ, classification
        return "gpt-4o-mini"   # $0.15/$0.60 per 1M
    elif complexity == "standard":  # General support
        return "gpt-4o"        # $2.50/$10 per 1M
    else:  # Complex reasoning
        return "o1-mini"       # $3/$12 per 1M
```

## Error Handling

```python
from openai import (
    OpenAI, APIError, RateLimitError, APIConnectionError,
    AuthenticationError, BadRequestError,
)

try:
    response = client.chat.completions.create(...)
except RateLimitError:
    # 429: Wait and retry (exponential backoff)
    pass
except APIConnectionError:
    # Network error: retry
    pass
except BadRequestError as e:
    # 400: Invalid request (don't retry — fix the request)
    # Common: context length exceeded, invalid model
    pass
except AuthenticationError:
    # 401: Invalid API key (don't retry)
    pass
except APIError as e:
    if e.status_code >= 500:
        # Server error: retry
        pass
    else:
        raise
```

## Production Checklist

- [ ] Retry with exponential backoff (handle 429, 500+)
- [ ] Model fallback chain (gpt-4o → gpt-4o-mini if rate limited)
- [ ] Cost tracking per user/feature
- [ ] Batch API for non-real-time (50% savings)
- [ ] Response caching for repeated queries
- [ ] Moderation API on input AND output
- [ ] Structured output (json_schema) for reliable parsing
- [ ] Streaming for user-facing responses
- [ ] Usage monitoring (track tokens/day, alert on spikes)
- [ ] API key rotation and secrets management
