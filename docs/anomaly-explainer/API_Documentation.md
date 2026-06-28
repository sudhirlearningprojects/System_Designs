# API Documentation

## Base URL
```
http://localhost:8200/api/v1
```

---

## Endpoints

### 1. Explain Anomaly (Manual Trigger)

```
POST /api/v1/explain
```

Manually submit an anomaly for explanation. Useful for testing or integrating with existing alerting systems.

**Request Body:**
```json
{
  "anomaly_id": "anom-payment-20240115103015",
  "timestamp": "2024-01-15T10:30:15Z",
  "service": "payment-service",
  "severity": "HIGH",
  "anomaly_type": "CPU_SPIKE",
  "primary_metric": "cpu_percent",
  "current_value": 95.2,
  "baseline_mean": 45.0,
  "baseline_stddev": 8.5,
  "z_score": 5.9,
  "correlated_signals": [
    {"metric": "error_rate", "value": 0.12, "z_score": 3.2},
    {"metric": "latency_p99_ms", "value": 3200, "z_score": 4.1}
  ],
  "all_metrics": {
    "cpu_percent": 95.2,
    "memory_percent": 72.3,
    "error_rate": 0.12,
    "latency_p99_ms": 3200
  },
  "labels": {
    "region": "us-east-1",
    "env": "production"
  }
}
```

**Response (200):**
```json
{
  "status": "explained",
  "explanation": {
    "anomaly_id": "anom-payment-20240115103015",
    "root_cause": "Database connection pool exhaustion due to batch reconciliation job",
    "explanation": "The CPU spike matches historical pattern INC-2847 where unclosed DB connections caused pool exhaustion. Correlated latency and error spikes confirm connection starvation.",
    "immediate_fix": [
      "Restart payment-service pods to release connections",
      "Check if batch reconciliation job is running",
      "Monitor connection pool via Grafana dashboard"
    ],
    "long_term_fix": "Add try-with-resources for all DB operations, set connection pool max-wait timeout, add monitoring at 80% utilization",
    "confidence": "HIGH",
    "similar_incident_id": "uuid-of-inc-2847",
    "estimated_impact": "~15% of payment transactions failing, estimated $50K/hour revenue impact"
  }
}
```

**Response (deduplicated):**
```json
{
  "status": "deduplicated",
  "message": "Recent anomaly already explained"
}
```

---

### 2. Submit Feedback

```
POST /api/v1/feedback/{anomaly_id}?feedback=HELPFUL
```

Submit human feedback on explanation quality. Used for continuous improvement.

**Parameters:**
| Name | Type | Values |
|------|------|--------|
| anomaly_id | path | UUID of the anomaly |
| feedback | query | `HELPFUL`, `NOT_HELPFUL`, `PARTIALLY_HELPFUL` |

**Response (200):**
```json
{
  "status": "feedback_recorded"
}
```

---

### 3. Health Check

```
GET /api/v1/health
```

**Response (200):**
```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Kafka Topics

### Input: `metrics.raw`

| Field | Type | Description |
|-------|------|-------------|
| timestamp | ISO8601 | Metric collection time |
| service | string | Service name |
| instance | string | Pod/instance ID |
| metrics.cpu_percent | float | CPU utilization |
| metrics.memory_percent | float | Memory utilization |
| metrics.error_rate | float | Error rate (0-1) |
| metrics.latency_p99_ms | float | p99 latency in ms |
| metrics.request_count | int | Requests in interval |
| labels | object | Metadata (region, env) |

### Output: `anomalies.detected`

| Field | Type | Description |
|-------|------|-------------|
| anomaly_id | string | Unique anomaly ID |
| timestamp | ISO8601 | Detection time |
| service | string | Affected service |
| severity | enum | CRITICAL/HIGH/MEDIUM/LOW |
| anomaly_type | enum | CPU_SPIKE/MEMORY_SPIKE/ERROR_SPIKE/LATENCY_SPIKE |
| z_score | float | Statistical deviation |
| correlated_signals | array | Other anomalous metrics |

---

## Integration Examples

### With Prometheus AlertManager

```yaml
# alertmanager webhook receiver
receivers:
  - name: anomaly-explainer
    webhook_configs:
      - url: http://anomaly-explainer:8200/api/v1/explain
        send_resolved: false
```

### With Datadog Webhooks

```json
{
  "url": "http://anomaly-explainer:8200/api/v1/explain",
  "payload": {
    "anomaly_id": "$ALERT_ID",
    "service": "$SERVICE",
    "anomaly_type": "$ALERT_TYPE",
    "current_value": "$VALUE",
    "severity": "$PRIORITY"
  }
}
```
