# AWS Auto Scaling - Deep Dive

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Launch Templates](#launch-templates)
4. [Auto Scaling Groups](#auto-scaling-groups)
5. [Scaling Policies](#scaling-policies)
6. [Health Checks](#health-checks)
7. [Lifecycle Hooks](#lifecycle-hooks)
8. [Integration with Load Balancers](#integration-with-load-balancers)
9. [Best Practices](#best-practices)
10. [Real-World Examples](#real-world-examples)

---

## Overview

**AWS Auto Scaling** automatically adjusts compute capacity to maintain performance and optimize costs.

### Key Benefits
- **High Availability**: Replace unhealthy instances automatically
- **Cost Optimization**: Scale down during low demand
- **Performance**: Scale up during high demand
- **Fault Tolerance**: Distribute instances across AZs
- **Predictive Scaling**: ML-based capacity planning

### Auto Scaling Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    Auto Scaling Group                        │
│                                                               │
│  Desired: 4  |  Min: 2  |  Max: 10                          │
│                                                               │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │   AZ-A           │         │   AZ-B           │          │
│  │  ┌────┐  ┌────┐  │         │  ┌────┐  ┌────┐  │          │
│  │  │EC2 │  │EC2 │  │         │  │EC2 │  │EC2 │  │          │
│  │  └────┘  └────┘  │         │  └────┘  └────┘  │          │
│  └──────────────────┘         └──────────────────┘          │
│                                                               │
│  Scaling Policies:                                           │
│  • Target Tracking: CPU > 70% → Scale Out                   │
│  • Step Scaling: Requests > 1000 → Add 2 instances         │
│  • Scheduled: 9 AM → Scale to 10, 6 PM → Scale to 2        │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Concepts

### 1. Auto Scaling Components

**Launch Template/Configuration**:
- AMI ID
- Instance type
- Key pair
- Security groups
- User data script

**Auto Scaling Group (ASG)**:
- Launch template
- Min/Max/Desired capacity
- VPC and subnets
- Health check settings
- Scaling policies

**Scaling Policies**:
- Target tracking
- Step scaling
- Simple scaling
- Scheduled scaling
- Predictive scaling

### 2. Capacity Settings

**Min Capacity**: Minimum number of instances (always running)
**Max Capacity**: Maximum number of instances (cost limit)
**Desired Capacity**: Target number of instances

**Example**:
```
Min: 2  |  Desired: 4  |  Max: 10

Scale Out: Desired increases (4 → 6 → 8 → 10)
Scale In: Desired decreases (10 → 8 → 6 → 4 → 2)
```

### 3. Cooldown Period

**Default cooldown**: 300 seconds (5 minutes)
- Prevents rapid scaling actions
- Allows metrics to stabilize
- Can be customized per scaling policy

---

## Launch Templates

**Launch Templates** define instance configuration (recommended over Launch Configurations).

### Launch Template vs Launch Configuration

| Feature | Launch Template | Launch Configuration |
|---------|-----------------|----------------------|
| Versioning | Yes | No |
| Multiple instance types | Yes | No |
| Spot instances | Yes | Limited |
| T2/T3 Unlimited | Yes | No |
| Modification | Yes | No (must recreate) |

### Create Launch Template

**AWS CLI**:
```bash
aws ec2 create-launch-template \
  --launch-template-name web-server-template \
  --version-description "v1.0" \
  --launch-template-data '{
    "ImageId": "ami-0c55b159cbfafe1f0",
    "InstanceType": "t3.medium",
    "KeyName": "my-key-pair",
    "SecurityGroupIds": ["sg-12345678"],
    "UserData": "IyEvYmluL2Jhc2gKZWNobyAiSGVsbG8gV29ybGQi",
    "IamInstanceProfile": {
      "Name": "EC2-S3-Access-Role"
    },
    "BlockDeviceMappings": [{
      "DeviceName": "/dev/xvda",
      "Ebs": {
        "VolumeSize": 20,
        "VolumeType": "gp3",
        "DeleteOnTermination": true,
        "Encrypted": true
      }
    }],
    "TagSpecifications": [{
      "ResourceType": "instance",
      "Tags": [
        {"Key": "Name", "Value": "web-server"},
        {"Key": "Environment", "Value": "production"}
      ]
    }]
  }'
```

**User Data Script** (Base64 encoded):
```bash
#!/bin/bash
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
echo "<h1>Hello from $(hostname -f)</h1>" > /var/www/html/index.html
```

### Launch Template Versions

**Create new version**:
```bash
aws ec2 create-launch-template-version \
  --launch-template-id lt-12345678 \
  --version-description "v1.1 - Updated AMI" \
  --source-version 1 \
  --launch-template-data '{"ImageId": "ami-0abcdef1234567890"}'
```

**Set default version**:
```bash
aws ec2 modify-launch-template \
  --launch-template-id lt-12345678 \
  --default-version 2
```

---

## Auto Scaling Groups

### Create Auto Scaling Group

```bash
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name web-asg \
  --launch-template LaunchTemplateId=lt-12345678,Version='$Latest' \
  --min-size 2 \
  --max-size 10 \
  --desired-capacity 4 \
  --vpc-zone-identifier "subnet-12345678,subnet-87654321" \
  --health-check-type ELB \
  --health-check-grace-period 300 \
  --target-group-arns arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/web-tg/1234567890abcdef \
  --tags Key=Name,Value=web-server,PropagateAtLaunch=true Key=Environment,Value=production,PropagateAtLaunch=true
```

### ASG Configuration Options

**VPC Zone Identifier**: Comma-separated subnet IDs
- Distribute instances across multiple AZs
- Use private subnets for internal instances
- Use public subnets for internet-facing instances

**Health Check Type**:
- **EC2**: Instance status checks (default)
- **ELB**: Load balancer health checks (recommended)

**Health Check Grace Period**: Time before health checks start (default: 300s)
- Allows instance to boot and initialize
- Set higher for slow-starting applications

**Termination Policies**:
- **Default**: Balance across AZs, then oldest launch template, then closest to billing hour
- **OldestInstance**: Terminate oldest instance first
- **NewestInstance**: Terminate newest instance first
- **OldestLaunchConfiguration**: Terminate instances with oldest launch config
- **ClosestToNextInstanceHour**: Minimize cost

---

## Scaling Policies

### 1. Target Tracking Scaling (Recommended)

**Automatically adjust capacity** to maintain target metric.

**Example: CPU Utilization**:
```bash
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name cpu-target-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ASGAverageCPUUtilization"
    },
    "TargetValue": 70.0
  }'
```

**Predefined Metrics**:
- `ASGAverageCPUUtilization`: Average CPU across instances
- `ASGAverageNetworkIn`: Average network in
- `ASGAverageNetworkOut`: Average network out
- `ALBRequestCountPerTarget`: Requests per target (ALB)

**Custom Metric Example**:
```bash
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name memory-target-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "CustomizedMetricSpecification": {
      "MetricName": "MemoryUtilization",
      "Namespace": "CWAgent",
      "Statistic": "Average"
    },
    "TargetValue": 80.0
  }'
```

**How it works**:
```
Current CPU: 85%  |  Target: 70%
→ Scale Out: Add instances to reduce CPU

Current CPU: 50%  |  Target: 70%
→ Scale In: Remove instances to increase CPU
```

### 2. Step Scaling

**Add/remove instances** based on metric thresholds.

```bash
# Create CloudWatch alarm
aws cloudwatch put-metric-alarm \
  --alarm-name cpu-high \
  --alarm-description "CPU above 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2

# Create step scaling policy
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name cpu-step-scaling \
  --policy-type StepScaling \
  --adjustment-type PercentChangeInCapacity \
  --metric-aggregation-type Average \
  --step-adjustments '[
    {
      "MetricIntervalLowerBound": 0,
      "MetricIntervalUpperBound": 10,
      "ScalingAdjustment": 10
    },
    {
      "MetricIntervalLowerBound": 10,
      "MetricIntervalUpperBound": 20,
      "ScalingAdjustment": 20
    },
    {
      "MetricIntervalLowerBound": 20,
      "ScalingAdjustment": 30
    }
  ]'
```

**Step Adjustments**:
```
CPU 80-90%: Add 10% capacity (4 → 5 instances)
CPU 90-100%: Add 20% capacity (4 → 5 instances)
CPU >100%: Add 30% capacity (4 → 6 instances)
```

### 3. Simple Scaling

**Single adjustment** when alarm triggers (legacy, not recommended).

```bash
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name scale-out \
  --scaling-adjustment 2 \
  --adjustment-type ChangeInCapacity \
  --cooldown 300
```

### 4. Scheduled Scaling

**Scale based on time** (predictable patterns).

```bash
# Scale up at 9 AM weekdays
aws autoscaling put-scheduled-update-group-action \
  --auto-scaling-group-name web-asg \
  --scheduled-action-name scale-up-morning \
  --recurrence "0 9 * * 1-5" \
  --min-size 5 \
  --max-size 20 \
  --desired-capacity 10

# Scale down at 6 PM weekdays
aws autoscaling put-scheduled-update-group-action \
  --auto-scaling-group-name web-asg \
  --scheduled-action-name scale-down-evening \
  --recurrence "0 18 * * 1-5" \
  --min-size 2 \
  --max-size 10 \
  --desired-capacity 4
```

**Cron Format**: `minute hour day month day-of-week`

### 5. Predictive Scaling

**ML-based forecasting** of future traffic.

```bash
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name predictive-scaling \
  --policy-type PredictiveScaling \
  --predictive-scaling-configuration '{
    "MetricSpecifications": [{
      "TargetValue": 70.0,
      "PredefinedMetricPairSpecification": {
        "PredefinedMetricType": "ASGCPUUtilization"
      }
    }],
    "Mode": "ForecastAndScale",
    "SchedulingBufferTime": 600
  }'
```

**Modes**:
- `ForecastOnly`: Generate forecast, don't scale
- `ForecastAndScale`: Generate forecast and scale proactively

---

## Health Checks

### EC2 Health Checks

**Status Checks**:
- **System Status**: AWS infrastructure (host, network)
- **Instance Status**: OS, software

**Unhealthy Instance**: Terminated and replaced automatically.

### ELB Health Checks

**More comprehensive** than EC2 checks:
- HTTP/HTTPS health check endpoint
- TCP connection check
- Custom health check logic

**Configuration**:
```bash
aws elbv2 modify-target-group \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/web-tg/1234567890abcdef \
  --health-check-protocol HTTP \
  --health-check-path /health \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3
```

**Health Check Logic**:
```
Healthy: 2 consecutive successful checks
Unhealthy: 3 consecutive failed checks
→ ASG terminates and replaces unhealthy instance
```

---

## Lifecycle Hooks

**Pause instance launch/termination** for custom actions.

### Use Cases
- Install software before instance goes in service
- Backup data before instance terminates
- Register/deregister with external systems
- Run custom health checks

### Lifecycle Hook Flow

```
Launch:
Pending → Pending:Wait (Hook) → InService

Terminate:
Terminating → Terminating:Wait (Hook) → Terminated
```

### Create Lifecycle Hook

```bash
aws autoscaling put-lifecycle-hook \
  --lifecycle-hook-name instance-launching \
  --auto-scaling-group-name web-asg \
  --lifecycle-transition autoscaling:EC2_INSTANCE_LAUNCHING \
  --default-result CONTINUE \
  --heartbeat-timeout 300 \
  --notification-target-arn arn:aws:sns:us-east-1:123456789012:asg-notifications
```

### Complete Lifecycle Action

```bash
# From Lambda or custom script
aws autoscaling complete-lifecycle-action \
  --lifecycle-hook-name instance-launching \
  --auto-scaling-group-name web-asg \
  --lifecycle-action-result CONTINUE \
  --instance-id i-1234567890abcdef0
```

---

## Integration with Load Balancers

### Application Load Balancer (ALB)

**Attach ASG to Target Group**:
```bash
aws autoscaling attach-load-balancer-target-groups \
  --auto-scaling-group-name web-asg \
  --target-group-arns arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/web-tg/1234567890abcdef
```

**Benefits**:
- Automatic registration/deregistration
- Health checks from ALB
- Connection draining during scale-in

### Network Load Balancer (NLB)

**Similar to ALB**, but Layer 4 (TCP/UDP).

### Classic Load Balancer (CLB)

**Legacy**, use ALB/NLB instead.

---

## Best Practices

### 1. Capacity Planning
- Set Min = desired steady-state capacity
- Set Max = 2-3x Min for burst capacity
- Use Desired for normal operations

### 2. Scaling Policies
- **Prefer Target Tracking**: Simplest and most effective
- **Use Step Scaling**: For complex scenarios
- **Combine policies**: CPU + Memory + Request Count
- **Avoid Simple Scaling**: Use Step Scaling instead

### 3. Health Checks
- Use ELB health checks (more comprehensive)
- Set grace period > instance boot time
- Implement custom health check endpoint

### 4. Cost Optimization
- Use Spot Instances for fault-tolerant workloads
- Use Scheduled Scaling for predictable patterns
- Set appropriate cooldown periods
- Monitor and adjust Min/Max capacity

### 5. High Availability
- Deploy across multiple AZs (minimum 2)
- Use multiple instance types (mixed instances policy)
- Set Min ≥ 2 for redundancy

---

## Real-World Examples

### Example 1: Web Application

**Requirements**:
- 2-10 instances
- Scale on CPU (70%) and Requests (1000/target)
- Deploy across 2 AZs

**Configuration**:
```bash
# Create ASG
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name web-asg \
  --launch-template LaunchTemplateId=lt-12345678 \
  --min-size 2 \
  --max-size 10 \
  --desired-capacity 4 \
  --vpc-zone-identifier "subnet-1a,subnet-1b" \
  --health-check-type ELB \
  --health-check-grace-period 300 \
  --target-group-arns arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/web-tg/abc

# CPU target tracking
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name cpu-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ASGAverageCPUUtilization"
    },
    "TargetValue": 70.0
  }'

# Request count target tracking
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name web-asg \
  --policy-name request-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ALBRequestCountPerTarget",
      "ResourceLabel": "app/web-alb/abc123/targetgroup/web-tg/def456"
    },
    "TargetValue": 1000.0
  }'
```

**Cost** (t3.medium @ $0.0416/hr):
- Min (2 instances): $60/month
- Avg (4 instances): $120/month
- Max (10 instances): $300/month

### Example 2: Batch Processing (Spot Instances)

**Requirements**:
- 0-50 instances
- Use Spot Instances (70% savings)
- Scale on SQS queue depth

**Configuration**:
```bash
# Create ASG with Spot
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name batch-asg \
  --mixed-instances-policy '{
    "LaunchTemplate": {
      "LaunchTemplateSpecification": {
        "LaunchTemplateId": "lt-12345678"
      },
      "Overrides": [
        {"InstanceType": "c6i.xlarge"},
        {"InstanceType": "c6i.2xlarge"},
        {"InstanceType": "c5.xlarge"}
      ]
    },
    "InstancesDistribution": {
      "OnDemandBaseCapacity": 0,
      "OnDemandPercentageAboveBaseCapacity": 0,
      "SpotAllocationStrategy": "capacity-optimized"
    }
  }' \
  --min-size 0 \
  --max-size 50 \
  --desired-capacity 0 \
  --vpc-zone-identifier "subnet-1a,subnet-1b,subnet-1c"

# Scale on SQS queue depth
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name batch-asg \
  --policy-name sqs-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "CustomizedMetricSpecification": {
      "MetricName": "ApproximateNumberOfMessagesVisible",
      "Namespace": "AWS/SQS",
      "Statistic": "Average",
      "Dimensions": [{
        "Name": "QueueName",
        "Value": "batch-queue"
      }]
    },
    "TargetValue": 100.0
  }'
```

**Cost Savings**:
- On-Demand: $0.34/hr × 50 = $17/hr = $12,240/month
- Spot: $0.10/hr × 50 = $5/hr = $3,600/month
- **Savings: 70% ($8,640/month)**

---

## Summary

**AWS Auto Scaling** provides automatic capacity management:
- **High Availability**: Replace unhealthy instances
- **Cost Optimization**: Scale down during low demand
- **Performance**: Scale up during high demand
- **Flexibility**: Multiple scaling policies

**Key Takeaways**:
1. Use Launch Templates (not Launch Configurations)
2. Prefer Target Tracking scaling policies
3. Use ELB health checks for comprehensive monitoring
4. Deploy across multiple AZs for high availability
5. Use Spot Instances for cost savings (fault-tolerant workloads)
6. Set appropriate Min/Max/Desired capacity
7. Monitor CloudWatch metrics and adjust policies

**Next**: Learn about ECS and EKS for container orchestration.
