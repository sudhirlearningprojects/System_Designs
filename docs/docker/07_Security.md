# 7. Security

## Docker Security Model

```
┌─────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                            │
├─────────────────────────────────────────────────────────────┤
│  Layer 6: Supply Chain    │ Image signing, scanning, SBOM   │
├───────────────────────────┼─────────────────────────────────┤
│  Layer 5: Runtime         │ Seccomp, AppArmor, read-only FS │
├───────────────────────────┼─────────────────────────────────┤
│  Layer 4: Container       │ Non-root, capabilities, no-new-priv │
├───────────────────────────┼─────────────────────────────────┤
│  Layer 3: Network         │ Network isolation, no ICC       │
├───────────────────────────┼─────────────────────────────────┤
│  Layer 2: Daemon          │ Rootless mode, TLS, auth        │
├───────────────────────────┼─────────────────────────────────┤
│  Layer 1: Host            │ Kernel hardening, updates       │
└─────────────────────────────────────────────────────────────┘
```

---

## Running as Non-Root

### In Dockerfile

```dockerfile
FROM node:20-alpine

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --chown=app:app . .
RUN npm ci --only=production

# Switch to non-root user
USER app

EXPOSE 3000
CMD ["node", "server.js"]
```

### At Runtime

```bash
# Override user at runtime
docker run --user 1000:1000 myapp
docker run --user nobody myapp

# Verify
docker exec myapp whoami
docker exec myapp id
```

### Why Non-Root Matters

```bash
# Root in container = root on host (without user namespaces)
# If container escapes, attacker has root access to host

# ❌ DANGEROUS: Root container with host mount
docker run -v /:/host -u root myapp
# Container can read/write entire host filesystem

# ✅ SAFE: Non-root with minimal access
docker run --user 1000:1000 --read-only myapp
```

---

## Rootless Docker

Run the entire Docker daemon without root privileges.

### Setup

```bash
# Install rootless Docker
dockerd-rootless-setuptool.sh install

# Set environment
export PATH=/home/user/bin:$PATH
export DOCKER_HOST=unix:///run/user/1000/docker.sock

# Start rootless daemon
systemctl --user start docker
systemctl --user enable docker

# Verify
docker info | grep "Root Dir"
# /home/user/.local/share/docker
```

### Rootless Limitations

| Feature | Rootless Support |
|---------|-----------------|
| Port < 1024 | ❌ (use port > 1024 or `net.ipv4.ip_unprivileged_port_start=0`) |
| Host networking | ❌ |
| Overlay network | ❌ (use slirp4netns) |
| AppArmor | ❌ |
| Cgroup v1 | ❌ (requires cgroup v2) |
| Privileged containers | ❌ |

---

## User Namespaces

Map container root (UID 0) to unprivileged host UID.

```json
// /etc/docker/daemon.json
{
  "userns-remap": "default"
}
```

```bash
# This creates /etc/subuid and /etc/subgid entries:
# dockremap:100000:65536

# Container UID 0 → Host UID 100000
# Container UID 1 → Host UID 100001
# ...

# Even if process is "root" inside container,
# it's unprivileged UID 100000 on host
```

---

## Capabilities

Linux capabilities split root privileges into granular units.

### Default Capabilities (Docker grants)

```
CAP_CHOWN, CAP_DAC_OVERRIDE, CAP_FSETID, CAP_FOWNER,
CAP_MKNOD, CAP_NET_RAW, CAP_SETGID, CAP_SETUID,
CAP_SETFCAP, CAP_SETPCAP, CAP_NET_BIND_SERVICE,
CAP_SYS_CHROOT, CAP_KILL, CAP_AUDIT_WRITE
```

### Hardened Container

```bash
# Drop ALL capabilities, add only what's needed
docker run \
  --cap-drop ALL \
  --cap-add NET_BIND_SERVICE \
  myapp

# Common capabilities needed:
# NET_BIND_SERVICE  — Bind to ports < 1024
# CHOWN             — Change file ownership
# SETUID/SETGID    — Switch user (init systems)
# SYS_PTRACE       — Debugging (strace, gdb)
# NET_ADMIN         — Network configuration
# SYS_ADMIN         — Mount, namespace operations (avoid!)
```

### Privileged Mode (AVOID)

```bash
# ❌ NEVER in production — gives ALL capabilities + device access
docker run --privileged myapp

# This grants:
# - All Linux capabilities
# - Access to all host devices (/dev/*)
# - Disables seccomp, AppArmor
# - Can mount host filesystems
# - Can load kernel modules
# - Essentially root on host
```

---

## Seccomp Profiles

Restrict which system calls a container can make.

### Default Profile

Docker's default seccomp profile blocks ~44 dangerous syscalls:
- `mount`, `umount2` — filesystem manipulation
- `reboot` — system reboot
- `swapon`, `swapoff` — swap management
- `init_module`, `delete_module` — kernel modules
- `clock_settime` — time manipulation
- `acct` — process accounting
- `settimeofday` — system time

### Custom Seccomp Profile

```json
{
  "defaultAction": "SCMP_ACT_ERRNO",
  "defaultErrnoRet": 1,
  "architectures": ["SCMP_ARCH_X86_64"],
  "syscalls": [
    {
      "names": [
        "read", "write", "open", "close", "stat", "fstat",
        "mmap", "mprotect", "munmap", "brk", "ioctl",
        "access", "pipe", "select", "sched_yield",
        "socket", "connect", "accept", "sendto", "recvfrom",
        "bind", "listen", "getsockname", "getpeername",
        "clone", "fork", "execve", "exit", "wait4",
        "kill", "getpid", "getuid", "getgid",
        "openat", "newfstatat", "epoll_create1",
        "epoll_ctl", "epoll_wait", "futex"
      ],
      "action": "SCMP_ACT_ALLOW"
    }
  ]
}
```

```bash
# Use custom profile
docker run --security-opt seccomp=./custom-seccomp.json myapp

# Disable seccomp (NOT recommended)
docker run --security-opt seccomp=unconfined myapp

# Generate profile from container activity (using OCI tools)
# Run container, trace syscalls, generate minimal profile
```

---

## AppArmor

Mandatory Access Control (MAC) — restricts file access, network, capabilities.

```bash
# Check AppArmor status
aa-status

# Docker's default AppArmor profile: docker-default
# Denies:
# - Writing to /proc (except /proc/self)
# - Mounting filesystems
# - Accessing /sys/firmware
# - Modifying network interfaces

# Use custom profile
docker run --security-opt apparmor=my-custom-profile myapp

# Disable AppArmor (NOT recommended)
docker run --security-opt apparmor=unconfined myapp
```

### Custom AppArmor Profile

```
#include <tunables/global>

profile docker-custom flags=(attach_disconnected,mediate_deleted) {
  #include <abstractions/base>

  # Deny all file writes except specific paths
  deny /etc/** w,
  deny /usr/** w,
  
  # Allow app-specific paths
  /app/** r,
  /tmp/** rw,
  /var/log/app/** rw,
  
  # Network
  network inet tcp,
  network inet udp,
  deny network raw,
  
  # Deny dangerous operations
  deny mount,
  deny ptrace,
  deny signal (send) peer=unconfined,
}
```

---

## No New Privileges

Prevent processes from gaining additional privileges via setuid/setgid binaries.

```bash
docker run --security-opt no-new-privileges:true myapp
```

This prevents:
- setuid binaries from escalating privileges
- Processes from gaining capabilities they don't already have
- Common privilege escalation attacks

---

## Read-Only Filesystem

```bash
docker run \
  --read-only \
  --tmpfs /tmp:size=100m,noexec,nosuid \
  --tmpfs /var/run:size=10m \
  -v app-logs:/var/log/app \
  myapp
```

**Benefits:**
- Prevents malware from writing to filesystem
- Prevents modification of application binaries
- Forces proper use of volumes for state

---

## Docker Content Trust (Image Signing)

```bash
# Enable content trust
export DOCKER_CONTENT_TRUST=1

# Push signed image
docker push registry.company.com/app:1.0
# Automatically signs with Notary

# Pull only signed images
docker pull registry.company.com/app:1.0
# Fails if image is not signed

# Delegate signing
docker trust signer add --key cert.pem developer registry.company.com/app
docker trust sign registry.company.com/app:1.0
```

---

## Image Scanning

### Docker Scout (Built-in)

```bash
# Scan for vulnerabilities
docker scout cves myapp:1.0
docker scout cves --only-severity critical,high myapp:1.0

# Get recommendations
docker scout recommendations myapp:1.0

# Compare images
docker scout compare myapp:1.0 myapp:1.1

# SBOM (Software Bill of Materials)
docker scout sbom myapp:1.0
```

### Trivy

```bash
# Scan image
trivy image myapp:1.0

# Only critical/high
trivy image --severity HIGH,CRITICAL myapp:1.0

# Fail CI if vulnerabilities found
trivy image --exit-code 1 --severity CRITICAL myapp:1.0

# Scan filesystem (Dockerfile context)
trivy fs --security-checks vuln,secret,config .

# Generate SBOM
trivy image --format spdx-json myapp:1.0 > sbom.json
```

### CI/CD Integration

```yaml
# GitHub Actions
- name: Scan image
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: myapp:${{ github.sha }}
    format: sarif
    output: trivy-results.sarif
    severity: CRITICAL,HIGH
    exit-code: 1

- name: Upload scan results
  uses: github/codeql-action/upload-sarif@v2
  with:
    sarif_file: trivy-results.sarif
```

---

## Secrets Management

### Docker Secrets (Compose)

```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt  # Development
  api_key:
    external: true  # Pre-created (Swarm)

services:
  app:
    secrets:
      - db_password
      - api_key
    # Available at /run/secrets/db_password
```

### Build Secrets (Never in Image)

```dockerfile
# syntax=docker/dockerfile:1
RUN --mount=type=secret,id=npmrc,target=/root/.npmrc npm ci
RUN --mount=type=secret,id=ssh_key,target=/root/.ssh/id_rsa git clone ...
```

```bash
docker build --secret id=npmrc,src=.npmrc --secret id=ssh_key,src=~/.ssh/id_rsa .
```

### Environment Variables (Least Secure)

```bash
# ❌ Visible in docker inspect, docker history, process list
docker run -e DB_PASSWORD=secret myapp

# ✅ Better: pass from file
docker run --env-file .env myapp

# ✅ Best: use secrets mount or external secrets manager
```

---

## Network Security

```bash
# Disable inter-container communication
# /etc/docker/daemon.json: { "icc": false }

# Internal network (no internet access)
docker network create --internal backend

# Restrict container to specific network
docker run --network backend myapp
# Cannot reach internet, only other containers on 'backend'
```

---

## Docker Daemon Security

### TLS for Remote Access

```bash
# Generate CA, server, and client certificates
# Then configure daemon:
dockerd \
  --tlsverify \
  --tlscacert=ca.pem \
  --tlscert=server-cert.pem \
  --tlskey=server-key.pem \
  -H=0.0.0.0:2376

# Client connects with:
docker --tlsverify \
  --tlscacert=ca.pem \
  --tlscert=client-cert.pem \
  --tlskey=client-key.pem \
  -H=tcp://docker-host:2376 info
```

### Authorization Plugins

```json
// /etc/docker/daemon.json
{
  "authorization-plugins": ["casbin-authz-plugin"]
}
```

---

## Security Checklist

### Image Build
- [ ] Use minimal base images (alpine, distroless, scratch)
- [ ] Pin base image versions (never `latest`)
- [ ] Multi-stage builds (no build tools in production)
- [ ] Scan images for vulnerabilities
- [ ] Sign images (Docker Content Trust / Cosign)
- [ ] No secrets in Dockerfile or image layers
- [ ] Use `.dockerignore` to exclude sensitive files

### Container Runtime
- [ ] Run as non-root user (`USER` in Dockerfile)
- [ ] Drop all capabilities, add only needed (`--cap-drop ALL`)
- [ ] Enable `no-new-privileges`
- [ ] Read-only root filesystem (`--read-only`)
- [ ] Set resource limits (CPU, memory, PIDs)
- [ ] Use seccomp profile (default or custom)
- [ ] Don't use `--privileged`
- [ ] Don't mount Docker socket

### Host & Daemon
- [ ] Keep Docker updated
- [ ] Use rootless Docker or user namespaces
- [ ] Enable TLS for remote daemon access
- [ ] Disable ICC if not needed
- [ ] Use internal networks for backend services
- [ ] Regular security audits (Docker Bench)

### Docker Bench for Security

```bash
# Run CIS Docker Benchmark checks
docker run --rm --net host --pid host \
  --userns host --cap-add audit_control \
  -v /var/lib:/var/lib:ro \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /etc:/etc:ro \
  docker/docker-bench-security
```

---

## Next: [Production Patterns →](08_Production_Patterns.md)
