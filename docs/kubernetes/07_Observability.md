# 7. Observability

## Three Pillars of Observability

```
┌─────────────────────────────────────────────────────────────┐
│                      OBSERVABILITY                            │
├───────────────────┬───────────────────┬─────────────────────┤
│     METRICS       │     LOGGING       │     TRACING         │
│   (Prometheus)    │   (Loki/EFK)      │   (Jaeger/Tempo)    │
│                   │                   │                     │
│ • What happened?  │ • Why it happened │ • Where it happened │
│ • Aggregated data │ • Event details   │ • Request flow      │
│ • Time-series     │ • Structured logs │ • Latency breakdown │
│ • Alerting        │ • Search/filter   │ • Dependencies      │
└───────────────────┴───────────────────┴─────────────────────┘
```

---

## Metrics (Prometheus + Grafana)

### Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   App Pods   │     │  Prometheus  │     │   Grafana    │
│  /metrics    │◄────│   (scrape)   │────►│ (dashboards) │
└──────────────┘     └──────┬───────┘     └──────────────┘
                            │
                     ┌──────▼───────┐
                     │ Alertmanager │
                     │ (routing)    │
                     └──────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
         ┌────▼───┐   ┌────▼───┐   ┌────▼───┐
         │ Slack  │   │PagerDuty│   │ Email  │
         └────────┘   └────────┘   └────────┘
```

### Prometheus Stack (kube-prometheus-stack)

```yaml
# ServiceMonitor - tell Prometheus what to scrape
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: payment-service
  namespace: monitoring
  labels:
    release: prometheus  # Must match Prometheus selector
spec:
  namespaceSelector:
    matchNames: ["production"]
  selector:
    matchLabels:
      app: payment-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
      scrapeTimeout: 10s
      honorLabels: true
```

### Key Metrics to Monitor

#### USE Method (Infrastructure)
- **Utilization**: % resource busy
- **Saturation**: Queue depth / waiting
- **Errors**: Error count

#### RED Method (Services)
- **Rate**: Requests per second
- **Errors**: Error rate
- **Duration**: Latency distribution

```promql
# Request rate
sum(rate(http_server_requests_seconds_count{service="payment"}[5m]))

# Error rate (%)
sum(rate(http_server_requests_seconds_count{service="payment",status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count{service="payment"}[5m])) * 100

# P99 latency
histogram_quantile(0.99, 
  sum(rate(http_server_requests_seconds_bucket{service="payment"}[5m])) by (le)
)

# Pod CPU usage vs request
sum(rate(container_cpu_usage_seconds_total{container="payment"}[5m])) by (pod)
/ sum(kube_pod_container_resource_requests{container="payment",resource="cpu"}) by (pod)

# Memory usage vs limit
sum(container_memory_working_set_bytes{container="payment"}) by (pod)
/ sum(kube_pod_container_resource_limits{container="payment",resource="memory"}) by (pod)

# Pod restart count
sum(increase(kube_pod_container_status_restarts_total{namespace="production"}[1h])) by (pod)
```

### PrometheusRule (Alerting)

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: payment-service-alerts
  namespace: monitoring
spec:
  groups:
    - name: payment-service.rules
      rules:
        # High error rate
        - alert: PaymentHighErrorRate
          expr: |
            sum(rate(http_server_requests_seconds_count{service="payment",status=~"5.."}[5m]))
            / sum(rate(http_server_requests_seconds_count{service="payment"}[5m])) > 0.05
          for: 5m
          labels:
            severity: critical
            team: payments
          annotations:
            summary: "Payment service error rate > 5%"
            description: "Error rate is {{ $value | humanizePercentage }}"
            runbook_url: "https://wiki.company.com/runbooks/payment-high-errors"

        # High latency
        - alert: PaymentHighLatency
          expr: |
            histogram_quantile(0.99,
              sum(rate(http_server_requests_seconds_bucket{service="payment"}[5m])) by (le)
            ) > 2
          for: 5m
          labels:
            severity: warning
            team: payments
          annotations:
            summary: "Payment P99 latency > 2s"

        # Pod crash looping
        - alert: PodCrashLooping
          expr: |
            increase(kube_pod_container_status_restarts_total{namespace="production"}[1h]) > 5
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Pod {{ $labels.pod }} is crash looping"

        # PVC almost full
        - alert: PVCAlmostFull
          expr: |
            kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes > 0.85
          for: 15m
          labels:
            severity: warning
          annotations:
            summary: "PVC {{ $labels.persistentvolumeclaim }} is 85% full"
```

### Alertmanager Configuration

```yaml
apiVersion: monitoring.coreos.com/v1alpha1
kind: AlertmanagerConfig
metadata:
  name: payment-alerts
  namespace: monitoring
spec:
  route:
    groupBy: ["alertname", "service"]
    groupWait: 30s
    groupInterval: 5m
    repeatInterval: 4h
    receiver: default
    routes:
      - matchers:
          - name: severity
            value: critical
        receiver: pagerduty-critical
      - matchers:
          - name: severity
            value: warning
        receiver: slack-warnings
  receivers:
    - name: default
      slackConfigs:
        - channel: "#alerts-general"
          sendResolved: true
    - name: pagerduty-critical
      pagerdutyConfigs:
        - routingKey:
            name: pagerduty-secret
            key: routing-key
          severity: critical
    - name: slack-warnings
      slackConfigs:
        - channel: "#alerts-payments"
          sendResolved: true
  inhibitRules:
    - sourceMatch:
        - name: severity
          value: critical
      targetMatch:
        - name: severity
          value: warning
      equal: ["alertname", "namespace"]
```

---

## Logging

### Structured Logging (Application)

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "ERROR",
  "service": "payment-service",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "message": "Payment processing failed",
  "error": "Connection timeout",
  "userId": "user-456",
  "paymentId": "pay-789",
  "amount": 99.99,
  "duration_ms": 5023
}
```

### Fluent Bit (Log Collection)

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluent-bit
  namespace: logging
spec:
  selector:
    matchLabels:
      app: fluent-bit
  template:
    spec:
      serviceAccountName: fluent-bit
      tolerations:
        - operator: Exists
      containers:
        - name: fluent-bit
          image: fluent/fluent-bit:2.2
          resources:
            requests:
              cpu: 100m
              memory: 128Mi
            limits:
              cpu: 200m
              memory: 256Mi
          volumeMounts:
            - name: varlog
              mountPath: /var/log
            - name: config
              mountPath: /fluent-bit/etc/
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
        - name: config
          configMap:
            name: fluent-bit-config
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluent-bit-config
  namespace: logging
data:
  fluent-bit.conf: |
    [SERVICE]
        Flush         5
        Log_Level     info
        Parsers_File  parsers.conf

    [INPUT]
        Name              tail
        Path              /var/log/containers/*.log
        Parser            cri
        Tag               kube.*
        Mem_Buf_Limit     50MB
        Skip_Long_Lines   On
        Refresh_Interval  10

    [FILTER]
        Name                kubernetes
        Match               kube.*
        Kube_URL            https://kubernetes.default.svc:443
        Kube_Tag_Prefix     kube.var.log.containers.
        Merge_Log           On
        K8S-Logging.Parser  On
        K8S-Logging.Exclude On

    [FILTER]
        Name    grep
        Match   kube.*
        Exclude $kubernetes['namespace_name'] kube-system

    [OUTPUT]
        Name            loki
        Match           kube.*
        Host            loki-gateway.logging.svc
        Port            80
        Labels          job=fluent-bit,namespace=$kubernetes['namespace_name'],app=$kubernetes['labels']['app']
        Auto_Kubernetes_Labels On
```

### Grafana Loki (Log Aggregation)

```yaml
# Loki query examples (LogQL)

# All errors from payment service
{namespace="production", app="payment-service"} |= "ERROR"

# JSON parsing + filtering
{namespace="production"} | json | level="ERROR" | duration_ms > 5000

# Rate of errors
sum(rate({namespace="production", app="payment-service"} |= "ERROR" [5m]))

# Top 10 error messages
topk(10, sum by (message) (
  count_over_time({namespace="production"} | json | level="ERROR" [1h])
))
```

---

## Distributed Tracing

### OpenTelemetry Collector

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: otel-collector
  namespace: monitoring
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: collector
          image: otel/opentelemetry-collector-contrib:0.90.0
          ports:
            - containerPort: 4317  # gRPC OTLP
            - containerPort: 4318  # HTTP OTLP
          volumeMounts:
            - name: config
              mountPath: /etc/otelcol
      volumes:
        - name: config
          configMap:
            name: otel-config
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: otel-config
data:
  config.yaml: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
          http:
            endpoint: 0.0.0.0:4318

    processors:
      batch:
        timeout: 5s
        send_batch_size: 1000
      memory_limiter:
        check_interval: 1s
        limit_mib: 512
      tail_sampling:
        policies:
          - name: errors
            type: status_code
            status_code: {status_codes: [ERROR]}
          - name: slow-traces
            type: latency
            latency: {threshold_ms: 2000}
          - name: probabilistic
            type: probabilistic
            probabilistic: {sampling_percentage: 10}

    exporters:
      otlp:
        endpoint: tempo.monitoring.svc:4317
        tls:
          insecure: true
      prometheus:
        endpoint: 0.0.0.0:8889

    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [memory_limiter, tail_sampling, batch]
          exporters: [otlp]
        metrics:
          receivers: [otlp]
          processors: [memory_limiter, batch]
          exporters: [prometheus]
```

### Auto-Instrumentation (OpenTelemetry Operator)

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: auto-instrumentation
  namespace: production
spec:
  exporter:
    endpoint: http://otel-collector.monitoring:4317
  propagators:
    - tracecontext
    - baggage
  sampler:
    type: parentbased_traceidratio
    argument: "0.1"  # 10% sampling
  java:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-java:latest
---
# Annotate deployment for auto-instrumentation
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-java: "true"
```

---

## Dashboards (Grafana)

### Essential Dashboards

1. **Cluster Overview**: Node count, CPU/memory utilization, pod count
2. **Namespace Overview**: Resource usage per namespace
3. **Workload Dashboard**: Per-deployment metrics (replicas, restarts, resource usage)
4. **Service Dashboard**: RED metrics per service
5. **Node Dashboard**: Per-node CPU, memory, disk, network
6. **PVC Dashboard**: Storage utilization and IOPS

### SLO Dashboard (Example)

```promql
# Availability SLO (99.9%)
1 - (
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[30d]))
  / sum(rate(http_server_requests_seconds_count[30d]))
)

# Error budget remaining
(1 - 0.999) - (
  sum(increase(http_server_requests_seconds_count{status=~"5.."}[30d]))
  / sum(increase(http_server_requests_seconds_count[30d]))
)

# Latency SLO (99% of requests < 500ms)
sum(rate(http_server_requests_seconds_bucket{le="0.5"}[30d]))
/ sum(rate(http_server_requests_seconds_count[30d]))
```

---

## Kubernetes-Specific Metrics

### Must-Have Alerts

| Alert | Expression | Severity |
|-------|-----------|----------|
| Node NotReady | `kube_node_status_condition{condition="Ready",status="true"} == 0` | Critical |
| Pod CrashLoop | `increase(kube_pod_container_status_restarts_total[1h]) > 5` | Critical |
| PVC Full | `kubelet_volume_stats_used_bytes/capacity > 0.9` | Warning |
| Deployment Unavailable | `kube_deployment_status_replicas_unavailable > 0` for 15m | Warning |
| HPA at Max | `kube_horizontalpodautoscaler_status_current_replicas == kube_horizontalpodautoscaler_spec_max_replicas` | Warning |
| Certificate Expiry | `certmanager_certificate_expiration_timestamp_seconds - time() < 7*24*3600` | Warning |
| etcd Leader Changes | `increase(etcd_server_leader_changes_seen_total[1h]) > 3` | Critical |

---

## Observability Stack Comparison

| Component | Option A | Option B |
|-----------|----------|----------|
| Metrics | Prometheus | Amazon Managed Prometheus |
| Dashboards | Grafana | Amazon Managed Grafana |
| Logging | Loki | CloudWatch Logs / OpenSearch |
| Tracing | Tempo/Jaeger | AWS X-Ray |
| Collection | OpenTelemetry | ADOT (AWS Distro for OTel) |

---

## Next: [CI/CD & GitOps →](08_CICD_and_GitOps.md)
