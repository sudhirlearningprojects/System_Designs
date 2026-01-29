# Spring Boot Containerization - Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Dockerfile Strategies](#dockerfile-strategies)
3. [Multi-Stage Builds](#multi-stage-builds)
4. [Docker Compose](#docker-compose)
5. [Production Best Practices](#production-best-practices)
6. [Kubernetes Deployment](#kubernetes-deployment)

---

## Introduction

### Why Containerize Spring Boot?

```
Benefits:
├─ Consistent environments (dev = prod)
├─ Easy scaling (horizontal/vertical)
├─ Isolation (dependencies, resources)
├─ Fast deployment (seconds vs minutes)
├─ Portability (run anywhere)
└─ Microservices architecture
```

### Container vs VM

```
Virtual Machine:
┌─────────────────────────────────┐
│     App A    │     App B        │
│  ┌────────┐  │  ┌────────┐     │
│  │ Bins   │  │  │ Bins   │     │
│  │ Libs   │  │  │ Libs   │     │
│  └────────┘  │  └────────┘     │
│  Guest OS    │  Guest OS        │
├──────────────┴──────────────────┤
│        Hypervisor               │
├─────────────────────────────────┤
│        Host OS                  │
└─────────────────────────────────┘

Container:
┌─────────────────────────────────┐
│  App A  │  App B  │  App C      │
│ ┌─────┐ │ ┌─────┐ │ ┌─────┐   │
│ │Bins │ │ │Bins │ │ │Bins │   │
│ │Libs │ │ │Libs │ │ │Libs │   │
│ └─────┘ │ └─────┘ │ └─────┘   │
├─────────┴─────────┴─────────────┤
│      Docker Engine              │
├─────────────────────────────────┤
│      Host OS                    │
└─────────────────────────────────┘

Containers are lighter and faster!
```

---

## Dockerfile Strategies

### Strategy 1: Basic Dockerfile

```dockerfile
# Basic approach (NOT recommended for production)
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/myapp-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Issues**:
- ❌ Large image size (~500MB)
- ❌ Includes build tools
- ❌ No layer optimization
- ❌ Rebuilds everything on code change

---

### Strategy 2: Optimized Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

# Create non-root user
RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

WORKDIR /app

# Copy JAR
COPY --chown=spring:spring target/myapp-1.0.0.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM optimization
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

**Improvements**:
- ✅ Non-root user (security)
- ✅ Health check
- ✅ JVM container awareness
- ✅ Memory optimization

---

### Strategy 3: Layered JAR (Spring Boot 2.3+)

```dockerfile
FROM openjdk:17-jdk-slim as builder

WORKDIR /app
COPY target/myapp-1.0.0.jar app.jar

# Extract layers
RUN java -Djarmode=layertools -jar app.jar extract

# Final image
FROM openjdk:17-jdk-slim

RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

WORKDIR /app

# Copy layers (cached separately)
COPY --from=builder --chown=spring:spring app/dependencies/ ./
COPY --from=builder --chown=spring:spring app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring app/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

**Benefits**:
- ✅ Layer caching (faster builds)
- ✅ Dependencies cached separately
- ✅ Only application layer changes on code update

**pom.xml configuration**:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <layers>
                    <enabled>true</enabled>
                </layers>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Multi-Stage Builds

### Complete Multi-Stage Dockerfile

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first (cache dependencies)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Extract layers
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy layers from build stage
COPY --from=build --chown=spring:spring /app/dependencies/ ./
COPY --from=build --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/application/ ./

# Environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
```

**Image size comparison**:
```
Basic Dockerfile:     ~500MB
Optimized:           ~350MB
Multi-stage + Alpine: ~200MB
```

---

## Docker Compose

### Single Service

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: spring-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mydb
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=password
    depends_on:
      db:
        condition: service_healthy
    networks:
      - app-network
    restart: unless-stopped

  db:
    image: postgres:15-alpine
    container_name: postgres-db
    environment:
      - POSTGRES_DB=mydb
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

volumes:
  postgres-data:

networks:
  app-network:
    driver: bridge
```

---

### Microservices Architecture

```yaml
version: '3.8'

services:
  # Service Discovery
  eureka:
    build: ./eureka-server
    container_name: eureka-server
    ports:
      - "8761:8761"
    networks:
      - microservices

  # API Gateway
  gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka
    depends_on:
      - eureka
    networks:
      - microservices

  # User Service
  user-service:
    build: ./user-service
    container_name: user-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://user-db:5432/userdb
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka
    depends_on:
      - eureka
      - user-db
    networks:
      - microservices
    deploy:
      replicas: 2

  user-db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=userdb
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    volumes:
      - user-db-data:/var/lib/postgresql/data
    networks:
      - microservices

  # Order Service
  order-service:
    build: ./order-service
    container_name: order-service
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://order-db:5432/orderdb
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:8761/eureka
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - eureka
      - order-db
      - kafka
    networks:
      - microservices

  order-db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=orderdb
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    volumes:
      - order-db-data:/var/lib/postgresql/data
    networks:
      - microservices

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    ports:
      - "6379:6379"
    networks:
      - microservices

  # Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - microservices

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - microservices

volumes:
  user-db-data:
  order-db-data:

networks:
  microservices:
    driver: bridge
```

---

## Production Best Practices

### 1. .dockerignore

```
# .dockerignore
target/
!target/*.jar
.git
.gitignore
.mvn
mvnw
mvnw.cmd
*.md
.idea
.vscode
*.iml
.DS_Store
```

### 2. application-docker.yml

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        jdbc:
          batch_size: 20
  
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6379}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    com.example: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### 3. Build and Run Commands

```bash
# Build image
docker build -t myapp:1.0.0 .

# Run container
docker run -d \
  --name myapp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mydb \
  myapp:1.0.0

# View logs
docker logs -f myapp

# Execute command in container
docker exec -it myapp sh

# Stop and remove
docker stop myapp
docker rm myapp

# Docker Compose
docker-compose up -d
docker-compose logs -f
docker-compose down
```

### 4. Resource Limits

```yaml
# docker-compose.yml
services:
  app:
    build: .
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    environment:
      - JAVA_OPTS=-Xmx1536m -Xms512m
```

### 5. Security Hardening

```dockerfile
# Use specific version (not latest)
FROM eclipse-temurin:17.0.9_9-jre-alpine

# Run as non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Read-only filesystem
VOLUME /tmp
RUN chmod 1777 /tmp

# Drop capabilities
SECURITY_OPT:
  - no-new-privileges:true

# Scan for vulnerabilities
RUN apk update && apk upgrade
```

---

## Kubernetes Deployment

### Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-app
  labels:
    app: spring-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: spring-app
  template:
    metadata:
      labels:
        app: spring-app
    spec:
      containers:
      - name: spring-app
        image: myapp:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: spring-app-service
spec:
  selector:
    app: spring-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
---
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
stringData:
  url: jdbc:postgresql://postgres:5432/mydb
  username: postgres
  password: password
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: spring-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: spring-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: spring-app-config
data:
  application.yml: |
    spring:
      datasource:
        hikari:
          maximum-pool-size: 20
      jpa:
        hibernate:
          ddl-auto: validate
    logging:
      level:
        root: INFO
```

---

## CI/CD Pipeline

### GitHub Actions

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean package -DskipTests
    
    - name: Build Docker image
      run: docker build -t myapp:${{ github.sha }} .
    
    - name: Push to Docker Hub
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        docker tag myapp:${{ github.sha }} myusername/myapp:latest
        docker push myusername/myapp:latest
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/spring-app spring-app=myusername/myapp:latest
        kubectl rollout status deployment/spring-app
```

---

## Monitoring and Logging

### Prometheus + Grafana

```yaml
# docker-compose.yml
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    networks:
      - monitoring

networks:
  monitoring:
```

**prometheus.yml**:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

---

## Summary

### Quick Reference

**Build Image**:
```bash
docker build -t myapp:1.0.0 .
```

**Run Container**:
```bash
docker run -d -p 8080:8080 --name myapp myapp:1.0.0
```

**Docker Compose**:
```bash
docker-compose up -d
```

**Kubernetes Deploy**:
```bash
kubectl apply -f deployment.yaml
```

### Best Practices Checklist

- ✅ Use multi-stage builds
- ✅ Leverage layer caching
- ✅ Run as non-root user
- ✅ Use specific image versions
- ✅ Add health checks
- ✅ Set resource limits
- ✅ Use .dockerignore
- ✅ Externalize configuration
- ✅ Implement logging
- ✅ Add monitoring (Prometheus)
- ✅ Use secrets for sensitive data
- ✅ Scan for vulnerabilities
- ✅ Implement CI/CD pipeline

### Image Size Optimization

```
Before optimization: 500MB
After multi-stage:   200MB
After Alpine:        150MB
After distroless:    120MB

Reduction: 76% smaller!
```

Containerization makes Spring Boot applications portable, scalable, and production-ready! 🚀
