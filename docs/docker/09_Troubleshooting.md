# 9. Troubleshooting & Performance

## Debugging Workflow

```
Problem Detected
       │
       ▼
┌─────────────────┐
│ Container State │──► Exited? Check exit code + logs
│                 │──► Restarting? Check health + resources
│                 │──► Created (not running)? Check config
└────────┬────────┘
         ▼
┌─────────────────┐
│ Check Logs      │──► Application errors? Fix code
│                 │──► Permission denied? Fix user/mounts
│                 │──► Connection refused? Check networking
└────────┬────────┘
         ▼
┌─────────────────┐
│ Check Resources │──► OOMKilled? Increase memory limit
│                 │──► CPU throttled? Increase CPU limit
│                 │──► Disk full? Cleanup/expand
└────────┬────────┘
         ▼
┌─────────────────┐
│ Check Network   │──► DNS failure? Check network config
│                 │──► Port conflict? Check port mapping
│                 │──► Can't reach service? Check network membership
└─────────────────┘
```

---

## Essential Debugging Commands

### Container State

```bash
# Overview of all containers
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}"

# Detailed inspection
docker inspect myapp
docker inspect --format '{{.State.Status}}' myapp
docker inspect --format '{{.State.ExitCode}}' myapp
docker inspect --format '{{.State.OOMKilled}}' myapp
docker inspect --format '{{json .State}}' myapp | jq

# Events (real-time container lifecycle)
docker events
docker events --filter container=myapp
docker events --filter event=die --filter event=oom
docker events --since "2024-01-15T10:00:00" --format '{{.Time}} {{.Actor.Attributes.name}} {{.Action}}'
```

### Logs

```bash
# Basic logs
docker logs myapp
docker logs --tail 100 myapp           # Last 100 lines
docker logs -f myapp                   # Follow (stream)
docker logs --since 30m myapp          # Last 30 minutes
docker logs --since 2024-01-15T10:00:00 --until 2024-01-15T11:00:00 myapp
docker logs myapp 2>&1 | grep ERROR    # Filter errors

# Logs with timestamps
docker logs -t myapp

# Compose logs (all services)
docker compose logs -f --tail 50
docker compose logs payment-service --since 5m
```

### Shell Access

```bash
# Interactive shell
docker exec -it myapp /bin/sh
docker exec -it myapp /bin/bash

# As root (when container runs as non-root)
docker exec -u 0 myapp /bin/sh

# If no shell in image (distroless/scratch)
docker debug myapp                     # Docker Desktop 4.27+
# Or use ephemeral debug container:
docker run --rm -it --pid=container:myapp --net=container:myapp busybox

# Run diagnostic command
docker exec myapp cat /etc/resolv.conf
docker exec myapp env
docker exec myapp ps aux
docker exec myapp df -h
docker exec myapp free -m
```

### Filesystem Inspection

```bash
# See what changed in container layer
docker diff myapp
# A /tmp/cache/data.json    (Added)
# C /var/log                 (Changed)
# D /tmp/old-file            (Deleted)

# Copy files out for inspection
docker cp myapp:/var/log/app/error.log ./error.log
docker cp myapp:/app/config/ ./config-dump/

# Export entire filesystem
docker export myapp > myapp-fs.tar
tar -tf myapp-fs.tar | grep -i config
```

---

## Common Issues & Solutions

### 1. Container Exits Immediately

```bash
# Check exit code
docker inspect --format '{{.State.ExitCode}}' myapp

# Exit 0: Process completed (not a daemon)
# Fix: Ensure process runs in foreground
# ❌ CMD service nginx start  (starts and exits)
# ✅ CMD ["nginx", "-g", "daemon off;"]

# Exit 1: Application error
docker logs myapp  # Check error message

# Exit 126: Permission denied (not executable)
# Fix: RUN chmod +x /app/entrypoint.sh

# Exit 127: Command not found
# Fix: Check PATH, verify binary exists in image

# Exit 137: OOMKilled or SIGKILL
docker inspect --format '{{.State.OOMKilled}}' myapp
# Fix: Increase --memory limit

# Exit 143: SIGTERM (graceful stop)
# Normal during docker stop
```

### 2. OOMKilled

```bash
# Detect
docker inspect --format '{{.State.OOMKilled}}' myapp
docker events --filter event=oom

# Check memory usage
docker stats myapp --no-stream

# Fix: Increase limit
docker update --memory 2g myapp
# Or in compose: deploy.resources.limits.memory

# Investigate: Is it a memory leak?
docker exec myapp cat /proc/1/status | grep VmRSS
# Monitor over time to detect growth
```

### 3. Container Can't Connect to Another Container

```bash
# Check both containers are on same network
docker network inspect app-network | jq '.[0].Containers'

# Test DNS resolution
docker exec myapp nslookup db
docker exec myapp getent hosts db

# Test connectivity
docker exec myapp ping db
docker exec myapp nc -zv db 5432
docker exec myapp curl -v http://api:8080/health

# Common fixes:
# 1. Containers must be on same user-defined network
# 2. Use service name (not container name) in Compose
# 3. Check if target container is healthy and listening
# 4. Check if internal network is blocking external access
```

### 4. Port Already in Use

```bash
# Find what's using the port
lsof -i :8080
ss -tlnp | grep 8080

# Fix: Use different host port
docker run -p 8081:8080 myapp

# Or stop conflicting service
docker stop conflicting-container
```

### 5. Permission Denied on Volume

```bash
# Check file ownership
docker exec myapp ls -la /data

# Check container user
docker exec myapp id
# uid=1000(app) gid=1000(app)

# Fix: Match ownership
docker run --user $(id -u):$(id -g) -v ./data:/data myapp

# Or fix permissions on host
sudo chown -R 1000:1000 ./data

# Or use named volume (Docker manages permissions)
docker volume create mydata
docker run -v mydata:/data myapp
```

### 6. Image Pull Failures

```bash
# Authentication issue
docker login registry.company.com

# Network issue
docker pull --debug myapp:1.0

# Image doesn't exist
docker manifest inspect registry.company.com/myapp:1.0

# Rate limited (Docker Hub)
# Fix: Use authenticated pulls or mirror
docker login  # Increases rate limit
# Or configure mirror in daemon.json
```

### 7. Build Failures

```bash
# Build with verbose output
docker build --progress=plain --no-cache .

# Check specific stage
docker build --target builder .

# Debug failed build step (use last successful layer)
docker run --rm -it <last-successful-image-id> /bin/sh

# Clear build cache
docker builder prune -af
```

### 8. Slow Container Startup

```bash
# Profile startup time
time docker run --rm myapp echo "started"

# Common causes:
# 1. Large image (slow pull) → Use smaller base images
# 2. Slow entrypoint script → Optimize init logic
# 3. Waiting for dependencies → Use health checks + depends_on
# 4. JVM warmup → Use CDS, GraalVM native image
# 5. DNS resolution timeout → Check DNS config
```

---

## Performance Tuning

### Image Size Optimization

```bash
# Analyze image layers
docker history myapp:1.0
dive myapp:1.0  # Interactive layer explorer

# Size comparison
docker images --format "{{.Repository}}:{{.Tag}} {{.Size}}" | sort -k2 -h
```

### Build Performance

```bash
# Parallel builds (BuildKit)
DOCKER_BUILDKIT=1 docker build .

# Cache mounts (avoid re-downloading)
RUN --mount=type=cache,target=/root/.m2 mvn package
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

# Build context optimization
# Use .dockerignore to exclude unnecessary files
# Smaller context = faster build start

# Multi-stage: only copy what's needed
COPY --from=builder /app/target/app.jar /app/app.jar
```

### Runtime Performance

```bash
# CPU pinning (reduce context switching)
docker run --cpuset-cpus "0-3" myapp

# NUMA awareness
docker run --cpuset-cpus "0-7" --cpuset-mems "0" myapp

# Disable userland proxy (use iptables directly)
# /etc/docker/daemon.json: { "userland-proxy": false }

# Host networking (eliminate NAT overhead)
docker run --network host myapp

# tmpfs for temp files (RAM speed)
docker run --tmpfs /tmp:size=500m myapp

# Tune kernel parameters
docker run --sysctl net.core.somaxconn=65535 \
           --sysctl net.ipv4.tcp_max_syn_backlog=65535 \
           myapp
```

### Storage Performance

```bash
# Use volumes over bind mounts for I/O-heavy workloads
# Volumes use native filesystem, bind mounts may have overhead on Mac/Windows

# Use local SSD for Docker data
# /etc/docker/daemon.json: { "data-root": "/mnt/ssd/docker" }

# Avoid writing to container layer (use volumes)
docker run --read-only -v app-data:/data myapp

# I/O limits (prevent noisy neighbors)
docker run --device-read-bps /dev/sda:100mb --device-write-bps /dev/sda:50mb myapp
```

### Network Performance

```bash
# Benchmark network
docker run --rm --network host nicolaka/netshoot iperf3 -c target-host

# Optimize DNS (reduce lookups)
docker run --dns 8.8.8.8 myapp
# Or in container: set ndots=1 in /etc/resolv.conf

# Connection pooling (application level)
# Reuse connections to databases, APIs, etc.
```

---

## Resource Monitoring

### Real-Time Stats

```bash
# All containers
docker stats

# Specific containers with custom format
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}\t{{.PIDs}}"

# One-shot (for scripting)
docker stats --no-stream --format "{{.Name}}: CPU={{.CPUPerc}} MEM={{.MemUsage}}"
```

### Cgroup Metrics (Detailed)

```bash
# Find container's cgroup
CONTAINER_ID=$(docker inspect --format '{{.Id}}' myapp)

# cgroups v2 (modern)
cat /sys/fs/cgroup/system.slice/docker-${CONTAINER_ID}.scope/cpu.stat
cat /sys/fs/cgroup/system.slice/docker-${CONTAINER_ID}.scope/memory.current
cat /sys/fs/cgroup/system.slice/docker-${CONTAINER_ID}.scope/memory.max
cat /sys/fs/cgroup/system.slice/docker-${CONTAINER_ID}.scope/io.stat
```

### Process Inspection

```bash
# Processes inside container
docker top myapp
docker top myapp -eo pid,ppid,user,%cpu,%mem,vsz,rss,comm

# From host (find container processes)
PID=$(docker inspect --format '{{.State.Pid}}' myapp)
ps aux | grep $PID
ls /proc/$PID/fd | wc -l  # Open file descriptors

# strace (requires SYS_PTRACE capability)
docker run --cap-add SYS_PTRACE myapp
docker exec myapp strace -p 1 -f -e trace=network
```

---

## Disk Space Management

### Identify Space Usage

```bash
# Docker disk usage summary
docker system df
docker system df -v  # Detailed

# Find large images
docker images --format "{{.Size}}\t{{.Repository}}:{{.Tag}}" | sort -hr | head -20

# Find large containers (writable layer)
docker ps -s --format "table {{.Names}}\t{{.Size}}" | sort -k2 -hr

# Find large volumes
docker system df -v | grep -A 100 "Local Volumes"

# Host-level Docker directory size
du -sh /var/lib/docker/
du -sh /var/lib/docker/overlay2/
du -sh /var/lib/docker/volumes/
```

### Cleanup Strategies

```bash
# Nuclear option: remove everything unused
docker system prune -af --volumes

# Selective cleanup
docker container prune -f                    # Stopped containers
docker image prune -f                        # Dangling images
docker image prune -af                       # All unused images
docker image prune -af --filter "until=168h" # Images older than 7 days
docker volume prune -f                       # Unused volumes
docker network prune -f                      # Unused networks
docker builder prune -af                     # Build cache

# Remove specific old images
docker images --filter "before=myapp:current" -q | xargs docker rmi

# Automated cleanup (systemd timer or cron)
# /etc/cron.daily/docker-cleanup
#!/bin/bash
docker system prune -af --filter "until=168h" >> /var/log/docker-cleanup.log 2>&1
docker volume prune -f >> /var/log/docker-cleanup.log 2>&1
```

### Prevent Disk Issues

```json
// /etc/docker/daemon.json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-opts": [
    "overlay2.size=10G"  // Per-container storage limit (requires xfs + pquota)
  ]
}
```

---

## Docker Daemon Troubleshooting

```bash
# Check daemon status
systemctl status docker
journalctl -u docker --since "1 hour ago"

# Daemon debug mode
# /etc/docker/daemon.json: { "debug": true }
# Then: journalctl -u docker -f

# Restart daemon (containers keep running with live-restore)
sudo systemctl restart docker

# Check daemon configuration
docker info
docker info --format '{{json .}}' | jq

# Verify daemon connectivity
docker version
curl --unix-socket /var/run/docker.sock http://localhost/version
```

---

## Debugging Tools

| Tool | Purpose | Install |
|------|---------|---------|
| `dive` | Analyze image layers | `brew install dive` |
| `ctop` | Container top (htop for containers) | `brew install ctop` |
| `lazydocker` | Terminal UI for Docker | `brew install lazydocker` |
| `docker-slim` | Minify images | `brew install docker-slim` |
| `hadolint` | Dockerfile linter | `brew install hadolint` |
| `dockle` | Container security linter | `brew install goodwithtech/r/dockle` |
| `grype` | Vulnerability scanner | `brew install grype` |
| `netshoot` | Network debugging | `docker run nicolaka/netshoot` |

### Hadolint (Dockerfile Linting)

```bash
# Lint Dockerfile
hadolint Dockerfile

# Common rules:
# DL3008: Pin versions in apt-get install
# DL3018: Pin versions in apk add
# DL3025: Use JSON notation for CMD/ENTRYPOINT
# DL4006: Set SHELL option -o pipefail before RUN with pipe
# SC2086: Double quote to prevent globbing
```

---

## Quick Reference

```bash
# === Debugging ===
docker logs --tail 100 -f <container>     # Logs
docker exec -it <container> /bin/sh       # Shell
docker inspect <container>                 # Full details
docker stats                               # Resource usage
docker events                              # Real-time events
docker diff <container>                    # Filesystem changes

# === Performance ===
docker stats --no-stream                   # Snapshot
docker top <container>                     # Processes
docker system df                           # Disk usage

# === Cleanup ===
docker system prune -af --volumes          # Remove all unused
docker container prune -f                  # Stopped containers
docker image prune -af                     # Unused images
docker volume prune -f                     # Unused volumes
docker builder prune -af                   # Build cache

# === Network Debug ===
docker exec <container> nslookup <service> # DNS
docker exec <container> nc -zv <host> <port> # TCP
docker network inspect <network>           # Network details
```
