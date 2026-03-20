# Multi-Cloud Connectivity Guide - Part 3: SD-WAN, Service Mesh & Application Integration

## Table of Contents
1. [SD-WAN Solutions](#sd-wan-solutions)
2. [Service Mesh](#service-mesh)
3. [API Gateway Integration](#api-gateway-integration)
4. [Database Replication](#database-replication)
5. [Best Practices](#best-practices)

---

## SD-WAN Solutions

**SD-WAN** (Software-Defined Wide Area Network) provides intelligent routing across multiple clouds.

### SD-WAN Benefits

- **Intelligent Routing**: Route traffic based on application, latency, cost
- **Multi-Path**: Use multiple connections (VPN, Direct Connect, Internet)
- **Application-Aware**: Prioritize critical applications
- **Centralized Management**: Single pane of glass
- **Cost Optimization**: Use cheaper paths when possible

### SD-WAN Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    SD-WAN Controller                         │
│              (Centralized Management)                        │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │ SD-WAN  │ │ SD-WAN  │ │ SD-WAN  │
    │  Edge   │ │  Edge   │ │  Edge   │
    └────┬────┘ └────┬────┘ └────┬────┘
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   VPC   │ │   VPC   │ │  VNet   │
    │10.0.0.0 │ │10.1.0.0 │ │10.2.0.0 │
    └─────────┘ └─────────┘ └─────────┘
```

### Major SD-WAN Vendors

#### 1. Cisco SD-WAN (Viptela)

**Features**:
- Application-aware routing
- Multi-cloud support (AWS, GCP, Azure)
- Integrated security (firewall, IPS)
- Zero-touch provisioning

**Deployment**:
```bash
# AWS - Deploy Cisco vEdge
aws ec2 run-instances \
  --image-id ami-cisco-vedge \
  --instance-type c5.xlarge \
  --subnet-id subnet-12345678 \
  --security-group-ids sg-12345678 \
  --user-data file://vedge-config.txt

# GCP - Deploy Cisco vEdge
gcloud compute instances create cisco-vedge-gcp \
  --image=cisco-vedge-image \
  --machine-type=n2-standard-4 \
  --subnet=default \
  --zone=us-central1-a

# Azure - Deploy Cisco vEdge
az vm create \
  --resource-group myResourceGroup \
  --name cisco-vedge-azure \
  --image cisco-vedge-image \
  --size Standard_D4s_v3 \
  --vnet-name myVNet \
  --subnet default
```

**Configuration**:
```yaml
# vEdge configuration
system:
  site-id: 100
  system-ip: 10.0.1.1
  organization-name: myorg

vpn 0:  # Transport VPN
  interface ge0/0:
    ip-address: 54.123.45.67/24
    tunnel-interface:
      encapsulation: ipsec
      color: public-internet

vpn 10:  # Service VPN
  interface ge0/1:
    ip-address: 10.0.1.1/24
  route:
    - prefix: 10.1.0.0/16
      next-hop: 10.0.1.254
```

**Cost**: $500-2,000/month per site (license + instance)

#### 2. VMware SD-WAN (VeloCloud)

**Features**:
- Dynamic path selection
- Cloud-native architecture
- Application optimization
- Built-in security

**Deployment**:
```bash
# Deploy VMware SD-WAN Edge in AWS
aws cloudformation create-stack \
  --stack-name vmware-sdwan-aws \
  --template-url https://s3.amazonaws.com/vmware-sdwan/edge-template.yaml \
  --parameters \
    ParameterKey=InstanceType,ParameterValue=c5.xlarge \
    ParameterKey=SubnetId,ParameterValue=subnet-12345678 \
    ParameterKey=ActivationKey,ParameterValue=XXXX-XXXX-XXXX
```

**Cost**: $300-1,500/month per site

#### 3. Silver Peak (HPE Aruba EdgeConnect)

**Features**:
- WAN optimization
- First-packet iQ (instant path selection)
- Orchestrator for centralized management
- Cloud-first architecture

**Cost**: $400-1,800/month per site

#### 4. Fortinet Secure SD-WAN

**Features**:
- Integrated FortiGate firewall
- Security-driven networking
- Multi-cloud support
- Zero-touch deployment

**Cost**: $300-1,200/month per site

### SD-WAN Use Cases

#### Use Case 1: Multi-Cloud Application
```
User → SD-WAN Edge (AWS) → Intelligent Routing:
  - Low latency traffic → AWS services
  - Database queries → GCP (Cloud SQL)
  - AI/ML workloads → Azure (Azure ML)
```

#### Use Case 2: Disaster Recovery
```
Primary: AWS (us-east-1)
Backup: GCP (us-central1)

SD-WAN automatically fails over to GCP if AWS is unavailable
```

#### Use Case 3: Cost Optimization
```
High-priority traffic → Direct Connect (low latency)
Bulk data transfer → Internet (low cost)
Backup traffic → VPN (encrypted, moderate cost)
```

### SD-WAN Configuration Example

**Policy-Based Routing**:
```yaml
# Route critical traffic over Direct Connect
policy:
  - name: critical-apps
    match:
      application: [SAP, Oracle, Salesforce]
    action:
      path: direct-connect
      priority: high

  - name: bulk-transfer
    match:
      application: [backup, file-sync]
    action:
      path: internet
      priority: low

  - name: default
    match:
      application: [*]
    action:
      path: vpn
      priority: medium
```

---

## Service Mesh

**Service Mesh** provides application-level connectivity and observability across clouds.

### Service Mesh Benefits

- **Service Discovery**: Automatic service registration
- **Load Balancing**: Client-side load balancing
- **Encryption**: mTLS between services
- **Observability**: Distributed tracing, metrics
- **Traffic Management**: Canary deployments, circuit breakers
- **Multi-Cloud**: Unified control plane

### Service Mesh Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                  Service Mesh Control Plane                  │
│                  (Istiod / Consul Server)                    │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │         │ │         │ │         │
    │ ┌─────┐ │ │ ┌─────┐ │ │ ┌─────┐ │
    │ │App A│ │ │ │App B│ │ │ │App C│ │
    │ │+Envoy│ │ │+Envoy│ │ │+Envoy│ │
    │ └─────┘ │ │ └─────┘ │ │ └─────┘ │
    └─────────┘ └─────────┘ └─────────┘
```

### Major Service Mesh Solutions

#### 1. Istio (Multi-Cloud)

**Features**:
- Multi-cluster support
- mTLS encryption
- Traffic management
- Observability (Prometheus, Grafana, Jaeger)

**Installation**:
```bash
# Install Istio on AWS EKS
istioctl install --set profile=default

# Install Istio on GCP GKE
istioctl install --set profile=default

# Configure multi-cluster
istioctl x create-remote-secret \
  --name=gke-cluster \
  --context=gke-context | \
  kubectl apply -f - --context=eks-context
```

**Service Configuration**:
```yaml
# VirtualService for cross-cloud routing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: payment-service
spec:
  hosts:
  - payment.example.com
  http:
  - match:
    - headers:
        region:
          exact: us-east
    route:
    - destination:
        host: payment.aws.svc.cluster.local
        port:
          number: 8080
  - match:
    - headers:
        region:
          exact: us-central
    route:
    - destination:
        host: payment.gcp.svc.cluster.local
        port:
          number: 8080
```

**Cost**: Free (open-source), infrastructure costs only

#### 2. Consul (HashiCorp)

**Features**:
- Service discovery
- Health checking
- KV store
- Multi-datacenter support
- Service mesh (Consul Connect)

**Installation**:
```bash
# Install Consul on AWS
helm install consul hashicorp/consul \
  --set global.name=consul-aws \
  --set global.datacenter=aws-us-east-1

# Install Consul on GCP
helm install consul hashicorp/consul \
  --set global.name=consul-gcp \
  --set global.datacenter=gcp-us-central1

# Configure WAN federation
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: consul-federation
data:
  config.json: |
    {
      "primary_datacenter": "aws-us-east-1",
      "retry_join_wan": ["consul-aws.example.com"]
    }
EOF
```

**Service Registration**:
```json
{
  "service": {
    "name": "payment-service",
    "tags": ["aws", "production"],
    "port": 8080,
    "check": {
      "http": "http://localhost:8080/health",
      "interval": "10s"
    }
  }
}
```

**Cost**: Free (open-source) or $0.03/hr per service (Consul Enterprise)

#### 3. Linkerd

**Features**:
- Lightweight (Rust-based)
- Automatic mTLS
- Multi-cluster support
- Low resource overhead

**Installation**:
```bash
# Install Linkerd on AWS EKS
linkerd install | kubectl apply -f -

# Install Linkerd on GCP GKE
linkerd install | kubectl apply -f -

# Link clusters
linkerd multicluster link \
  --cluster-name gke-cluster \
  --api-server-address https://gke.example.com:6443 | \
  kubectl apply -f - --context=eks-context
```

**Cost**: Free (open-source)

### Service Mesh Use Cases

#### Use Case 1: Cross-Cloud Service Communication
```
AWS (Order Service) → Istio → GCP (Payment Service) → Azure (Inventory Service)
```

**Benefits**:
- Automatic mTLS encryption
- Distributed tracing
- Circuit breakers
- Retry logic

#### Use Case 2: Canary Deployment Across Clouds
```yaml
# Deploy new version to GCP, route 10% traffic
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: payment-service
spec:
  hosts:
  - payment.example.com
  http:
  - route:
    - destination:
        host: payment.aws.svc.cluster.local
        subset: v1
      weight: 90
    - destination:
        host: payment.gcp.svc.cluster.local
        subset: v2
      weight: 10
```

---

## API Gateway Integration

**API Gateway** provides centralized entry point for multi-cloud services.

### API Gateway Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                               │
│              (Kong / Apigee / AWS API Gateway)               │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Routing Rules:                                       │   │
│  │  /orders → AWS Lambda                                 │   │
│  │  /payments → GCP Cloud Functions                      │   │
│  │  /inventory → Azure Functions                         │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
    │   AWS   │ │   GCP   │ │  Azure  │
    │ Lambda  │ │  Cloud  │ │Functions│
    │         │ │Functions│ │         │
    └─────────┘ └─────────┘ └─────────┘
```

### Kong API Gateway (Multi-Cloud)

**Installation**:
```bash
# Deploy Kong on Kubernetes (works on EKS, GKE, AKS)
helm install kong kong/kong \
  --set ingressController.installCRDs=false \
  --set proxy.type=LoadBalancer

# Configure routes
kubectl apply -f - <<EOF
apiVersion: configuration.konghq.com/v1
kind: KongIngress
metadata:
  name: multi-cloud-routing
route:
  methods:
  - GET
  - POST
  paths:
  - /orders
  - /payments
  - /inventory
upstream:
  hash_on: none
  hash_fallback: none
EOF
```

**Route Configuration**:
```yaml
# Route to AWS
apiVersion: v1
kind: Service
metadata:
  name: orders-service
  annotations:
    konghq.com/override: multi-cloud-routing
spec:
  type: ExternalName
  externalName: orders.aws.example.com
  ports:
  - port: 443

# Route to GCP
apiVersion: v1
kind: Service
metadata:
  name: payments-service
  annotations:
    konghq.com/override: multi-cloud-routing
spec:
  type: ExternalName
  externalName: payments.gcp.example.com
  ports:
  - port: 443
```

**Cost**: Free (open-source) or $0.03/hr per instance (Kong Enterprise)

### Google Apigee (Multi-Cloud)

**Features**:
- API management
- Analytics
- Developer portal
- Monetization

**Configuration**:
```xml
<!-- Apigee proxy configuration -->
<ProxyEndpoint name="default">
  <HTTPProxyConnection>
    <BasePath>/api</BasePath>
  </HTTPProxyConnection>
  <RouteRule name="aws-route">
    <Condition>request.path MatchesPath "/orders"</Condition>
    <TargetEndpoint>aws-target</TargetEndpoint>
  </RouteRule>
  <RouteRule name="gcp-route">
    <Condition>request.path MatchesPath "/payments"</Condition>
    <TargetEndpoint>gcp-target</TargetEndpoint>
  </RouteRule>
</ProxyEndpoint>

<TargetEndpoint name="aws-target">
  <HTTPTargetConnection>
    <URL>https://api.aws.example.com</URL>
  </HTTPTargetConnection>
</TargetEndpoint>

<TargetEndpoint name="gcp-target">
  <HTTPTargetConnection>
    <URL>https://api.gcp.example.com</URL>
  </HTTPTargetConnection>
</TargetEndpoint>
```

**Cost**: $0.50-$3.00 per million API calls

---

## Database Replication

**Database replication** enables data synchronization across clouds.

### Replication Strategies

#### 1. Active-Passive (Disaster Recovery)
```
AWS (Primary) → Replication → GCP (Standby)
```

**Use Case**: Disaster recovery, read replicas

#### 2. Active-Active (Multi-Master)
```
AWS (Primary) ↔ Bidirectional Replication ↔ GCP (Primary)
```

**Use Case**: Global applications, low latency reads

#### 3. Multi-Region (Sharding)
```
US Users → AWS (us-east-1)
EU Users → GCP (europe-west1)
APAC Users → Azure (asia-southeast1)
```

**Use Case**: Data residency, compliance

### PostgreSQL Replication (AWS RDS to GCP Cloud SQL)

**Setup**:
```bash
# AWS RDS - Enable logical replication
aws rds modify-db-parameter-group \
  --db-parameter-group-name myparamgroup \
  --parameters "ParameterName=rds.logical_replication,ParameterValue=1,ApplyMethod=pending-reboot"

# Reboot instance
aws rds reboot-db-instance --db-instance-identifier mydb

# Create publication
psql -h mydb.aws.rds.amazonaws.com -U postgres -c "CREATE PUBLICATION mypub FOR ALL TABLES;"

# GCP Cloud SQL - Create subscription
psql -h mydb.gcp.cloudsql.com -U postgres -c "CREATE SUBSCRIPTION mysub CONNECTION 'host=mydb.aws.rds.amazonaws.com dbname=mydb user=postgres password=xxx' PUBLICATION mypub;"
```

**Monitoring**:
```sql
-- Check replication lag
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;
```

### MongoDB Atlas (Multi-Cloud)

**Features**:
- Global clusters (AWS + GCP + Azure)
- Automatic failover
- Zone sharding
- Cross-region replication

**Configuration**:
```javascript
// Create global cluster
atlas clusters create globalCluster \
  --provider AWS \
  --region US_EAST_1 \
  --tier M30 \
  --replicationSpecs '[
    {
      "regionConfigs": [
        {
          "providerName": "AWS",
          "regionName": "US_EAST_1",
          "priority": 7,
          "electableNodes": 3
        },
        {
          "providerName": "GCP",
          "regionName": "CENTRAL_US",
          "priority": 6,
          "electableNodes": 2
        },
        {
          "providerName": "AZURE",
          "regionName": "US_EAST_2",
          "priority": 5,
          "electableNodes": 2
        }
      ]
    }
  ]'
```

**Cost**: $0.08-$10/hr depending on tier

### MySQL Replication (AWS RDS to Azure Database)

**Setup**:
```bash
# AWS RDS - Create read replica endpoint
aws rds create-db-instance-read-replica \
  --db-instance-identifier mydb-replica \
  --source-db-instance-identifier mydb \
  --publicly-accessible

# Get binlog position
mysql -h mydb.aws.rds.amazonaws.com -u admin -p -e "SHOW MASTER STATUS;"

# Azure Database for MySQL - Configure replication
az mysql server replica create \
  --name mydb-azure \
  --resource-group myResourceGroup \
  --source-server mydb-aws-endpoint \
  --location eastus
```

---

## Best Practices

### 1. Network Design

**Principle**: Design for redundancy and failover

```
Primary Path: Direct Connect (low latency)
Backup Path: VPN (encrypted)
Tertiary Path: Internet (last resort)
```

**Implementation**:
- Use BGP for automatic failover
- Configure health checks
- Set up monitoring and alerts

### 2. Security

**Principle**: Zero trust, encrypt everything

- **Encryption in Transit**: TLS 1.3, IPsec, mTLS
- **Encryption at Rest**: AES-256
- **Authentication**: OAuth 2.0, JWT, mTLS
- **Authorization**: RBAC, ABAC
- **Network Segmentation**: Security groups, NACLs, firewall rules

**Example**:
```yaml
# Istio mTLS policy
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT
```

### 3. Observability

**Principle**: Monitor everything, alert on anomalies

**Metrics**:
- Latency (p50, p95, p99)
- Throughput (requests/sec)
- Error rate (%)
- Bandwidth utilization

**Tools**:
- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **Jaeger**: Distributed tracing
- **ELK Stack**: Log aggregation

**Example**:
```yaml
# Prometheus scrape config
scrape_configs:
  - job_name: 'aws-services'
    static_configs:
    - targets: ['aws-service:9090']
  - job_name: 'gcp-services'
    static_configs:
    - targets: ['gcp-service:9090']
  - job_name: 'azure-services'
    static_configs:
    - targets: ['azure-service:9090']
```

### 4. Cost Optimization

**Principle**: Use the right tool for the job

- **Low bandwidth (<2 TB/month)**: VPN
- **High bandwidth (>2 TB/month)**: Direct Connect
- **Variable workloads**: SD-WAN with intelligent routing
- **API-driven**: API Gateway with caching

**Cost Monitoring**:
```bash
# AWS Cost Explorer
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --filter file://filter.json

# GCP Billing
gcloud billing accounts list
gcloud billing accounts describe ACCOUNT_ID
```

### 5. Disaster Recovery

**Principle**: Plan for failure

**RTO (Recovery Time Objective)**: How long to recover
**RPO (Recovery Point Objective)**: How much data loss acceptable

**Strategy**:
```
Tier 1 (Critical): RTO < 1 hour, RPO < 5 minutes
  → Active-Active multi-cloud with real-time replication

Tier 2 (Important): RTO < 4 hours, RPO < 1 hour
  → Active-Passive with hourly backups

Tier 3 (Standard): RTO < 24 hours, RPO < 24 hours
  → Backup to secondary cloud daily
```

---

## Summary - Part 3

**Application-Level Integration** provides flexibility and control:
- **SD-WAN**: Intelligent routing, application-aware
- **Service Mesh**: mTLS, observability, traffic management
- **API Gateway**: Centralized entry point, routing
- **Database Replication**: Data synchronization across clouds

**Key Takeaways**:
1. Use SD-WAN for intelligent multi-path routing
2. Use Service Mesh for microservices communication
3. Use API Gateway for centralized API management
4. Use database replication for data synchronization
5. Implement comprehensive monitoring and alerting
6. Design for redundancy and failover
7. Encrypt everything (in transit and at rest)
8. Optimize costs based on workload patterns

**Cost Summary** (Monthly):
- **VPN**: $280-350
- **Direct Connect**: $480-530
- **SD-WAN**: $300-2,000 per site
- **Service Mesh**: Infrastructure costs only (free software)
- **API Gateway**: $0.50-$3.00 per million calls

**Next**: Part 4 covers real-world architectures and case studies.
