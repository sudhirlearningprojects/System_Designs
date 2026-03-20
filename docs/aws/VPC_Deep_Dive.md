# AWS VPC (Virtual Private Cloud) - Deep Dive

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Subnets](#subnets)
4. [Route Tables](#route-tables)
5. [Internet Gateway](#internet-gateway)
6. [NAT Gateway](#nat-gateway)
7. [Security](#security)
8. [VPC Peering](#vpc-peering)
9. [VPN & Direct Connect](#vpn--direct-connect)
10. [Best Practices](#best-practices)

---

## Overview

**AWS VPC** is a logically isolated virtual network where you launch AWS resources. You have complete control over IP addressing, subnets, routing, and security.

### Key Benefits
- **Isolation**: Logically isolated from other VPCs
- **Control**: Full control over network configuration
- **Security**: Multiple layers of security (Security Groups, NACLs)
- **Connectivity**: Connect to on-premises networks via VPN/Direct Connect
- **Scalability**: Support for thousands of instances

### VPC Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                    VPC (10.0.0.0/16)                             │
│                                                                   │
│  ┌────────────────────────────┐  ┌────────────────────────────┐ │
│  │  Public Subnet (AZ-A)      │  │  Public Subnet (AZ-B)      │ │
│  │  10.0.1.0/24               │  │  10.0.2.0/24               │ │
│  │  ┌──────┐  ┌──────┐        │  │  ┌──────┐  ┌──────┐        │ │
│  │  │ EC2  │  │ ALB  │        │  │  │ EC2  │  │ ALB  │        │ │
│  │  └──────┘  └──────┘        │  │  └──────┘  └──────┘        │ │
│  └────────────────────────────┘  └────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────┐  ┌────────────────────────────┐ │
│  │  Private Subnet (AZ-A)     │  │  Private Subnet (AZ-B)     │ │
│  │  10.0.11.0/24              │  │  10.0.12.0/24              │ │
│  │  ┌──────┐  ┌──────┐        │  │  ┌──────┐  ┌──────┐        │ │
│  │  │ EC2  │  │ RDS  │        │  │  │ EC2  │  │ RDS  │        │ │
│  │  └──────┘  └──────┘        │  │  └──────┘  └──────┘        │ │
│  └────────────────────────────┘  └────────────────────────────┘ │
│                                                                   │
│  ┌──────────────────┐                                            │
│  │ Internet Gateway │                                            │
│  └──────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Concepts

### 1. CIDR Blocks
**Classless Inter-Domain Routing** defines IP address ranges.

**CIDR Notation**: `10.0.0.0/16`
- `10.0.0.0` = Network address
- `/16` = Subnet mask (first 16 bits are network, last 16 bits are hosts)

**CIDR Calculation**:
```
/16 = 65,536 IP addresses (2^16)
/24 = 256 IP addresses (2^8)
/28 = 16 IP addresses (2^4)
```

**AWS Reserved IPs** (per subnet):
- `.0` = Network address
- `.1` = VPC router
- `.2` = DNS server
- `.3` = Reserved for future use
- `.255` = Broadcast (not supported but reserved)

**Example**: `10.0.1.0/24` subnet
- Total IPs: 256
- Usable IPs: 251 (256 - 5 reserved)
- Range: `10.0.1.0` - `10.0.1.255`
- Usable: `10.0.1.4` - `10.0.1.254`

### 2. VPC Limits
- **VPCs per region**: 5 (default, can increase to 100+)
- **Subnets per VPC**: 200
- **CIDR blocks per VPC**: 5 (primary + 4 secondary)
- **Elastic IPs per region**: 5 (can request increase)
- **Internet Gateways per VPC**: 1
- **NAT Gateways per AZ**: No limit (but consider cost)

### 3. Default VPC
Every AWS account has a **default VPC** in each region:
- CIDR: `172.31.0.0/16`
- Default subnet in each AZ
- Internet Gateway attached
- Default Security Group and NACL

**Best Practice**: Don't use default VPC for production; create custom VPCs.

---

## Subnets

### Public vs Private Subnets

**Public Subnet**:
- Has route to Internet Gateway
- Instances can have public IPs
- Use case: Web servers, load balancers

**Private Subnet**:
- No direct route to Internet Gateway
- Instances use NAT Gateway for outbound internet
- Use case: Application servers, databases

### Subnet Design Example

**VPC**: `10.0.0.0/16` (65,536 IPs)

| Subnet Type | AZ | CIDR | IPs | Purpose |
|-------------|-----|------|-----|---------|
| Public | us-east-1a | 10.0.1.0/24 | 251 | Web tier |
| Public | us-east-1b | 10.0.2.0/24 | 251 | Web tier |
| Private | us-east-1a | 10.0.11.0/24 | 251 | App tier |
| Private | us-east-1b | 10.0.12.0/24 | 251 | App tier |
| Private | us-east-1a | 10.0.21.0/24 | 251 | Database |
| Private | us-east-1b | 10.0.22.0/24 | 251 | Database |

**Create Subnet**:
```bash
aws ec2 create-subnet \
  --vpc-id vpc-12345678 \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=public-subnet-1a}]'
```

---

## Route Tables

**Route tables** control traffic routing within VPC.

### Main Route Table
Every VPC has a **main route table** (default for all subnets).

**Default routes**:
```
Destination       Target
10.0.0.0/16       local
```

### Custom Route Tables

**Public Subnet Route Table**:
```
Destination       Target
10.0.0.0/16       local
0.0.0.0/0         igw-12345678
```

**Private Subnet Route Table**:
```
Destination       Target
10.0.0.0/16       local
0.0.0.0/0         nat-12345678
```

**Create Route Table**:
```bash
# Create route table
aws ec2 create-route-table --vpc-id vpc-12345678

# Add route to Internet Gateway
aws ec2 create-route \
  --route-table-id rtb-12345678 \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id igw-12345678

# Associate with subnet
aws ec2 associate-route-table \
  --route-table-id rtb-12345678 \
  --subnet-id subnet-12345678
```

---

## Internet Gateway

**Internet Gateway (IGW)** enables communication between VPC and internet.

### Characteristics
- **Horizontally scaled**: Redundant and highly available
- **No bandwidth constraints**: Scales automatically
- **One per VPC**: Only one IGW can be attached
- **No cost**: Free to use

### How It Works
```
Internet
   │
   ▼
┌──────────────────┐
│ Internet Gateway │
└────────┬─────────┘
         │
    ┌────▼────┐
    │   VPC   │
    └─────────┘
```

**Create and Attach IGW**:
```bash
# Create IGW
aws ec2 create-internet-gateway

# Attach to VPC
aws ec2 attach-internet-gateway \
  --internet-gateway-id igw-12345678 \
  --vpc-id vpc-12345678
```

---

## NAT Gateway

**NAT Gateway** enables private subnet instances to access internet (outbound only).

### NAT Gateway vs NAT Instance

| Feature | NAT Gateway | NAT Instance |
|---------|-------------|--------------|
| Availability | Highly available within AZ | Manual failover |
| Bandwidth | Up to 100 Gbps | Depends on instance type |
| Maintenance | Managed by AWS | You manage |
| Cost | $0.045/hr + $0.045/GB | EC2 instance cost |
| Security Groups | No | Yes |
| Bastion Server | No | Can be used as bastion |

### NAT Gateway Architecture
```
┌─────────────────────────────────────────┐
│              VPC                         │
│                                          │
│  ┌────────────────┐  ┌────────────────┐ │
│  │ Public Subnet  │  │ Private Subnet │ │
│  │                │  │                │ │
│  │  ┌──────────┐  │  │  ┌──────────┐  │ │
│  │  │   NAT    │◄─┼──┼──│   EC2    │  │ │
│  │  │ Gateway  │  │  │  │ Instance │  │ │
│  │  └────┬─────┘  │  │  └──────────┘  │ │
│  └───────┼────────┘  └────────────────┘ │
│          │                               │
│     ┌────▼─────┐                         │
│     │   IGW    │                         │
│     └────┬─────┘                         │
└──────────┼───────────────────────────────┘
           │
           ▼
       Internet
```

**Create NAT Gateway**:
```bash
# Allocate Elastic IP
aws ec2 allocate-address --domain vpc

# Create NAT Gateway in public subnet
aws ec2 create-nat-gateway \
  --subnet-id subnet-12345678 \
  --allocation-id eipalloc-12345678

# Add route in private subnet route table
aws ec2 create-route \
  --route-table-id rtb-87654321 \
  --destination-cidr-block 0.0.0.0/0 \
  --nat-gateway-id nat-12345678
```

**High Availability**: Deploy NAT Gateway in each AZ.

**Cost Example**:
- NAT Gateway: $0.045/hr = $32.85/month
- Data processing: $0.045/GB
- Total (1 TB/month): $32.85 + $45 = $77.85/month

---

## Security

### 1. Security Groups (Stateful)

**Characteristics**:
- **Stateful**: Return traffic automatically allowed
- **Instance-level**: Applied to ENI
- **Allow rules only**: Cannot create deny rules
- **All rules evaluated**: Not processed in order

**Example**:
```bash
# Create security group
aws ec2 create-security-group \
  --group-name web-sg \
  --description "Web server security group" \
  --vpc-id vpc-12345678

# Allow HTTP
aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow HTTPS
aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# Allow SSH from specific IP
aws ec2 authorize-security-group-ingress \
  --group-id sg-12345678 \
  --protocol tcp \
  --port 22 \
  --cidr 203.0.113.0/24
```

### 2. Network ACLs (Stateless)

**Characteristics**:
- **Stateless**: Must explicitly allow return traffic
- **Subnet-level**: Applied to entire subnet
- **Allow and Deny rules**: Can create both
- **Processed in order**: Lowest rule number first

**Default NACL**: Allows all inbound and outbound traffic.

**Custom NACL Example**:
```
Rule # | Type  | Protocol | Port | Source      | Allow/Deny
-------|-------|----------|------|-------------|------------
100    | HTTP  | TCP      | 80   | 0.0.0.0/0   | ALLOW
110    | HTTPS | TCP      | 443  | 0.0.0.0/0   | ALLOW
120    | SSH   | TCP      | 22   | 10.0.0.0/16 | ALLOW
*      | ALL   | ALL      | ALL  | 0.0.0.0/0   | DENY
```

**Create NACL**:
```bash
# Create NACL
aws ec2 create-network-acl --vpc-id vpc-12345678

# Add inbound rule
aws ec2 create-network-acl-entry \
  --network-acl-id acl-12345678 \
  --ingress \
  --rule-number 100 \
  --protocol tcp \
  --port-range From=80,To=80 \
  --cidr-block 0.0.0.0/0 \
  --rule-action allow

# Add outbound rule
aws ec2 create-network-acl-entry \
  --network-acl-id acl-12345678 \
  --egress \
  --rule-number 100 \
  --protocol tcp \
  --port-range From=1024,To=65535 \
  --cidr-block 0.0.0.0/0 \
  --rule-action allow
```

### Security Groups vs NACLs

| Feature | Security Group | Network ACL |
|---------|----------------|-------------|
| Level | Instance (ENI) | Subnet |
| State | Stateful | Stateless |
| Rules | Allow only | Allow and Deny |
| Processing | All rules | Ordered (lowest first) |
| Default | Deny all inbound | Allow all |

---

## VPC Peering

**VPC Peering** connects two VPCs privately using AWS network.

### Characteristics
- **Non-transitive**: A↔B and B↔C doesn't mean A↔C
- **No overlapping CIDRs**: VPCs must have different IP ranges
- **Cross-region**: Can peer VPCs in different regions
- **Cross-account**: Can peer VPCs in different AWS accounts

### VPC Peering Architecture
```
┌─────────────────┐         ┌─────────────────┐
│   VPC A         │         │   VPC B         │
│  10.0.0.0/16    │◄───────►│  10.1.0.0/16    │
│                 │ Peering │                 │
└─────────────────┘         └─────────────────┘
```

**Create VPC Peering**:
```bash
# Create peering connection
aws ec2 create-vpc-peering-connection \
  --vpc-id vpc-12345678 \
  --peer-vpc-id vpc-87654321

# Accept peering connection
aws ec2 accept-vpc-peering-connection \
  --vpc-peering-connection-id pcx-12345678

# Add routes in both VPCs
aws ec2 create-route \
  --route-table-id rtb-12345678 \
  --destination-cidr-block 10.1.0.0/16 \
  --vpc-peering-connection-id pcx-12345678
```

---

## VPN & Direct Connect

### 1. Site-to-Site VPN
Connect on-premises network to VPC over internet.

**Components**:
- **Virtual Private Gateway (VGW)**: VPN endpoint on AWS side
- **Customer Gateway (CGW)**: VPN endpoint on customer side
- **VPN Connection**: IPsec tunnel

**Characteristics**:
- **Bandwidth**: Up to 1.25 Gbps per tunnel
- **Latency**: Internet latency (variable)
- **Cost**: $0.05/hr per VPN connection + data transfer
- **Setup time**: Minutes

### 2. AWS Direct Connect
Dedicated network connection from on-premises to AWS.

**Characteristics**:
- **Bandwidth**: 1 Gbps, 10 Gbps, 100 Gbps
- **Latency**: Consistent, low latency
- **Cost**: Port hours + data transfer out
- **Setup time**: Weeks to months

**Direct Connect Pricing** (us-east-1):
- 1 Gbps port: $0.30/hr = $219/month
- 10 Gbps port: $2.25/hr = $1,642/month
- Data transfer out: $0.02/GB

---

## Best Practices

### 1. CIDR Planning
- Use RFC 1918 private ranges: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
- Plan for growth: Use `/16` for VPC (65K IPs)
- Reserve IP space for future VPC peering
- Use consistent CIDR scheme across environments

### 2. Multi-AZ Design
- Deploy subnets in at least 2 AZs
- Use separate route tables per AZ for private subnets
- Deploy NAT Gateway in each AZ for high availability

### 3. Security Layers
- Use Security Groups as primary firewall
- Use NACLs as secondary defense layer
- Principle of least privilege
- Separate security groups by tier (web, app, database)

### 4. Cost Optimization
- Use VPC endpoints for S3/DynamoDB (free, no NAT Gateway cost)
- Consider NAT instance for low-traffic workloads
- Use VPC peering instead of VPN when possible
- Monitor data transfer costs

### 5. Monitoring
- Enable VPC Flow Logs for traffic analysis
- Monitor NAT Gateway metrics (bytes, packets, errors)
- Set CloudWatch alarms for unusual traffic patterns
- Use AWS Network Firewall for advanced threat protection

---

## Summary

**AWS VPC** provides complete control over your virtual network:
- **Isolation**: Logically isolated network
- **Flexibility**: Custom IP ranges, subnets, routing
- **Security**: Multiple layers (Security Groups, NACLs)
- **Connectivity**: Internet Gateway, NAT Gateway, VPN, Direct Connect
- **Scalability**: Support for thousands of instances

**Key Takeaways**:
1. Plan CIDR blocks carefully (no overlap, room for growth)
2. Use public subnets for internet-facing resources
3. Use private subnets for internal resources
4. Deploy across multiple AZs for high availability
5. Use Security Groups as primary firewall
6. Enable VPC Flow Logs for monitoring
7. Use VPC endpoints to reduce NAT Gateway costs

**Next**: Learn about Auto Scaling, ECS, and EKS for container orchestration.
