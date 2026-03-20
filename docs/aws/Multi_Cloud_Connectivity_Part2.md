# Multi-Cloud Connectivity Guide - Part 2: Direct Connect & Dedicated Interconnect

## Table of Contents
1. [Direct Connect Overview](#direct-connect-overview)
2. [AWS Direct Connect to GCP](#aws-direct-connect-to-gcp)
3. [AWS Direct Connect to Azure](#aws-direct-connect-to-azure)
4. [Cloud Interconnect Partners](#cloud-interconnect-partners)
5. [Cost Comparison](#cost-comparison)

---

## Direct Connect Overview

**Direct Connect** provides dedicated network connection from on-premises to AWS, which can be extended to other clouds.

### Direct Connect vs VPN

| Feature | Direct Connect | VPN |
|---------|----------------|-----|
| Bandwidth | 1-100 Gbps | Up to 1.25 Gbps |
| Latency | Low, consistent | Variable (internet) |
| Setup Time | Weeks to months | Minutes to hours |
| Cost | High | Low |
| Encryption | Optional (MACsec) | Built-in (IPsec) |
| Use Case | High bandwidth, mission-critical | Low bandwidth, testing |

### Direct Connect Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                         AWS                                  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Direct Connect Gateway                        │   │
│  │         (Global resource)                             │   │
│  └────────────────────┬─────────────────────────────────┘   │
│                       │                                       │
│  ┌────────────────────▼─────────────────────────────────┐   │
│  │         Virtual Private Gateway (VGW)                 │   │
│  │         Attached to VPC (10.0.0.0/16)                │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────┼───────────────────────────────────────┘
                        │
                        │ Dedicated Connection
                        │ (1 Gbps / 10 Gbps / 100 Gbps)
                        │
┌───────────────────────▼───────────────────────────────────────┐
│              AWS Direct Connect Location                      │
│              (Equinix, Megaport, etc.)                       │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Cross-Connect                                 │   │
│  └────────────────────┬─────────────────────────────────┘   │
└───────────────────────┼───────────────────────────────────────┘
                        │
                        │ Partner Connection
                        │
┌───────────────────────▼───────────────────────────────────────┐
│              GCP Partner Interconnect                         │
│              (Equinix, Megaport, etc.)                       │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Cloud Router                                  │   │
│  │         Attached to VPC (10.1.0.0/16)                │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│                       GCP                                     │
└─────────────────────────────────────────────────────────────┘
```

### Direct Connect Components

**AWS Direct Connect**:
- **Connection**: Physical connection (1/10/100 Gbps)
- **Virtual Interface (VIF)**: Logical connection to VPC
- **Direct Connect Gateway**: Connect to multiple VPCs/regions
- **LAG (Link Aggregation Group)**: Combine multiple connections

**GCP Partner Interconnect**:
- **VLAN Attachment**: Logical connection to VPC
- **Cloud Router**: BGP routing
- **Partner**: Equinix, Megaport, etc.

**Azure ExpressRoute**:
- **Circuit**: Physical connection
- **Peering**: Private (VNet) or Microsoft (Office 365, Dynamics)
- **Gateway**: ExpressRoute Gateway in VNet

---

## AWS Direct Connect to GCP

### Architecture Overview
```
AWS VPC (us-east-1) → Direct Connect → Equinix → Partner Interconnect → GCP VPC (us-central1)
```

### Step-by-Step Setup

#### Step 1: Order AWS Direct Connect

**Option A: Dedicated Connection** (1/10/100 Gbps)
```bash
# Create connection
aws directconnect create-connection \
  --location EqDC2 \
  --bandwidth 1Gbps \
  --connection-name aws-to-gcp-dx

# Output: dxcon-12345678

# AWS will provide LOA-CFA (Letter of Authorization)
# Send LOA-CFA to colocation provider (Equinix)
```

**Option B: Hosted Connection** (50 Mbps - 10 Gbps)
```bash
# Request from AWS Partner (Equinix, Megaport)
# Partner provisions connection
# Accept connection in AWS Console
```

**Provisioning Time**: 2-4 weeks for dedicated, 1-2 weeks for hosted

#### Step 2: Create Direct Connect Gateway

```bash
# Create DX Gateway (global resource)
aws directconnect create-direct-connect-gateway \
  --direct-connect-gateway-name aws-to-gcp-dxgw \
  --amazon-side-asn 64512

# Output: dxgw-87654321
```

#### Step 3: Create Virtual Private Gateway

```bash
# Create VGW
aws ec2 create-vpn-gateway \
  --type ipsec.1 \
  --amazon-side-asn 64512

# Output: vgw-11223344

# Attach to VPC
aws ec2 attach-vpn-gateway \
  --vpn-gateway-id vgw-11223344 \
  --vpc-id vpc-12345678

# Associate VGW with DX Gateway
aws directconnect create-direct-connect-gateway-association \
  --direct-connect-gateway-id dxgw-87654321 \
  --gateway-id vgw-11223344 \
  --add-allowed-prefixes-to-direct-connect-gateway cidr=10.0.0.0/16
```

#### Step 4: Create Private Virtual Interface (VIF)

```bash
# Create private VIF
aws directconnect create-private-virtual-interface \
  --connection-id dxcon-12345678 \
  --new-private-virtual-interface '{
    "virtualInterfaceName": "aws-to-gcp-vif",
    "vlan": 100,
    "asn": 65000,
    "authKey": "MyBGPAuthKey123",
    "amazonAddress": "169.254.10.1/30",
    "customerAddress": "169.254.10.2/30",
    "addressFamily": "ipv4",
    "directConnectGatewayId": "dxgw-87654321"
  }'

# Output: dxvif-55667788
```

**VIF Configuration**:
- **VLAN**: 100 (must match partner side)
- **BGP ASN**: 65000 (GCP side)
- **BGP Peering**: 169.254.10.1 (AWS) ↔ 169.254.10.2 (GCP)

#### Step 5: Order GCP Partner Interconnect

**Using Equinix Fabric**:
```bash
# Create VLAN attachment in GCP
gcloud compute interconnects attachments create gcp-to-aws-attachment \
  --region=us-central1 \
  --router=gcp-to-aws-router \
  --interconnect=equinix-fabric \
  --vlan=100

# Output: Pairing key for Equinix
```

**Pairing Key**: `12345678-1234-1234-1234-123456789012/us-central1/1`

#### Step 6: Create Cloud Router in GCP

```bash
# Create Cloud Router
gcloud compute routers create gcp-to-aws-router \
  --network=default \
  --region=us-central1 \
  --asn=65000

# Add BGP peer
gcloud compute routers add-bgp-peer gcp-to-aws-router \
  --peer-name=aws-peer \
  --peer-asn=64512 \
  --interface=gcp-to-aws-attachment \
  --peer-ip-address=169.254.10.1 \
  --region=us-central1
```

#### Step 7: Configure Cross-Connect at Equinix

**Equinix Fabric Portal**:
1. Create connection from AWS Direct Connect port to GCP Partner Interconnect
2. Use AWS LOA-CFA and GCP Pairing Key
3. Select VLAN 100
4. Provision connection (takes 1-2 hours)

**Alternative: Megaport Portal**:
```bash
# Use Megaport Cloud Router (MCR)
# Create VXC (Virtual Cross Connect) from AWS to GCP
# Configure BGP on both sides
```

#### Step 8: Verify Connection

**AWS**:
```bash
# Check VIF status
aws directconnect describe-virtual-interfaces \
  --virtual-interface-id dxvif-55667788 \
  --query 'virtualInterfaces[0].virtualInterfaceState'

# Output: "available"

# Check BGP status
aws directconnect describe-virtual-interfaces \
  --virtual-interface-id dxvif-55667788 \
  --query 'virtualInterfaces[0].bgpPeers[0].bgpStatus'

# Output: "up"
```

**GCP**:
```bash
# Check attachment status
gcloud compute interconnects attachments describe gcp-to-aws-attachment \
  --region=us-central1 \
  --format="get(state)"

# Output: "ACTIVE"

# Check BGP session
gcloud compute routers get-status gcp-to-aws-router \
  --region=us-central1 \
  --format="get(result.bgpPeerStatus[0].status)"

# Output: "UP"
```

**Test Connectivity**:
```bash
# From AWS EC2 instance (10.0.1.10)
ping 10.1.1.10  # GCP VM private IP

# Check latency
ping -c 10 10.1.1.10
# Average: 5-10ms (vs 50-100ms over internet)
```

### Performance Characteristics

**Bandwidth**: 1 Gbps (can scale to 100 Gbps)
**Latency**: 5-10ms (consistent)
**Packet Loss**: <0.01%
**Jitter**: <1ms

**Monitoring**:
```bash
# AWS CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/DX \
  --metric-name ConnectionBpsEgress \
  --dimensions Name=ConnectionId,Value=dxcon-12345678 \
  --start-time 2024-01-15T00:00:00Z \
  --end-time 2024-01-15T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### Direct Connect Cost (AWS to GCP)

**AWS Direct Connect**:
- Port Hours (1 Gbps): $0.30/hr = $219/month
- Data Transfer Out: $0.02/GB (to Direct Connect location)

**GCP Partner Interconnect**:
- VLAN Attachment: $0.05/hr = $36.50/month
- Data Transfer Out: $0.05/GB (to Partner Interconnect)

**Equinix Fabric** (Partner):
- Connection: $100-300/month (varies by location)

**Example** (1 TB/month):
- AWS: $219 + (1000 × $0.02) = $239/month
- GCP: $36.50 + (1000 × $0.05) = $86.50/month
- Equinix: $200/month
- **Total: $525.50/month**

**Break-even vs VPN**: ~2 TB/month or when consistent low latency required

---

## AWS Direct Connect to Azure

### Architecture Overview
```
AWS VPC → Direct Connect → Equinix → ExpressRoute → Azure VNet
```

### Step-by-Step Setup

#### Step 1: Order AWS Direct Connect

```bash
# Create connection (same as AWS to GCP)
aws directconnect create-connection \
  --location EqDC2 \
  --bandwidth 1Gbps \
  --connection-name aws-to-azure-dx

# Create DX Gateway
aws directconnect create-direct-connect-gateway \
  --direct-connect-gateway-name aws-to-azure-dxgw \
  --amazon-side-asn 64512

# Create VGW and associate
aws ec2 create-vpn-gateway --type ipsec.1 --amazon-side-asn 64512
aws ec2 attach-vpn-gateway --vpn-gateway-id vgw-11223344 --vpc-id vpc-12345678
aws directconnect create-direct-connect-gateway-association \
  --direct-connect-gateway-id dxgw-87654321 \
  --gateway-id vgw-11223344

# Create private VIF
aws directconnect create-private-virtual-interface \
  --connection-id dxcon-12345678 \
  --new-private-virtual-interface '{
    "virtualInterfaceName": "aws-to-azure-vif",
    "vlan": 200,
    "asn": 12076,
    "amazonAddress": "169.254.20.1/30",
    "customerAddress": "169.254.20.2/30",
    "directConnectGatewayId": "dxgw-87654321"
  }'
```

**Note**: Azure uses ASN 12076 for ExpressRoute

#### Step 2: Order Azure ExpressRoute

```bash
# Create ExpressRoute circuit
az network express-route create \
  --resource-group myResourceGroup \
  --name azure-to-aws-circuit \
  --peering-location "Equinix-Washington-DC" \
  --bandwidth 1000 \
  --provider "Equinix" \
  --sku-family MeteredData \
  --sku-tier Standard

# Get service key
az network express-route show \
  --resource-group myResourceGroup \
  --name azure-to-aws-circuit \
  --query serviceKey \
  --output tsv

# Output: 12345678-1234-1234-1234-123456789012
```

**Service Key**: Provide to Equinix to provision circuit

#### Step 3: Configure Azure Private Peering

```bash
# Create private peering
az network express-route peering create \
  --resource-group myResourceGroup \
  --circuit-name azure-to-aws-circuit \
  --peering-type AzurePrivatePeering \
  --peer-asn 64512 \
  --primary-peer-subnet 169.254.20.0/30 \
  --secondary-peer-subnet 169.254.20.4/30 \
  --vlan-id 200

# Wait for peering to be provisioned
az network express-route peering show \
  --resource-group myResourceGroup \
  --circuit-name azure-to-aws-circuit \
  --name AzurePrivatePeering \
  --query provisioningState

# Output: "Succeeded"
```

#### Step 4: Create ExpressRoute Gateway

```bash
# Create public IP
az network public-ip create \
  --resource-group myResourceGroup \
  --name azure-er-gateway-ip \
  --allocation-method Dynamic

# Create ExpressRoute gateway (takes 30-45 minutes)
az network vnet-gateway create \
  --resource-group myResourceGroup \
  --name azure-er-gateway \
  --public-ip-address azure-er-gateway-ip \
  --vnet myVNet \
  --gateway-type ExpressRoute \
  --sku Standard \
  --no-wait
```

#### Step 5: Connect Gateway to Circuit

```bash
# Create connection
az network vpn-connection create \
  --resource-group myResourceGroup \
  --name azure-to-aws-connection \
  --vnet-gateway1 azure-er-gateway \
  --express-route-circuit2 azure-to-aws-circuit \
  --location eastus
```

#### Step 6: Configure Cross-Connect at Equinix

**Equinix Fabric Portal**:
1. Use AWS LOA-CFA and Azure Service Key
2. Create cross-connect between AWS and Azure ports
3. Configure VLAN 200 on both sides
4. Provision connection (1-2 hours)

#### Step 7: Verify Connection

**AWS**:
```bash
# Check BGP status
aws directconnect describe-virtual-interfaces \
  --virtual-interface-id dxvif-55667788 \
  --query 'virtualInterfaces[0].bgpPeers[0].bgpStatus'

# Output: "up"
```

**Azure**:
```bash
# Check circuit status
az network express-route show \
  --resource-group myResourceGroup \
  --name azure-to-aws-circuit \
  --query circuitProvisioningState

# Output: "Enabled"

# Check peering status
az network express-route peering show \
  --resource-group myResourceGroup \
  --circuit-name azure-to-aws-circuit \
  --name AzurePrivatePeering \
  --query peeringState

# Output: "Enabled"
```

**Test Connectivity**:
```bash
# From AWS EC2 instance
ping 10.2.1.10  # Azure VM private IP

# From Azure VM
ping 10.0.1.10  # AWS EC2 private IP
```

### ExpressRoute Cost (AWS to Azure)

**AWS Direct Connect**:
- Port Hours (1 Gbps): $0.30/hr = $219/month
- Data Transfer Out: $0.02/GB

**Azure ExpressRoute**:
- Circuit (1 Gbps, Metered): $0.025/hr = $18.25/month
- Data Transfer Out: $0.025/GB (first 10 TB)

**Equinix Fabric**:
- Connection: $100-300/month

**Example** (1 TB/month):
- AWS: $219 + (1000 × $0.02) = $239/month
- Azure: $18.25 + (1000 × $0.025) = $43.25/month
- Equinix: $200/month
- **Total: $482.25/month**

---

## Cloud Interconnect Partners

### Major Partners

#### 1. Equinix Fabric
**Coverage**: 60+ metros worldwide
**Bandwidth**: 50 Mbps - 10 Gbps
**Setup Time**: 1-2 weeks
**Cost**: $100-500/month per connection

**Features**:
- Direct connections to AWS, GCP, Azure
- Software-defined networking
- On-demand provisioning
- Pay-as-you-go pricing

#### 2. Megaport
**Coverage**: 700+ locations worldwide
**Bandwidth**: 1 Mbps - 10 Gbps
**Setup Time**: Minutes to hours
**Cost**: $50-300/month per connection

**Features**:
- Megaport Cloud Router (MCR) for multi-cloud
- Instant provisioning
- Flexible bandwidth (scale up/down)
- No long-term contracts

#### 3. PacketFabric
**Coverage**: 40+ markets
**Bandwidth**: 50 Mbps - 10 Gbps
**Setup Time**: Minutes
**Cost**: $100-400/month per connection

**Features**:
- Automated provisioning
- API-driven
- Multi-cloud connectivity
- Network-as-a-Service

#### 4. Console Connect (PCCW Global)
**Coverage**: 40+ countries
**Bandwidth**: 50 Mbps - 10 Gbps
**Setup Time**: Hours to days
**Cost**: $150-500/month per connection

### Partner Comparison

| Partner | Setup Time | Min Bandwidth | Locations | Best For |
|---------|-----------|---------------|-----------|----------|
| Equinix | 1-2 weeks | 50 Mbps | 60+ metros | Enterprise, global |
| Megaport | Minutes | 1 Mbps | 700+ | Flexible, on-demand |
| PacketFabric | Minutes | 50 Mbps | 40+ markets | Automation, API |
| Console Connect | Hours | 50 Mbps | 40+ countries | Asia-Pacific |

---

## Cost Comparison

### VPN vs Direct Connect (1 TB/month)

| Connection Type | Setup Time | Monthly Cost | Bandwidth | Latency |
|----------------|-----------|--------------|-----------|---------|
| **VPN (AWS-GCP)** | Hours | $283 | 1.25 Gbps | 50-100ms |
| **VPN (AWS-Azure)** | Hours | $352 | 1.25 Gbps | 50-100ms |
| **Direct Connect (AWS-GCP)** | 2-4 weeks | $526 | 1 Gbps | 5-10ms |
| **Direct Connect (AWS-Azure)** | 2-4 weeks | $482 | 1 Gbps | 5-10ms |

### Break-Even Analysis

**VPN vs Direct Connect**:
```
VPN Cost: $280 + ($0.09 + $0.12) × Data Transfer
Direct Connect Cost: $455 + ($0.02 + $0.05) × Data Transfer

Break-even: ~2 TB/month
```

**Recommendation**:
- **< 2 TB/month**: Use VPN
- **> 2 TB/month**: Use Direct Connect
- **Mission-critical**: Use Direct Connect regardless of data volume

---

## Summary - Part 2

**Direct Connect** provides high-bandwidth, low-latency connectivity:
- **Setup Time**: 2-4 weeks
- **Bandwidth**: 1-100 Gbps
- **Latency**: 5-10ms (consistent)
- **Cost**: $480-530/month (1 Gbps + 1 TB data)
- **Use Case**: High bandwidth, mission-critical workloads

**Key Takeaways**:
1. Direct Connect for high bandwidth and low latency
2. Use cloud interconnect partners (Equinix, Megaport) for multi-cloud
3. Plan for 2-4 weeks provisioning time
4. Break-even at ~2 TB/month vs VPN
5. Consider redundant connections for high availability
6. Monitor BGP sessions and bandwidth utilization

**Next**: Part 3 covers SD-WAN, Service Mesh, and Application-Level Integration.
