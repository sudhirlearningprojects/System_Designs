# 1. API Fundamentals

## Chat Completions

```python
from openai import OpenAI

client = OpenAI()

# Basic completion
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "You are a helpful coding assistant."},
        {"role": "user", "content": "Write a Python fibonacci function."},
    ],
    temperature=0.7,
    max_tokens=1024,
)

print(response.choices[0].message.content)
print(f"Tokens: {response.usage.prompt_tokens} in, {response.usage.completion_tokens} out")
print(f"Finish reason: {response.choices[0].finish_reason}")
```

## Streaming

```python
stream = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Explain microservices."}],
    stream=True,
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

## Async

```python
from openai import AsyncOpenAI
import asyncio

async_client = AsyncOpenAI()

async def generate():
    response = await async_client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": "Hello!"}],
    )
    return response.choices[0].message.content

result = asyncio.run(generate())
```

## Vision (Image Input)

```python
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "What's in this image?"},
            {"type": "image_url", "image_url": {"url": "https://example.com/image.png"}},
        ],
    }],
)

# From base64
import base64
with open("screenshot.png", "rb") as f:
    b64 = base64.b64encode(f.read()).decode()

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "Describe this error screenshot."},
            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{b64}"}},
        ],
    }],
)
```

## Structured Output (JSON Mode)

```python
from pydantic import BaseModel

class SupportTicket(BaseModel):
    category: str
    priority: str
    summary: str
    suggested_action: str

# Method 1: response_format with schema
response = client.chat.completions.create(
    model="gpt-4o-2024-08-06",
    messages=[
        {"role": "system", "content": "Extract support ticket info."},
        {"role": "user", "content": "I was charged twice and I'm furious!"},
    ],
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "support_ticket",
            "schema": SupportTicket.model_json_schema(),
            "strict": True,
        },
    },
)

ticket = SupportTicket.model_validate_json(response.choices[0].message.content)

# Method 2: client.beta.chat.completions.parse (SDK helper)
response = client.beta.chat.completions.parse(
    model="gpt-4o-2024-08-06",
    messages=[
        {"role": "system", "content": "Extract ticket info."},
        {"role": "user", "content": "I was charged twice!"},
    ],
    response_format=SupportTicket,
)
ticket = response.choices[0].message.parsed  # Already a SupportTicket object
```

## Reasoning Models (o1)

```python
# o1 models think before answering (chain-of-thought built in)
response = client.chat.completions.create(
    model="o1",
    messages=[{
        "role": "user",
        "content": "Design a distributed consensus algorithm for a 5-node cluster."
    }],
    # Note: o1 doesn't support temperature, system messages, or streaming
)

# o1 uses "reasoning tokens" (billed but not shown)
print(f"Reasoning tokens: {response.usage.completion_tokens_details.reasoning_tokens}")
```

---

## Next: [Function Calling & Tools →](02_Function_Calling.md)
