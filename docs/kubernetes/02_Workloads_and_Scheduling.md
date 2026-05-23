# 2. Workloads & Scheduling

## Pods

The smallest deployable unit in Kubernetes. A Pod encapsulates one or more containers that share network and storage.

### Pod Anatomy

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: web-app
  labels:
    app: web
spec:
  # Init containers run sequentially before main containers
  initContainers:
    - name: db-migration
      image: flyway/flyway:9
      command: ["flyway", "migrate"]
      env:
        - name: FLYWAY_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url

  containers:
    - name: app
      image: myapp:2.1.0
      ports:
        - containerPort: 8080
          protocol: TCP
      resources:
        requests:
          cpu: 250m        # 0.25 CPU cores
          memory: 256Mi    # 256 MiB
        limits:
          cpu: 500m
          memory: 512Mi
      env:
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: db.host
      volumeMounts:
        - name: config-vol
          mountPath: /etc/config
          readOnly: true
      livenessProbe:
        httpGet:
          path: /health/live
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
        failureThreshold: 3
      readinessProbe:
        httpGet:
          path: /health/ready
          port: 8080
        initialDelaySeconds: 5
        periodSeconds: 5
      startupProbe:
        httpGet:
          path: /health/started
          port: 8080
        failureThreshold: 30
        periodSeconds: 10
      lifecycle:
        preStop:
          exec:
            command: ["/bin/sh", "-c", "sleep 15"]  # Graceful shutdown

    # Sidecar container
    - name: log-shipper
      image: fluent-bit:2.1
      volumeMounts:
        - name: log-vol
          mountPath: /var/log/app

  volumes:
    - name: config-vol
      configMap:
        name: app-config
    - name: log-vol
      emptyDir: {}

  # Scheduling constraints
  nodeSelector:
    node-type: compute-optimized
  tolerations:
    - key: "dedicated"
      operator: "Equal"
      value: "high-memory"
      effect: "NoSchedule"
  terminationGracePeriodSeconds: 60
  serviceAccountName: web-app-sa
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
```

### Pod Lifecycle

```
Pending → Running → Succeeded/Failed
              │
              └→ Unknown (node lost)
```

| Phase | Description |
|-------|-------------|
| Pending | Accepted but not running (scheduling, image pull) |
| Running | At least one container running |
| Succeeded | All containers terminated successfully (exit 0) |
| Failed | At least one container terminated with error |
| Unknown | Node communication lost |

### Health Probes

| Probe | Purpose | Failure Action |
|-------|---------|----------------|
| **Startup** | App has started | Kill container (restart) |
| **Liveness** | App is alive | Kill container (restart) |
| **Readiness** | App can serve traffic | Remove from Service endpoints |

**Probe Types:**
```yaml
# HTTP GET
httpGet:
  path: /healthz
  port: 8080
  httpHeaders:
    - name: Custom-Header
      value: Awesome

# TCP Socket
tcpSocket:
  port: 3306

# Exec command
exec:
  command: ["pg_isready", "-U", "postgres"]

# gRPC (K8s 1.27+)
grpc:
  port: 50051
  service: health.v1.Health
```

---

## Deployments

The standard way to run stateless applications.

### Production Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  namespace: production
  labels:
    app.kubernetes.io/name: payment-service
    app.kubernetes.io/version: "3.2.1"
spec:
  replicas: 5
  revisionHistoryLimit: 10
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1          # Max pods above desired during update
      maxUnavailable: 0    # Zero downtime
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
        version: "3.2.1"
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  app: payment-service
              topologyKey: kubernetes.io/hostname
        # Spread across AZs
        topologySpreadConstraints:
          - maxSkew: 1
            topologyKey: topology.kubernetes.io/zone
            whenUnsatisfiable: DoNotSchedule
            labelSelector:
              matchLabels:
                app: payment-service
      containers:
        - name: payment
          image: registry.company.com/payment-service:3.2.1
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: 500m
              memory: 512Mi
            limits:
              cpu: "1"
              memory: 1Gi
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          envFrom:
            - configMapRef:
                name: payment-config
            - secretRef:
                name: payment-secrets
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: payment-service
```

### Deployment Strategies

#### Rolling Update (Default)
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 25%         # 25% extra pods during update
    maxUnavailable: 25%   # 25% can be unavailable
```

#### Blue-Green (via Service switch)
```bash
# Deploy green version
kubectl apply -f deployment-green.yaml

# Verify green is healthy
kubectl rollout status deployment/app-green

# Switch traffic
kubectl patch service app-service -p '{"spec":{"selector":{"version":"green"}}}'

# Cleanup blue
kubectl delete deployment app-blue
```

#### Canary (progressive traffic shift)
```yaml
# Canary: 10% traffic (1 out of 10 replicas)
# Main deployment: 9 replicas
# Canary deployment: 1 replica (same Service selector)

# Or use Istio VirtualService:
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  http:
    - route:
        - destination:
            host: payment-service
            subset: stable
          weight: 90
        - destination:
            host: payment-service
            subset: canary
          weight: 10
```

### Rollback

```bash
# Check rollout history
kubectl rollout history deployment/payment-service

# Rollback to previous version
kubectl rollout undo deployment/payment-service

# Rollback to specific revision
kubectl rollout undo deployment/payment-service --to-revision=3

# Pause/Resume rollout
kubectl rollout pause deployment/payment-service
kubectl rollout resume deployment/payment-service
```

---

## StatefulSets

For stateful applications requiring stable identity and persistent storage.

### When to Use StatefulSet

- Databases (PostgreSQL, MySQL, MongoDB)
- Message queues (Kafka, RabbitMQ)
- Distributed systems (Elasticsearch, ZooKeeper)
- Any app needing stable network identity or ordered deployment

### StatefulSet Guarantees

1. **Stable network identity**: `pod-name-{0,1,2,...}` with stable DNS
2. **Ordered deployment**: Pods created sequentially (0 → 1 → 2)
3. **Ordered termination**: Pods deleted in reverse order (2 → 1 → 0)
4. **Stable storage**: Each Pod gets its own PVC that persists across rescheduling

### Production StatefulSet

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: database
spec:
  serviceName: postgres-headless  # Required: headless service
  replicas: 3
  podManagementPolicy: OrderedReady  # or Parallel
  updateStrategy:
    type: RollingUpdate
    rollingUpdate:
      partition: 0  # Update all pods; set higher for canary
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  app: postgres
              topologyKey: kubernetes.io/hostname
      containers:
        - name: postgres
          image: postgres:16
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: password
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
          resources:
            requests:
              cpu: "1"
              memory: 2Gi
            limits:
              cpu: "2"
              memory: 4Gi
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            periodSeconds: 5
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: gp3-encrypted
        resources:
          requests:
            storage: 100Gi
---
# Headless Service (required for StatefulSet DNS)
apiVersion: v1
kind: Service
metadata:
  name: postgres-headless
spec:
  clusterIP: None  # Headless
  selector:
    app: postgres
  ports:
    - port: 5432
```

**DNS Records Created:**
```
postgres-0.postgres-headless.database.svc.cluster.local
postgres-1.postgres-headless.database.svc.cluster.local
postgres-2.postgres-headless.database.svc.cluster.local
```

---

## DaemonSets

Ensures a Pod runs on every (or selected) node.

### Use Cases
- Log collectors (Fluentd, Fluent Bit)
- Monitoring agents (Node Exporter, Datadog)
- Network plugins (Calico, Cilium)
- Storage daemons (CSI node plugins)

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
    metadata:
      labels:
        app: fluent-bit
    spec:
      tolerations:
        - operator: Exists  # Run on ALL nodes including masters
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
              readOnly: true
            - name: containers
              mountPath: /var/lib/docker/containers
              readOnly: true
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
        - name: containers
          hostPath:
            path: /var/lib/docker/containers
```

---

## Jobs and CronJobs

### Job (Run-to-Completion)

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration
spec:
  backoffLimit: 3           # Retry 3 times on failure
  activeDeadlineSeconds: 600  # Timeout after 10 minutes
  ttlSecondsAfterFinished: 3600  # Cleanup after 1 hour
  template:
    spec:
      restartPolicy: Never  # or OnFailure
      containers:
        - name: migrate
          image: flyway/flyway:9
          command: ["flyway", "migrate"]
          env:
            - name: FLYWAY_URL
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: url
```

### Parallel Job

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: batch-processor
spec:
  completions: 100    # Total tasks to complete
  parallelism: 10     # Run 10 pods concurrently
  completionMode: Indexed  # Each pod gets unique index
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: worker
          image: batch-worker:1.0
          env:
            - name: JOB_INDEX
              valueFrom:
                fieldRef:
                  fieldPath: metadata.annotations['batch.kubernetes.io/job-completion-index']
```

### CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: daily-report
spec:
  schedule: "0 2 * * *"          # 2 AM daily
  timeZone: "America/New_York"   # K8s 1.27+
  concurrencyPolicy: Forbid      # Don't overlap
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 5
  startingDeadlineSeconds: 300   # Must start within 5 min
  jobTemplate:
    spec:
      backoffLimit: 2
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: report
              image: report-generator:1.0
              resources:
                requests:
                  cpu: 500m
                  memory: 1Gi
```

**ConcurrencyPolicy Options:**
- `Allow` — Multiple jobs can run simultaneously
- `Forbid` — Skip new job if previous still running
- `Replace` — Kill running job, start new one

---

## Scheduling

### Node Selectors (Simple)

```yaml
spec:
  nodeSelector:
    disktype: ssd
    gpu: "true"
```

### Node Affinity (Advanced)

```yaml
spec:
  affinity:
    nodeAffinity:
      # Hard requirement
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
          - matchExpressions:
              - key: topology.kubernetes.io/zone
                operator: In
                values: ["us-east-1a", "us-east-1b"]
      # Soft preference
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 80
          preference:
            matchExpressions:
              - key: node-type
                operator: In
                values: ["compute-optimized"]
```

### Pod Affinity / Anti-Affinity

```yaml
spec:
  affinity:
    # Co-locate with cache pods
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              app: redis-cache
          topologyKey: kubernetes.io/hostname

    # Spread across nodes
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              app: payment-service
          topologyKey: kubernetes.io/hostname
```

### Topology Spread Constraints

```yaml
spec:
  topologySpreadConstraints:
    # Spread evenly across zones
    - maxSkew: 1
      topologyKey: topology.kubernetes.io/zone
      whenUnsatisfiable: DoNotSchedule
      labelSelector:
        matchLabels:
          app: web
    # Spread across nodes within each zone
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: ScheduleAnyway
      labelSelector:
        matchLabels:
          app: web
```

### Taints and Tolerations

```bash
# Taint a node (repel pods)
kubectl taint nodes gpu-node-1 gpu=true:NoSchedule
kubectl taint nodes spot-node-1 spot=true:PreferNoSchedule
kubectl taint nodes maintenance-node maintenance=true:NoExecute
```

```yaml
# Pod tolerates the taint
spec:
  tolerations:
    - key: "gpu"
      operator: "Equal"
      value: "true"
      effect: "NoSchedule"
    - key: "spot"
      operator: "Exists"
      effect: "PreferNoSchedule"
    - key: "maintenance"
      operator: "Exists"
      effect: "NoExecute"
      tolerationSeconds: 3600  # Evict after 1 hour
```

**Taint Effects:**
| Effect | Behavior |
|--------|----------|
| NoSchedule | Don't schedule new pods (existing stay) |
| PreferNoSchedule | Try to avoid, but allow if needed |
| NoExecute | Evict existing pods + don't schedule new |

### Priority and Preemption

```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: critical-service
value: 1000000
globalDefault: false
preemptionPolicy: PreemptLowerPriority
description: "For critical production services"
---
apiVersion: v1
kind: Pod
spec:
  priorityClassName: critical-service
  containers:
    - name: app
      image: critical-app:1.0
```

**Built-in Priority Classes:**
- `system-cluster-critical` (2000000000)
- `system-node-critical` (2000001000)

---

## Resource Management

### Requests vs Limits

```yaml
resources:
  requests:    # Guaranteed minimum (used for scheduling)
    cpu: 250m       # 0.25 cores
    memory: 256Mi   # 256 MiB
  limits:      # Maximum allowed (throttled/OOMKilled if exceeded)
    cpu: 500m       # Throttled if exceeded
    memory: 512Mi   # OOMKilled if exceeded
```

**Key Rules:**
- **CPU**: Compressible — throttled when exceeding limit
- **Memory**: Incompressible — OOMKilled when exceeding limit
- Set requests = limits for **Guaranteed** QoS (critical services)
- Set requests < limits for **Burstable** QoS (general workloads)

### QoS Classes

| Class | Condition | Eviction Priority |
|-------|-----------|-------------------|
| Guaranteed | requests == limits for all containers | Last to evict |
| Burstable | At least one request set, requests < limits | Middle |
| BestEffort | No requests or limits set | First to evict |

### LimitRange (Namespace Defaults)

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
  namespace: production
spec:
  limits:
    - type: Container
      default:          # Default limits
        cpu: 500m
        memory: 512Mi
      defaultRequest:   # Default requests
        cpu: 100m
        memory: 128Mi
      max:
        cpu: "4"
        memory: 8Gi
      min:
        cpu: 50m
        memory: 64Mi
    - type: Pod
      max:
        cpu: "8"
        memory: 16Gi
```

### ResourceQuota (Namespace Caps)

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: production-quota
  namespace: production
spec:
  hard:
    requests.cpu: "100"
    requests.memory: 200Gi
    limits.cpu: "200"
    limits.memory: 400Gi
    pods: "500"
    services: "50"
    persistentvolumeclaims: "100"
    count/deployments.apps: "50"
```

---

## Next: [Networking →](03_Networking.md)
