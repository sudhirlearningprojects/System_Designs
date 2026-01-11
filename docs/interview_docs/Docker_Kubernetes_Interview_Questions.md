# Docker & Kubernetes Interview Questions

## 🐳 Docker Interview Questions

### 🎯 Beginner Level (0-2 years)

#### Q1: What is Docker and why is it used?
**Answer**: Docker is a containerization platform that packages applications and their dependencies into lightweight, portable containers. Benefits:
- **Consistency**: "Works on my machine" → "Works everywhere"
- **Isolation**: Applications don't interfere with each other
- **Portability**: Run anywhere Docker is installed
- **Efficiency**: Lighter than VMs, faster startup
- **Scalability**: Easy to scale up/down

#### Q2: What's the difference between Docker Image and Container?
**Answer**:

| Docker Image | Docker Container |
|--------------|------------------|
| Read-only template | Running instance of image |
| Blueprint/Recipe | Actual running application |
| Static | Dynamic |
| Can't be modified | Can be started/stopped/modified |

**Analogy**: Image is like a **class**, Container is like an **object**

**Example**:
```bash
# Download image (template)
docker pull nginx

# Create and run container from image
docker run nginx

# One image can create multiple containers
docker run --name web1 nginx
docker run --name web2 nginx
docker run --name web3 nginx
```

#### Q3: What is a Dockerfile?
**Answer**: A text file containing instructions to build a Docker image.

```dockerfile
# Example Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/myapp.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

#### Q4: Explain basic Docker commands.
**Answer**:
```bash
# Image operations
docker pull nginx                    # Download image
docker images                       # List images
docker rmi nginx                     # Remove image

# Container operations
docker run -d -p 8080:80 nginx      # Run container
docker ps                           # List running containers
docker ps -a                        # List all containers
docker stop <container-id>          # Stop container
docker rm <container-id>            # Remove container

# Build
docker build -t myapp .             # Build image from Dockerfile
```

#### Q5: What does the `-d` flag do in `docker run`?
**Answer**: Runs container in **detached mode** (in background). Without `-d`, container runs in foreground and blocks terminal.

```bash
docker run nginx           # Foreground (blocks terminal)
docker run -d nginx        # Background (returns container ID)
```

---

### 🚀 Intermediate Level (2-5 years)

#### Q6: Explain Docker networking modes.
**Answer**:

| Network Mode | Description | Use Case |
|--------------|-------------|----------|
| **bridge** | Default, isolated network | Most applications |
| **host** | Uses host's network | High performance |
| **none** | No networking | Security isolation |
| **overlay** | Multi-host networking | Docker Swarm |

**Examples**:
```bash
# Bridge network (default)
docker run nginx                     # Uses default bridge

# Custom bridge network
docker network create mynetwork
docker run --network mynetwork nginx

# Host network
docker run --network host nginx      # Uses host's network stack

# No network
docker run --network none nginx      # Isolated, no networking

# List networks
docker network ls

# Inspect network
docker network inspect mynetwork
```

#### Q7: What are Docker volumes and why use them?
**Answer**: Volumes provide persistent storage for containers. Data survives container restarts/deletions.

```bash
# Named volume
docker volume create mydata
docker run -v mydata:/app/data nginx

# Bind mount (host directory)
docker run -v /host/path:/container/path nginx

# Anonymous volume
docker run -v /app/data nginx
```

**Types**:
- **Named volumes**: Managed by Docker
- **Bind mounts**: Direct host directory mapping
- **Anonymous volumes**: Temporary, Docker-managed

#### Q8: What is Docker Compose?
**Answer**: Tool for defining and running multi-container applications using YAML files.

```yaml
# docker-compose.yml
version: '3.8'
services:
  web:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
  db:
    image: postgres:13
    environment:
      POSTGRES_PASSWORD: password
    volumes:
      - db_data:/var/lib/postgresql/data

volumes:
  db_data:
```

```bash
docker-compose up -d        # Start all services
docker-compose down         # Stop and remove
docker-compose logs web     # View logs
```

#### Q9: How do you optimize Docker image size?
**Answer**:
```dockerfile
# ❌ Bad - Large image
FROM ubuntu:20.04
RUN apt-get update && apt-get install -y openjdk-17-jdk
COPY target/app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]

# ✅ Good - Optimized
FROM openjdk:17-jdk-alpine
COPY target/app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]

# ✅ Better - Multi-stage build
FROM maven:3.8-openjdk-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package

FROM openjdk:17-jdk-alpine
COPY --from=build /app/target/app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
```

**Optimization techniques**:
- Use Alpine Linux base images
- Multi-stage builds
- Minimize layers
- Use .dockerignore
- Remove package managers after use

#### Q10: What is the difference between CMD and ENTRYPOINT?
**Answer**:

| CMD | ENTRYPOINT |
|-----|------------|
| Can be overridden | Cannot be overridden |
| Default command | Fixed command |
| `docker run image ls` overrides CMD | `docker run image ls` passes `ls` as argument |

**CMD Example**:
```dockerfile
FROM alpine
CMD ["echo", "hello"]
```
```bash
docker run image          # Output: hello
docker run image world    # Output: world (CMD overridden)
```

**ENTRYPOINT Example**:
```dockerfile
FROM alpine
ENTRYPOINT ["echo"]
```
```bash
docker run image          # Output: (empty)
docker run image hello    # Output: hello (passed as argument)
```

**Combined ENTRYPOINT + CMD**:
```dockerfile
FROM alpine
ENTRYPOINT ["echo"]
CMD ["hello"]
```
```bash
docker run image          # Output: hello
docker run image world    # Output: world
```

---

### 🔥 Advanced Level (5+ years)

#### Q11: Explain Docker's layered architecture.
**Answer**: Docker images are built in layers using Union File System (UFS):

```dockerfile
FROM ubuntu:20.04          # Layer 1: Base OS
RUN apt-get update         # Layer 2: Package updates  
RUN apt-get install nginx  # Layer 3: Nginx installation
COPY index.html /var/www/  # Layer 4: Application files
```

**Benefits**:
- **Caching**: Unchanged layers are reused
- **Sharing**: Multiple images share common layers
- **Efficiency**: Only changed layers are downloaded

```bash
# View image layers
docker history nginx
docker inspect nginx
```

#### Q12: How does Docker handle security?
**Answer**:

**Security best practices**:
```dockerfile
# Use specific versions
FROM node:16.14.2-alpine

# Don't run as root
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001
USER nextjs

# Minimal permissions
COPY --chown=nextjs:nodejs package*.json ./
RUN npm ci --only=production
COPY --chown=nextjs:nodejs . .

# Scan for vulnerabilities
RUN npm audit fix

CMD ["npm", "start"]
```

**Security measures**:
- **Namespaces**: Process isolation
- **Cgroups**: Resource limits
- **Capabilities**: Limit root privileges
- **Seccomp**: System call filtering
- **AppArmor/SELinux**: Mandatory access control
- **Image scanning**: Detect vulnerabilities
- **Read-only filesystem**: Prevent tampering

#### Q13: What are Docker build contexts and .dockerignore?
**Answer**: Build context is the set of files sent to Docker daemon during build.

```bash
# .dockerignore
node_modules
*.log
.git
README.md
Dockerfile
.dockerignore
```

```dockerfile
# Only necessary files are copied
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
CMD ["npm", "start"]
```

**Benefits**:
- Faster builds (smaller context)
- Better security (exclude sensitive files)
- Smaller images

#### Q14: Explain Docker registry and image distribution.
**Answer**:
```bash
# Tag image for registry
docker tag myapp:latest registry.company.com/myapp:v1.0.0

# Push to registry
docker push registry.company.com/myapp:v1.0.0

# Pull from registry
docker pull registry.company.com/myapp:v1.0.0

# Private registry with authentication
docker login registry.company.com
```

**Registry types**:
- **Docker Hub**: Public registry
- **Private registries**: AWS ECR, Google GCR, Azure ACR
- **Self-hosted**: Harbor, Nexus

#### Q15: How do you troubleshoot Docker containers?
**Answer**:
```bash
# Container logs
docker logs <container-id>
docker logs -f <container-id>        # Follow logs

# Execute commands in running container
docker exec -it <container-id> /bin/bash
docker exec <container-id> ps aux

# Container resource usage
docker stats
docker stats <container-id>

# Inspect container details
docker inspect <container-id>

# Debug failed container
docker run --rm -it <image> /bin/bash
```

---

## ☸️ Kubernetes Interview Questions

### 🎯 Beginner Level (0-2 years)

#### Q16: What is Kubernetes and why is it needed?
**Answer**: Kubernetes (K8s) is a container orchestration platform that automates deployment, scaling, and management of containerized applications.

**Problems it solves**:
- **Manual scaling**: Auto-scaling based on load
- **Service discovery**: Automatic load balancing
- **Health monitoring**: Self-healing containers
- **Rolling updates**: Zero-downtime deployments
- **Resource management**: Efficient cluster utilization

#### Q17: Explain basic Kubernetes architecture.
**Answer**:
```
┌──────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                         │
│                                                              │
│  ┌──────────────────┐      ┌──────────────────────────────┐ │
│  │   Master Node    │      │       Worker Nodes           │ │
│  │                  │      │                              │ │
│  │  ┌────────────┐  │      │  ┌─────────┐ ┌────────────┐ │ │
│  │  │ API Server │  │      │  │ kubelet │ │   Pods     │ │ │
│  │  │ etcd       │  │      │  │ kube-   │ │            │ │ │
│  │  │ Scheduler  │  │      │  │ proxy   │ │ ┌────────┐ │ │ │
│  │  │ Controller │  │      │  │         │ │ │Container│ │ │ │
│  │  │ Manager    │  │      │  │         │ │ └────────┘ │ │ │
│  │  └────────────┘  │      │  └─────────┘ └────────────┘ │ │
│  └──────────────────┘      └──────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

**Master Components**:
- **API Server**: Entry point for all operations
- **etcd**: Distributed key-value store
- **Scheduler**: Assigns pods to nodes
- **Controller Manager**: Maintains desired state

**Worker Components**:
- **kubelet**: Node agent
- **kube-proxy**: Network proxy
- **Container Runtime**: Docker/containerd

#### Q18: What is a Pod in Kubernetes?
**Answer**: Smallest deployable unit in Kubernetes. Contains one or more containers that share:
- Network (IP address)
- Storage (volumes)
- Lifecycle

```yaml
# pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
spec:
  containers:
  - name: nginx
    image: nginx:1.20
    ports:
    - containerPort: 80
```

```bash
kubectl apply -f pod.yaml
kubectl get pods
kubectl describe pod nginx-pod
```

#### Q19: What are Kubernetes Services?
**Answer**: Services provide stable network endpoints for pods.

**Service Types**:

| Type | Description | Use Case |
|------|-------------|----------|
| **ClusterIP** | Internal cluster access | Database, internal APIs |
| **NodePort** | External access via node port | Development, testing |
| **LoadBalancer** | Cloud load balancer | Production external access |
| **ExternalName** | DNS alias | External service integration |

**Example**:
```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  selector:
    app: nginx
  ports:
  - port: 80
    targetPort: 80
  type: ClusterIP
```

```bash
# Create service
kubectl apply -f service.yaml

# List services
kubectl get services
kubectl get svc

# Describe service
kubectl describe service nginx-service

# Delete service
kubectl delete service nginx-service
```

#### Q20: What is a Deployment?
**Answer**: Manages replica sets and provides declarative updates for pods.

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.20
        ports:
        - containerPort: 80
```

```bash
kubectl apply -f deployment.yaml
kubectl get deployments
kubectl scale deployment nginx-deployment --replicas=5
```

---

### 🚀 Intermediate Level (2-5 years)

#### Q21: Explain Kubernetes networking.
**Answer**:
```
┌─────────────────────────────────────────────────────────┐
│                 Kubernetes Networking                  │
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Node 1    │    │   Node 2    │    │   Node 3    │ │
│  │             │    │             │    │             │ │
│  │ Pod A       │    │ Pod C       │    │ Pod E       │ │
│  │ 10.1.1.2    │    │ 10.1.2.2    │    │ 10.1.3.2    │ │
│  │             │    │             │    │             │ │
│  │ Pod B       │    │ Pod D       │    │ Pod F       │ │
│  │ 10.1.1.3    │    │ 10.1.2.3    │    │ 10.1.3.3    │ │
│  └─────────────┘    └─────────────┘    └─────────────┘ │
│         │                   │                   │       │
│         └───────────────────┼───────────────────┘       │
│                             │                           │
│              ┌─────────────────────────┐                │
│              │    Cluster Network      │                │
│              │    (CNI Plugin)         │                │
│              └─────────────────────────┘                │
└─────────────────────────────────────────────────────────┘
```

**Networking principles**:
- Every pod gets unique IP
- Pods can communicate without NAT
- Nodes can communicate with pods
- Services provide stable endpoints

**CNI Plugins**: Flannel, Calico, Weave, Cilium

#### Q22: What are ConfigMaps and Secrets?
**Answer**:
```yaml
# ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  database_url: "postgresql://db:5432/myapp"
  log_level: "INFO"
  config.properties: |
    server.port=8080
    server.host=0.0.0.0

---
# Secret
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  username: YWRtaW4=  # base64 encoded
  password: cGFzc3dvcmQ=

---
# Using in Pod
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:latest
    env:
    - name: DATABASE_URL
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: database_url
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: password
    volumeMounts:
    - name: config-volume
      mountPath: /etc/config
  volumes:
  - name: config-volume
    configMap:
      name: app-config
```

#### Q23: Explain Kubernetes storage (Volumes, PV, PVC).
**Answer**:
```yaml
# PersistentVolume
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-storage
spec:
  capacity:
    storage: 10Gi
  accessModes:
  - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: fast-ssd
  hostPath:
    path: /data/pv-storage

---
# PersistentVolumeClaim
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pvc-storage
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: fast-ssd

---
# Pod using PVC
apiVersion: v1
kind: Pod
metadata:
  name: storage-pod
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: storage
      mountPath: /usr/share/nginx/html
  volumes:
  - name: storage
    persistentVolumeClaim:
      claimName: pvc-storage
```

**Storage Types**:
- **emptyDir**: Temporary storage
- **hostPath**: Node filesystem
- **PV/PVC**: Persistent storage
- **Cloud storage**: EBS, GCE PD, Azure Disk

#### Q24: What are Ingress and Ingress Controllers?
**Answer**: Ingress manages external HTTP/HTTPS access to services.

```yaml
# Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
      - path: /web
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
  tls:
  - hosts:
    - myapp.example.com
    secretName: tls-secret
```

**Popular Ingress Controllers**:
- NGINX Ingress Controller
- Traefik
- HAProxy
- AWS ALB Ingress Controller

#### Q25: Explain Kubernetes resource management.
**Answer**:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: resource-demo
spec:
  containers:
  - name: app
    image: nginx
    resources:
      requests:        # Minimum guaranteed
        memory: "128Mi"
        cpu: "100m"
      limits:          # Maximum allowed
        memory: "256Mi"
        cpu: "200m"
```

**Resource types**:
- **CPU**: Measured in millicores (1000m = 1 CPU)
- **Memory**: Measured in bytes (Mi, Gi)
- **Storage**: Persistent volume claims
- **Custom**: GPU, network bandwidth

**QoS Classes**:
- **Guaranteed**: requests = limits
- **Burstable**: requests < limits
- **BestEffort**: no requests/limits

---

### 🔥 Advanced Level (5+ years)

#### Q26: Explain Kubernetes RBAC (Role-Based Access Control).
**Answer**:
```yaml
# ServiceAccount
apiVersion: v1
kind: ServiceAccount
metadata:
  name: app-service-account
  namespace: default

---
# Role (namespace-scoped)
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
  namespace: default
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]

---
# ClusterRole (cluster-scoped)
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: node-reader
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]

---
# RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-pods
  namespace: default
subjects:
- kind: ServiceAccount
  name: app-service-account
  namespace: default
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io

---
# ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: read-nodes
subjects:
- kind: ServiceAccount
  name: app-service-account
  namespace: default
roleRef:
  kind: ClusterRole
  name: node-reader
  apiGroup: rbac.authorization.k8s.io
```

#### Q27: What are Kubernetes Operators?
**Answer**: Operators extend Kubernetes API to manage complex applications using custom resources and controllers.

```yaml
# Custom Resource Definition
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: databases.example.com
spec:
  group: example.com
  versions:
  - name: v1
    served: true
    storage: true
    schema:
      openAPIV3Schema:
        type: object
        properties:
          spec:
            type: object
            properties:
              size:
                type: integer
              version:
                type: string
  scope: Namespaced
  names:
    plural: databases
    singular: database
    kind: Database

---
# Custom Resource
apiVersion: example.com/v1
kind: Database
metadata:
  name: my-database
spec:
  size: 3
  version: "13.4"
```

**Popular Operators**:
- Prometheus Operator
- PostgreSQL Operator
- MongoDB Operator
- Istio Operator

#### Q28: Explain Kubernetes scheduling and node affinity.
**Answer**:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: scheduling-demo
spec:
  # Node Selector (simple)
  nodeSelector:
    disktype: ssd
  
  # Node Affinity (advanced)
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: kubernetes.io/arch
            operator: In
            values:
            - amd64
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        preference:
          matchExpressions:
          - key: zone
            operator: In
            values:
            - us-west-1a
    
    # Pod Affinity
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - cache
        topologyKey: kubernetes.io/hostname
    
    # Pod Anti-Affinity
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
            - key: app
              operator: In
              values:
              - web
          topologyKey: kubernetes.io/hostname
  
  # Tolerations
  tolerations:
  - key: "node-role.kubernetes.io/master"
    operator: "Exists"
    effect: "NoSchedule"
  
  containers:
  - name: app
    image: nginx
```

#### Q29: How do you implement blue-green deployments in Kubernetes?
**Answer**:
```yaml
# Blue Deployment (current)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-blue
  labels:
    version: blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
      version: blue
  template:
    metadata:
      labels:
        app: myapp
        version: blue
    spec:
      containers:
      - name: app
        image: myapp:v1.0

---
# Green Deployment (new)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-green
  labels:
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
      version: green
  template:
    metadata:
      labels:
        app: myapp
        version: green
    spec:
      containers:
      - name: app
        image: myapp:v2.0

---
# Service (switch between blue/green)
apiVersion: v1
kind: Service
metadata:
  name: app-service
spec:
  selector:
    app: myapp
    version: blue  # Switch to 'green' for deployment
  ports:
  - port: 80
    targetPort: 8080
```

**Deployment process**:
1. Deploy green version alongside blue
2. Test green version
3. Switch service selector to green
4. Monitor and rollback if needed
5. Remove blue deployment

#### Q30: What are Helm charts and how do you use them?
**Answer**: Helm is a package manager for Kubernetes that uses charts (templates) to deploy applications.

```yaml
# Chart.yaml
apiVersion: v2
name: myapp
description: My Application Helm Chart
version: 0.1.0
appVersion: "1.0"

# values.yaml
replicaCount: 3
image:
  repository: myapp
  tag: "latest"
  pullPolicy: IfNotPresent
service:
  type: ClusterIP
  port: 80
ingress:
  enabled: true
  host: myapp.example.com
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "myapp.fullname" . }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "myapp.selectorLabels" . | nindent 8 }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        ports:
        - containerPort: 8080
        resources:
          {{- toYaml .Values.resources | nindent 12 }}
```

```bash
# Helm commands
helm create myapp                    # Create chart
helm install myapp ./myapp          # Install chart
helm upgrade myapp ./myapp          # Upgrade release
helm rollback myapp 1               # Rollback to revision 1
helm uninstall myapp                # Uninstall release
helm list                           # List releases
```

---

## 🎯 Expert Level (Architect/Senior)

#### Q31: Design a highly available Kubernetes cluster.
**Answer**:
```yaml
# Multi-master setup with etcd cluster
# 3 master nodes for HA
# 5 worker nodes across availability zones
# External load balancer for API server
# Separate etcd cluster (3 or 5 nodes)

# Example cluster configuration
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
kubernetesVersion: v1.25.0
controlPlaneEndpoint: "k8s-api.example.com:6443"
etcd:
  external:
    endpoints:
    - https://etcd1.example.com:2379
    - https://etcd2.example.com:2379
    - https://etcd3.example.com:2379
networking:
  serviceSubnet: "10.96.0.0/12"
  podSubnet: "10.244.0.0/16"
apiServer:
  certSANs:
  - "k8s-api.example.com"
  - "10.0.0.100"
```

**HA Components**:
- Multiple master nodes
- External etcd cluster
- Load balancer for API server
- Multi-AZ worker nodes
- Network redundancy

#### Q32: How do you implement monitoring and observability?
**Answer**:
```yaml
# Prometheus + Grafana + AlertManager stack
# ServiceMonitor for application metrics
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: app-metrics
spec:
  selector:
    matchLabels:
      app: myapp
  endpoints:
  - port: metrics
    interval: 30s
    path: /metrics

---
# PrometheusRule for alerts
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: app-alerts
spec:
  groups:
  - name: app.rules
    rules:
    - alert: HighErrorRate
      expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "High error rate detected"

---
# Jaeger for distributed tracing
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: jaeger
spec:
  strategy: production
  storage:
    type: elasticsearch
```

**Observability Stack**:
- **Metrics**: Prometheus + Grafana
- **Logging**: ELK/EFK stack
- **Tracing**: Jaeger/Zipkin
- **APM**: New Relic, Datadog

#### Q33: Implement GitOps with ArgoCD.
**Answer**:
```yaml
# ArgoCD Application
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/company/k8s-manifests
    targetRevision: HEAD
    path: apps/myapp
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true

---
# AppProject for multi-tenancy
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: team-a
  namespace: argocd
spec:
  description: Team A applications
  sourceRepos:
  - 'https://github.com/company/team-a-*'
  destinations:
  - namespace: 'team-a-*'
    server: https://kubernetes.default.svc
  clusterResourceWhitelist:
  - group: ''
    kind: Namespace
  namespaceResourceWhitelist:
  - group: 'apps'
    kind: Deployment
  - group: ''
    kind: Service
```

**GitOps Benefits**:
- Declarative configuration
- Version control for infrastructure
- Automated deployments
- Audit trail
- Rollback capabilities

---

## 🔧 Practical Scenarios

#### Q34: Debug a failing pod.
**Answer**:
```bash
# Check pod status
kubectl get pods
kubectl describe pod <pod-name>

# Check logs
kubectl logs <pod-name>
kubectl logs <pod-name> -c <container-name>
kubectl logs <pod-name> --previous

# Execute into pod
kubectl exec -it <pod-name> -- /bin/bash

# Check events
kubectl get events --sort-by=.metadata.creationTimestamp

# Check resource usage
kubectl top pods
kubectl top nodes

# Debug networking
kubectl exec -it <pod-name> -- nslookup kubernetes.default
kubectl exec -it <pod-name> -- wget -qO- http://service-name
```

#### Q35: Implement canary deployment.
**Answer**:
```yaml
# Stable deployment (90% traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-stable
spec:
  replicas: 9
  selector:
    matchLabels:
      app: myapp
      version: stable
  template:
    metadata:
      labels:
        app: myapp
        version: stable
    spec:
      containers:
      - name: app
        image: myapp:v1.0

---
# Canary deployment (10% traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-canary
spec:
  replicas: 1
  selector:
    matchLabels:
      app: myapp
      version: canary
  template:
    metadata:
      labels:
        app: myapp
        version: canary
    spec:
      containers:
      - name: app
        image: myapp:v2.0

---
# Service routes to both versions
apiVersion: v1
kind: Service
metadata:
  name: app-service
spec:
  selector:
    app: myapp  # Routes to both stable and canary
  ports:
  - port: 80
    targetPort: 8080
```

---

## 💡 Best Practices Summary

### Docker Best Practices
1. **Use official base images**
2. **Multi-stage builds** for smaller images
3. **Don't run as root** user
4. **Use .dockerignore** to exclude files
5. **Pin specific versions** of base images
6. **Minimize layers** and use layer caching
7. **Scan images** for vulnerabilities
8. **Use health checks** in containers

### Kubernetes Best Practices
1. **Use namespaces** for isolation
2. **Set resource requests/limits**
3. **Implement health checks** (liveness/readiness)
4. **Use ConfigMaps/Secrets** for configuration
5. **Apply RBAC** for security
6. **Use labels and selectors** effectively
7. **Implement monitoring** and logging
8. **Plan for disaster recovery**
9. **Use GitOps** for deployments
10. **Regular cluster maintenance** and updates