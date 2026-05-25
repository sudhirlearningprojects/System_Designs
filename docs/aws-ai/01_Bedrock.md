# 1. Amazon Bedrock

## Overview

Amazon Bedrock is a fully managed service for accessing foundation models (Claude, Llama, Mistral, Titan, Cohere) through a unified API. No infrastructure to manage — just API calls.

### Available Models (2024-2025)

| Provider | Models | Best For |
|----------|--------|----------|
| **Anthropic** | Claude 4 Opus, Claude 4 Sonnet, Claude 3.5 Haiku | General purpose, coding, analysis |
| **Meta** | Llama 3.1 8B/70B/405B | Open-weight, customizable |
| **Mistral** | Mistral Large, Mistral Small | European compliance, efficiency |
| **Amazon** | Titan Text, Titan Embeddings, Titan Image | Cost-effective, AWS-native |
| **Cohere** | Command R+, Embed | RAG-optimized, multilingual |
| **AI21** | Jamba 1.5 | Long context, efficiency |
| **Stability AI** | SDXL, SD3 | Image generation |

---

## Setup

### Enable Model Access

```bash
# Request model access (required before use)
# AWS Console → Bedrock → Model access → Request access

# Or via CLI (check available models)
aws bedrock list-foundation-models \
  --query "modelSummaries[?providerName=='Anthropic'].{Model:modelId,Name:modelName}" \
  --output table

# Get model details
aws bedrock get-foundation-model --model-identifier anthropic.claude-sonnet-4-20250514-v1:0
```

### IAM Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:InvokeModelWithResponseStream",
        "bedrock:ListFoundationModels",
        "bedrock:GetFoundationModel"
      ],
      "Resource": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-*"
    }
  ]
}
```

---

## Invoke Models (Python SDK)

```python
# pip install boto3

import boto3
import json

bedrock_runtime = boto3.client("bedrock-runtime", region_name="us-east-1")

# ============ Claude (Anthropic) ============

response = bedrock_runtime.invoke_model(
    modelId="anthropic.claude-sonnet-4-20250514-v1:0",
    contentType="application/json",
    accept="application/json",
    body=json.dumps({
        "anthropic_version": "bedrock-2023-05-31",
        "max_tokens": 1024,
        "system": "You are a helpful assistant.",
        "messages": [
            {"role": "user", "content": "Explain AWS Bedrock in 3 sentences."}
        ],
        "temperature": 0.7,
    }),
)

result = json.loads(response["body"].read())
print(result["content"][0]["text"])
print(f"Tokens: {result['usage']['input_tokens']} in, {result['usage']['output_tokens']} out")
```

### Using the Converse API (Unified, Recommended)

```python
# Converse API: Same interface for ALL models (Claude, Llama, Mistral, Titan)
response = bedrock_runtime.converse(
    modelId="anthropic.claude-sonnet-4-20250514-v1:0",
    messages=[
        {"role": "user", "content": [{"text": "What is Amazon Bedrock?"}]}
    ],
    system=[{"text": "You are a helpful AWS expert."}],
    inferenceConfig={
        "maxTokens": 1024,
        "temperature": 0.7,
        "topP": 0.9,
    },
)

print(response["output"]["message"]["content"][0]["text"])
print(f"Tokens: {response['usage']['inputTokens']} in, {response['usage']['outputTokens']} out")
print(f"Latency: {response['metrics']['latencyMs']}ms")
```

### Streaming

```python
# Stream response token by token
response = bedrock_runtime.converse_stream(
    modelId="anthropic.claude-sonnet-4-20250514-v1:0",
    messages=[
        {"role": "user", "content": [{"text": "Write a poem about cloud computing."}]}
    ],
    inferenceConfig={"maxTokens": 500},
)

for event in response["stream"]:
    if "contentBlockDelta" in event:
        print(event["contentBlockDelta"]["delta"]["text"], end="", flush=True)
    elif "messageStop" in event:
        print(f"\nStop reason: {event['messageStop']['stopReason']}")
    elif "metadata" in event:
        usage = event["metadata"]["usage"]
        print(f"\nTokens: {usage['inputTokens']} in, {usage['outputTokens']} out")
```

### Tool Use (Function Calling)

```python
# Define tools
tool_config = {
    "tools": [
        {
            "toolSpec": {
                "name": "get_weather",
                "description": "Get current weather for a city.",
                "inputSchema": {
                    "json": {
                        "type": "object",
                        "properties": {
                            "city": {"type": "string", "description": "City name"},
                            "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
                        },
                        "required": ["city"],
                    }
                },
            }
        }
    ]
}

# Invoke with tools
response = bedrock_runtime.converse(
    modelId="anthropic.claude-sonnet-4-20250514-v1:0",
    messages=[{"role": "user", "content": [{"text": "What's the weather in Seattle?"}]}],
    toolConfig=tool_config,
)

# Check if model wants to use a tool
output = response["output"]["message"]
if output["content"][0].get("toolUse"):
    tool_use = output["content"][0]["toolUse"]
    print(f"Tool: {tool_use['name']}, Input: {tool_use['input']}")
    
    # Execute tool and send result back
    tool_result = get_weather(**tool_use["input"])
    
    # Continue conversation with tool result
    messages = [
        {"role": "user", "content": [{"text": "What's the weather in Seattle?"}]},
        {"role": "assistant", "content": output["content"]},
        {"role": "user", "content": [{"toolResult": {
            "toolUseId": tool_use["toolUseId"],
            "content": [{"json": tool_result}],
        }}]},
    ]
    
    final_response = bedrock_runtime.converse(
        modelId="anthropic.claude-sonnet-4-20250514-v1:0",
        messages=messages,
        toolConfig=tool_config,
    )
    print(final_response["output"]["message"]["content"][0]["text"])
```

---

## Embeddings

```python
# Amazon Titan Embeddings
response = bedrock_runtime.invoke_model(
    modelId="amazon.titan-embed-text-v2:0",
    contentType="application/json",
    body=json.dumps({
        "inputText": "How do I cancel my subscription?",
        "dimensions": 1024,  # 256, 512, or 1024
        "normalize": True,
    }),
)

result = json.loads(response["body"].read())
embedding = result["embedding"]  # List of 1024 floats
print(f"Embedding dimension: {len(embedding)}")

# Cohere Embed (multilingual, better for RAG)
response = bedrock_runtime.invoke_model(
    modelId="cohere.embed-english-v3",
    contentType="application/json",
    body=json.dumps({
        "texts": ["How do I cancel?", "What is the refund policy?"],
        "input_type": "search_query",  # or "search_document" for indexing
    }),
)
result = json.loads(response["body"].read())
embeddings = result["embeddings"]  # List of embedding vectors
```

---

## Bedrock Guardrails

```bash
# Create guardrail
aws bedrock create-guardrail \
  --name "production-guardrail" \
  --description "Safety guardrail for customer support agent" \
  --topic-policy-config '{
    "topicsConfig": [
      {"name": "Financial Advice", "definition": "Providing specific investment or financial advice", "type": "DENY"},
      {"name": "Competitor Discussion", "definition": "Discussing competitor products or services", "type": "DENY"}
    ]
  }' \
  --content-policy-config '{
    "filtersConfig": [
      {"type": "SEXUAL", "inputStrength": "HIGH", "outputStrength": "HIGH"},
      {"type": "VIOLENCE", "inputStrength": "HIGH", "outputStrength": "HIGH"},
      {"type": "HATE", "inputStrength": "HIGH", "outputStrength": "HIGH"},
      {"type": "INSULTS", "inputStrength": "MEDIUM", "outputStrength": "HIGH"},
      {"type": "MISCONDUCT", "inputStrength": "HIGH", "outputStrength": "HIGH"},
      {"type": "PROMPT_ATTACK", "inputStrength": "HIGH", "outputStrength": "NONE"}
    ]
  }' \
  --word-policy-config '{
    "wordsConfig": [{"text": "competitor_name"}],
    "managedWordListsConfig": [{"type": "PROFANITY"}]
  }'
```

```python
# Use guardrail with model invocation
response = bedrock_runtime.converse(
    modelId="anthropic.claude-sonnet-4-20250514-v1:0",
    messages=[{"role": "user", "content": [{"text": "Give me investment advice"}]}],
    guardrailConfig={
        "guardrailIdentifier": "your-guardrail-id",
        "guardrailVersion": "DRAFT",  # or version number
        "trace": "enabled",  # See which guardrail triggered
    },
)

# Check if guardrail intervened
if response.get("stopReason") == "guardrail_intervened":
    print("Guardrail blocked this request")
    trace = response.get("trace", {}).get("guardrail", {})
    print(f"Reason: {trace}")
```

---

## Image Generation (Titan Image / Stability AI)

```python
import base64

# Amazon Titan Image Generator
response = bedrock_runtime.invoke_model(
    modelId="amazon.titan-image-generator-v2:0",
    contentType="application/json",
    body=json.dumps({
        "textToImageParams": {"text": "A futuristic data center in the clouds"},
        "imageGenerationConfig": {
            "numberOfImages": 1,
            "height": 1024,
            "width": 1024,
            "quality": "premium",
        },
    }),
)

result = json.loads(response["body"].read())
image_bytes = base64.b64decode(result["images"][0])
with open("generated.png", "wb") as f:
    f.write(image_bytes)
```

---

## Cross-Region Inference (Multi-Region)

```python
# Bedrock supports cross-region inference for higher throughput
# Automatically routes to available capacity across regions

response = bedrock_runtime.converse(
    modelId="us.anthropic.claude-sonnet-4-20250514-v1:0",  # "us." prefix = cross-region
    messages=[{"role": "user", "content": [{"text": "Hello!"}]}],
)
# Routes to any US region with available capacity
```

---

## Provisioned Throughput

```bash
# For guaranteed capacity (no throttling)
aws bedrock create-provisioned-model-throughput \
  --model-id anthropic.claude-sonnet-4-20250514-v1:0 \
  --provisioned-model-name my-dedicated-claude \
  --model-units 1  # Each unit = specific tokens/min capacity

# Use provisioned model
# modelId = "arn:aws:bedrock:us-east-1:123456789:provisioned-model/my-dedicated-claude"
```

---

## Cost Comparison

| Model | Input (per 1M tokens) | Output (per 1M tokens) |
|-------|----------------------|------------------------|
| Claude 4 Sonnet | $3.00 | $15.00 |
| Claude 3.5 Haiku | $0.80 | $4.00 |
| Llama 3.1 70B | $2.65 | $3.50 |
| Llama 3.1 8B | $0.22 | $0.22 |
| Mistral Large | $4.00 | $12.00 |
| Titan Text Express | $0.20 | $0.60 |
| Titan Embeddings v2 | $0.02 | — |

---

## Next: [Bedrock Agents & RAG →](02_Bedrock_Agents_RAG.md)
