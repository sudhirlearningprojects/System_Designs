# 1. Core Concepts & Architecture

## What is Docker?

Docker is a platform for building, shipping, and running applications in isolated environments called **containers**. Containers package an application with all its dependencies, ensuring consistent behavior across environments.

**Containers vs VMs:**

```
┌─────────────────────────────┐    ┌─────────────────────────────┐
│       VIRTUAL MACHINES       │    │         CONTAINERS           │
├─────────────────────────────┤    ├─────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐  │    │  ┌─────┐ ┌─────┐ ┌─────┐  │
│  │App A│ │App B│ │App C│  │    │  │App A│ │App B│ │App C│  │
│  ├─────┤ ├─────┤ ├─────┤  │    │  ├─────┤ ├─────┤ ├─────┤  │
│  │Bins │ │Bins │ │Bins │  │    │  │Bins │ │Bins │ │Bins │  │
│  ├─────┤ ├─────┤ ├─────┤  │    │  └──┬──┘ └──┬──┘ └──┬──┘  │
│  │Guest│ │Guest│ │Guest│  │    │     └────────┼────────┘     │
│  │ OS  │ │ OS  │ │ OS  │  │    │        Container Runtime     │
│  └─────┘ └─────┘ └─────┘  │    │         (containerd)        │
│      HYPERVISOR             │    ├─────────────────────────────┤
├─────────────────────────────┤    │         HOST OS (Linux)      │
│         HOST OS              │    ├─────────────────────────────┤
├─────────────────────────────┤    │         HARDWARE             │
│         HARDWARE             │    └─────────────────────────────┘
└─────────────────────────────┘
   ~GBs per VM, minutes boot        ~MBs per container, ms boot
```

| Aspect | VM | Container |
|--------|-----|-----------|
| Isolation | Full OS kernel | Shared kernel, namespace isolation |
| Size | GBs | MBs |
| Boot time | Minutes | Milliseconds |
| Performance | ~5-10% overhead | Near-native |
| Density | 10-20 per host | 100s-1000s per host |

---

## Docker Engine Architecture

### Component Stack

```
┌─────────────────────────────────────────────────────────┐
│                    DOCKER CLI                             │
│              (docker build, run, push, etc.)              │
└────────────────────────┬────────────────────────────────┘
                         │ /var/run/docker.sock (Unix socket)
                         │ or TCP :2376 (remote)
┌────────────────────────▼────────────────────────────────┐
│                    DOCKER DAEMON (dockerd)                │
│                                                          │
│  ┌──────────┐ ┌──────────┐ ┌────────┐ ┌─────────────┐ │
│  │  Image   │ │ Network  │ │ Volume │ │   BuildKit  │ │
│  │ Manager  │ │ Manager  │ │Manager │ │  (builder)  │ │
│  └──────────┘ └──────────┘ └────────┘ └─────────────┘ │
└────────────────────────┬────────────────────────────────┘
                         │ gRPC
┌────────────────────────▼────────────────────────────────┐
│                    containerd                             │
│         (container lifecycle, image management)           │
│                                                          │
│  ┌──────────────┐  ┌───────────┐  ┌─────────────────┐  │
│  │   Content    │  │ Snapshots │  │    Tasks         │  │
│  │   Store      │  │ (layers)  │  │ (running ctrs)  │  │
│  └──────────────┘  └───────────┘  └─────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    containerd-shim                        │
│         (per-container process, allows daemon restart)    │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    runc (OCI Runtime)                     │
│         (creates namespaces, cgroups, starts process)    │
└─────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Role |
|-----------|------|
| **Docker CLI** | User interface, sends commands to daemon |
| **dockerd** | Daemon managing images, networks, volumes, builds |
| **containerd** | Industry-standard container runtime (CNCF graduated) |
| **containerd-shim** | Keeps container running if containerd restarts |
| **runc** | Low-level OCI runtime that creates containers |
| **BuildKit** | Next-gen image builder (parallel, caching, secrets) |

---

## Linux Primitives (How Containers Work)

Containers are NOT virtual machines. They are isolated processes using Linux kernel features.

### 1. Namespaces (Isolation)

Namespaces limit what a process can **see**.

| Namespace | Isolates | Flag |
|-----------|----------|------|
| **PID** | Process IDs (container sees PID 1) | `CLONE_NEWPID` |
| **NET** | Network stack (interfaces, IPs, ports) | `CLONE_NEWNET` |
| **MNT** | Filesystem mount points | `CLONE_NEWNS` |
| **UTS** | Hostname and domain name | `CLONE_NEWUTS` |
| **IPC** | Inter-process communication | `CLONE_NEWIPC` |
| **USER** | User and group IDs (uid/gid mapping) | `CLONE_NEWUSER` |
| **CGROUP** | Cgroup root directory | `CLONE_NEWCGROUP` |
| **TIME** | System clocks (Linux 5.6+) | `CLONE_NEWTIME` |

```bash
# View namespaces of a container process
ls -la /proc/<PID>/ns/

# Run command in container's namespace
nsenter --target <PID> --mount --uts --ipc --net --pid -- /bin/sh
```

### 2. Cgroups (Resource Limits)

Cgroups limit what a process can **use**.

| Resource | Control |
|----------|---------|
| **CPU** | Shares, quota, pinning |
| **Memory** | Limit, swap, OOM priority |
| **I/O** | Bandwidth, IOPS limits |
| **PIDs** | Max number of processes |
| **Network** | Bandwidth (via tc) |

```bash
# cgroups v2 hierarchy (modern)
/sys/fs/cgroup/system.slice/docker-<container-id>.scope/
├── cpu.max          # CPU quota (e.g., "100000 100000" = 1 CPU)
├── memory.max       # Memory limit in bytes
├── memory.current   # Current memory usage
├── pids.max         # Max processes
└── io.max           # I/O limits
```

### 3. Union Filesystem (Layered Images)

Images are built from read-only layers stacked on top of each other. Containers add a thin writable layer on top.

```
┌─────────────────────────────────┐
│   Container Layer (R/W)          │  ← Writable, ephemeral
├─────────────────────────────────┤
│   Layer 4: COPY app.jar          │  ← Read-only
├─────────────────────────────────┤
│   Layer 3: RUN apt-get install   │  ← Read-only
├─────────────────────────────────┤
│   Layer 2: ENV JAVA_HOME=...     │  ← Read-only (metadata only)
├─────────────────────────────────┤
│   Layer 1: Base Image (ubuntu)   │  ← Read-only
└─────────────────────────────────┘
```

**Storage Drivers:**
| Driver | Description | Best For |
|--------|-------------|----------|
| **overlay2** | Default, production-ready | Most workloads |
| **btrfs** | Copy-on-write filesystem | btrfs hosts |
| **zfs** | ZFS-based | ZFS hosts |
| **fuse-overlayfs** | FUSE-based overlay | Rootless Docker |

### 4. Seccomp (System Call Filtering)

Restricts which system calls a container can make.

```bash
# Docker's default seccomp profile blocks ~44 syscalls including:
# - mount, umount (filesystem manipulation)
# - reboot, swapon/swapoff (system control)
# - clock_settime (time manipulation)
# - kernel module loading
```

### 5. Capabilities (Fine-Grained Privileges)

Instead of full root, containers get specific capabilities.

```bash
# Default capabilities granted:
# CAP_CHOWN, CAP_DAC_OVERRIDE, CAP_FSETID, CAP_FOWNER,
# CAP_MKNOD, CAP_NET_RAW, CAP_SETGID, CAP_SETUID,
# CAP_SETFCAP, CAP_SETPCAP, CAP_NET_BIND_SERVICE,
# CAP_SYS_CHROOT, CAP_KILL, CAP_AUDIT_WRITE

# Drop all, add only what's needed
docker run --cap-drop=ALL --cap-add=NET_BIND_SERVICE myapp
```

---

## OCI Standards

The **Open Container Initiative** defines industry standards:

| Standard | Purpose | Spec |
|----------|---------|------|
| **OCI Image Spec** | Image format (layers, config, manifest) | How images are built and stored |
| **OCI Runtime Spec** | Container runtime behavior | How containers are created and run |
| **OCI Distribution Spec** | Registry API | How images are pushed/pulled |

This means images built with Docker work with Podman, containerd, CRI-O, etc.

### Image Manifest (OCI)

```json
{
  "schemaVersion": 2,
  "mediaType": "application/vnd.oci.image.manifest.v1+json",
  "config": {
    "mediaType": "application/vnd.oci.image.config.v1+json",
    "digest": "sha256:abc123...",
    "size": 7023
  },
  "layers": [
    {
      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
      "digest": "sha256:def456...",
      "size": 32654
    }
  ]
}
```

### Multi-Platform Images (Manifest List)

```json
{
  "schemaVersion": 2,
  "mediaType": "application/vnd.oci.image.index.v1+json",
  "manifests": [
    {
      "mediaType": "application/vnd.oci.image.manifest.v1+json",
      "digest": "sha256:amd64...",
      "platform": { "architecture": "amd64", "os": "linux" }
    },
    {
      "mediaType": "application/vnd.oci.image.manifest.v1+json",
      "digest": "sha256:arm64...",
      "platform": { "architecture": "arm64", "os": "linux" }
    }
  ]
}
```

---

## Docker Objects

### Images
- Read-only template for creating containers
- Built from Dockerfile instructions
- Composed of layers (each instruction = layer)
- Identified by `repository:tag` or `sha256:digest`

### Containers
- Running instance of an image
- Isolated process with its own filesystem, network, PID space
- Ephemeral by default (data lost when removed)
- Can be stopped, started, restarted, removed

### Volumes
- Persistent storage managed by Docker
- Survive container removal
- Can be shared between containers

### Networks
- Isolated network environments
- Containers on same network can communicate by name
- Multiple network drivers (bridge, host, overlay, macvlan)

---

## Docker Editions & Versions

| Edition | Use Case |
|---------|----------|
| **Docker Engine (CE)** | Free, open-source, production-ready |
| **Docker Desktop** | Local development (Mac/Windows/Linux GUI) |
| **Mirantis Container Runtime** | Enterprise (formerly Docker EE) |

**Current Version (2024):** Docker Engine 25.x, Docker Compose v2.x

**Key Recent Changes:**
- Docker Compose v2 (Go rewrite, `docker compose` not `docker-compose`)
- BuildKit as default builder
- Containerd image store (experimental)
- Wasm container support (beta)
- Docker Init (generate Dockerfile/compose for projects)
- Docker Scout (vulnerability scanning)

---

## Installation

### Linux (Ubuntu/Debian)

```bash
# Remove old versions
sudo apt-get remove docker docker-engine docker.io containerd runc

# Install prerequisites
sudo apt-get update
sudo apt-get install ca-certificates curl gnupg

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Add repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Post-install: run without sudo
sudo usermod -aG docker $USER
newgrp docker
```

### Verify Installation

```bash
docker version
docker info
docker run hello-world
```

---

## Docker Daemon Configuration

```json
// /etc/docker/daemon.json
{
  "storage-driver": "overlay2",
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "default-address-pools": [
    { "base": "172.17.0.0/12", "size": 24 }
  ],
  "dns": ["8.8.8.8", "8.8.4.4"],
  "live-restore": true,
  "userland-proxy": false,
  "experimental": false,
  "metrics-addr": "0.0.0.0:9323",
  "default-ulimits": {
    "nofile": { "Name": "nofile", "Hard": 65536, "Soft": 65536 }
  },
  "insecure-registries": [],
  "registry-mirrors": ["https://mirror.gcr.io"]
}
```

**Key Options:**
| Option | Purpose |
|--------|---------|
| `live-restore` | Containers keep running during daemon restart |
| `userland-proxy` | Disable for better performance (use iptables) |
| `metrics-addr` | Expose Prometheus metrics |
| `log-driver` | Default logging driver for all containers |
| `storage-driver` | Filesystem driver for image layers |

---

## Next: [Images & Dockerfile →](02_Images_and_Dockerfile.md)
