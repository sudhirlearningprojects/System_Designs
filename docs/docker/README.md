# Docker Complete Guide

A comprehensive, production-focused guide to Docker — from fundamentals to advanced patterns.

## 📚 Documentation Index

| # | Document | Description |
|---|----------|-------------|
| 1 | [Core Concepts & Architecture](01_Core_Concepts.md) | Engine architecture, namespaces, cgroups, OCI, containerd |
| 2 | [Images & Dockerfile](02_Images_and_Dockerfile.md) | Multi-stage builds, layer caching, best practices, BuildKit |
| 3 | [Containers & Runtime](03_Containers_and_Runtime.md) | Lifecycle, resource limits, exec, health checks, restart policies |
| 4 | [Networking](04_Networking.md) | Bridge, host, overlay, macvlan, DNS, port mapping |
| 5 | [Storage & Volumes](05_Storage_and_Volumes.md) | Volumes, bind mounts, tmpfs, storage drivers, backup |
| 6 | [Docker Compose](06_Docker_Compose.md) | Multi-container apps, profiles, secrets, depends_on, scaling |
| 7 | [Security](07_Security.md) | Rootless, user namespaces, seccomp, AppArmor, scanning |
| 8 | [Production Patterns](08_Production_Patterns.md) | Logging, monitoring, CI/CD, registries, orchestration |
| 9 | [Troubleshooting & Performance](09_Troubleshooting.md) | Debugging, performance tuning, common issues, cleanup |

## 🎯 Who Is This For?

- Developers containerizing applications for the first time
- DevOps engineers building production container pipelines
- SREs optimizing container performance and security
- Teams migrating from VMs to containers

## 🏗️ Docker Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      DOCKER CLIENT                            │
│              docker build | run | push | pull                 │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API (unix socket / TCP)
┌──────────────────────────▼──────────────────────────────────┐
│                      DOCKER DAEMON (dockerd)                  │
│         Image management, networking, volumes, API           │
├─────────────────────────────────────────────────────────────┤
│                      containerd                              │
│         Container lifecycle, image pull/push, snapshots      │
├─────────────────────────────────────────────────────────────┤
│                      runc (OCI runtime)                       │
│         Create and run containers using Linux primitives     │
├─────────────────────────────────────────────────────────────┤
│                      LINUX KERNEL                             │
│         Namespaces │ Cgroups │ Union FS │ Seccomp │ LSM     │
└─────────────────────────────────────────────────────────────┘
```

## ⚡ Quick Reference

```bash
# Build
docker build -t myapp:1.0 .
docker build --target production -t myapp:1.0 .

# Run
docker run -d --name app -p 8080:8080 --restart unless-stopped myapp:1.0

# Compose
docker compose up -d
docker compose logs -f app
docker compose down -v

# Debug
docker exec -it <container> /bin/sh
docker logs --tail 100 -f <container>
docker stats

# Cleanup
docker system prune -af --volumes
```
