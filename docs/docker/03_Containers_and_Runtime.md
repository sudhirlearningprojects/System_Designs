# 3. Containers & Runtime

## Container Lifecycle

```
Created → Running → Paused → Running → Stopped → Removed
   │         │                             │
   │         └──── OOMKilled ──────────────┘
   │         └──── Crashed (exit != 0) ────┘
   └──── Never started (config error) ─────→ Removed
```

### Lifecycle Commands

```bash
# Create (don't start)
docker create --name myapp -p 8080:8080 myapp:1.0

# Start
docker start myapp

# Run (create + start in one)
docker run -d --name myapp -p 8080:8080 myapp:1.0

# Pause (freeze with SIGSTOP via cgroup freezer)
docker pause myapp
docker unpause myapp

# Stop (SIGTERM → wait → SIGKILL)
docker stop myapp              # 10s grace period (default)
docker stop -t 30 myapp       # 30s grace period

# Kill (immediate SIGKILL)
docker kill myapp
docker kill -s SIGTERM myapp   # Send specific signal

# Restart
docker restart myapp
docker restart -t 5 myapp     # 5s grace before kill

# Remove
docker rm myapp               # Must be stopped
docker rm -f myapp            # Force remove (kills if running)

# Remove all stopped containers
docker container prune
```

---

## Running Containers

### Essential Run Options

```bash
docker run \
  -d                              # Detached (background)
  --name payment-service          # Container name
  --hostname payment              # Container hostname
  -p 8080:8080                    # Port mapping host:container
  -p 127.0.0.1:9090:9090         # Bind to specific interface
  --restart unless-stopped        # Restart policy
  -e DB_HOST=postgres             # Environment variable
  -e DB_PASSWORD                  # Pass from host env
  --env-file .env                 # Load env from file
  -v pgdata:/var/lib/postgresql   # Named volume
  -v $(pwd)/config:/etc/config:ro # Bind mount (read-only)
  --tmpfs /tmp:size=100m          # tmpfs mount
  --network app-network           # Attach to network
  --cpus 2                        # CPU limit
  --memory 1g                     # Memory limit
  --memory-swap 2g                # Memory + swap limit
  --pids-limit 100                # Max processes
  --read-only                     # Read-only root filesystem
  --user 1000:1000                # Run as specific UID:GID
  --cap-drop ALL                  # Drop all capabilities
  --cap-add NET_BIND_SERVICE      # Add specific capability
  --security-opt no-new-privileges # Prevent privilege escalation
  --health-cmd "curl -f http://localhost:8080/health" \
  --health-interval 30s           # Health check
  --log-driver json-file          # Logging driver
  --log-opt max-size=10m          # Log rotation
  --log-opt max-file=3            # Max log files
  --label team=payments           # Metadata label
  --init                          # Use tini as PID 1
  myapp:1.0                       # Image
```

### Restart Policies

| Policy | Behavior |
|--------|----------|
| `no` | Never restart (default) |
| `on-failure[:max]` | Restart on non-zero exit (optional max retries) |
| `always` | Always restart (even on stop, restarts on daemon start) |
| `unless-stopped` | Like `always` but not after manual stop |

```bash
# Production: restart unless manually stopped
docker run -d --restart unless-stopped myapp:1.0

# Retry up to 5 times on failure
docker run -d --restart on-failure:5 myapp:1.0

# Update restart policy on running container
docker update --restart unless-stopped myapp
```

---

## Resource Constraints

### CPU

```bash
# Limit to 1.5 CPUs
docker run --cpus 1.5 myapp

# CPU shares (relative weight, default 1024)
docker run --cpu-shares 512 myapp    # Half priority
docker run --cpu-shares 2048 myapp   # Double priority

# Pin to specific CPUs
docker run --cpuset-cpus "0,1" myapp       # CPUs 0 and 1
docker run --cpuset-cpus "0-3" myapp       # CPUs 0 through 3

# CPU quota (fine-grained)
docker run --cpu-period 100000 --cpu-quota 50000 myapp  # 50% of 1 CPU
```

### Memory

```bash
# Hard memory limit (OOMKilled if exceeded)
docker run --memory 512m myapp

# Memory + swap (total limit)
docker run --memory 512m --memory-swap 1g myapp   # 512m RAM + 512m swap
docker run --memory 512m --memory-swap 512m myapp # No swap allowed
docker run --memory 512m --memory-swap -1 myapp   # Unlimited swap

# Soft limit (preference, not enforced)
docker run --memory-reservation 256m myapp

# OOM kill priority (-1000 to 1000, lower = less likely to be killed)
docker run --oom-score-adj -500 myapp

# Disable OOM killer (dangerous!)
docker run --oom-kill-disable --memory 512m myapp

# Kernel memory limit
docker run --kernel-memory 50m myapp
```

### I/O

```bash
# Block I/O weight (10-1000, default 500)
docker run --blkio-weight 300 myapp

# Device read/write rate limits
docker run --device-read-bps /dev/sda:10mb myapp
docker run --device-write-bps /dev/sda:10mb myapp
docker run --device-read-iops /dev/sda:1000 myapp
docker run --device-write-iops /dev/sda:1000 myapp
```

### PIDs

```bash
# Limit number of processes (prevent fork bombs)
docker run --pids-limit 100 myapp
```

### Ulimits

```bash
# Set file descriptor limits
docker run --ulimit nofile=65536:65536 myapp

# Set max processes
docker run --ulimit nproc=4096:4096 myapp

# Core dump size
docker run --ulimit core=0:0 myapp  # Disable core dumps
```

---

## Executing Commands in Containers

```bash
# Interactive shell
docker exec -it myapp /bin/sh
docker exec -it myapp /bin/bash

# Run command
docker exec myapp cat /etc/hosts
docker exec myapp ps aux

# As different user
docker exec -u root myapp apt-get update
docker exec -u 0 myapp whoami

# With environment variables
docker exec -e DEBUG=true myapp ./run-diagnostics.sh

# Working directory
docker exec -w /app/logs myapp ls -la

# Detached (background)
docker exec -d myapp /app/cleanup.sh
```

---

## Container Inspection

```bash
# Full inspection (JSON)
docker inspect myapp

# Specific fields
docker inspect --format '{{.State.Status}}' myapp
docker inspect --format '{{.NetworkSettings.IPAddress}}' myapp
docker inspect --format '{{.HostConfig.Memory}}' myapp
docker inspect --format '{{json .Config.Env}}' myapp
docker inspect --format '{{.State.Pid}}' myapp

# Resource usage (live)
docker stats
docker stats myapp --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# Processes inside container
docker top myapp
docker top myapp -eo pid,user,cmd

# Filesystem changes since creation
docker diff myapp
# A = Added, C = Changed, D = Deleted

# Port mappings
docker port myapp

# Logs
docker logs myapp
docker logs --tail 100 -f myapp              # Last 100 + follow
docker logs --since 2024-01-15T10:00:00 myapp # Since timestamp
docker logs --since 30m myapp                 # Last 30 minutes
```

---

## Container Signals & Graceful Shutdown

### Signal Flow

```
docker stop myapp
       │
       ▼
SIGTERM sent to PID 1 in container
       │
       ├── App handles SIGTERM → graceful shutdown
       │   (drain connections, finish requests, flush buffers)
       │
       ▼ (after grace period, default 10s)
SIGKILL sent → immediate termination
```

### Proper Signal Handling

```dockerfile
# ❌ BAD: Shell form — /bin/sh is PID 1, app doesn't get signals
CMD npm start

# ✅ GOOD: Exec form — app is PID 1, receives signals directly
CMD ["node", "server.js"]

# ✅ GOOD: Use tini for proper signal forwarding + zombie reaping
RUN apk add --no-cache tini
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["node", "server.js"]

# ✅ GOOD: Docker --init flag (uses built-in tini)
# docker run --init myapp
```

### Why Tini/Init?

PID 1 has special responsibilities in Linux:
1. **Signal forwarding**: Must forward signals to child processes
2. **Zombie reaping**: Must wait() on orphaned child processes
3. Most applications don't handle PID 1 responsibilities

---

## Container as Ephemeral Process

### Stateless Design

```bash
# Containers should be:
# 1. Disposable — can be stopped/removed anytime
# 2. Reproducible — same image = same behavior
# 3. Stateless — no important data in container layer

# Store state externally:
# - Databases (volumes or external services)
# - Object storage (S3)
# - Cache (Redis)
# - Logs (stdout → log driver → aggregator)
```

### One Process Per Container

```bash
# ❌ BAD: Multiple services in one container
# supervisord running nginx + php-fpm + cron

# ✅ GOOD: Separate containers
docker run -d --name web nginx
docker run -d --name app php-fpm
docker run -d --name cron mycron
```

---

## Container Filesystem

### Layers and Copy-on-Write

```
┌─────────────────────────────────┐
│   Container Layer (thin R/W)     │ ← Writes go here (CoW)
├─────────────────────────────────┤
│   Image Layer N (read-only)      │
├─────────────────────────────────┤
│   ...                            │
├─────────────────────────────────┤
│   Image Layer 1 (read-only)      │
└─────────────────────────────────┘
```

**Copy-on-Write (CoW):**
- Reading: Reads from highest layer containing the file
- Writing: Copies file from image layer to container layer, then modifies
- Deleting: Creates "whiteout" file in container layer

### Read-Only Containers

```bash
# Read-only root filesystem (security best practice)
docker run --read-only \
  --tmpfs /tmp:size=100m \
  --tmpfs /var/run:size=10m \
  -v app-data:/data \
  myapp:1.0
```

---

## Container Export/Import

```bash
# Export container filesystem to tar
docker export myapp > myapp-fs.tar

# Import as new image (flat, no layers/history)
docker import myapp-fs.tar myapp-imported:1.0

# Save image (preserves layers and history)
docker save myapp:1.0 > myapp-image.tar
docker save myapp:1.0 | gzip > myapp-image.tar.gz

# Load image
docker load < myapp-image.tar

# Copy files to/from container
docker cp myapp:/app/logs/error.log ./error.log
docker cp ./config.yaml myapp:/etc/config/config.yaml
```

---

## Runtime Metrics

```bash
# Live resource usage
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}\t{{.PIDs}}"

# Output:
# NAME              CPU %   MEM USAGE / LIMIT   NET I/O         BLOCK I/O       PIDS
# payment-service   2.5%    256MiB / 1GiB       1.2MB / 500KB   10MB / 5MB      25
# postgres          5.1%    512MiB / 2GiB       800KB / 1.5MB   50MB / 100MB    30

# Container events
docker events
docker events --filter container=myapp --filter event=die
docker events --since "2024-01-15T10:00:00" --until "2024-01-15T11:00:00"
```

---

## Update Running Container

```bash
# Update resource limits without restart
docker update --cpus 2 --memory 1g myapp
docker update --restart unless-stopped myapp
docker update --pids-limit 200 myapp

# Note: Not all options can be updated live
# CPU, memory, restart policy, pids-limit can be updated
# Network, volumes, ports cannot be updated (recreate container)
```

---

## Container Wait and Exit Codes

```bash
# Wait for container to stop and get exit code
docker wait myapp
# Returns: 0 (success), 1 (error), 137 (OOMKilled/SIGKILL), 143 (SIGTERM)

# Common exit codes:
# 0   — Success
# 1   — General error
# 126 — Command not executable
# 127 — Command not found
# 128+N — Fatal signal N (137=SIGKILL, 143=SIGTERM)
# 255 — Exit status out of range
```

---

## Next: [Networking →](04_Networking.md)
