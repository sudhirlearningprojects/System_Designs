# 2. Images & Dockerfile

## Image Fundamentals

An image is an ordered collection of filesystem layers + metadata that forms the basis for containers.

### Image Naming Convention

```
[registry/][namespace/]repository[:tag|@digest]

Examples:
  ubuntu:22.04                              # Docker Hub official
  nginx:1.25-alpine                         # Docker Hub official with variant
  mycompany/payment-service:3.2.1           # Docker Hub user namespace
  registry.company.com/team/app:v2.0.0      # Private registry
  ghcr.io/org/app@sha256:abc123...          # Pinned by digest (immutable)
```

### Image Layers

```bash
# Inspect image layers
docker image inspect nginx:1.25 --format '{{.RootFS.Layers}}'

# View layer history
docker history nginx:1.25

# Layer sharing between images
IMAGE A: [base] [apt-get] [copy-app-a]
IMAGE B: [base] [apt-get] [copy-app-b]
         ↑ shared layers (stored once on disk)
```

---

## Dockerfile Reference

### Complete Instruction Set

| Instruction | Purpose | Layer? |
|-------------|---------|--------|
| `FROM` | Base image | Yes |
| `RUN` | Execute command | Yes |
| `COPY` | Copy files from build context | Yes |
| `ADD` | Copy + extract archives + URL fetch | Yes |
| `WORKDIR` | Set working directory | Metadata |
| `ENV` | Set environment variable | Metadata |
| `ARG` | Build-time variable | No |
| `EXPOSE` | Document port (informational) | Metadata |
| `VOLUME` | Create mount point | Metadata |
| `USER` | Set user for subsequent instructions | Metadata |
| `CMD` | Default command (overridable) | Metadata |
| `ENTRYPOINT` | Main executable (not easily overridden) | Metadata |
| `LABEL` | Add metadata key-value pairs | Metadata |
| `HEALTHCHECK` | Container health check command | Metadata |
| `SHELL` | Override default shell | Metadata |
| `STOPSIGNAL` | Signal to stop container | Metadata |
| `ONBUILD` | Trigger for child images | Metadata |

---

## Multi-Stage Builds

The most important pattern for production images — separate build dependencies from runtime.

### Java (Spring Boot)

```dockerfile
# ============ Stage 1: Build ============
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Cache dependencies (layer caching)
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

# Build application
COPY src ./src
RUN ./mvnw package -DskipTests -B && \
    java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ============ Stage 2: Runtime ============
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app

# Copy layered Spring Boot application
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### Node.js

```dockerfile
# ============ Stage 1: Dependencies ============
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production && \
    cp -R node_modules /prod_modules && \
    npm ci  # All deps for building

# ============ Stage 2: Build ============
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ============ Stage 3: Runtime ============
FROM node:20-alpine AS runtime
RUN apk add --no-cache tini
WORKDIR /app

# Non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=deps /prod_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY package.json ./

EXPOSE 3000
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["node", "dist/main.js"]
```

### Go

```dockerfile
# ============ Stage 1: Build ============
FROM golang:1.22-alpine AS builder
RUN apk add --no-cache git ca-certificates
WORKDIR /app

# Cache modules
COPY go.mod go.sum ./
RUN go mod download

# Build static binary
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build -ldflags="-w -s -X main.version=1.0.0" \
    -o /app/server ./cmd/server

# ============ Stage 2: Runtime ============
FROM scratch
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=builder /app/server /server

EXPOSE 8080
ENTRYPOINT ["/server"]
```

### Python

```dockerfile
# ============ Stage 1: Build ============
FROM python:3.12-slim AS builder
WORKDIR /app

RUN pip install --no-cache-dir poetry && \
    poetry config virtualenvs.in-project true

COPY pyproject.toml poetry.lock ./
RUN poetry install --only main --no-interaction --no-ansi

COPY . .

# ============ Stage 2: Runtime ============
FROM python:3.12-slim AS runtime
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app
USER app

COPY --from=builder /app/.venv ./.venv
COPY --from=builder /app/src ./src

ENV PATH="/app/.venv/bin:$PATH"
EXPOSE 8000
CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

## BuildKit

Docker's modern build engine (default since Docker 23.0).

### BuildKit Features

```bash
# Enable BuildKit (if not default)
export DOCKER_BUILDKIT=1

# Or in daemon.json
{ "features": { "buildkit": true } }
```

### Build Secrets (Never Leak in Layers)

```dockerfile
# syntax=docker/dockerfile:1

FROM node:20-alpine
WORKDIR /app
COPY package.json ./

# Secret is available during build but NOT stored in image layer
RUN --mount=type=secret,id=npmrc,target=/root/.npmrc \
    npm ci

COPY . .
RUN npm run build
```

```bash
# Pass secret at build time
docker build --secret id=npmrc,src=$HOME/.npmrc -t myapp .
```

### SSH Forwarding

```dockerfile
# syntax=docker/dockerfile:1

FROM alpine
RUN apk add --no-cache git openssh-client

# Use host SSH agent for private repos
RUN --mount=type=ssh \
    git clone git@github.com:company/private-repo.git /app
```

```bash
docker build --ssh default -t myapp .
```

### Cache Mounts (Faster Builds)

```dockerfile
# syntax=docker/dockerfile:1

FROM golang:1.22 AS builder
WORKDIR /app
COPY go.mod go.sum ./

# Cache Go modules between builds
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download

COPY . .
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go build -o /app/server ./cmd/server
```

```dockerfile
# Maven cache
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

# apt cache
RUN --mount=type=cache,target=/var/cache/apt \
    --mount=type=cache,target=/var/lib/apt \
    apt-get update && apt-get install -y curl
```

### Bind Mounts (Build Context)

```dockerfile
# Mount source without COPY (useful for large contexts)
RUN --mount=type=bind,source=package.json,target=package.json \
    --mount=type=bind,source=package-lock.json,target=package-lock.json \
    npm ci
```

---

## Layer Caching Strategy

### Cache-Friendly Ordering

```dockerfile
# ❌ BAD: Any source change invalidates dependency cache
COPY . .
RUN npm ci && npm run build

# ✅ GOOD: Dependencies cached separately from source
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build
```

### .dockerignore

```gitignore
# .dockerignore
.git
.gitignore
node_modules
dist
build
target
*.md
.env*
.vscode
.idea
docker-compose*.yml
Dockerfile*
**/*.test.js
**/*.spec.ts
coverage
.nyc_output
```

---

## ENTRYPOINT vs CMD

```dockerfile
# ENTRYPOINT: the executable (hard to override)
# CMD: default arguments (easily overridden)

# Pattern 1: Fixed executable, configurable args
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=production"]
# docker run myapp                          → java -jar app.jar --spring.profiles.active=production
# docker run myapp --server.port=9090       → java -jar app.jar --server.port=9090

# Pattern 2: Shell script entrypoint (for init logic)
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["start"]

# Pattern 3: Direct command (simple apps)
CMD ["nginx", "-g", "daemon off;"]
```

### Exec Form vs Shell Form

```dockerfile
# Exec form (preferred) — PID 1 is your process, receives signals
CMD ["node", "server.js"]
ENTRYPOINT ["java", "-jar", "app.jar"]

# Shell form — PID 1 is /bin/sh, your process is child (won't get SIGTERM!)
CMD node server.js
ENTRYPOINT java -jar app.jar
```

**Always use exec form in production** — ensures proper signal handling for graceful shutdown.

---

## ARG vs ENV

```dockerfile
# ARG: build-time only, not in final image
ARG JAVA_VERSION=21
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

ARG BUILD_DATE
ARG GIT_SHA
LABEL build-date=$BUILD_DATE
LABEL git-sha=$GIT_SHA

# ENV: persists in image and running container
ENV APP_PORT=8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"

CMD java $JAVA_OPTS -jar app.jar
```

```bash
docker build --build-arg JAVA_VERSION=17 --build-arg GIT_SHA=$(git rev-parse HEAD) .
```

---

## HEALTHCHECK

```dockerfile
# HTTP health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# TCP check (no curl available)
HEALTHCHECK --interval=10s --timeout=2s --retries=3 \
  CMD nc -z localhost 8080 || exit 1

# Custom script
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD /app/healthcheck.sh
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--interval` | 30s | Time between checks |
| `--timeout` | 30s | Max time for check to complete |
| `--start-period` | 0s | Grace period for startup |
| `--retries` | 3 | Consecutive failures before unhealthy |

---

## Multi-Platform Builds

Build images for multiple architectures (amd64, arm64).

```bash
# Create builder with multi-platform support
docker buildx create --name multiarch --driver docker-container --use

# Build and push for multiple platforms
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag registry.company.com/app:1.0 \
  --push .

# Inspect manifest
docker buildx imagetools inspect registry.company.com/app:1.0
```

```dockerfile
# Platform-aware Dockerfile
FROM --platform=$BUILDPLATFORM golang:1.22 AS builder
ARG TARGETPLATFORM
ARG TARGETOS
ARG TARGETARCH

WORKDIR /app
COPY . .
RUN GOOS=$TARGETOS GOARCH=$TARGETARCH go build -o /server

FROM --platform=$TARGETPLATFORM alpine:3.19
COPY --from=builder /server /server
CMD ["/server"]
```

---

## Image Best Practices

### 1. Use Minimal Base Images

| Base Image | Size | Use Case |
|-----------|------|----------|
| `scratch` | 0 MB | Static Go binaries |
| `alpine:3.19` | 7 MB | General minimal |
| `distroless/java21` | 200 MB | Java (no shell, no package manager) |
| `distroless/static` | 2 MB | Static binaries (with CA certs) |
| `ubuntu:22.04` | 77 MB | When you need apt packages |
| `debian:12-slim` | 74 MB | Debian minimal |

### 2. Pin Versions

```dockerfile
# ❌ BAD
FROM node:latest
RUN apt-get install -y curl

# ✅ GOOD
FROM node:20.11.0-alpine3.19
RUN apk add --no-cache curl=8.5.0-r0
```

### 3. Minimize Layers

```dockerfile
# ❌ BAD: 4 layers
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y wget
RUN rm -rf /var/lib/apt/lists/*

# ✅ GOOD: 1 layer
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      curl \
      wget && \
    rm -rf /var/lib/apt/lists/*
```

### 4. Order by Change Frequency

```dockerfile
# Least changing → Most changing
FROM base-image          # Rarely changes
COPY requirements.txt . # Changes when deps change
RUN pip install -r ...   # Cached if requirements unchanged
COPY . .                 # Changes every commit
RUN build                # Rebuilds every time
```

### 5. Use .dockerignore

### 6. Don't Run as Root

```dockerfile
RUN addgroup -S app && adduser -S app -G app
USER app
```

### 7. Label Images

```dockerfile
LABEL org.opencontainers.image.source="https://github.com/company/app"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.created="2024-01-15T10:00:00Z"
LABEL org.opencontainers.image.authors="team@company.com"
```

---

## Image Scanning

```bash
# Docker Scout (built-in)
docker scout cves myapp:1.0
docker scout recommendations myapp:1.0

# Trivy
trivy image myapp:1.0
trivy image --severity HIGH,CRITICAL myapp:1.0

# Grype
grype myapp:1.0
```

---

## Image Size Optimization

```bash
# Check image size
docker images myapp
docker image inspect myapp:1.0 --format '{{.Size}}'

# Analyze layers with dive
dive myapp:1.0
```

**Size reduction techniques:**
1. Multi-stage builds (don't ship build tools)
2. Alpine/distroless base images
3. Remove package manager cache (`rm -rf /var/lib/apt/lists/*`)
4. Use `--no-install-recommends`
5. Combine RUN commands
6. Use `.dockerignore`
7. Strip binaries (`-ldflags="-w -s"` for Go)

---

## Next: [Containers & Runtime →](03_Containers_and_Runtime.md)
