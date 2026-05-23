# 4. Networking

## Docker Network Model

```
┌─────────────────────────────────────────────────────────────┐
│                         HOST                                  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              docker0 (bridge) 172.17.0.1              │   │
│  │                                                       │   │
│  │   ┌─────────┐    ┌─────────┐    ┌─────────┐         │   │
│  │   │Container│    │Container│    │Container│         │   │
│  │   │172.17.0.2│   │172.17.0.3│   │172.17.0.4│        │   │
│  │   │  veth   │    │  veth   │    │  veth   │         │   │
│  │   └────┬────┘    └────┬────┘    └────┬────┘         │   │
│  │        └──────────────┼──────────────┘               │   │
│  └───────────────────────┼──────────────────────────────┘   │
│                           │                                   │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │              iptables (NAT/MASQUERADE)                 │   │
│  └────────────────────────┬─────────────────────────────┘   │
│                           │                                   │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │              eth0 (host interface)                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Network Drivers

| Driver | Scope | Use Case |
|--------|-------|----------|
| **bridge** | Single host | Default; container-to-container on same host |
| **host** | Single host | No network isolation; use host's network stack |
| **none** | Single host | No networking (fully isolated) |
| **overlay** | Multi-host | Swarm/multi-host communication |
| **macvlan** | Single host | Containers get real MAC/IP on physical network |
| **ipvlan** | Single host | Like macvlan but shares host MAC |

---

## Bridge Networks

### Default Bridge (docker0)

```bash
# Containers on default bridge:
# - Can communicate by IP
# - CANNOT communicate by name (no DNS)
# - All containers share one bridge
docker run -d --name app1 nginx
docker run -d --name app2 nginx
# app1 cannot reach app2 by name
```

### User-Defined Bridge (Recommended)

```bash
# Create custom bridge network
docker network create \
  --driver bridge \
  --subnet 172.20.0.0/16 \
  --ip-range 172.20.240.0/20 \
  --gateway 172.20.0.1 \
  --opt com.docker.network.bridge.name=app-bridge \
  --opt com.docker.network.bridge.enable_icc=true \
  --opt com.docker.network.bridge.enable_ip_masquerade=true \
  app-network

# Run containers on custom network
docker run -d --name payment --network app-network payment:1.0
docker run -d --name postgres --network app-network postgres:16

# Containers can reach each other by name!
# payment can connect to postgres:5432
```

**User-defined bridge advantages over default:**
1. **Automatic DNS resolution** — containers communicate by name
2. **Better isolation** — only connected containers can communicate
3. **Connect/disconnect live** — attach containers without restart
4. **Configurable** — custom subnets, gateways, options

### Connect/Disconnect Containers

```bash
# Connect running container to additional network
docker network connect app-network existing-container

# Disconnect
docker network disconnect app-network existing-container

# Connect with specific IP
docker network connect --ip 172.20.0.100 app-network myapp

# Container on multiple networks
docker run -d --name gateway \
  --network frontend \
  --network backend \
  gateway:1.0
# Or connect after creation:
docker network connect backend gateway
```

---

## Host Network

Container shares the host's network namespace — no isolation, no port mapping needed.

```bash
# Container uses host's IP and ports directly
docker run -d --network host nginx
# nginx is accessible on host's port 80 directly

# Use cases:
# - Maximum network performance (no NAT overhead)
# - Container needs to see all host traffic
# - Monitoring/networking tools
```

**Limitations:**
- Port conflicts with host services
- No port mapping (`-p` is ignored)
- Less isolation
- Only works on Linux (Docker Desktop uses VM)

---

## None Network

Completely isolated — no network interface except loopback.

```bash
docker run -d --network none myapp
# Only has lo (127.0.0.1)
# Use case: security-sensitive batch processing
```

---

## Overlay Network (Multi-Host)

For Docker Swarm or containers across multiple hosts.

```bash
# Create overlay network (requires Swarm mode)
docker network create \
  --driver overlay \
  --subnet 10.0.9.0/24 \
  --attachable \
  --encrypted \
  multi-host-network

# --attachable: allows standalone containers (not just services)
# --encrypted: IPsec encryption between nodes
```

---

## Macvlan Network

Containers get their own MAC address and appear as physical devices on the network.

```bash
# Create macvlan network
docker network create \
  --driver macvlan \
  --subnet 192.168.1.0/24 \
  --gateway 192.168.1.1 \
  --opt parent=eth0 \
  macvlan-net

# Container gets real IP on physical network
docker run -d --network macvlan-net --ip 192.168.1.100 myapp

# Use cases:
# - Legacy apps that need to be on physical network
# - Apps that need routable IPs
# - Network monitoring tools
```

### Macvlan 802.1q (VLAN Trunking)

```bash
# Create macvlan on VLAN 100
docker network create \
  --driver macvlan \
  --subnet 192.168.100.0/24 \
  --gateway 192.168.100.1 \
  --opt parent=eth0.100 \
  macvlan-vlan100
```

---

## IPvlan Network

Like macvlan but all containers share the host's MAC address.

```bash
# L2 mode (same subnet as host)
docker network create \
  --driver ipvlan \
  --subnet 192.168.1.0/24 \
  --gateway 192.168.1.1 \
  --opt parent=eth0 \
  --opt ipvlan_mode=l2 \
  ipvlan-net

# L3 mode (routing between subnets)
docker network create \
  --driver ipvlan \
  --subnet 10.10.0.0/24 \
  --opt parent=eth0 \
  --opt ipvlan_mode=l3 \
  ipvlan-l3
```

---

## Port Mapping

```bash
# Map host port to container port
docker run -p 8080:80 nginx              # host:container
docker run -p 127.0.0.1:8080:80 nginx    # Bind to localhost only
docker run -p 8080:80/tcp nginx           # TCP only (default)
docker run -p 8080:80/udp nginx           # UDP
docker run -p 8080-8090:80-90 nginx       # Port range

# Random host port
docker run -p 80 nginx                    # Random host port → container 80
docker run -P nginx                       # Map all EXPOSE'd ports to random

# Check port mappings
docker port myapp
# 80/tcp -> 0.0.0.0:8080
# 80/tcp -> [::]:8080
```

### Port Mapping Internals

```bash
# Docker creates iptables rules:
# 1. DNAT: Redirect incoming traffic to container IP
# 2. MASQUERADE: Container outbound traffic uses host IP

# View rules
iptables -t nat -L DOCKER -n
# DNAT tcp -- 0.0.0.0/0 0.0.0.0/0 tcp dpt:8080 to:172.17.0.2:80
```

---

## DNS Resolution

### Built-in DNS (User-Defined Networks)

```bash
# Docker runs embedded DNS server at 127.0.0.11
# Containers on user-defined networks get automatic DNS

docker network create mynet
docker run -d --name db --network mynet postgres:16
docker run -d --name app --network mynet myapp:1.0

# Inside 'app' container:
# nslookup db → 172.20.0.2
# ping db → works!
```

### DNS Configuration

```bash
# Custom DNS servers
docker run --dns 8.8.8.8 --dns 8.8.4.4 myapp

# Custom DNS search domains
docker run --dns-search example.com myapp

# Custom hostname
docker run --hostname payment-service myapp

# Add host entries
docker run --add-host db.local:192.168.1.100 myapp
docker run --add-host host.docker.internal:host-gateway myapp  # Access host from container
```

### Service Discovery Patterns

```bash
# Pattern 1: Docker DNS (simple)
# Containers on same network resolve by name

# Pattern 2: Network aliases
docker run --network mynet --network-alias payment myapp
docker run --network mynet --network-alias payment myapp-v2
# Both respond to "payment" (round-robin)

# Pattern 3: External DNS (production)
# Use Consul, CoreDNS, or cloud DNS
```

---

## Network Inspection & Debugging

```bash
# List networks
docker network ls

# Inspect network (see connected containers, config)
docker network inspect app-network

# Inspect container networking
docker inspect --format '{{json .NetworkSettings.Networks}}' myapp

# Get container IP
docker inspect --format '{{.NetworkSettings.IPAddress}}' myapp

# Test connectivity from container
docker exec myapp ping db
docker exec myapp nslookup payment-service
docker exec myapp curl -s http://api:8080/health
docker exec myapp nc -zv postgres 5432

# Network debugging container
docker run --rm --network app-network nicolaka/netshoot \
  tcpdump -i any port 8080

# Check iptables rules
iptables -t nat -L -n -v | grep DOCKER
```

---

## Network Performance

### Disable Userland Proxy

```json
// /etc/docker/daemon.json
{
  "userland-proxy": false
}
```

By default, Docker uses a userland proxy (docker-proxy) for port forwarding. Disabling it uses pure iptables — better performance for high-traffic services.

### Network Modes Performance Comparison

| Mode | Latency | Throughput | Isolation |
|------|---------|------------|-----------|
| host | Lowest | Highest | None |
| macvlan/ipvlan | Low | High | Good |
| bridge | Medium | Good | Good |
| overlay | Higher | Lower | Good (multi-host) |

---

## IPv6 Support

```json
// /etc/docker/daemon.json
{
  "ipv6": true,
  "fixed-cidr-v6": "fd00::/80"
}
```

```bash
# Create dual-stack network
docker network create \
  --ipv6 \
  --subnet 172.20.0.0/16 \
  --subnet fd00:dead:beef::/48 \
  dual-stack-net
```

---

## Network Security

```bash
# Disable inter-container communication on default bridge
# /etc/docker/daemon.json: { "icc": false }

# Internal network (no external access)
docker network create --internal isolated-net

# Encrypted overlay
docker network create --driver overlay --encrypted secure-overlay
```

---

## Next: [Storage & Volumes →](05_Storage_and_Volumes.md)
