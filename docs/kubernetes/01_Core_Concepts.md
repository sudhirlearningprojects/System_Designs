# 1. Kubernetes Core Concepts

## What is Kubernetes?

Kubernetes (K8s) is a container orchestration platform that automates deployment, scaling, and management of containerized applications. Originally designed by Google (based on Borg), now maintained by CNCF.

**Key Value Propositions:**
- Self-healing (restarts failed containers, replaces unhealthy nodes)
- Horizontal scaling (scale up/down based on load)
- Service discovery and load balancing
- Automated rollouts and rollbacks
- Secret and configuration management
- Storage orchestration

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CONTROL PLANE                             │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │  API Server  │  │  Scheduler   │  │  Controller Manager   │ │
│  │  (kube-api)  │  │              │  │  - Node Controller    │ │
│  └──────┬───────┘  └──────────────┘  │  - Replication Ctrl   │ │
│         │                             │  - Endpoint Ctrl      │ │
│  ┌──────▼───────┐  ┌──────────────┐  │  - Service Account    │ │
│  │    etcd      │  │  Cloud Ctrl  │  └───────────────────────┘ │
│  │  (key-value) │  │  Manager     │                             │
│  └──────────────┘  └──────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┼─────────┐
                    │         │         │
┌───────────────────▼─┐ ┌────▼──────┐ ┌▼───────────────────────┐
│     WORKER NODE 1   │ │  NODE 2   │ │      WORKER NODE 3     │
│                     │ │           │ │                         │
│ ┌─────────────────┐ │ │  ...      │ │ ┌─────────────────────┐│
│ │     kubelet     │ │ │           │ │ │      kubelet        ││
│ ├─────────────────┤ │ │           │ │ ├─────────────────────┤│
│ │   kube-proxy    │ │ │           │ │ │    kube-proxy       ││
│ ├─────────────────┤ │ │           │ │ ├─────────────────────┤│
│ │ Container       │ │ │           │ │ │ Container Runtime   ││
│ │ Runtime (CRI)   │ │ │           │ │ │ (containerd/CRI-O)  ││
│ ├─────────────────┤ │ │           │ │ ├─────────────────────┤│
│ │ Pod │ Pod │ Pod │ │ │           │ │ │ Pod │ Pod │ Pod     ││
│ └─────────────────┘ │ │           │ │ └─────────────────────┘│
└─────────────────────┘ └───────────┘ └─────────────────────────┘
```

---

### Control Plane Components

#### 1. kube-apiserver
- **Role**: Front door to the cluster; all communication goes through it
- **Function**: RESTful API server, validates and processes requests, persists state to etcd
- **Production**: Run 3+ replicas behind a load balancer

```yaml
# API Server flags for production
--etcd-servers=https://etcd1:2379,https://etcd2:2379,https://etcd3:2379
--enable-admission-plugins=NodeRestriction,PodSecurity,ResourceQuota,LimitRanger
--audit-log-path=/var/log/kubernetes/audit.log
--audit-log-maxage=30
--encryption-provider-config=/etc/kubernetes/encryption-config.yaml
--tls-cert-file=/etc/kubernetes/pki/apiserver.crt
--tls-private-key-file=/etc/kubernetes/pki/apiserver.key
```

#### 2. etcd
- **Role**: Distributed key-value store for all cluster state
- **Function**: Source of truth for cluster configuration, state, and metadata
- **Production**: Run 3 or 5 node cluster (odd numbers for quorum)

```bash
# etcd cluster health check
etcdctl endpoint health --cluster \
  --cacert=/etc/etcd/ca.crt \
  --cert=/etc/etcd/server.crt \
  --key=/etc/etcd/server.key

# Backup etcd (critical for DR)
etcdctl snapshot save /backup/etcd-$(date +%Y%m%d).db \
  --cacert=/etc/etcd/ca.crt \
  --cert=/etc/etcd/server.crt \
  --key=/etc/etcd/server.key
```

#### 3. kube-scheduler
- **Role**: Assigns Pods to Nodes
- **Function**: Watches for unscheduled Pods, selects optimal node based on:
  - Resource requirements (CPU, memory)
  - Affinity/anti-affinity rules
  - Taints and tolerations
  - Data locality
  - Inter-pod affinity

**Scheduling Algorithm:**
```
1. Filtering: Eliminate nodes that don't meet requirements
2. Scoring: Rank remaining nodes (0-100)
3. Binding: Assign Pod to highest-scoring node
```

#### 4. kube-controller-manager
- **Role**: Runs controller loops that regulate cluster state
- **Key Controllers**:
  - **Node Controller**: Monitors node health (40s timeout)
  - **Replication Controller**: Maintains desired replica count
  - **Endpoints Controller**: Populates Service endpoints
  - **ServiceAccount Controller**: Creates default accounts for namespaces

#### 5. cloud-controller-manager
- **Role**: Integrates with cloud provider APIs
- **Manages**: Load balancers, routes, node lifecycle, volumes

---

### Worker Node Components

#### 1. kubelet
- **Role**: Agent on each node; ensures containers are running in Pods
- **Function**:
  - Registers node with API server
  - Watches for Pod specs assigned to its node
  - Manages container lifecycle via CRI
  - Reports node and Pod status
  - Runs liveness/readiness/startup probes

#### 2. kube-proxy
- **Role**: Network proxy on each node
- **Function**: Maintains network rules for Service abstraction
- **Modes**:
  - `iptables` (default): O(n) rule matching
  - `ipvs`: O(1) connection processing, better for large clusters
  - `nftables`: Modern replacement for iptables

#### 3. Container Runtime
- **Role**: Runs containers
- **Options**: containerd (default), CRI-O
- **Note**: Docker was removed in K8s 1.24 (dockershim deprecation)

---

## Kubernetes Objects

### Object Model

Every K8s object has:
```yaml
apiVersion: apps/v1          # API group/version
kind: Deployment             # Object type
metadata:                    # Identity
  name: my-app
  namespace: production
  labels:
    app: my-app
    version: v2
  annotations:
    description: "Production deployment"
spec:                        # Desired state
  replicas: 3
  ...
status:                      # Current state (managed by K8s)
  availableReplicas: 3
  ...
```

### Core Objects

| Object | Purpose | Scope |
|--------|---------|-------|
| Pod | Smallest deployable unit | Namespaced |
| Service | Stable network endpoint | Namespaced |
| Namespace | Virtual cluster isolation | Cluster |
| Node | Worker machine | Cluster |
| ConfigMap | Non-sensitive configuration | Namespaced |
| Secret | Sensitive data | Namespaced |
| ServiceAccount | Identity for Pods | Namespaced |

### Workload Objects

| Object | Purpose |
|--------|---------|
| Deployment | Stateless apps with rolling updates |
| StatefulSet | Stateful apps with stable identity |
| DaemonSet | One Pod per node (agents, log collectors) |
| Job | Run-to-completion tasks |
| CronJob | Scheduled recurring tasks |
| ReplicaSet | Maintains Pod replicas (managed by Deployment) |

---

## Namespaces

Namespaces provide logical isolation within a cluster.

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    env: production
    team: platform
```

**Default Namespaces:**
- `default` — Default for objects with no namespace
- `kube-system` — System components (DNS, proxy, metrics)
- `kube-public` — Publicly readable (cluster info)
- `kube-node-lease` — Node heartbeat leases

**Production Namespace Strategy:**
```
├── production          # Live traffic
├── staging             # Pre-production testing
├── development         # Dev environments
├── monitoring          # Prometheus, Grafana
├── logging             # EFK/Loki stack
├── ingress             # Ingress controllers
├── cert-manager        # TLS certificate management
└── istio-system        # Service mesh
```

---

## Labels and Selectors

Labels are key-value pairs for organizing and selecting objects.

```yaml
metadata:
  labels:
    app.kubernetes.io/name: payment-service
    app.kubernetes.io/version: "2.1.0"
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: ecommerce
    app.kubernetes.io/managed-by: helm
    environment: production
    team: payments
    cost-center: cc-1234
```

**Selector Types:**
```yaml
# Equality-based
selector:
  matchLabels:
    app: payment-service
    environment: production

# Set-based
selector:
  matchExpressions:
    - key: environment
      operator: In
      values: [production, staging]
    - key: tier
      operator: NotIn
      values: [frontend]
```

---

## Annotations

Annotations store non-identifying metadata (not used for selection).

```yaml
metadata:
  annotations:
    # Ingress configuration
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    
    # Deployment info
    kubernetes.io/change-cause: "Updated to v2.1.0 - fixed payment bug"
    
    # Monitoring
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/metrics"
    
    # Service mesh
    sidecar.istio.io/inject: "true"
```

---

## How Kubernetes Works — Request Flow

### Pod Creation Flow

```
User → kubectl apply → API Server → etcd (persist)
                                  → Scheduler (watch: unscheduled pods)
                                  → Scheduler assigns node
                                  → API Server updates etcd
                                  → kubelet (watch: pods for my node)
                                  → kubelet → CRI → Container starts
                                  → kubelet reports status → API Server → etcd
```

### Detailed Sequence:

1. **User** runs `kubectl apply -f deployment.yaml`
2. **kubectl** sends HTTP POST to API Server
3. **API Server**:
   - Authenticates (who are you?)
   - Authorizes (can you do this?)
   - Admission control (mutating → validating webhooks)
   - Persists to etcd
4. **Deployment Controller** sees new Deployment, creates ReplicaSet
5. **ReplicaSet Controller** sees new ReplicaSet, creates Pod objects
6. **Scheduler** sees unscheduled Pods, assigns to nodes
7. **kubelet** on assigned node pulls image, starts container
8. **kube-proxy** updates iptables/ipvs rules for Service routing

---

## Declarative vs Imperative

### Imperative (avoid in production)
```bash
kubectl run nginx --image=nginx:1.25
kubectl expose deployment nginx --port=80
kubectl scale deployment nginx --replicas=3
```

### Declarative (production standard)
```bash
kubectl apply -f manifests/
# or
kubectl apply -k overlays/production/
```

**Why Declarative?**
- Version controlled (Git)
- Reproducible
- Auditable
- Supports GitOps workflows

---

## Resource Versioning

Kubernetes uses **resourceVersion** for optimistic concurrency:

```yaml
metadata:
  resourceVersion: "12345"  # etcd revision
```

- Every write increments the version
- Updates must include current resourceVersion (prevents conflicts)
- Watch API uses resourceVersion for efficient change detection

---

## Next: [Workloads & Scheduling →](02_Workloads_and_Scheduling.md)
