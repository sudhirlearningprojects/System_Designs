# 6. Docker Compose

## Overview

Docker Compose defines and runs multi-container applications using a YAML file. Compose V2 is the current standard (`docker compose` command, not the legacy `docker-compose`).

---

## Compose File Reference

### Complete Production Example

```yaml
# docker-compose.yml (Compose Specification)
name: ecommerce-platform

services:
  # ============ Application Services ============
  api-gateway:
    image: registry.company.com/api-gateway:${VERSION:-latest}
    build:
      context: ./services/api-gateway
      dockerfile: Dockerfile
      target: production
      args:
        - BUILD_DATE=${BUILD_DATE}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - JAVA_OPTS=-Xmx512m -Xms256m
    env_file:
      - .env
      - .env.production
    depends_on:
      payment-service:
        condition: service_healthy
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - frontend
      - backend
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: "1.0"
          memory: 1G
        reservations:
          cpus: "0.5"
          memory: 512M
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
        window: 120s
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
    restart: unless-stopped

  payment-service:
    build:
      context: ./services/payment
      target: production
    expose:
      - "8081"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: payments
      REDIS_HOST: redis
      KAFKA_BROKERS: kafka:9092
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - backend
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 512M
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/health"]
      interval: 15s
      timeout: 3s
      retries: 3
      start_period: 30s
    secrets:
      - db_password
      - stripe_api_key
    restart: unless-stopped

  # ============ Infrastructure Services ============
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: payments
      POSTGRES_USER: app
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d:ro
    ports:
      - "127.0.0.1:5432:5432"  # Localhost only
    networks:
      - backend
    deploy:
      resources:
        limits:
          cpus: "2.0"
          memory: 2G
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d payments"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    secrets:
      - db_password
    shm_size: 256m
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    ports:
      - "127.0.0.1:6379:6379"
    networks:
      - backend
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 512M
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    restart: unless-stopped

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - backend
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    restart: unless-stopped

  # ============ Observability ============
  prometheus:
    image: prom/prometheus:v2.48.0
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - monitoring
      - backend
    profiles:
      - monitoring
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.2.0
    environment:
      GF_SECURITY_ADMIN_PASSWORD__FILE: /run/secrets/grafana_password
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/dashboards:/etc/grafana/provisioning/dashboards:ro
    ports:
      - "3000:3000"
    networks:
      - monitoring
    depends_on:
      - prometheus
    profiles:
      - monitoring
    secrets:
      - grafana_password
    restart: unless-stopped

# ============ Volumes ============
volumes:
  pgdata:
    driver: local
  redis-data:
    driver: local
  kafka-data:
    driver: local
  prometheus-data:
    driver: local
  grafana-data:
    driver: local

# ============ Networks ============
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true  # No external access
  monitoring:
    driver: bridge

# ============ Secrets ============
secrets:
  db_password:
    file: ./secrets/db_password.txt
  stripe_api_key:
    file: ./secrets/stripe_key.txt
  grafana_password:
    file: ./secrets/grafana_password.txt
```

---

## Key Compose Features

### depends_on with Health Checks

```yaml
services:
  app:
    depends_on:
      db:
        condition: service_healthy      # Wait for healthy
      redis:
        condition: service_started      # Just wait for start
      migrations:
        condition: service_completed_successfully  # Wait for exit 0
```

### Profiles (Selective Service Startup)

```yaml
services:
  app:
    # No profile = always starts
    image: myapp:1.0

  debug-tools:
    image: nicolaka/netshoot
    profiles:
      - debug

  prometheus:
    image: prom/prometheus
    profiles:
      - monitoring
      - full

  load-test:
    image: grafana/k6
    profiles:
      - testing
```

```bash
# Start only default services
docker compose up -d

# Start with monitoring
docker compose --profile monitoring up -d

# Start with multiple profiles
docker compose --profile monitoring --profile debug up -d
```

### Secrets

```yaml
secrets:
  # File-based (development)
  db_password:
    file: ./secrets/db_password.txt

  # Environment variable (CI/CD)
  api_key:
    environment: "API_KEY"

services:
  app:
    secrets:
      - db_password
      - api_key
    # Secrets available at /run/secrets/<name>
```

### Extensions (Reusable Fragments)

```yaml
# Define reusable blocks with x- prefix
x-common-env: &common-env
  LOG_LEVEL: info
  OTEL_ENDPOINT: http://otel-collector:4317

x-healthcheck-defaults: &healthcheck-defaults
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 30s

x-deploy-defaults: &deploy-defaults
  resources:
    limits:
      cpus: "0.5"
      memory: 512M
    reservations:
      cpus: "0.25"
      memory: 256M

services:
  service-a:
    environment:
      <<: *common-env
      SERVICE_NAME: service-a
    healthcheck:
      <<: *healthcheck-defaults
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
    deploy:
      <<: *deploy-defaults

  service-b:
    environment:
      <<: *common-env
      SERVICE_NAME: service-b
    healthcheck:
      <<: *healthcheck-defaults
      test: ["CMD", "curl", "-f", "http://localhost:8081/health"]
    deploy:
      <<: *deploy-defaults
```

### Multiple Compose Files (Override)

```bash
# Base + environment-specific overrides
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

```yaml
# docker-compose.yml (base)
services:
  app:
    build: .
    environment:
      - NODE_ENV=development

# docker-compose.prod.yml (override)
services:
  app:
    image: registry.company.com/app:${VERSION}
    environment:
      - NODE_ENV=production
    deploy:
      replicas: 3
```

### Environment Variables

```yaml
services:
  app:
    environment:
      # Direct value
      - DB_HOST=postgres
      # From host environment (or .env file)
      - DB_PASSWORD=${DB_PASSWORD}
      # With default
      - LOG_LEVEL=${LOG_LEVEL:-info}
      # Required (fails if not set)
      - API_KEY=${API_KEY:?API_KEY must be set}
    env_file:
      - .env           # Default
      - .env.local     # Override (last wins)
```

```bash
# .env file (auto-loaded)
COMPOSE_PROJECT_NAME=myproject
VERSION=3.2.1
DB_PASSWORD=secret123
```

---

## Compose Commands

```bash
# Start services
docker compose up -d                    # Detached
docker compose up -d --build            # Rebuild images
docker compose up -d --force-recreate   # Recreate containers
docker compose up -d --scale app=3      # Scale service

# Stop services
docker compose stop                     # Stop (keep containers)
docker compose down                     # Stop + remove containers + networks
docker compose down -v                  # Also remove volumes
docker compose down --rmi all           # Also remove images

# Logs
docker compose logs                     # All services
docker compose logs -f app              # Follow specific service
docker compose logs --tail 50 app       # Last 50 lines
docker compose logs --since 30m         # Last 30 minutes

# Status
docker compose ps                       # Running services
docker compose ps -a                    # All (including stopped)
docker compose top                      # Processes in each service

# Execute
docker compose exec app /bin/sh         # Shell into running service
docker compose exec -u root app bash    # As root
docker compose run --rm app npm test    # One-off command (new container)

# Build
docker compose build                    # Build all
docker compose build --no-cache app     # No cache for specific service
docker compose build --parallel         # Parallel builds

# Pull
docker compose pull                     # Pull all images

# Config validation
docker compose config                   # Validate and show resolved config
docker compose config --services        # List services
docker compose config --volumes         # List volumes

# Watch (auto-rebuild on file changes, Compose 2.22+)
docker compose watch
```

---

## Development Workflow

### Watch Mode (Hot Reload)

```yaml
services:
  app:
    build: .
    develop:
      watch:
        # Sync source files (no rebuild)
        - action: sync
          path: ./src
          target: /app/src
        # Rebuild on dependency change
        - action: rebuild
          path: ./package.json
        # Sync + restart on config change
        - action: sync+restart
          path: ./config
          target: /app/config
```

```bash
docker compose watch  # Auto-sync/rebuild on changes
```

### Development vs Production

```yaml
# docker-compose.yml (shared base)
services:
  app:
    environment:
      - DB_HOST=postgres

# docker-compose.override.yml (auto-loaded in dev)
services:
  app:
    build:
      context: .
      target: development
    volumes:
      - ./src:/app/src        # Live code reload
    ports:
      - "8080:8080"
      - "9229:9229"          # Debug port
    environment:
      - NODE_ENV=development
      - DEBUG=app:*

# docker-compose.prod.yml (explicit for production)
services:
  app:
    image: registry.company.com/app:${VERSION}
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 1G
    environment:
      - NODE_ENV=production
```

---

## Networking in Compose

```yaml
services:
  frontend:
    networks:
      - frontend-net

  api:
    networks:
      - frontend-net
      - backend-net

  db:
    networks:
      backend-net:
        ipv4_address: 172.28.0.10
        aliases:
          - database
          - postgres-primary

networks:
  frontend-net:
    driver: bridge
  backend-net:
    driver: bridge
    internal: true  # No internet access
    ipam:
      config:
        - subnet: 172.28.0.0/16
```

**DNS in Compose:**
- Services resolve each other by service name
- `api` can reach `db` at `db:5432`
- Network aliases provide additional DNS names

---

## Health Checks in Compose

```yaml
services:
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
      start_interval: 2s  # Compose 2.24+ (faster startup checks)

  redis:
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 3

  kafka:
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  elasticsearch:
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q '\"status\":\"green\"'"]
      interval: 30s
      timeout: 10s
      retries: 10
      start_period: 120s
```

---

## Compose Best Practices

1. **Use named volumes** for persistent data
2. **Use health checks** with `depends_on: condition: service_healthy`
3. **Use profiles** to separate dev tools from production services
4. **Use secrets** instead of environment variables for sensitive data
5. **Use `.env`** for variable substitution
6. **Use extensions** (`x-`) to avoid repetition
7. **Set resource limits** to prevent runaway containers
8. **Use `restart: unless-stopped`** for production services
9. **Use `internal: true`** networks for backend services
10. **Pin image versions** — never use `latest` in production

---

## Next: [Security →](07_Security.md)
