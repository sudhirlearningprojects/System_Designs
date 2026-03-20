# Multi-Cloud Connectivity Guide - Part 1: Overview & VPN

## Table of Contents
1. [Overview](#overview)
2. [Connectivity Options](#connectivity-options)
3. [VPN Connections](#vpn-connections)
4. [AWS to GCP VPN](#aws-to-gcp-vpn)
5. [AWS to Azure VPN](#aws-to-azure-vpn)

---

## Overview

**Multi-cloud connectivity** enables services in different cloud providers to communicate securely and efficiently.

### Why Multi-Cloud?

**Use Cases**:
- **Disaster Recovery**: Failover to another cloud
- **Best-of-Breed**: Use best services from each cloud
- **Vendor Lock-in Avoidance**: Reduce dependency on single provider
- **Data Residency**: Comply with regional regulations
- **Migration**: Gradual migration between clouds
- **Hybrid Architecture**: Distribute workloads across clouds

### Challenges

- **Network Complexity**: Different networking models
- **Security**: Multiple security boundaries
- **Latency**: Inter-cloud traffic over internet
- **Cost**: Data transfer charges
- **Management**: Multiple consoles and tools
- **Consistency**: Different APIs and services

---

## Connectivity Options

### 1. Public Internet (Simplest)
```
AWS Service → Public IP → Internet → Public IP → GCP Service
```
**Pros**: Simple, no setup
**Cons**: Not secure, high latency, no SLA
**Use Case**: Non-sensitive data, testing

### 2. VPN (Site-to-Site)
```
AWS VPC → VPN Gateway → IPsec Tunnel → VPN Gateway → GCP VPC
```
**Pros**: Encrypted, moderate cost
**Cons**: Limited bandwidth (1.25 Gbps), internet-dependent
**Use Case**: Low-to-medium bandwidth, encrypted traffic

### 3. Direct Connect / Dedicated Interconnect
```
AWS VPC → Direct Connect → Colocation → Partner Interconnect → GCP VPC
```
**Pros**: High bandwidth, low latency, predictable performance
**Cons**: Expensive, complex setup, long provisioning time
**Use Case**: High bandwidth, mission-critical workloads

### 4. Cloud Interconnect Partners
```
AWS → Equinix/Megaport → GCP
```
**Pros**: Easier than direct connections, flexible
**Cons**: Additional cost, partner dependency
**Use Case**: Multi-cloud without direct connections

### 5. SD-WAN Solutions
```
AWS → SD-WAN (Cisco/VMware) → GCP
```
**Pros**: Intelligent routing, application-aware
**Cons**: Additional cost, complexity
**Use Case**: Complex multi-cloud architectures

### 6. Service Mesh / API Gateway
```
AWS Service → API Gateway → Internet → API Gateway → GCP Service
```
**Pros**: Application-level control, observability
**Cons**: Application changes required
**Use Case**: Microservices, API-driven architectures

---

## VPN Connections

### VPN Overview

**IPsec VPN** creates encrypted tunnels between cloud providers.

**Characteristics**:
- **Encryption**: AES-256, SHA-256
- **Bandwidth**: Up to 1.25 Gbps per tunnel
- **Latency**: Internet latency (variable)
- **Cost**: Low ($0.05/hr per connection)
- **Setup Time**: Minutes to hours

### VPN Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                         AWS VPC                              │
│                      10.0.0.0/16                             │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Virtual Private Gateway (VGW)                 │   │
│  │         Public IP: 54.123.45.67                       │   │
│  └────────────────────┬─────────────────────────────────┘   │
└───────────────────────┼───────────────────────────────────────┘
                        │
                        │ IPsec Tunnel (Encrypted)
                        │ Internet
                        │
┌───────────────────────┼───────────────────────────────────────┐
│  ┌────────────────────▼─────────────────────────────────┐   │
│  │         Cloud VPN Gateway                             │   │
│  │         Public IP: 35.234.56.78                       │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│                       GCP VPC                                 │
│                     10.1.0.0/16                              │
└─────────────────────────────────────────────────────────────┘
```

### VPN Components

**AWS Side**:
- **Virtual Private Gateway (VGW)**: VPN endpoint attached to VPC
- **Customer Gateway (CGW)**: Represents remote VPN device
- **VPN Connection**: IPsec tunnels (2 for redundancy)

**GCP Side**:
- **Cloud VPN Gateway**: VPN endpoint in GCP
- **VPN Tunnel**: IPsec tunnel to AWS
- **Cloud Router**: BGP routing (optional)

**Azure Side**:
- **Virtual Network Gateway**: VPN endpoint in VNet
- **Local Network Gateway**: Represents AWS VGW
- **Connection**: IPsec tunnel

---

## AWS to GCP VPN

### Step-by-Step Setup

#### Step 1: AWS - Create Virtual Private Gateway

```bash
# Create VGW
aws ec2 create-vpn-gateway \
  --type ipsec.1 \
  --amazon-side-asn 64512 \
  --tag-specifications 'ResourceType=vpn-gateway,Tags=[{Key=Name,Value=aws-to-gcp-vgw}]'

# Output: vgw-12345678

# Attach to VPC
aws ec2 attach-vpn-gateway \
  --vpn-gateway-id vgw-12345678 \
  --vpc-id vpc-12345678

# Enable route propagation
aws ec2 enable-vgw-route-propagation \
  --route-table-id rtb-12345678 \
  --gateway-id vgw-12345678
```

#### Step 2: GCP - Create Cloud VPN Gateway

```bash
# Create VPN gateway
gcloud compute vpn-gateways create gcp-to-aws-gateway \
  --network=default \
  --region=us-central1

# Get external IP
gcloud compute addresses create gcp-vpn-ip \
  --region=us-central1

# Output: 35.234.56.78
```

#### Step 3: AWS - Create Customer Gateway

```bash
# Create CGW (using GCP VPN IP)
aws ec2 create-customer-gateway \
  --type ipsec.1 \
  --public-ip 35.234.56.78 \
  --bgp-asn 65000 \
  --tag-specifications 'ResourceType=customer-gateway,Tags=[{Key=Name,Value=gcp-cgw}]'

# Output: cgw-87654321
```

#### Step 4: AWS - Create VPN Connection

```bash
# Create VPN connection
aws ec2 create-vpn-connection \
  --type ipsec.1 \
  --customer-gateway-id cgw-87654321 \
  --vpn-gateway-id vgw-12345678 \
  --options TunnelOptions='[{TunnelInsideCidr=169.254.10.0/30,PreSharedKey=MySecurePreSharedKey123},{TunnelInsideCidr=169.254.10.4/30,PreSharedKey=MySecurePreSharedKey456}]' \
  --tag-specifications 'ResourceType=vpn-connection,Tags=[{Key=Name,Value=aws-to-gcp-vpn}]'

# Output: vpn-11223344

# Download configuration
aws ec2 describe-vpn-connections \
  --vpn-connection-ids vpn-11223344 \
  --query 'VpnConnections[0].CustomerGatewayConfiguration' \
  --output text > vpn-config.xml
```

**VPN Configuration Details**:
```
Tunnel 1:
  AWS Outside IP: 54.123.45.67
  GCP Outside IP: 35.234.56.78
  Inside CIDR: 169.254.10.0/30
  Pre-Shared Key: MySecurePreSharedKey123

Tunnel 2:
  AWS Outside IP: 54.123.45.68
  GCP Outside IP: 35.234.56.78
  Inside CIDR: 169.254.10.4/30
  Pre-Shared Key: MySecurePreSharedKey456
```

#### Step 5: GCP - Create VPN Tunnels

```bash
# Create Cloud Router (for BGP)
gcloud compute routers create gcp-to-aws-router \
  --network=default \
  --region=us-central1 \
  --asn=65000

# Create VPN Tunnel 1
gcloud compute vpn-tunnels create tunnel-1 \
  --peer-address=54.123.45.67 \
  --shared-secret=MySecurePreSharedKey123 \
  --ike-version=2 \
  --target-vpn-gateway=gcp-to-aws-gateway \
  --router=gcp-to-aws-router \
  --region=us-central1

# Create VPN Tunnel 2
gcloud compute vpn-tunnels create tunnel-2 \
  --peer-address=54.123.45.68 \
  --shared-secret=MySecurePreSharedKey456 \
  --ike-version=2 \
  --target-vpn-gateway=gcp-to-aws-gateway \
  --router=gcp-to-aws-router \
  --region=us-central1

# Configure BGP sessions
gcloud compute routers add-bgp-peer gcp-to-aws-router \
  --peer-name=aws-tunnel-1 \
  --peer-asn=64512 \
  --interface=tunnel-1 \
  --peer-ip-address=169.254.10.1 \
  --region=us-central1

gcloud compute routers add-bgp-peer gcp-to-aws-router \
  --peer-name=aws-tunnel-2 \
  --peer-asn=64512 \
  --interface=tunnel-2 \
  --peer-ip-address=169.254.10.5 \
  --region=us-central1
```

#### Step 6: Configure Firewall Rules

**AWS Security Group**:
```bash
# Allow traffic from GCP VPC (10.1.0.0/16)
aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol all \
  --cidr 10.1.0.0/16
```

**GCP Firewall Rule**:
```bash
# Allow traffic from AWS VPC (10.0.0.0/16)
gcloud compute firewall-rules create allow-from-aws \
  --network=default \
  --allow=all \
  --source-ranges=10.0.0.0/16
```

#### Step 7: Verify Connection

**AWS**:
```bash
# Check VPN status
aws ec2 describe-vpn-connections \
  --vpn-connection-ids vpn-11223344 \
  --query 'VpnConnections[0].VgwTelemetry'

# Output:
# [
#   {
#     "Status": "UP",
#     "LastStatusChange": "2024-01-15T10:30:00Z"
#   },
#   {
#     "Status": "UP",
#     "LastStatusChange": "2024-01-15T10:30:00Z"
#   }
# ]
```

**GCP**:
```bash
# Check tunnel status
gcloud compute vpn-tunnels describe tunnel-1 \
  --region=us-central1 \
  --format="get(status)"

# Output: ESTABLISHED
```

**Test Connectivity**:
```bash
# From AWS EC2 instance (10.0.1.10)
ping 10.1.1.10  # GCP VM private IP

# From GCP VM (10.1.1.10)
ping 10.0.1.10  # AWS EC2 private IP
```

### VPN Performance

**Bandwidth**: 1.25 Gbps per tunnel (2.5 Gbps total with 2 tunnels)
**Latency**: Internet latency + ~5-10ms VPN overhead
**Packet Loss**: Depends on internet quality

**Monitoring**:
```bash
# AWS CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/VPN \
  --metric-name TunnelDataIn \
  --dimensions Name=VpnId,Value=vpn-11223344 \
  --start-time 2024-01-15T00:00:00Z \
  --end-time 2024-01-15T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### VPN Cost

**AWS**:
- VPN Connection: $0.05/hr = $36.50/month
- Data Transfer Out: $0.09/GB (first 10 TB)

**GCP**:
- VPN Gateway: $0.05/hr = $36.50/month
- Data Transfer Out: $0.12/GB (first 10 TB)

**Example** (1 TB/month):
- AWS: $36.50 + (1000 × $0.09) = $126.50/month
- GCP: $36.50 + (1000 × $0.12) = $156.50/month
- **Total: $283/month**

---

## AWS to Azure VPN

### Step-by-Step Setup

#### Step 1: AWS - Create Virtual Private Gateway

```bash
# Create VGW
aws ec2 create-vpn-gateway \
  --type ipsec.1 \
  --amazon-side-asn 64512 \
  --tag-specifications 'ResourceType=vpn-gateway,Tags=[{Key=Name,Value=aws-to-azure-vgw}]'

# Attach to VPC
aws ec2 attach-vpn-gateway \
  --vpn-gateway-id vgw-12345678 \
  --vpc-id vpc-12345678
```

#### Step 2: Azure - Create Virtual Network Gateway

```bash
# Create public IP
az network public-ip create \
  --resource-group myResourceGroup \
  --name azure-vpn-ip \
  --allocation-method Dynamic

# Create VPN gateway (takes 30-45 minutes)
az network vnet-gateway create \
  --resource-group myResourceGroup \
  --name azure-vpn-gateway \
  --public-ip-address azure-vpn-ip \
  --vnet myVNet \
  --gateway-type Vpn \
  --vpn-type RouteBased \
  --sku VpnGw1 \
  --no-wait

# Get public IP (after gateway is created)
az network public-ip show \
  --resource-group myResourceGroup \
  --name azure-vpn-ip \
  --query ipAddress \
  --output tsv

# Output: 52.168.10.20
```

#### Step 3: AWS - Create Customer Gateway

```bash
# Create CGW (using Azure VPN IP)
aws ec2 create-customer-gateway \
  --type ipsec.1 \
  --public-ip 52.168.10.20 \
  --bgp-asn 65515 \
  --tag-specifications 'ResourceType=customer-gateway,Tags=[{Key=Name,Value=azure-cgw}]'

# Output: cgw-87654321
```

#### Step 4: AWS - Create VPN Connection

```bash
# Create VPN connection
aws ec2 create-vpn-connection \
  --type ipsec.1 \
  --customer-gateway-id cgw-87654321 \
  --vpn-gateway-id vgw-12345678 \
  --options StaticRoutesOnly=true \
  --tag-specifications 'ResourceType=vpn-connection,Tags=[{Key=Name,Value=aws-to-azure-vpn}]'

# Output: vpn-11223344

# Add static route for Azure VNet (10.2.0.0/16)
aws ec2 create-vpn-connection-route \
  --vpn-connection-id vpn-11223344 \
  --destination-cidr-block 10.2.0.0/16

# Get AWS VPN public IPs
aws ec2 describe-vpn-connections \
  --vpn-connection-ids vpn-11223344 \
  --query 'VpnConnections[0].VgwTelemetry[*].OutsideIpAddress'

# Output: ["54.123.45.67", "54.123.45.68"]
```

#### Step 5: Azure - Create Local Network Gateway

```bash
# Create Local Network Gateway (represents AWS)
az network local-gateway create \
  --resource-group myResourceGroup \
  --name aws-local-gateway \
  --gateway-ip-address 54.123.45.67 \
  --local-address-prefixes 10.0.0.0/16
```

#### Step 6: Azure - Create VPN Connection

```bash
# Create connection
az network vpn-connection create \
  --resource-group myResourceGroup \
  --name azure-to-aws-connection \
  --vnet-gateway1 azure-vpn-gateway \
  --local-gateway2 aws-local-gateway \
  --shared-key MySecurePreSharedKey123 \
  --location eastus
```

#### Step 7: Configure Network Security

**AWS Security Group**:
```bash
# Allow traffic from Azure VNet (10.2.0.0/16)
aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol all \
  --cidr 10.2.0.0/16
```

**Azure Network Security Group**:
```bash
# Allow traffic from AWS VPC (10.0.0.0/16)
az network nsg rule create \
  --resource-group myResourceGroup \
  --nsg-name myNSG \
  --name allow-from-aws \
  --priority 100 \
  --source-address-prefixes 10.0.0.0/16 \
  --destination-address-prefixes '*' \
  --access Allow \
  --protocol '*'
```

#### Step 8: Verify Connection

**AWS**:
```bash
# Check VPN status
aws ec2 describe-vpn-connections \
  --vpn-connection-ids vpn-11223344 \
  --query 'VpnConnections[0].VgwTelemetry[*].Status'

# Output: ["UP", "UP"]
```

**Azure**:
```bash
# Check connection status
az network vpn-connection show \
  --resource-group myResourceGroup \
  --name azure-to-aws-connection \
  --query connectionStatus

# Output: "Connected"
```

**Test Connectivity**:
```bash
# From AWS EC2 instance
ping 10.2.1.10  # Azure VM private IP

# From Azure VM
ping 10.0.1.10  # AWS EC2 private IP
```

### Azure VPN Cost

**Azure**:
- VPN Gateway (VpnGw1): $0.19/hr = $138.70/month
- Data Transfer Out: $0.087/GB (first 10 TB)

**AWS**:
- VPN Connection: $0.05/hr = $36.50/month
- Data Transfer Out: $0.09/GB (first 10 TB)

**Example** (1 TB/month):
- Azure: $138.70 + (1000 × $0.087) = $225.70/month
- AWS: $36.50 + (1000 × $0.09) = $126.50/month
- **Total: $352.20/month**

---

## Summary - Part 1

**VPN Connections** provide encrypted connectivity between clouds:
- **Setup Time**: Minutes to hours
- **Bandwidth**: Up to 1.25 Gbps per tunnel
- **Cost**: $280-350/month (including 1 TB data transfer)
- **Use Case**: Low-to-medium bandwidth, encrypted traffic

**Key Takeaways**:
1. VPN is simplest multi-cloud connectivity option
2. Use 2 tunnels for redundancy
3. Configure BGP for dynamic routing (AWS-GCP)
4. Use static routes for simpler setup (AWS-Azure)
5. Monitor tunnel status and bandwidth
6. Consider Direct Connect for high bandwidth

**Next**: Part 2 covers Direct Connect, Dedicated Interconnect, and Cloud Interconnect Partners.
