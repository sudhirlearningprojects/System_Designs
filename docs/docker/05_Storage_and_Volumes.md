# 5. Storage & Volumes

## Storage Types

```
┌─────────────────────────────────────────────────────────────┐
│                     CONTAINER                                 │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Volumes    │  │ Bind Mounts  │  │      tmpfs       │  │
│  │              │  │              │  │                   │  │
│  │ Docker-      │  │ Host path    │  │ RAM-backed        │  │
│  │ managed      │  │ mounted into │  │ (never written    │  │
│  │ /var/lib/    │  │ container    │  │  to disk)         │  │
│  │ docker/      │  │              │  │                   │  │
│  │ volumes/     │  │              │  │                   │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│                                                              │
│  Best for:         Best for:          Best for:             │
│  - Databases       - Dev (live code)  - Secrets             │
│  - Persistent      - Config files     - Temp data           │
│    data            - Build artifacts  - Sensitive data       │
└─────────────────────────────────────────────────────────────┘
```

| Type | Managed By | Persistence | Performance | Use Case |
|------|-----------|-------------|-------------|----------|
| **Volume** | Docker | Survives container removal | Best | Databases, persistent data |
| **Bind Mount** | User | Depends on host path | Good | Development, config |
| **tmpfs** | Kernel | Lost on container stop | Fastest | Secrets, temp files |

---

## Volumes

### Volume Lifecycle

```bash
# Create volume
docker volume create pgdata
docker volume create --driver local \
  --opt type=none \
  --opt device=/mnt/ssd/data \
  --opt o=bind \
  fast-storage

# List volumes
docker volume ls
docker volume ls --filter dangling=true  # Unused volumes

# Inspect volume
docker volume inspect pgdata
# {
#   "Name": "pgdata",
#   "Driver": "local",
#   "Mountpoint": "/var/lib/docker/volumes/pgdata/_data",
#   "Scope": "local"
# }

# Remove volume
docker volume rm pgdata

# Remove all unused volumes
docker volume prune
docker volume prune --all  # Including named volumes not in use
```

### Using Volumes

```bash
# Named volume (recommended)
docker run -d \
  --name postgres \
  -v pgdata:/var/lib/postgresql/data \
  postgres:16

# Anonymous volume (auto-generated name)
docker run -d -v /var/lib/postgresql/data postgres:16

# Read-only volume
docker run -d -v config-vol:/etc/config:ro myapp

# Volume from another container (shared data)
docker run -d --name data-container -v shared-data:/data busybox
docker run -d --volumes-from data-container myapp

# Mount syntax (more explicit, recommended for new projects)
docker run -d \
  --mount type=volume,source=pgdata,target=/var/lib/postgresql/data \
  --mount type=volume,source=pg-config,target=/etc/postgresql,readonly \
  postgres:16
```

### Volume Drivers

```bash
# Local driver with NFS
docker volume create \
  --driver local \
  --opt type=nfs \
  --opt o=addr=192.168.1.100,rw,nfsvers=4 \
  --opt device=:/exports/data \
  nfs-data

# Local driver with specific filesystem
docker volume create \
  --driver local \
  --opt type=ext4 \
  --opt device=/dev/sdb1 \
  ssd-volume

# Third-party drivers (examples):
# - REX-Ray (multi-cloud)
# - Portworx (distributed storage)
# - NetApp Trident
# - AWS EBS (via plugin)
```

---

## Bind Mounts

Direct mapping of host directory/file into container.

```bash
# Bind mount directory
docker run -d \
  -v /host/path/data:/container/path/data \
  myapp

# Bind mount single file
docker run -d \
  -v /host/config/nginx.conf:/etc/nginx/nginx.conf:ro \
  nginx

# Mount syntax (explicit)
docker run -d \
  --mount type=bind,source=/host/path,target=/container/path,readonly \
  myapp

# Current directory (development)
docker run -d \
  -v $(pwd):/app \
  -v /app/node_modules \  # Anonymous volume to preserve node_modules
  node:20-alpine npm run dev
```

### Bind Mount Propagation

```bash
# rprivate (default): No propagation
docker run --mount type=bind,source=/data,target=/data,bind-propagation=rprivate myapp

# rshared: Mounts propagate in both directions
docker run --mount type=bind,source=/data,target=/data,bind-propagation=rshared myapp

# rslave: Mounts propagate from host to container only
docker run --mount type=bind,source=/data,target=/data,bind-propagation=rslave myapp
```

### Bind Mount vs Volume

| Aspect | Volume | Bind Mount |
|--------|--------|------------|
| Location | Docker manages (`/var/lib/docker/volumes/`) | Anywhere on host |
| Portability | Works on any Docker host | Depends on host path |
| Pre-population | Docker copies image data to volume | No (host content shown) |
| Permissions | Docker manages | Host filesystem permissions |
| Backup | `docker volume` commands | Standard file tools |
| Best for | Production data | Development, config files |

---

## tmpfs Mounts

RAM-backed filesystem — never written to disk, lost when container stops.

```bash
# tmpfs mount
docker run -d \
  --tmpfs /tmp:size=100m,mode=1777 \
  --tmpfs /run:size=10m \
  myapp

# Mount syntax
docker run -d \
  --mount type=tmpfs,target=/tmp,tmpfs-size=100m,tmpfs-mode=1777 \
  myapp
```

**Use cases:**
- Sensitive data (secrets, tokens) that shouldn't persist
- Temporary files that don't need disk I/O
- `/tmp` in read-only containers
- High-performance scratch space

---

## Volume Backup & Restore

### Backup

```bash
# Backup volume to tar file
docker run --rm \
  -v pgdata:/source:ro \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/pgdata-$(date +%Y%m%d).tar.gz -C /source .

# Backup with specific container's volumes
docker run --rm \
  --volumes-from postgres \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/postgres-data.tar.gz -C /var/lib/postgresql/data .
```

### Restore

```bash
# Restore volume from tar
docker volume create pgdata-restored

docker run --rm \
  -v pgdata-restored:/target \
  -v $(pwd)/backups:/backup:ro \
  alpine tar xzf /backup/pgdata-20240115.tar.gz -C /target
```

### Copy Between Volumes

```bash
# Clone a volume
docker volume create pgdata-clone

docker run --rm \
  -v pgdata:/source:ro \
  -v pgdata-clone:/target \
  alpine sh -c "cp -a /source/. /target/"
```

---

## Storage Drivers

Storage drivers handle how image layers and container writable layers are stored.

### overlay2 (Default, Recommended)

```bash
# Check current storage driver
docker info | grep "Storage Driver"

# overlay2 structure
/var/lib/docker/overlay2/
├── <layer-id>/
│   ├── diff/        # Layer content
│   ├── link         # Shortened layer ID
│   ├── lower        # Parent layers
│   ├── merged/      # Union mount (container only)
│   └── work/        # OverlayFS work directory
└── l/               # Symlinks for shortened IDs
```

### Storage Driver Comparison

| Driver | Backing FS | Performance | Stability |
|--------|-----------|-------------|-----------|
| overlay2 | xfs, ext4 | Excellent | Production-ready |
| btrfs | btrfs | Good | Good for btrfs hosts |
| zfs | zfs | Good | Good for zfs hosts |
| fuse-overlayfs | Any | Good | Rootless Docker |
| vfs | Any | Poor (no CoW) | Testing only |

### Best Practices for Storage

```json
// /etc/docker/daemon.json
{
  "storage-driver": "overlay2",
  "storage-opts": [
    "overlay2.override_kernel_check=true"
  ],
  "data-root": "/mnt/docker-data"  // Move to dedicated disk
}
```

**Filesystem recommendations:**
- Use **xfs** with `d_type=true` (default on modern systems)
- Use dedicated disk/partition for `/var/lib/docker`
- Use SSD for better I/O performance
- Monitor disk usage (`docker system df`)

---

## Disk Usage Management

```bash
# Show disk usage summary
docker system df
# TYPE            TOTAL   ACTIVE  SIZE    RECLAIMABLE
# Images          15      5       5.2GB   3.1GB (59%)
# Containers      8       3       200MB   150MB (75%)
# Local Volumes   10      4       2.1GB   1.5GB (71%)
# Build Cache     -       -       1.8GB   1.8GB

# Detailed breakdown
docker system df -v

# Image sizes
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | sort -k3 -h

# Container sizes (writable layer)
docker ps -s --format "table {{.Names}}\t{{.Size}}"
```

### Cleanup Strategies

```bash
# Remove all unused data (images, containers, networks, volumes)
docker system prune -af --volumes

# Selective cleanup
docker container prune -f          # Stopped containers
docker image prune -af             # All unused images
docker volume prune -f             # Unused volumes
docker network prune -f            # Unused networks
docker builder prune -af           # Build cache

# Remove images older than 24h
docker image prune -af --filter "until=24h"

# Remove images without tag
docker images -f "dangling=true" -q | xargs docker rmi

# Automated cleanup (cron)
# 0 2 * * * docker system prune -af --filter "until=168h" >> /var/log/docker-cleanup.log
```

---

## Volume Permissions

### Common Permission Issues

```bash
# Problem: Container runs as non-root but volume owned by root
# Solution 1: Match UID in Dockerfile
RUN groupadd -r app && useradd -r -g app -u 1000 app
USER app

# Solution 2: Init container to fix permissions
docker run --rm -v mydata:/data alpine chown -R 1000:1000 /data

# Solution 3: Use named volumes (Docker pre-populates with image data)
# If image has /data owned by uid 1000, named volume inherits that
```

### Security Considerations

```bash
# Never mount Docker socket unless absolutely necessary
# ❌ DANGEROUS: Full Docker access = root on host
docker run -v /var/run/docker.sock:/var/run/docker.sock myapp

# ❌ DANGEROUS: Host root filesystem
docker run -v /:/host myapp

# ✅ SAFE: Specific paths, read-only where possible
docker run -v ./config:/etc/app/config:ro myapp
```

---

## Next: [Docker Compose →](06_Docker_Compose.md)
