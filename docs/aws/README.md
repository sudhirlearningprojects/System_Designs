# AWS Deep Dive Documentation

Comprehensive deep dive guides for core AWS compute and container services.

## 📚 Available Documentation

### 1. [AWS EC2 (Elastic Compute Cloud)](EC2_Deep_Dive.md)
**Virtual servers in the cloud**

**Topics Covered**:
- Instance types and families (General Purpose, Compute, Memory, Storage, GPU)
- Pricing models (On-Demand, Reserved, Spot, Savings Plans, Dedicated)
- Storage options (EBS, Instance Store, EFS, FSx)
- Networking (ENI, EIP, Enhanced Networking, Placement Groups)
- Security (Security Groups, IAM Roles, Key Pairs, Session Manager)
- High availability and scaling
- Monitoring with CloudWatch
- Real-world use cases and cost examples

**Key Highlights**:
- 500+ instance types for any workload
- Up to 90% savings with Spot Instances
- Sub-millisecond latency with Enhanced Networking
- Multi-AZ deployment for fault tolerance

---

### 2. [AWS VPC (Virtual Private Cloud)](VPC_Deep_Dive.md)
**Isolated virtual network in AWS**

**Topics Covered**:
- CIDR blocks and IP address planning
- Subnets (Public vs Private)
- Route tables and routing
- Internet Gateway and NAT Gateway
- Security (Security Groups vs NACLs)
- VPC Peering for private connectivity
- VPN and Direct Connect for hybrid cloud
- VPC endpoints for cost optimization
- Best practices for network design

**Key Highlights**:
- Complete control over network configuration
- Multi-layer security (Security Groups + NACLs)
- VPC endpoints reduce NAT Gateway costs by 80%
- VPC Peering for private inter-VPC communication

---

### 3. [AWS Auto Scaling](Auto_Scaling_Deep_Dive.md)
**Automatic capacity management**

**Topics Covered**:
- Launch Templates vs Launch Configurations
- Auto Scaling Groups (ASG) configuration
- Scaling policies (Target Tracking, Step, Simple, Scheduled, Predictive)
- Health checks (EC2 vs ELB)
- Lifecycle hooks for custom actions
- Integration with Load Balancers
- Cost optimization strategies
- Real-world examples (web apps, batch processing)

**Key Highlights**:
- Target Tracking scaling (simplest and most effective)
- Spot Instances for 70% cost savings
- Predictive Scaling with ML-based forecasting
- Multi-AZ deployment for high availability

---

### 4. [AWS ECS (Elastic Container Service)](ECS_Deep_Dive.md)
**Fully managed container orchestration**

**Topics Covered**:
- ECS vs EKS vs Self-Managed comparison
- Launch types (Fargate vs EC2)
- Task Definitions (single and multi-container)
- ECS Services and deployment strategies
- Networking (awsvpc mode, Security Groups)
- Storage (Ephemeral, EFS, Docker volumes)
- Auto Scaling (Target Tracking, Scheduled)
- Load Balancing (ALB, NLB)
- Cost optimization and best practices

**Key Highlights**:
- Fargate for serverless containers (no EC2 management)
- EC2 launch type for 66% cost savings (high-density workloads)
- Task-level IAM roles and Security Groups
- Deployment circuit breaker for safe deployments
- VPC endpoints reduce NAT costs by 80%

---

### 5. [AWS EKS (Elastic Kubernetes Service)](EKS_Deep_Dive.md)
**Managed Kubernetes on AWS**

**Topics Covered**:
- Kubernetes fundamentals
- EKS cluster architecture
- Node groups (Managed, Self-Managed, Fargate, Spot)
- Networking (VPC CNI, Service types, AWS Load Balancer Controller)
- Storage (EBS CSI, EFS CSI)
- Security (IRSA, Pod Security, Secrets Manager)
- Auto Scaling (HPA, Cluster Autoscaler, Karpenter)
- Observability and monitoring
- Best practices for production

**Key Highlights**:
- Managed control plane across 3 AZs
- IRSA for pod-level IAM permissions
- Karpenter for fast, flexible node provisioning
- Spot instances for up to 90% cost savings
- AWS Load Balancer Controller for ALB/NLB
- Standard Kubernetes APIs (CNCF certified)

---

## 🎯 Quick Comparison

### When to Use What?

| Use Case | Recommended Service | Why? |
|----------|---------------------|------|
| Traditional applications | **EC2** | Full control, mature ecosystem |
| Microservices (AWS-native) | **ECS Fargate** | Serverless, simple, cost-effective |
| Microservices (Kubernetes) | **EKS** | Portability, K8s ecosystem |
| Batch processing | **EC2 Spot** | 90% cost savings |
| Variable workloads | **ECS Fargate** | Pay per task, no idle capacity |
| High-density workloads | **ECS EC2** | Lower cost per container |
| Multi-cloud strategy | **EKS** | Kubernetes portability |
| Existing K8s apps | **EKS** | Lift and shift |

### Cost Comparison (10 containers, 0.5 vCPU, 1 GB each)

| Service | Monthly Cost | Notes |
|---------|--------------|-------|
| **EC2 (t3.medium)** | $60 | 2 instances, highest density |
| **ECS EC2** | $60 | Same as EC2, container orchestration |
| **ECS Fargate** | $180 | Serverless, no management |
| **EKS + EC2** | $133 | $73 control plane + $60 nodes |
| **EKS + Fargate** | $253 | $73 control plane + $180 Fargate |

**Key Insight**: EC2/ECS EC2 cheapest for steady-state, Fargate best for variable workloads.

---

## 🏗️ Architecture Patterns

### 1. Three-Tier Web Application
```
Internet → ALB → ECS Fargate (Web) → ECS Fargate (App) → RDS (Database)
```
**Services**: VPC, ECS Fargate, ALB, RDS, Auto Scaling
**Cost**: ~$200-500/month

### 2. Microservices Platform
```
Internet → ALB → EKS (API Gateway) → EKS (Services) → RDS/DynamoDB
```
**Services**: VPC, EKS, ALB, RDS, DynamoDB, Service Mesh
**Cost**: ~$500-2,000/month

### 3. Batch Processing Pipeline
```
S3 → Lambda → EC2 Spot Fleet → S3
```
**Services**: EC2 Spot, Auto Scaling, S3, Lambda
**Cost**: ~$100-500/month (70% savings with Spot)

### 4. High-Performance Computing
```
EC2 (Cluster Placement Group) → EFS → S3
```
**Services**: EC2 (c6i/p4d), EFS, S3, Placement Groups
**Cost**: ~$1,000-10,000/month

---

## 💡 Best Practices Summary

### Security
1. ✅ Use IAM roles (never store credentials)
2. ✅ Enable encryption at rest and in transit
3. ✅ Use Security Groups as primary firewall
4. ✅ Deploy in private subnets
5. ✅ Enable VPC Flow Logs
6. ✅ Use Secrets Manager for sensitive data
7. ✅ Regularly patch and update

### High Availability
1. ✅ Deploy across multiple AZs (minimum 2)
2. ✅ Use Auto Scaling for automatic recovery
3. ✅ Use Load Balancers for traffic distribution
4. ✅ Implement health checks
5. ✅ Regular backups (EBS snapshots, AMIs)
6. ✅ Use Multi-AZ for databases

### Cost Optimization
1. ✅ Use Reserved Instances/Savings Plans (40-70% savings)
2. ✅ Use Spot Instances for fault-tolerant workloads (90% savings)
3. ✅ Right-size instances (use CloudWatch metrics)
4. ✅ Use Auto Scaling to match demand
5. ✅ Stop non-production instances when not in use
6. ✅ Use VPC endpoints to reduce NAT costs
7. ✅ Delete unused resources (EBS volumes, snapshots, EIPs)

### Performance
1. ✅ Choose appropriate instance type for workload
2. ✅ Use Enhanced Networking
3. ✅ Use Placement Groups for low-latency workloads
4. ✅ Use EBS-optimized instances
5. ✅ Monitor CloudWatch metrics
6. ✅ Use caching (ElastiCache, CloudFront)

---

## 📖 Learning Path

### Beginner
1. Start with **EC2** - Understand virtual machines
2. Learn **VPC** - Networking fundamentals
3. Explore **Auto Scaling** - Dynamic capacity management

### Intermediate
4. Learn **ECS** - Container orchestration (simpler)
5. Understand **Load Balancers** - ALB, NLB
6. Practice **IAM** - Security and permissions

### Advanced
7. Master **EKS** - Kubernetes on AWS
8. Implement **CI/CD** - CodePipeline, CodeBuild
9. Design **Multi-tier architectures**
10. Optimize **Costs and Performance**

---

## 🔗 Additional Resources

### AWS Documentation
- [EC2 User Guide](https://docs.aws.amazon.com/ec2/)
- [VPC User Guide](https://docs.aws.amazon.com/vpc/)
- [Auto Scaling User Guide](https://docs.aws.amazon.com/autoscaling/)
- [ECS Developer Guide](https://docs.aws.amazon.com/ecs/)
- [EKS User Guide](https://docs.aws.amazon.com/eks/)

### AWS Training
- [AWS Skill Builder](https://skillbuilder.aws/) - Free training
- [AWS Certified Solutions Architect](https://aws.amazon.com/certification/certified-solutions-architect-associate/)
- [AWS Workshops](https://workshops.aws/)

### Tools
- [AWS CLI](https://aws.amazon.com/cli/) - Command-line interface
- [eksctl](https://eksctl.io/) - EKS cluster management
- [AWS CDK](https://aws.amazon.com/cdk/) - Infrastructure as Code
- [Terraform](https://www.terraform.io/) - Multi-cloud IaC

---

## 🚀 Quick Start Commands

### EC2
```bash
# Launch instance
aws ec2 run-instances --image-id ami-12345678 --instance-type t3.medium --key-name my-key

# Connect to instance
ssh -i my-key.pem ec2-user@<public-ip>
```

### VPC
```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16

# Create subnet
aws ec2 create-subnet --vpc-id vpc-12345678 --cidr-block 10.0.1.0/24
```

### Auto Scaling
```bash
# Create Auto Scaling Group
aws autoscaling create-auto-scaling-group --auto-scaling-group-name my-asg --launch-template LaunchTemplateId=lt-12345678 --min-size 2 --max-size 10
```

### ECS
```bash
# Create cluster
aws ecs create-cluster --cluster-name my-cluster

# Run task
aws ecs run-task --cluster my-cluster --task-definition my-task:1 --launch-type FARGATE
```

### EKS
```bash
# Create cluster
eksctl create cluster --name my-cluster --region us-east-1 --nodes 3

# Get kubeconfig
aws eks update-kubeconfig --name my-cluster
```

---

## 📊 Cost Calculator

Use the [AWS Pricing Calculator](https://calculator.aws) to estimate costs for your architecture.

**Example Estimates**:
- **Small Web App**: $100-300/month (ECS Fargate + RDS)
- **Medium Web App**: $500-1,500/month (EKS + RDS Multi-AZ)
- **Large Web App**: $2,000-10,000/month (Multi-region, high availability)

---

## 🤝 Contributing

Found an error or want to add content? Please open an issue or submit a pull request.

---

## 📝 License

This documentation is part of the System Designs Collection project.

---

**Last Updated**: 2024
**Maintained By**: System Designs Collection Team
