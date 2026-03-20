# Cost-Effective Microservices Deployment Guide

## Table of Contents
1. [AWS Solutions](#aws-solutions)
2. [GCP Solutions](#gcp-solutions)
3. [Azure Solutions](#azure-solutions)
4. [Kubernetes Cost Optimization](#kubernetes-cost-optimization)
5. [Multi-Cloud Comparison](#multi-cloud-comparison)
6. [Cost Optimization Strategies](#cost-optimization-strategies)

---

## AWS Solutions

### 1. AWS ECS with Fargate Spot (Most Cost-Effective for Stateless Services)

**Cost**: ~70% cheaper than EC2 on-demand

```yaml
# ECS Task Definition with Fargate Spot
{
  "family": "my-microservice",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [{
    "name": "app",
    "image": "my-app:latest",
    "portMappings": [{
      "containerPort": 8080,
      "protocol": "tcp"
    }]
  }]
}
```

```bash
# Deploy with Spot capacity
aws ecs create-service \
  --cluster my-cluster \
  --service-name my-service \
  --task-definition my-microservice \
  --desired-count 3 \
  --capacity-provider-strategy \
    capacityProvider=FARGATE_SPOT,weight=70,base=0 \
    capacityProvider=FARGATE,weight=30,base=1
```

**Monthly Cost Estimate** (3 services, 2 tasks each):
- Fargate Spot: ~$15-20/month per service
- Total: ~$45-60/month

---

### 2. AWS Lambda + API Gateway (Best for Low-Traffic Services)

**Cost**: Pay only for execution time

```javascript
// Lambda Function
exports.handler = async (event) => {
    const response = {
        statusCode: 200,
        body: JSON.stringify({ message: 'Hello from Lambda' })
    };
    return response;
};
```

```yaml
# SAM Template
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31

Resources:
  MyMicroservice:
    Type: AWS::Serverless::Function
    Properties:
      Handler: index.handler
      Runtime: nodejs18.x
      MemorySize: 512
      Timeout: 30
      Events:
        Api:
          Type: Api
          Properties:
            Path: /api/{proxy+}
            Method: ANY
```

**Monthly Cost Estimate** (1M requests, 500ms avg):
- Lambda: $0.20 (compute) + $0.20 (requests) = $0.40
- API Gateway: $3.50
- Total: ~$4/month per service

---

### 3. AWS EKS with Spot Instances (Best for Kubernetes)

**Cost**: 60-70% cheaper than on-demand

```yaml
# eksctl cluster config
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: cost-effective-cluster
  region: us-east-1

managedNodeGroups:
  - name: spot-nodes
    instanceTypes:
      - t3.medium
      - t3a.medium
      - t2.medium
    spot: true
    minSize: 2
    maxSize: 10
    desiredCapacity: 3
    labels:
      workload-type: spot
    tags:
      nodegroup-type: spot
```

**Monthly Cost Estimate** (3 t3.medium spot instances):
- EKS Control Plane: $73
- 3x t3.medium spot (~$0.0125/hr): ~$27
- Total: ~$100/month

---

### 4. AWS App Runner (Simplest Deployment)

**Cost**: Automatic scaling, pay for usage

```yaml
# apprunner.yaml
version: 1.0
runtime: python3
build:
  commands:
    build:
      - pip install -r requirements.txt
run:
  command: gunicorn app:app
  network:
    port: 8080
  env:
    - name: ENV
      value: production
```

```bash
# Deploy
aws apprunner create-service \
  --service-name my-service \
  --source-configuration '{
    "CodeRepository": {
      "RepositoryUrl": "https://github.com/user/repo",
      "SourceCodeVersion": {"Type": "BRANCH", "Value": "main"}
    },
    "AutoDeploymentsEnabled": true
  }' \
  --instance-configuration '{
    "Cpu": "0.25 vCPU",
    "Memory": "0.5 GB"
  }'
```

**Monthly Cost Estimate** (low traffic):
- Provisioned: $5/month (0.25 vCPU, 0.5 GB)
- Compute: ~$10/month
- Total: ~$15/month per service

---

### 5. AWS Lightsail Containers (Budget-Friendly)

**Cost**: Fixed pricing, predictable costs

```bash
# Deploy container
aws lightsail create-container-service \
  --service-name my-microservice \
  --power micro \
  --scale 1

# Push deployment
aws lightsail create-container-service-deployment \
  --service-name my-microservice \
  --containers '{
    "app": {
      "image": "my-app:latest",
      "ports": {"8080": "HTTP"}
    }
  }' \
  --public-endpoint '{
    "containerName": "app",
    "containerPort": 8080
  }'
```

**Monthly Cost**:
- Micro (512 MB, 0.25 vCPU): $7/month
- Small (1 GB, 0.5 vCPU): $10/month
- Medium (2 GB, 1 vCPU): $20/month

---

## GCP Solutions

### 1. Cloud Run (Best Overall Value)

**Cost**: Pay only for requests, auto-scales to zero

```yaml
# service.yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: my-microservice
spec:
  template:
    metadata:
      annotations:
        autoscaling.knative.dev/minScale: "0"
        autoscaling.knative.dev/maxScale: "10"
    spec:
      containers:
      - image: gcr.io/project/my-app:latest
        ports:
        - containerPort: 8080
        resources:
          limits:
            cpu: "1"
            memory: 512Mi
```

```bash
# Deploy
gcloud run deploy my-microservice \
  --image gcr.io/project/my-app:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --min-instances 0 \
  --max-instances 10 \
  --cpu 1 \
  --memory 512Mi
```

**Monthly Cost Estimate** (1M requests, 500ms avg):
- CPU: $0.024 per vCPU-second = ~$12
- Memory: $0.0025 per GiB-second = ~$0.60
- Requests: $0.40 per million = $0.40
- Total: ~$13/month per service

---

### 2. GKE Autopilot (Managed Kubernetes)

**Cost**: Pay only for pods, no node management

```yaml
# Create Autopilot cluster
gcloud container clusters create-auto cost-effective-cluster \
  --region us-central1 \
  --release-channel regular
```

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-microservice
spec:
  replicas: 2
  selector:
    matchLabels:
      app: my-microservice
  template:
    metadata:
      labels:
        app: my-microservice
    spec:
      containers:
      - name: app
        image: gcr.io/project/my-app:latest
        resources:
          requests:
            cpu: 250m
            memory: 512Mi
          limits:
            cpu: 500m
            memory: 1Gi
```

**Monthly Cost Estimate** (2 pods, 0.25 CPU, 512MB each):
- Pod compute: ~$25/month
- No control plane cost (included)
- Total: ~$25/month

---

### 3. Cloud Functions (Event-Driven Services)

**Cost**: Pay per invocation

```javascript
// index.js
exports.myMicroservice = (req, res) => {
  res.json({ message: 'Hello from Cloud Function' });
};
```

```bash
# Deploy
gcloud functions deploy my-microservice \
  --runtime nodejs18 \
  --trigger-http \
  --allow-unauthenticated \
  --memory 256MB \
  --timeout 60s
```

**Monthly Cost Estimate** (1M invocations):
- Invocations: $0.40
- Compute: ~$2.50
- Total: ~$3/month per function

---

### 4. GKE Standard with Spot VMs

**Cost**: 60-91% cheaper than regular VMs

```yaml
# cluster.yaml
gcloud container clusters create cost-effective-cluster \
  --region us-central1 \
  --num-nodes 1 \
  --machine-type e2-medium \
  --spot
```

```yaml
# Node pool with spot instances
gcloud container node-pools create spot-pool \
  --cluster cost-effective-cluster \
  --region us-central1 \
  --spot \
  --machine-type e2-medium \
  --num-nodes 2 \
  --enable-autoscaling \
  --min-nodes 1 \
  --max-nodes 5
```

**Monthly Cost Estimate**:
- Control plane: $73/month
- 3x e2-medium spot (~$0.008/hr): ~$17
- Total: ~$90/month

---

## Azure Solutions

### 1. Azure Container Apps (Best Value)

**Cost**: Consumption-based pricing

```bash
# Create Container App
az containerapp create \
  --name my-microservice \
  --resource-group myResourceGroup \
  --environment myEnvironment \
  --image myregistry.azurecr.io/my-app:latest \
  --target-port 8080 \
  --ingress external \
  --min-replicas 0 \
  --max-replicas 10 \
  --cpu 0.25 \
  --memory 0.5Gi
```

```yaml
# containerapp.yaml
properties:
  configuration:
    ingress:
      external: true
      targetPort: 8080
    secrets: []
  template:
    containers:
    - image: myregistry.azurecr.io/my-app:latest
      name: my-app
      resources:
        cpu: 0.25
        memory: 0.5Gi
    scale:
      minReplicas: 0
      maxReplicas: 10
```

**Monthly Cost Estimate** (low traffic):
- vCPU: $0.000024/second = ~$15
- Memory: $0.000003/second = ~$2
- Requests: $0.40 per million
- Total: ~$17/month per service

---

### 2. Azure Functions (Consumption Plan)

**Cost**: Pay per execution

```csharp
// Function.cs
[FunctionName("MyMicroservice")]
public static async Task<IActionResult> Run(
    [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post")] HttpRequest req,
    ILogger log)
{
    return new OkObjectResult(new { message = "Hello from Azure Function" });
}
```

```bash
# Deploy
func azure functionapp publish my-function-app
```

**Monthly Cost Estimate** (1M executions):
- Executions: $0.20
- Compute: ~$1
- Total: ~$1.20/month per function

---

### 3. AKS with Spot Node Pools

**Cost**: Up to 90% discount

```bash
# Create AKS cluster
az aks create \
  --resource-group myResourceGroup \
  --name cost-effective-aks \
  --node-count 1 \
  --node-vm-size Standard_B2s \
  --enable-cluster-autoscaler \
  --min-count 1 \
  --max-count 5

# Add spot node pool
az aks nodepool add \
  --resource-group myResourceGroup \
  --cluster-name cost-effective-aks \
  --name spotpool \
  --priority Spot \
  --eviction-policy Delete \
  --spot-max-price -1 \
  --node-count 2 \
  --node-vm-size Standard_B2s \
  --enable-cluster-autoscaler \
  --min-count 1 \
  --max-count 5
```

**Monthly Cost Estimate**:
- Control plane: Free (with SLA: $73)
- 3x Standard_B2s spot (~$0.004/hr): ~$9
- Total: ~$9-82/month

---

### 4. Azure Container Instances (Simple Deployment)

**Cost**: Per-second billing

```bash
# Deploy container
az container create \
  --resource-group myResourceGroup \
  --name my-microservice \
  --image myregistry.azurecr.io/my-app:latest \
  --cpu 1 \
  --memory 1 \
  --ports 8080 \
  --dns-name-label my-microservice \
  --restart-policy OnFailure
```

**Monthly Cost Estimate** (1 vCPU, 1 GB):
- Linux: ~$30/month
- Windows: ~$100/month

---

## Kubernetes Cost Optimization

### 1. Use Spot/Preemptible Instances

```yaml
# Deployment with spot node affinity
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-microservice
spec:
  replicas: 3
  template:
    spec:
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
              - key: kubernetes.io/lifecycle
                operator: In
                values:
                - spot
      tolerations:
      - key: "spot"
        operator: "Equal"
        value: "true"
        effect: "NoSchedule"
      containers:
      - name: app
        image: my-app:latest
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 200m
            memory: 256Mi
```

**Savings**: 60-90% on compute costs

---

### 2. Horizontal Pod Autoscaler (HPA)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: my-microservice-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-microservice
  minReplicas: 1
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

**Savings**: 30-50% by scaling down during low traffic

---

### 3. Vertical Pod Autoscaler (VPA)

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: my-microservice-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-microservice
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: app
      minAllowed:
        cpu: 50m
        memory: 64Mi
      maxAllowed:
        cpu: 500m
        memory: 512Mi
```

**Savings**: 20-40% by right-sizing resources

---

### 4. Cluster Autoscaler

```yaml
# AWS EKS
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-autoscaler-priority-expander
  namespace: kube-system
data:
  priorities: |-
    10:
      - .*-spot-.*
    50:
      - .*-on-demand-.*
```

```bash
# Install cluster autoscaler
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
```

**Savings**: 40-60% by scaling nodes based on demand

---

### 5. KEDA (Kubernetes Event-Driven Autoscaling)

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: my-microservice-scaler
spec:
  scaleTargetRef:
    name: my-microservice
  minReplicaCount: 0  # Scale to zero
  maxReplicaCount: 10
  triggers:
  - type: prometheus
    metadata:
      serverAddress: http://prometheus:9090
      metricName: http_requests_total
      threshold: '100'
      query: sum(rate(http_requests_total[1m]))
```

**Savings**: 50-70% by scaling to zero during idle periods

---

### 6. Resource Quotas and Limits

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-quota
  namespace: production
spec:
  hard:
    requests.cpu: "10"
    requests.memory: 20Gi
    limits.cpu: "20"
    limits.memory: 40Gi
    pods: "50"
```

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: resource-limits
  namespace: production
spec:
  limits:
  - max:
      cpu: "2"
      memory: 2Gi
    min:
      cpu: 50m
      memory: 64Mi
    default:
      cpu: 200m
      memory: 256Mi
    defaultRequest:
      cpu: 100m
      memory: 128Mi
    type: Container
```

**Savings**: Prevents resource waste and over-provisioning

---

### 7. Pod Disruption Budgets (PDB)

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: my-microservice-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: my-microservice
```

**Benefit**: Allows safe use of spot instances with graceful handling

---

### 8. Node Affinity for Cost Optimization

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: batch-job
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node.kubernetes.io/instance-type
                operator: In
                values:
                - t3.micro
                - t3.small
      containers:
      - name: batch
        image: batch-job:latest
```

---

## Multi-Cloud Comparison

### Cost Comparison (3 Microservices, Low-Medium Traffic)

| Solution | AWS | GCP | Azure | Monthly Cost |
|----------|-----|-----|-------|--------------|
| **Serverless** | Lambda + API Gateway | Cloud Run | Container Apps | $10-20 |
| **Containers (Managed)** | Fargate Spot | Cloud Run | Container Apps | $45-60 |
| **Kubernetes (Spot)** | EKS + Spot | GKE Autopilot | AKS + Spot | $90-100 |
| **Simple Container** | Lightsail | Cloud Run | Container Instances | $20-30 |
| **App Platform** | App Runner | Cloud Run | Container Apps | $45-50 |

---

### Feature Comparison

| Feature | AWS | GCP | Azure |
|---------|-----|-----|-------|
| **Scale to Zero** | Lambda, App Runner | Cloud Run, Functions | Container Apps, Functions |
| **Spot Instances** | EC2 Spot, Fargate Spot | Preemptible VMs | Spot VMs |
| **Managed K8s** | EKS | GKE, GKE Autopilot | AKS |
| **Serverless Containers** | Fargate, App Runner | Cloud Run | Container Apps |
| **Free Tier** | Limited | Generous | Limited |
| **Pricing Model** | Complex | Simple | Moderate |

---

## Cost Optimization Strategies

### 1. Right-Sizing Resources

```yaml
# Before (over-provisioned)
resources:
  requests:
    cpu: 1000m
    memory: 2Gi
  limits:
    cpu: 2000m
    memory: 4Gi

# After (right-sized)
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 200m
    memory: 512Mi
```

**Savings**: 80-90% on compute costs

---

### 2. Use Reserved Instances/Savings Plans

```bash
# AWS Savings Plan (1-year commitment)
# 3x t3.medium on-demand: $90/month
# 3x t3.medium with savings plan: $60/month
# Savings: 33%

# GCP Committed Use Discounts (1-year)
# 3x e2-medium on-demand: $73/month
# 3x e2-medium committed: $47/month
# Savings: 36%

# Azure Reserved Instances (1-year)
# 3x B2s on-demand: $60/month
# 3x B2s reserved: $40/month
# Savings: 33%
```

---

### 3. Multi-Tenancy

```yaml
# Single cluster for multiple environments
apiVersion: v1
kind: Namespace
metadata:
  name: dev
---
apiVersion: v1
kind: Namespace
metadata:
  name: staging
---
apiVersion: v1
kind: Namespace
metadata:
  name: production
```

**Savings**: 50-70% by sharing infrastructure

---

### 4. Service Mesh for Efficiency

```yaml
# Istio for traffic management
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-service
spec:
  hosts:
  - my-service
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: my-service
        subset: v2
      weight: 10
    - destination:
        host: my-service
        subset: v1
      weight: 90
```

**Benefit**: Gradual rollouts reduce failed deployments and waste

---

### 5. Caching Strategy

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
data:
  redis.conf: |
    maxmemory 256mb
    maxmemory-policy allkeys-lru
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        resources:
          requests:
            cpu: 100m
            memory: 256Mi
```

**Savings**: 40-60% reduction in database costs

---

### 6. Database Optimization

```yaml
# Use read replicas
apiVersion: v1
kind: Service
metadata:
  name: postgres-read
spec:
  selector:
    app: postgres
    role: replica
  ports:
  - port: 5432
---
# Use connection pooling
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pgbouncer
spec:
  template:
    spec:
      containers:
      - name: pgbouncer
        image: pgbouncer/pgbouncer
        env:
        - name: MAX_CLIENT_CONN
          value: "1000"
        - name: DEFAULT_POOL_SIZE
          value: "25"
```

**Savings**: 30-50% on database costs

---

### 7. Observability Cost Control

```yaml
# Prometheus with retention limits
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 30s  # Increase interval
      evaluation_interval: 30s
    storage:
      tsdb:
        retention.time: 7d  # Reduce retention
        retention.size: 10GB
```

**Savings**: 50-70% on monitoring costs

---

### 8. CI/CD Optimization

```yaml
# GitHub Actions with caching
name: Build and Deploy
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/cache@v3
      with:
        path: ~/.m2/repository
        key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    - name: Build
      run: mvn clean package
```

**Savings**: 40-60% on CI/CD costs

---

## Recommended Architecture by Budget

### Budget: $50-100/month (Startup)

```yaml
# AWS: Lambda + API Gateway + RDS Serverless
# GCP: Cloud Run + Cloud SQL
# Azure: Container Apps + Azure SQL Serverless

Services:
  - 3-5 microservices
  - Serverless compute
  - Managed database (serverless)
  - CloudFront/CDN
  - S3/Cloud Storage

Estimated Cost: $50-100/month
```

---

### Budget: $100-500/month (Small Business)

```yaml
# AWS: ECS Fargate Spot + RDS
# GCP: GKE Autopilot + Cloud SQL
# Azure: Container Apps + Azure Database

Services:
  - 5-10 microservices
  - Container orchestration
  - Managed database
  - Redis cache
  - Load balancer

Estimated Cost: $200-400/month
```

---

### Budget: $500-2000/month (Medium Business)

```yaml
# AWS: EKS with Spot + RDS Multi-AZ
# GCP: GKE Standard with Spot + Cloud SQL HA
# Azure: AKS with Spot + Azure Database HA

Services:
  - 10-20 microservices
  - Kubernetes cluster
  - High-availability database
  - Redis cluster
  - Service mesh
  - Monitoring stack

Estimated Cost: $800-1500/month
```

---

### Budget: $2000+/month (Enterprise)

```yaml
# Multi-region, high availability setup

Services:
  - 20+ microservices
  - Multi-region Kubernetes
  - Database replication
  - CDN
  - Advanced monitoring
  - Security tools
  - Disaster recovery

Estimated Cost: $2000-5000/month
```

---

## Cost Monitoring Tools

### 1. Kubecost (Kubernetes)

```bash
# Install Kubecost
kubectl create namespace kubecost
helm repo add kubecost https://kubecost.github.io/cost-analyzer/
helm install kubecost kubecost/cost-analyzer \
  --namespace kubecost \
  --set kubecostToken="your-token"
```

### 2. AWS Cost Explorer

```bash
# Get cost and usage
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=SERVICE
```

### 3. GCP Cost Management

```bash
# Export billing data to BigQuery
gcloud beta billing accounts list
gcloud beta billing accounts get-iam-policy BILLING_ACCOUNT_ID
```

### 4. Azure Cost Management

```bash
# Get cost analysis
az consumption usage list \
  --start-date 2024-01-01 \
  --end-date 2024-01-31
```

---

## Best Practices Summary

1. **Start Serverless**: Use Lambda/Cloud Run/Container Apps for new projects
2. **Use Spot Instances**: 60-90% savings for fault-tolerant workloads
3. **Right-Size Resources**: Monitor and adjust CPU/memory requests
4. **Enable Autoscaling**: HPA, VPA, and cluster autoscaler
5. **Scale to Zero**: Use KEDA or serverless for idle services
6. **Cache Aggressively**: Redis/Memcached for frequently accessed data
7. **Use CDN**: CloudFront/Cloud CDN for static assets
8. **Monitor Costs**: Set up alerts and budgets
9. **Reserved Capacity**: Commit for 1-3 years for predictable workloads
10. **Multi-Tenancy**: Share infrastructure across environments

---

## Quick Decision Matrix

| Requirement | Best Choice | Monthly Cost |
|-------------|-------------|--------------|
| Low traffic (<10K req/day) | Lambda/Cloud Run/Functions | $5-20 |
| Medium traffic (10K-1M req/day) | Fargate Spot/Cloud Run | $50-200 |
| High traffic (>1M req/day) | EKS/GKE/AKS with Spot | $200-1000 |
| Batch processing | Lambda/Cloud Run Jobs | $10-50 |
| Real-time streaming | ECS/GKE/AKS | $100-500 |
| Microservices (5-10) | Cloud Run/Container Apps | $50-150 |
| Microservices (10-20) | EKS/GKE/AKS Autopilot | $200-500 |
| Enterprise (20+) | Multi-region K8s | $1000+ |

---

## Interview Tips

**Q: How to reduce Kubernetes costs?**
- Use spot instances (60-90% savings)
- Enable HPA and VPA
- Right-size resource requests
- Use KEDA for scale-to-zero
- Implement cluster autoscaler

**Q: Serverless vs Containers?**
- Serverless: Low traffic, event-driven, pay-per-use
- Containers: High traffic, long-running, predictable costs

**Q: Multi-cloud strategy?**
- Use Kubernetes for portability
- Terraform for infrastructure as code
- Avoid vendor-specific services for critical paths

**Q: Cost monitoring best practices?**
- Set up billing alerts
- Tag all resources
- Use cost allocation reports
- Review monthly spending
- Implement showback/chargeback
