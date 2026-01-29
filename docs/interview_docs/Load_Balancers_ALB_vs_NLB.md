# Load Balancers - Deep Dive (ALB vs NLB)

## Overview

A Load Balancer distributes incoming network traffic across multiple servers to ensure no single server becomes overwhelmed, improving application availability, scalability, and fault tolerance.

**Key Benefits**:
- High availability and fault tolerance
- Horizontal scalability
- Better resource utilization
- Reduced response time
- Health monitoring and automatic failover

---

## Load Balancer Architecture

```
                    Internet
                       ↓
              ┌────────────────┐
              │ Load Balancer  │
              └────────────────┘
                       ↓
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
   ┌────────┐     ┌────────┐     ┌────────┐
   │Server 1│     │Server 2│     │Server 3│
   └────────┘     └────────┘     └────────┘
```

---

## Load Balancing Algorithms

### 1. Round Robin

Distributes requests sequentially across servers.

```
Request 1 → Server 1
Request 2 → Server 2
Request 3 → Server 3
Request 4 → Server 1 (repeat)
```

**Pros**: Simple, fair distribution  
**Cons**: Doesn't consider server load or capacity

### 2. Least Connections

Routes to server with fewest active connections.

```
Server 1: 10 connections
Server 2: 5 connections  ← Route here
Server 3: 8 connections
```

**Pros**: Better for long-lived connections  
**Cons**: Requires tracking connection state

### 3. Weighted Round Robin

Assigns weights based on server capacity.

```
Server 1 (weight=3): Gets 3 requests
Server 2 (weight=2): Gets 2 requests
Server 3 (weight=1): Gets 1 request
```

**Pros**: Handles heterogeneous servers  
**Cons**: Static weights don't adapt to load

### 4. IP Hash

Routes based on client IP address.

```
hash(client_ip) % num_servers
Same client always goes to same server
```

**Pros**: Session persistence without cookies  
**Cons**: Uneven distribution if few clients

### 5. Least Response Time

Routes to server with lowest response time.

```
Server 1: 50ms average
Server 2: 30ms average ← Route here
Server 3: 45ms average
```

**Pros**: Best performance  
**Cons**: Complex to implement

---

## OSI Model and Load Balancing Layers

```
Layer 7 (Application)  → HTTP/HTTPS, WebSocket
Layer 6 (Presentation) → SSL/TLS
Layer 5 (Session)      → Session management
Layer 4 (Transport)    → TCP/UDP
Layer 3 (Network)      → IP routing
Layer 2 (Data Link)    → MAC addresses
Layer 1 (Physical)     → Physical cables
```

**Layer 4 (L4) Load Balancing**: Transport layer (TCP/UDP)  
**Layer 7 (L7) Load Balancing**: Application layer (HTTP/HTTPS)

---

## AWS Load Balancer Types

### 1. Application Load Balancer (ALB) - Layer 7

### 2. Network Load Balancer (NLB) - Layer 4

### 3. Classic Load Balancer (CLB) - Legacy (Layer 4 & 7)

### 4. Gateway Load Balancer (GWLB) - Layer 3

---

## Application Load Balancer (ALB)

### Overview

ALB operates at Layer 7 (Application layer) and makes routing decisions based on HTTP/HTTPS content.

### Architecture

```
                    Client
                      ↓
              ┌──────────────┐
              │     ALB      │
              │  (Layer 7)   │
              └──────────────┘
                      ↓
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
   Target Group 1  Target Group 2  Target Group 3
   /api/*          /images/*       /admin/*
        ↓             ↓             ↓
   EC2 Instances  EC2 Instances  Lambda Functions
```

### Key Features

**1. Content-Based Routing**

Route based on URL path, hostname, headers, query strings:

```
# Path-based routing
/api/*     → API servers
/images/*  → Image servers
/admin/*   → Admin servers

# Host-based routing
api.example.com   → API servers
www.example.com   → Web servers
admin.example.com → Admin servers

# Header-based routing
User-Agent: Mobile → Mobile servers
User-Agent: Desktop → Desktop servers

# Query string routing
?version=v2 → New version servers
?version=v1 → Old version servers
```

**2. HTTP/HTTPS Support**

```
Client → HTTPS → ALB → HTTP → Backend Servers
         (SSL termination at ALB)
```

**3. WebSocket Support**

```
Client ←→ WebSocket ←→ ALB ←→ WebSocket ←→ Server
```

**4. HTTP/2 and gRPC Support**

```
Client → HTTP/2 → ALB → HTTP/1.1 → Backend
```

**5. Request/Response Modification**

```
# Add custom headers
X-Forwarded-For: client_ip
X-Forwarded-Proto: https
X-Forwarded-Port: 443

# Redirect HTTP to HTTPS
http://example.com → https://example.com
```

**6. Authentication Integration**

```
ALB → Amazon Cognito → User Pool
ALB → OIDC Provider (Google, Facebook)
```

**7. Fixed Response**

```
# Return custom response without backend
GET /maintenance → 503 Service Unavailable
```

### ALB Configuration Example

```yaml
# ALB with path-based routing
LoadBalancer:
  Type: application
  Scheme: internet-facing
  Subnets:
    - subnet-1a
    - subnet-1b
  SecurityGroups:
    - sg-alb

Listeners:
  - Port: 443
    Protocol: HTTPS
    DefaultActions:
      - Type: forward
        TargetGroupArn: !Ref WebTargetGroup
    Rules:
      - Priority: 1
        Conditions:
          - Field: path-pattern
            Values: ['/api/*']
        Actions:
          - Type: forward
            TargetGroupArn: !Ref APITargetGroup
      
      - Priority: 2
        Conditions:
          - Field: host-header
            Values: ['admin.example.com']
        Actions:
          - Type: forward
            TargetGroupArn: !Ref AdminTargetGroup

TargetGroups:
  WebTargetGroup:
    Protocol: HTTP
    Port: 80
    HealthCheck:
      Path: /health
      Interval: 30
      Timeout: 5
      HealthyThreshold: 2
      UnhealthyThreshold: 3
```

### ALB Use Cases

✅ **Microservices Architecture**
```
/users/*    → User Service
/orders/*   → Order Service
/payments/* → Payment Service
```

✅ **Multi-Tenant Applications**
```
tenant1.app.com → Tenant 1 servers
tenant2.app.com → Tenant 2 servers
```

✅ **A/B Testing**
```
90% traffic → Version A
10% traffic → Version B
```

✅ **Blue-Green Deployment**
```
Initially: 100% → Blue environment
Gradually: 50% → Blue, 50% → Green
Finally: 100% → Green environment
```

✅ **API Gateway Alternative**
```
/v1/api/* → Legacy API
/v2/api/* → New API
```

### ALB Pricing

- **Per hour**: ~$0.0225/hour
- **Per LCU** (Load Balancer Capacity Unit): ~$0.008/hour
  - LCU = max(new connections/sec, active connections, bandwidth, rule evaluations)

**Example Cost**:
```
1 ALB running 24/7 for 1 month:
- Hourly: $0.0225 × 24 × 30 = $16.20
- LCU: $0.008 × 24 × 30 × 10 LCU = $57.60
Total: ~$73.80/month
```

---

## Network Load Balancer (NLB)

### Overview

NLB operates at Layer 4 (Transport layer) and makes routing decisions based on TCP/UDP protocols and IP addresses.

### Architecture

```
                    Client
                      ↓
              ┌──────────────┐
              │     NLB      │
              │  (Layer 4)   │
              └──────────────┘
                      ↓
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
   Target Group 1  Target Group 2  Target Group 3
   Port 80         Port 443        Port 3306
        ↓             ↓             ↓
   Web Servers    API Servers    Database Servers
```

### Key Features

**1. Ultra-Low Latency**

```
ALB: ~100-400ms latency
NLB: <1ms latency (sub-millisecond)
```

**2. Static IP Address**

```
NLB provides static IP per AZ:
- 52.1.2.3 (us-east-1a)
- 52.1.2.4 (us-east-1b)

Benefits:
- Whitelist in firewalls
- DNS A records
- Client-side caching
```

**3. Elastic IP Support**

```
Assign your own Elastic IPs:
- 203.0.113.1 (your IP)
- 203.0.113.2 (your IP)
```

**4. Preserve Source IP**

```
Client IP: 203.0.113.50
         ↓
       NLB (preserves IP)
         ↓
Backend sees: 203.0.113.50 (original client IP)

vs ALB:
Backend sees: 10.0.1.5 (ALB IP)
Client IP in X-Forwarded-For header
```

**5. TCP/UDP/TLS Support**

```
- TCP: Port 80, 443, 3306, 5432
- UDP: Port 53 (DNS), 123 (NTP)
- TLS: SSL/TLS termination
```

**6. Millions of Requests per Second**

```
NLB: Handles millions of requests/sec
ALB: Handles thousands of requests/sec
```

**7. Connection Draining**

```
Server marked unhealthy:
- Existing connections: Continue (up to 3600s)
- New connections: Route to healthy servers
```

### NLB Configuration Example

```yaml
# NLB with TCP routing
LoadBalancer:
  Type: network
  Scheme: internet-facing
  Subnets:
    - subnet-1a
    - subnet-1b
  IpAddressType: ipv4

Listeners:
  - Port: 80
    Protocol: TCP
    DefaultActions:
      - Type: forward
        TargetGroupArn: !Ref WebTargetGroup
  
  - Port: 443
    Protocol: TLS
    Certificates:
      - CertificateArn: !Ref SSLCertificate
    DefaultActions:
      - Type: forward
        TargetGroupArn: !Ref WebTargetGroup

TargetGroups:
  WebTargetGroup:
    Protocol: TCP
    Port: 80
    HealthCheck:
      Protocol: TCP
      Port: 80
      Interval: 30
      HealthyThreshold: 3
      UnhealthyThreshold: 3
    DeregistrationDelay: 300
```

### NLB Use Cases

✅ **High-Performance Applications**
```
Gaming servers (low latency critical)
Financial trading platforms
Real-time bidding systems
```

✅ **Static IP Requirements**
```
Third-party integrations requiring IP whitelisting
Legacy systems with hardcoded IPs
Compliance requirements
```

✅ **Non-HTTP Protocols**
```
TCP: Database connections (MySQL, PostgreSQL)
UDP: DNS servers, VoIP, gaming
Custom protocols: MQTT, AMQP
```

✅ **Extreme Scale**
```
Millions of requests per second
Sudden traffic spikes
DDoS protection
```

✅ **Preserve Source IP**
```
Security logging
Geo-location services
IP-based access control
```

### NLB Pricing

- **Per hour**: ~$0.0225/hour
- **Per NLCU** (Network Load Balancer Capacity Unit): ~$0.006/hour
  - NLCU = max(new connections/sec, active connections, bandwidth)

**Example Cost**:
```
1 NLB running 24/7 for 1 month:
- Hourly: $0.0225 × 24 × 30 = $16.20
- NLCU: $0.006 × 24 × 30 × 10 NLCU = $43.20
Total: ~$59.40/month
```

---

## ALB vs NLB - Detailed Comparison

| Feature | ALB (Layer 7) | NLB (Layer 4) |
|---------|---------------|---------------|
| **OSI Layer** | Layer 7 (Application) | Layer 4 (Transport) |
| **Protocols** | HTTP, HTTPS, WebSocket, gRPC | TCP, UDP, TLS |
| **Routing** | Path, host, header, query string | IP address, port |
| **Latency** | ~100-400ms | <1ms (sub-millisecond) |
| **Throughput** | Thousands of requests/sec | Millions of requests/sec |
| **Static IP** | ❌ No | ✅ Yes |
| **Elastic IP** | ❌ No | ✅ Yes |
| **Preserve Source IP** | ❌ No (X-Forwarded-For) | ✅ Yes |
| **SSL Termination** | ✅ Yes | ✅ Yes (TLS listener) |
| **WebSocket** | ✅ Yes | ✅ Yes |
| **HTTP/2** | ✅ Yes | ❌ No |
| **Content-Based Routing** | ✅ Yes | ❌ No |
| **Authentication** | ✅ Yes (Cognito, OIDC) | ❌ No |
| **WAF Integration** | ✅ Yes | ❌ No |
| **Fixed Response** | ✅ Yes | ❌ No |
| **Health Checks** | HTTP/HTTPS | TCP/HTTP/HTTPS |
| **Target Types** | EC2, IP, Lambda, ECS | EC2, IP, ECS |
| **Cross-Zone LB** | Always enabled (free) | Optional (charged) |
| **Use Case** | Web apps, microservices, APIs | High performance, static IP, non-HTTP |
| **Cost** | ~$73.80/month | ~$59.40/month |

---

## When to Use ALB vs NLB

### Use ALB When:

✅ **HTTP/HTTPS Applications**
```
Web applications
REST APIs
Microservices
```

✅ **Content-Based Routing Needed**
```
Route /api/* to API servers
Route /images/* to CDN
Route by hostname or headers
```

✅ **Microservices Architecture**
```
Multiple services behind single load balancer
Path-based routing to different services
```

✅ **Authentication Required**
```
Integrate with Cognito
OIDC authentication
```

✅ **WAF Protection Needed**
```
SQL injection protection
XSS protection
Rate limiting
```

✅ **Lambda Functions as Targets**
```
Serverless applications
Event-driven architecture
```

✅ **HTTP/2 or gRPC**
```
Modern web applications
gRPC microservices
```

### Use NLB When:

✅ **Ultra-Low Latency Required**
```
Gaming servers (<1ms latency)
Financial trading platforms
Real-time bidding systems
```

✅ **Static IP Address Needed**
```
Third-party IP whitelisting
Firewall rules
DNS A records
```

✅ **Non-HTTP Protocols**
```
TCP: Database connections (port 3306, 5432)
UDP: DNS (port 53), VoIP, gaming
Custom protocols: MQTT, AMQP
```

✅ **Extreme Scale**
```
Millions of requests per second
Sudden traffic spikes
DDoS protection
```

✅ **Preserve Source IP**
```
Security logging with real client IP
Geo-location services
IP-based access control
```

✅ **Long-Lived TCP Connections**
```
WebSocket connections
Database connections
Streaming protocols
```

---

## Real-World Architecture Examples

### Example 1: E-Commerce Platform (ALB)

```
                    Internet
                       ↓
              ┌────────────────┐
              │      ALB       │
              └────────────────┘
                       ↓
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
   /products/*    /checkout/*    /admin/*
        ↓              ↓              ↓
   Product Svc    Payment Svc    Admin Svc
```

**Why ALB?**
- Path-based routing to microservices
- HTTPS termination
- WAF for security
- Authentication for admin panel

### Example 2: Gaming Platform (NLB)

```
                    Internet
                       ↓
              ┌────────────────┐
              │      NLB       │
              │  Static IP     │
              └────────────────┘
                       ↓
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
   Game Server 1  Game Server 2  Game Server 3
   (TCP/UDP)      (TCP/UDP)      (TCP/UDP)
```

**Why NLB?**
- Sub-millisecond latency
- UDP support for game traffic
- Static IP for player connections
- Preserve source IP for anti-cheat

### Example 3: Hybrid Architecture (ALB + NLB)

```
                    Internet
                       ↓
              ┌────────────────┐
              │      ALB       │  ← Web traffic (HTTP/HTTPS)
              └────────────────┘
                       ↓
              Web Application Servers
                       
                    Internet
                       ↓
              ┌────────────────┐
              │      NLB       │  ← Game traffic (TCP/UDP)
              └────────────────┘
                       ↓
              Game Servers
```

**Why Both?**
- ALB for web dashboard (HTTP)
- NLB for game servers (TCP/UDP)
- Best of both worlds

### Example 4: Database Cluster (NLB)

```
                Applications
                       ↓
              ┌────────────────┐
              │      NLB       │
              │   Port 5432    │
              └────────────────┘
                       ↓
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
   PostgreSQL 1   PostgreSQL 2   PostgreSQL 3
   (Read Replica) (Read Replica) (Read Replica)
```

**Why NLB?**
- TCP protocol (port 5432)
- Preserve source IP for logging
- Low latency for queries
- Static IP for connection strings

---

## Health Checks

### ALB Health Checks

```yaml
HealthCheck:
  Protocol: HTTP
  Path: /health
  Port: 80
  Interval: 30          # Check every 30 seconds
  Timeout: 5            # Wait 5 seconds for response
  HealthyThreshold: 2   # 2 successful checks = healthy
  UnhealthyThreshold: 3 # 3 failed checks = unhealthy
  Matcher: 200          # Expected HTTP status code
```

**Health Check Response**:
```json
GET /health
Response: 200 OK
{
  "status": "healthy",
  "database": "connected",
  "cache": "connected"
}
```

### NLB Health Checks

```yaml
HealthCheck:
  Protocol: TCP         # or HTTP/HTTPS
  Port: 80
  Interval: 30
  HealthyThreshold: 3
  UnhealthyThreshold: 3
```

**TCP Health Check**:
```
NLB → TCP SYN → Server
Server → TCP SYN-ACK → NLB
NLB → TCP ACK → Server
Result: Healthy
```

---

## SSL/TLS Termination

### ALB SSL Termination

```
Client → HTTPS (443) → ALB → HTTP (80) → Backend
         [Encrypted]         [Unencrypted]
```

**Benefits**:
- Offload SSL processing from backend
- Centralized certificate management
- SNI support (multiple certificates)

### NLB TLS Termination

```
Client → TLS (443) → NLB → TCP (80) → Backend
         [Encrypted]       [Unencrypted]
```

**Configuration**:
```yaml
Listener:
  Port: 443
  Protocol: TLS
  Certificates:
    - CertificateArn: arn:aws:acm:us-east-1:123456789012:certificate/abc123
  DefaultActions:
    - Type: forward
      TargetGroupArn: !Ref TargetGroup
```

---

## Cross-Zone Load Balancing

### Without Cross-Zone

```
AZ-1: 2 instances → 50% traffic each
AZ-2: 4 instances → 12.5% traffic each

Uneven distribution!
```

### With Cross-Zone

```
AZ-1: 2 instances → 16.67% traffic each
AZ-2: 4 instances → 16.67% traffic each

Even distribution!
```

**ALB**: Cross-zone always enabled (free)  
**NLB**: Cross-zone optional (charged for data transfer)

---

## Connection Draining / Deregistration Delay

```
Server marked unhealthy:
  ↓
Stop sending new requests
  ↓
Wait for existing connections to complete (0-3600 seconds)
  ↓
Remove server from target group
```

**Configuration**:
```yaml
TargetGroup:
  DeregistrationDelay: 300  # Wait 5 minutes
```

---

## Monitoring and Metrics

### ALB Metrics (CloudWatch)

```
- RequestCount: Total requests
- TargetResponseTime: Backend response time
- HTTPCode_Target_2XX_Count: Successful responses
- HTTPCode_Target_4XX_Count: Client errors
- HTTPCode_Target_5XX_Count: Server errors
- UnHealthyHostCount: Unhealthy targets
- ActiveConnectionCount: Active connections
```

### NLB Metrics (CloudWatch)

```
- ActiveFlowCount: Active TCP connections
- NewFlowCount: New connections per second
- ProcessedBytes: Total bytes processed
- TCP_Client_Reset_Count: Client resets
- TCP_Target_Reset_Count: Target resets
- UnHealthyHostCount: Unhealthy targets
```

---

## Best Practices

### ALB Best Practices

1. **Use Path-Based Routing**
```
/api/* → API servers
/static/* → Static content servers
```

2. **Enable Access Logs**
```
s3://my-bucket/alb-logs/
```

3. **Use Target Group Health Checks**
```
GET /health → 200 OK
```

4. **Enable WAF for Security**
```
Block SQL injection
Block XSS attacks
Rate limiting
```

5. **Use HTTPS with ACM Certificates**
```
Free SSL certificates from AWS Certificate Manager
```

### NLB Best Practices

1. **Use Static IPs for Whitelisting**
```
Elastic IP: 203.0.113.1
```

2. **Enable Cross-Zone Load Balancing**
```
Even distribution across AZs
```

3. **Configure Appropriate Health Checks**
```
TCP health checks for non-HTTP
```

4. **Use Connection Draining**
```
DeregistrationDelay: 300 seconds
```

5. **Monitor Connection Metrics**
```
ActiveFlowCount
NewFlowCount
```

---

## Interview Questions & Answers

### Q1: What's the main difference between ALB and NLB?

**Answer**: 
- **ALB** operates at Layer 7 (Application) and routes based on HTTP content (path, headers)
- **NLB** operates at Layer 4 (Transport) and routes based on TCP/UDP and IP addresses

### Q2: When would you choose NLB over ALB?

**Answer**:
- Need ultra-low latency (<1ms)
- Require static IP addresses
- Non-HTTP protocols (TCP/UDP)
- Millions of requests per second
- Need to preserve source IP

### Q3: Can ALB handle WebSocket connections?

**Answer**: Yes, ALB supports WebSocket and maintains persistent connections.

### Q4: How does ALB preserve client IP?

**Answer**: ALB adds X-Forwarded-For header with client IP. NLB preserves source IP natively.

### Q5: What's the cost difference between ALB and NLB?

**Answer**: 
- ALB: ~$73.80/month (higher due to LCU processing)
- NLB: ~$59.40/month (lower due to simpler processing)

### Q6: Can you use both ALB and NLB together?

**Answer**: Yes, common pattern:
- ALB for web traffic (HTTP/HTTPS)
- NLB for backend services (TCP/UDP)

### Q7: What's cross-zone load balancing?

**Answer**: Distributes traffic evenly across all targets in all enabled AZs, not just within same AZ.

---

## Key Takeaways

1. **ALB = Layer 7** (HTTP/HTTPS, content-based routing)
2. **NLB = Layer 4** (TCP/UDP, ultra-low latency)
3. **Use ALB** for web apps, microservices, APIs
4. **Use NLB** for high performance, static IP, non-HTTP
5. **ALB** has more features (WAF, authentication, Lambda)
6. **NLB** has better performance (millions req/sec, <1ms latency)
7. **Both** support SSL/TLS termination and health checks
8. **Consider hybrid** architecture for complex applications

---

## Practice Problems

1. Design load balancer architecture for e-commerce platform
2. Choose between ALB and NLB for gaming server
3. Implement path-based routing for microservices
4. Configure health checks for database cluster
5. Design hybrid architecture with both ALB and NLB
6. Calculate cost for high-traffic application
7. Implement blue-green deployment with ALB
8. Configure NLB for static IP requirements
9. Design multi-region load balancing strategy
10. Troubleshoot unhealthy target issues
