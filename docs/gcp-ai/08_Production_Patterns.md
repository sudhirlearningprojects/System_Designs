# 8. Production Patterns for GCP AI

## Enterprise Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                 ENTERPRISE AI ARCHITECTURE (GCP)                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │ Cloud Run /  │───►│ Apigee       │───►│ Vertex AI        │  │
│  │ GKE          │    │ (API mgmt,   │    │ (Gemini)         │  │
│  │              │    │  rate limit)  │    │                  │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│         │                    │                     │             │
│         │              ┌─────▼──────┐        ┌────▼─────┐      │
│         │              │ Safety     │        │ Vector   │      │
│         │              │ Filters    │        │ Search   │      │
│         │              └────────────┘        └──────────┘      │
│         │                                                       │
│  ┌──────▼──────────────────────────────────────────────────┐   │
│  │ VPC Service Controls (data perimeter)                    │   │
│  │ Private Google Access (no public internet)               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ MONITORING: Cloud Monitoring + Cloud Trace + Cloud Logging│   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## VPC Service Controls (Data Perimeter)

```bash
# Create service perimeter (prevents data exfiltration)
gcloud access-context-manager perimeters create ai-perimeter \
  --title="AI Services Perimeter" \
  --resources="projects/my-project" \
  --restricted-services="aiplatform.googleapis.com,storage.googleapis.com" \
  --access-levels="accessPolicies/POLICY_ID/accessLevels/corp-network" \
  --policy=POLICY_ID

# Only requests from corporate network can access Vertex AI
# Data cannot leave the perimeter (even to other GCP projects)
```

---

## Cost Optimization

```python
from google.cloud import monitoring_v3
from google.protobuf import timestamp_pb2
import time

# Track Vertex AI costs via Cloud Monitoring
client = monitoring_v3.MetricServiceClient()
project_name = f"projects/my-project"

# Query token usage
interval = monitoring_v3.TimeInterval({
    "end_time": {"seconds": int(time.time())},
    "start_time": {"seconds": int(time.time()) - 86400},  # Last 24h
})

results = client.list_time_series(
    request={
        "name": project_name,
        "filter": 'metric.type = "aiplatform.googleapis.com/prediction/online/token_count"',
        "interval": interval,
        "view": monitoring_v3.ListTimeSeriesRequest.TimeSeriesView.FULL,
    }
)

for series in results:
    for point in series.points:
        print(f"Tokens: {point.value.int64_value}")
```

### Cost Strategies

| Strategy | Savings | How |
|----------|---------|-----|
| Context caching | 75% on cached tokens | Cache system prompt + static docs |
| Gemini Flash (vs Pro) | 90% | Use Flash for simple tasks |
| Batch predictions | 50% | Non-real-time processing |
| Committed use | 20-40% | 1-year commitment on compute |
| Shorter prompts | 20-40% | Optimize prompt length |

---

## Monitoring

```python
from google.cloud import logging as cloud_logging

# Structured logging for AI requests
logger = cloud_logging.Client().logger("ai-agent")

def log_ai_request(query, response, model, tokens, latency_ms, cost):
    logger.log_struct({
        "severity": "INFO",
        "message": "ai_request",
        "model": model,
        "tokens_in": tokens["input"],
        "tokens_out": tokens["output"],
        "latency_ms": latency_ms,
        "cost_usd": cost,
        "query_length": len(query),
        "response_length": len(response),
    })

# Cloud Monitoring custom metrics
from google.cloud import monitoring_v3

def record_metric(metric_type, value, labels=None):
    client = monitoring_v3.MetricServiceClient()
    series = monitoring_v3.TimeSeries()
    series.metric.type = f"custom.googleapis.com/ai/{metric_type}"
    if labels:
        for k, v in labels.items():
            series.metric.labels[k] = v
    series.resource.type = "global"
    
    point = monitoring_v3.Point()
    point.value.double_value = value
    point.interval.end_time.seconds = int(time.time())
    series.points = [point]
    
    client.create_time_series(name=f"projects/my-project", time_series=[series])

# Usage
record_metric("latency", 2100, {"model": "gemini-flash"})
record_metric("cost", 0.003, {"model": "gemini-flash"})
record_metric("quality_score", 0.92, {"model": "gemini-flash"})
```

### Alerting

```bash
# Alert on high latency
gcloud monitoring policies create \
  --display-name="AI Latency Alert" \
  --condition-display-name="P95 > 5s" \
  --condition-filter='metric.type="custom.googleapis.com/ai/latency"' \
  --condition-threshold-value=5000 \
  --condition-threshold-comparison=COMPARISON_GT \
  --condition-threshold-duration=300s \
  --notification-channels="projects/my-project/notificationChannels/CHANNEL_ID"
```

---

## Deployment on Cloud Run

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8080
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
```

```bash
# Deploy AI agent as Cloud Run service
gcloud run deploy ai-agent \
  --source=. \
  --region=us-central1 \
  --service-account=vertex-ai-sa@my-project.iam.gserviceaccount.com \
  --set-env-vars="PROJECT_ID=my-project,LOCATION=us-central1" \
  --min-instances=1 \
  --max-instances=100 \
  --memory=2Gi \
  --cpu=2 \
  --concurrency=80 \
  --ingress=internal  # Only accessible from VPC
```

---

## Production Checklist

### Security
- [ ] VPC Service Controls (data perimeter)
- [ ] Private Google Access (no public internet for AI calls)
- [ ] Service account with least-privilege IAM roles
- [ ] CMEK (Customer-Managed Encryption Keys) for data at rest
- [ ] Cloud Audit Logs enabled for all AI API calls
- [ ] Safety filters configured on Gemini

### Reliability
- [ ] Multi-region deployment (us-central1 + europe-west1)
- [ ] Cloud Run with min-instances > 0 (no cold starts)
- [ ] Retry logic with exponential backoff
- [ ] Fallback model (Flash if Pro is unavailable)
- [ ] Context caching for consistent performance

### Cost
- [ ] Gemini Flash for simple tasks (90% cheaper than Pro)
- [ ] Context caching for repeated system prompts
- [ ] Batch predictions for non-real-time workloads
- [ ] Budget alerts in Cloud Billing
- [ ] Token usage monitoring and optimization

### Monitoring
- [ ] Cloud Logging (structured JSON logs)
- [ ] Cloud Monitoring (custom metrics: tokens, latency, cost)
- [ ] Cloud Trace (distributed tracing)
- [ ] Alerting policies (latency, errors, cost)
- [ ] Dashboard in Cloud Monitoring
