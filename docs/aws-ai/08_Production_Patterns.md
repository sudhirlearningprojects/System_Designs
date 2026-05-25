# 8. Production Patterns for AWS AI

## Enterprise Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                 ENTERPRISE AI ARCHITECTURE (AWS)                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │ API Gateway  │───►│ Lambda /     │───►│ Bedrock          │  │
│  │ (throttle,   │    │ ECS / EKS    │    │ (Claude, Llama)  │  │
│  │  auth, WAF)  │    │              │    │                  │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│         │                    │                     │             │
│         │              ┌─────▼──────┐        ┌────▼─────┐      │
│         │              │ Bedrock    │        │OpenSearch│      │
│         │              │ Guardrails │        │Serverless│      │
│         │              └────────────┘        └──────────┘      │
│         │                                                       │
│  ┌──────▼──────────────────────────────────────────────────┐   │
│  │ VPC: Private subnets, VPC Endpoints for all services     │   │
│  │ No internet access for AI services                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ MONITORING: CloudWatch + X-Ray + CloudTrail              │   │
│  │ Custom metrics: tokens, latency, cost, quality            │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## VPC Endpoints (Private Access)

```bash
# Create VPC endpoint for Bedrock (no internet needed)
aws ec2 create-vpc-endpoint \
  --vpc-id vpc-123456 \
  --service-name com.amazonaws.us-east-1.bedrock-runtime \
  --vpc-endpoint-type Interface \
  --subnet-ids subnet-abc123 subnet-def456 \
  --security-group-ids sg-789012 \
  --private-dns-enabled

# Also create endpoints for:
# com.amazonaws.us-east-1.bedrock
# com.amazonaws.us-east-1.bedrock-agent-runtime
# com.amazonaws.us-east-1.s3 (for Knowledge Base docs)
```

---

## Cost Tracking & Optimization

```python
import boto3
from datetime import datetime, timedelta

# Track Bedrock costs via CloudWatch
cloudwatch = boto3.client("cloudwatch")

# Get token usage metrics
response = cloudwatch.get_metric_statistics(
    Namespace="AWS/Bedrock",
    MetricName="InputTokenCount",
    Dimensions=[{"Name": "ModelId", "Value": "anthropic.claude-sonnet-4-20250514-v1:0"}],
    StartTime=datetime.utcnow() - timedelta(hours=24),
    EndTime=datetime.utcnow(),
    Period=3600,
    Statistics=["Sum"],
)

total_input_tokens = sum(dp["Sum"] for dp in response["Datapoints"])
print(f"Input tokens (24h): {total_input_tokens:,.0f}")
print(f"Estimated cost: ${total_input_tokens * 3.0 / 1_000_000:.2f}")

# Cost optimization: Model routing
def select_model(query: str, complexity: str) -> str:
    """Route to cheapest model that can handle the task."""
    if complexity == "simple":
        return "anthropic.claude-3-5-haiku-20241022-v1:0"  # $0.80/$4
    elif complexity == "standard":
        return "anthropic.claude-sonnet-4-20250514-v1:0"    # $3/$15
    else:
        return "anthropic.claude-opus-4-20250514-v1:0"      # $15/$75
```

### Cost Alerts

```bash
# CloudWatch alarm for daily spend
aws cloudwatch put-metric-alarm \
  --alarm-name "bedrock-daily-cost-high" \
  --metric-name "InvocationCount" \
  --namespace "AWS/Bedrock" \
  --statistic Sum \
  --period 86400 \
  --threshold 10000 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --alarm-actions "arn:aws:sns:us-east-1:123456789:ai-alerts"
```

---

## Monitoring with CloudWatch

```python
# Custom metrics for AI application
cloudwatch = boto3.client("cloudwatch")

def log_ai_metrics(model, tokens_in, tokens_out, latency_ms, quality_score):
    cloudwatch.put_metric_data(
        Namespace="MyApp/AI",
        MetricData=[
            {"MetricName": "TokensUsed", "Value": tokens_in + tokens_out,
             "Dimensions": [{"Name": "Model", "Value": model}]},
            {"MetricName": "Latency", "Value": latency_ms, "Unit": "Milliseconds",
             "Dimensions": [{"Name": "Model", "Value": model}]},
            {"MetricName": "QualityScore", "Value": quality_score,
             "Dimensions": [{"Name": "Model", "Value": model}]},
            {"MetricName": "CostUSD", "Value": calculate_cost(model, tokens_in, tokens_out),
             "Dimensions": [{"Name": "Model", "Value": model}]},
        ],
    )
```

### X-Ray Tracing

```python
from aws_xray_sdk.core import xray_recorder, patch_all

patch_all()  # Auto-instrument boto3, requests, etc.

@xray_recorder.capture("agent_pipeline")
def handle_query(query: str) -> str:
    # Each subsegment appears in X-Ray trace
    with xray_recorder.in_subsegment("embed_query"):
        embedding = embed(query)
    
    with xray_recorder.in_subsegment("search_knowledge_base"):
        docs = search(embedding)
    
    with xray_recorder.in_subsegment("generate_response"):
        response = generate(query, docs)
    
    # Add metadata to trace
    xray_recorder.current_subsegment().put_metadata("tokens", response.usage)
    xray_recorder.current_subsegment().put_annotation("model", "claude-sonnet")
    
    return response.text
```

---

## Multi-Region Failover

```python
import boto3
from botocore.exceptions import ClientError

class BedrockWithFailover:
    """Bedrock client with automatic region failover."""
    
    def __init__(self, regions=["us-east-1", "us-west-2", "eu-west-1"]):
        self.clients = {
            region: boto3.client("bedrock-runtime", region_name=region)
            for region in regions
        }
        self.regions = regions
    
    def invoke(self, model_id: str, messages: list, **kwargs) -> dict:
        for region in self.regions:
            try:
                response = self.clients[region].converse(
                    modelId=model_id, messages=messages, **kwargs
                )
                return response
            except ClientError as e:
                error_code = e.response["Error"]["Code"]
                if error_code in ["ThrottlingException", "ServiceUnavailableException"]:
                    continue  # Try next region
                raise
        
        raise Exception("All regions exhausted")
```

---

## Infrastructure as Code (CDK)

```python
# AWS CDK for complete AI stack
from aws_cdk import (
    Stack, aws_bedrock as bedrock, aws_opensearchserverless as aoss,
    aws_lambda as lambda_, aws_iam as iam, aws_ec2 as ec2,
)
from constructs import Construct

class AIStack(Stack):
    def __init__(self, scope: Construct, id: str, **kwargs):
        super().__init__(scope, id, **kwargs)
        
        # VPC
        vpc = ec2.Vpc(self, "AIVPC", max_azs=2, nat_gateways=1)
        
        # VPC Endpoint for Bedrock
        vpc.add_interface_endpoint("BedrockEndpoint",
            service=ec2.InterfaceVpcEndpointAwsService("bedrock-runtime"),
        )
        
        # OpenSearch Serverless (vector store)
        collection = aoss.CfnCollection(self, "VectorStore",
            name="ai-vectors", type="VECTORSEARCH",
        )
        
        # Lambda for agent actions
        agent_fn = lambda_.Function(self, "AgentActions",
            runtime=lambda_.Runtime.PYTHON_3_12,
            handler="handler.lambda_handler",
            code=lambda_.Code.from_asset("./lambda"),
            vpc=vpc,
            timeout=Duration.seconds(30),
        )
        
        # Grant Bedrock access
        agent_fn.add_to_role_policy(iam.PolicyStatement(
            actions=["bedrock:InvokeModel"],
            resources=["arn:aws:bedrock:*::foundation-model/*"],
        ))
```

---

## Production Checklist

### Security
- [ ] VPC endpoints for Bedrock, S3, OpenSearch (no public internet)
- [ ] IAM roles with least-privilege (specific model ARNs)
- [ ] Bedrock Guardrails enabled (content filtering + topic blocking)
- [ ] CloudTrail logging all Bedrock API calls
- [ ] KMS encryption for data at rest
- [ ] No API keys in code (use IAM roles)

### Reliability
- [ ] Multi-region failover (us-east-1 → us-west-2)
- [ ] Provisioned throughput for critical workloads
- [ ] Retry with exponential backoff (handle ThrottlingException)
- [ ] Circuit breaker in application code
- [ ] Dead letter queue for failed async invocations

### Cost
- [ ] Model tiering (Haiku for simple, Sonnet for complex)
- [ ] Response caching (ElastiCache/DynamoDB)
- [ ] CloudWatch alarms on token usage
- [ ] Batch processing for non-real-time (Bedrock Batch)
- [ ] Right-size Knowledge Base (semantic chunking, not too many docs)

### Monitoring
- [ ] CloudWatch custom metrics (tokens, latency, cost, quality)
- [ ] X-Ray tracing for end-to-end visibility
- [ ] CloudWatch Logs for all invocations
- [ ] Alarms: latency > 5s, error rate > 5%, cost > budget
- [ ] Dashboard: real-time tokens, cost, quality scores
