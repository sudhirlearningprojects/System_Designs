# 6. Production Best Practices

## High Availability Architecture

### Control Plane HA

```
┌─────────────────────────────────────────────────────────────┐
│                    LOAD BALANCER                              │
│              (NLB / HAProxy / keepalived)                     │
└──────────┬──────────────────┬──────────────────┬────────────┘
           │                  │                  │
┌──────────▼────┐  ┌─────────▼─────┐  ┌────────▼──────┐
│  API Server 1 │  │  API Server 2 │  │  API Server 3 │
│  Scheduler    │  │  Scheduler    │  │  Scheduler    │
│  Ctrl Manager │  │  (standby)    │  │  (standby)    │
│  etcd         │  │  etcd         │  │  etcd         │
└───────────────┘  └───────────────┘  └───────────────┘
     AZ-1               AZ-2               AZ-3
```

**Key HA Requirements:**
- 3+ control plane nodes across AZs
- etcd: 3 or 5 nodes (odd for quorum)
- Scheduler/Controller Manager: leader election (only 1 active)
- API Server: all active behind load balancer

### Worker Node HA

```yaml
# Spread workloads across AZs
spec:
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: topology.kubernetes.io/zone
      whenUnsatisfiable: DoNotSchedule
      labelSelector:
        matchLabels:
          app: critical-service
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              app: critical-service
          topologyKey: kubernetes.io/hostname
```

---

## Autoscaling

### Horizontal Pod Autoscaler (HPA)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payment-service-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  minReplicas: 3
  maxReplicas: 50
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 50        # Scale up by 50% at a time
          periodSeconds: 60
        - type: Pods
          value: 5         # Or add 5 pods at a time
          periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5 min before scaling down
      policies:
        - type: Percent
          value: 10        # Scale down by 10% at a time
          periodSeconds: 60
  metrics:
    # CPU-based
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    # Memory-based
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    # Custom metric (requests per second)
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
    # External metric (SQS queue depth)
    - type: External
      external:
        metric:
          name: sqs_messages_visible
          selector:
            matchLabels:
              queue: payment-queue
        target:
          type: AverageValue
          averageValue: "50"
```

### Vertical Pod Autoscaler (VPA)

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: payment-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  updatePolicy:
    updateMode: "Auto"  # Off, Initial, Recreate, Auto
  resourcePolicy:
    containerPolicies:
      - containerName: payment
        minAllowed:
          cpu: 100m
          memory: 128Mi
        maxAllowed:
          cpu: "4"
          memory: 8Gi
        controlledResources: ["cpu", "memory"]
        controlledValues: RequestsAndLimits
```

**VPA Modes:**
| Mode | Behavior |
|------|----------|
| Off | Only recommendations, no changes |
| Initial | Set resources only at Pod creation |
| Recreate | Evict and recreate Pods to apply |
| Auto | Apply changes (may evict Pods) |

### Cluster Autoscaler / Karpenter

#### Karpenter (AWS - Recommended)

```yaml
apiVersion: karpenter.sh/v1beta1
kind: NodePool
metadata:
  name: general-purpose
spec:
  template:
    spec:
      requirements:
        - key: kubernetes.io/arch
          operator: In
          values: ["amd64"]
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["on-demand", "spot"]
        - key: karpenter.k8s.aws/instance-category
          operator: In
          values: ["c", "m", "r"]
        - key: karpenter.k8s.aws/instance-generation
          operator: Gt
          values: ["5"]
      nodeClassRef:
        name: default
  limits:
    cpu: "1000"
    memory: 2000Gi
  disruption:
    consolidationPolicy: WhenUnderutilized
    consolidateAfter: 30s
    expireAfter: 720h  # Replace nodes every 30 days
---
apiVersion: karpenter.k8s.aws/v1beta1
kind: EC2NodeClass
metadata:
  name: default
spec:
  amiFamily: AL2
  subnetSelectorTerms:
    - tags:
        karpenter.sh/discovery: my-cluster
  securityGroupSelectorTerms:
    - tags:
        karpenter.sh/discovery: my-cluster
  blockDeviceMappings:
    - deviceName: /dev/xvda
      ebs:
        volumeSize: 100Gi
        volumeType: gp3
        encrypted: true
```

### KEDA (Event-Driven Autoscaling)

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: order-processor
spec:
  scaleTargetRef:
    name: order-processor
  minReplicaCount: 1
  maxReplicaCount: 100
  pollingInterval: 15
  cooldownPeriod: 300
  triggers:
    - type: kafka
      metadata:
        bootstrapServers: kafka:9092
        consumerGroup: order-group
        topic: orders
        lagThreshold: "100"
    - type: prometheus
      metadata:
        serverAddress: http://prometheus:9090
        metricName: http_requests_total
        threshold: "500"
        query: sum(rate(http_requests_total{service="order"}[2m]))
```

---

## Pod Disruption Budgets (PDB)

Ensure minimum availability during voluntary disruptions (node drain, upgrades).

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: payment-service-pdb
  namespace: production
spec:
  # Option 1: Minimum available
  minAvailable: 2        # At least 2 pods must be running
  # Option 2: Maximum unavailable
  # maxUnavailable: 1    # At most 1 pod can be down
  selector:
    matchLabels:
      app: payment-service
  unhealthyPodEvictionPolicy: IfHealthyBudget  # K8s 1.27+
```

**Rules of Thumb:**
- Critical services: `minAvailable: 2` or `maxUnavailable: 1`
- Stateful workloads: `maxUnavailable: 1`
- Batch jobs: `maxUnavailable: 50%`

---

## Graceful Shutdown

```yaml
spec:
  terminationGracePeriodSeconds: 60
  containers:
    - name: app
      lifecycle:
        preStop:
          exec:
            # Wait for load balancer to deregister
            command: ["/bin/sh", "-c", "sleep 15"]
```

**Shutdown Sequence:**
```
1. Pod marked for termination
2. Pod removed from Service endpoints (async)
3. preStop hook executes
4. SIGTERM sent to container
5. App handles SIGTERM (drain connections, finish requests)
6. After terminationGracePeriodSeconds → SIGKILL
```

**Application-side handling (Java/Spring Boot):**
```yaml
# application.yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

## Resource Right-Sizing

### Guidelines

| Workload Type | CPU Request | CPU Limit | Memory Request | Memory Limit |
|---------------|-------------|-----------|----------------|--------------|
| Web API | P95 usage | 2-4x request | P99 usage | 1.5x request |
| Background worker | Average usage | 2x request | P99 usage | 1.2x request |
| Database | P95 usage | = request | P99 usage | = request |
| Cache (Redis) | Low | Low | Expected dataset | = request |

### Determining Right Values

```bash
# Check actual usage with metrics-server
kubectl top pods -n production

# Use VPA in recommendation mode
kubectl get vpa payment-service-vpa -o jsonpath='{.status.recommendation}'
```

**Prometheus queries for right-sizing:**
```promql
# P95 CPU usage over 7 days
quantile_over_time(0.95, 
  rate(container_cpu_usage_seconds_total{container="payment"}[5m])[7d:5m]
)

# P99 memory usage over 7 days
quantile_over_time(0.99,
  container_memory_working_set_bytes{container="payment"}[7d:5m]
)
```

---

## Configuration Management

### ConfigMaps

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-config
  namespace: production
data:
  # Simple key-value
  LOG_LEVEL: "INFO"
  MAX_RETRIES: "3"
  
  # File-based config
  application.yaml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:postgresql://postgres:5432/payments
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
```

### Immutable ConfigMaps (Production)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: payment-config-v3  # Version in name
immutable: true  # Cannot be modified (prevents accidental changes)
data:
  LOG_LEVEL: "INFO"
```

### Config Reload Without Restart

**Option 1: Mounted ConfigMap (auto-updates)**
```yaml
volumeMounts:
  - name: config
    mountPath: /etc/config
# Files update automatically (kubelet sync period ~1 min)
# App must watch for file changes
```

**Option 2: Reloader (triggers rollout on ConfigMap change)**
```yaml
metadata:
  annotations:
    reloader.stakater.com/auto: "true"
```

---

## Multi-Tenancy

### Namespace-Based Isolation

```yaml
# Per-tenant namespace with quotas
apiVersion: v1
kind: Namespace
metadata:
  name: tenant-acme
  labels:
    tenant: acme
    pod-security.kubernetes.io/enforce: restricted
---
apiVersion: v1
kind: ResourceQuota
metadata:
  name: tenant-quota
  namespace: tenant-acme
spec:
  hard:
    requests.cpu: "10"
    requests.memory: 20Gi
    limits.cpu: "20"
    limits.memory: 40Gi
    pods: "50"
    services: "10"
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: tenant-isolation
  namespace: tenant-acme
spec:
  podSelector: {}
  policyTypes: [Ingress, Egress]
  ingress:
    - from:
        - podSelector: {}  # Same namespace only
  egress:
    - to:
        - podSelector: {}
    - to:  # DNS
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
```

---

## Production Deployment Checklist

### Pod Spec
- [ ] Resource requests and limits set
- [ ] Liveness, readiness, and startup probes configured
- [ ] SecurityContext (non-root, read-only rootfs, drop capabilities)
- [ ] terminationGracePeriodSeconds appropriate for app
- [ ] preStop hook for graceful shutdown
- [ ] Image pinned by digest or specific tag (never `latest`)
- [ ] ServiceAccount with minimal permissions

### Deployment
- [ ] Multiple replicas (≥3 for critical services)
- [ ] PodDisruptionBudget configured
- [ ] Pod anti-affinity (spread across nodes)
- [ ] Topology spread constraints (spread across AZs)
- [ ] Rolling update strategy with maxUnavailable: 0
- [ ] Revision history limit set

### Scaling
- [ ] HPA configured with appropriate metrics
- [ ] VPA in recommendation mode for right-sizing
- [ ] Cluster autoscaler / Karpenter configured
- [ ] PDB allows autoscaler to drain nodes

### Networking
- [ ] Network Policies (default deny + explicit allow)
- [ ] Ingress with TLS termination
- [ ] Rate limiting at ingress level
- [ ] Service mesh for mTLS (if required)

### Observability
- [ ] Prometheus metrics exposed
- [ ] Structured logging (JSON)
- [ ] Distributed tracing headers propagated
- [ ] Alerts configured for SLOs

---

## Cost Optimization

### Spot/Preemptible Instances

```yaml
# Karpenter: prefer spot for non-critical workloads
spec:
  requirements:
    - key: karpenter.sh/capacity-type
      operator: In
      values: ["spot"]
  # Pods must tolerate interruption
---
# Pod tolerates spot interruption
spec:
  tolerations:
    - key: "karpenter.sh/capacity-type"
      operator: "Equal"
      value: "spot"
      effect: "NoSchedule"
```

### Right-Sizing Recommendations

```bash
# Identify over-provisioned pods
kubectl top pods --all-namespaces --sort-by=cpu | head -20

# Find pods without resource limits
kubectl get pods --all-namespaces -o json | \
  jq '.items[] | select(.spec.containers[].resources.limits == null) | .metadata.name'
```

### Namespace Cost Allocation

Use labels for cost tracking:
```yaml
metadata:
  labels:
    cost-center: "engineering"
    team: "payments"
    environment: "production"
```

---

## Next: [Observability →](07_Observability.md)
