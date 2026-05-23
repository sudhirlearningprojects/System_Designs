# 8. Production Patterns

## Logging

### Logging Architecture

```
┌──────────────────────────────────────────────────────────┐
│  Container stdout/stderr                                  │
│         │                                                 │
│         ▼                                                 │
│  Docker Logging Driver                                    │
│  (json-file, fluentd, syslog, awslogs, etc.)            │
│         │                                                 │
│         ▼                                                 │
│  Log Aggregator (Loki, ELK, CloudWatch)                  │
│         │                                                 │
│         ▼                                                 │
│  Dashboard (Grafana, Kibana)                             │
└──────────────────────────────────────────────────────────┘
```

### Logging Drivers

| Driver | Destination | Use Case |
|--------|-------------|----------|
| `json-file` | Local JSON files (default) | Development, small deployments |
| `local` | Optimized local storage | Better performance than json-file |
| `fluentd` | Fluentd collector | Production (flexible routing) |
| `syslog` | Syslog daemon | Traditional infrastructure |
| `awslogs` | CloudWatch Logs | AWS deployments |
| `gcplogs` | Google Cloud Logging | GCP deployments |
| `splunk` | Splunk HTTP Event Collector | Enterprise |
| `none` | Discard all logs | High-throughput, external logging |

### Configuration

```json
// /etc/docker/daemon.json (global default)
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "5",
    "compress": "true",
    "labels": "service,environment",
    "tag": "{{.Name}}/{{.ID}}"
  }
}
```

```bash
# Per-container override
docker run -d \
  --log-driver json-file \
  --log-opt max-size=50m \
  --log-opt max-file=3 \
  --log-opt tag="{{.Name}}" \
  myapp

# AWS CloudWatch
docker run -d \
  --log-driver awslogs \
  --log-opt awslogs-region=us-east-1 \
  --log-opt awslogs-group=/ecs/payment-service \
  --log-opt awslogs-stream-prefix=payment \
  --log-opt awslogs-create-group=true \
  myapp

# Fluentd
docker run -d \
  --log-driver fluentd \
  --log-opt fluentd-address=localhost:24224 \
  --log-opt tag="docker.{{.Name}}" \
  --log-opt fluentd-async=true \
  --log-opt fluentd-buffer-limit=1048576 \
  myapp
```

### Structured Logging Best Practices

```bash
# Application should log to stdout/stderr in JSON format
# ❌ BAD
echo "Error processing payment for user 123"

# ✅ GOOD
echo '{"level":"error","msg":"Payment processing failed","userId":"123","paymentId":"pay-456","error":"timeout","timestamp":"2024-01-15T10:30:00Z"}'
```

```yaml
# Docker Compose with logging
services:
  app:
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
        labels: "service"
    labels:
      service: payment
```

---

## Monitoring

### Container Metrics

```bash
# Built-in stats
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

# Prometheus metrics endpoint
# /etc/docker/daemon.json
{
  "metrics-addr": "0.0.0.0:9323",
  "experimental": true
}
```

### Prometheus + cAdvisor + Grafana

```yaml
# docker-compose.monitoring.yml
services:
  prometheus:
    image: prom/prometheus:v2.48.0
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'

  cadvisor:
    image: gcr.io/cadvisor/cadvisor:v0.47.2
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
      - /dev/disk/:/dev/disk:ro
    ports:
      - "8080:8080"
    privileged: true
    devices:
      - /dev/kmsg

  grafana:
    image: grafana/grafana:10.2.0
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

  node-exporter:
    image: prom/node-exporter:v1.7.0
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--path.rootfs=/rootfs'

volumes:
  prometheus-data:
  grafana-data:
```

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'docker'
    static_configs:
      - targets: ['host.docker.internal:9323']

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']

  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
```

### Key Metrics to Monitor

| Metric | Alert Threshold | Source |
|--------|----------------|--------|
| CPU usage | > 80% sustained | cAdvisor |
| Memory usage | > 85% of limit | cAdvisor |
| Container restarts | > 3 in 5 min | Docker events |
| Disk usage | > 80% | Node exporter |
| Network errors | > 0.1% | cAdvisor |
| Container health | unhealthy | Docker health check |

---

## CI/CD Pipeline

### GitHub Actions

```yaml
name: Build and Push
on:
  push:
    branches: [main]
    tags: ['v*']

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
      security-events: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,prefix=
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          platforms: linux/amd64,linux/arm64
          cache-from: type=gha
          cache-to: type=gha,mode=max
          build-args: |
            BUILD_DATE=${{ github.event.head_commit.timestamp }}
            GIT_SHA=${{ github.sha }}

      - name: Scan image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
          format: sarif
          output: trivy-results.sarif
          severity: CRITICAL,HIGH

      - name: Upload scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: trivy-results.sarif
```

### Build Cache Strategies

```bash
# Registry cache (share across CI runners)
docker buildx build \
  --cache-from type=registry,ref=registry.company.com/app:cache \
  --cache-to type=registry,ref=registry.company.com/app:cache,mode=max \
  --push -t registry.company.com/app:1.0 .

# GitHub Actions cache
docker buildx build \
  --cache-from type=gha \
  --cache-to type=gha,mode=max \
  --push -t myapp:1.0 .

# Local cache (for local builds)
docker buildx build \
  --cache-from type=local,src=/tmp/.buildx-cache \
  --cache-to type=local,dest=/tmp/.buildx-cache-new,mode=max \
  -t myapp:1.0 .
```

---

## Container Registry

### Private Registry

```yaml
# Self-hosted registry
services:
  registry:
    image: registry:2
    ports:
      - "5000:5000"
    volumes:
      - registry-data:/var/lib/registry
      - ./certs:/certs:ro
      - ./auth:/auth:ro
    environment:
      REGISTRY_HTTP_TLS_CERTIFICATE: /certs/domain.crt
      REGISTRY_HTTP_TLS_KEY: /certs/domain.key
      REGISTRY_AUTH: htpasswd
      REGISTRY_AUTH_HTPASSWD_REALM: Registry Realm
      REGISTRY_AUTH_HTPASSWD_PATH: /auth/htpasswd
      REGISTRY_STORAGE_DELETE_ENABLED: "true"
      REGISTRY_STORAGE_CACHE_BLOBDESCRIPTOR: inmemory

volumes:
  registry-data:
```

### Registry Operations

```bash
# Login
docker login registry.company.com

# Tag and push
docker tag myapp:1.0 registry.company.com/team/myapp:1.0
docker push registry.company.com/team/myapp:1.0

# Pull
docker pull registry.company.com/team/myapp:1.0

# List tags (Registry API)
curl -s https://registry.company.com/v2/team/myapp/tags/list

# Garbage collection (reclaim space)
docker exec registry bin/registry garbage-collect /etc/docker/registry/config.yml
```

### ECR (AWS)

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com

# Create repository
aws ecr create-repository --repository-name myapp --image-scanning-configuration scanOnPush=true

# Push
docker tag myapp:1.0 123456789.dkr.ecr.us-east-1.amazonaws.com/myapp:1.0
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/myapp:1.0

# Lifecycle policy (auto-cleanup)
aws ecr put-lifecycle-policy --repository-name myapp --lifecycle-policy-text '{
  "rules": [{
    "rulePriority": 1,
    "description": "Keep last 10 images",
    "selection": {
      "tagStatus": "any",
      "countType": "imageCountMoreThan",
      "countNumber": 10
    },
    "action": { "type": "expire" }
  }]
}'
```

---

## Container Orchestration Patterns

### Single Host (Docker Compose)

```bash
# Production single-host deployment
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# With Watchtower (auto-update)
docker run -d \
  --name watchtower \
  -v /var/run/docker.sock:/var/run/docker.sock \
  containrrr/watchtower \
  --interval 300 \
  --cleanup \
  --label-enable  # Only update containers with label
```

### Multi-Host (Docker Swarm)

```bash
# Initialize swarm
docker swarm init --advertise-addr 192.168.1.10

# Join workers
docker swarm join --token SWMTKN-... 192.168.1.10:2377

# Deploy stack
docker stack deploy -c docker-compose.yml myapp

# Scale service
docker service scale myapp_web=5

# Rolling update
docker service update --image myapp:2.0 --update-parallelism 2 --update-delay 10s myapp_web
```

### Container-to-Kubernetes Migration Path

```
Docker Compose (dev) → Docker Swarm (simple prod) → Kubernetes (scale)
                    → Kubernetes directly (if team has expertise)
```

---

## Health Check Patterns

### Application Health Endpoints

```bash
# Liveness: Is the process alive?
GET /health/live → 200 OK

# Readiness: Can it serve traffic?
GET /health/ready → 200 OK (checks DB, cache, dependencies)

# Startup: Has it finished initializing?
GET /health/started → 200 OK
```

### Docker Health Check with Dependencies

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health/ready || exit 1
```

### Compose Health Check Orchestration

```yaml
services:
  db:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready"]
      interval: 5s
      retries: 10

  migrations:
    depends_on:
      db:
        condition: service_healthy
    command: flyway migrate
    # Exits after migration

  app:
    depends_on:
      migrations:
        condition: service_completed_successfully
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      start_period: 30s
```

---

## Zero-Downtime Deployment (Single Host)

```bash
#!/bin/bash
# Blue-green deployment with Docker Compose

# Pull new image
docker compose pull app

# Scale up new version alongside old
docker compose up -d --no-deps --scale app=2 --no-recreate app

# Wait for new container to be healthy
sleep 30

# Remove old container
docker compose up -d --no-deps --scale app=1 app

echo "Deployment complete"
```

### With Traefik (Reverse Proxy)

```yaml
services:
  traefik:
    image: traefik:v3.0
    command:
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:80"
    ports:
      - "80:80"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro

  app:
    image: myapp:${VERSION}
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.app.rule=Host(`app.example.com`)"
      - "traefik.http.services.app.loadbalancer.server.port=8080"
      - "traefik.http.services.app.loadbalancer.healthcheck.path=/health"
      - "traefik.http.services.app.loadbalancer.healthcheck.interval=10s"
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
        order: start-first
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 10s
      timeout: 3s
      retries: 3
      start_period: 30s
```

---

## Next: [Troubleshooting & Performance →](09_Troubleshooting.md)
