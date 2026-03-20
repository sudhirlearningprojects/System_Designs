# AWS ECS (Elastic Container Service) - Deep Dive

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Launch Types](#launch-types)
4. [Task Definitions](#task-definitions)
5. [Services](#services)
6. [Networking](#networking)
7. [Storage](#storage)
8. [Auto Scaling](#auto-scaling)
9. [Load Balancing](#load-balancing)
10. [Best Practices](#best-practices)

---

## Overview

**AWS ECS** is a fully managed container orchestration service for running Docker containers at scale.

### Key Benefits
- **Fully Managed**: No control plane to manage
- **AWS Integration**: Native integration with ALB, CloudWatch, IAM, Secrets Manager
- **Cost Effective**: No additional charge (pay for EC2/Fargate only)
- **Scalable**: Run thousands of containers
- **Secure**: IAM roles, Security Groups, VPC isolation

### ECS vs EKS vs Self-Managed

| Feature | ECS | EKS | Self-Managed K8s |
|---------|-----|-----|------------------|
| Control Plane | Managed (Free) | Managed ($0.10/hr) | You manage |
| Learning Curve | Low | High | High |
| AWS Integration | Native | Good | Manual |
| Portability | AWS only | Multi-cloud | Multi-cloud |
| Ecosystem | AWS services | K8s ecosystem | K8s ecosystem |

### ECS Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    ECS Cluster                               │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              ECS Service (web-service)                  │ │
│  │  Desired Count: 4  |  Running: 4                       │ │
│  │                                                          │ │
│  │  ┌──────────────┐  ┌──────────────┐                    │ │
│  │  │   EC2/Fargate│  │   EC2/Fargate│                    │ │
│  │  │   ┌────────┐ │  │   ┌────────┐ │                    │ │
│  │  │   │  Task  │ │  │   │  Task  │ │                    │ │
│  │  │   │┌──────┐│ │  │   │┌──────┐│ │                    │ │
│  │  │   ││ Cont ││ │  │   ││ Cont ││ │                    │ │
│  │  │   │└──────┘│ │  │   │└──────┘│ │                    │ │
│  │  │   └────────┘ │  │   └────────┘ │                    │ │
│  │  └──────────────┘  └──────────────┘                    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Application Load Balancer                       │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Concepts

### 1. Cluster
**Logical grouping** of tasks or services.

**Create Cluster**:
```bash
aws ecs create-cluster --cluster-name production-cluster
```

**Cluster Types**:
- **Fargate**: Serverless (no EC2 management)
- **EC2**: You manage EC2 instances
- **External**: On-premises or other cloud

### 2. Task Definition
**Blueprint** for your application (like Dockerfile for containers).

**Components**:
- Container definitions (image, CPU, memory, ports)
- Task role (IAM permissions for containers)
- Execution role (IAM permissions for ECS agent)
- Network mode (bridge, host, awsvpc)
- Volumes (EFS, Docker volumes)

### 3. Task
**Running instance** of a task definition.

**Task States**:
```
PROVISIONING → PENDING → RUNNING → DEPROVISIONING → STOPPED
```

### 4. Service
**Maintains desired number** of tasks (like Deployment in Kubernetes).

**Features**:
- Auto-restart failed tasks
- Load balancer integration
- Auto Scaling
- Rolling updates
- Blue/green deployments

### 5. Container Instance
**EC2 instance** running ECS agent (EC2 launch type only).

---

## Launch Types

### 1. Fargate (Serverless)

**Characteristics**:
- **No EC2 management**: AWS manages infrastructure
- **Pay per task**: vCPU and memory per second
- **Isolation**: Each task gets dedicated resources
- **Cold start**: 30-60 seconds

**Pricing** (us-east-1):
- vCPU: $0.04048/hr per vCPU
- Memory: $0.004445/hr per GB

**Example** (0.5 vCPU, 1 GB RAM):
- Cost: (0.5 × $0.04048) + (1 × $0.004445) = $0.024685/hr
- Monthly: $18/month per task

**Use Cases**:
- Microservices
- Batch jobs
- CI/CD pipelines
- Variable workloads

### 2. EC2 Launch Type

**Characteristics**:
- **You manage EC2**: Choose instance types, AMI, scaling
- **Pay for EC2**: Instance hours (not per task)
- **Higher density**: Run multiple tasks per instance
- **More control**: Custom AMI, instance store, GPU

**Pricing** (t3.medium):
- Instance: $0.0416/hr = $30.37/month
- Can run 4-8 tasks per instance
- Cost per task: $3.80-$7.60/month

**Use Cases**:
- High-density workloads
- GPU workloads
- Cost optimization (Reserved Instances)
- Custom requirements

### 3. Fargate vs EC2 Cost Comparison

**Scenario**: 10 tasks (0.5 vCPU, 1 GB each)

**Fargate**:
- 10 tasks × $18/month = $180/month

**EC2** (t3.medium, 2 vCPU, 4 GB):
- 2 instances × $30.37/month = $60.74/month
- **Savings: 66%**

**Break-even**: Fargate cheaper for <30% utilization, EC2 cheaper for >30%.

---

## Task Definitions

### Basic Task Definition

```json
{
  "family": "web-app",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789012:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "web",
      "image": "nginx:latest",
      "cpu": 256,
      "memory": 512,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 80,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "ENV",
          "value": "production"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/web-app",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "web"
        }
      }
    }
  ]
}
```

**Register Task Definition**:
```bash
aws ecs register-task-definition --cli-input-json file://task-definition.json
```

### Multi-Container Task Definition

```json
{
  "family": "app-with-sidecar",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "myapp:latest",
      "cpu": 768,
      "memory": 1536,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "dependsOn": [
        {
          "containerName": "envoy",
          "condition": "HEALTHY"
        }
      ]
    },
    {
      "name": "envoy",
      "image": "envoyproxy/envoy:latest",
      "cpu": 256,
      "memory": 512,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 9901,
          "protocol": "tcp"
        }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:9901/ready || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

### Task Definition Parameters

**CPU and Memory** (Fargate):
| vCPU | Memory Options |
|------|----------------|
| 0.25 | 0.5 GB, 1 GB, 2 GB |
| 0.5  | 1 GB - 4 GB (1 GB increments) |
| 1    | 2 GB - 8 GB (1 GB increments) |
| 2    | 4 GB - 16 GB (1 GB increments) |
| 4    | 8 GB - 30 GB (1 GB increments) |

**Network Modes**:
- **awsvpc**: Each task gets ENI (Fargate only supports this)
- **bridge**: Docker bridge network (EC2 only)
- **host**: Host network (EC2 only)
- **none**: No network

**IAM Roles**:
- **Task Role**: Permissions for application (S3, DynamoDB, etc.)
- **Execution Role**: Permissions for ECS agent (pull image, write logs)

---

## Services

### Create Service

```bash
aws ecs create-service \
  --cluster production-cluster \
  --service-name web-service \
  --task-definition web-app:1 \
  --desired-count 4 \
  --launch-type FARGATE \
  --network-configuration '{
    "awsvpcConfiguration": {
      "subnets": ["subnet-12345678", "subnet-87654321"],
      "securityGroups": ["sg-12345678"],
      "assignPublicIp": "DISABLED"
    }
  }' \
  --load-balancers '[
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/web-tg/abc123",
      "containerName": "web",
      "containerPort": 80
    }
  ]' \
  --health-check-grace-period-seconds 60 \
  --deployment-configuration '{
    "maximumPercent": 200,
    "minimumHealthyPercent": 100,
    "deploymentCircuitBreaker": {
      "enable": true,
      "rollback": true
    }
  }'
```

### Service Parameters

**Desired Count**: Number of tasks to run

**Deployment Configuration**:
- **maximumPercent**: Max % of desired count during deployment (200 = double)
- **minimumHealthyPercent**: Min % of desired count during deployment (100 = no downtime)

**Example** (Desired: 4):
```
maximumPercent: 200, minimumHealthyPercent: 100
→ Deploy 4 new tasks, then stop 4 old tasks (8 total during deployment)

maximumPercent: 150, minimumHealthyPercent: 50
→ Deploy 2 new tasks, stop 2 old tasks, repeat (6 total during deployment)
```

**Deployment Circuit Breaker**:
- Automatically roll back failed deployments
- Monitors task health during deployment
- Rolls back if tasks fail to start

### Service Discovery

**AWS Cloud Map** integration for service-to-service communication.

```bash
aws ecs create-service \
  --cluster production-cluster \
  --service-name backend-service \
  --task-definition backend:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration '{...}' \
  --service-registries '[
    {
      "registryArn": "arn:aws:servicediscovery:us-east-1:123456789012:service/srv-abc123"
    }
  ]'
```

**Access service**:
```bash
# From another ECS task
curl http://backend-service.local:8080
```

---

## Networking

### awsvpc Network Mode

**Each task gets**:
- Elastic Network Interface (ENI)
- Private IP address
- Security Group

**Benefits**:
- Task-level security groups
- VPC Flow Logs per task
- PrivateLink support

**Limitations**:
- ENI limits per instance (EC2 launch type)
- Slower task startup (ENI attachment)

### Security Groups

**Task-level security groups** (awsvpc mode):
```bash
# Web tier security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-web \
  --protocol tcp \
  --port 80 \
  --source-group sg-alb

# App tier security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-app \
  --protocol tcp \
  --port 8080 \
  --source-group sg-web
```

### VPC Endpoints

**Reduce NAT Gateway costs** by using VPC endpoints:
- **ECR**: Pull container images
- **S3**: Access S3 buckets
- **CloudWatch Logs**: Send logs
- **Secrets Manager**: Retrieve secrets

**Cost Savings**:
- NAT Gateway: $0.045/hr + $0.045/GB = $32.85/month + data
- VPC Endpoint: $0.01/hr = $7.30/month (no data charges)

---

## Storage

### 1. Ephemeral Storage

**Task storage** (deleted when task stops):
- Fargate: 20 GB (default), up to 200 GB
- EC2: Instance store or EBS

**Configure ephemeral storage** (Fargate):
```json
{
  "ephemeralStorage": {
    "sizeInGiB": 100
  }
}
```

**Pricing**: $0.000111/GB-hr (100 GB = $8.10/month)

### 2. Amazon EFS

**Persistent shared storage** across tasks.

**Task Definition**:
```json
{
  "volumes": [
    {
      "name": "efs-volume",
      "efsVolumeConfiguration": {
        "fileSystemId": "fs-12345678",
        "transitEncryption": "ENABLED",
        "authorizationConfig": {
          "accessPointId": "fsap-12345678",
          "iam": "ENABLED"
        }
      }
    }
  ],
  "containerDefinitions": [
    {
      "name": "app",
      "mountPoints": [
        {
          "sourceVolume": "efs-volume",
          "containerPath": "/mnt/efs",
          "readOnly": false
        }
      ]
    }
  ]
}
```

**Use Cases**:
- Shared configuration files
- User uploads
- ML model storage
- Content management

**Pricing**: $0.30/GB-month (Standard), $0.043/GB-month (Infrequent Access)

### 3. Docker Volumes

**EC2 launch type only**:
```json
{
  "volumes": [
    {
      "name": "docker-volume",
      "dockerVolumeConfiguration": {
        "scope": "shared",
        "autoprovision": true,
        "driver": "local"
      }
    }
  ]
}
```

---

## Auto Scaling

### Target Tracking Scaling

**Scale based on metrics**:
```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/production-cluster/web-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# CPU target tracking
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/production-cluster/web-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'

# Memory target tracking
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/production-cluster/web-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name memory-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 80.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageMemoryUtilization"
    }
  }'

# ALB request count tracking
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/production-cluster/web-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name request-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 1000.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ALBRequestCountPerTarget",
      "ResourceLabel": "app/web-alb/abc123/targetgroup/web-tg/def456"
    }
  }'
```

### Scheduled Scaling

```bash
# Scale up at 9 AM
aws application-autoscaling put-scheduled-action \
  --service-namespace ecs \
  --resource-id service/production-cluster/web-service \
  --scalable-dimension ecs:service:DesiredCount \
  --scheduled-action-name scale-up-morning \
  --schedule "cron(0 9 * * ? *)" \
  --scalable-target-action MinCapacity=5,MaxCapacity=20

# Scale down at 6 PM
aws application-autoscaling put-scheduled-action \
  --service-namespace ecs \
  --resource-id service/production-cluster/web-service \
  --scalable-dimension ecs:service:DesiredCount \
  --scheduled-action-name scale-down-evening \
  --schedule "cron(0 18 * * ? *)" \
  --scalable-target-action MinCapacity=2,MaxCapacity=10
```

---

## Load Balancing

### Application Load Balancer (ALB)

**Layer 7** (HTTP/HTTPS) load balancing.

**Features**:
- Path-based routing
- Host-based routing
- Health checks
- Sticky sessions
- WebSocket support

**Create Target Group**:
```bash
aws elbv2 create-target-group \
  --name web-tg \
  --protocol HTTP \
  --port 80 \
  --vpc-id vpc-12345678 \
  --target-type ip \
  --health-check-path /health \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3
```

**Note**: Use `target-type ip` for awsvpc network mode.

### Network Load Balancer (NLB)

**Layer 4** (TCP/UDP) load balancing.

**Use Cases**:
- Ultra-low latency
- Static IP addresses
- TCP/UDP protocols
- Millions of requests per second

---

## Best Practices

### 1. Task Definition
- Use specific image tags (not `latest`)
- Set resource limits (CPU, memory)
- Use health checks
- Enable logging (CloudWatch Logs)
- Use secrets for sensitive data (Secrets Manager, Parameter Store)

### 2. Service Configuration
- Set appropriate desired count (≥2 for HA)
- Use deployment circuit breaker
- Configure health check grace period
- Use service discovery for microservices

### 3. Networking
- Use private subnets for tasks
- Use VPC endpoints to reduce NAT costs
- Use task-level security groups
- Enable VPC Flow Logs

### 4. Cost Optimization
- Use Fargate Spot (70% savings)
- Use EC2 for high-density workloads
- Right-size CPU and memory
- Use scheduled scaling for predictable patterns

### 5. Security
- Use IAM roles (task role, execution role)
- Store secrets in Secrets Manager
- Enable encryption (EFS, CloudWatch Logs)
- Use private subnets
- Scan images for vulnerabilities

---

## Summary

**AWS ECS** is a powerful container orchestration service:
- **Fully Managed**: No control plane management
- **Flexible**: Fargate (serverless) or EC2 (more control)
- **Integrated**: Native AWS service integration
- **Scalable**: Auto Scaling, load balancing
- **Secure**: IAM, Security Groups, VPC isolation

**Key Takeaways**:
1. Use Fargate for simplicity, EC2 for cost optimization
2. Use awsvpc network mode for task-level security
3. Enable Auto Scaling for dynamic workloads
4. Use ALB for HTTP/HTTPS, NLB for TCP/UDP
5. Store secrets in Secrets Manager
6. Use VPC endpoints to reduce NAT costs
7. Enable deployment circuit breaker for safe deployments

**Next**: Learn about EKS for Kubernetes on AWS.
