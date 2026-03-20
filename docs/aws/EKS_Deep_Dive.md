# AWS EKS (Elastic Kubernetes Service) - Deep Dive

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Cluster Architecture](#cluster-architecture)
4. [Node Groups](#node-groups)
5. [Networking](#networking)
6. [Storage](#storage)
7. [Security](#security)
8. [Auto Scaling](#auto-scaling)
9. [Observability](#observability)
10. [Best Practices](#best-practices)

---

## Overview

**AWS EKS** is a managed Kubernetes service that runs Kubernetes control plane across multiple AZs.

### Key Benefits
- **Managed Control Plane**: AWS manages masters, etcd, API server
- **High Availability**: Control plane across 3 AZs
- **Kubernetes Native**: Standard Kubernetes APIs
- **AWS Integration**: IAM, VPC, ALB, EBS, EFS
- **Certified**: CNCF certified Kubernetes conformant

### EKS vs ECS vs Self-Managed K8s

| Feature | EKS | ECS | Self-Managed K8s |
|---------|-----|-----|------------------|
| Control Plane | Managed ($0.10/hr) | Managed (Free) | You manage |
| Learning Curve | High | Low | High |
| Portability | Multi-cloud | AWS only | Multi-cloud |
| Ecosystem | K8s ecosystem | AWS services | K8s ecosystem |
| Maturity | Kubernetes | AWS proprietary | Kubernetes |

### EKS Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    EKS Control Plane                         │
│              (Managed by AWS, Multi-AZ)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ API Server   │  │ etcd         │  │ Controller   │      │
│  │ Scheduler    │  │ (Multi-AZ)   │  │ Manager      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐     ┌────▼────┐     ┌────▼────┐
    │  Node   │     │  Node   │     │  Node   │
    │  (AZ-A) │     │  (AZ-B) │     │  (AZ-C) │
    │ ┌─────┐ │     │ ┌─────┐ │     │ ┌─────┐ │
    │ │ Pod │ │     │ │ Pod │ │     │ │ Pod │ │
    │ └─────┘ │     │ └─────┘ │     │ └─────┘ │
    └─────────┘     └─────────┘     └─────────┘
```

---

## Core Concepts

### 1. Kubernetes Basics

**Pod**: Smallest deployable unit (1+ containers)
**Deployment**: Manages ReplicaSets and Pods
**Service**: Exposes Pods as network service
**Namespace**: Virtual cluster for resource isolation
**ConfigMap**: Configuration data
**Secret**: Sensitive data (passwords, tokens)

### 2. EKS Components

**Control Plane**:
- API Server (kubectl endpoint)
- etcd (cluster state)
- Scheduler (pod placement)
- Controller Manager (reconciliation loops)

**Data Plane**:
- Worker nodes (EC2 or Fargate)
- kubelet (node agent)
- kube-proxy (network proxy)
- Container runtime (containerd)

### 3. EKS Pricing

**Control Plane**: $0.10/hr = $73/month per cluster

**Data Plane**:
- **EC2**: Instance costs (t3.medium = $30.37/month)
- **Fargate**: vCPU + memory (0.5 vCPU, 1 GB = $18/month)

**Example** (10 pods):
- EKS Control Plane: $73/month
- EC2 (2x t3.medium): $60.74/month
- **Total: $133.74/month**

---

## Cluster Architecture

### Create EKS Cluster

**Using eksctl** (recommended):
```bash
eksctl create cluster \
  --name production-cluster \
  --region us-east-1 \
  --version 1.28 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 10 \
  --managed
```

**Using AWS CLI**:
```bash
# Create cluster
aws eks create-cluster \
  --name production-cluster \
  --role-arn arn:aws:iam::123456789012:role/EKSClusterRole \
  --resources-vpc-config subnetIds=subnet-12345678,subnet-87654321,securityGroupIds=sg-12345678 \
  --kubernetes-version 1.28

# Wait for cluster to be active
aws eks wait cluster-active --name production-cluster

# Update kubeconfig
aws eks update-kubeconfig --name production-cluster --region us-east-1
```

### Cluster Configuration

**VPC Requirements**:
- Minimum 2 subnets in different AZs
- Subnets must have available IPs for pods
- Tag subnets: `kubernetes.io/cluster/<cluster-name> = shared`

**IAM Roles**:
- **Cluster Role**: Permissions for EKS control plane
- **Node Role**: Permissions for worker nodes
- **Pod Role**: Permissions for pods (IRSA)

### Kubernetes Versions

**Supported versions**: 1.25, 1.26, 1.27, 1.28 (latest)
**Support duration**: 14 months
**Upgrade path**: One minor version at a time (1.26 → 1.27 → 1.28)

---

## Node Groups

### 1. Managed Node Groups (Recommended)

**AWS manages**:
- Node provisioning
- Node updates
- Node termination
- Auto Scaling Group

**Create Managed Node Group**:
```bash
aws eks create-nodegroup \
  --cluster-name production-cluster \
  --nodegroup-name standard-workers \
  --node-role arn:aws:iam::123456789012:role/EKSNodeRole \
  --subnets subnet-12345678 subnet-87654321 \
  --instance-types t3.medium t3.large \
  --scaling-config minSize=2,maxSize=10,desiredSize=3 \
  --disk-size 20 \
  --labels environment=production,team=platform \
  --tags Key=Name,Value=eks-worker
```

**Update Strategy**:
```bash
# Rolling update (default)
aws eks update-nodegroup-version \
  --cluster-name production-cluster \
  --nodegroup-name standard-workers \
  --kubernetes-version 1.28

# Force update (ignore pod disruption budgets)
aws eks update-nodegroup-version \
  --cluster-name production-cluster \
  --nodegroup-name standard-workers \
  --force
```

### 2. Self-Managed Node Groups

**You manage**:
- EC2 instances
- Auto Scaling Group
- Launch template
- Updates and patches

**Use Cases**:
- Custom AMI
- GPU instances
- Instance store
- Specific kernel modules

### 3. Fargate Profiles

**Serverless nodes** (no EC2 management).

**Create Fargate Profile**:
```bash
aws eks create-fargate-profile \
  --cluster-name production-cluster \
  --fargate-profile-name backend-profile \
  --pod-execution-role-arn arn:aws:iam::123456789012:role/EKSFargatePodExecutionRole \
  --subnets subnet-12345678 subnet-87654321 \
  --selectors namespace=backend,labels={app=api}
```

**Fargate Pod Example**:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: backend-api
  namespace: backend
  labels:
    app: api
spec:
  containers:
  - name: api
    image: myapp:latest
    resources:
      requests:
        cpu: 500m
        memory: 1Gi
      limits:
        cpu: 1000m
        memory: 2Gi
```

**Fargate Limitations**:
- No DaemonSets
- No privileged containers
- No host networking
- No GPU support

### 4. Spot Instances

**Save up to 90%** using EC2 Spot.

**Managed Node Group with Spot**:
```bash
aws eks create-nodegroup \
  --cluster-name production-cluster \
  --nodegroup-name spot-workers \
  --node-role arn:aws:iam::123456789012:role/EKSNodeRole \
  --subnets subnet-12345678 subnet-87654321 \
  --instance-types t3.medium t3.large t3a.medium \
  --capacity-type SPOT \
  --scaling-config minSize=0,maxSize=20,desiredSize=5
```

**Best Practices**:
- Use multiple instance types
- Use Cluster Autoscaler or Karpenter
- Handle interruptions gracefully
- Use for fault-tolerant workloads

---

## Networking

### 1. VPC CNI Plugin

**AWS VPC CNI** assigns VPC IP addresses to pods.

**Characteristics**:
- Pods get VPC IP addresses
- Pods can communicate with VPC resources directly
- Security groups for pods
- High performance (no overlay network)

**IP Address Calculation**:
```
t3.medium: 3 ENIs × 6 IPs = 18 IPs per node
- 1 IP for node
- 17 IPs for pods
```

**Max Pods per Node**:
```
t3.small:  11 pods
t3.medium: 17 pods
t3.large:  35 pods
t3.xlarge: 58 pods
m5.large:  29 pods
m5.xlarge: 58 pods
```

### 2. Service Types

**ClusterIP** (default):
```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
  - port: 80
    targetPort: 8080
```

**NodePort**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: NodePort
  selector:
    app: backend
  ports:
  - port: 80
    targetPort: 8080
    nodePort: 30080
```

**LoadBalancer** (Classic Load Balancer):
```yaml
apiVersion: v1
kind: Service
metadata:
  name: web-service
spec:
  type: LoadBalancer
  selector:
    app: web
  ports:
  - port: 80
    targetPort: 8080
```

### 3. AWS Load Balancer Controller

**Modern load balancing** (ALB, NLB).

**Install**:
```bash
# Add IAM policy
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json
aws iam create-policy --policy-name AWSLoadBalancerControllerIAMPolicy --policy-document file://iam_policy.json

# Install controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=production-cluster \
  --set serviceAccount.create=true \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=arn:aws:iam::123456789012:role/AmazonEKSLoadBalancerControllerRole
```

**Ingress (ALB)**:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: web-ingress
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /health
spec:
  ingressClassName: alb
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
```

**Service (NLB)**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: web-service
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: external
    service.beta.kubernetes.io/aws-load-balancer-nlb-target-type: ip
    service.beta.kubernetes.io/aws-load-balancer-scheme: internet-facing
spec:
  type: LoadBalancer
  selector:
    app: web
  ports:
  - port: 80
    targetPort: 8080
```

### 4. Network Policies

**Control pod-to-pod traffic** (requires Calico or Cilium).

**Install Calico**:
```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/master/manifests/calico-vxlan.yaml
```

**Network Policy Example**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-policy
  namespace: backend
spec:
  podSelector:
    matchLabels:
      app: api
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 5432
```

---

## Storage

### 1. Amazon EBS CSI Driver

**Persistent volumes** backed by EBS.

**Install**:
```bash
# Add IAM policy
aws iam create-policy \
  --policy-name AmazonEKS_EBS_CSI_Driver_Policy \
  --policy-document file://ebs-csi-policy.json

# Install driver
kubectl apply -k "github.com/kubernetes-sigs/aws-ebs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.25"
```

**StorageClass**:
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-gp3
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

**PersistentVolumeClaim**:
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
spec:
  accessModes:
  - ReadWriteOnce
  storageClassName: ebs-gp3
  resources:
    requests:
      storage: 100Gi
```

**Pod with PVC**:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: postgres
spec:
  containers:
  - name: postgres
    image: postgres:15
    volumeMounts:
    - name: data
      mountPath: /var/lib/postgresql/data
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: postgres-pvc
```

### 2. Amazon EFS CSI Driver

**Shared file system** across pods.

**Install**:
```bash
kubectl apply -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.7"
```

**StorageClass**:
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: efs-sc
provisioner: efs.csi.aws.com
parameters:
  provisioningMode: efs-ap
  fileSystemId: fs-12345678
  directoryPerms: "700"
```

**PersistentVolumeClaim**:
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: efs-pvc
spec:
  accessModes:
  - ReadWriteMany
  storageClassName: efs-sc
  resources:
    requests:
      storage: 100Gi
```

---

## Security

### 1. IAM Roles for Service Accounts (IRSA)

**Pod-level IAM permissions** (recommended).

**Create OIDC Provider**:
```bash
eksctl utils associate-iam-oidc-provider \
  --cluster production-cluster \
  --approve
```

**Create IAM Role**:
```bash
eksctl create iamserviceaccount \
  --cluster production-cluster \
  --namespace backend \
  --name s3-access \
  --attach-policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess \
  --approve
```

**Use in Pod**:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: backend-api
  namespace: backend
spec:
  serviceAccountName: s3-access
  containers:
  - name: api
    image: myapp:latest
    env:
    - name: AWS_REGION
      value: us-east-1
```

### 2. Pod Security Standards

**Enforce security policies**:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

### 3. Secrets Management

**AWS Secrets Manager**:
```bash
# Install Secrets Store CSI Driver
helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
helm install csi-secrets-store secrets-store-csi-driver/secrets-store-csi-driver --namespace kube-system

# Install AWS Provider
kubectl apply -f https://raw.githubusercontent.com/aws/secrets-store-csi-driver-provider-aws/main/deployment/aws-provider-installer.yaml
```

**SecretProviderClass**:
```yaml
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: aws-secrets
spec:
  provider: aws
  parameters:
    objects: |
      - objectName: "db-password"
        objectType: "secretsmanager"
```

---

## Auto Scaling

### 1. Horizontal Pod Autoscaler (HPA)

**Scale pods** based on CPU/memory/custom metrics.

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: web-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: web
  minReplicas: 2
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
```

### 2. Cluster Autoscaler

**Scale nodes** based on pending pods.

**Install**:
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
```

**Configure**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  template:
    spec:
      containers:
      - name: cluster-autoscaler
        image: registry.k8s.io/autoscaling/cluster-autoscaler:v1.28.0
        command:
        - ./cluster-autoscaler
        - --v=4
        - --stderrthreshold=info
        - --cloud-provider=aws
        - --skip-nodes-with-local-storage=false
        - --expander=least-waste
        - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/production-cluster
```

### 3. Karpenter (Recommended)

**Fast, flexible node provisioning**.

**Install**:
```bash
helm repo add karpenter https://charts.karpenter.sh
helm install karpenter karpenter/karpenter \
  --namespace karpenter \
  --create-namespace \
  --set clusterName=production-cluster \
  --set clusterEndpoint=$(aws eks describe-cluster --name production-cluster --query "cluster.endpoint" --output text)
```

**Provisioner**:
```yaml
apiVersion: karpenter.sh/v1alpha5
kind: Provisioner
metadata:
  name: default
spec:
  requirements:
  - key: karpenter.sh/capacity-type
    operator: In
    values: ["spot", "on-demand"]
  - key: node.kubernetes.io/instance-type
    operator: In
    values: ["t3.medium", "t3.large", "t3a.medium"]
  limits:
    resources:
      cpu: 1000
      memory: 1000Gi
  providerRef:
    name: default
  ttlSecondsAfterEmpty: 30
```

---

## Best Practices

### 1. Cluster Design
- Use managed node groups
- Deploy across 3 AZs
- Use multiple node groups (general, compute, memory)
- Enable control plane logging

### 2. Networking
- Use AWS Load Balancer Controller
- Implement Network Policies
- Use private subnets for nodes
- Plan IP address space carefully

### 3. Security
- Use IRSA for pod permissions
- Enable Pod Security Standards
- Use Secrets Manager for sensitive data
- Regularly update Kubernetes version

### 4. Cost Optimization
- Use Spot instances (Karpenter)
- Right-size pods (requests/limits)
- Use HPA for dynamic scaling
- Use Fargate for low-utilization workloads

### 5. Observability
- Enable control plane logging
- Use Container Insights
- Implement distributed tracing
- Set up alerts for critical metrics

---

## Summary

**AWS EKS** provides managed Kubernetes on AWS:
- **Managed Control Plane**: AWS manages masters
- **High Availability**: Multi-AZ control plane
- **AWS Integration**: IAM, VPC, ALB, EBS, EFS
- **Scalability**: Cluster Autoscaler, Karpenter, HPA
- **Security**: IRSA, Pod Security, Network Policies

**Key Takeaways**:
1. Use managed node groups for simplicity
2. Use Karpenter for efficient node scaling
3. Use IRSA for pod-level IAM permissions
4. Use AWS Load Balancer Controller for ALB/NLB
5. Use Spot instances for cost savings
6. Enable control plane logging
7. Regularly update Kubernetes version

**EKS is ideal for**:
- Kubernetes expertise in team
- Multi-cloud portability
- Rich Kubernetes ecosystem
- Complex microservices architectures

**Cost**: $73/month (control plane) + node costs
