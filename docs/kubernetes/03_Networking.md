# 3. Networking

## Kubernetes Networking Model

Four fundamental networking requirements:
1. **Pod-to-Pod**: Every Pod can communicate with every other Pod without NAT
2. **Pod-to-Service**: Pods access Services via stable virtual IPs
3. **External-to-Service**: External traffic reaches Services via Ingress/LoadBalancer
4. **Pod-to-External**: Pods can reach external services (egress)

### Network Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     EXTERNAL TRAFFIC                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                  LOAD BALANCER (L4)                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              INGRESS CONTROLLER (L7)                          │
│         (NGINX / Istio Gateway / AWS ALB)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    SERVICES (ClusterIP)                       │
│              Virtual IP → Pod Endpoints                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                         PODS                                  │
│    ┌─────────┐    ┌─────────┐    ┌─────────┐               │
│    │ Pod A   │    │ Pod B   │    │ Pod C   │               │
│    │10.0.1.5 │    │10.0.2.3 │    │10.0.3.7 │               │
│    └─────────┘    └─────────┘    └─────────┘               │
└─────────────────────────────────────────────────────────────┘
```

---

## Services

A Service provides a stable endpoint (IP + DNS) to access a set of Pods.

### Service Types

| Type | Scope | Use Case |
|------|-------|----------|
| ClusterIP | Internal only | Inter-service communication |
| NodePort | External via node IP:port | Development, testing |
| LoadBalancer | External via cloud LB | Production external access |
| ExternalName | DNS CNAME alias | Access external services |
| Headless | No ClusterIP | StatefulSets, direct Pod access |

### ClusterIP (Default)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: payment-service
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: payment-service
  ports:
    - name: http
      port: 80           # Service port
      targetPort: 8080   # Container port
      protocol: TCP
    - name: grpc
      port: 9090
      targetPort: 9090
```

**Access**: `payment-service.production.svc.cluster.local:80`

### LoadBalancer (Production External)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  annotations:
    # AWS-specific
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-scheme: "internet-facing"
    service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled: "true"
    service.beta.kubernetes.io/aws-load-balancer-ssl-cert: "arn:aws:acm:..."
    service.beta.kubernetes.io/aws-load-balancer-ssl-ports: "443"
spec:
  type: LoadBalancer
  selector:
    app: api-gateway
  ports:
    - name: https
      port: 443
      targetPort: 8080
  externalTrafficPolicy: Local  # Preserve client IP
```

### Headless Service (StatefulSets)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka-headless
spec:
  clusterIP: None  # No virtual IP
  selector:
    app: kafka
  ports:
    - port: 9092
```

**DNS Records**: Individual A records per Pod
```
kafka-0.kafka-headless.default.svc.cluster.local → 10.0.1.5
kafka-1.kafka-headless.default.svc.cluster.local → 10.0.2.3
kafka-2.kafka-headless.default.svc.cluster.local → 10.0.3.7
```

### ExternalName (External Service Alias)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-db
spec:
  type: ExternalName
  externalName: mydb.us-east-1.rds.amazonaws.com
```

---

## Ingress

Layer 7 (HTTP/HTTPS) routing to Services.

### NGINX Ingress Controller

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: main-ingress
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/enable-cors: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.example.com
        - app.example.com
      secretName: tls-secret
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /v1/payments
            pathType: Prefix
            backend:
              service:
                name: payment-service
                port:
                  number: 80
          - path: /v1/users
            pathType: Prefix
            backend:
              service:
                name: user-service
                port:
                  number: 80
    - host: app.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend
                port:
                  number: 80
```

### AWS ALB Ingress (AWS Load Balancer Controller)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: alb-ingress
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:...
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
    alb.ingress.kubernetes.io/healthcheck-path: /health
    alb.ingress.kubernetes.io/group.name: production
    alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:...
spec:
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-service
                port:
                  number: 80
```

### Gateway API (Next-gen Ingress)

```yaml
# Gateway (infrastructure team manages)
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: production-gateway
spec:
  gatewayClassName: istio
  listeners:
    - name: https
      protocol: HTTPS
      port: 443
      tls:
        mode: Terminate
        certificateRefs:
          - name: tls-cert
      allowedRoutes:
        namespaces:
          from: Selector
          selector:
            matchLabels:
              gateway-access: "true"
---
# HTTPRoute (app team manages)
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: payment-route
spec:
  parentRefs:
    - name: production-gateway
  hostnames:
    - "api.example.com"
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /v1/payments
      backendRefs:
        - name: payment-service
          port: 80
          weight: 90
        - name: payment-service-canary
          port: 80
          weight: 10
```

---

## DNS (CoreDNS)

Kubernetes uses CoreDNS for service discovery.

### DNS Resolution Format

```
<service>.<namespace>.svc.cluster.local
<pod-ip-dashed>.<namespace>.pod.cluster.local
```

**Examples:**
```
payment-service.production.svc.cluster.local     → ClusterIP
postgres-0.postgres-headless.db.svc.cluster.local → Pod IP
```

### CoreDNS Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns
  namespace: kube-system
data:
  Corefile: |
    .:53 {
        errors
        health {
            lameduck 5s
        }
        ready
        kubernetes cluster.local in-addr.arpa ip6.arpa {
            pods insecure
            fallthrough in-addr.arpa ip6.arpa
            ttl 30
        }
        prometheus :9153
        forward . /etc/resolv.conf {
            max_concurrent 1000
        }
        cache 30
        loop
        reload
        loadbalance
    }
```

### DNS Policies

```yaml
spec:
  dnsPolicy: ClusterFirst  # Default: use CoreDNS
  # dnsPolicy: Default     # Use node's DNS
  # dnsPolicy: None        # Custom DNS config below
  dnsConfig:
    nameservers:
      - 8.8.8.8
    searches:
      - production.svc.cluster.local
      - svc.cluster.local
    options:
      - name: ndots
        value: "2"  # Reduce DNS lookups for external domains
```

---

## Network Policies

Firewall rules for Pod-to-Pod communication. **Default: all traffic allowed.**

### Deny All (Zero Trust Baseline)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all
  namespace: production
spec:
  podSelector: {}  # Apply to all pods
  policyTypes:
    - Ingress
    - Egress
```

### Allow Specific Traffic

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-service-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: payment-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # Allow from API gateway only
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
        - namespaceSelector:
            matchLabels:
              name: ingress
      ports:
        - protocol: TCP
          port: 8080
  egress:
    # Allow to database
    - to:
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - protocol: TCP
          port: 5432
    # Allow DNS
    - to:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
    # Allow external payment processor
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
            except:
              - 10.0.0.0/8
              - 172.16.0.0/12
              - 192.168.0.0/16
      ports:
        - protocol: TCP
          port: 443
```

### Namespace Isolation

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: namespace-isolation
  namespace: production
spec:
  podSelector: {}
  policyTypes:
    - Ingress
  ingress:
    # Only allow traffic from same namespace
    - from:
        - podSelector: {}
    # And from monitoring namespace
    - from:
        - namespaceSelector:
            matchLabels:
              name: monitoring
```

---

## Service Mesh (Istio)

### Architecture

```
┌─────────────────────────────────────────────┐
│              CONTROL PLANE                    │
│  ┌─────────┐  ┌────────┐  ┌─────────────┐  │
│  │  Istiod │  │ Pilot  │  │   Citadel   │  │
│  │(config) │  │(xDS)   │  │  (mTLS CA)  │  │
│  └─────────┘  └────────┘  └─────────────┘  │
└─────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│              DATA PLANE                      │
│  ┌──────────────────────────────────────┐   │
│  │ Pod                                   │   │
│  │  ┌─────────┐  ┌──────────────────┐  │   │
│  │  │   App   │←→│  Envoy Sidecar   │  │   │
│  │  └─────────┘  └──────────────────┘  │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### Traffic Management

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: payment-service
spec:
  hosts:
    - payment-service
  http:
    # Canary routing
    - match:
        - headers:
            x-canary:
              exact: "true"
      route:
        - destination:
            host: payment-service
            subset: canary
    # Weighted routing
    - route:
        - destination:
            host: payment-service
            subset: stable
          weight: 95
        - destination:
            host: payment-service
            subset: canary
          weight: 5
    # Timeout and retry
    timeout: 10s
    retries:
      attempts: 3
      perTryTimeout: 3s
      retryOn: 5xx,reset,connect-failure
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: payment-service
spec:
  host: payment-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 60s
      maxEjectionPercent: 50
    tls:
      mode: ISTIO_MUTUAL  # mTLS
  subsets:
    - name: stable
      labels:
        version: v1
    - name: canary
      labels:
        version: v2
```

### Circuit Breaker

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: external-api
spec:
  host: external-api.production.svc.cluster.local
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 50
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 10
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 10s
      baseEjectionTime: 30s
      maxEjectionPercent: 100
```

---

## CNI Plugins

Container Network Interface plugins implement Pod networking.

| Plugin | Features | Best For |
|--------|----------|----------|
| **Calico** | NetworkPolicy, BGP, eBPF | General purpose, large clusters |
| **Cilium** | eBPF, L7 policies, observability | Security-focused, high performance |
| **AWS VPC CNI** | Native VPC networking | EKS (Pods get VPC IPs) |
| **Flannel** | Simple overlay (VXLAN) | Small clusters, learning |
| **Weave** | Mesh networking, encryption | Multi-cloud |

### Cilium (eBPF-based)

```yaml
# L7 Network Policy with Cilium
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: api-l7-policy
spec:
  endpointSelector:
    matchLabels:
      app: api-server
  ingress:
    - fromEndpoints:
        - matchLabels:
            app: frontend
      toPorts:
        - ports:
            - port: "8080"
              protocol: TCP
          rules:
            http:
              - method: GET
                path: "/api/v1/.*"
              - method: POST
                path: "/api/v1/orders"
```

---

## Next: [Storage →](04_Storage.md)
