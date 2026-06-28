# Deployment Guide

## 1. Local Development (Docker Compose)

```bash
# Clone and start
git clone https://github.com/sudhir512kj/system-designs.git
cd system-designs/anomaly-explainer

# Start all infrastructure
docker-compose up -d

# Verify services are healthy
docker-compose ps
# Expected: kafka, postgres, redis, flink-jobmanager, flink-taskmanager all UP

# Create tables
docker exec -it $(docker ps -q -f name=postgres) psql -U admin -d anomaly_explainer -f /docker-entrypoint-initdb.d/init.sql

# Seed knowledge base (requires OPENAI_API_KEY)
export OPENAI_API_KEY=sk-your-key
python seed_knowledge_base.py

# Start pipeline components (3 terminals)
# Terminal 1: Anomaly detector
python flink_anomaly_detector.py

# Terminal 2: RAG explainer service
python explainer_service.py

# Terminal 3: Metrics producer (anomaly after 60s)
python metrics_producer.py --anomaly-after 60 --service payment-service --type cpu_spike
```

---

## 2. Kubernetes Production Deployment

### 2.1 Namespace & Resources

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: anomaly-explainer
  labels:
    app.kubernetes.io/part-of: observability
```

### 2.2 RAG Service Deployment

```yaml
# rag-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-explainer
  namespace: anomaly-explainer
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rag-explainer
  template:
    metadata:
      labels:
        app: rag-explainer
    spec:
      containers:
        - name: rag-explainer
          image: your-registry/anomaly-explainer:latest
          ports:
            - containerPort: 8200
          env:
            - name: KAFKA_BOOTSTRAP_SERVERS
              value: "kafka-cluster:9092"
            - name: POSTGRES_HOST
              valueFrom:
                secretKeyRef:
                  name: anomaly-explainer-secrets
                  key: postgres-host
            - name: OPENAI_API_KEY
              valueFrom:
                secretKeyRef:
                  name: anomaly-explainer-secrets
                  key: openai-api-key
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1Gi"
          livenessProbe:
            httpGet:
              path: /api/v1/health
              port: 8200
            initialDelaySeconds: 10
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /api/v1/health
              port: 8200
            initialDelaySeconds: 5
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: rag-explainer
  namespace: anomaly-explainer
spec:
  selector:
    app: rag-explainer
  ports:
    - port: 8200
      targetPort: 8200
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rag-explainer-hpa
  namespace: anomaly-explainer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-explainer
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

### 2.3 Flink on Kubernetes

```yaml
# flink-deployment.yaml (using Flink Kubernetes Operator)
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: anomaly-detector
  namespace: anomaly-explainer
spec:
  image: flink:1.18-java17
  flinkVersion: v1_18
  flinkConfiguration:
    state.backend: rocksdb
    state.checkpoints.dir: s3://flink-state/anomaly-detector/checkpoints
    state.savepoints.dir: s3://flink-state/anomaly-detector/savepoints
    execution.checkpointing.interval: "30000"
    restart-strategy: fixed-delay
    restart-strategy.fixed-delay.attempts: "5"
    restart-strategy.fixed-delay.delay: "10s"
  serviceAccount: flink
  jobManager:
    resource:
      memory: "2g"
      cpu: 1
  taskManager:
    resource:
      memory: "4g"
      cpu: 2
    replicas: 3
  job:
    jarURI: s3://flink-jars/anomaly-detector-1.0.jar
    entryClass: org.sudhir512kj.anomalyexplainer.flink.AnomalyDetectionJob
    parallelism: 6
    upgradeMode: savepoint
```

### 2.4 Monitoring Stack

```yaml
# prometheus-servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: rag-explainer-monitor
  namespace: anomaly-explainer
spec:
  selector:
    matchLabels:
      app: rag-explainer
  endpoints:
    - port: metrics
      path: /metrics
      interval: 15s
```

---

## 3. AWS Production Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    AWS Account                            │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Amazon MSK (Kafka)                               │   │
│  │  - 3 brokers across 3 AZs                        │   │
│  │  - Topics: metrics.raw, anomalies.detected       │   │
│  └──────────────────────────────────────────────────┘   │
│                          │                               │
│  ┌──────────────────────▼──────────────────────────┐   │
│  │  Amazon EMR (Flink)                              │   │
│  │  - Anomaly detection job                         │   │
│  │  - Auto-scaling task managers                    │   │
│  └──────────────────────────────────────────────────┘   │
│                          │                               │
│  ┌──────────────────────▼──────────────────────────┐   │
│  │  ECS Fargate (RAG Service)                       │   │
│  │  - 3+ tasks across AZs                          │   │
│  │  - ALB for load balancing                        │   │
│  └──────────────────────────────────────────────────┘   │
│           │              │              │                 │
│  ┌────────▼───┐  ┌──────▼───┐  ┌──────▼──────────┐    │
│  │ RDS Postgres│  │ElastiCache│  │Amazon Bedrock   │    │
│  │ + pgvector │  │(Redis)    │  │(Claude/Titan)   │    │
│  │ Multi-AZ   │  │Cluster    │  │                 │    │
│  └────────────┘  └──────────┘  └─────────────────┘    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  S3: Flink checkpoints, incident archives        │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### IaC (Terraform snippet)

```hcl
# main.tf (key resources only)
module "msk" {
  source              = "terraform-aws-modules/msk-kafka-cluster/aws"
  cluster_name        = "anomaly-explainer"
  kafka_version       = "3.5.1"
  number_of_broker_nodes = 3
  broker_node_instance_type = "kafka.m5.large"
}

resource "aws_rds_cluster" "vector_db" {
  cluster_identifier = "anomaly-kb"
  engine            = "aurora-postgresql"
  engine_version    = "16.1"
  master_username   = "admin"
  master_password   = var.db_password
  database_name     = "anomaly_explainer"
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "anomaly-cache"
  node_type           = "cache.r6g.large"
  num_cache_clusters  = 3
  engine              = "redis"
  engine_version      = "7.0"
}
```

---

## 4. CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy Anomaly Explainer
on:
  push:
    branches: [main]
    paths: ['anomaly-explainer/**']

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: pip install -r requirements.txt
      - run: pytest tests/ -v
      - run: python test_detector.py

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: ${{ secrets.ECR_REGISTRY }}/anomaly-explainer:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: task-definition.json
          service: rag-explainer
          cluster: observability
          wait-for-service-stability: true
```
