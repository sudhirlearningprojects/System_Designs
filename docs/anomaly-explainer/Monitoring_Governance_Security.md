# Monitoring, Governance, Regulation & Security

## 1. Monitoring & Observability

### 1.1 Key Metrics to Track

**Pipeline Health:**
| Metric | Alert Threshold | Description |
|--------|----------------|-------------|
| `anomaly.detection.latency_p99` | > 5s | Time from metric ingestion to anomaly emit |
| `rag.explanation.latency_p99` | > 15s | Time from anomaly to explanation |
| `kafka.consumer.lag` | > 10K messages | Flink consumer falling behind |
| `vector_search.latency_p99` | > 200ms | Vector DB query time |
| `llm.request.latency_p99` | > 10s | LLM API response time |
| `llm.request.error_rate` | > 5% | LLM failures |
| `explanation.accuracy` | < 70% | Based on human feedback |
| `false_positive.rate` | > 10% | Anomalies that weren't real issues |

**Business Metrics:**
| Metric | Target | Description |
|--------|--------|-------------|
| MTTR reduction | 40-60% | Compared to baseline without tool |
| Explanation helpfulness | > 80% | From feedback loop |
| Coverage | > 90% | % of incidents with explanations |
| Engineer satisfaction | > 4/5 | Quarterly survey |

### 1.2 Prometheus Metrics

```python
# metrics.py
from prometheus_client import Counter, Histogram, Gauge

# Anomaly detection
anomalies_detected = Counter(
    "anomalies_detected_total", "Total anomalies detected", ["service", "type", "severity"]
)
detection_latency = Histogram(
    "anomaly_detection_latency_seconds", "Detection latency", buckets=[0.1, 0.5, 1, 2, 5, 10]
)

# RAG pipeline
explanation_latency = Histogram(
    "explanation_generation_latency_seconds", "E2E explanation time", buckets=[1, 2, 5, 10, 15, 30]
)
vector_search_latency = Histogram(
    "vector_search_latency_seconds", "Vector DB query time", buckets=[0.01, 0.05, 0.1, 0.2, 0.5]
)
llm_requests = Counter("llm_requests_total", "LLM API calls", ["model", "status"])
llm_tokens_used = Counter("llm_tokens_used_total", "Tokens consumed", ["model", "type"])

# Quality
explanation_feedback = Counter(
    "explanation_feedback_total", "Human feedback", ["rating"]  # HELPFUL, NOT_HELPFUL
)
confidence_distribution = Counter(
    "explanation_confidence_total", "Confidence levels", ["level"]  # HIGH, MEDIUM, LOW
)

# System health
kafka_consumer_lag = Gauge("kafka_consumer_lag", "Consumer lag", ["topic", "partition"])
active_anomalies = Gauge("active_anomalies_count", "Currently unresolved anomalies", ["service"])
```

### 1.3 Grafana Dashboards

**Dashboard 1: Pipeline Overview**
```
┌─────────────────────────────────────────────────────────┐
│  Anomaly Explainer - Pipeline Health                     │
├──────────────────┬──────────────────┬───────────────────┤
│ Detection Rate   │ Explanation Rate  │ E2E Latency p99  │
│ 12 anomalies/hr  │ 11 explained/hr  │ 8.2 seconds      │
├──────────────────┴──────────────────┴───────────────────┤
│                                                          │
│  [Kafka Lag Graph]  [LLM Latency Graph]  [Error Rate]  │
│                                                          │
├──────────────────────────────────────────────────────────┤
│  Top Services: payment(4) > auth(3) > order(2) > ...   │
│  Confidence: HIGH(65%) MEDIUM(25%) LOW(10%)             │
│  Feedback: Helpful(82%) Not Helpful(8%) No Response(10%)│
└──────────────────────────────────────────────────────────┘
```

**Dashboard 2: RAG Quality**
```
┌─────────────────────────────────────────────────────────┐
│  RAG Quality & Accuracy                                  │
├──────────────────┬──────────────────┬───────────────────┤
│ Avg Similarity   │ Knowledge Base   │ Feedback Score    │
│ Score: 0.84      │ Size: 847 docs   │ 4.2/5            │
├──────────────────┴──────────────────┴───────────────────┤
│  [Similarity Score Distribution]                        │
│  [Confidence vs Feedback Correlation]                   │
│  [Most Retrieved Incidents - Top 10]                    │
│  [Gaps: Anomalies with LOW confidence]                  │
└──────────────────────────────────────────────────────────┘
```

### 1.4 Distributed Tracing

```python
# Using OpenTelemetry
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider

tracer = trace.get_tracer("anomaly-explainer")

async def process_anomaly(anomaly: AnomalyEvent):
    with tracer.start_as_current_span("process_anomaly") as span:
        span.set_attribute("service", anomaly.service)
        span.set_attribute("anomaly_type", anomaly.anomaly_type)
        span.set_attribute("severity", anomaly.severity)

        with tracer.start_as_current_span("vector_search"):
            similar = search_similar_incidents(anomaly)
            span.set_attribute("results_count", len(similar))
            span.set_attribute("top_similarity", similar[0]["similarity"] if similar else 0)

        with tracer.start_as_current_span("llm_generation"):
            explanation = generate_explanation(anomaly, similar, runbooks)
            span.set_attribute("confidence", explanation.confidence)
            span.set_attribute("tokens_used", response.usage.total_tokens)

        with tracer.start_as_current_span("notification"):
            send_slack_notification(anomaly, explanation)
```

### 1.5 Alerting Rules

```yaml
# prometheus-alerts.yml
groups:
  - name: anomaly-explainer
    rules:
      - alert: HighExplanationLatency
        expr: histogram_quantile(0.99, explanation_generation_latency_seconds_bucket) > 15
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Explanation generation too slow (p99 > 15s)"

      - alert: LLMErrorRate
        expr: rate(llm_requests_total{status="error"}[5m]) / rate(llm_requests_total[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "LLM error rate > 5%"

      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Anomaly detector falling behind"

      - alert: LowExplanationQuality
        expr: rate(explanation_feedback_total{rating="NOT_HELPFUL"}[1h]) / rate(explanation_feedback_total[1h]) > 0.2
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "Explanation quality degrading (>20% negative feedback)"
```

---

## 2. Governance

### 2.1 Data Governance

**Data Lineage:**
```
Raw Metrics → Kafka → Flink (transformed) → Anomaly Event → Vector Search → LLM → Explanation
     │                    │                       │                │              │
     ▼                    ▼                       ▼                ▼              ▼
  [Retained 7d]    [State checkpointed]    [Stored in DB]   [Cached 1hr]   [Stored + audited]
```

**Data Classification:**
| Data | Classification | Retention | Access |
|------|---------------|-----------|--------|
| Raw metrics | Internal | 7 days | Engineering teams |
| Anomaly events | Internal | 90 days | Engineering + SRE |
| Incident KB | Confidential | Indefinite | SRE + selected teams |
| Explanations | Internal | 1 year | All engineering |
| LLM prompts/responses | Confidential | 30 days | Platform team only |
| Feedback data | Internal | Indefinite | ML/Platform team |

### 2.2 AI/LLM Governance

**Prompt Management:**
- Version-controlled prompts in Git
- A/B testing framework for prompt changes
- Prompt review process (PR-based)
- No customer PII in prompts (only service names + metric values)

**Model Governance:**
| Policy | Implementation |
|--------|---------------|
| Model versioning | Pin LLM model version, test before upgrade |
| Output validation | JSON schema validation on LLM output |
| Hallucination prevention | RAG grounding + confidence scoring |
| Cost tracking | Token usage per explanation, daily budget caps |
| Bias monitoring | Review explanations for consistent patterns |

**LLM Output Guardrails:**
```python
def validate_explanation(raw_output: str) -> dict:
    """Validate LLM output meets quality standards."""
    try:
        result = json.loads(raw_output)
    except json.JSONDecodeError:
        raise ValueError("LLM returned non-JSON output")

    required_fields = ["root_cause", "explanation", "immediate_fix", "confidence"]
    for field in required_fields:
        if field not in result:
            raise ValueError(f"Missing required field: {field}")

    if result["confidence"] not in ("HIGH", "MEDIUM", "LOW"):
        result["confidence"] = "LOW"

    if len(result["root_cause"]) > 500:
        result["root_cause"] = result["root_cause"][:500]

    # Block if explanation references non-existent services
    # Block if explanation contains potential PII
    return result
```

### 2.3 Knowledge Base Governance

**Ingestion Process:**
```
Incident Resolved → Post-mortem Written → Review & Approve → Embed → Index in Vector DB
                                              │
                                     (Quality gate: must have
                                      root_cause + resolution)
```

**Quality Metrics:**
- Coverage: % of services with incident history
- Freshness: Average age of top-retrieved incidents
- Relevance: Average similarity score of retrieved results

---

## 3. Security

### 3.1 Threat Model

| Threat | Risk | Mitigation |
|--------|------|-----------|
| Prompt injection via metric labels | Medium | Sanitize all inputs, structured prompts |
| Knowledge base poisoning | High | Review process for new entries |
| LLM data exfiltration | Medium | No sensitive data in prompts, use private endpoints |
| Unauthorized access to explanations | Low | RBAC on all endpoints |
| Kafka message tampering | Low | TLS + authentication |
| Vector DB unauthorized access | Medium | Network isolation + auth |

### 3.2 Authentication & Authorization

```python
# auth.py
from fastapi import Security, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import jwt

security = HTTPBearer()

ROLES = {
    "admin": ["explain", "feedback", "manage_kb", "view_metrics"],
    "sre": ["explain", "feedback", "view_metrics"],
    "engineer": ["explain", "feedback"],
    "readonly": ["view_metrics"],
}

def verify_token(credentials: HTTPAuthorizationCredentials = Security(security)):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=["HS256"])
        return payload
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

def require_permission(permission: str):
    def decorator(func):
        async def wrapper(*args, user=Depends(verify_token), **kwargs):
            role = user.get("role", "readonly")
            if permission not in ROLES.get(role, []):
                raise HTTPException(status_code=403, detail="Insufficient permissions")
            return await func(*args, **kwargs)
        return wrapper
    return decorator
```

### 3.3 Data Protection

```python
# sanitizer.py
import re

PII_PATTERNS = [
    (r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', '<EMAIL>'),
    (r'\b\d{3}[-.]?\d{3}[-.]?\d{4}\b', '<PHONE>'),
    (r'\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b', '<IP>'),
    (r'(api[_-]?key|token|secret|password)\s*[:=]\s*\S+', '<REDACTED_CREDENTIAL>'),
]

def sanitize_for_llm(text: str) -> str:
    """Remove PII and secrets before sending to LLM."""
    for pattern, replacement in PII_PATTERNS:
        text = re.sub(pattern, replacement, text, flags=re.IGNORECASE)
    return text
```

### 3.4 Network Security

```yaml
# Kubernetes NetworkPolicy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: anomaly-explainer-policy
spec:
  podSelector:
    matchLabels:
      app: anomaly-explainer
  policyTypes: [Ingress, Egress]
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
      ports:
        - port: 8200
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - port: 5432
    - to:
        - podSelector:
            matchLabels:
              app: redis
      ports:
        - port: 6379
    - to:  # LLM API (external)
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - port: 443
```

### 3.5 Secrets Management

```yaml
# Use AWS Secrets Manager or HashiCorp Vault
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: anomaly-explainer-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: anomaly-explainer-secrets
  data:
    - secretKey: OPENAI_API_KEY
      remoteRef:
        key: /prod/anomaly-explainer/openai-key
    - secretKey: POSTGRES_PASSWORD
      remoteRef:
        key: /prod/anomaly-explainer/db-password
```

### 3.6 Audit Logging

```python
# audit.py
import structlog

audit_log = structlog.get_logger("audit")

def log_explanation_request(anomaly: AnomalyEvent, user: dict):
    audit_log.info(
        "explanation_requested",
        anomaly_id=anomaly.anomaly_id,
        service=anomaly.service,
        user_id=user["sub"],
        user_role=user["role"],
    )

def log_kb_modification(action: str, document_id: str, user: dict):
    audit_log.info(
        "knowledge_base_modified",
        action=action,  # CREATE, UPDATE, DELETE
        document_id=document_id,
        user_id=user["sub"],
        user_role=user["role"],
    )

def log_llm_interaction(prompt_hash: str, tokens: int, model: str):
    audit_log.info(
        "llm_interaction",
        prompt_hash=prompt_hash,  # Hash, not full prompt
        tokens_used=tokens,
        model=model,
    )
```

---

## 4. Compliance & Regulation

### 4.1 AI Compliance Checklist

| Requirement | Implementation |
|-------------|---------------|
| Transparency | Explain which incidents influenced the answer |
| Traceability | Full audit trail of every explanation generated |
| Human oversight | Feedback loop, ability to override/correct |
| Data minimization | Only metric values + service names to LLM |
| Right to explanation | Show similarity scores + retrieved context |
| Bias detection | Monitor explanation patterns across teams |

### 4.2 SOC 2 Alignment

| Control | Implementation |
|---------|---------------|
| Access control | RBAC + JWT authentication |
| Encryption at rest | Postgres TDE, encrypted S3 |
| Encryption in transit | TLS everywhere |
| Audit logging | All API calls + LLM interactions logged |
| Change management | GitOps, PR reviews for prompt changes |
| Incident response | Self-monitoring (alerting on the alerting system) |

### 4.3 Cost Governance

```python
# cost_tracker.py
COST_PER_1K_TOKENS = {
    "gpt-4o": {"input": 0.005, "output": 0.015},
    "gpt-4o-mini": {"input": 0.00015, "output": 0.0006},
    "text-embedding-3-small": {"input": 0.00002},
}

DAILY_BUDGET = 50.0  # $50/day cap

class CostTracker:
    def __init__(self):
        self.daily_cost = 0.0

    def track(self, model: str, input_tokens: int, output_tokens: int):
        costs = COST_PER_1K_TOKENS[model]
        cost = (input_tokens / 1000 * costs["input"]) + (output_tokens / 1000 * costs.get("output", 0))
        self.daily_cost += cost

        if self.daily_cost > DAILY_BUDGET:
            raise BudgetExceededError(f"Daily budget ${DAILY_BUDGET} exceeded")

    def should_use_cheaper_model(self) -> bool:
        return self.daily_cost > DAILY_BUDGET * 0.8  # Switch at 80%
```
