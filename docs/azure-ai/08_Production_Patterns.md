# 8. Production Patterns for Azure AI

## Enterprise Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                 ENTERPRISE AI ARCHITECTURE (Azure)                │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │ App Service  │───►│ API Mgmt     │───►│ Azure OpenAI     │  │
│  │ / AKS        │    │ (rate limit, │    │ (GPT-4o)         │  │
│  │              │    │  auth, cache) │    │                  │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│         │                    │                     │             │
│         │              ┌─────▼──────┐        ┌────▼─────┐      │
│         │              │ Content    │        │ AI Search│      │
│         │              │ Safety     │        │ (RAG)    │      │
│         │              └────────────┘        └──────────┘      │
│         │                                                       │
│  ┌──────▼──────────────────────────────────────────────────┐   │
│  │ NETWORKING: Private Endpoints + VNet Integration         │   │
│  │ All AI services on private network (no public internet)  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ MONITORING: App Insights + Log Analytics + Azure Monitor  │   │
│  │ Custom metrics: tokens, latency, cost, quality            │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Private Networking

```bash
# Create VNet
az network vnet create --name ai-vnet -g rg-ai --address-prefix 10.0.0.0/16
az network vnet subnet create --name ai-subnet --vnet-name ai-vnet -g rg-ai --address-prefix 10.0.1.0/24

# Create private endpoint for Azure OpenAI
az network private-endpoint create \
  --name pe-openai \
  --resource-group rg-ai \
  --vnet-name ai-vnet \
  --subnet ai-subnet \
  --private-connection-resource-id $(az cognitiveservices account show --name my-aoai -g rg-ai --query id -o tsv) \
  --group-id account \
  --connection-name openai-connection

# Create private DNS zone
az network private-dns zone create -g rg-ai --name "privatelink.openai.azure.com"
az network private-dns link vnet create -g rg-ai \
  --zone-name "privatelink.openai.azure.com" \
  --name openai-dns-link \
  --virtual-network ai-vnet \
  --registration-enabled false

# Disable public access
az cognitiveservices account update --name my-aoai -g rg-ai \
  --public-network-access Disabled
```

---

## API Management (Gateway)

```bash
# Create APIM instance
az apim create --name my-ai-apim -g rg-ai --publisher-email admin@company.com \
  --publisher-name "Company" --sku-name Developer

# Import Azure OpenAI as backend
# APIM provides: rate limiting, caching, auth, load balancing across regions
```

### Multi-Region Load Balancing

```xml
<!-- APIM policy: Round-robin across Azure OpenAI instances -->
<policies>
  <inbound>
    <set-variable name="region" value="@{
      var regions = new[] { "eastus", "westus", "northeurope" };
      return regions[new Random().Next(regions.Length)];
    }" />
    <set-backend-service base-url="@($"https://aoai-{context.Variables["region"]}.openai.azure.com/")" />
    <retry condition="@(context.Response.StatusCode == 429)" count="3" interval="10">
      <!-- Retry on different region if rate limited -->
      <set-variable name="region" value="@{
        var regions = new[] { "eastus", "westus", "northeurope" };
        return regions[new Random().Next(regions.Length)];
      }" />
      <set-backend-service base-url="@($"https://aoai-{context.Variables["region"]}.openai.azure.com/")" />
    </retry>
  </inbound>
</policies>
```

---

## Cost Optimization

### Token Tracking

```python
import logging
from openai import AzureOpenAI

class CostTrackingClient:
    """Wrapper that tracks token usage and cost."""
    
    PRICING = {  # Per 1M tokens
        "gpt-4o": {"input": 2.50, "output": 10.00},
        "gpt-4o-mini": {"input": 0.15, "output": 0.60},
        "text-embedding-3-small": {"input": 0.02},
    }
    
    def __init__(self, client: AzureOpenAI):
        self.client = client
        self.total_cost = 0.0
        self.total_tokens = 0
    
    def chat(self, model: str, messages: list, **kwargs) -> dict:
        response = self.client.chat.completions.create(model=model, messages=messages, **kwargs)
        
        # Track cost
        pricing = self.PRICING.get(model, {"input": 5.0, "output": 15.0})
        cost = (
            response.usage.prompt_tokens * pricing["input"] / 1_000_000 +
            response.usage.completion_tokens * pricing["output"] / 1_000_000
        )
        self.total_cost += cost
        self.total_tokens += response.usage.total_tokens
        
        logging.info(f"Model={model}, Tokens={response.usage.total_tokens}, Cost=${cost:.4f}, Total=${self.total_cost:.2f}")
        
        return response
```

### Cost Reduction Strategies

```python
# 1. Model tiering (use cheapest model that works)
def select_model(query: str) -> str:
    if len(query) < 50 and is_simple_question(query):
        return "gpt-4o-mini"  # 20x cheaper
    return "gpt-4o"

# 2. Response caching (Azure Redis Cache)
import redis
cache = redis.Redis(host="my-cache.redis.cache.windows.net", port=6380, ssl=True)

def cached_completion(model, messages, ttl=3600):
    cache_key = f"aoai:{hash(str(messages))}"
    cached = cache.get(cache_key)
    if cached:
        return json.loads(cached)
    
    response = client.chat.completions.create(model=model, messages=messages)
    cache.setex(cache_key, ttl, json.dumps(response.model_dump()))
    return response

# 3. Prompt optimization (shorter prompts = fewer tokens)
# Use concise system prompts, remove redundant instructions
```

---

## Monitoring with Application Insights

```python
from opencensus.ext.azure.log_exporter import AzureLogHandler
from opencensus.ext.azure import metrics_exporter
import logging

# Setup Application Insights
logger = logging.getLogger(__name__)
logger.addHandler(AzureLogHandler(connection_string="InstrumentationKey=..."))

# Custom metrics
def track_ai_request(model, tokens, latency_ms, cost, success):
    logger.info("ai_request", extra={
        "custom_dimensions": {
            "model": model,
            "tokens": tokens,
            "latency_ms": latency_ms,
            "cost_usd": cost,
            "success": success,
        }
    })

# KQL queries in Log Analytics:
# Average latency by model
# customEvents | where name == "ai_request" | summarize avg(todouble(customDimensions.latency_ms)) by tostring(customDimensions.model)

# Daily cost
# customEvents | where name == "ai_request" | summarize sum(todouble(customDimensions.cost_usd)) by bin(timestamp, 1d)
```

---

## Managed Identity (No Keys!)

```python
# Production: Use Managed Identity instead of API keys
from azure.identity import DefaultAzureCredential, get_bearer_token_provider

# Works automatically in Azure (App Service, AKS, Functions, VMs)
credential = DefaultAzureCredential()
token_provider = get_bearer_token_provider(credential, "https://cognitiveservices.azure.com/.default")

client = AzureOpenAI(
    azure_endpoint="https://my-aoai.openai.azure.com/",
    azure_ad_token_provider=token_provider,
    api_version="2024-10-21",
)

# Assign role (one-time setup)
# az role assignment create \
#   --assignee <app-managed-identity-id> \
#   --role "Cognitive Services OpenAI User" \
#   --scope /subscriptions/.../resourceGroups/.../providers/Microsoft.CognitiveServices/accounts/my-aoai
```

---

## Disaster Recovery

```bash
# Deploy Azure OpenAI in multiple regions
# Region 1: East US (primary)
# Region 2: West US (secondary)
# Region 3: North Europe (tertiary)

# Use APIM or Traffic Manager for failover
# If East US returns 429/503 → route to West US → route to North Europe

# Data replication:
# AI Search: Geo-replicated index (built-in)
# Blob Storage: GRS (geo-redundant storage) for documents
```

---

## Production Checklist

### Security
- [ ] Private endpoints for all AI services (no public access)
- [ ] Managed Identity (no API keys in code)
- [ ] Content Safety enabled with appropriate thresholds
- [ ] Prompt shields enabled (jailbreak detection)
- [ ] Network Security Groups (NSG) restricting traffic
- [ ] Key Vault for any remaining secrets
- [ ] RBAC with least-privilege roles

### Reliability
- [ ] Multi-region deployment (failover)
- [ ] APIM with retry policies
- [ ] Provisioned Throughput (PTU) for predictable latency
- [ ] Circuit breaker pattern in application code
- [ ] Health check endpoints

### Cost
- [ ] Model tiering (GPT-4o-mini for simple tasks)
- [ ] Response caching (Redis)
- [ ] Token budgets and alerts
- [ ] Prompt optimization (shorter = cheaper)
- [ ] Batch API for non-real-time workloads

### Monitoring
- [ ] Application Insights for custom AI metrics
- [ ] Azure Monitor alerts (latency, errors, cost)
- [ ] Log Analytics for query analysis
- [ ] Content Safety dashboard
- [ ] Token usage tracking per user/feature
