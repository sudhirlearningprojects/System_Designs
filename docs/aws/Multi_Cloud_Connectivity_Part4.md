# Multi-Cloud Connectivity Guide - Part 4: Real-World Architectures & Case Studies

## Table of Contents
1. [Architecture Patterns](#architecture-patterns)
2. [Case Study 1: E-Commerce Platform](#case-study-1-e-commerce-platform)
3. [Case Study 2: Financial Services](#case-study-2-financial-services)
4. [Case Study 3: Media Streaming](#case-study-3-media-streaming)
5. [Decision Framework](#decision-framework)
6. [Summary](#summary)

---

## Architecture Patterns

### Pattern 1: Disaster Recovery (Active-Passive)

**Scenario**: Primary workload in AWS, failover to GCP

```
┌─────────────────────────────────────────────────────────────┐
│                         AWS (Primary)                        │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │     EKS      │  │     RDS      │  │      S3      │      │
│  │  (Active)    │  │  (Primary)   │  │   (Active)   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼──────────────────┼──────────────────┼──────────────┘
          │                  │                  │
          │ Replication      │ Replication      │ Replication
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼──────────────┐
│                         GCP (Standby)                         │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │     GKE      │  │  Cloud SQL   │  │     GCS      │      │
│  │  (Standby)   │  │  (Replica)   │  │  (Replica)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

**Components**:
- **Connectivity**: VPN (low cost) or Direct Connect (low latency)
- **DNS**: Route 53 with health checks for automatic failover
- **Replication**: Database replication, S3 cross-region replication
- **RTO**: 15-60 minutes
- **RPO**: 5-15 minutes

**Cost** (Monthly):
- VPN: $280
- Data replication (100 GB/day): $270
- Standby infrastructure: $500
- **Total: ~$1,050/month**

**Implementation**:
```bash
# Route 53 health check and failover
aws route53 create-health-check \
  --health-check-config IPAddress=54.123.45.67,Port=443,Type=HTTPS,ResourcePath=/health

aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "app.example.com",
        "Type": "A",
        "SetIdentifier": "AWS-Primary",
        "Failover": "PRIMARY",
        "AliasTarget": {
          "HostedZoneId": "Z1234567890ABC",
          "DNSName": "aws-alb.example.com",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'
```

---

### Pattern 2: Best-of-Breed (Multi-Cloud Services)

**Scenario**: Use best services from each cloud

```
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                       │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   AWS EKS    │  │  GCP BigQuery│  │  Azure ML    │      │
│  │ (Compute)    │  │  (Analytics) │  │  (AI/ML)     │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼──────────────────┼──────────────────┼──────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                    ┌────────▼────────┐
                    │   API Gateway   │
                    │   (Kong/Apigee) │
                    └─────────────────┘
```

**Components**:
- **Connectivity**: SD-WAN for intelligent routing
- **Integration**: API Gateway + Service Mesh
- **Data Sync**: Event-driven (Kafka, Pub/Sub, Event Grid)

**Cost** (Monthly):
- SD-WAN: $1,500 (3 sites)
- API Gateway: $500
- Data transfer: $1,000
- **Total: ~$3,000/month**

**Use Cases**:
- AWS: Compute (EKS), Storage (S3), CDN (CloudFront)
- GCP: Analytics (BigQuery), ML (Vertex AI), Data (Cloud Spanner)
- Azure: AI (Azure OpenAI), Enterprise (Active Directory), IoT (IoT Hub)

---

### Pattern 3: Geographic Distribution

**Scenario**: Serve users from nearest cloud region

```
┌─────────────────────────────────────────────────────────────┐
│                    Global Load Balancer                      │
│              (Cloudflare / AWS Global Accelerator)           │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │ us-east │ │ europe  │ │  asia   │
    │         │ │  -west  │ │ -south  │
    └─────────┘ └─────────┘ └─────────┘
```

**Components**:
- **Connectivity**: Direct Connect between regions
- **DNS**: GeoDNS routing (Route 53, Cloud DNS)
- **Data**: Multi-region database (Spanner, DynamoDB Global Tables)

**Latency**:
- US users → AWS us-east-1: 20ms
- EU users → GCP europe-west1: 15ms
- APAC users → Azure asia-southeast1: 25ms

**Cost** (Monthly):
- 3x Direct Connect: $1,500
- Multi-region database: $2,000
- Data transfer: $1,500
- **Total: ~$5,000/month**

---

## Case Study 1: E-Commerce Platform

### Requirements

**Business**:
- 10M users globally
- 99.99% uptime SLA
- <100ms API response time
- PCI DSS compliance

**Technical**:
- Microservices architecture
- Real-time inventory
- Payment processing
- Order fulfillment

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Cloudflare (CDN + WAF)                    │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │         │ │         │ │         │
    │ Web/API │ │ Payment │ │Inventory│
    │  (EKS)  │ │(Cloud   │ │ (AKS)   │
    │         │ │ Run)    │ │         │
    │ DynamoDB│ │ Spanner │ │ Cosmos  │
    └─────────┘ └─────────┘ └─────────┘
```

### Implementation

**1. Frontend (AWS)**:
```yaml
# EKS deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-frontend
spec:
  replicas: 10
  template:
    spec:
      containers:
      - name: web
        image: ecommerce/web:latest
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
        env:
        - name: PAYMENT_API
          value: https://payment.gcp.example.com
        - name: INVENTORY_API
          value: https://inventory.azure.example.com
```

**2. Payment Service (GCP)**:
```python
# Cloud Run service
from flask import Flask, request
import stripe

app = Flask(__name__)
stripe.api_key = os.environ['STRIPE_KEY']

@app.route('/charge', methods=['POST'])
def charge():
    amount = request.json['amount']
    token = request.json['token']
    
    try:
        charge = stripe.Charge.create(
            amount=amount,
            currency='usd',
            source=token
        )
        return {'status': 'success', 'charge_id': charge.id}
    except stripe.error.CardError as e:
        return {'status': 'error', 'message': str(e)}, 400
```

**3. Inventory Service (Azure)**:
```csharp
// AKS service
[ApiController]
[Route("api/inventory")]
public class InventoryController : ControllerBase
{
    private readonly CosmosClient _cosmosClient;
    
    [HttpGet("{productId}")]
    public async Task<IActionResult> GetInventory(string productId)
    {
        var container = _cosmosClient.GetContainer("ecommerce", "inventory");
        var item = await container.ReadItemAsync<Inventory>(productId, new PartitionKey(productId));
        return Ok(item.Resource);
    }
    
    [HttpPost("reserve")]
    public async Task<IActionResult> ReserveInventory([FromBody] ReservationRequest request)
    {
        // Atomic inventory reservation
        var container = _cosmosClient.GetContainer("ecommerce", "inventory");
        var response = await container.PatchItemAsync<Inventory>(
            request.ProductId,
            new PartitionKey(request.ProductId),
            new[] { PatchOperation.Increment("/quantity", -request.Quantity) }
        );
        return Ok(response.Resource);
    }
}
```

**4. Service Mesh (Istio)**:
```yaml
# Cross-cloud service communication
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: payment-service-gcp
spec:
  hosts:
  - payment.gcp.example.com
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  location: MESH_EXTERNAL
  resolution: DNS

---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: payment-routing
spec:
  hosts:
  - payment.gcp.example.com
  http:
  - timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
    route:
    - destination:
        host: payment.gcp.example.com
```

### Performance

**Metrics**:
- API Latency: p50=45ms, p95=120ms, p99=250ms
- Throughput: 50K requests/sec
- Availability: 99.99% (4.38 minutes downtime/month)
- Error Rate: 0.01%

**Monitoring**:
```yaml
# Prometheus alerts
groups:
- name: ecommerce
  rules:
  - alert: HighLatency
    expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
    for: 5m
    annotations:
      summary: "High API latency detected"
  
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
    for: 5m
    annotations:
      summary: "High error rate detected"
```

### Cost

**Monthly**:
- AWS (Web/API): $5,000
- GCP (Payment): $2,000
- Azure (Inventory): $3,000
- Cloudflare: $200
- Direct Connect: $1,500
- **Total: $11,700/month**

**Per User**: $1.17/month (10M users)

---

## Case Study 2: Financial Services

### Requirements

**Business**:
- Real-time fraud detection
- Regulatory compliance (SOC 2, PCI DSS)
- Data residency (US, EU, APAC)
- 99.999% uptime (5.26 minutes/year)

**Technical**:
- Low latency (<10ms)
- Strong consistency
- Audit logging
- Encryption everywhere

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Users (Global)                            │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │ us-east │ │ europe  │ │  asia   │
    │         │ │  -west  │ │ -south  │
    │ Trading │ │ Trading │ │ Trading │
    │ Engine  │ │ Engine  │ │ Engine  │
    │         │ │         │ │         │
    │ Aurora  │ │ Spanner │ │ Cosmos  │
    │ Global  │ │ (Multi- │ │ (Multi- │
    │Database │ │ Region) │ │ Master) │
    └─────────┘ └─────────┘ └─────────┘
         │           │           │
         └───────────┼───────────┘
                     │
            ┌────────▼────────┐
            │  Fraud Detection│
            │  (AWS SageMaker)│
            └─────────────────┘
```

### Implementation

**1. Trading Engine (AWS)**:
```java
@Service
public class TradingEngine {
    
    @Autowired
    private AuroraGlobalDatabase database;
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TradeResult executeTrade(TradeRequest request) {
        // Check balance
        Account account = database.getAccount(request.getAccountId());
        if (account.getBalance() < request.getAmount()) {
            throw new InsufficientFundsException();
        }
        
        // Execute trade (atomic)
        Trade trade = new Trade();
        trade.setAccountId(request.getAccountId());
        trade.setAmount(request.getAmount());
        trade.setTimestamp(Instant.now());
        
        database.save(trade);
        
        // Update balance
        account.setBalance(account.getBalance() - request.getAmount());
        database.save(account);
        
        // Publish event for fraud detection
        eventBus.publish(new TradeExecutedEvent(trade));
        
        return new TradeResult(trade.getId(), "SUCCESS");
    }
}
```

**2. Fraud Detection (AWS SageMaker)**:
```python
import boto3
import json

sagemaker = boto3.client('sagemaker-runtime')

def detect_fraud(trade):
    # Real-time inference
    response = sagemaker.invoke_endpoint(
        EndpointName='fraud-detection-endpoint',
        ContentType='application/json',
        Body=json.dumps({
            'account_id': trade['account_id'],
            'amount': trade['amount'],
            'timestamp': trade['timestamp'],
            'location': trade['location']
        })
    )
    
    result = json.loads(response['Body'].read())
    fraud_score = result['fraud_score']
    
    if fraud_score > 0.8:
        # Block transaction
        block_transaction(trade['id'])
        send_alert(trade['account_id'])
    
    return fraud_score
```

**3. Multi-Region Database (GCP Spanner)**:
```sql
-- Create multi-region database
CREATE DATABASE trading_db
  OPTIONS (
    default_leader = 'us-central1',
    version_retention_period = '7d'
  );

-- Create table with interleaving
CREATE TABLE Accounts (
  account_id STRING(36) NOT NULL,
  balance NUMERIC NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
) PRIMARY KEY (account_id);

CREATE TABLE Trades (
  account_id STRING(36) NOT NULL,
  trade_id STRING(36) NOT NULL,
  amount NUMERIC NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  status STRING(20) NOT NULL
) PRIMARY KEY (account_id, trade_id),
  INTERLEAVE IN PARENT Accounts ON DELETE CASCADE;

-- Strong consistency read
SELECT * FROM Accounts WHERE account_id = @account_id;

-- Atomic transaction
BEGIN TRANSACTION;
  UPDATE Accounts SET balance = balance - @amount WHERE account_id = @account_id;
  INSERT INTO Trades (account_id, trade_id, amount, timestamp, status)
    VALUES (@account_id, @trade_id, @amount, CURRENT_TIMESTAMP(), 'COMPLETED');
COMMIT TRANSACTION;
```

**4. Connectivity (Direct Connect)**:
```bash
# AWS Direct Connect with MACsec encryption
aws directconnect create-connection \
  --location EqDC2 \
  --bandwidth 10Gbps \
  --connection-name trading-dx \
  --encryption-mode must_encrypt

# Create LAG for redundancy
aws directconnect create-lag \
  --number-of-connections 2 \
  --location EqDC2 \
  --connections-bandwidth 10Gbps \
  --lag-name trading-lag
```

### Performance

**Metrics**:
- Trade Execution: <5ms (p99)
- Fraud Detection: <10ms (real-time)
- Database Replication Lag: <100ms
- Availability: 99.999% (5.26 minutes/year)

**Compliance**:
- Encryption: TLS 1.3, AES-256
- Audit Logging: CloudTrail, Cloud Audit Logs, Azure Monitor
- Data Residency: US (AWS), EU (GCP), APAC (Azure)
- Certifications: SOC 2, PCI DSS, ISO 27001

### Cost

**Monthly**:
- AWS (Trading + ML): $15,000
- GCP (Spanner Multi-Region): $10,000
- Azure (Trading): $8,000
- Direct Connect (10 Gbps): $2,000
- Data Transfer: $3,000
- **Total: $38,000/month**

---

## Case Study 3: Media Streaming

### Requirements

**Business**:
- 100M users globally
- 4K video streaming
- Live streaming support
- Content delivery <50ms latency

**Technical**:
- CDN for global distribution
- Transcoding pipeline
- Adaptive bitrate streaming
- DRM protection

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Cloudflare Stream (CDN)                   │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │         │ │         │ │         │
    │ Origin  │ │Transcode│ │  Live   │
    │ Storage │ │Pipeline │ │Streaming│
    │  (S3)   │ │(Compute)│ │ (Media  │
    │         │ │         │ │Services)│
    │CloudFront│ │         │ │         │
    └─────────┘ └─────────┘ └─────────┘
```

### Implementation

**1. Video Upload (AWS S3)**:
```python
import boto3

s3 = boto3.client('s3')

def upload_video(file_path, video_id):
    # Upload to S3
    s3.upload_file(
        file_path,
        'media-bucket',
        f'videos/{video_id}/original.mp4',
        ExtraArgs={
            'StorageClass': 'INTELLIGENT_TIERING',
            'ServerSideEncryption': 'AES256'
        }
    )
    
    # Trigger transcoding
    sns = boto3.client('sns')
    sns.publish(
        TopicArn='arn:aws:sns:us-east-1:123456789012:video-uploaded',
        Message=json.dumps({
            'video_id': video_id,
            'bucket': 'media-bucket',
            'key': f'videos/{video_id}/original.mp4'
        })
    )
```

**2. Transcoding Pipeline (GCP)**:
```python
from google.cloud import storage, pubsub_v1
import ffmpeg

def transcode_video(video_id, input_path):
    # Download from AWS S3
    s3 = boto3.client('s3')
    s3.download_file('media-bucket', input_path, '/tmp/input.mp4')
    
    # Transcode to multiple resolutions
    resolutions = [
        ('1080p', 1920, 1080, '5000k'),
        ('720p', 1280, 720, '3000k'),
        ('480p', 854, 480, '1500k'),
        ('360p', 640, 360, '800k')
    ]
    
    for name, width, height, bitrate in resolutions:
        output_path = f'/tmp/{name}.mp4'
        
        ffmpeg.input('/tmp/input.mp4') \
            .output(output_path, vcodec='libx264', video_bitrate=bitrate, s=f'{width}x{height}') \
            .run()
        
        # Upload to GCS
        storage_client = storage.Client()
        bucket = storage_client.bucket('media-transcoded')
        blob = bucket.blob(f'{video_id}/{name}.mp4')
        blob.upload_from_filename(output_path)
    
    # Generate HLS manifest
    generate_hls_manifest(video_id, resolutions)
```

**3. Live Streaming (Azure Media Services)**:
```csharp
using Microsoft.Azure.Management.Media;
using Microsoft.Azure.Management.Media.Models;

public class LiveStreamingService
{
    private readonly IAzureMediaServicesClient _client;
    
    public async Task<LiveEvent> CreateLiveEvent(string eventName)
    {
        // Create live event
        var liveEvent = new LiveEvent(
            location: "eastus",
            description: "Live streaming event",
            input: new LiveEventInput(
                streamingProtocol: LiveEventInputProtocol.RTMP,
                accessControl: new LiveEventInputAccessControl(
                    ip: new IPAccessControl(
                        allow: new List<IPRange> { new IPRange(name: "AllowAll", address: "0.0.0.0", subnetPrefixLength: 0) }
                    )
                )
            ),
            encoding: new LiveEventEncoding(
                encodingType: LiveEventEncodingType.Standard,
                presetName: "Default720p"
            )
        );
        
        liveEvent = await _client.LiveEvents.CreateAsync(
            resourceGroupName: "media-rg",
            accountName: "mediaaccount",
            liveEventName: eventName,
            parameters: liveEvent,
            autoStart: true
        );
        
        return liveEvent;
    }
}
```

**4. CDN Distribution (Cloudflare)**:
```javascript
// Cloudflare Worker for adaptive bitrate
addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request))
})

async function handleRequest(request) {
  const url = new URL(request.url)
  const videoId = url.pathname.split('/')[2]
  
  // Detect client bandwidth
  const bandwidth = request.headers.get('downlink') || 10
  
  // Select appropriate resolution
  let resolution = '1080p'
  if (bandwidth < 2) resolution = '360p'
  else if (bandwidth < 5) resolution = '480p'
  else if (bandwidth < 10) resolution = '720p'
  
  // Fetch from origin
  const originUrl = `https://origin.example.com/${videoId}/${resolution}.mp4`
  const response = await fetch(originUrl, {
    cf: {
      cacheEverything: true,
      cacheTtl: 86400
    }
  })
  
  return response
}
```

### Performance

**Metrics**:
- CDN Hit Ratio: 95%
- Video Start Time: <2 seconds
- Buffering Ratio: <0.5%
- Global Latency: <50ms (p95)

**Scale**:
- 100M users
- 10PB storage
- 500 Gbps peak bandwidth
- 1M concurrent streams

### Cost

**Monthly**:
- AWS S3 (10PB): $230,000
- GCP Compute (Transcoding): $50,000
- Azure Media Services: $30,000
- Cloudflare (CDN): $20,000
- Data Transfer: $100,000
- **Total: $430,000/month**

**Per User**: $4.30/month (100M users)

---

## Decision Framework

### When to Use VPN

**Criteria**:
- Data transfer <2 TB/month
- Non-critical workloads
- Budget constraints
- Quick setup required

**Cost**: $280-350/month

### When to Use Direct Connect

**Criteria**:
- Data transfer >2 TB/month
- Mission-critical workloads
- Low latency required (<10ms)
- Predictable performance needed

**Cost**: $480-530/month (1 Gbps)

### When to Use SD-WAN

**Criteria**:
- Multiple cloud providers
- Application-aware routing needed
- Dynamic path selection required
- Centralized management desired

**Cost**: $300-2,000/month per site

### When to Use Service Mesh

**Criteria**:
- Microservices architecture
- mTLS encryption required
- Observability needed
- Traffic management (canary, circuit breaker)

**Cost**: Infrastructure costs only

### When to Use API Gateway

**Criteria**:
- API-driven architecture
- Centralized authentication/authorization
- Rate limiting needed
- Analytics required

**Cost**: $0.50-$3.00 per million calls

---

## Summary

**Multi-cloud connectivity** enables flexible, resilient architectures:

**Key Patterns**:
1. **Disaster Recovery**: Active-Passive, RTO <1 hour
2. **Best-of-Breed**: Use best services from each cloud
3. **Geographic Distribution**: Serve users from nearest region

**Real-World Examples**:
1. **E-Commerce**: AWS (Web) + GCP (Payment) + Azure (Inventory)
2. **Financial Services**: Multi-region trading with fraud detection
3. **Media Streaming**: AWS (Storage) + GCP (Transcoding) + Azure (Live)

**Decision Factors**:
- **Data Volume**: VPN (<2 TB), Direct Connect (>2 TB)
- **Latency**: VPN (50-100ms), Direct Connect (5-10ms)
- **Cost**: VPN ($280), Direct Connect ($480), SD-WAN ($300-2,000)
- **Complexity**: VPN (Low), Direct Connect (Medium), SD-WAN (High)

**Best Practices**:
1. Start with VPN, upgrade to Direct Connect as needed
2. Use SD-WAN for intelligent routing
3. Implement Service Mesh for microservices
4. Monitor everything (latency, bandwidth, errors)
5. Design for redundancy and failover
6. Encrypt everything (in transit and at rest)
7. Optimize costs based on workload patterns

**Total Cost Examples**:
- **Small** (VPN): $280-500/month
- **Medium** (Direct Connect): $500-2,000/month
- **Large** (SD-WAN + Direct Connect): $2,000-10,000/month
- **Enterprise** (Multi-region): $10,000-50,000/month

Multi-cloud is complex but enables flexibility, resilience, and best-of-breed architectures!
