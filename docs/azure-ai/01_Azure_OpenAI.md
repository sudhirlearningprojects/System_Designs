# 1. Azure OpenAI Service

## Overview

Azure OpenAI provides access to OpenAI models (GPT-4o, GPT-4, o1, DALL-E 3, Whisper) with enterprise features: private networking, content filtering, regional deployment, and SLA guarantees.

### Why Azure OpenAI vs Direct OpenAI?

| Feature | Azure OpenAI | Direct OpenAI |
|---------|-------------|---------------|
| Data privacy | Data stays in your Azure region | Data processed by OpenAI |
| Networking | Private endpoints, VNet integration | Public internet only |
| Compliance | SOC2, HIPAA, FedRAMP, GDPR | SOC2 only |
| SLA | 99.9% uptime guarantee | Best-effort |
| Content filtering | Built-in, configurable | Basic moderation |
| Fine-tuning | Managed, your data stays in Azure | Managed by OpenAI |
| Billing | Azure subscription, enterprise agreements | Credit card / API billing |
| Rate limits | Provisioned throughput (PTU) available | Token-based limits |

---

## Setup (Azure Portal)

```
1. Azure Portal → Create Resource → "Azure OpenAI"
2. Select subscription, resource group, region
3. Pricing tier: S0 (standard)
4. Network: Public (dev) or Private endpoint (production)
5. Create → Wait for deployment

6. Go to resource → Model Deployments → Deploy Model
7. Select: gpt-4o, version 2024-08-06
8. Deployment name: "gpt-4o" (you'll reference this in code)
9. Tokens per minute: 30K (adjust based on needs)
```

## Setup (Azure CLI)

```bash
# Create resource
az cognitiveservices account create \
  --name my-aoai \
  --resource-group rg-ai \
  --kind OpenAI \
  --sku S0 \
  --location eastus2

# Deploy models
az cognitiveservices account deployment create \
  --name my-aoai -g rg-ai \
  --deployment-name gpt-4o \
  --model-name gpt-4o \
  --model-version "2024-08-06" \
  --model-format OpenAI \
  --sku-name Standard \
  --sku-capacity 30

az cognitiveservices account deployment create \
  --name my-aoai -g rg-ai \
  --deployment-name text-embedding-3-small \
  --model-name text-embedding-3-small \
  --model-version "1" \
  --model-format OpenAI \
  --sku-name Standard \
  --sku-capacity 120

# Get credentials
ENDPOINT=$(az cognitiveservices account show --name my-aoai -g rg-ai --query "properties.endpoint" -o tsv)
KEY=$(az cognitiveservices account keys list --name my-aoai -g rg-ai --query "key1" -o tsv)
```

## Setup (Terraform)

```hcl
resource "azurerm_cognitive_account" "openai" {
  name                = "my-aoai"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  kind                = "OpenAI"
  sku_name            = "S0"

  network_acls {
    default_action = "Deny"
    virtual_network_rules {
      subnet_id = azurerm_subnet.ai_subnet.id
    }
  }
}

resource "azurerm_cognitive_deployment" "gpt4o" {
  name                 = "gpt-4o"
  cognitive_account_id = azurerm_cognitive_account.openai.id

  model {
    format  = "OpenAI"
    name    = "gpt-4o"
    version = "2024-08-06"
  }

  sku {
    name     = "Standard"
    capacity = 30
  }
}
```

---

## Chat Completions (Python SDK)

```python
# pip install openai

from openai import AzureOpenAI
import os

client = AzureOpenAI(
    azure_endpoint=os.environ["AZURE_OPENAI_ENDPOINT"],
    api_key=os.environ["AZURE_OPENAI_KEY"],
    api_version="2024-10-21",  # Latest GA version
)

# Basic chat completion
response = client.chat.completions.create(
    model="gpt-4o",  # This is your DEPLOYMENT name, not model name
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Explain Azure OpenAI in 3 sentences."},
    ],
    temperature=0.7,
    max_tokens=500,
)

print(response.choices[0].message.content)
print(f"Tokens: {response.usage.prompt_tokens} in, {response.usage.completion_tokens} out")
```

### Streaming

```python
stream = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Write a poem about cloud computing."}],
    stream=True,
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

### Function Calling (Tool Use)

```python
import json

tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "Get current weather for a location",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {"type": "string", "description": "City name"},
                    "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
                },
                "required": ["location"],
            },
        },
    }
]

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "What's the weather in Seattle?"}],
    tools=tools,
    tool_choice="auto",
)

# Check if model wants to call a tool
if response.choices[0].message.tool_calls:
    tool_call = response.choices[0].message.tool_calls[0]
    print(f"Tool: {tool_call.function.name}")
    print(f"Args: {tool_call.function.arguments}")
    
    # Execute tool and send result back
    tool_result = get_weather(**json.loads(tool_call.function.arguments))
    
    messages = [
        {"role": "user", "content": "What's the weather in Seattle?"},
        response.choices[0].message,
        {"role": "tool", "tool_call_id": tool_call.id, "content": json.dumps(tool_result)},
    ]
    
    final_response = client.chat.completions.create(model="gpt-4o", messages=messages)
    print(final_response.choices[0].message.content)
```

### Structured Output (JSON Mode)

```python
from pydantic import BaseModel

class SupportTicket(BaseModel):
    category: str
    priority: str
    summary: str
    suggested_action: str

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "Extract support ticket info. Respond in JSON."},
        {"role": "user", "content": "I've been charged twice this month and I'm furious!"},
    ],
    response_format={"type": "json_object"},
)

ticket = SupportTicket.model_validate_json(response.choices[0].message.content)
print(f"Category: {ticket.category}, Priority: {ticket.priority}")
```

---

## Embeddings

```python
# Generate embeddings for RAG
response = client.embeddings.create(
    model="text-embedding-3-small",  # Deployment name
    input=["How do I cancel my subscription?", "What is the refund policy?"],
)

embeddings = [item.embedding for item in response.data]
print(f"Embedding dimension: {len(embeddings[0])}")  # 1536

# Batch processing (max 2048 inputs per request)
texts = ["text1", "text2", ..., "text2048"]
response = client.embeddings.create(model="text-embedding-3-small", input=texts)
```

---

## Image Generation (DALL-E 3)

```python
response = client.images.generate(
    model="dall-e-3",  # Deployment name
    prompt="A futuristic data center in the clouds, digital art style",
    size="1024x1024",
    quality="hd",
    n=1,
)

image_url = response.data[0].url
print(f"Image URL: {image_url}")
```

---

## Fine-Tuning on Azure

```bash
# 1. Upload training data
az cognitiveservices account file upload \
  --name my-aoai -g rg-ai \
  --file-path training_data.jsonl \
  --purpose fine-tune

# 2. Create fine-tuning job
az cognitiveservices account fine-tuning create \
  --name my-aoai -g rg-ai \
  --training-file file-abc123 \
  --model gpt-4o-mini-2024-07-18 \
  --suffix "my-custom-model"

# 3. Monitor
az cognitiveservices account fine-tuning show \
  --name my-aoai -g rg-ai \
  --job-id ftjob-xyz789

# 4. Deploy fine-tuned model
az cognitiveservices account deployment create \
  --name my-aoai -g rg-ai \
  --deployment-name my-custom-model \
  --model-name gpt-4o-mini-2024-07-18.ft-abc123 \
  --model-format OpenAI \
  --sku-name Standard \
  --sku-capacity 10
```

```python
# Python SDK for fine-tuning
# Upload file
file = client.files.create(file=open("training.jsonl", "rb"), purpose="fine-tune")

# Create job
job = client.fine_tuning.jobs.create(
    training_file=file.id,
    model="gpt-4o-mini-2024-07-18",
    hyperparameters={"n_epochs": 3},
)

# Monitor
status = client.fine_tuning.jobs.retrieve(job.id)
print(f"Status: {status.status}, Model: {status.fine_tuned_model}")
```

---

## Content Filtering

Azure OpenAI includes built-in content filtering (can be customized):

```python
# Content filter results are in the response
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Tell me about safety protocols"}],
)

# Check content filter results
if hasattr(response.choices[0], 'content_filter_results'):
    filters = response.choices[0].content_filter_results
    print(f"Hate: {filters.get('hate', {}).get('severity', 'safe')}")
    print(f"Violence: {filters.get('violence', {}).get('severity', 'safe')}")
    print(f"Self-harm: {filters.get('self_harm', {}).get('severity', 'safe')}")
    print(f"Sexual: {filters.get('sexual', {}).get('severity', 'safe')}")
```

### Custom Content Filter Policy

```bash
# Create custom filter (Azure Portal or REST API)
# Allows you to:
# - Adjust severity thresholds (low/medium/high)
# - Enable/disable specific categories
# - Add blocklists (custom blocked terms)
# - Enable prompt shields (jailbreak detection)
# - Enable groundedness detection
```

---

## Authentication Options

```python
# Option 1: API Key (simple, development)
client = AzureOpenAI(
    azure_endpoint="https://my-aoai.openai.azure.com/",
    api_key="your-key-here",
    api_version="2024-10-21",
)

# Option 2: Azure AD / Entra ID (production, no keys to manage)
from azure.identity import DefaultAzureCredential, get_bearer_token_provider

credential = DefaultAzureCredential()
token_provider = get_bearer_token_provider(credential, "https://cognitiveservices.azure.com/.default")

client = AzureOpenAI(
    azure_endpoint="https://my-aoai.openai.azure.com/",
    azure_ad_token_provider=token_provider,
    api_version="2024-10-21",
)

# Option 3: Managed Identity (for Azure-hosted apps)
# Assign "Cognitive Services OpenAI User" role to your app's managed identity
# Then use DefaultAzureCredential() — it auto-detects managed identity
```

---

## Provisioned Throughput (PTU)

```bash
# For predictable performance (no throttling, guaranteed capacity)
az cognitiveservices account deployment create \
  --name my-aoai -g rg-ai \
  --deployment-name gpt-4o-ptu \
  --model-name gpt-4o \
  --model-version "2024-08-06" \
  --model-format OpenAI \
  --sku-name ProvisionedManaged \
  --sku-capacity 50  # 50 PTUs

# PTU pricing: ~$2/hour per PTU (varies by model)
# 50 PTUs ≈ 300 requests/min for GPT-4o
```

---

## Next: [Azure AI Search →](02_AI_Search.md)
